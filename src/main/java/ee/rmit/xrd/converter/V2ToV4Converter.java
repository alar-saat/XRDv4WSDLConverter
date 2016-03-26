package ee.rmit.xrd.converter;

import ee.rmit.xrd.wsdl.MessagePart;
import ee.rmit.xrd.wsdl.Wsdl;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.List;
import java.util.Optional;

import static ee.rmit.xrd.utils.StringUtils.isBlank;
import static ee.rmit.xrd.wsdl.QualifiedNames.XSD_NS;

public class V2ToV4Converter extends AbstractConverter {
    private Wsdl wsdlV4Template;

    public V2ToV4Converter(Wsdl wsdlV2, Wsdl wsdlV4Template) {
        super(wsdlV2);
        checkV2Version();
        this.wsdlV4Template = wsdlV4Template;
    }

    private void checkV2Version() {
        if (!isXrdV2()) {
            throw new IllegalStateException(String.format("Required XRD v2 namespaces '%s' not found", XRDV2_NS));
        }
    }

    public void convert(boolean useWrapperElements) {
        addBindingOperation(wsdlV4Template);
        addPortTypeOperation(wsdlV4Template);
        addAbstractMessages(wsdlV4Template);
        addSchemaObjects(useWrapperElements);
    }

    @Override
    public void addSchemaObjects(boolean useWrapperElements) {
        //we expect that v2 is rpc/encoded
        createInputSchemaElement(useWrapperElements);
        createOutputSchemaElement(useWrapperElements);
        if (!faultMessageParts.isEmpty()) {
            createFaultSchemaElement();
        }
        importAllSchemaObjects(wsdlV4Template);
    }

    private void createInputSchemaElement(boolean useWrapperElements) {
        MessagePart keha = getRequiredMessagePartWithTypeAndName("keha", inputMessageParts);
        String typeReference = isSchemaReference(keha) ? keha.getType().getValue() : keha.getType().getQualifiedValue();
        String prefix = wsdl.getWsdlSchema().getPrefix();
        Document doc = wsdl.getWsdl();
        Element element = doc.createElementNS(XSD_NS.getNamespaceURI(), createQualifiedName(prefix, "element"));
        element.setAttribute("name", operationName);
        if (useWrapperElements) {
            Element complexType = doc.createElementNS(XSD_NS.getNamespaceURI(), createQualifiedName(prefix, "complexType"));
            Element sequence = doc.createElementNS(XSD_NS.getNamespaceURI(), createQualifiedName(prefix, "sequence"));
            Element kehaElement = doc.createElementNS(XSD_NS.getNamespaceURI(), createQualifiedName(prefix, "element"));
            kehaElement.setAttribute("name", "keha");
            kehaElement.setAttribute("type", typeReference);
            sequence.appendChild(kehaElement);
            complexType.appendChild(sequence);
            element.appendChild(complexType);
        } else {
            element.setAttribute("type", typeReference);
        }
        wsdl.getWsdlSchema().appendChild(element);
    }

    private boolean isSchemaReference(MessagePart part) {
        if (part.getType() == null || part.getType().getPrefix() == null) {
            return false;
        }
        String prefix = part.getType().getPrefix();
        return wsdl.getNamespaces().stream().anyMatch(qName ->
                qName.getLocalPart().equals(prefix) && qName.getNamespaceURI().equals(XSD_NS.getNamespaceURI()));
    }

    private MessagePart getRequiredMessagePartWithTypeAndName(String name, List<MessagePart> parts) {
        Optional<MessagePart> partHolder = parts.stream().filter(part ->
                !part.isElement() && name.equals(part.getName().getValue())).findFirst();
        if (partHolder.isPresent()) {
            return partHolder.get();
        }
        throw new IllegalStateException(String.format("XRD v2 and no message part with attributes 'type' and 'name' = '%s'", name));
    }

    private String createQualifiedName(String prefix, String name) {
        if (isBlank(prefix)) {
            return name;
        }
        return String.format("%s:%s", prefix, name);
    }

    private void createOutputSchemaElement(boolean useWrapperElements) {
        MessagePart paring = getRequiredMessagePartWithTypeAndName("paring", outputMessageParts);
        MessagePart keha = getRequiredMessagePartWithTypeAndName("keha", outputMessageParts);
        String paringTypeReference = isSchemaReference(paring) ? paring.getType().getValue() : paring.getType().getQualifiedValue();
        String kehaTypeReference = isSchemaReference(keha) ? keha.getType().getValue() : keha.getType().getQualifiedValue();
        String prefix = wsdl.getWsdlSchema().getPrefix();
        Document doc = wsdl.getWsdl();
        Element element = doc.createElementNS(XSD_NS.getNamespaceURI(), createQualifiedName(prefix, "element"));
        element.setAttribute("name", operationName + "Response");
        if (useWrapperElements) {
            Element complexType = doc.createElementNS(XSD_NS.getNamespaceURI(), createQualifiedName(prefix, "complexType"));
            Element sequence = doc.createElementNS(XSD_NS.getNamespaceURI(), createQualifiedName(prefix, "sequence"));
            Element paringElement = doc.createElementNS(XSD_NS.getNamespaceURI(), createQualifiedName(prefix, "element"));
            paringElement.setAttribute("name", "paring");
            paringElement.setAttribute("type", paringTypeReference);
            Element kehaElement = doc.createElementNS(XSD_NS.getNamespaceURI(), createQualifiedName(prefix, "element"));
            kehaElement.setAttribute("name", "keha");
            kehaElement.setAttribute("type", kehaTypeReference);
            sequence.appendChild(paringElement);
            sequence.appendChild(kehaElement);
            complexType.appendChild(sequence);
            element.appendChild(complexType);
        } else {
            element.setAttribute("type", kehaTypeReference);
        }
        wsdl.getWsdlSchema().appendChild(element);
    }

    private void createFaultSchemaElement() {
        Document doc = wsdl.getWsdl();
        String prefix = wsdl.getWsdlSchema().getPrefix();
        faultMessageParts.forEach(part -> {
            if (!part.isElement()) {
                String typeReference = isSchemaReference(part) ? part.getType().getValue() : part.getType().getQualifiedValue();
                Element element = doc.createElementNS(XSD_NS.getNamespaceURI(), createQualifiedName(prefix, "element"));
                element.setAttribute("name", part.getName().getValue());
                element.setAttribute("type", typeReference);
                wsdl.getWsdlSchema().appendChild(element);
            }
        });
    }
}
