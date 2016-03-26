package ee.rmit.xrd.test.wsdl;

import ee.rmit.xrd.test.AbstractWsdlValidator;
import ee.rmit.xrd.wsdl.Wsdl;
import ee.rmit.xrd.wsdl.WsdlExtractor;
import org.junit.Test;
import org.w3c.dom.Element;

import java.net.URL;
import java.util.List;
import java.util.Map;

import static ee.rmit.xrd.utils.DomUtils.getDirectChildElementsOnly;
import static ee.rmit.xrd.utils.DomUtils.parseDocument;
import static ee.rmit.xrd.wsdl.QualifiedNames.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ExtractorTests extends AbstractWsdlValidator {

    @Test
    public void testExtractAllServicesDocLiteral() throws Exception {
        WsdlExtractor extractor = new WsdlExtractor(createWsdlFromClasspathResource("document-literal.wsdl"));

        Map<String, Wsdl> wsdls = extractor.extractServices();

        assertEquals(3, wsdls.size());
        validateService1(wsdls.get("service1"));
        validateService2(wsdls.get("service2"));
        validateService3(wsdls.get("service3"));
    }

    private Wsdl createWsdlFromClasspathResource(String resourceName) throws Exception {
        URL url = Thread.currentThread().getContextClassLoader().getResource(resourceName);
        return new Wsdl(parseDocument(url));
    }

    private void validateService1(Wsdl wsdl) {
        assertEquals(1, wsdl.getWsdlBindingOperations().size());
        validateBindingOperationName("service1", wsdl);
        assertEquals(1, wsdl.getWsdlPortTypeOperations().size());
        validatePortTypeOperationName("service1", wsdl);
        assertEquals(3, wsdl.getWsdlMessages().size());
        validateMessageByName("service1", wsdl.getWsdlMessages());
        validateMessageByName("service1Response", wsdl.getWsdlMessages());
        validateMessageByName("xrdHeader", wsdl.getWsdlMessages());
        List<Element> elements = getDirectChildElementsOnly(XSD_ELEMENT, wsdl.getWsdlSchema());
        assertEquals(2, elements.size());
        validateSchemaObjectByName("service1", elements);
        validateSchemaObjectByName("service1Response", elements);
        assertTrue(getDirectChildElementsOnly(XSD_SIMPLE_TYPE, wsdl.getWsdlSchema()).isEmpty());
        assertTrue(getDirectChildElementsOnly(XSD_COMPLEX_TYPE, wsdl.getWsdlSchema()).isEmpty());
        assertTrue(getDirectChildElementsOnly(XSD_ATTRIBUTE, wsdl.getWsdlSchema()).isEmpty());
    }

    private void validateService2(Wsdl wsdl) {
        assertEquals(1, wsdl.getWsdlBindingOperations().size());
        validateBindingOperationName("service2", wsdl);
        assertEquals(1, wsdl.getWsdlPortTypeOperations().size());
        validatePortTypeOperationName("service2", wsdl);
        assertEquals(3, wsdl.getWsdlMessages().size());
        validateMessageByName("service2", wsdl.getWsdlMessages());
        validateMessageByName("service2Response", wsdl.getWsdlMessages());
        validateMessageByName("xrdHeader", wsdl.getWsdlMessages());
        List<Element> elements = getDirectChildElementsOnly(XSD_ELEMENT, wsdl.getWsdlSchema());
        assertEquals(2, elements.size());
        validateSchemaObjectByName("service2", elements);
        validateSchemaObjectByName("service2Response", elements);
        elements = getDirectChildElementsOnly(XSD_COMPLEX_TYPE, wsdl.getWsdlSchema());
        assertEquals(2, elements.size());
        validateSchemaObjectByName("service2RequestType", elements);
        validateSchemaObjectByName("service2ResponseType", elements);
        assertTrue(getDirectChildElementsOnly(XSD_SIMPLE_TYPE, wsdl.getWsdlSchema()).isEmpty());
        assertTrue(getDirectChildElementsOnly(XSD_ATTRIBUTE, wsdl.getWsdlSchema()).isEmpty());
    }

    private void validateService3(Wsdl wsdl) {
        assertEquals(1, wsdl.getWsdlBindingOperations().size());
        validateBindingOperationName("service3", wsdl);
        assertEquals(1, wsdl.getWsdlPortTypeOperations().size());
        validatePortTypeOperationName("service3", wsdl);
        assertEquals(4, wsdl.getWsdlMessages().size());
        validateMessageByName("service3", wsdl.getWsdlMessages());
        validateMessageByName("service3Response", wsdl.getWsdlMessages());
        validateMessageByName("xrdHeader", wsdl.getWsdlMessages());
        validateMessageByName("businessException", wsdl.getWsdlMessages());
        List<Element> elements = getDirectChildElementsOnly(XSD_ELEMENT, wsdl.getWsdlSchema());
        assertEquals(4, elements.size());
        validateSchemaObjectByName("service3", elements);
        validateSchemaObjectByName("service3Response", elements);
        validateSchemaObjectByName("goods", elements);
        validateSchemaObjectByName("businessException", elements);
        elements = getDirectChildElementsOnly(XSD_COMPLEX_TYPE, wsdl.getWsdlSchema());
        assertEquals(6, elements.size());
        validateSchemaObjectByName("service3RequestType", elements);
        validateSchemaObjectByName("service3ResponseType", elements);
        validateSchemaObjectByName("service3GoodsNode", elements);
        validateSchemaObjectByName("goodsNode", elements);
        validateSchemaObjectByName("baseNode", elements);
        validateSchemaObjectByName("businessExceptionType", elements);
        elements = getDirectChildElementsOnly(XSD_SIMPLE_TYPE, wsdl.getWsdlSchema());
        assertEquals(2, elements.size());
        validateSchemaObjectByName("code", elements);
        validateSchemaObjectByName("baseAttributeType", elements);
        elements = getDirectChildElementsOnly(XSD_ATTRIBUTE, wsdl.getWsdlSchema());
        assertEquals(1, elements.size());
        validateSchemaObjectByName("baseAttribute", elements);
    }

    @Test
    public void testExtractSelectedServicesDocLiteral() throws Exception {
        WsdlExtractor extractor = new WsdlExtractor(createWsdlFromClasspathResource("document-literal.wsdl"));

        Map<String, Wsdl> wsdls = extractor.extractServices("service1", "service3");

        assertEquals(2, wsdls.size());
        validateService1(wsdls.get("service1"));
        validateService3(wsdls.get("service3"));
    }

    @Test
    public void testExtractSelectedServicesIntoSingleWsdlDocLiteral() throws Exception {
        WsdlExtractor extractor = new WsdlExtractor(createWsdlFromClasspathResource("document-literal.wsdl"));

        Map<String, Wsdl> wsdls = extractor.extractServices("service1*", "service3*");

        assertEquals(1, wsdls.size());
        validateService1AndService3(wsdls.get("services"));
    }

    private void validateService1AndService3(Wsdl wsdl) {
        assertEquals(2, wsdl.getWsdlBindingOperations().size());
        validateBindingOperationName("service1", wsdl);
        validateBindingOperationName("service3", wsdl);
        assertEquals(2, wsdl.getWsdlPortTypeOperations().size());
        validatePortTypeOperationName("service1", wsdl);
        validatePortTypeOperationName("service3", wsdl);
        assertEquals(6, wsdl.getWsdlMessages().size());
        validateMessageByName("service1", wsdl.getWsdlMessages());
        validateMessageByName("service3", wsdl.getWsdlMessages());
        validateMessageByName("service1Response", wsdl.getWsdlMessages());
        validateMessageByName("service3Response", wsdl.getWsdlMessages());
        validateMessageByName("xrdHeader", wsdl.getWsdlMessages());
        validateMessageByName("businessException", wsdl.getWsdlMessages());
        List<Element> elements = getDirectChildElementsOnly(XSD_ELEMENT, wsdl.getWsdlSchema());
        assertEquals(6, elements.size());
        validateSchemaObjectByName("service1", elements);
        validateSchemaObjectByName("service1Response", elements);
        validateSchemaObjectByName("service3", elements);
        validateSchemaObjectByName("service3Response", elements);
        validateSchemaObjectByName("goods", elements);
        validateSchemaObjectByName("businessException", elements);
        elements = getDirectChildElementsOnly(XSD_COMPLEX_TYPE, wsdl.getWsdlSchema());
        assertEquals(6, elements.size());
        validateSchemaObjectByName("service3RequestType", elements);
        validateSchemaObjectByName("service3ResponseType", elements);
        validateSchemaObjectByName("service3GoodsNode", elements);
        validateSchemaObjectByName("goodsNode", elements);
        validateSchemaObjectByName("baseNode", elements);
        validateSchemaObjectByName("businessExceptionType", elements);
        elements = getDirectChildElementsOnly(XSD_SIMPLE_TYPE, wsdl.getWsdlSchema());
        assertEquals(2, elements.size());
        validateSchemaObjectByName("code", elements);
        validateSchemaObjectByName("baseAttributeType", elements);
        elements = getDirectChildElementsOnly(XSD_ATTRIBUTE, wsdl.getWsdlSchema());
        assertEquals(1, elements.size());
        validateSchemaObjectByName("baseAttribute", elements);
    }

    @Test
    public void testExtractAllServicesRpcEncoded() throws Exception {
        WsdlExtractor extractor = new WsdlExtractor(createWsdlFromClasspathResource("rpc-encoded.wsdl"));

        Map<String, Wsdl> wsdls = extractor.extractServices();

        assertEquals(3, wsdls.size());
        validateService1RpcEncoded(wsdls.get("service1"));
        validateService2RpcEncoded(wsdls.get("service2"));
        validateService3RpcEncoded(wsdls.get("service3"));
    }

    private void validateService1RpcEncoded(Wsdl wsdl) {
        assertEquals(1, wsdl.getWsdlBindingOperations().size());
        validateBindingOperationName("service1", wsdl);
        assertEquals(1, wsdl.getWsdlPortTypeOperations().size());
        validatePortTypeOperationName("service1", wsdl);
        assertEquals(3, wsdl.getWsdlMessages().size());
        validateMessageByName("service1", wsdl.getWsdlMessages());
        validateMessageByName("service1Response", wsdl.getWsdlMessages());
        validateMessageByName("xteehdr", wsdl.getWsdlMessages());
        List<Element> elements = getDirectChildElementsOnly(XSD_COMPLEX_TYPE, wsdl.getWsdlSchema());
        assertEquals(2, elements.size());
        validateSchemaObjectByName("service1RequestType", elements);
        validateSchemaObjectByName("service1ResponseType", elements);
        assertTrue(getDirectChildElementsOnly(XSD_ELEMENT, wsdl.getWsdlSchema()).isEmpty());
        assertTrue(getDirectChildElementsOnly(XSD_SIMPLE_TYPE, wsdl.getWsdlSchema()).isEmpty());
        assertTrue(getDirectChildElementsOnly(XSD_ATTRIBUTE, wsdl.getWsdlSchema()).isEmpty());
    }

    private void validateService2RpcEncoded(Wsdl wsdl) {
        assertEquals(1, wsdl.getWsdlBindingOperations().size());
        validateBindingOperationName("service2", wsdl);
        assertEquals(1, wsdl.getWsdlPortTypeOperations().size());
        validatePortTypeOperationName("service2", wsdl);
        assertEquals(3, wsdl.getWsdlMessages().size());
        validateMessageByName("service2", wsdl.getWsdlMessages());
        validateMessageByName("service2Response", wsdl.getWsdlMessages());
        validateMessageByName("xteehdr", wsdl.getWsdlMessages());
        List<Element> elements = getDirectChildElementsOnly(XSD_COMPLEX_TYPE, wsdl.getWsdlSchema());
        assertEquals(2, elements.size());
        validateSchemaObjectByName("service2RequestType", elements);
        validateSchemaObjectByName("service2ResponseType", elements);
        assertTrue(getDirectChildElementsOnly(XSD_ELEMENT, wsdl.getWsdlSchema()).isEmpty());
        assertTrue(getDirectChildElementsOnly(XSD_SIMPLE_TYPE, wsdl.getWsdlSchema()).isEmpty());
        assertTrue(getDirectChildElementsOnly(XSD_ATTRIBUTE, wsdl.getWsdlSchema()).isEmpty());
    }

    private void validateService3RpcEncoded(Wsdl wsdl) {
        assertEquals(1, wsdl.getWsdlBindingOperations().size());
        validateBindingOperationName("service3", wsdl);
        assertEquals(1, wsdl.getWsdlPortTypeOperations().size());
        validatePortTypeOperationName("service3", wsdl);
        assertEquals(3, wsdl.getWsdlMessages().size());
        validateMessageByName("service3", wsdl.getWsdlMessages());
        validateMessageByName("service3Response", wsdl.getWsdlMessages());
        validateMessageByName("xteehdr", wsdl.getWsdlMessages());
        List<Element> elements = getDirectChildElementsOnly(XSD_ELEMENT, wsdl.getWsdlSchema());
        assertEquals(1, elements.size());
        validateSchemaObjectByName("goods", elements);
        elements = getDirectChildElementsOnly(XSD_COMPLEX_TYPE, wsdl.getWsdlSchema());
        assertEquals(5, elements.size());
        validateSchemaObjectByName("service3RequestType", elements);
        validateSchemaObjectByName("service3ResponseType", elements);
        validateSchemaObjectByName("service3GoodsNode", elements);
        validateSchemaObjectByName("goodsNode", elements);
        validateSchemaObjectByName("baseNode", elements);
        elements = getDirectChildElementsOnly(XSD_SIMPLE_TYPE, wsdl.getWsdlSchema());
        assertEquals(2, elements.size());
        validateSchemaObjectByName("code", elements);
        validateSchemaObjectByName("baseAttributeType", elements);
        elements = getDirectChildElementsOnly(XSD_ATTRIBUTE, wsdl.getWsdlSchema());
        assertEquals(1, elements.size());
        validateSchemaObjectByName("baseAttribute", elements);
    }

    @Test
    public void testExtractSelectedServicesRpcEncoded() throws Exception {
        WsdlExtractor extractor = new WsdlExtractor(createWsdlFromClasspathResource("rpc-encoded.wsdl"));

        Map<String, Wsdl> wsdls = extractor.extractServices("service1", "service3");

        assertEquals(2, wsdls.size());
        validateService1RpcEncoded(wsdls.get("service1"));
        validateService3RpcEncoded(wsdls.get("service3"));
    }

    @Test
    public void testExtractSelectedServicesIntoSingleWsdlRpcEncoded() throws Exception {
        WsdlExtractor extractor = new WsdlExtractor(createWsdlFromClasspathResource("rpc-encoded.wsdl"));

        Map<String, Wsdl> wsdls = extractor.extractServices("service1*", "service3*");

        assertEquals(1, wsdls.size());
        validateService1AndService3RpcEncoded(wsdls.get("services"));
    }

    private void validateService1AndService3RpcEncoded(Wsdl wsdl) {
        assertEquals(2, wsdl.getWsdlBindingOperations().size());
        validateBindingOperationName("service1", wsdl);
        validateBindingOperationName("service3", wsdl);
        assertEquals(2, wsdl.getWsdlPortTypeOperations().size());
        validatePortTypeOperationName("service1", wsdl);
        validatePortTypeOperationName("service3", wsdl);
        assertEquals(5, wsdl.getWsdlMessages().size());
        validateMessageByName("service1", wsdl.getWsdlMessages());
        validateMessageByName("service3", wsdl.getWsdlMessages());
        validateMessageByName("service1Response", wsdl.getWsdlMessages());
        validateMessageByName("service3Response", wsdl.getWsdlMessages());
        validateMessageByName("xteehdr", wsdl.getWsdlMessages());
        List<Element> elements = getDirectChildElementsOnly(XSD_ELEMENT, wsdl.getWsdlSchema());
        assertEquals(1, elements.size());
        validateSchemaObjectByName("goods", elements);
        elements = getDirectChildElementsOnly(XSD_COMPLEX_TYPE, wsdl.getWsdlSchema());
        assertEquals(7, elements.size());
        validateSchemaObjectByName("service1RequestType", elements);
        validateSchemaObjectByName("service1ResponseType", elements);
        validateSchemaObjectByName("service3RequestType", elements);
        validateSchemaObjectByName("service3ResponseType", elements);
        validateSchemaObjectByName("service3GoodsNode", elements);
        validateSchemaObjectByName("goodsNode", elements);
        validateSchemaObjectByName("baseNode", elements);
        elements = getDirectChildElementsOnly(XSD_SIMPLE_TYPE, wsdl.getWsdlSchema());
        assertEquals(2, elements.size());
        validateSchemaObjectByName("code", elements);
        validateSchemaObjectByName("baseAttributeType", elements);
        elements = getDirectChildElementsOnly(XSD_ATTRIBUTE, wsdl.getWsdlSchema());
        assertEquals(1, elements.size());
        validateSchemaObjectByName("baseAttribute", elements);
    }
}
