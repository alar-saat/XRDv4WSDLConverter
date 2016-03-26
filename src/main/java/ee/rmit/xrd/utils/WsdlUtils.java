package ee.rmit.xrd.utils;

import ee.rmit.xrd.wsdl.Wsdl;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static ee.rmit.xrd.utils.DomUtils.*;
import static ee.rmit.xrd.wsdl.QualifiedNames.WSDL_PART;

public final class WsdlUtils {
    private WsdlUtils() {
    }

    public static Wsdl copyWsdlDeep(Wsdl wsdl) {
        try {
            DocumentBuilder builder = createBuilder();
            Document document = builder.newDocument();
            Node node = document.importNode(wsdl.getDocumentElement(), true);
            document.appendChild(node);
            return new Wsdl(document);
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException("Cannot copy WSDL", e);
        }
    }

    public static Wsdl createTemplate(Wsdl wsdl) {
        Wsdl template = copyWsdlDeep(wsdl);
        template.removeAllOperations();
        template.removeAllMessages();
        template.removeAllSchemaElements();
        return template;
    }

    public static boolean hasXrdSoapHeaderElement(String name, Element xrdHeaderMessage, List<Element> soapHeaderElements) {
        List<Element> xrdHeaderMessageParts = getRequiredChildElements(WSDL_PART, xrdHeaderMessage);
        List<String> headerElementPartValues = new ArrayList<>();
        soapHeaderElements.forEach(elem -> headerElementPartValues.add(getRequiredAttributeValue("part", elem)));

        return headerElementPartValues.stream().anyMatch(part -> {
            Optional<Element> messagePartHolder =
                xrdHeaderMessageParts.stream().filter(elem -> getRequiredAttributeValue("name", elem).equals(part)).findFirst();
            if (messagePartHolder.isPresent()) {
                Element messagePart = messagePartHolder.get();
                String elementValue = getRequiredAttributeValue("element", messagePart);
                return elementValue.split(":")[1].equals(name);
            }
            throw new IllegalStateException(String.format("Required XRD header element by part name '%s' not found", part));
        });
    }
}
