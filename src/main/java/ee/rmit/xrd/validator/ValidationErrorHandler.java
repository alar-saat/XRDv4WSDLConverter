package ee.rmit.xrd.validator;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

import java.util.List;

public interface ValidationErrorHandler extends ErrorHandler {

    List<SAXParseException> getErrors();

    boolean hasWarning();
}
