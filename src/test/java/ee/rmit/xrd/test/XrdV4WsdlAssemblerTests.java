package ee.rmit.xrd.test;

import ee.rmit.xrd.XrdV4WsdlAssembler;
import ee.rmit.xrd.wsdl.Wsdl;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static ee.rmit.xrd.utils.DomUtils.getRequiredAttributeValue;
import static ee.rmit.xrd.utils.DomUtils.parseDocument;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class XrdV4WsdlAssemblerTests {
    private XrdV4WsdlAssembler assembler;
    private String producerName = "emta-v6";
    private Path wsdlDir;

    @Before
    public void setUp() throws Exception {
        URL url = Thread.currentThread().getContextClassLoader().getResource("wsdl");
        if (url == null) {
            throw new IllegalStateException("WSDL dir required");
        }
        wsdlDir = Paths.get(url.getPath());
        assembler = new XrdV4WsdlAssembler(wsdlDir);
        assembler.setProducerName(producerName);
        assembler.setTemplateLocation("classpath://template/emta-v6-template.wsdl");
    }

    @Test
    public void testAssembleAllServices() throws Exception {
        assembler.assembleWsdlFiles();
        Path assembledWsdlFile = wsdlDir.resolve(producerName + ".wsdl");
        assertTrue(Files.isRegularFile(assembledWsdlFile));
        List<String> serviceNames = getServiceNames(assembledWsdlFile);
        assertEquals(3, serviceNames.size());
        assertTrue(serviceNames.contains("service1"));
        assertTrue(serviceNames.contains("service2"));
        assertTrue(serviceNames.contains("service3"));
    }

    private List<String> getServiceNames(Path assembledWsdlFile) throws Exception {
        Document document = parseDocument(assembledWsdlFile);
        Wsdl wsdl = new Wsdl(document);
        List<String> operationNames = new ArrayList<>();
        wsdl.getWsdlPortTypeOperations().forEach(elem -> operationNames.add(getRequiredAttributeValue("name", elem)));
        return operationNames;
    }

    @Test
    public void testAssembleService2() throws Exception {
        assembler.assembleWsdlFiles("service2.wsdl");
        Path assembledWsdlFile = wsdlDir.resolve(producerName + ".wsdl");
        assertTrue(Files.isRegularFile(assembledWsdlFile));
        List<String> serviceNames = getServiceNames(assembledWsdlFile);
        assertEquals(1, serviceNames.size());
        assertTrue(serviceNames.contains("service2"));
    }

    @Test
    public void testAssembleService1AndService3() throws Exception {
        assembler.assembleWsdlFiles("{service1,service3}.*");
        Path assembledWsdlFile = wsdlDir.resolve(producerName + ".wsdl");
        assertTrue(Files.isRegularFile(assembledWsdlFile));
        List<String> serviceNames = getServiceNames(assembledWsdlFile);
        assertEquals(2, serviceNames.size());
        assertTrue(serviceNames.contains("service1"));
        assertTrue(serviceNames.contains("service3"));
    }

    @Test
    public void testAssembleServicesNoValidation() throws Exception {
        assembler.assembleWsdlFilesNoValidation("{service1,service2,service3}.wsdl");
        Path assembledWsdlFile = wsdlDir.resolve(producerName + ".wsdl");
        assertTrue(Files.isRegularFile(assembledWsdlFile));
        List<String> serviceNames = getServiceNames(assembledWsdlFile);
        assertEquals(3, serviceNames.size());
        assertTrue(serviceNames.contains("service1"));
        assertTrue(serviceNames.contains("service2"));
        assertTrue(serviceNames.contains("service3"));
    }
}
