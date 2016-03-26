package ee.rmit.xrd.validator;

import ee.rmit.xrd.wsdl.Wsdl;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

import static ee.rmit.xrd.utils.DomUtils.*;
import static ee.rmit.xrd.utils.FileUtils.*;
import static ee.rmit.xrd.utils.LoggerUtils.logError;
import static ee.rmit.xrd.utils.LoggerUtils.logWarning;
import static ee.rmit.xrd.wsdl.QualifiedNames.WSDL_OPERATION;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;

public class XrdV4WsdlValidator {
    private Wsdl wsdlV4;
    private Path outputDir;
    private Path schemaFile;

    public XrdV4WsdlValidator(Wsdl wsdlV4) {
        this(wsdlV4, Paths.get(System.getProperty("java.io.tmpdir")));
    }

    public XrdV4WsdlValidator(Wsdl wsdlV4, Path outputDir) {
        this.wsdlV4 = wsdlV4;
        this.outputDir = outputDir;
    }

    public Path getSchemaFile() {
        return schemaFile;
    }

    public void validate() {
        checkOperationsInWsdl();
        saveSchemaDocument(createDocumentFromSchema());
        Validator validator = createValidator();
        DefaultValidationErrorHandler errorHandler = new DefaultValidationErrorHandler();
        validator.setErrorHandler(errorHandler);
        try {
            validator.validate(new StreamSource(new ByteArrayInputStream(readFromFile(schemaFile))));
        } catch (SAXException | IOException e) {
            throw new IllegalStateException("Cannot validate schema file: " + schemaFile, e);
        }
        List<SAXParseException> errors = errorHandler.getErrors();
        if (!errors.isEmpty()) {
            logError("Validation finished with error(s). Check schema file: " + schemaFile);
            throw new IllegalStateException("Invalid schema in WSDL types");
        } else if (errorHandler.hasWarning()) {
            logWarning("Validation finished with warning(s). Check schema file: " + schemaFile);
        } else {
            try {
                Files.delete(schemaFile);
            } catch (IOException e) {
                logError("Cannot delete valid schema file: " + schemaFile);
            }
        }
        checkReferences();
    }

    private void checkOperationsInWsdl() {
        if (wsdlV4.getWsdlPortTypeOperations().isEmpty()) {
            throw new IllegalStateException(String.format("Required qualified element %s not found in 'portType'", WSDL_OPERATION));
        }
        if (wsdlV4.getWsdlBindingOperations().isEmpty()) {
            throw new IllegalStateException(String.format("Required qualified element %s not found in 'binding'", WSDL_OPERATION));
        }
    }

    private Validator createValidator() {
        URL schemaUrl = Thread.currentThread().getContextClassLoader().getResource("XMLSchema.xsd");
        if (schemaUrl == null) {
            throw new IllegalStateException("Classpath resource 'XMLSchema.xsd' not found");
        }
        URL xmlUrl = Thread.currentThread().getContextClassLoader().getResource("xml.xsd");
        if (xmlUrl == null) {
            throw new IllegalStateException("Classpath resource 'xml.xsd' not found");
        }
        SchemaFactory schemaFactory = SchemaFactory.newInstance(W3C_XML_SCHEMA_NS_URI);
        try {
            Source[] sources = new Source[]{new StreamSource(new ByteArrayInputStream(readFromUrl(xmlUrl)))
                    , new StreamSource(new ByteArrayInputStream(readFromUrl(schemaUrl)))};
            Schema schema = schemaFactory.newSchema(sources);
            return schema.newValidator();
        } catch (SAXException | IOException e) {
            throw new IllegalStateException("Cannot create schema", e);
        }
    }

    private void saveSchemaDocument(Document document) {
        if (outputDir == null) {
            outputDir = Paths.get(System.getProperty("java.io.tmpdir"));
        }
        schemaFile = outputDir.resolve(UUID.randomUUID().toString() + ".xsd");
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(schemaFile.toFile());
            serializeFormattedDomSource(new DOMSource(document.getDocumentElement()), new StreamResult(fos));
        } catch (TransformerException | IOException e) {
            throw new IllegalStateException("Cannot save schema file: " + schemaFile, e);
        } finally {
            closeOutputStream(fos);
        }
    }

    private Document createDocumentFromSchema() {
        try {
            Document schemaDoc = createBuilder().newDocument();
            Node importedSchema = schemaDoc.importNode(wsdlV4.getWsdlSchema(), true);
            addMissingNamespacesFromSchemaElement((Element) importedSchema);
            schemaDoc.appendChild(importedSchema);
            return schemaDoc;
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException("Cannot create schema document", e);
        }
    }

    private void addMissingNamespacesFromSchemaElement(Element schema) {
        List<QName> schemaNamespaces = getAllNamespaceDeclarations(schema);
        wsdlV4.getNamespaces().stream().filter(qName -> !schemaNamespaces.contains(qName)).forEach(qName -> addNamespace(schema, qName));
    }

    private void checkReferences() {
        ServiceValidator serviceValidator = new ServiceValidator(wsdlV4);
        serviceValidator.validate();

        BindingValidator bindingValidator = new BindingValidator(wsdlV4);
        bindingValidator.validate();

        PortTypeValidator portTypeValidator = new PortTypeValidator(wsdlV4);
        portTypeValidator.validate();

        MessageValidator messageValidator = new MessageValidator(wsdlV4);
        messageValidator.validate();
    }
}
