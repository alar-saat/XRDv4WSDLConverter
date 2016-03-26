package ee.rmit.xrd.wsdl;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;

import static ee.rmit.xrd.utils.DomUtils.*;
import static ee.rmit.xrd.utils.LoggerUtils.logInfo;
import static ee.rmit.xrd.utils.StringUtils.isBlank;

public class WsdlTemplate {
    private String producerName = "producer";
    private Wsdl template;

    public WsdlTemplate(String templateLocation, String producerName) {
        if (producerName != null && producerName.trim().length() >= 3) {
            this.producerName = producerName.trim();
        }
        setUpWsdlTemplate(templateLocation);
    }

    public String getProducerName() {
        return producerName;
    }

    public Wsdl getTemplate() {
        return template;
    }

    private void setUpWsdlTemplate(String templateLocation) {
        try {
            URL url = null;
            if (isBlank(templateLocation)) {
                logInfo("Using default 'template.wsdl' as classpath resource");
                url = Thread.currentThread().getContextClassLoader().getResource("template.wsdl");
                if (url == null) {
                    throw new IllegalStateException("Classpath resource 'template.wsdl' not found");
                }
            } else if (templateLocation.startsWith("classpath://")) {
                String resourceName = templateLocation.substring(12);
                logInfo(String.format("Using custom '%s' as classpath resource", resourceName));
                url = Thread.currentThread().getContextClassLoader().getResource(resourceName);
                if (url == null) {
                    throw new IllegalStateException(String.format("Classpath resource '%s' not found", resourceName));
                }
            } else if (templateLocation.startsWith("http://") || templateLocation.startsWith("https://")) {
                logInfo(String.format("Using HTTP resource '%s' as template", templateLocation));
                url = new URL(templateLocation);
            } else {
                logInfo(String.format("Using file '%s' as template", templateLocation));
                Path templateFile = Paths.get(templateLocation);
                if (!Files.isRegularFile(templateFile)) {
                    throw new IllegalStateException(String.format("Not a reqular file '%s'", templateLocation));
                }
                url = templateFile.toUri().toURL();
            }
            template = new Wsdl(parseDocument(url));
            changeProducerName();
        } catch (SAXException | ParserConfigurationException | IOException e) {
            throw new IllegalStateException("Cannot create template document", e);
        }
    }

    private void changeProducerName() {
        if (template.getTargetNamespaceUri().equals(String.format("http://%s.x-road.eu", producerName))) {
            logInfo(String.format("Replacing the producer name in target namespace not required. Target namespace '%s'"
                    , template.getTargetNamespaceUri()));
            return;
        }
        String newTargetNamespace = template.getTargetNamespaceUri().replaceFirst("producer", producerName);
        QName newTns = new QName(newTargetNamespace, "tns", "");

        //change in templateTargetNamespace
        template.setTargetNamespace(new TargetNamespace(newTargetNamespace, newTns));

        //change in templateNamespaces
        for (Iterator<QName> it = template.getNamespaces().iterator(); it.hasNext(); ) {
            QName qName = it.next();
            if (qName.getLocalPart().equals("tns")) {
                it.remove();
                break;
            }
        }
        template.getNamespaces().add(newTns);

        //change in root document -> wsdl:definitions
        Element root = template.getDocumentElement();
        Node ns = getRequiredNamespaceDeclaration("tns", root);
        ns.setTextContent(newTargetNamespace);

        //change in attribute targetNamespace
        Node targetNamespace = getRequiredAttribute("targetNamespace", root);
        targetNamespace.setTextContent(newTargetNamespace);

        //change in schemas -> wsdl:types -> schema
        Node node = getRequiredAttribute("targetNamespace", template.getWsdlSchema());
        node.setTextContent(newTargetNamespace);
        logInfo(String.format("Target namespace replaced. New target namespace '%s'", newTargetNamespace));
    }
}
