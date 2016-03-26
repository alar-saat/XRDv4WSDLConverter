package ee.rmit.xrd.wsdl;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.Objects;

import static ee.rmit.xrd.utils.DomUtils.getAttributeOrNull;
import static ee.rmit.xrd.utils.DomUtils.getRequiredAttribute;

public class MessagePart {
    private Element part;
    private Attribute name;
    private Attribute type;
    private Attribute element;

    public MessagePart(Element part) {
        this.part = part;
        init();
    }

    private void init() {
        Node name = getRequiredAttribute("name", part);
        this.name = new Attribute(name);
        Node type = getAttributeOrNull("type", part);
        if (type == null) {
            Node element = getRequiredAttribute("element", part);
            this.element = new Attribute(element);
        } else {
            this.type = new Attribute(type);
        }
    }

    public Element getPart() {
        return part;
    }

    public Attribute getName() {
        return name;
    }

    public Attribute getType() {
        return type;
    }

    public Attribute getElement() {
        return element;
    }

    public boolean isElement() {
        return element != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessagePart that = (MessagePart) o;
        return Objects.equals(part, that.part) &&
                Objects.equals(name, that.name) &&
                Objects.equals(type, that.type) &&
                Objects.equals(element, that.element);
    }

    @Override
    public int hashCode() {
        return Objects.hash(part, name, type, element);
    }

    @Override
    public String toString() {
        return "MessagePart{" +
                "name=" + name +
                ", type=" + type +
                ", element=" + element +
                '}';
    }
}
