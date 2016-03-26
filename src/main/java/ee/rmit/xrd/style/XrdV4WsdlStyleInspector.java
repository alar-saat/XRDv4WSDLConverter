package ee.rmit.xrd.style;

import ee.rmit.xrd.wsdl.Attribute;
import ee.rmit.xrd.wsdl.Wsdl;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static ee.rmit.xrd.utils.DomUtils.*;
import static ee.rmit.xrd.utils.LoggerUtils.logWarning;
import static ee.rmit.xrd.wsdl.QualifiedNames.*;

/**
 * <pre>
 * Always validate wsdl first {@link ee.rmit.xrd.validator.XrdV4WsdlValidator} and then check the style.
 * -----------------------------------------------------------------------------------------------------------
 * Namespace prefix check.
 * XRD SOAP header element check.
 * Abstract message name correlated with operation name check.
 * </pre>
 */
public class XrdV4WsdlStyleInspector {
    private Wsdl wsdlV4;
    private Wsdl wsdlV4Template;
    private List<QName> redundantNamespaces = new ArrayList<>();

    public XrdV4WsdlStyleInspector(Wsdl wsdlV4, Wsdl wsdlV4Template) {
        this.wsdlV4 = wsdlV4;
        this.wsdlV4Template = wsdlV4Template;
        checkTargetNamespacesEquality();
    }

    private void checkTargetNamespacesEquality() {
        if (!wsdlV4.getTargetNamespaceUri().equals(wsdlV4Template.getTargetNamespaceUri())) {
            throw new IllegalStateException(String.format("Different target namespace URI-s. Wsdl '%s', template '%s'"
                    , wsdlV4.getTargetNamespaceUri(), wsdlV4Template.getTargetNamespaceUri()));
        }
    }

    public void checkStyle() {
        addNamespacesFromTemplate();
        correctNamespacePrefixes();
        if (hasNamespace(MIME_NS.getNamespaceURI())) {
            setPrefixForNamespace(MIME_NS, wsdlV4.getDocumentElement());
        }
        //wsdl:types
        handleSchemas();
        //wsdl:message
        handleMessages();
        //wsdl:portType
        handlePortType();
        //wsdl:binding
        handleBinding();
        //wsdl:service
        handleService();
        removeRedundantNamespaces();
        wsdlV4.setTargetNamespace(wsdlV4Template.getTargetNamespace());
        wsdlV4.updateNamespacesList();

        SoapHeaderStyleInspector soapHeaderStyleInspector = new SoapHeaderStyleInspector(wsdlV4, wsdlV4Template);
        soapHeaderStyleInspector.checkStyle();

        AbstractMessageStyleInspector messageStyleInspector = new AbstractMessageStyleInspector(wsdlV4);
        messageStyleInspector.checkStyle();
    }

    private boolean hasNamespace(String namespace) {
        return wsdlV4.getNamespaces().stream().anyMatch(qName -> qName.getNamespaceURI().equals(namespace));
    }

    private void addNamespacesFromTemplate() {
        handleTargetNamespace();
        List<QName> nsWithDifferentPrefixes = new ArrayList<>();
        for (QName qName : wsdlV4.getNamespaces()) {
            QName qNameInTemplate = getIfSameNamespaceAndDifferentPrefixInTemplateOrNull(qName);
            if (qNameInTemplate != null) {
                nsWithDifferentPrefixes.add(qNameInTemplate);
                redundantNamespaces.add(qName);
            }
        }
        addNamespaces(wsdlV4.getDocumentElement(), nsWithDifferentPrefixes);
    }

    private void handleTargetNamespace() {
        if (wsdlV4.getTargetNamespacePrefix().equals(wsdlV4Template.getTargetNamespacePrefix())) {
            return;
        }
        addNamespace(wsdlV4.getDocumentElement(), new QName(wsdlV4.getTargetNamespaceUri(), wsdlV4Template.getTargetNamespacePrefix(), ""));
        wsdlV4.getDocumentElement().removeAttribute("xmlns:" + wsdlV4.getTargetNamespacePrefix());
    }

    private QName getIfSameNamespaceAndDifferentPrefixInTemplateOrNull(QName qName) {
        Optional<QName> qNameHolder =
                wsdlV4Template.getNamespaces().stream().filter(qNameInList -> qNameInList.getNamespaceURI().equals(qName.getNamespaceURI())
                        && !qNameInList.getLocalPart().equals(qName.getLocalPart())).findFirst();
        if (qNameHolder.isPresent()) {
            return qNameHolder.get();
        }
        return null;
    }

    private void correctNamespacePrefixes() {
        setPrefixForNamespace(WSDL_NS, wsdlV4.getDocumentElement());
        setPrefixForNamespace(SOAP_NS, wsdlV4.getDocumentElement());
        setPrefixForNamespace(XRD_NS, wsdlV4.getDocumentElement());
    }

    private void handleSchemas() {
        Element schema = wsdlV4.getWsdlSchema();
        if (hasPrefix(schema)) {
            setPrefixToNull(schema.getPrefix(), schema);
        }
        List<Attribute> attributes = new ArrayList<>();
        setAttributesToListThatHavePrefixedValue(schema, attributes, "type", "base", "ref");
        for (Attribute attribute : attributes) {
            QName schemaPrefix = getQualifiedNameByPrefixOrNull(attribute.getPrefix());
            if (schemaPrefix != null && schemaPrefix.getNamespaceURI().equals(XSD_NS.getNamespaceURI())) {
                //template schema uses default namespace only (no namespace prefix)
                attribute.setValue(attribute.getValue());
                continue;
            }
            setNewAttributeValuePrefixIfRequired(attribute);
        }
        redundantNamespaces.forEach(qName -> schema.removeAttribute("xmlns:" + qName.getLocalPart()));
    }

    private QName getQualifiedNameByPrefixOrNull(String prefix) {
        Optional<QName> name = wsdlV4.getNamespaces().stream().filter(qName -> qName.getLocalPart().equals(prefix)).findFirst();
        if (name.isPresent()) {
            return name.get();
        }
        return null;
    }

    private void setNewAttributeValuePrefixIfRequired(Attribute attribute) {
        if (wsdlV4.getTargetNamespace().isTargetNamespacePrefix(attribute.getPrefix())) {
            if (!attribute.getPrefix().equals(wsdlV4Template.getTargetNamespacePrefix())) {
                attribute.setValue(String.format("%s:%s", wsdlV4Template.getTargetNamespacePrefix(), attribute.getValue()));
            }
            return;
        }
        QName qName = getQualifiedNameByPrefixOrNull(attribute.getPrefix());
        if (qName == null) {
            logWarning(String.format("Cannot find namespace URI for prefix: %s. Removing prefix from value: %s"
                    , attribute.getPrefix(), attribute.getQualifiedValue()));
            attribute.setValue(attribute.getValue());
            return;
        }
        Optional<QName> qNameHolder =
                wsdlV4Template.getNamespaces().stream().filter(tempQName -> tempQName.getNamespaceURI().equals(qName.getNamespaceURI())).findFirst();
        if (qNameHolder.isPresent()) {
            QName templateQName = qNameHolder.get();
            if (!templateQName.getLocalPart().equals(attribute.getPrefix())) {
                attribute.setValue(String.format("%s:%s", templateQName.getLocalPart(), attribute.getValue()));
            }
        } else {
            logWarning(String.format("Cannot find namespace URI: %s in template. Removing prefix from value: %s"
                    , qName.getNamespaceURI(), attribute.getQualifiedValue()));
            attribute.setValue(attribute.getValue());
        }
    }

    private void handleMessages() {
        for (Element message : wsdlV4.getWsdlMessages()) {
            List<Attribute> attributes = new ArrayList<>();
            setAttributesToListThatHavePrefixedValue(message, attributes, "type", "element");
            attributes.forEach(this::setNewAttributeValuePrefixIfRequired);
        }
    }

    private void handlePortType() {
        List<Attribute> attributes = new ArrayList<>();
        setAttributesToListThatHavePrefixedValue(wsdlV4.getWsdlPortType(), attributes, "message");
        attributes.forEach(this::setNewAttributeValuePrefixIfRequired);
    }

    private void handleBinding() {
        List<Attribute> attributes = new ArrayList<>();
        setAttributesToListThatHavePrefixedValue(wsdlV4.getWsdlBinding(), attributes, "message", "type");
        attributes.forEach(this::setNewAttributeValuePrefixIfRequired);
    }

    private void handleService() {
        List<Attribute> attributes = new ArrayList<>();
        setAttributesToListThatHavePrefixedValue(wsdlV4.getWsdlService(), attributes, "binding");
        attributes.forEach(this::setNewAttributeValuePrefixIfRequired);
    }

    private void removeRedundantNamespaces() {
        redundantNamespaces.forEach(qName -> {
            if (qName.getLocalPart().equals("tns") && !qName.getNamespaceURI().equals(wsdlV4.getTargetNamespaceUri())) {
                Node node = getAttributeOrNull("xmlns:tns", wsdlV4.getDocumentElement());
                if (node != null) {
                    node.setTextContent(wsdlV4.getTargetNamespaceUri());
                }
            } else {
                wsdlV4.getDocumentElement().removeAttribute("xmlns:" + qName.getLocalPart());
            }
        });
        Node node = getAttributeOrNull("xmlns", wsdlV4.getDocumentElement());
        if (node != null) {
            wsdlV4.getDocumentElement().removeAttribute(node.getLocalName());
        }
    }
}
