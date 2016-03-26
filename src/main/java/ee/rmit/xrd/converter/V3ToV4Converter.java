package ee.rmit.xrd.converter;

import ee.rmit.xrd.wsdl.MessagePart;
import ee.rmit.xrd.wsdl.Wsdl;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.List;
import java.util.Optional;

import static ee.rmit.xrd.utils.DomUtils.*;
import static ee.rmit.xrd.utils.LoggerUtils.logInfo;
import static ee.rmit.xrd.wsdl.QualifiedNames.XSD_ELEMENT;

public class V3ToV4Converter extends AbstractConverter {
    private Wsdl wsdlV4Template;

    public V3ToV4Converter(Wsdl wsdlV3, Wsdl wsdlV4Template) {
        super(wsdlV3);
        checkV3Version();
        this.wsdlV4Template = wsdlV4Template;
    }

    private void checkV3Version() {
        if (!isXrdV3()) {
            throw new IllegalStateException(String.format("Required XRD v3 namespaces '%s' or '%s' not found", XRDV3_1_NS, XRDV3_0_NS));
        }
    }

    public void convert() {
        addBindingOperation(wsdlV4Template);
        addPortTypeOperation(wsdlV4Template);
        addAbstractMessages(wsdlV4Template);
        addSchemaObjects(false);
    }

    @Override
    public void addSchemaObjects(boolean useWrapperElements) {
        //we expect that v3 is document/literal wrapped
        Element schema = wsdl.getWsdlSchema();
        List<Element> schemaElements = getDirectChildElementsOnly(XSD_ELEMENT, schema);
        checkInputSchemaElementName(schemaElements);
        checkOutputSchemaElementName(schemaElements);
        importAllSchemaObjects(wsdlV4Template);
    }

    private void checkInputSchemaElementName(List<Element> schemaElements) {
        MessagePart elementPart = getRequiredMessagePartWithElement(inputMessageParts);
        String name = elementPart.getElement().getValue();
        if (!name.equals(operationName)) {
            Optional<Element> schemaElementHolder =
                    schemaElements.stream().filter(elem -> getRequiredAttributeValue("name", elem).equals(name)).findFirst();
            if (!schemaElementHolder.isPresent()) {
                throw new IllegalStateException(String.format("Required schema element by name '%s' not found", name));
            }
            Element schemaElement = schemaElementHolder.get();
            Node attribute = getRequiredAttribute("name", schemaElement);
            attribute.setTextContent(operationName);
        }
    }

    private MessagePart getRequiredMessagePartWithElement(List<MessagePart> parts) {
        Optional<MessagePart> partHolder = parts.stream().filter(MessagePart::isElement).findFirst();
        if (partHolder.isPresent()) {
            return partHolder.get();
        }
        throw new IllegalStateException("XRD v3 and no message part with attribute 'element'");
    }

    private void checkOutputSchemaElementName(List<Element> schemaElements) {
        MessagePart elementPart = getRequiredMessagePartWithElement(outputMessageParts);
        String name = elementPart.getElement().getValue();
        String validResponseName = operationName + "Response";
        if (!name.equals(validResponseName)) {
            logInfo(String.format("Output schema element name '%s' will be changed to '%s'", name, validResponseName));
            Optional<Element> schemaElementHolder =
                    schemaElements.stream().filter(elem -> getRequiredAttributeValue("name", elem).equals(name)).findFirst();
            if (!schemaElementHolder.isPresent()) {
                throw new IllegalStateException(String.format("Required schema element by name '%s' not found", name));
            }
            Element schemaElement = schemaElementHolder.get();
            Node attribute = getRequiredAttribute("name", schemaElement);
            attribute.setTextContent(validResponseName);
        }
    }
}
