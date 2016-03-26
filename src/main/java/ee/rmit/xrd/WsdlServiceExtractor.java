package ee.rmit.xrd;

import ee.rmit.xrd.wsdl.Wsdl;
import ee.rmit.xrd.wsdl.WsdlExtractor;

import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;

import static ee.rmit.xrd.utils.DomUtils.serializeFormattedDomSource;
import static ee.rmit.xrd.utils.LoggerUtils.*;

public class WsdlServiceExtractor extends AbstractConfiguration {
    /* wsdl for service extraction */
    private Wsdl wsdl;

    public WsdlServiceExtractor(String wsdlFile) {
        wsdl = createWsdl(wsdlFile);
    }

    /**
     * <pre>
     *     serviceNames = undefined or empty    -> all services into separate wsdls
     *     serviceNames = service1, service3    -> only these services are extraced into separate wsdls
     *     serviceNames = service1*, service3*  -> only these services are extraced into single wsdl
     * </pre>
     */
    public void extractServices(String... serviceNames) {
        long start = System.currentTimeMillis();
        try {
            logConfig();
            createWsdlDirIfNotSet();
            logInfo(String.format("Extracting services using search filter: %s", Arrays.toString(serviceNames)));
            WsdlExtractor extractor = new WsdlExtractor(wsdl);
            Map<String, Wsdl> wsdls = extractor.extractServices(serviceNames);
            wsdls.forEach((name, wsdl) -> {
                Path wsdlFile = wsdlDir.resolve(name + ".wsdl");
                try {
                    try (FileOutputStream fos = new FileOutputStream(wsdlFile.toFile())) {
                        serializeFormattedDomSource(new DOMSource(wsdl.getDocumentElement()), new StreamResult(fos));
                        logInfo(String.format("Service '%s' extracted to file '%s'", name, wsdlFile));
                    }
                } catch (IOException | TransformerException e) {
                    throw new IllegalStateException("Cannot save extracted service to file: " + wsdlFile, e);
                }
            });
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Cannot extract services", e);
        } finally {
            setJaxpDebuggingOff();
            logStopWatch("Extracting of the services finished", start);
        }
    }

    public static void main(String[] args) {
        try {
            logInfo("WsdlServiceExtractor main args: " + Arrays.toString(args));
            if (args.length != 3) {
                throw new IllegalArgumentException("Missing argument(s). 3 required, got " + args.length);
            }
            Path wsdlOutputDir = Paths.get(args[0]);
            if (!Files.isDirectory(wsdlOutputDir)) {
                throw new IllegalArgumentException("Invalid WSDL output dir: " + args[0]);
            }
            String wsdlFileIn = args[1];
            WsdlServiceExtractor extractor = new WsdlServiceExtractor(wsdlFileIn);
            extractor.setWsdlDir(wsdlOutputDir);
            String services = args[2];
            if (services.trim().isEmpty()) {
                extractor.extractServices("");
            } else {
                extractor.extractServices(services.split(","));
            }
        } catch (IllegalArgumentException e) {
            logError(e.getMessage());
        } catch (Exception e) {
            logError(null, e);
        }
    }
}
