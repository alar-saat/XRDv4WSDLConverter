package ee.rmit.xrd;

import ee.rmit.xrd.style.XrdV4WsdlStyleInspector;
import ee.rmit.xrd.validator.XrdV4WsdlValidator;
import ee.rmit.xrd.wsdl.Wsdl;
import ee.rmit.xrd.wsdl.WsdlAppender;

import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static ee.rmit.xrd.utils.DomUtils.serializeFormattedDomSource;
import static ee.rmit.xrd.utils.FileUtils.listFiles;
import static ee.rmit.xrd.utils.LoggerUtils.*;
import static ee.rmit.xrd.utils.StringUtils.isBlank;

public class XrdV4WsdlAssembler extends AbstractConfiguration {
    /* filtered wsdl files for assembling into main wsdl */
    private List<Path> wsdlFiles = new ArrayList<>();

    public XrdV4WsdlAssembler(Path wsdlDir) {
        if (wsdlDir == null) {
            throw new IllegalStateException("WSDL directory undefined");
        }
        if (!Files.isDirectory(wsdlDir)) {
            throw new IllegalStateException("Not a directory: " + wsdlDir);
        }
        this.wsdlDir = wsdlDir;
    }

    public XrdV4WsdlAssembler(String wsdlDir) {
        this(Paths.get(wsdlDir));
    }

    /**
     * <pre>
     *     glob = undefined or empty            -> *.{wsdl,Wsdl,WSDL}
     *     glob = myWsdlFile1.wsdl              -> myWsdlFile1.wsdl
     *     glob = {myWsdlFile1,myWsdlFile2}.*   -> myWsdlFile1.wsdl, myWsdlFile2.WSDL
     * </pre>
     */
    public void assembleWsdlFiles(String glob) {
        long start = System.currentTimeMillis();
        try {
            logConfig();
            setUpWsdlTemplate();
            setUpWsdlFiles(glob);
            WsdlAppender appender = new WsdlAppender(template.getTemplate());
            logInfo("WSDL files to assemble: " + wsdlFiles.size());
            for (Path wsdlFile : wsdlFiles) {
                //init wsdl
                logInfo(String.format("WSDL file to append '%s'", wsdlFile));
                Wsdl wsdl = new Wsdl(wsdlFile);

                //validate
                XrdV4WsdlValidator validator = new XrdV4WsdlValidator(wsdl, wsdlDir);
                validator.validate();

                //check style of the valid document
                XrdV4WsdlStyleInspector styleInspector = new XrdV4WsdlStyleInspector(wsdl, template.getTemplate());
                styleInspector.checkStyle();

                //add wsdl to the template
                appender.appendWsdl(wsdl);
            }
            Path finalWsdl = wsdlDir.resolve(template.getProducerName() + ".wsdl");
            try (FileOutputStream fos = new FileOutputStream(finalWsdl.toFile())) {
                serializeFormattedDomSource(new DOMSource(template.getTemplate().getDocumentElement()), new StreamResult(fos));
                logInfo(String.format("Assembled WSDL file '%s'", finalWsdl));
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Cannot assemble WSDL files", e);
        } finally {
            setJaxpDebuggingOff();
            logStopWatch("Assembling of the WSDL files finished", start);
        }
    }

    private void setUpWsdlFiles(String glob) {
        try {
            if (isBlank(glob)) {
                glob = "*.{wsdl,Wsdl,WSDL}";
            }
            logInfo(String.format("Searching for WSDL files in directory '%s' and using search filter '%s'", wsdlDir, glob));
            wsdlFiles = listFiles(wsdlDir, glob);
            for (Iterator<Path> it = wsdlFiles.iterator(); it.hasNext(); ) {
                Path wsdlFile = it.next();
                if (isAssembledWsdlFile(wsdlFile)) {
                    logInfo(String.format("Removing assembled WSDL file '%s'", wsdlFile));
                    it.remove();
                }
            }
            Collections.sort(wsdlFiles);
            if (wsdlFiles.isEmpty()) {
                throw new IllegalStateException("WSDL files to assemble not found");
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot set up WSDL files", e);
        }
    }

    private boolean isAssembledWsdlFile(Path wsdlFile) {
        return wsdlFile.getFileName() != null && wsdlFile.getFileName().toString().startsWith(template.getProducerName());
    }

    public void assembleWsdlFiles() {
        assembleWsdlFiles(null);
    }

    public void assembleWsdlFilesNoValidation(String glob) {
        long start = System.currentTimeMillis();
        try {
            logConfig();
            setUpWsdlTemplate();
            setUpWsdlFiles(glob);
            WsdlAppender appender = new WsdlAppender(template.getTemplate());
            logInfo("WSDL files to assemble: " + wsdlFiles.size());
            for (Path wsdlFile : wsdlFiles) {
                //init wsdl
                logInfo(String.format("WSDL file to append '%s'", wsdlFile));
                Wsdl wsdl = new Wsdl(wsdlFile);

                //add wsdl to the template
                appender.appendWsdl(wsdl);
            }
            Path finalWsdl = wsdlDir.resolve(template.getProducerName() + ".wsdl");
            try (FileOutputStream fos = new FileOutputStream(finalWsdl.toFile())) {
                serializeFormattedDomSource(new DOMSource(template.getTemplate().getDocumentElement()), new StreamResult(fos));
                logInfo(String.format("Assembled WSDL file '%s'", finalWsdl));
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Cannot assemble WSDL files", e);
        } finally {
            setJaxpDebuggingOff();
            logStopWatch("Assembling of the WSDL files finished", start);
        }
    }

    public void assembleWsdlFilesNoValidation() {
        assembleWsdlFilesNoValidation(null);
    }

    public static void main(String[] args) {
        try {
            logInfo("XrdV4WsdlAssembler main args: " + Arrays.toString(args));
            if (args.length != 4) {
                throw new IllegalArgumentException("Missing argument(s). 4 required, got " + args.length);
            }
            Path wsdlInputDir = Paths.get(args[0]);
            if (!Files.isDirectory(wsdlInputDir)) {
                throw new IllegalArgumentException("Invalid WSDL files dir: " + args[0]);
            }
            XrdV4WsdlAssembler assembler = new XrdV4WsdlAssembler(wsdlInputDir);

            String wsdlTemplate = args[1];
            if (!wsdlTemplate.trim().isEmpty()) {
                assembler.setTemplateLocation(wsdlTemplate);
            }

            String producerName = args[3];
            if (!producerName.trim().isEmpty()) {
                assembler.setProducerName(producerName);
            }

            String glob = args[2];
            if (glob.trim().isEmpty()) {
                assembler.assembleWsdlFiles();
            } else {
                assembler.assembleWsdlFiles(glob);
            }
        } catch (IllegalArgumentException e) {
            logError(e.getMessage());
        } catch (Exception e) {
            logError(null, e);
        }
    }
}
