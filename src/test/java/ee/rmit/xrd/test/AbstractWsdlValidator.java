package ee.rmit.xrd.test;

import ee.rmit.xrd.wsdl.Wsdl;
import org.w3c.dom.Element;

import java.util.List;

import static ee.rmit.xrd.utils.DomUtils.getRequiredAttributeValue;
import static ee.rmit.xrd.utils.DomUtils.getRequiredChildElements;
import static ee.rmit.xrd.wsdl.QualifiedNames.WSDL_OPERATION;

public class AbstractWsdlValidator {

    protected void validateSchemaObjectByName(String name, List<Element> elements) {
        if (elements.stream().noneMatch(elem -> getRequiredAttributeValue("name", elem).equals(name))) {
            throw new IllegalArgumentException(String.format("Required schema object '%s' not found", name));
        }
    }

    protected void validateMessageByName(String name, List<Element> elements) {
        if (elements.stream().noneMatch(elem -> getRequiredAttributeValue("name", elem).equals(name))) {
            throw new IllegalArgumentException(String.format("Required abstract message '%s' not found", name));
        }
    }

    protected void validateMimePartByName(String name, List<Element> elements) {
        if (elements.stream().noneMatch(elem -> getRequiredAttributeValue("name", elem).equals(name))) {
            throw new IllegalArgumentException(String.format("Required mime part '%s' not found", name));
        }
    }

    protected void validatePortTypeOperationName(String name, Wsdl wsdl) {
        List<Element> portTypeOperations = getRequiredChildElements(WSDL_OPERATION, wsdl.getWsdlPortType());
        if (portTypeOperations.stream().noneMatch(elem -> getRequiredAttributeValue("name", elem).equals(name))) {
            throw new IllegalArgumentException(String.format("Required portType operation '%s' not found", name));
        }
    }

    protected void validateBindingOperationName(String name, Wsdl wsdl) {
        List<Element> bindingOperations = getRequiredChildElements(WSDL_OPERATION, wsdl.getWsdlBinding());
        if (bindingOperations.stream().noneMatch(elem -> getRequiredAttributeValue("name", elem).equals(name))) {
            throw new IllegalArgumentException(String.format("Required binding operation '%s' not found", name));
        }
    }
}
