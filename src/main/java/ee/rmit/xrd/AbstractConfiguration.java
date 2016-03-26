package ee.rmit.xrd;

import ee.rmit.xrd.wsdl.Wsdl;
import ee.rmit.xrd.wsdl.WsdlTemplate;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static ee.rmit.xrd.utils.DomUtils.getDocumentBuilderFactoryClassName;
import static ee.rmit.xrd.utils.DomUtils.parseDocument;
import static ee.rmit.xrd.utils.LoggerUtils.logInfo;
import static ee.rmit.xrd.utils.StringUtils.isBlank;

public abstract class AbstractConfiguration {
    /* for input/output wsdl files, if not set -> 'java.io.tmpdir' */
    protected Path wsdlDir;
    /* JAXP debugging on/off */
    protected boolean debug = false;
    /* producer name used for target namespace in wsdl document -> http://${producer}.x-road.eu */
    protected String producerName;
    /* template wsdl file location -> classpath://, http(s)://, or filesystem */
    protected String templateLocation;
    /* input wsdl file location (extractor, converter) -> classpath://, http(s)://, or filesystem */
    protected String wsdlFileLocation;
    /* constructed template object from 'producerName' and 'templateLocation' */
    protected WsdlTemplate template;

    public void setWsdlDir(Path wsdlDir) {
        this.wsdlDir = wsdlDir;
    }

    public void setJaxpDebuggingOn() {
        if (!debug) {
            System.setProperty("jaxp.debug", "1");
        }
        debug = true;
    }

    public void setJaxpDebuggingOff() {
        if (debug) {
            System.setProperty("jaxp.debug", "0");
        }
        debug = false;
    }

    public void setProducerName(String producerName) {
        this.producerName = producerName;
    }

    public void setTemplateLocation(String templateLocation) {
        this.templateLocation = templateLocation;
    }

    protected void setUpWsdlTemplate() {
        template = new WsdlTemplate(templateLocation, producerName);
    }

    protected void logConfig() throws ParserConfigurationException {
        logInfo("DOM document builder factory class: " + getDocumentBuilderFactoryClassName());
        logInfo("JAXP debug: " + (debug ? "on" : "off"));
    }

    protected Wsdl createWsdl(String wsdlFile) {
        if (isBlank(wsdlFile)) {
            throw new IllegalStateException("WSDL file location undefined");
        }
        wsdlFileLocation = wsdlFile;
        try {
            URL url = null;
            if (wsdlFile.startsWith("classpath://")) {
                String resourceName = wsdlFile.substring(12);
                logInfo(String.format("Using custom '%s' as classpath resource", resourceName));
                url = Thread.currentThread().getContextClassLoader().getResource(resourceName);
                if (url == null) {
                    throw new IllegalStateException(String.format("Classpath resource '%s' not found", resourceName));
                }
            } else if (wsdlFile.startsWith("http://") || wsdlFile.startsWith("https://")) {
                logInfo(String.format("Using HTTP resource '%s'", wsdlFile));
                url = new URL(wsdlFile);
            } else {
                logInfo(String.format("Using file '%s'", wsdlFile));
                Path templateFile = Paths.get(wsdlFile);
                if (!Files.isRegularFile(templateFile)) {
                    throw new IllegalStateException(String.format("Not a reqular file '%s'", wsdlFile));
                }
                url = templateFile.toUri().toURL();
            }
            return new Wsdl(parseDocument(url));
        } catch (IOException | ParserConfigurationException | SAXException e) {
            throw new IllegalStateException("Cannot create WSDL document", e);
        }
    }

    protected void createWsdlDirIfNotSet() {
        if (wsdlDir == null) {
            wsdlDir = Paths.get(System.getProperty("java.io.tmpdir"));
        }
        if (!Files.isDirectory(wsdlDir)) {
            try {
                Files.createDirectories(wsdlDir);
            } catch (IOException e) {
                throw new IllegalStateException("Cannot create directory for WSDL files", e);
            }
        }
        logInfo("WSDLs directory: " + wsdlDir);
    }
}
