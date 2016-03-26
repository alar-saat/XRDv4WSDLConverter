package ee.rmit.xrd.validator;

import ee.rmit.xrd.wsdl.Wsdl;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import javax.xml.transform.dom.DOMSource;
import java.util.ArrayList;
import java.util.List;

import static ee.rmit.xrd.utils.DomUtils.*;
import static ee.rmit.xrd.utils.LoggerUtils.logError;
import static ee.rmit.xrd.utils.StringUtils.isBlank;
import static ee.rmit.xrd.wsdl.QualifiedNames.*;

public class PortTypeValidator extends AbstractValidator {
    private List<String> xrdDocumentationElements = new ArrayList<>();

    public PortTypeValidator(Wsdl wsdl) {
        super(wsdl);
        xrdDocumentationElements.add("title");
        xrdDocumentationElements.add("notes");
        xrdDocumentationElements.add("techNotes");
    }

    public void validate() {
        checkRequiredAttributes(wsdlV4.getWsdlPortType(), "name");
        checkPortTypeOperations();
    }

    private void checkPortTypeOperations() {
        for (Element operation : wsdlV4.getWsdlPortTypeOperations()) {
            try {
                if (hasChildElement(WSDL_DOC, operation)) {
                    Element documentation = getRequiredChildElement(WSDL_DOC, operation);
                    checkXrdDocumentation(documentation);
                }
                Element input = getRequiredChildElement(WSDL_INPUT, operation);
                checkPortTypeOperation(input);
                Element output = getRequiredChildElement(WSDL_OUTPUT, operation);
                checkPortTypeOperation(output);
            } catch (RuntimeException e) {
                logError("WSDL 'portType' operation containing an error: " + formatDomSource(new DOMSource(operation)));
                throw e;
            }
        }
    }

    private void checkXrdDocumentation(Element element) {
        Element xrdTitle = getRequiredChildElement(XRD_TITLE, element);
        if (isBlank(xrdTitle.getTextContent())) {
            throw new IllegalStateException("XRD 'title' element without text content");
        }
        List<Element> xrdElements = getRequiredChildElements(new QName(XRD_NS.getNamespaceURI(), "*", ""), element);
        xrdElements.forEach(elem -> {
            if (!xrdDocumentationElements.contains(elem.getLocalName())) {
                throw new IllegalStateException(String.format("Invalid XRD element '%s'", elem.getLocalName()));
            }
        });
    }

    private void checkPortTypeOperation(Element element) {
        String qualifiedMessageValue = getRequiredAttributeValue("message", element);
        checkQualifiedAttributeForTargetNamespace(qualifiedMessageValue);
        String messageValue = qualifiedMessageValue.split(":")[1];
        if (wsdlV4.getWsdlMessages().stream()
                .noneMatch(elem -> getRequiredAttributeValue("name", elem).equals(messageValue))) {
            throw new IllegalStateException(String.format("Required abstract message by name '%s' not found", messageValue));
        }
    }
}
