package ee.rmit.xrd.wsdl;

public class WsdlMessagePart {
    private PartAttributeType attributeType;
    private Attribute value;

    public WsdlMessagePart(PartAttributeType attributeType, Attribute value) {
        this.attributeType = attributeType;
        this.value = value;
    }

    public PartAttributeType getAttributeType() {
        return attributeType;
    }

    public Attribute getValue() {
        return value;
    }

    public String getPrefix() {
        return value.getPrefix();
    }

    public String getAttrValue() {
        return value.getValue();
    }

    @Override
    public String toString() {
        return "WsdlMessagePart{" +
                "attributeType=" + attributeType +
                ", value=" + value +
                '}';
    }
}
