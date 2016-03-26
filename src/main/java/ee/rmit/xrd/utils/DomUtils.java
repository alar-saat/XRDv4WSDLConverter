package ee.rmit.xrd.utils;

import ee.rmit.xrd.wsdl.Attribute;
import ee.rmit.xrd.wsdl.QualifiedNames;
import ee.rmit.xrd.wsdl.TargetNamespace;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static ee.rmit.xrd.utils.FileUtils.readFromFile;
import static ee.rmit.xrd.utils.FileUtils.readFromUrl;
import static ee.rmit.xrd.utils.StringUtils.isNotBlank;
import static ee.rmit.xrd.wsdl.QualifiedNames.*;

public final class DomUtils {
    private static final String REMOVE_WHITESPACE_XSL = "<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">" +
            "<xsl:output method=\"xml\" omit-xml-declaration=\"no\" />" +
            "<xsl:strip-space elements=\"*\" />" +
            "<xsl:template match=\"@*|node()\">" +
            "<xsl:copy>" +
            "<xsl:apply-templates select=\"@*|node()\" />" +
            "</xsl:copy>" +
            "</xsl:template>" +
            "</xsl:stylesheet>";

    private DomUtils() {
    }

    public static String formatDomSource(DOMSource source) {
        return formatDomSource(source, true);
    }

    public static String formatDomSource(DOMSource source, boolean omitXmlDeclaration) {
        try {
            StreamResult result = new StreamResult(new StringWriter());
            serializeFormattedDomSource(source, result, omitXmlDeclaration);
            return result.getWriter().toString();
        } catch (TransformerException e) {
            return "<error>" + e.getMessage() + "</error>";
        }
    }

    public static void serializeFormattedDomSource(DOMSource source, StreamResult result) throws TransformerException {
        serializeFormattedDomSource(source, result, true);
    }

    public static void serializeFormattedDomSource(DOMSource source, StreamResult result, boolean omitXmlDeclaration) throws TransformerException {
        Transformer transformer =
                TransformerFactory.newInstance().newTransformer(new StreamSource(new StringReader(REMOVE_WHITESPACE_XSL)));
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        transformer.setOutputProperty(OutputKeys.ENCODING, "utf-8");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, (omitXmlDeclaration ? "yes" : "no"));
        transformer.transform(source, result);
    }

    public static String getDocumentBuilderFactoryClassName() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        return factory.getClass().getName();
    }

    public static DocumentBuilder createBuilder() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setIgnoringComments(true);
        factory.setNamespaceAware(true);
        return factory.newDocumentBuilder();
    }

    public static Document parseDocument(Path wsdlFile) throws IOException, ParserConfigurationException, SAXException {
        return createBuilder().parse(new ByteArrayInputStream(readFromFile(wsdlFile)));
    }

    public static Document parseDocument(URL url) throws IOException, ParserConfigurationException, SAXException {
        return createBuilder().parse(new ByteArrayInputStream(readFromUrl(url)));
    }

    public static List<Element> getDirectChildElementsOnly(QName qName, Element element) {
        List<Element> elements = new ArrayList<>();
        NodeList list = element.getChildNodes();
        if (list == null || list.getLength() <= 0) {
            return elements;
        }
        for (int i = 0; i < list.getLength(); i++) {
            Node node = list.item(i);
            if (Node.ELEMENT_NODE == node.getNodeType()) {
                Element elem = (Element) node;
                if (qName == null) {
                    elements.add(elem);
                } else if (qName.getNamespaceURI().equals(elem.getNamespaceURI())) {
                    if (qName.getLocalPart().equals("*") || qName.getLocalPart().equals(elem.getLocalName())) {
                        elements.add(elem);
                    }
                }
            }
        }
        return elements;
    }

    public static boolean hasChildElement(QName qName, Element element) {
        List<Element> elements = getChildElements(qName, element);
        return !elements.isEmpty();
    }

    public static List<Element> getChildElements(QName qName, Element element) {
        NodeList list = element.getElementsByTagNameNS(qName.getNamespaceURI(), qName.getLocalPart());
        List<Element> elements = new ArrayList<>();
        if (list == null || list.getLength() <= 0) {
            return elements;
        }
        for (int i = 0; i < list.getLength(); i++) {
            elements.add((Element) list.item(i));
        }
        return elements;
    }

    public static Element getChildElementOrNull(QName qName, Element element) {
        List<Element> elements = getChildElements(qName, element);
        if (elements.isEmpty()) {
            return null;
        }
        if (elements.size() > 1) {
            throw new IllegalStateException(String.format("Required qualified element %s is declared more than once: %d"
                    , qName, elements.size()));
        }
        return elements.get(0);
    }

    public static List<Element> getRequiredChildElements(QName qName, Element element) {
        List<Element> elements = getChildElements(qName, element);
        if (elements.isEmpty()) {
            throw new IllegalStateException(String.format("Required qualified element %s not found", qName));
        }
        return elements;
    }

    public static Element getRequiredChildElement(QName qName, Element element) {
        List<Element> elements = getRequiredChildElements(qName, element);
        if (elements.size() > 1) {
            throw new IllegalStateException(String.format("Required qualified element %s is declared more than once: %d"
                    , qName, elements.size()));
        }
        return elements.get(0);
    }

    public static void setPrefixForNamespace(QName qName, Element element) {
        if (isSameNamespaceAndDifferentPrefix(qName, element)) {
            element.setPrefix(qName.getPrefix());
        }
        NodeList list = element.getElementsByTagNameNS(qName.getNamespaceURI(), "*");
        if (list == null || list.getLength() <= 0) {
            return;
        }
        for (int i = 0; i < list.getLength(); i++) {
            Element child = (Element) list.item(i);
            if (isSameNamespaceAndDifferentPrefix(qName, child)) {
                child.setPrefix(qName.getPrefix());
            }
        }
    }

    public static boolean isSameNamespaceAndDifferentPrefix(QName qName, Element element) {
        return element.getNamespaceURI() != null
                && qName.getNamespaceURI().equals(element.getNamespaceURI())
                && (element.getPrefix() == null || !qName.getPrefix().equals(element.getPrefix()));
    }

    public static void setPrefixToNull(String prefix, Element element) {
        if (hasThisPrefix(prefix, element)) {
            element.setPrefix(null);
        }
        NodeList list = element.getElementsByTagNameNS(element.getNamespaceURI(), "*");
        if (list == null || list.getLength() <= 0) {
            return;
        }
        for (int i = 0; i < list.getLength(); i++) {
            Element child = (Element) list.item(i);
            if (hasThisPrefix(prefix, child)) {
                child.setPrefix(null);
            }
        }
    }

    public static boolean hasPrefix(Element element) {
        return element.getPrefix() != null && element.getPrefix().length() > 0;
    }

    public static boolean hasThisPrefix(String prefix, Element element) {
        return hasPrefix(element) && element.getPrefix().equals(prefix);
    }

    public static TargetNamespace getRequiredTargetNamespace(Element element) {
        NamedNodeMap map = element.getAttributes();
        Node targetNamespace = map.getNamedItem("targetNamespace");
        if (targetNamespace == null) {
            throw new IllegalStateException("'targetNamespace' attribute not found");
        }
        String targetNamespaceValue = targetNamespace.getTextContent();
        List<QName> namespaces = getAllNamespaceDeclarations(element);
        for (QName ns : namespaces) {
            if (ns.getNamespaceURI().equals(targetNamespaceValue)) {
                return new TargetNamespace(targetNamespaceValue, ns);
            }
        }
        throw new IllegalStateException(String.format("Target namespace declaration for URI '%s' not found", targetNamespaceValue));
    }

    public static List<QName> getAllNamespaceDeclarations(Element element) {
        List<QName> nodes = new ArrayList<>();
        NamedNodeMap map = element.getAttributes();
        for (int i = 0; i < map.getLength(); i++) {
            Node node = map.item(i);
            String nsUri = node.getNamespaceURI();
            if (nsUri == null) {
                continue;
            }
            if ("http://www.w3.org/2000/xmlns/".equals(nsUri)) {
                nodes.add(new QName(node.getTextContent(), node.getLocalName(), ""));
            }
        }
        return nodes;
    }

    public static Node getNamespaceDeclarationOrNull(String localName, Element element) {
        NamedNodeMap map = element.getAttributes();
        for (int i = 0; i < map.getLength(); i++) {
            Node node = map.item(i);
            String nsUri = node.getNamespaceURI();
            if (nsUri == null) {
                continue;
            }
            if ("http://www.w3.org/2000/xmlns/".equals(nsUri) && localName.equals(node.getLocalName())) {
                return node;
            }
        }
        return null;
    }

    public static Node getRequiredNamespaceDeclaration(String localName, Element element) {
        Node node = getNamespaceDeclarationOrNull(localName, element);
        if (node == null) {
            throw new IllegalStateException(String.format("Namespace declaration for prefix '%s' not found", localName));
        }
        return node;
    }

    public static void setAttributesToListThatHavePrefixedValue(Element element, List<Attribute> attributes, String... names) {
        for (String name : names) {
            setAttributeToListThatHavePrefixedValue(element, attributes, name);
            NodeList list = element.getElementsByTagName("*");
            if (list == null || list.getLength() == 0) {
                continue;
            }
            for (int i = 0; i < list.getLength(); i++) {
                Node node = list.item(i);
                setAttributeToListThatHavePrefixedValue((Element) node, attributes, name);
            }
        }
    }

    private static void setAttributeToListThatHavePrefixedValue(Element element, List<Attribute> attributes, String name) {
        Node attributeNode = getAttributeOrNull(name, element);
        if (attributeNode != null) {
            String value = attributeNode.getTextContent();
            if (value != null && value.contains(":")) {
                Attribute valueWithPrefix = new Attribute(attributeNode);
                if (!attributes.contains(valueWithPrefix)) {
                    attributes.add(valueWithPrefix);
                }
            }
        }
    }

    public static Node getAttributeOrNull(String attribute, Element element) {
        if (!element.hasAttributes()) {
            return null;
        }
        NamedNodeMap attributes = element.getAttributes();
        return attributes.getNamedItem(attribute);
    }

    public static Node getRequiredAttribute(String attribute, Element element) {
        Node node = getAttributeOrNull(attribute, element);
        if (node == null) {
            throw new IllegalStateException(String.format("Attribute {%s}%s not found", element.getLocalName(), attribute));
        }
        return node;
    }

    public static String getAttributeValueOrNull(String attribute, Element element) {
        Node node = getAttributeOrNull(attribute, element);
        if (node == null) {
            return null;
        }
        String value = node.getTextContent();
        if (isNotBlank(value)) {
            return value;
        }
        return null;
    }

    public static String getRequiredAttributeValue(String attribute, Element element) {
        String value = getAttributeValueOrNull(attribute, element);
        if (value == null) {
            throw new IllegalStateException(String.format("Attribute {%s}%s has no value", element.getLocalName(), attribute));
        }
        return value;
    }

    public static boolean hasAttributeValue(String expected, String attribute, Element element) {
        Node node = getAttributeOrNull(attribute, element);
        return node != null && expected.equals(node.getTextContent());
    }

    public static void addNamespaces(Element element, List<QName> qNames) {
        qNames.forEach(qName -> addNamespace(element, qName));
    }

    public static void addNamespace(Element element, QName qName) {
        element.setAttributeNS("http://www.w3.org/2000/xmlns/"
                , "xmlns:" + qName.getLocalPart()
                , qName.getNamespaceURI());
    }

    public static Element getWsdlAbstractMessageByNameValueOrNull(String nameValue, Element wsdl) {
        List<Element> messages = getRequiredChildElements(QualifiedNames.WSDL_MESSAGE, wsdl);
        Optional<Element> messageHolder =
                messages.stream().filter(elem -> getRequiredAttributeValue("name", elem).equals(nameValue)).findFirst();
        if (messageHolder.isPresent()) {
            return messageHolder.get();
        }
        return null;
    }

    public static Element getRequiredWsdlAbstractMessageByNameValue(String nameValue, Element wsdl) {
        Element message = getWsdlAbstractMessageByNameValueOrNull(nameValue, wsdl);
        if (message == null) {
            throw new IllegalStateException(String.format("Required abstract message by name '%s' not found", nameValue));
        }
        return message;
    }

    public static Element getSchemaElementByNameValueOrNull(String nameValue, Element schema) {
        List<Element> elements = getDirectChildElementsOnly(XSD_ELEMENT, schema);
        Optional<Element> elementHolder =
                elements.stream().filter(elem -> getRequiredAttributeValue("name", elem).equals(nameValue)).findFirst();
        if (elementHolder.isPresent()) {
            return elementHolder.get();
        }
        return null;
    }

    public static Element getRequiredSchemaElementByNameValue(String nameValue, Element schema) {
        Element element = getSchemaElementByNameValueOrNull(nameValue, schema);
        if (element == null) {
            throw new IllegalStateException(String.format("Required schema element by name '%s' not found", nameValue));
        }
        return element;
    }

    public static Element getSchemaAttributeByNameValueOrNull(String nameValue, Element schema) {
        List<Element> elements = getDirectChildElementsOnly(XSD_ATTRIBUTE, schema);
        elements.addAll(getDirectChildElementsOnly(XSD_ATTRIBUTE_GROUP, schema));
        Optional<Element> elementHolder =
                elements.stream().filter(elem -> getRequiredAttributeValue("name", elem).equals(nameValue)).findFirst();
        if (elementHolder.isPresent()) {
            return elementHolder.get();
        }
        return null;
    }

    public static Element getRequiredSchemaAttributeByNameValue(String nameValue, Element schema) {
        Element element = getSchemaAttributeByNameValueOrNull(nameValue, schema);
        if (element == null) {
            throw new IllegalStateException(String.format("Required schema attribute by name '%s' not found", nameValue));
        }
        return element;
    }

    /**
     * <pre>
     *     Schema type: simpleType, complexType
     * </pre>
     */
    public static Element getSchemaTypeByNameValueOrNull(String nameValue, Element schema) {
        List<Element> elements = getDirectChildElementsOnly(XSD_SIMPLE_TYPE, schema);
        elements.addAll(getDirectChildElementsOnly(XSD_COMPLEX_TYPE, schema));
        Optional<Element> elementHolder =
                elements.stream().filter(elem -> getRequiredAttributeValue("name", elem).equals(nameValue)).findFirst();
        if (elementHolder.isPresent()) {
            return elementHolder.get();
        }
        return null;
    }

    public static Element getRequiredSchemaTypeByNameValue(String nameValue, Element schema) {
        Element element = getSchemaTypeByNameValueOrNull(nameValue, schema);
        if (element == null) {
            throw new IllegalStateException(String.format("Required schema type by name '%s' not found", nameValue));
        }
        return element;
    }

    public static List<String> getXsdPrefixes(Element element) {
        List<String> prefixes = new ArrayList<>();
        if (element.getPrefix() != null && element.getNamespaceURI().equals(XSD_NS.getNamespaceURI())) {
            prefixes.add(element.getPrefix());
        }
        List<Element> xsdElements = getChildElements(new QName(XSD_NS.getNamespaceURI(), "*"), element);
        xsdElements.forEach(elem -> {
            String prefix = elem.getPrefix();
            if (prefix != null && !prefixes.contains(prefix)) {
                prefixes.add(prefix);
            }
        });
        return prefixes;
    }
}
