package ee.rmit.xrd.test.style;

import ee.rmit.xrd.style.XrdV4WsdlStyleInspector;
import ee.rmit.xrd.wsdl.Wsdl;
import ee.rmit.xrd.wsdl.WsdlTemplate;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import java.net.URL;
import java.util.List;
import java.util.Optional;

import static ee.rmit.xrd.utils.DomUtils.*;
import static ee.rmit.xrd.wsdl.QualifiedNames.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class StyleInspectorTests {
    private XrdV4WsdlStyleInspector styleInspector;
    private Wsdl wsdl;

    @Before
    public void setUp() throws Exception {
        URL url = Thread.currentThread().getContextClassLoader().getResource("style.wsdl");
        wsdl = new Wsdl(parseDocument(url));
        WsdlTemplate wsdlTemplate = new WsdlTemplate(null, "emta-v6");
        Wsdl template = wsdlTemplate.getTemplate();
        styleInspector = new XrdV4WsdlStyleInspector(wsdl, template);
    }

    @Test
    public void testCheckStyle() throws Exception {
        styleInspector.checkStyle();
        validateNamespaces();
        validateWsdlTypes();
        validateWsdlMessages();
        validateWsdlPortTypeOperations();
        validateWsdlBindingOperations();
        validateWsdlService();
    }

    private void validateNamespaces() {
        List<QName> namespaces = wsdl.getNamespaces();
        assertTrue(hasNamespaceDeclaration(WSDL_NS, namespaces));
        assertTrue(hasNamespaceDeclaration(MIME_NS, namespaces));
        assertTrue(hasNamespaceDeclaration(SOAP_NS, namespaces));
        assertTrue(hasNamespaceDeclaration(XRD_NS, namespaces));
        assertTrue(hasNamespaceDeclaration(XSD_NS, namespaces));
        assertTrue(hasNamespaceDeclaration(new QName("http://emta-v6.x-road.eu", "tns", ""), namespaces));
    }

    private boolean hasNamespaceDeclaration(QName expected, List<QName> actualNamespaces) {
        return actualNamespaces.stream().anyMatch(qName -> qName.equals(expected));
    }

    private void validateWsdlTypes() {
        validateElement(WSDL_TYPES, wsdl.getWsdlTypes());
        Element wsdlSchema = wsdl.getWsdlSchema();
        validateElement(new QName(XSD_NS.getNamespaceURI(), "schema", ""), wsdlSchema);
        assertEquals("http://emta-v6.x-road.eu", wsdlSchema.getAttribute("targetNamespace"));
        assertEquals("http://www.w3.org/2001/XMLSchema", wsdlSchema.getNamespaceURI());
    }

    private void validateElement(QName expected, Element actual) {
        assertEquals(expected.getNamespaceURI(), actual.getNamespaceURI());
        assertEquals(expected.getLocalPart(), actual.getLocalName());
        if (actual.getPrefix() == null) {
            assertEquals(expected.getPrefix(), "");
        } else {
            assertEquals(expected.getPrefix(), actual.getPrefix());
        }
    }

    private void validateElementAndValue(QName expected, String expectedValue, Element actual) {
        validateElement(expected, actual);
        assertEquals(expectedValue, actual.getTextContent());
    }

    private void validateElementAndAttributeValue(QName expected, String attribute, String attributeValue, Element actual) {
        validateElement(expected, actual);
        String actualAttributeValue = actual.getAttribute(attribute);
        assertEquals(attributeValue, actualAttributeValue);
    }

    private void validateWsdlMessages() {
        assertEquals(5, wsdl.getWsdlMessages().size());

        Element upload = getRequiredWsdlMessageByName("upload");
        List<Element> elements = getRequiredChildElements(WSDL_PART, upload);
        assertEquals(2, elements.size());
        validateElementAndAttributeValue(WSDL_PART, "element", "tns:upload", getRequiredElementByAttributeName("parameters", elements));
        validateElementAndAttributeValue(WSDL_PART, "type", "xsd:base64Binary", getRequiredElementByAttributeName("file", elements));

        Element uploadResponse = getRequiredWsdlMessageByName("uploadResponse");
        elements = getRequiredChildElements(WSDL_PART, uploadResponse);
        assertEquals(1, elements.size());
        validateElementAndAttributeValue(WSDL_PART, "element", "tns:uploadResponse", getRequiredElementByAttributeName("parameters", elements));

        Element download = getRequiredWsdlMessageByName("download");
        elements = getRequiredChildElements(WSDL_PART, download);
        assertEquals(1, elements.size());
        validateElementAndAttributeValue(WSDL_PART, "element", "tns:download", getRequiredElementByAttributeName("parameters", elements));

        Element downloadResponse = getRequiredWsdlMessageByName("downloadResponse");
        elements = getRequiredChildElements(WSDL_PART, downloadResponse);
        assertEquals(2, elements.size());
        validateElementAndAttributeValue(WSDL_PART, "element", "tns:downloadResponse", getRequiredElementByAttributeName("parameters", elements));
        validateElementAndAttributeValue(WSDL_PART, "type", "xsd:base64Binary", getRequiredElementByAttributeName("file", elements));

        Element xrdHeader = getRequiredWsdlMessageByName("xrdHeader");
        elements = getRequiredChildElements(WSDL_PART, xrdHeader);
        assertEquals(6, elements.size());
        validateElementAndAttributeValue(WSDL_PART, "element", "xrd:client", getRequiredElementByAttributeName("client", elements));
        validateElementAndAttributeValue(WSDL_PART, "element", "xrd:service", getRequiredElementByAttributeName("service", elements));
        validateElementAndAttributeValue(WSDL_PART, "element", "xrd:id", getRequiredElementByAttributeName("id", elements));
        validateElementAndAttributeValue(WSDL_PART, "element", "xrd:userId", getRequiredElementByAttributeName("userId", elements));
        validateElementAndAttributeValue(WSDL_PART, "element", "xrd:issue", getRequiredElementByAttributeName("issue", elements));
        validateElementAndAttributeValue(WSDL_PART, "element", "xrd:protocolVersion", getRequiredElementByAttributeName("protocolVersion", elements));
    }

    private Element getRequiredWsdlMessageByName(String name) {
        Optional<Element> messageHolder =
                wsdl.getWsdlMessages().stream().filter(elem -> getRequiredAttributeValue("name", elem).equals(name)).findFirst();
        if (messageHolder.isPresent()) {
            return messageHolder.get();
        }
        throw new IllegalArgumentException(String.format("Required abstract message '%s' not found", name));
    }

    private Element getRequiredElementByAttributeName(String name, List<Element> elements) {
        Optional<Element> elementHolder =
                elements.stream().filter(elem -> getRequiredAttributeValue("name", elem).equals(name)).findFirst();
        if (elementHolder.isPresent()) {
            return elementHolder.get();
        }
        throw new IllegalArgumentException(String.format("Required element '%s' not found", name));
    }

    private void validateWsdlPortTypeOperations() {
        assertEquals(2, wsdl.getWsdlPortTypeOperations().size());
        validateElementAndAttributeValue(WSDL_PORT_TYPE, "name", "mimePort", wsdl.getWsdlPortType());

        Element upload = getRequiredWsdlPortTypeOperationByName("upload");
        Element input = getRequiredChildElement(WSDL_INPUT, upload);
        validateElementAndAttributeValue(WSDL_INPUT, "message", "tns:upload", input);
        Element output = getRequiredChildElement(WSDL_OUTPUT, upload);
        validateElementAndAttributeValue(WSDL_OUTPUT, "message", "tns:uploadResponse", output);

        Element download = getRequiredWsdlPortTypeOperationByName("download");
        input = getRequiredChildElement(WSDL_INPUT, download);
        validateElementAndAttributeValue(WSDL_INPUT, "message", "tns:download", input);
        output = getRequiredChildElement(WSDL_OUTPUT, download);
        validateElementAndAttributeValue(WSDL_OUTPUT, "message", "tns:downloadResponse", output);
    }

    private Element getRequiredWsdlPortTypeOperationByName(String name) {
        Optional<Element> messageHolder =
                wsdl.getWsdlPortTypeOperations().stream().filter(elem -> getRequiredAttributeValue("name", elem).equals(name)).findFirst();
        if (messageHolder.isPresent()) {
            return messageHolder.get();
        }
        throw new IllegalArgumentException(String.format("Required port type operation '%s' not found", name));
    }

    private void validateWsdlBindingOperations() {
        assertEquals(2, wsdl.getWsdlBindingOperations().size());
        validateElementAndAttributeValue(WSDL_BINDING, "name", "mimeBind", wsdl.getWsdlBinding());
        validateElementAndAttributeValue(WSDL_BINDING, "type", "tns:mimePort", wsdl.getWsdlBinding());

        Element soapBinding = getRequiredChildElement(SOAP_BINDING, wsdl.getWsdlBinding());
        validateElementAndAttributeValue(SOAP_BINDING, "style", "document", soapBinding);
        validateElementAndAttributeValue(SOAP_BINDING, "transport", "http://schemas.xmlsoap.org/soap/http", soapBinding);

        validateBindingOperationUpload();
        validateBindingOperationDownload();
    }

    private void validateBindingOperationUpload() {
        Element upload = getRequiredWsdlBindingOperationByName("upload");

        Element soapOperation = getRequiredChildElement(SOAP_OPERATION, upload);
        validateElementAndAttributeValue(SOAP_OPERATION, "soapAction", "", soapOperation);

        Element xrdVersion = getRequiredChildElement(XRD_VERSION, upload);
        validateElementAndValue(XRD_VERSION, "v1", xrdVersion);

        Element input = getRequiredChildElement(WSDL_INPUT, upload);
        Element multipartMime = getRequiredChildElement(MIME_MULTIPART, input);
        validateElement(MIME_MULTIPART, multipartMime);
        List<Element> parts = getRequiredChildElements(MIME_PART, multipartMime);
        assertEquals(2, parts.size());
        Element partSoap = getRequiredMimePartByName("soap", parts);
        List<Element> xrdHeaders = getRequiredChildElements(SOAP_HEADER, partSoap);
        assertEquals(6, xrdHeaders.size());
        validateXrdHeaderByPart(XRD_CLIENT.getLocalPart(), xrdHeaders);
        validateXrdHeaderByPart(XRD_SERVICE.getLocalPart(), xrdHeaders);
        validateXrdHeaderByPart(XRD_ID.getLocalPart(), xrdHeaders);
        validateXrdHeaderByPart(XRD_PROTO_VER.getLocalPart(), xrdHeaders);
        validateXrdHeaderByPart(XRD_USER_ID.getLocalPart(), xrdHeaders);
        validateXrdHeaderByPart(XRD_ISSUE.getLocalPart(), xrdHeaders);
        Element soapBody = getRequiredChildElement(SOAP_BODY, partSoap);
        assertEquals("parameters", soapBody.getAttribute("parts"));
        assertEquals("literal", soapBody.getAttribute("use"));
        Element partFile = getRequiredMimePartByName("file", parts);
        Element content = getRequiredChildElement(MIME_CONTENT, partFile);
        assertEquals("file", content.getAttribute("part"));
        assertEquals("application/octet-stream", content.getAttribute("type"));

        Element output = getRequiredChildElement(WSDL_OUTPUT, upload);
        xrdHeaders = getRequiredChildElements(SOAP_HEADER, output);
        assertEquals(6, xrdHeaders.size());
        validateXrdHeaderByPart(XRD_CLIENT.getLocalPart(), xrdHeaders);
        validateXrdHeaderByPart(XRD_SERVICE.getLocalPart(), xrdHeaders);
        validateXrdHeaderByPart(XRD_ID.getLocalPart(), xrdHeaders);
        validateXrdHeaderByPart(XRD_PROTO_VER.getLocalPart(), xrdHeaders);
        validateXrdHeaderByPart(XRD_USER_ID.getLocalPart(), xrdHeaders);
        validateXrdHeaderByPart(XRD_ISSUE.getLocalPart(), xrdHeaders);
        soapBody = getRequiredChildElement(SOAP_BODY, output);
        assertEquals("literal", soapBody.getAttribute("use"));
    }

    private void validateBindingOperationDownload() {
        Element download = getRequiredWsdlBindingOperationByName("download");

        Element soapOperation = getRequiredChildElement(SOAP_OPERATION, download);
        validateElementAndAttributeValue(SOAP_OPERATION, "soapAction", "", soapOperation);

        Element xrdVersion = getRequiredChildElement(XRD_VERSION, download);
        validateElementAndValue(XRD_VERSION, "v1", xrdVersion);

        Element input = getRequiredChildElement(WSDL_INPUT, download);
        List<Element> xrdHeaders = getRequiredChildElements(SOAP_HEADER, input);
        assertEquals(4, xrdHeaders.size());
        validateXrdHeaderByPart(XRD_CLIENT.getLocalPart(), xrdHeaders);
        validateXrdHeaderByPart(XRD_SERVICE.getLocalPart(), xrdHeaders);
        validateXrdHeaderByPart(XRD_ID.getLocalPart(), xrdHeaders);
        validateXrdHeaderByPart(XRD_PROTO_VER.getLocalPart(), xrdHeaders);
        Element soapBody = getRequiredChildElement(SOAP_BODY, input);
        assertEquals("literal", soapBody.getAttribute("use"));

        Element output = getRequiredChildElement(WSDL_OUTPUT, download);
        Element multipartMime = getRequiredChildElement(MIME_MULTIPART, output);
        validateElement(MIME_MULTIPART, multipartMime);
        List<Element> parts = getRequiredChildElements(MIME_PART, multipartMime);
        assertEquals(2, parts.size());
        Element partSoap = getRequiredMimePartByName("soap", parts);
        xrdHeaders = getRequiredChildElements(SOAP_HEADER, partSoap);
        assertEquals(4, xrdHeaders.size());
        validateXrdHeaderByPart(XRD_CLIENT.getLocalPart(), xrdHeaders);
        validateXrdHeaderByPart(XRD_SERVICE.getLocalPart(), xrdHeaders);
        validateXrdHeaderByPart(XRD_ID.getLocalPart(), xrdHeaders);
        validateXrdHeaderByPart(XRD_PROTO_VER.getLocalPart(), xrdHeaders);
        soapBody = getRequiredChildElement(SOAP_BODY, partSoap);
        assertEquals("parameters", soapBody.getAttribute("parts"));
        assertEquals("literal", soapBody.getAttribute("use"));
        Element partFile = getRequiredMimePartByName("file", parts);
        Element content = getRequiredChildElement(MIME_CONTENT, partFile);
        assertEquals("file", content.getAttribute("part"));
        assertEquals("application/octet-stream", content.getAttribute("type"));
    }

    private Element getRequiredWsdlBindingOperationByName(String name) {
        Optional<Element> messageHolder =
                wsdl.getWsdlBindingOperations().stream().filter(elem -> getRequiredAttributeValue("name", elem).equals(name)).findFirst();
        if (messageHolder.isPresent()) {
            return messageHolder.get();
        }
        throw new IllegalArgumentException(String.format("Required binding operation '%s' not found", name));
    }

    private Element getRequiredMimePartByName(String name, List<Element> elements) {
        Optional<Element> messageHolder =
                elements.stream().filter(elem -> getRequiredAttributeValue("name", elem).equals(name)).findFirst();
        if (messageHolder.isPresent()) {
            return messageHolder.get();
        }
        throw new IllegalArgumentException(String.format("Required mime part '%s' not found", name));
    }

    private void validateXrdHeaderByPart(String part, List<Element> elements) {
        Optional<Element> xrdHeaderHolder = elements.stream().filter(elem ->
                getRequiredAttributeValue("part", elem).equals(part)).findFirst();
        if (!xrdHeaderHolder.isPresent()) {
            throw new IllegalArgumentException(String.format("Required xrd header by part '%s' not found", part));

        }
        Element xrdHeader = xrdHeaderHolder.get();
        assertEquals("tns:xrdHeader", xrdHeader.getAttribute("message"));
        assertEquals("literal", xrdHeader.getAttribute("use"));
    }

    private void validateWsdlService() {
        validateElement(WSDL_SERVICE, wsdl.getWsdlService());
        Element wsdlPort = getRequiredChildElement(WSDL_PORT, wsdl.getWsdlService());
        validateElementAndAttributeValue(WSDL_PORT, "binding", "tns:mimeBind", wsdlPort);
        validateElementAndAttributeValue(WSDL_PORT, "name", "mimeServicePort", wsdlPort);
        Element addressSoap = getRequiredChildElement(SOAP_ADDRESS, wsdlPort);
        validateElementAndAttributeValue(SOAP_ADDRESS, "location", "http://TURVASERVER/", addressSoap);
        Element titleXrd = getRequiredChildElement(XRD_TITLE, wsdlPort);
        validateElementAndValue(XRD_TITLE, "Maksu- ja Tolliameti X-tee teenused", titleXrd);
    }
}
