package ee.rmit.xrd.wsdl;

import org.w3c.dom.Node;

import java.util.Objects;

public class Attribute {
    private Node attribute;
    private String qualifiedValue;
    private String prefix;
    private String value;
    private String name;
    private boolean qualified;

    public Attribute(Node attribute) {
        this.attribute = attribute;
        init();
    }

    private void init() {
        qualifiedValue = attribute.getTextContent();
        String[] splitted = qualifiedValue.split(":");
        if (splitted.length == 2) {
            prefix = splitted[0];
            value = splitted[1];
            qualified = true;
        } else {
            prefix = "";
            value = qualifiedValue;
        }
        name = attribute.getLocalName();
    }

    public Node getAttribute() {
        return attribute;
    }

    public String getQualifiedValue() {
        return qualifiedValue;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getValue() {
        return value;
    }

    public String getName() {
        return name;
    }

    public void setValue(String value) {
        attribute.setTextContent(value);
    }

    public boolean isQualified() {
        return qualified;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Attribute that = (Attribute) o;
        return Objects.equals(attribute, that.attribute);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(attribute);
    }

    @Override
    public String toString() {
        return "Attribute{" +
                "qualifiedValue='" + qualifiedValue + '\'' +
                ", prefix='" + prefix + '\'' +
                ", value='" + value + '\'' +
                ", name='" + name + '\'' +
                ", isQualified=" + qualified +
                '}';
    }
}
