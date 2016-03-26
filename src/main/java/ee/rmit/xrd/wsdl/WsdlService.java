package ee.rmit.xrd.wsdl;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static ee.rmit.xrd.utils.DomUtils.*;
import static ee.rmit.xrd.utils.StringUtils.isBlank;
import static ee.rmit.xrd.utils.StringUtils.isNotBlank;
import static ee.rmit.xrd.wsdl.QualifiedNames.*;

public class WsdlService {
    private String service;
    private Wsdl wsdl;
    private List<Element> schemaObjects = new ArrayList<>();
    private List<Element> wsdlMessages = new ArrayList<>();
    private Element portTypeOperation;
    private Element bindingOperation;

    public WsdlService(String service, Wsdl wsdl) {
        this.service = service;
        this.wsdl = wsdl;
        init();
    }

    public String getService() {
        return service;
    }

    public List<Element> getSchemaObjects() {
        return schemaObjects;
    }

    public List<Element> getWsdlMessages() {
        return wsdlMessages;
    }

    public Element getPortTypeOperation() {
        return portTypeOperation;
    }

    public Element getBindingOperation() {
        return bindingOperation;
    }

    private void init() {
        initBindingOperation();
        initPortTypeOperation();
        initAbstractMessages();
        initSchemaElements();
    }

    private void initBindingOperation() {
        Optional<Element> holder =
                wsdl.getWsdlBindingOperations().stream().filter(elem -> getRequiredAttributeValue("name", elem).equals(service)).findFirst();
        if (!holder.isPresent()) {
            throw new IllegalStateException(String.format("Required binding operation '%s' not found", service));
        }
        bindingOperation = holder.get();
    }

    private void initPortTypeOperation() {
        Optional<Element> holder =
                wsdl.getWsdlPortTypeOperations().stream().filter(elem -> getRequiredAttributeValue("name", elem).equals(service)).findFirst();
        if (!holder.isPresent()) {
            throw new IllegalStateException(String.format("Required portType operation '%s' not found", service));
        }
        portTypeOperation = holder.get();
    }

    private void initAbstractMessages() {
        Element input = getRequiredChildElement(WSDL_INPUT, portTypeOperation);
        String attrValue = getRequiredAttributeValue("message", input);
        Element message = getRequiredWsdlAbstractMessageByNameValue(getUnqualifiedName(attrValue), wsdl.getDocumentElement());
        wsdlMessages.add(message);
        Element output = getRequiredChildElement(WSDL_OUTPUT, portTypeOperation);
        attrValue = getRequiredAttributeValue("message", output);
        message = getRequiredWsdlAbstractMessageByNameValue(getUnqualifiedName(attrValue), wsdl.getDocumentElement());
        wsdlMessages.add(message);
        //optional
        Element fault = getChildElementOrNull(WSDL_FAULT, portTypeOperation);
        if (fault != null) {
            attrValue = getRequiredAttributeValue("message", fault);
            message = getRequiredWsdlAbstractMessageByNameValue(getUnqualifiedName(attrValue), wsdl.getDocumentElement());
            wsdlMessages.add(message);
        }
        addAbstractMessagesDefinedInSoapHeaders();
    }

    private String getUnqualifiedName(String qualified) {
        String[] splitted = qualified.split(":");
        if (splitted.length == 2) {
            return splitted[1];
        }
        return qualified;
    }

    private void addAbstractMessagesDefinedInSoapHeaders() {
        List<Element> elements = getChildElements(SOAP_HEADER, bindingOperation);
        elements.forEach(elem -> {
            String attrValue = getRequiredAttributeValue("message", elem);
            Element message = getRequiredWsdlAbstractMessageByNameValue(getUnqualifiedName(attrValue), wsdl.getDocumentElement());
            if (!wsdlMessages.contains(message)) {
                wsdlMessages.add(message);
            }
        });
    }

    private void initSchemaElements() {
        wsdlMessages.forEach(elem -> {
            List<Element> parts = getRequiredChildElements(WSDL_PART, elem);
            parts.forEach(part -> {
                WsdlMessagePart messagePart = getWsdlMessagePart(part);
                if (messagePart.getPrefix().equals(wsdl.getTargetNamespacePrefix())) {
                    Element element = null;
                    if (messagePart.getAttributeType().equals(PartAttributeType.element)) {
                        element = getRequiredSchemaElementByNameValue(messagePart.getAttrValue(), wsdl.getWsdlSchema());
                    } else {
                        element = getRequiredSchemaTypeByNameValue(messagePart.getAttrValue(), wsdl.getWsdlSchema());
                    }
                    addSchemaObject(element);
                    addSchemaObjectDependencies(element);
                }
            });
        });
    }

    private WsdlMessagePart getWsdlMessagePart(Element element) {
        Node attribute = getAttributeOrNull("element", element);
        if (attribute != null) {
            return new WsdlMessagePart(PartAttributeType.element, new Attribute(attribute));
        }
        attribute = getAttributeOrNull("type", element);
        if (attribute != null) {
            return new WsdlMessagePart(PartAttributeType.type, new Attribute(attribute));
        }
        throw new IllegalStateException(String.format("Abstract message part '%s' without required attribute: 'element' or 'type'"
                , element.getAttribute("name")));
    }

    private boolean addSchemaObject(Element element) {
        if (schemaObjects.contains(element)) {
            return false;
        }
        schemaObjects.add(element);
        return true;
    }

    private void addSchemaObjectDependencies(Element element) {
        if (isElementOrAttributeWithTypeAttributeForTargetNamespace(element)) {
            String typeAttrValue = getRequiredAttributeValue("type", element).split(":")[1];
            Element typeRef = getRequiredSchemaTypeByNameValue(typeAttrValue, wsdl.getWsdlSchema());
            if (addSchemaObject(typeRef)) {
                addSchemaObjectDependencies(typeRef);
            }
        } else if (isElementWithRefAttributeForTargetNamespace(element)) {
            String refAttrValue = getRequiredAttributeValue("ref", element).split(":")[1];
            Element elementRef = getRequiredSchemaElementByNameValue(refAttrValue, wsdl.getWsdlSchema());
            if (addSchemaObject(elementRef)) {
                addSchemaObjectDependencies(elementRef);
            }
        } else if (isAttributeWithRefForTargetNamespace(element)) {
            String refAttrValue = getRequiredAttributeValue("ref", element).split(":")[1];
            Element elementRef = getRequiredSchemaAttributeByNameValue(refAttrValue, wsdl.getWsdlSchema());
            if (addSchemaObject(elementRef)) {
                addSchemaObjectDependencies(elementRef);
            }
        } else if (isExtensionOrRestrictionWithBaseAttributeForTargetNamespace(element)) {
            String baseAttrValue = getRequiredAttributeValue("base", element).split(":")[1];
            Element elementBase = getRequiredSchemaTypeByNameValue(baseAttrValue, wsdl.getWsdlSchema());
            if (addSchemaObject(elementBase)) {
                addSchemaObjectDependencies(elementBase);
            }
        } else {
            List<Element> elements = getChildElements(XSD_ELEMENT, element);
            elements.addAll(getChildElements(XSD_ATTRIBUTE, element));
            elements.addAll(getChildElements(XSD_EXTENSION, element));
            elements.addAll(getChildElements(XSD_RESTRICTION, element));
            elements.forEach(elem -> {
                String name = getSchemaDependencyNameOrNull(elem);
                if (isQualifiedNameForTargetNamespace(name)) {
                    addSchemaObjectDependencies(elem);
                }
            });
        }
    }

    private boolean isElementOrAttributeWithTypeAttributeForTargetNamespace(Element element) {
        if (element.getLocalName().equals("element") || element.getLocalName().equals("attribute")) {
            String type = element.getAttribute("type");
            if (isQualifiedNameForTargetNamespace(type)) {
                return true;
            }
        }
        return false;
    }

    private boolean isQualifiedNameForTargetNamespace(String qualified) {
        if (isBlank(qualified)) {
            return false;
        }
        String[] splitted = qualified.split(":");
        return splitted[0].equals(wsdl.getTargetNamespacePrefix());
    }

    private boolean isElementWithRefAttributeForTargetNamespace(Element element) {
        if (element.getLocalName().equals("element")) {
            String type = element.getAttribute("ref");
            if (isQualifiedNameForTargetNamespace(type)) {
                return true;
            }
        }
        return false;
    }

    private boolean isAttributeWithRefForTargetNamespace(Element element) {
        if (element.getLocalName().equals("attribute")) {
            String type = element.getAttribute("ref");
            if (isQualifiedNameForTargetNamespace(type)) {
                return true;
            }
        }
        return false;
    }

    private boolean isExtensionOrRestrictionWithBaseAttributeForTargetNamespace(Element element) {
        if (element.getLocalName().equals("extension") || element.getLocalName().equals("restriction")) {
            String type = element.getAttribute("base");
            if (isQualifiedNameForTargetNamespace(type)) {
                return true;
            }
        }
        return false;
    }

    private String getSchemaDependencyNameOrNull(Element element) {
        if (!element.hasAttributes()) {
            return null;
        }
        String name = element.getAttribute("type");
        if (isNotBlank(name)) {
            return name;
        }
        name = element.getAttribute("ref");
        if (isNotBlank(name)) {
            return name;
        }
        name = element.getAttribute("base");
        if (isNotBlank(name)) {
            return name;
        }
        return null;
    }
}
