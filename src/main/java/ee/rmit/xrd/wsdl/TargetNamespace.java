package ee.rmit.xrd.wsdl;

import javax.xml.namespace.QName;

public class TargetNamespace {
    private String targetNamespace;
    private QName tns;

    public TargetNamespace(String targetNamespace, QName tns) {
        this.targetNamespace = targetNamespace;
        this.tns = tns;
    }

    public String getTargetNamespace() {
        return targetNamespace;
    }

    public QName getTns() {
        return tns;
    }

    public boolean isTargetNamespacePrefix(String prefix) {
        return prefix.equals(tns.getLocalPart());
    }

    public String getTargetNamespacePrefix() {
        return tns.getLocalPart();
    }

    @Override
    public String toString() {
        return "TargetNamespace{" +
                "targetNamespace='" + targetNamespace + '\'' +
                ", tns=" + tns +
                '}';
    }
}
