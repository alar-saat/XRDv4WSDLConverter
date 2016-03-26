package ee.rmit.xrd.test;

import ee.rmit.xrd.WsdlServiceExtractor;
import ee.rmit.xrd.wsdl.Wsdl;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Element;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static ee.rmit.xrd.utils.DomUtils.getRequiredAttributeValue;
import static org.junit.Assert.assertEquals;

public class WsdlServiceExtractorTests {
    private Path wsdlDir;
    private WsdlServiceExtractor extractor;

    @Before
    public void setUp() throws Exception {
        URL url = Thread.currentThread().getContextClassLoader().getResource(".");
        if (url == null) {
            throw new IllegalStateException("WSDL dir required");
        }
        wsdlDir = Paths.get(url.getPath());
        extractor = new WsdlServiceExtractor("classpath://document-literal.wsdl");
        extractor.setWsdlDir(wsdlDir);
    }

    @Test
    public void testExtractServices() throws Exception {
        extractor.extractServices("service1*", "service2*");

        Wsdl result = new Wsdl(wsdlDir.resolve("services.wsdl"));
        assertEquals(2, result.getWsdlBindingOperations().size());
        validateByNameAttribute("service1", result.getWsdlBindingOperations());
        validateByNameAttribute("service2", result.getWsdlBindingOperations());
        assertEquals(2, result.getWsdlPortTypeOperations().size());
        validateByNameAttribute("service1", result.getWsdlPortTypeOperations());
        validateByNameAttribute("service2", result.getWsdlPortTypeOperations());
        assertEquals(5, result.getWsdlMessages().size());
        validateByNameAttribute("service1", result.getWsdlMessages());
        validateByNameAttribute("service1Response", result.getWsdlMessages());
        validateByNameAttribute("service2", result.getWsdlMessages());
        validateByNameAttribute("service2Response", result.getWsdlMessages());
        validateByNameAttribute("xrdHeader", result.getWsdlMessages());
    }

    private void validateByNameAttribute(String name, List<Element> elements) {
        if (elements.stream().noneMatch(elem -> getRequiredAttributeValue("name", elem).equals(name))) {
            throw new IllegalArgumentException(String.format("Required element by name '%s' attribute not found", name));
        }
    }
}
