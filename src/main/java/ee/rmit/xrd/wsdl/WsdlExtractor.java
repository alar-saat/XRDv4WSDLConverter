package ee.rmit.xrd.wsdl;

import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static ee.rmit.xrd.utils.DomUtils.getRequiredAttributeValue;
import static ee.rmit.xrd.utils.LoggerUtils.logInfo;
import static ee.rmit.xrd.utils.WsdlUtils.createTemplate;

public class WsdlExtractor {
    private Wsdl wsdl;

    public WsdlExtractor(Wsdl wsdl) {
        this.wsdl = wsdl;
    }

    /**
     * <pre>
     *     serviceNames = undefined or empty    -> all services into separate wsdls
     *     serviceNames = service1, service3    -> only these services are extraced into separate wsdls
     *     serviceNames = service1*, service3*  -> only these services are extraced into single wsdl
     * </pre>
     */
    public Map<String, Wsdl> extractServices(String... serviceNames) {
        boolean isSingleWsdlCase = isSingleWsdlCase(serviceNames);
        List<String> services = createServiceListNoAsteriskInName(serviceNames);
        Map<String, Wsdl> extractedServices = new TreeMap<>();
        Wsdl template = null;
        if (isSingleWsdlCase) {
            template = createTemplate(wsdl);
            extractedServices.put("services", template);
        }
        for (String service : services) {
            if (!isSingleWsdlCase) {
                template = createTemplate(wsdl);
                extractedServices.put(service, template);
            }
            WsdlService wsdlService = new WsdlService(service, wsdl);
            addSchemaElements(wsdlService.getSchemaObjects(), template);
            addAbstractMessages(wsdlService.getWsdlMessages(), template);
            addPortTypeOperation(wsdlService.getPortTypeOperation(), template);
            addBindingOperation(wsdlService.getBindingOperation(), template);
            logInfo(String.format("Service '%s' extracted", service));
        }
        return extractedServices;
    }

    private boolean isSingleWsdlCase(String... serviceNames) {
        if (serviceNames == null || serviceNames.length == 0) {
            return false;
        }
        for (String serviceName : serviceNames) {
            if (!serviceName.endsWith("*")) {
                return false;
            }
        }
        return true;
    }

    private List<String> createServiceListNoAsteriskInName(String... serviceNames) {
        List<String> services = new ArrayList<>();
        if (serviceNames != null && serviceNames.length > 0 && !serviceNames[0].trim().isEmpty()) {
            for (String serviceName : serviceNames) {
                if (serviceName.endsWith("*")) {
                    services.add(serviceName.substring(0, serviceName.indexOf("*")).trim());
                } else {
                    services.add(serviceName.trim());
                }
            }
        }
        //collect all services
        if (services.isEmpty()) {
            wsdl.getWsdlPortTypeOperations().forEach(elem -> services.add(getRequiredAttributeValue("name", elem)));
        }
        return services;
    }

    private void addSchemaElements(List<Element> elements, Wsdl wsdl) {
        elements.forEach(elem -> {
            if (!wsdl.containsSchemaObject(elem)) {
                Element element = (Element) wsdl.getWsdl().importNode(elem, true);
                wsdl.getWsdlSchema().appendChild(element);
            }
        });
    }

    private void addAbstractMessages(List<Element> elements, Wsdl wsdl) {
        elements.forEach(elem -> {
            if (!wsdl.containsMessage(elem)) {
                Element element = (Element) wsdl.getWsdl().importNode(elem, true);
                wsdl.getDocumentElement().insertBefore(element, wsdl.getWsdlPortType());
                wsdl.getWsdlMessages().add(element);
            }
        });
    }

    private void addPortTypeOperation(Element element, Wsdl wsdl) {
        if (!wsdl.containsPortTypeOperation(element)) {
            Element node = (Element) wsdl.getWsdl().importNode(element, true);
            wsdl.getWsdlPortType().appendChild(node);
            wsdl.getWsdlPortTypeOperations().add(node);
        }
    }

    private void addBindingOperation(Element element, Wsdl wsdl) {
        if (!wsdl.containsBindingOperation(element)) {
            Element node = (Element) wsdl.getWsdl().importNode(element, true);
            wsdl.getWsdlBinding().appendChild(node);
            wsdl.getWsdlBindingOperations().add(node);
        }
    }
}
