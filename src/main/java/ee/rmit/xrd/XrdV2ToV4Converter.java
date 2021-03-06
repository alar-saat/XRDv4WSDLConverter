package ee.rmit.xrd;

import ee.rmit.xrd.converter.V2ToV4Converter;
import ee.rmit.xrd.wsdl.Wsdl;
import ee.rmit.xrd.wsdl.WsdlAppender;
import ee.rmit.xrd.wsdl.WsdlExtractor;

import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;

import static ee.rmit.xrd.utils.DomUtils.serializeFormattedDomSource;
import static ee.rmit.xrd.utils.LoggerUtils.*;
import static ee.rmit.xrd.utils.WsdlUtils.copyWsdlDeep;

public class XrdV2ToV4Converter extends AbstractConfiguration {
    /* v2 wsdl for conversion */
    private Wsdl wsdl;

    public XrdV2ToV4Converter(String wsdlFile) {
        wsdl = createWsdl(wsdlFile);
    }

    /* useWrapperElements -> use wrapper elements like 'keha' and 'paring' mandatory in xrd v2 message */
    public void convert(boolean useWrapperElements) {
        long start = System.currentTimeMillis();
        try {
            logConfig();
            createWsdlDirIfNotSet();
            setUpWsdlTemplate();
            logInfo(String.format("Converting XRD v2 WSDL '%s' to XRD v4. Wrapper elements (keha/paring) required: %s"
                    , wsdlFileLocation, useWrapperElements));

            Wsdl wsdlV4Main = copyWsdlDeep(template.getTemplate());
            WsdlAppender appender = new WsdlAppender(wsdlV4Main);
            WsdlExtractor extractor = new WsdlExtractor(wsdl);
            Map<String, Wsdl> wsdls = extractor.extractServices();
            logInfo(String.format("Services found in WSDL: %d", wsdls.size()));
            for (Map.Entry<String, Wsdl> entry : wsdls.entrySet()) {
                String name = entry.getKey();
                Wsdl wsdlV2 = entry.getValue();
                if (wsdlV2.containsPortTypeOperation("listMethods")) {
                    logInfo("Skipping meta service 'listMethods'");
                    continue;
                }
                if (wsdlV2.containsPortTypeOperation("testSystem")) {
                    logInfo("Skipping meta service 'testSystem'");
                    continue;
                }
                Wsdl wsdlV4 = copyWsdlDeep(template.getTemplate());

                //convert v2 to v4
                V2ToV4Converter converter = new V2ToV4Converter(wsdlV2, wsdlV4);
                converter.convert(useWrapperElements);
                logInfo(String.format("Service '%s' converted to XRD v4", name));

                //add wsdl to the template
                appender.appendWsdl(wsdlV4);
            }
            Path fileV4Main = wsdlDir.resolve("xrd-v4.wsdl");
            try (FileOutputStream fos = new FileOutputStream(fileV4Main.toFile())) {
                serializeFormattedDomSource(new DOMSource(wsdlV4Main.getDocumentElement()), new StreamResult(fos));
            }
            logInfo(String.format("XRD v2 WSDL converted. Output file '%s'", fileV4Main));
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Cannot convert WSDL", e);
        } finally {
            setJaxpDebuggingOff();
            logStopWatch("Conversion of the WSDL finished", start);
        }
    }

    public void convert() {
        convert(true);
    }

    public static void main(String[] args) {
        try {
            logInfo("XrdV2ToV4Converter main args: " + Arrays.toString(args));
            if (args.length != 4) {
                throw new IllegalArgumentException("Missing argument(s). 4 required, got " + args.length);
            }
            Path wsdlOutputDir = Paths.get(args[0]);
            if (!Files.isDirectory(wsdlOutputDir)) {
                throw new IllegalArgumentException("Invalid WSDL output dir: " + args[0]);
            }
            String wsdlFileIn = args[1];
            String useWrapperElements = args[2];
            String producerName = args[3];
            XrdV2ToV4Converter converter = new XrdV2ToV4Converter(wsdlFileIn);
            converter.setWsdlDir(wsdlOutputDir);
            converter.setProducerName(producerName);
            converter.convert(Boolean.valueOf(useWrapperElements));
        } catch (IllegalArgumentException e) {
            logError(e.getMessage());
        } catch (Exception e) {
            logError(null, e);
        }
    }
}
