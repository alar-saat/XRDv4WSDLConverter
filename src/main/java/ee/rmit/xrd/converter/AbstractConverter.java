package ee.rmit.xrd.converter;

import ee.rmit.xrd.wsdl.Attribute;
import ee.rmit.xrd.wsdl.MessagePart;
import ee.rmit.xrd.wsdl.Wsdl;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static ee.rmit.xrd.utils.DomUtils.*;
import static ee.rmit.xrd.utils.LoggerUtils.logWarning;
import static ee.rmit.xrd.utils.StringUtils.isBlank;
import static ee.rmit.xrd.utils.StringUtils.isNotBlank;
import static ee.rmit.xrd.utils.WsdlUtils.hasXrdSoapHeaderElement;
import static ee.rmit.xrd.wsdl.QualifiedNames.*;

public abstract class AbstractConverter {
    protected Wsdl wsdl;
    protected String operationName;
    protected List<MessagePart> inputMessageParts = new ArrayList<>();
    protected List<MessagePart> outputMessageParts = new ArrayList<>();
    protected List<MessagePart> faultMessageParts = new ArrayList<>();

    private boolean isXrdV2;
    private QName xrdQName;
    private Element inputAbstractMessage;
    private Element outputAbstractMessage;
    private Element faultAbstractMessage;
    private int inputMultipartRelatedAttachmentParts = 0;
    private int outputMultipartRelatedAttachmentParts = 0;

    public static final String XRDV3_1_NS = "http://x-road.ee/xsd/x-road.xsd";
    public static final String XRDV3_0_NS = "http://x-rd.net/xsd/xroad.xsd";
    public static final String XRDV2_NS = "http://x-tee.riik.ee/xsd/xtee.xsd";

    public AbstractConverter(Wsdl wsdl) {
        checkXrdVersion(wsdl);
        this.wsdl = wsdl;
    }

    /* useWrapperElements -> use wrapper elements like 'keha' and 'paring' mandatory in xrd v2 message */
    public abstract void addSchemaObjects(boolean useWrapperElements);

    private void checkXrdVersion(Wsdl wsdl) {
        List<QName> xrdQNames = new ArrayList<>();
        wsdl.getNamespaces().forEach(qName -> {
            if (qName.getNamespaceURI().equals(XRDV3_1_NS)
                    || qName.getNamespaceURI().equals(XRDV3_0_NS)
                    || qName.getNamespaceURI().equals(XRDV2_NS)) {
                xrdQNames.add(qName);
            }
        });
        if (xrdQNames.isEmpty()) {
            throw new IllegalStateException("Required XRD namespace not found");
        }
        if (xrdQNames.size() > 1) {
            throw new IllegalStateException("XRD namespace must be declared exactly 1 time, got " + xrdQNames.size());
        }
        xrdQName = xrdQNames.get(0);
        isXrdV2 = xrdQName.getNamespaceURI().equals(XRDV2_NS);
    }

    protected boolean isXrdV2() {
        return isXrdV2;
    }

    protected boolean isXrdV3() {
        return !isXrdV2;
    }

    protected void addBindingOperation(Wsdl template) {
        int size = wsdl.getWsdlBindingOperations().size();
        if (size != 1) {
            throw new IllegalStateException("WSDL binding operations must be exactly 1, got " + size);
        }
        Element origOperation = wsdl.getWsdlBindingOperations().get(0);
        operationName = getRequiredAttributeValue("name", origOperation);
        String version = getXrdVersionOrNull(origOperation);

        Element origInput = getRequiredChildElement(WSDL_INPUT, origOperation);
        Element origOutput = getRequiredChildElement(WSDL_OUTPUT, origOperation);
        Element origFault = getChildElementOrNull(WSDL_FAULT, origOperation);

        Document doc = template.getWsdl();
        Element binding = template.getWsdlBinding();

        Element operation = doc.createElementNS(WSDL_NS.getNamespaceURI(), WSDL_NS.getPrefix() + ":operation");
        operation.setAttribute("name", operationName);

        Element soapOperation = doc.createElementNS(SOAP_NS.getNamespaceURI(), SOAP_NS.getPrefix() + ":operation");
        soapOperation.setAttribute("soapAction", "");
        operation.appendChild(soapOperation);

        if (version != null) {
            Element xrdVersion = doc.createElementNS(XRD_NS.getNamespaceURI(), XRD_NS.getPrefix() + ":version");
            xrdVersion.setTextContent(version);
            operation.appendChild(xrdVersion);
        }

        Element input = doc.createElementNS(WSDL_NS.getNamespaceURI(), WSDL_NS.getPrefix() + ":input");
        boolean isXrdIssueSoapHeader = hasOptionalXrdIssueSoapHeader(origInput);
        List<Element> header = createXrdSoapHeaderElements(isXrdIssueSoapHeader, doc);
        handleMultipartRelated(origInput, input, header, doc);
        operation.appendChild(input);

        Element output = doc.createElementNS(WSDL_NS.getNamespaceURI(), WSDL_NS.getPrefix() + ":output");
        header = createXrdSoapHeaderElements(isXrdIssueSoapHeader, doc);
        handleMultipartRelated(origOutput, output, header, doc);
        operation.appendChild(output);

        if (origFault != null) {
            Element fault = createSoapFaultInBinding(origFault, doc);
            operation.appendChild(fault);
        }

        binding.appendChild(operation);
        template.getWsdlBindingOperations().add(operation);
    }

    private void handleMultipartRelated(Element origElement, Element element, List<Element> header, Document doc) {
        Element origMultipartRelated = getChildElementOrNull(MIME_MULTIPART, origElement);
        if (origMultipartRelated == null) {
            header.forEach(element::appendChild);
            Element soapBody = doc.createElementNS(SOAP_NS.getNamespaceURI(), SOAP_NS.getPrefix() + ":body");
            soapBody.setAttribute("use", "literal");
            element.appendChild(soapBody);
        } else {
            Element multipartRelated = createMultipartRelated(origElement.getLocalName(), origMultipartRelated, doc);
            List<Element> parts = getRequiredChildElements(MIME_PART, multipartRelated);
            Element soapPart = getEmptyPartInMultipart(parts);
            header.forEach(soapPart::appendChild);
            Element soapBody = doc.createElementNS(SOAP_NS.getNamespaceURI(), SOAP_NS.getPrefix() + ":body");
            soapBody.setAttribute("use", "literal");
            soapBody.setAttribute("parts", "parameters");
            soapPart.appendChild(soapBody);
            element.appendChild(multipartRelated);
        }
    }

    private String getXrdVersionOrNull(Element element) {
        Element version = getChildElementOrNull(new QName(xrdQName.getNamespaceURI(), "version"), element);
        if (version != null) {
            String versionValue = version.getTextContent();
            if (isNotBlank(versionValue)) {
                return versionValue;
            }
        }
        return null;
    }

    private boolean hasOptionalXrdIssueSoapHeader(Element element) {
        Element xrdHeaderMessage = getOldXrdHeaderMessage();
        if (xrdHeaderMessage == null) {
            return false;
        }
        try {
            List<Element> soapHeaderElements = getChildElements(SOAP_HEADER, element);
            if (soapHeaderElements.isEmpty()) {
                return false;
            }
            String issue;
            if (isXrdV2) {
                issue = "toimik";//v2 header
            } else {
                issue = "issue";//v3 header
            }
            return hasXrdSoapHeaderElement(issue, xrdHeaderMessage, soapHeaderElements);
        } catch (IllegalStateException e) {
            logWarning(e.getMessage());
            return false;
        }
    }

    private Element getOldXrdHeaderMessage() {
        Optional<Element> messageHolder = wsdl.getWsdlMessages().stream().filter(elem -> {
            List<Element> parts = getRequiredChildElements(WSDL_PART, elem);
            String expected;
            if (isXrdV2) {
                expected = xrdQName.getLocalPart() + ":andmekogu";//v2 header
            } else {
                expected = xrdQName.getLocalPart() + ":producer";//v3 header
            }
            return parts.stream().anyMatch(part -> hasAttributeValue(expected, "element", part));
        }).findFirst();

        return messageHolder.isPresent() ? messageHolder.get() : null;
    }

    private List<Element> createXrdSoapHeaderElements(boolean isXrdIssueSoapHeader, Document doc) {
        List<Element> header = new ArrayList<>();
        header.add(createXrdSoapHeaderElement("client", doc));
        header.add(createXrdSoapHeaderElement("service", doc));
        header.add(createXrdSoapHeaderElement("id", doc));
        header.add(createXrdSoapHeaderElement("protocolVersion", doc));
        header.add(createXrdSoapHeaderElement("userId", doc));
        if (isXrdIssueSoapHeader) {
            header.add(createXrdSoapHeaderElement("issue", doc));
        }
        return header;
    }

    private Element createXrdSoapHeaderElement(String part, Document doc) {
        Element headerElement = doc.createElementNS(SOAP_NS.getNamespaceURI(), SOAP_NS.getPrefix() + ":header");
        headerElement.setAttribute("message", "tns:xrdHeader");
        headerElement.setAttribute("use", "literal");
        headerElement.setAttribute("part", part);
        return headerElement;
    }

    private Element createMultipartRelated(String parentLocalName, Element origMultipartRelated, Document doc) {
        Element multipartRelated = doc.createElementNS(MIME_NS.getNamespaceURI(), MIME_NS.getPrefix() + ":multipartRelated");

        Element soapPart = doc.createElementNS(MIME_NS.getNamespaceURI(), MIME_NS.getPrefix() + ":part");
        //soapPart.setAttribute("name", "soap"); //[WS-I] R2908 The mime:part element in a DESCRIPTION MUST NOT have a name attribute.
        multipartRelated.appendChild(soapPart);

        List<Element> origParts = getRequiredChildElements(MIME_PART, origMultipartRelated);
        boolean isInputChannel = parentLocalName.equals(WSDL_INPUT.getLocalPart());
        if (isInputChannel) {
            //first part is soap envelope, skip that part
            inputMultipartRelatedAttachmentParts = origParts.size() - 1;
        } else {
            //first part is soap envelope, skip that part
            outputMultipartRelatedAttachmentParts = origParts.size() - 1;
        }
        int index = 0;
        for (Element origPart : origParts) {
            Element origContent = getChildElementOrNull(MIME_CONTENT, origPart);
            if (origContent != null) {
                Element attachmentPart = doc.createElementNS(MIME_NS.getNamespaceURI(), MIME_NS.getPrefix() + ":part");
                Element content = doc.createElementNS(MIME_NS.getNamespaceURI(), MIME_NS.getPrefix() + ":content");
                String name = "attachment";
                if (origParts.size() > 2) {
                    name += ++index;
                }
                //attachmentPart.setAttribute("name", name); //[WS-I] R2908 The mime:part element in a DESCRIPTION MUST NOT have a name attribute.
                content.setAttribute("part", name);
                String type = getAttributeValueOrNull("type", content);
                if (type == null) {
                    type = "application/octet-stream";
                }
                content.setAttribute("type", type);
                attachmentPart.appendChild(content);
                multipartRelated.appendChild(attachmentPart);
            }
        }
        return multipartRelated;
    }

    private Element getEmptyPartInMultipart(List<Element> parts) {
        Optional<Element> partHolder =
                parts.stream().filter(elem -> !elem.hasChildNodes()).findFirst();
        if (partHolder.isPresent()) {
            return partHolder.get();
        }
        throw new IllegalStateException("Required empty part in multipart/related not found");
    }

    private Element createSoapFaultInBinding(Element origFault, Document doc) {
        String name = getRequiredAttributeValue("name", origFault);
        Element origSoapFault = getRequiredChildElement(SOAP_FAULT, origFault);
        String soapName = getRequiredAttributeValue("name", origSoapFault);
        Element fault = doc.createElementNS(WSDL_NS.getNamespaceURI(), WSDL_NS.getPrefix() + ":fault");
        fault.setAttribute("name", name);
        Element soapFault = doc.createElementNS(SOAP_NS.getNamespaceURI(), SOAP_NS.getPrefix() + ":fault");
        soapFault.setAttribute("name", soapName);
        soapFault.setAttribute("use", "literal");
        fault.appendChild(soapFault);
        return fault;
    }

    protected void addPortTypeOperation(Wsdl template) {
        int size = wsdl.getWsdlPortTypeOperations().size();
        if (size != 1) {
            throw new IllegalStateException("WSDL portType operations must be exactly 1, got " + size);
        }
        Element origOperation = wsdl.getWsdlPortTypeOperations().get(0);
        String title = getXrdTitleOrNull(origOperation);

        Element origInput = getRequiredChildElement(WSDL_INPUT, origOperation);
        inputAbstractMessage = getRequiredAbstractMessage(getRequiredAttributeValue("message", origInput));
        Element origOutput = getRequiredChildElement(WSDL_OUTPUT, origOperation);
        outputAbstractMessage = getRequiredAbstractMessage(getRequiredAttributeValue("message", origOutput));
        Element origFault = getChildElementOrNull(WSDL_FAULT, origOperation);
        if (origFault != null) {
            faultAbstractMessage = getRequiredAbstractMessage(getRequiredAttributeValue("message", origFault));
        }

        Document doc = template.getWsdl();
        Element portType = template.getWsdlPortType();

        Element operation = doc.createElementNS(WSDL_NS.getNamespaceURI(), WSDL_NS.getPrefix() + ":operation");
        operation.setAttribute("name", operationName);

        if (title != null) {
            Element documentation = doc.createElementNS(WSDL_NS.getNamespaceURI(), WSDL_NS.getPrefix() + ":documentation");
            Element xrdTitle = doc.createElementNS(XRD_NS.getNamespaceURI(), XRD_NS.getPrefix() + ":title");
            xrdTitle.setTextContent(title);
            documentation.appendChild(xrdTitle);
            operation.appendChild(documentation);
        }

        Element input = doc.createElementNS(WSDL_NS.getNamespaceURI(), WSDL_NS.getPrefix() + ":input");
        input.setAttribute("message", "tns:" + operationName);
        operation.appendChild(input);

        Element output = doc.createElementNS(WSDL_NS.getNamespaceURI(), WSDL_NS.getPrefix() + ":output");
        output.setAttribute("message", String.format("tns:%sResponse", operationName));
        operation.appendChild(output);

        if (origFault != null) {
            String faultMessage = getRequiredAttributeValue("message", origFault);
            String[] splitted = faultMessage.split(":");
            if (splitted.length == 2) {
                faultMessage = splitted[1];
            }
            String faultName = getAttributeValueOrNull("name", origFault);
            if (isBlank(faultName)) {
                faultName = "exception";
            }
            Element fault = doc.createElementNS(WSDL_NS.getNamespaceURI(), WSDL_NS.getPrefix() + ":fault");
            fault.setAttribute("message", "tns:" + faultMessage);
            fault.setAttribute("name", faultName);
            operation.appendChild(fault);
        }

        portType.appendChild(operation);
        template.getWsdlPortTypeOperations().add(operation);
    }

    private String getXrdTitleOrNull(Element element) {
        Element documentation = getChildElementOrNull(new QName(WSDL_NS.getNamespaceURI(), "documentation"), element);
        if (documentation == null) {
            return null;
        }
        Element title = getChildElementOrNull(new QName(xrdQName.getNamespaceURI(), "title"), documentation);
        if (title != null) {
            String titleValue = title.getTextContent();
            if (isNotBlank(titleValue)) {
                return titleValue;
            }
        }
        return null;
    }

    private Element getRequiredAbstractMessage(String qName) {
        String[] splitted = qName.split(":");
        final String name;
        if (splitted.length == 2) {
            name = splitted[1];
        } else {
            name = qName;
        }
        Optional<Element> messageHolder =
                wsdl.getWsdlMessages().stream().filter(elem -> getRequiredAttributeValue("name", elem).equals(name)).findFirst();
        if (messageHolder.isPresent()) {
            return messageHolder.get();
        }
        throw new IllegalStateException(String.format("Required abstract message by name '%s' not found", name));
    }

    protected void addAbstractMessages(Wsdl template) {
        addInputAbstractMessage(template);
        prepareInputMessageParts();

        addOutputAbstractMessage(template);
        prepareOutputMessageParts();

        if (faultAbstractMessage != null) {
            addFaultAbstractMessage(template);
            prepareFaultMessageParts();
        }
    }

    private void addInputAbstractMessage(Wsdl template) {
        Document doc = template.getWsdl();
        Element message = doc.createElementNS(WSDL_NS.getNamespaceURI(), WSDL_NS.getPrefix() + ":message");
        message.setAttribute("name", operationName);
        Element soapPart = doc.createElementNS(WSDL_NS.getNamespaceURI(), WSDL_NS.getPrefix() + ":part");
        soapPart.setAttribute("element", "tns:" + operationName);
        soapPart.setAttribute("name", "parameters");
        message.appendChild(soapPart);
        if (inputMultipartRelatedAttachmentParts == 1) {
            message.appendChild(createAttachmentPart("attachment", doc));
        } else if (inputMultipartRelatedAttachmentParts > 1) {
            for (int i = 1; i <= inputMultipartRelatedAttachmentParts; i++) {
                message.appendChild(createAttachmentPart("attachment" + i, doc));
            }
        }
        template.getDocumentElement().insertBefore(message, template.getWsdlPortType());
        template.getWsdlMessages().add(message);
    }

    private Element createAttachmentPart(String name, Document doc) {
        Element attachment = doc.createElementNS(WSDL_NS.getNamespaceURI(), WSDL_NS.getPrefix() + ":part");
        attachment.setAttribute("type", "xsd:base64Binary");
        attachment.setAttribute("name", name);
        return attachment;
    }

    private void prepareInputMessageParts() {
        List<Element> inputMessageParts = getDirectChildElementsOnly(WSDL_PART, inputAbstractMessage);
        if (inputMessageParts.isEmpty()) {
            throw new IllegalStateException(String.format("Input abstract message '%s' without part element(s)"
                    , getAttributeValueOrNull("name", inputAbstractMessage)));
        }
        inputMessageParts.stream().forEach(elem -> this.inputMessageParts.add(new MessagePart(elem)));
    }

    private void addOutputAbstractMessage(Wsdl template) {
        Document doc = template.getWsdl();
        Element message = doc.createElementNS(WSDL_NS.getNamespaceURI(), WSDL_NS.getPrefix() + ":message");
        message.setAttribute("name", operationName + "Response");
        Element soapPart = doc.createElementNS(WSDL_NS.getNamespaceURI(), WSDL_NS.getPrefix() + ":part");
        soapPart.setAttribute("element", String.format("tns:%sResponse", operationName));
        soapPart.setAttribute("name", "parameters");
        message.appendChild(soapPart);
        if (outputMultipartRelatedAttachmentParts == 1) {
            message.appendChild(createAttachmentPart("attachment", doc));
        } else if (outputMultipartRelatedAttachmentParts > 1) {
            for (int i = 1; i <= outputMultipartRelatedAttachmentParts; i++) {
                message.appendChild(createAttachmentPart("attachment" + i, doc));
            }
        }
        template.getDocumentElement().insertBefore(message, template.getWsdlPortType());
        template.getWsdlMessages().add(message);
    }

    private void prepareOutputMessageParts() {
        List<Element> outputMessageParts = getDirectChildElementsOnly(WSDL_PART, outputAbstractMessage);
        if (outputMessageParts.isEmpty()) {
            throw new IllegalStateException(String.format("Output abstract message '%s' without part element(s)"
                    , getAttributeValueOrNull("name", outputAbstractMessage)));
        }
        outputMessageParts.stream().forEach(elem -> this.outputMessageParts.add(new MessagePart(elem)));
    }

    private void addFaultAbstractMessage(Wsdl template) {
        Document doc = template.getWsdl();
        Element message = doc.createElementNS(WSDL_NS.getNamespaceURI(), WSDL_NS.getPrefix() + ":message");
        String messageName = getRequiredAttributeValue("name", faultAbstractMessage);
        message.setAttribute("name", messageName);
        List<Element> faultMessageParts = getDirectChildElementsOnly(WSDL_PART, faultAbstractMessage);
        faultMessageParts.forEach(elem -> {
            String name = getRequiredAttributeValue("name", elem);
            String type = getAttributeValueOrNull("type", elem);
            if (type != null) {
                Element part = doc.createElementNS(WSDL_NS.getNamespaceURI(), WSDL_NS.getPrefix() + ":part");
                part.setAttribute("element", "tns:" + name);
                part.setAttribute("name", name);
                message.appendChild(part);
            } else {
                String element = getAttributeValueOrNull("element", elem);
                if (element != null) {
                    Element part = doc.createElementNS(WSDL_NS.getNamespaceURI(), WSDL_NS.getPrefix() + ":part");
                    String[] splitted = element.split(":");
                    part.setAttribute("element", "tns:" + (splitted.length == 2 ? splitted[1] : element));
                    part.setAttribute("name", name);
                    message.appendChild(part);
                }
            }
        });
        template.getDocumentElement().insertBefore(message, template.getWsdlPortType());
        template.getWsdlMessages().add(message);
    }

    private void prepareFaultMessageParts() {
        List<Element> faultMessageParts = getDirectChildElementsOnly(WSDL_PART, faultAbstractMessage);
        if (faultMessageParts.isEmpty()) {
            throw new IllegalStateException(String.format("Fault abstract message '%s' without part element(s)"
                    , getAttributeValueOrNull("name", faultAbstractMessage)));
        }
        faultMessageParts.stream().forEach(elem -> this.faultMessageParts.add(new MessagePart(elem)));
    }

    protected void importAllSchemaObjects(Wsdl template) {
        QName soapEncodingQName = getSoapEncodingQNameOrNull();
        Element schema = wsdl.getWsdlSchema();
        List<Element> schemaElements = getDirectChildElementsOnly(XSD_ELEMENT, schema);
        schemaElements.addAll(getDirectChildElementsOnly(XSD_COMPLEX_TYPE, schema));
        schemaElements.addAll(getDirectChildElementsOnly(XSD_SIMPLE_TYPE, schema));
        schemaElements.addAll(getDirectChildElementsOnly(XSD_ATTRIBUTE, schema));
        schemaElements.addAll(getDirectChildElementsOnly(XSD_ATTRIBUTE_GROUP, schema));
        schemaElements.stream().forEach(elem -> {
            if (soapEncodingQName != null) {
                handleSoapEncodingRestrictions(soapEncodingQName, elem);
            }
            Element importedElement = (Element) template.getWsdl().importNode(elem, true);
            String origTargetNamespacePrefix = wsdl.getTargetNamespacePrefix();
            List<String> origXsdNamespacePrefixes = getXsdPrefixes(importedElement);
            origXsdNamespacePrefixes.forEach(prefix -> setPrefixToNull(prefix, importedElement));
            List<Attribute> attributes = new ArrayList<>();
            setAttributesToListThatHavePrefixedValue(importedElement, attributes, "type", "base", "ref");
            for (Attribute attribute : attributes) {
                Node attributeNode = attribute.getAttribute();
                if (attribute.getPrefix().equals(origTargetNamespacePrefix)) {
                    attributeNode.setTextContent("tns:" + attribute.getValue());
                } else {
                    Optional<String> xsdPrefixHolder =
                            origXsdNamespacePrefixes.stream().filter(prefix -> prefix.equals(attribute.getPrefix())).findFirst();
                    if (xsdPrefixHolder.isPresent()) {
                        attributeNode.setTextContent(attribute.getValue());
                    } else if (attribute.getPrefix().equals(xrdQName.getLocalPart())) {
                        if (attribute.getValue().equals("faultCode") || attribute.getValue().equals("faultString")) {
                            attributeNode.setTextContent("string");
                        }
                    }
                }
            }
            List<Element> xrdElements = getChildElements(new QName(xrdQName.getNamespaceURI(), "*"), importedElement);
            xrdElements.forEach(xrdElem -> {
                String qName = String.format("%s:%s", XRD_NS.getLocalPart(), xrdElem.getLocalName());
                if (xrdElem.getLocalName().equals("technotes")) {
                    qName = XRD_NS.getLocalPart() + ":techNotes";
                }
                template.getWsdl().renameNode(xrdElem, XRD_NS.getNamespaceURI(), qName);
            });
            template.getWsdlSchema().appendChild(importedElement);
        });
    }

    private QName getSoapEncodingQNameOrNull() {
        Optional<QName> qNameHolder = wsdl.getNamespaces().stream()
                .filter(qName -> qName.getNamespaceURI().equals("http://schemas.xmlsoap.org/soap/encoding/")).findFirst();
        return qNameHolder.isPresent() ? qNameHolder.get() : null;
    }

    private void handleSoapEncodingRestrictions(QName soapEncodingQName, Element element) {
        List<Element> restrictions = getChildElements(XSD_RESTRICTION, element);
        restrictions.addAll(getChildElements(XSD_EXTENSION, element));
        for (Element restriction : restrictions) {
            String base = getAttributeValueOrNull("base", restriction);
            if (base == null) {
                continue;
            }
            String[] splitted = base.split(":");
            if (soapEncodingQName.getLocalPart().equals(splitted[0])) {
                Node parent = restriction.getParentNode();
                if (parent.getNodeType() == Node.ELEMENT_NODE && parent.getLocalName().equals("complexContent")) {
                    Element complexContent = (Element) parent;
                    List<Element> children = getDirectChildElementsOnly(null, restriction);
                    children.forEach(elem -> {
                        Node node = wsdl.getWsdl().importNode(elem, true);
                        complexContent.getParentNode().appendChild(node);
                    });
                    complexContent.getParentNode().removeChild(complexContent);
                }
            }
        }
    }
}