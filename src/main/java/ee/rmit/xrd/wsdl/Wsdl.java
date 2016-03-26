package ee.rmit.xrd.wsdl;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;

import static ee.rmit.xrd.utils.DomUtils.*;
import static ee.rmit.xrd.wsdl.QualifiedNames.*;

public class Wsdl {
    private Document wsdl;
    private Element documentElement;
    private Element wsdlTypes;
    private Element wsdlSchema;
    private List<Element> wsdlMessages;
    private Element wsdlPortType;
    private List<Element> wsdlPortTypeOperations;
    private Element wsdlBinding;
    private List<Element> wsdlBindingOperations;
    private Element wsdlService;
    private List<QName> namespaces;
    private TargetNamespace targetNamespace;

    public Wsdl(String wsdl) throws ParserConfigurationException, SAXException, IOException {
        this(Paths.get(wsdl));
    }

    public Wsdl(Path wsdl) throws ParserConfigurationException, SAXException, IOException {
        this(parseDocument(wsdl));
    }

    public Wsdl(Document wsdl) {
        if (wsdl == null) {
            throw new IllegalStateException("WSDL document undefined");
        }
        this.wsdl = wsdl;
        initWsdlParts();
    }

    private void initWsdlParts() {
        documentElement = wsdl.getDocumentElement();
        checkWsdlRootElement();
        wsdlTypes = getRequiredChildElement(WSDL_TYPES, documentElement);
        wsdlSchema = getRequiredChildElement(WSDL_SCHEMA, documentElement);
        wsdlMessages = getRequiredChildElements(WSDL_MESSAGE, documentElement);
        wsdlPortType = getRequiredChildElement(WSDL_PORT_TYPE, documentElement);
        //optional
        wsdlPortTypeOperations = getChildElements(WSDL_OPERATION, wsdlPortType);
        wsdlBinding = getRequiredChildElement(WSDL_BINDING, documentElement);
        //optional
        wsdlBindingOperations = getChildElements(WSDL_OPERATION, wsdlBinding);
        wsdlService = getRequiredChildElement(WSDL_SERVICE, documentElement);
        namespaces = getAllNamespaceDeclarations(documentElement);
        targetNamespace = getRequiredTargetNamespace(documentElement);
        addNamespacesDeclaredInSchema();
    }

    private void checkWsdlRootElement() {
        if (documentElement.getLocalName().equals(WSDL_DEFINITIONS.getLocalPart())
                && documentElement.getNamespaceURI().equals(WSDL_DEFINITIONS.getNamespaceURI())) {
            return;
        }
        throw new IllegalStateException(String.format("Required qualified element {%s}%s not found"
                , WSDL_DEFINITIONS.getNamespaceURI(), WSDL_DEFINITIONS.getLocalPart()));
    }

    private void addNamespacesDeclaredInSchema() {
        Element types = getRequiredChildElement(QualifiedNames.WSDL_TYPES, documentElement);
        List<Element> schemas = getChildElements(QualifiedNames.WSDL_SCHEMA, types);
        for (Element schema : schemas) {
            List<QName> schemaNamespaces = getAllNamespaceDeclarations(schema);
            schemaNamespaces.stream().filter(schemaQName -> !hasQName(schemaQName)).forEach(namespaces::add);
        }
    }

    public boolean hasQName(QName qName) {
        return namespaces.stream().anyMatch(qNameInList ->
                qName.getNamespaceURI().equals(qNameInList.getNamespaceURI()) && qName.getLocalPart().equals(qNameInList.getLocalPart()));
    }

    public Document getWsdl() {
        return wsdl;
    }

    public Element getDocumentElement() {
        return documentElement;
    }

    public Element getWsdlTypes() {
        return wsdlTypes;
    }

    public Element getWsdlSchema() {
        return wsdlSchema;
    }

    public List<Element> getWsdlMessages() {
        return wsdlMessages;
    }

    public Element getWsdlPortType() {
        return wsdlPortType;
    }

    public List<Element> getWsdlPortTypeOperations() {
        return wsdlPortTypeOperations;
    }

    public Element getWsdlBinding() {
        return wsdlBinding;
    }

    public List<Element> getWsdlBindingOperations() {
        return wsdlBindingOperations;
    }

    public Element getWsdlService() {
        return wsdlService;
    }

    public List<QName> getNamespaces() {
        return namespaces;
    }

    public TargetNamespace getTargetNamespace() {
        return targetNamespace;
    }

    public String getTargetNamespaceUri() {
        return targetNamespace.getTargetNamespace();
    }

    public String getTargetNamespacePrefix() {
        return targetNamespace.getTargetNamespacePrefix();
    }

    public void setTargetNamespace(TargetNamespace targetNamespace) {
        this.targetNamespace = targetNamespace;
    }

    public void updateNamespacesList() {
        namespaces = getAllNamespaceDeclarations(documentElement);
    }

    public void removeWsdlMessage(String name) {
        for (Iterator<Element> it = wsdlMessages.iterator(); it.hasNext(); ) {
            String messageName = getRequiredAttributeValue("name", it.next());
            if (messageName.equals(name)) {
                it.remove();
                break;
            }
        }
    }

    public void removeAllOperations() {
        wsdlBindingOperations.forEach(elem -> wsdlBinding.removeChild(elem));
        wsdlBindingOperations.clear();
        wsdlPortTypeOperations.forEach(elem -> wsdlPortType.removeChild(elem));
        wsdlPortTypeOperations.clear();
    }

    public void removeAllMessages() {
        wsdlMessages.forEach(elem -> documentElement.removeChild(elem));
        wsdlMessages.clear();
    }

    public void removeAllSchemaElements() {
        List<Element> elements = getDirectChildElementsOnly(XSD_ELEMENT, wsdlSchema);
        elements.forEach(elem -> wsdlSchema.removeChild(elem));
        elements = getDirectChildElementsOnly(XSD_COMPLEX_TYPE, wsdlSchema);
        elements.forEach(elem -> wsdlSchema.removeChild(elem));
        elements = getDirectChildElementsOnly(XSD_SIMPLE_TYPE, wsdlSchema);
        elements.forEach(elem -> wsdlSchema.removeChild(elem));
        elements = getDirectChildElementsOnly(XSD_ATTRIBUTE, wsdlSchema);
        elements.forEach(elem -> wsdlSchema.removeChild(elem));
        elements = getDirectChildElementsOnly(XSD_ATTRIBUTE_GROUP, wsdlSchema);
        elements.forEach(elem -> wsdlSchema.removeChild(elem));
    }

    public boolean containsBindingOperation(Element element) {
        return containsBindingOperation(getRequiredAttributeValue("name", element));
    }

    public boolean containsBindingOperation(String name) {
        return wsdlBindingOperations.stream().anyMatch(elem -> getRequiredAttributeValue("name", elem).equals(name));
    }

    public boolean containsPortTypeOperation(Element element) {
        return containsPortTypeOperation(getRequiredAttributeValue("name", element));
    }

    public boolean containsPortTypeOperation(String name) {
        return wsdlPortTypeOperations.stream().anyMatch(elem -> getRequiredAttributeValue("name", elem).equals(name));
    }

    public boolean containsMessage(Element element) {
        return containsMessage(getRequiredAttributeValue("name", element));
    }

    public boolean containsMessage(String name) {
        return wsdlMessages.stream().anyMatch(elem -> getRequiredAttributeValue("name", elem).equals(name));
    }

    public boolean containsSchemaObject(Element element) {
        String name = getRequiredAttributeValue("name", element);
        String localName = element.getLocalName();
        switch (localName) {
            case "element":
                return getSchemaElementByNameValueOrNull(name, wsdlSchema) != null;
            case "simpleType":
                return getSchemaTypeByNameValueOrNull(name, wsdlSchema) != null;
            case "complexType":
                return getSchemaTypeByNameValueOrNull(name, wsdlSchema) != null;
            case "attribute":
                return getSchemaAttributeByNameValueOrNull(name, wsdlSchema) != null;
            case "attributeGroup":
                return getSchemaAttributeByNameValueOrNull(name, wsdlSchema) != null;
            default:
                return false;
        }
    }
}
