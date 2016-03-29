package ee.rmit.xrd.validator;

import ee.rmit.xrd.wsdl.Wsdl;
import org.w3c.dom.Element;

import javax.xml.transform.dom.DOMSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static ee.rmit.xrd.utils.DomUtils.*;
import static ee.rmit.xrd.utils.LoggerUtils.logError;
import static ee.rmit.xrd.utils.StringUtils.isBlank;
import static ee.rmit.xrd.wsdl.QualifiedNames.*;

public class BindingValidator extends AbstractValidator {
    private List<String> xrdHeaderElements = new ArrayList<>();
    private List<Element> xrdHeaderParts = new ArrayList<>();

    public BindingValidator(Wsdl wsdl) {
        super(wsdl);
        xrdHeaderElements.add("client");
        xrdHeaderElements.add("service");
        xrdHeaderElements.add("id");
        xrdHeaderElements.add("protocolVersion");
        xrdHeaderElements.add("userId");
        xrdHeaderElements.add("issue");
    }

    public void validate() {
        checkRequiredAttributes(wsdlV4.getWsdlBinding(), "name", "type");
        String typeValue = wsdlV4.getWsdlBinding().getAttribute("type");
        checkQualifiedAttributeForTargetNamespace(typeValue);
        //check reference to wsdl:portType
        checkRequiredAttributeValue(typeValue.split(":")[1], "name", wsdlV4.getWsdlPortType());
        Element soapBinding = getRequiredChildElement(SOAP_BINDING, wsdlV4.getWsdlBinding());
        checkRequiredAttributes(soapBinding, "transport");
        checkRequiredAttributeValue("http://schemas.xmlsoap.org/soap/http", "transport", soapBinding);
        checkOptionalAttributeValue("document", "style", soapBinding);
        checkBindingOperations();
    }

    private void checkBindingOperations() {
        for (Element operation : wsdlV4.getWsdlBindingOperations()) {
            try {
                String operationName = getRequiredAttributeValue("name", operation);
                checkRequiredPortTypeOperationName(operation.getAttribute("name"));
                Element soapOperation = getRequiredChildElement(SOAP_OPERATION, operation);
                checkRequiredAttributes(soapOperation, "soapAction");
                checkOptionalAttributeValue("document", "style", soapOperation);
                //optional xrd:version
                if (hasChildElement(XRD_VERSION, operation)) {
                    Element xrdVersion = getRequiredChildElement(XRD_VERSION, operation);
                    checkXrdVersionValue(xrdVersion);
                }
                Element input = getRequiredChildElement(WSDL_INPUT, operation);
                checkBindingOperation(input, operationName, true);
                Element output = getRequiredChildElement(WSDL_OUTPUT, operation);
                checkBindingOperation(output, operationName, false);
            } catch (RuntimeException e) {
                logError("WSDL 'binding' operation containing an error: " + formatDomSource(new DOMSource(operation)));
                throw e;
            }
        }
    }

    private void checkRequiredPortTypeOperationName(String operation) {
        if (wsdlV4.getWsdlPortTypeOperations().stream().noneMatch(oper -> getRequiredAttributeValue("name", oper).equals(operation))) {
            throw new IllegalStateException(String.format("Required port type operation by name '%s' not found", operation));
        }
    }

    private void checkXrdVersionValue(Element element) {
        String value = element.getTextContent();
        if (isBlank(value) || !value.matches("v\\d{1,2}")) {
            throw new IllegalStateException(String.format("Invalid XRD service version '%s' for element '%s'"
                    , value, element.getLocalName()));
        }
    }

    private void checkBindingOperation(Element element, String operationName, boolean input) {
        MultipartMimeValidator validator = new MultipartMimeValidator(element, operationName, input);
        if (validator.isMultipart()) {
            validator.validate();
        } else {
            validateSoapPart(element);
        }
    }

    //wsdl:input, wsdl:output or mime:part
    private void validateSoapPart(Element element) {
        List<Element> headerElements = getRequiredChildElements(SOAP_HEADER, element);
        validateSoapHeaderAttributes(headerElements);
        Element body = getRequiredChildElement(SOAP_BODY, element);
        //multipart related
        if (element.getLocalName().equals("part")) {
            checkRequiredAttributesWithExactCount(body, "parts", "use");
        } else {
            checkRequiredAttributesWithExactCount(body, "use");
        }
        checkRequiredAttributeValue("literal", "use", body);
    }

    private void validateSoapHeaderAttributes(List<Element> headerElements) {
        headerElements.forEach(elem -> {
            checkRequiredAttributesWithExactCount(elem, "message", "part", "use");
            checkRequiredAttributeValue("literal", "use", elem);
            checkQualifiedAttributeForTargetNamespace(elem.getAttribute("message"));
            checkAbstractMessagePartReference(elem);
        });
        checkRequiredXrdHeader("client", headerElements);
        checkRequiredXrdHeader("service", headerElements);
        checkRequiredXrdHeader("id", headerElements);
        checkRequiredXrdHeader("protocolVersion", headerElements);
    }

    private void checkAbstractMessagePartReference(Element element) {
        String message = element.getAttribute("message").split(":")[1];
        String part = element.getAttribute("part");
        if (xrdHeaderParts.isEmpty()) {
            Optional<Element> abstractMessage =
                    wsdlV4.getWsdlMessages().stream().filter(elem -> getRequiredAttributeValue("name", elem).equals(message)).findFirst();
            if (abstractMessage.isPresent()) {
                xrdHeaderParts.addAll(getRequiredChildElements(WSDL_PART, abstractMessage.get()));
            }
            if (xrdHeaderParts.isEmpty()) {
                throw new IllegalStateException(String.format("Required abstract message '%s' for XRD header element '%s' not found"
                        , message, part));
            }
        }
        Optional<Element> partInAbstractMessageHolder =
                xrdHeaderParts.stream().filter(elem -> hasAttributeValue(part, "name", elem)).findFirst();
        if (!partInAbstractMessageHolder.isPresent()) {
            throw new IllegalStateException(String.format("Required abstract message '%s' part '%s' not found", message, part));
        }
        checkXrdQualifiedValue(partInAbstractMessageHolder.get());
    }

    private void checkXrdQualifiedValue(Element element) {
        String qualifiedElementValue = getRequiredAttributeValue("element", element);
        checkQualifiedAttributeForXrdNamespace(qualifiedElementValue);
        String elementValue = qualifiedElementValue.split(":")[1];
        if (xrdHeaderElements.stream().noneMatch(name -> name.equals(elementValue))) {
            throw new IllegalStateException(String.format("Unknown XRD header value '%s' for attribute 'element'", elementValue));
        }
    }

    private void checkRequiredXrdHeader(String headerName, List<Element> headerElements) {
        for (Element element : xrdHeaderParts) {
            String value = element.getAttribute("element");
            if (value.split(":")[1].equals(headerName)) {
                String name = element.getAttribute("name");
                if (headerElements.stream().anyMatch(elem -> elem.getAttribute("part").equals(name))) {
                    return;
                }
            }
        }
        throw new IllegalStateException(String.format("Required XRD header '%s' not found", headerName));
    }

    private class MultipartMimeValidator {
        //input or output
        private Element element;
        private String operationName;
        //true -> input, false -> output channel
        private boolean input = true;
        private Element multipartRelated;

        private MultipartMimeValidator(Element element, String operationName, boolean input) {
            this.element = element;
            this.operationName = operationName;
            this.input = input;
        }

        private boolean isMultipart() {
            List<Element> multiparts = getChildElements(MIME_MULTIPART, element);
            if (multiparts.isEmpty()) {
                return false;
            }
            if (multiparts.size() == 1) {
                multipartRelated = multiparts.get(0);
                return true;
            }
            throw new IllegalStateException("Invalid count of 'multipartRelated' tags. Required 0 or 1, got " + multiparts.size());
        }

        private void validate() {
            List<Element> parts = getRequiredChildElements(MIME_PART, multipartRelated);
            //first part must be soap envelope
            Element soapPart = parts.get(0);
            //checkRequiredAttributes(soapPart, "name"); //[WS-I] R2908 The mime:part element in a DESCRIPTION MUST NOT have a name attribute.
            validateSoapPart(soapPart);
            Element body = getRequiredChildElement(SOAP_BODY, soapPart);
            String partsValue = getRequiredAttributeValue("parts", body);
            checkRequiredSchemaElementValue(partsValue);
            //at least one attachment part must exist
            if (parts.size() == 1) {
                throw new IllegalStateException("'multipartRelated' without an attachment part");
            }
            parts.subList(1, parts.size()).forEach(elem -> {
                //checkRequiredAttributes(elem, "name"); //[WS-I] R2908 The mime:part element in a DESCRIPTION MUST NOT have a name attribute.
                Element content = getRequiredChildElement(MIME_CONTENT, elem);
                checkRequiredAttributes(content, "part", "type");
                String partValue = getRequiredAttributeValue("part", content);
                checkRequiredSchemaTypeValue(partValue);
            });
        }

        private void checkRequiredSchemaElementValue(String partValue) {
            Element part = getRequiredAbstractMessagePart(partValue);
            String attributeValue = getRequiredAttributeValue("element", part);
            checkQualifiedAttributeForAnyNamespace(attributeValue);
        }

        private Element getRequiredAbstractMessagePart(String partValue) {
            Optional<Element> portTypeOperationHolder = wsdlV4.getWsdlPortTypeOperations()
                    .stream().filter(elem -> getRequiredAttributeValue("name", elem).equals(operationName)).findFirst();
            if (!portTypeOperationHolder.isPresent()) {
                throw new IllegalStateException(String.format("Required portType operation '%s' not found", operationName));
            }
            Element portTypeOperation = portTypeOperationHolder.get();
            Element messageReference = getRequiredChildElement((input ? WSDL_INPUT : WSDL_OUTPUT), portTypeOperation);
            String qualifiedMessageValue = getRequiredAttributeValue("message", messageReference);
            checkQualifiedAttributeForTargetNamespace(qualifiedMessageValue);
            String messageValue = qualifiedMessageValue.split(":")[1];
            Optional<Element> abstractMessageHolder =
                    wsdlV4.getWsdlMessages().stream().filter(elem -> getRequiredAttributeValue("name", elem).equals(messageValue)).findFirst();
            if (!abstractMessageHolder.isPresent()) {
                throw new IllegalStateException(String.format("Required abstract message by name '%s' not found", messageValue));
            }
            Element abstractMessage = abstractMessageHolder.get();
            Optional<Element> partHolder =
                    getRequiredChildElements(WSDL_PART, abstractMessage).stream().filter(elem -> getRequiredAttributeValue("name", elem).equals(partValue)).findFirst();
            if (!partHolder.isPresent()) {
                throw new IllegalStateException(String.format("Required abstract message part by name '%s' not found", partValue));
            }
            return partHolder.get();
        }

        private void checkRequiredSchemaTypeValue(String partValue) {
            Element part = getRequiredAbstractMessagePart(partValue);
            String attributeValue = getRequiredAttributeValue("type", part);
            checkQualifiedAttributeForXsdNamespace(attributeValue);
        }
    }
}
