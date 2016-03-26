package ee.rmit.xrd.style;

import ee.rmit.xrd.wsdl.Wsdl;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static ee.rmit.xrd.utils.DomUtils.*;
import static ee.rmit.xrd.utils.DomUtils.getRequiredSchemaElementByNameValue;
import static ee.rmit.xrd.wsdl.QualifiedNames.*;

public class AbstractMessageStyleInspector {
    private Wsdl wsdl;

    public AbstractMessageStyleInspector(Wsdl wsdl) {
        this.wsdl = wsdl;
    }

    public void checkStyle() {
        for (Element operation : wsdl.getWsdlPortTypeOperations()) {
            String operationName = getRequiredAttributeValue("name", operation);
            handleInputOrOutput(operationName, getRequiredChildElement(WSDL_INPUT, operation));
            handleInputOrOutput(operationName + "Response", getRequiredChildElement(WSDL_OUTPUT, operation));
        }
    }

    private void handleInputOrOutput(String operationName, Element element) {
        String messageValue = getRequiredAttributeValue("message", element).split(":")[1];
        Element message = getRequiredAbstractMessageByAttributeName(messageValue);
        Element part = getMessagePartWithElementAttribute(message);
        Element elementInSchema = getElementInSchema(part);
        renameAttributesForInputOrOutput(operationName, element);
        renameAttributesForMessage(operationName, message);
        renameAttributesForMessagePart(operationName, part);
        renameAttributesForSchemaElement(operationName, elementInSchema);
    }

    private Element getRequiredAbstractMessageByAttributeName(String name) {
        Optional<Element> messageHolder =
                wsdl.getWsdlMessages().stream().filter(elem -> getRequiredAttributeValue("name", elem).equals(name)).findFirst();
        if (!messageHolder.isPresent()) {
            throw new IllegalStateException(String.format("Required abstract message by name '%s' not found", name));
        }
        return messageHolder.get();
    }

    private Element getMessagePartWithElementAttribute(Element element) {
        List<Element> parts = getRequiredChildElements(WSDL_PART, element);
        List<Element> filteredParts = new ArrayList<>();
        parts.forEach(elem -> {
            if (elem.hasAttribute("element")) {
                filteredParts.add(elem);
            }
        });
        if (filteredParts.size() != 1) {
            throw new IllegalStateException(String.format("Exactly one abstract message '%s' part was excpected, got %d"
                    , element.getLocalName(), filteredParts.size()));
        }
        return filteredParts.get(0);
    }

    private Element getElementInSchema(Element part) {
        String value = getRequiredAttributeValue("element", part).split(":")[1];
        return getRequiredSchemaElementByNameValue(value, wsdl.getWsdlSchema());
    }

    private void renameAttributesForInputOrOutput(String operationName, Element element) {
        Node attribute = getRequiredAttribute("message", element);
        attribute.setTextContent(String.format("%s:%s", wsdl.getTargetNamespacePrefix(), operationName));
    }

    private void renameAttributesForMessage(String operationName, Element element) {
        Node attribute = getRequiredAttribute("name", element);
        attribute.setTextContent(operationName);
    }

    private void renameAttributesForMessagePart(String operationName, Element element) {
        Node attributeName = getRequiredAttribute("name", element);
        attributeName.setTextContent("parameters");
        Node attributeElement = getRequiredAttribute("element", element);
        attributeElement.setTextContent(String.format("%s:%s", wsdl.getTargetNamespacePrefix(), operationName));
    }

    private void renameAttributesForSchemaElement(String operationName, Element element) {
        Node attribute = getRequiredAttribute("name", element);
        attribute.setTextContent(operationName);
    }
}
