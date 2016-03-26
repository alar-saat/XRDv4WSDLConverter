package ee.rmit.xrd.wsdl;

import javax.xml.namespace.QName;

public final class QualifiedNames {
    /* namespaces */
    public static final QName XSD_NS = new QName("http://www.w3.org/2001/XMLSchema", "xsd", "xsd");
    public static final QName XRD_NS = new QName("http://x-road.eu/xsd/xroad.xsd", "xrd", "xrd");
    public static final QName SOAP_NS = new QName("http://schemas.xmlsoap.org/wsdl/soap/", "soap", "soap");
    public static final QName MIME_NS = new QName("http://schemas.xmlsoap.org/wsdl/mime/", "mime", "mime");
    public static final QName WSDL_NS = new QName("http://schemas.xmlsoap.org/wsdl/", "wsdl", "wsdl");
    /* WSDL main elements */
    public static final QName WSDL_DEFINITIONS = new QName(WSDL_NS.getNamespaceURI(), "definitions", WSDL_NS.getPrefix());
    public static final QName WSDL_TYPES = new QName(WSDL_NS.getNamespaceURI(), "types", WSDL_NS.getPrefix());
    public static final QName WSDL_MESSAGE = new QName(WSDL_NS.getNamespaceURI(), "message", WSDL_NS.getPrefix());
    public static final QName WSDL_PORT_TYPE = new QName(WSDL_NS.getNamespaceURI(), "portType", WSDL_NS.getPrefix());
    public static final QName WSDL_BINDING = new QName(WSDL_NS.getNamespaceURI(), "binding", WSDL_NS.getPrefix());
    public static final QName WSDL_SERVICE = new QName(WSDL_NS.getNamespaceURI(), "service", WSDL_NS.getPrefix());
    /* WSDL nested elements */
    public static final QName WSDL_SCHEMA = new QName(XSD_NS.getNamespaceURI(), "schema", "");
    public static final QName WSDL_OPERATION = new QName(WSDL_NS.getNamespaceURI(), "operation", WSDL_NS.getPrefix());
    public static final QName WSDL_OUTPUT = new QName(WSDL_NS.getNamespaceURI(), "output", WSDL_NS.getPrefix());
    public static final QName WSDL_INPUT = new QName(WSDL_NS.getNamespaceURI(), "input", WSDL_NS.getPrefix());
    public static final QName WSDL_FAULT = new QName(WSDL_NS.getNamespaceURI(), "fault", WSDL_NS.getPrefix());
    public static final QName WSDL_PART = new QName(WSDL_NS.getNamespaceURI(), "part", WSDL_NS.getPrefix());
    public static final QName WSDL_PORT = new QName(WSDL_NS.getNamespaceURI(), "port", WSDL_NS.getPrefix());
    public static final QName WSDL_DOC = new QName(WSDL_NS.getNamespaceURI(), "documentation", WSDL_NS.getPrefix());
    /* MIME nested elements */
    public static final QName MIME_MULTIPART = new QName(MIME_NS.getNamespaceURI(), "multipartRelated", MIME_NS.getPrefix());
    public static final QName MIME_PART = new QName(MIME_NS.getNamespaceURI(), "part", MIME_NS.getPrefix());
    public static final QName MIME_CONTENT = new QName(MIME_NS.getNamespaceURI(), "content", MIME_NS.getPrefix());
    /* SOAP nested elements */
    public static final QName SOAP_OPERATION = new QName(SOAP_NS.getNamespaceURI(), "operation", SOAP_NS.getPrefix());
    public static final QName SOAP_HEADER = new QName(SOAP_NS.getNamespaceURI(), "header", SOAP_NS.getPrefix());
    public static final QName SOAP_BODY = new QName(SOAP_NS.getNamespaceURI(), "body", SOAP_NS.getPrefix());
    public static final QName SOAP_FAULT = new QName(SOAP_NS.getNamespaceURI(), "fault", SOAP_NS.getPrefix());
    public static final QName SOAP_ADDRESS = new QName(SOAP_NS.getNamespaceURI(), "address", SOAP_NS.getPrefix());
    public static final QName SOAP_BINDING = new QName(SOAP_NS.getNamespaceURI(), "binding", SOAP_NS.getPrefix());
    /* XRD nested elements */
    public static final QName XRD_TITLE = new QName(XRD_NS.getNamespaceURI(), "title", XRD_NS.getPrefix());
    public static final QName XRD_NOTES = new QName(XRD_NS.getNamespaceURI(), "notes", XRD_NS.getPrefix());
    public static final QName XRD_TECH_NOTES = new QName(XRD_NS.getNamespaceURI(), "techNotes", XRD_NS.getPrefix());
    public static final QName XRD_VERSION = new QName(XRD_NS.getNamespaceURI(), "version", XRD_NS.getPrefix());
    public static final QName XRD_CLIENT = new QName(XRD_NS.getNamespaceURI(), "client", XRD_NS.getPrefix());
    public static final QName XRD_SERVICE = new QName(XRD_NS.getNamespaceURI(), "service", XRD_NS.getPrefix());
    public static final QName XRD_ID = new QName(XRD_NS.getNamespaceURI(), "id", XRD_NS.getPrefix());
    public static final QName XRD_PROTO_VER = new QName(XRD_NS.getNamespaceURI(), "protocolVersion", XRD_NS.getPrefix());
    public static final QName XRD_USER_ID = new QName(XRD_NS.getNamespaceURI(), "userId", XRD_NS.getPrefix());
    public static final QName XRD_ISSUE = new QName(XRD_NS.getNamespaceURI(), "issue", XRD_NS.getPrefix());
    /* XSD nested elements */
    public static final QName XSD_ELEMENT = new QName(XSD_NS.getNamespaceURI(), "element", XSD_NS.getPrefix());
    public static final QName XSD_COMPLEX_TYPE = new QName(XSD_NS.getNamespaceURI(), "complexType", XSD_NS.getPrefix());
    public static final QName XSD_SIMPLE_TYPE = new QName(XSD_NS.getNamespaceURI(), "simpleType", XSD_NS.getPrefix());
    public static final QName XSD_ATTRIBUTE = new QName(XSD_NS.getNamespaceURI(), "attribute", XSD_NS.getPrefix());
    public static final QName XSD_ATTRIBUTE_GROUP = new QName(XSD_NS.getNamespaceURI(), "attributeGroup", XSD_NS.getPrefix());
    public static final QName XSD_EXTENSION = new QName(XSD_NS.getNamespaceURI(), "extension", XSD_NS.getPrefix());
    public static final QName XSD_RESTRICTION = new QName(XSD_NS.getNamespaceURI(), "restriction", XSD_NS.getPrefix());

    private QualifiedNames() {
    }
}
