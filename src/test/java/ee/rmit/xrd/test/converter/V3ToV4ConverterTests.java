package ee.rmit.xrd.test.converter;

import ee.rmit.xrd.converter.V3ToV4Converter;
import ee.rmit.xrd.test.AbstractWsdlValidator;
import ee.rmit.xrd.wsdl.Wsdl;
import ee.rmit.xrd.wsdl.WsdlTemplate;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Element;

import java.net.URL;
import java.util.List;

import static ee.rmit.xrd.utils.DomUtils.*;
import static ee.rmit.xrd.wsdl.QualifiedNames.*;
import static org.junit.Assert.assertEquals;

public class V3ToV4ConverterTests extends AbstractWsdlValidator {
    private Wsdl template;

    @Before
    public void setUp() throws Exception {
        WsdlTemplate wsdlTemplate = new WsdlTemplate(null, "emta-v6");
        template = wsdlTemplate.getTemplate();
    }

    @Test
    public void testSimpleConvert() throws Exception {
        Wsdl v3Wsdl = createWsdlFromClasspathResource("simple-v3.wsdl");

        V3ToV4Converter converter = new V3ToV4Converter(v3Wsdl, template);
        converter.convert();

        validateSchemaObjects();
        validateMessages();
        validatePortTypeOperation();
        validateBindingOperation();
    }

    private Wsdl createWsdlFromClasspathResource(String resourceName) throws Exception {
        URL url = Thread.currentThread().getContextClassLoader().getResource(resourceName);
        return new Wsdl(parseDocument(url));
    }

    private void validateSchemaObjects() {
        List<Element> elements = getDirectChildElementsOnly(XSD_ELEMENT, template.getWsdlSchema());
        assertEquals(3, elements.size());
        validateSchemaObjectByName("napTeenus", elements);
        validateSchemaObjectByName("napTeenusResponse", elements);
        validateSchemaObjectByName("businessException", elements);
        List<Element> types = getDirectChildElementsOnly(XSD_COMPLEX_TYPE, template.getWsdlSchema());
        assertEquals(2, types.size());
        validateSchemaObjectByName("napTeenusRequestType", types);
        validateSchemaObjectByName("napTeenusResponseType", types);
    }

    private void validateMessages() {
        assertEquals(4, template.getWsdlMessages().size());
        validateMessageByName("napTeenus", template.getWsdlMessages());
        validateMessageByName("napTeenusResponse", template.getWsdlMessages());
        validateMessageByName("businessException", template.getWsdlMessages());
        validateMessageByName("xrdHeader", template.getWsdlMessages());
    }

    private void validatePortTypeOperation() {
        assertEquals(1, template.getWsdlPortTypeOperations().size());
        Element operation = getRequiredChildElement(WSDL_OPERATION, template.getWsdlPortType());
        assertEquals("napTeenus", operation.getAttribute("name"));
        Element input = getRequiredChildElement(WSDL_INPUT, operation);
        assertEquals("tns:napTeenus", input.getAttribute("message"));
        Element output = getRequiredChildElement(WSDL_OUTPUT, operation);
        assertEquals("tns:napTeenusResponse", output.getAttribute("message"));
        Element fault = getRequiredChildElement(WSDL_FAULT, operation);
        assertEquals("tns:businessException", fault.getAttribute("message"));
    }

    private void validateBindingOperation() {
        assertEquals(1, template.getWsdlBindingOperations().size());
        Element operation = getRequiredChildElement(WSDL_OPERATION, template.getWsdlBinding());
        assertEquals("napTeenus", operation.getAttribute("name"));
        Element input = getRequiredChildElement(WSDL_INPUT, operation);
        List<Element> headers = getRequiredChildElements(SOAP_HEADER, input);
        assertEquals(6, headers.size());
        getRequiredChildElement(SOAP_BODY, input);
        Element output = getRequiredChildElement(WSDL_OUTPUT, operation);
        headers = getRequiredChildElements(SOAP_HEADER, output);
        assertEquals(6, headers.size());
        getRequiredChildElement(SOAP_BODY, output);
    }

    @Test
    public void testMimeConvert() throws Exception {
        Wsdl v3Wsdl = createWsdlFromClasspathResource("mime-v3.wsdl");

        V3ToV4Converter converter = new V3ToV4Converter(v3Wsdl, template);
        converter.convert();

        validateSchemaObjects();
        validateMessages();
        validatePortTypeOperation();
        validateBindingOperation();
        validateMimeParts();
    }

    private void validateMimeParts() {
        Element operation = getRequiredChildElement(WSDL_OPERATION, template.getWsdlBinding());

        Element input = getRequiredChildElement(WSDL_INPUT, operation);
        Element multipart = getRequiredChildElement(MIME_MULTIPART, input);
        List<Element> parts = getRequiredChildElements(MIME_PART, multipart);
        validateMimePartByName("soap", parts);
        validateMimePartByName("attachment", parts);

        Element output = getRequiredChildElement(WSDL_OUTPUT, operation);
        multipart = getRequiredChildElement(MIME_MULTIPART, output);
        parts = getRequiredChildElements(MIME_PART, multipart);
        validateMimePartByName("soap", parts);
        validateMimePartByName("attachment1", parts);
        validateMimePartByName("attachment2", parts);
    }
}
