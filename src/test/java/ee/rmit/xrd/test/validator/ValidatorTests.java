package ee.rmit.xrd.test.validator;

import ee.rmit.xrd.validator.XrdV4WsdlValidator;
import ee.rmit.xrd.wsdl.Wsdl;
import org.junit.Test;

import java.net.URL;
import java.nio.file.Files;

import static ee.rmit.xrd.utils.DomUtils.parseDocument;
import static ee.rmit.xrd.wsdl.QualifiedNames.WSDL_INPUT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ValidatorTests {
    private XrdV4WsdlValidator validator;

    @Test
    public void testValid() throws Exception {
        setUpValidator("valid.wsdl");
        //happy path test
        validator.validate();
    }

    private void setUpValidator(String wsdlResource) throws Exception {
        URL url = Thread.currentThread().getContextClassLoader().getResource(wsdlResource);
        Wsdl wsdl = new Wsdl(parseDocument(url));
        validator = new XrdV4WsdlValidator(wsdl);
    }

    @Test
    public void testValidMime() throws Exception {
        setUpValidator("valid_mime.wsdl");
        //happy path test
        validator.validate();
    }

    @Test
    public void testInvalidSchema() throws Exception {
        setUpValidator("invalid_schema.wsdl");
        try {
            validator.validate();
            fail("Should have thrown IllegalStateException");
        } catch (IllegalStateException expected) {
            assertEquals("Invalid schema in WSDL types", expected.getMessage());
            Files.delete(validator.getSchemaFile());
        }
    }

    @Test
    public void testInvalidServiceBindingReference() throws Exception {
        setUpValidator("invalid_service_ref.wsdl");
        try {
            validator.validate();
            fail("Should have thrown IllegalStateException");
        } catch (IllegalStateException expected) {
            assertEquals("Invalid attribute 'name' value 'serviceBind' for element 'binding'. Expected value 'serviceBindInvalid'", expected.getMessage());
        }
    }

    @Test
    public void testInvalidBindingPortTypeReference() throws Exception {
        setUpValidator("invalid_binding_ref.wsdl");
        try {
            validator.validate();
            fail("Should have thrown IllegalStateException");
        } catch (IllegalStateException expected) {
            assertEquals("Invalid attribute 'name' value 'servicePortType' for element 'portType'. Expected value 'servicePortTypeInvalid'", expected.getMessage());
        }
    }

    @Test
    public void testInvalidBindingMissingInput() throws Exception {
        setUpValidator("invalid_binding_missing_input.wsdl");
        try {
            validator.validate();
            fail("Should have thrown IllegalStateException");
        } catch (IllegalStateException expected) {
            assertEquals(String.format("Required qualified element %s not found", WSDL_INPUT), expected.getMessage());
        }
    }

    @Test
    public void testInvalidBindingMissingHeader() throws Exception {
        setUpValidator("invalid_binding_missing_header.wsdl");
        try {
            validator.validate();
            fail("Should have thrown IllegalStateException");
        } catch (IllegalStateException expected) {
            assertEquals("Required XRD header 'client' not found", expected.getMessage());
        }
    }

    @Test
    public void testInvalidPortTypeMissingInput() throws Exception {
        setUpValidator("invalid_portType_missing_input.wsdl");
        try {
            validator.validate();
            fail("Should have thrown IllegalStateException");
        } catch (IllegalStateException expected) {
            assertEquals(String.format("Required qualified element %s not found", WSDL_INPUT), expected.getMessage());
        }
    }

    @Test
    public void testInvalidPortTypeMessageReference() throws Exception {
        setUpValidator("invalid_portType_ref.wsdl");
        try {
            validator.validate();
            fail("Should have thrown IllegalStateException");
        } catch (IllegalStateException expected) {
            assertEquals("Required abstract message by name 'serviceInvalid' not found", expected.getMessage());
        }
    }

    @Test
    public void testInvalidMessageMissingPartAttributes() throws Exception {
        setUpValidator("invalid_message_missing_part_attributes.wsdl");
        try {
            validator.validate();
            fail("Should have thrown IllegalStateException");
        } catch (IllegalStateException expected) {
            assertEquals("Required attribute {part}name not found", expected.getMessage());
        }
    }

    @Test
    public void testInvalidMessageSchemaReference() throws Exception {
        setUpValidator("invalid_message_ref.wsdl");
        try {
            validator.validate();
            fail("Should have thrown IllegalStateException");
        } catch (IllegalStateException expected) {
            assertEquals("Required schema element by name 'serviceInvalid' not found", expected.getMessage());
        }
    }
}
