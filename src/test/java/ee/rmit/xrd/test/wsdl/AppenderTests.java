package ee.rmit.xrd.test.wsdl;

import ee.rmit.xrd.test.AbstractWsdlValidator;
import ee.rmit.xrd.wsdl.Wsdl;
import ee.rmit.xrd.wsdl.WsdlAppender;
import ee.rmit.xrd.wsdl.WsdlTemplate;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Element;

import java.net.URL;
import java.util.List;

import static ee.rmit.xrd.utils.DomUtils.*;
import static ee.rmit.xrd.wsdl.QualifiedNames.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AppenderTests extends AbstractWsdlValidator {
    private WsdlAppender appender;
    private Wsdl template;

    @Before
    public void setUp() throws Exception {
        WsdlTemplate wsdlTemplate = new WsdlTemplate(null, "emta-v6");
        template = wsdlTemplate.getTemplate();
        appender = new WsdlAppender(template);
    }

    @Test
    public void testAppend() throws Exception {
        assertTrue(template.getWsdlPortTypeOperations().isEmpty());
        assertTrue(template.getWsdlBindingOperations().isEmpty());
        URL url = Thread.currentThread().getContextClassLoader().getResource("valid.wsdl");
        Wsdl wsdl = new Wsdl(parseDocument(url));

        appender.appendWsdl(wsdl);

        assertEquals(1, template.getWsdlPortTypeOperations().size());
        assertEquals(1, template.getWsdlBindingOperations().size());
        validateSchemaElements();
        validateMessages();
        validatePortTypeOperation();
        validateBindingOperation();
    }

    private void validateSchemaElements() {
        List<Element> elements = getDirectChildElementsOnly(XSD_ELEMENT, template.getWsdlSchema());
        assertEquals(2, elements.size());
        validateSchemaObjectByName("service", elements);
        validateSchemaObjectByName("serviceResponse", elements);
    }

    private void validateMessages() {
        assertEquals(3, template.getWsdlMessages().size());
        validateMessageByName("service", template.getWsdlMessages());
        validateMessageByName("serviceResponse", template.getWsdlMessages());
        validateMessageByName("xrdHeader", template.getWsdlMessages());
    }

    private void validatePortTypeOperation() {
        Element operation = getRequiredChildElement(WSDL_OPERATION, template.getWsdlPortType());
        assertEquals("service", operation.getAttribute("name"));
        Element input = getRequiredChildElement(WSDL_INPUT, operation);
        assertEquals("tns:service", input.getAttribute("message"));
        Element output = getRequiredChildElement(WSDL_OUTPUT, operation);
        assertEquals("tns:serviceResponse", output.getAttribute("message"));
    }

    private void validateBindingOperation() {
        Element operation = getRequiredChildElement(WSDL_OPERATION, template.getWsdlBinding());
        assertEquals("service", operation.getAttribute("name"));
        Element input = getRequiredChildElement(WSDL_INPUT, operation);
        List<Element> headers = getRequiredChildElements(SOAP_HEADER, input);
        assertEquals(4, headers.size());
        getRequiredChildElement(SOAP_BODY, input);
        Element output = getRequiredChildElement(WSDL_OUTPUT, operation);
        headers = getRequiredChildElements(SOAP_HEADER, output);
        assertEquals(4, headers.size());
        getRequiredChildElement(SOAP_BODY, output);
    }
}
