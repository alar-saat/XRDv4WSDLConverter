package ee.rmit.xrd.validator;

import ee.rmit.xrd.wsdl.Wsdl;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.transform.dom.DOMSource;
import java.util.List;

import static ee.rmit.xrd.utils.DomUtils.*;
import static ee.rmit.xrd.utils.LoggerUtils.logError;
import static ee.rmit.xrd.utils.StringUtils.isNotBlank;
import static ee.rmit.xrd.utils.DomUtils.getRequiredSchemaElementByNameValue;
import static ee.rmit.xrd.wsdl.QualifiedNames.WSDL_PART;

public class MessageValidator extends AbstractValidator {

    public MessageValidator(Wsdl wsdl) {
        super(wsdl);
    }

    public void validate() {
        wsdlV4.getWsdlMessages().forEach(elem -> {
            checkRequiredAttributes(elem, "name");
            List<Element> parts = getRequiredChildElements(WSDL_PART, elem);
            parts.forEach(part -> {
                try {
                    checkRequiredAttributes(part, "name");
                    String elementValue = getOptionalElementAttributeValue(part);
                    if (elementValue == null) {
                        String typeValue = getOptionalTypeAttributeValue(part);
                        if (typeValue == null) {
                            throw new IllegalStateException(String.format("Abstract message '%s' part '%s' " +
                                    "without required attribute: 'element' or 'type'", elem.getAttribute("name"), part.getAttribute("name")));
                        }
                    } else {
                        String[] splitted = elementValue.split(":");
                        String prefix = splitted[0];
                        String value = splitted[1];
                        //check schema reference only when target namespace prefix
                        if (prefix.equals(wsdlV4.getTargetNamespacePrefix())) {
                            //just check presence
                            getRequiredSchemaElementByNameValue(value, wsdlV4.getWsdlSchema());
                        }
                    }
                } catch (RuntimeException e) {
                    logError("WSDL 'message' containing an error: " + formatDomSource(new DOMSource(elem)));
                    throw e;
                }
            });
        });
    }

    private String getOptionalElementAttributeValue(Element element) {
        Node elementAttibute = getAttributeOrNull("element", element);
        if (elementAttibute != null) {
            String value = elementAttibute.getTextContent();
            if (isNotBlank(value)) {
                checkQualifiedAttributeForAnyNamespace(value);
                checkNotRequiredAttributes(element, "type");
                return value;
            }
            throw new IllegalStateException(String.format("{%s}%s without text content"
                    , element.getLocalName(), elementAttibute.getLocalName()));
        }
        return null;
    }

    private String getOptionalTypeAttributeValue(Element element) {
        Node typeAttibute = getAttributeOrNull("type", element);
        if (typeAttibute != null) {
            String value = typeAttibute.getTextContent();
            if (isNotBlank(value)) {
                checkQualifiedAttributeForXsdNamespace(value);
                checkNotRequiredAttributes(element, "element");
                return value;
            }
            throw new IllegalStateException(String.format("{%s}%s without text content"
                    , element.getLocalName(), typeAttibute.getLocalName()));
        }
        return null;
    }
}
