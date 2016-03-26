package ee.rmit.xrd.validator;

import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.util.ArrayList;
import java.util.List;

import static ee.rmit.xrd.utils.LoggerUtils.logError;
import static ee.rmit.xrd.utils.LoggerUtils.logWarning;

public class DefaultValidationErrorHandler implements ValidationErrorHandler {
    private boolean warning = false;
    private List<SAXParseException> errors = new ArrayList<SAXParseException>();

    @Override
    public List<SAXParseException> getErrors() {
        return errors;
    }

    @Override
    public boolean hasWarning() {
        return warning;
    }

    @Override
    public void warning(SAXParseException ex) throws SAXException {
        warning = true;
        logWarning(String.format("%d:%d - %s", ex.getLineNumber(), ex.getColumnNumber(), ex.getMessage()));
    }

    @Override
    public void error(SAXParseException ex) throws SAXException {
        errors.add(ex);
        logError(String.format("%d:%d - %s", ex.getLineNumber(), ex.getColumnNumber(), ex.getMessage()));
    }

    @Override
    public void fatalError(SAXParseException ex) throws SAXException {
        errors.add(ex);
        logError(ex.getMessage());
    }
}
