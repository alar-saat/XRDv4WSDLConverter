package ee.rmit.xrd.validator;

import ee.rmit.xrd.wsdl.Wsdl;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

import javax.xml.namespace.QName;
import java.util.Optional;

import static ee.rmit.xrd.utils.StringUtils.isBlank;
import static ee.rmit.xrd.utils.StringUtils.isNotBlank;
import static ee.rmit.xrd.wsdl.QualifiedNames.XRD_NS;
import static ee.rmit.xrd.wsdl.QualifiedNames.XSD_NS;

public abstract class AbstractValidator {
    protected Wsdl wsdlV4;

    public AbstractValidator(Wsdl wsdlV4) {
        this.wsdlV4 = wsdlV4;
    }

    public abstract void validate();

    protected void checkRequiredAttributes(Element element, String... attributes) {
        for (String attribute : attributes) {
            if (!element.hasAttribute(attribute)) {
                throw new IllegalStateException(String.format("Required attribute {%s}%s not found"
                        , element.getLocalName(), attribute));
            }
        }
    }

    protected void checkRequiredAttributesWithExactCount(Element element, String... attributes) {
        checkRequiredAttributes(element, attributes);
        NamedNodeMap map = element.getAttributes();
        if (map.getLength() != attributes.length) {
            throw new IllegalStateException(String.format("Attributes count mismatch for element '%s'. Required %d got %d"
                    , element.getLocalName(), attributes.length, map.getLength()));
        }
    }

    protected void checkNotRequiredAttributes(Element element, String... attributes) {
        for (String attribute : attributes) {
            if (element.hasAttribute(attribute)) {
                throw new IllegalStateException(String.format("Not required attribute {%s}%s found"
                        , element.getLocalName(), attribute));
            }
        }
    }

    protected void checkQualifiedAttributeForTargetNamespace(String attributeValue) {
        String[] splitted = attributeValue.split(":");
        String prefix = splitted[0];
        if (splitted.length != 2 || !prefix.equals(wsdlV4.getTargetNamespacePrefix())) {
            throw new IllegalStateException(String.format("This attribute value '%s' does not belong to target namespace %s"
                    , attributeValue, wsdlV4.getTargetNamespace().getTns()));
        }
    }

    protected void checkQualifiedAttributeForXrdNamespace(String attributeValue) {
        String[] splitted = attributeValue.split(":");
        String prefix = splitted[0];
        Optional<QName> xrdQName =
                wsdlV4.getNamespaces().stream().filter(qName -> qName.getNamespaceURI().equals(XRD_NS.getNamespaceURI())).findFirst();
        if (splitted.length != 2 || !xrdQName.isPresent() || !prefix.equals(xrdQName.get().getLocalPart())) {
            throw new IllegalStateException(String.format("This attribute value '%s' does not belong to XRD namespace %s"
                    , attributeValue, XRD_NS));
        }
    }

    protected void checkQualifiedAttributeForAnyNamespace(String attributeValue) {
        String[] splitted = attributeValue.split(":");
        String prefix = splitted[0];
        if (splitted.length != 2
                || wsdlV4.getNamespaces().stream().filter(qName -> qName.getPrefix().equals(prefix)).findAny().isPresent()) {
            throw new IllegalStateException(String.format("This attribute value '%s' does not belong to any namespace", attributeValue));
        }
    }

    protected void checkQualifiedAttributeForXsdNamespace(String attributeValue) {
        String[] splitted = attributeValue.split(":");
        String prefix = splitted[0];
        Optional<QName> xrdQName =
                wsdlV4.getNamespaces().stream().filter(qName -> qName.getNamespaceURI().equals(XSD_NS.getNamespaceURI())).findFirst();
        if (splitted.length != 2 || !xrdQName.isPresent() || !prefix.equals(xrdQName.get().getLocalPart())) {
            throw new IllegalStateException(String.format("This attribute value '%s' does not belong to XSD namespace %s"
                    , attributeValue, XSD_NS));
        }
    }

    protected void checkRequiredAttributeValue(String expected, String attribute, Element element) {
        String actual = element.getAttribute(attribute);
        if (isBlank(actual) || !actual.equals(expected)) {
            throw new IllegalStateException(String.format("Invalid attribute '%s' value '%s' for element '%s'. Expected value '%s'"
                    , attribute, actual, element.getLocalName(), expected));
        }
    }

    protected void checkOptionalAttributeValue(String expected, String attribute, Element element) {
        String actual = element.getAttribute(attribute);
        if (isNotBlank(actual) && !actual.equals(expected)) {
            throw new IllegalStateException(String.format("Invalid attribute '%s' value '%s' for element '%s'. Expected value '%s'"
                    , attribute, actual, element.getLocalName(), expected));
        }
    }
}
