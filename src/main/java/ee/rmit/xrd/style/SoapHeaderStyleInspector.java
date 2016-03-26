package ee.rmit.xrd.style;

import ee.rmit.xrd.wsdl.Wsdl;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static ee.rmit.xrd.utils.DomUtils.*;
import static ee.rmit.xrd.utils.WsdlUtils.hasXrdSoapHeaderElement;
import static ee.rmit.xrd.wsdl.QualifiedNames.*;

public class SoapHeaderStyleInspector {
    private Wsdl wsdl;
    private Wsdl template;
    private List<String> xrdHeaderElements = new ArrayList<>();
    private String xrdHeaderMessage;

    public SoapHeaderStyleInspector(Wsdl wsdl, Wsdl template) {
        this.wsdl = wsdl;
        this.template = template;
        xrdHeaderElements.add(XRD_CLIENT.getLocalPart());
        xrdHeaderElements.add(XRD_SERVICE.getLocalPart());
        xrdHeaderElements.add(XRD_ID.getLocalPart());
        xrdHeaderElements.add(XRD_PROTO_VER.getLocalPart());
        xrdHeaderElements.add(XRD_USER_ID.getLocalPart());
        xrdHeaderElements.add(XRD_ISSUE.getLocalPart());
        xrdHeaderMessage = wsdl.getTargetNamespacePrefix() + ":xrdHeader";
    }

    public void checkStyle() {
        for (Element operation : wsdl.getWsdlBindingOperations()) {
            Element input = getInputMultipartAware(operation);
            handleXrdSoapHeaderElements(input);
            handleSoapBody(input);
            Element output = getOutputMultipartAware(operation);
            handleXrdSoapHeaderElements(output);
            handleSoapBody(output);
        }
        removeAllXrdHeaderMessages();
        importXrdHeaderMessage();
    }

    private Element getInputMultipartAware(Element element) {
        Element input = getRequiredChildElement(WSDL_INPUT, element);
        Element multiPart = getMultipartFirstPartOrNull(input);
        return multiPart == null ? input : multiPart;
    }

    private Element getMultipartFirstPartOrNull(Element element) {
        List<Element> multiparts = getChildElements(MIME_MULTIPART, element);
        if (multiparts.isEmpty()) {
            return null;
        }
        List<Element> parts = getRequiredChildElements(MIME_PART, element);
        return parts.get(0);
    }

    private Element getOutputMultipartAware(Element element) {
        Element output = getRequiredChildElement(WSDL_OUTPUT, element);
        Element multiPart = getMultipartFirstPartOrNull(output);
        return multiPart == null ? output : multiPart;
    }

    private void handleXrdSoapHeaderElements(Element element) {
        List<Element> headerElements = getRequiredChildElements(SOAP_HEADER, element);
        if (isAllXrdHeaderElementsWellFormed(headerElements)) {
            return;
        }
        String messageValue = getRequiredAttributeValue("message", headerElements.get(0));
        Element xrdHeaderMessage = geRequiredXrdHeaderAbstractMessageByName(messageValue.split(":")[1], wsdl.getWsdlMessages());

        //optional userId header element
        boolean hasUserId = hasOptionalHeaderElement(XRD_USER_ID.getLocalPart(), xrdHeaderMessage, headerElements);
        //optional issue header element
        boolean hasIssue = hasOptionalHeaderElement(XRD_ISSUE.getLocalPart(), xrdHeaderMessage, headerElements);
        int position = 0;
        setXrdHeaderElement(XRD_CLIENT.getLocalPart(), headerElements.get(position));
        setXrdHeaderElement(XRD_SERVICE.getLocalPart(), headerElements.get(++position));
        setXrdHeaderElement(XRD_ID.getLocalPart(), headerElements.get(++position));
        setXrdHeaderElement(XRD_PROTO_VER.getLocalPart(), headerElements.get(++position));
        if (hasUserId) {
            setXrdHeaderElement(XRD_USER_ID.getLocalPart(), headerElements.get(++position));
        }
        if (hasIssue) {
            setXrdHeaderElement(XRD_ISSUE.getLocalPart(), headerElements.get(++position));
        }
    }

    private void removeAllXrdHeaderMessages() {
        List<Element> redundantXrdHeaderMessages = new ArrayList<>();
        wsdl.getWsdlMessages().forEach(elem -> {
            List<Element> parts = getRequiredChildElements(WSDL_PART, elem);
            for (Element part : parts) {
                Node elemAttr = getAttributeOrNull("element", part);
                if (elemAttr != null) {
                    String elemValue = elemAttr.getTextContent().split(":")[1];
                    if (xrdHeaderElements.contains(elemValue)) {
                        wsdl.getDocumentElement().removeChild(elem);
                        redundantXrdHeaderMessages.add(elem);
                        break;
                    }
                }
            }
        });
        redundantXrdHeaderMessages.forEach(elem -> wsdl.removeWsdlMessage(getRequiredAttributeValue("name", elem)));
    }

    private void importXrdHeaderMessage() {
        Element templateXrdHeader =
            geRequiredXrdHeaderAbstractMessageByName(xrdHeaderMessage.split(":")[1], template.getWsdlMessages());
        Node importedXrdHeader = wsdl.getWsdl().importNode(templateXrdHeader, true);
        wsdl.getDocumentElement().insertBefore(importedXrdHeader, wsdl.getWsdlPortType());
        wsdl.getWsdlMessages().add((Element) importedXrdHeader);
    }

    private boolean isAllXrdHeaderElementsWellFormed(List<Element> headerElements) {
        return headerElements.stream().allMatch(elem -> {
            String messageValue = getRequiredAttributeValue("message", elem);
            String partValue = getRequiredAttributeValue("part", elem);
            return messageValue.equals(xrdHeaderMessage) && xrdHeaderElements.contains(partValue);
        });
    }

    private Element geRequiredXrdHeaderAbstractMessageByName(String name, List<Element> messages) {
        Optional<Element> xrdHeaderHolder = messages
            .stream().filter(elem -> getRequiredAttributeValue("name", elem).equals(name)).findFirst();
        if (xrdHeaderHolder.isPresent()) {
            return xrdHeaderHolder.get();
        }
        throw new IllegalArgumentException(String.format("XRD header's abstract message by name '%s' not found", name));
    }

    private boolean hasOptionalHeaderElement(String name, Element xrdHeader, List<Element> headerElements) {
        int numOfRequiredHeaders = 4;
        return headerElements.size() != numOfRequiredHeaders && hasXrdSoapHeaderElement(name, xrdHeader, headerElements);
    }

    private void setXrdHeaderElement(String name, Element element) {
        Node messageAttribute = getRequiredAttribute("message", element);
        messageAttribute.setTextContent(xrdHeaderMessage);
        Node partAttribute = getRequiredAttribute("part", element);
        partAttribute.setTextContent(name);
    }

    private void handleSoapBody(Element element) {
        Element body = getRequiredChildElement(SOAP_BODY, element);
        Node attributeParts = getAttributeOrNull("parts", body);
        if (attributeParts != null) {
            attributeParts.setTextContent("parameters");
        }
    }
}
