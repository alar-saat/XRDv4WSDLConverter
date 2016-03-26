package ee.rmit.xrd.wsdl;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.List;

import static ee.rmit.xrd.utils.DomUtils.getDirectChildElementsOnly;
import static ee.rmit.xrd.utils.DomUtils.getRequiredAttributeValue;
import static ee.rmit.xrd.wsdl.QualifiedNames.*;

public class WsdlAppender {
    private Wsdl template;
    private List<String> bindingOperationNames = new ArrayList<>();
    private List<String> portTypeOperationNames = new ArrayList<>();
    private List<String> messageNames = new ArrayList<>();
    private List<String> schemaElementNames = new ArrayList<>();
    private List<String> schemaTypeNames = new ArrayList<>();
    private List<String> schemaAttributeNames = new ArrayList<>();

    public WsdlAppender(Wsdl template) {
        this.template = template;
        init();
    }

    private void init() {
        template.getWsdlBindingOperations().forEach(elem -> {
            String name = getRequiredAttributeValue("name", elem);
            if (bindingOperationNames.contains(name)) {
                throw new IllegalStateException(String.format("Binding operation name '%s' declared twice", name));
            }
            bindingOperationNames.add(name);
        });
        template.getWsdlPortTypeOperations().forEach(elem -> {
            String name = getRequiredAttributeValue("name", elem);
            if (portTypeOperationNames.contains(name)) {
                throw new IllegalStateException(String.format("PortType operation name '%s' declared twice", name));
            }
            portTypeOperationNames.add(name);
        });
        template.getWsdlMessages().forEach(elem -> {
            String name = getRequiredAttributeValue("name", elem);
            if (messageNames.contains(name)) {
                throw new IllegalStateException(String.format("Message name '%s' declared twice", name));
            }
            messageNames.add(name);
        });
        List<Element> schemaElements = getDirectChildElementsOnly(XSD_ELEMENT, template.getWsdlSchema());
        schemaElements.forEach(elem -> {
            String name = getRequiredAttributeValue("name", elem);
            if (schemaElementNames.contains(name)) {
                throw new IllegalStateException(String.format("Schema element name '%s' declared twice", name));
            }
            schemaElementNames.add(name);
        });
        List<Element> schemaTypes = getDirectChildElementsOnly(XSD_COMPLEX_TYPE, template.getWsdlSchema());
        schemaTypes.addAll(getDirectChildElementsOnly(XSD_SIMPLE_TYPE, template.getWsdlSchema()));
        schemaTypes.forEach(elem -> {
            String name = getRequiredAttributeValue("name", elem);
            if (schemaTypeNames.contains(name)) {
                throw new IllegalStateException(String.format("Schema type name '%s' declared twice", name));
            }
            schemaTypeNames.add(name);
        });
        List<Element> schemaAttributes = getDirectChildElementsOnly(XSD_ATTRIBUTE, template.getWsdlSchema());
        schemaAttributes.addAll(getDirectChildElementsOnly(XSD_ATTRIBUTE_GROUP, template.getWsdlSchema()));
        schemaAttributes.forEach(elem -> {
            String name = getRequiredAttributeValue("name", elem);
            if (schemaAttributeNames.contains(name)) {
                throw new IllegalStateException(String.format("Schema attribute name '%s' declared twice", name));
            }
            schemaAttributeNames.add(name);
        });
    }

    public void appendWsdl(Wsdl wsdl) {
        appendBindingOperations(wsdl);
        appendPortTypeOperations(wsdl);
        appendAbstractMessages(wsdl);
        appendSchemaElements(wsdl);
        appendSchemaTypes(wsdl);
        appendSchemaAttributes(wsdl);
    }

    private void appendBindingOperations(Wsdl wsdl) {
        wsdl.getWsdlBindingOperations().forEach(elem -> {
            String name = getRequiredAttributeValue("name", elem);
            if (!bindingOperationNames.contains(name)) {
                bindingOperationNames.add(name);
                Node bindingOperation = template.getWsdl().importNode(elem, true);
                template.getWsdlBinding().appendChild(bindingOperation);
                template.getWsdlBindingOperations().add((Element) bindingOperation);
            }
        });
    }

    private void appendPortTypeOperations(Wsdl wsdl) {
        wsdl.getWsdlPortTypeOperations().forEach(elem -> {
            String name = getRequiredAttributeValue("name", elem);
            if (!portTypeOperationNames.contains(name)) {
                portTypeOperationNames.add(name);
                Node portTypeOperation = template.getWsdl().importNode(elem, true);
                template.getWsdlPortType().appendChild(portTypeOperation);
                template.getWsdlPortTypeOperations().add((Element) portTypeOperation);
            }
        });
    }

    private void appendAbstractMessages(Wsdl wsdl) {
        wsdl.getWsdlMessages().forEach(elem -> {
            String name = getRequiredAttributeValue("name", elem);
            if (!messageNames.contains(name)) {
                messageNames.add(name);
                Node message = template.getWsdl().importNode(elem, true);
                template.getDocumentElement().insertBefore(message, template.getWsdlPortType());
                template.getWsdlMessages().add((Element) message);
            }
        });
    }

    private void appendSchemaElements(Wsdl wsdl) {
        List<Element> schemaElements = getDirectChildElementsOnly(XSD_ELEMENT, wsdl.getWsdlSchema());
        schemaElements.forEach(elem -> {
            String name = getRequiredAttributeValue("name", elem);
            if (!schemaElementNames.contains(name)) {
                schemaElementNames.add(name);
                importAndAppendSchemaObject(elem);
            }
        });
    }

    private void importAndAppendSchemaObject(Element element) {
        Node node = template.getWsdl().importNode(element, true);
        template.getWsdlSchema().appendChild(node);
    }

    private void appendSchemaTypes(Wsdl wsdl) {
        List<Element> schemaTypes = getDirectChildElementsOnly(XSD_COMPLEX_TYPE, wsdl.getWsdlSchema());
        schemaTypes.addAll(getDirectChildElementsOnly(XSD_SIMPLE_TYPE, wsdl.getWsdlSchema()));
        schemaTypes.forEach(elem -> {
            String name = getRequiredAttributeValue("name", elem);
            if (!schemaTypeNames.contains(name)) {
                schemaTypeNames.add(name);
                importAndAppendSchemaObject(elem);
            }
        });
    }

    private void appendSchemaAttributes(Wsdl wsdl) {
        List<Element> schemaAttributes = getDirectChildElementsOnly(XSD_ATTRIBUTE, wsdl.getWsdlSchema());
        schemaAttributes.addAll(getDirectChildElementsOnly(XSD_ATTRIBUTE_GROUP, wsdl.getWsdlSchema()));
        schemaAttributes.forEach(elem -> {
            String name = getRequiredAttributeValue("name", elem);
            if (!schemaAttributeNames.contains(name)) {
                schemaAttributeNames.add(name);
                importAndAppendSchemaObject(elem);
            }
        });
    }
}
