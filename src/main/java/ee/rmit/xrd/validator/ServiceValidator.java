package ee.rmit.xrd.validator;

import ee.rmit.xrd.wsdl.Wsdl;
import org.w3c.dom.Element;

import static ee.rmit.xrd.utils.DomUtils.getRequiredChildElement;
import static ee.rmit.xrd.wsdl.QualifiedNames.WSDL_PORT;

public class ServiceValidator extends AbstractValidator {

    public ServiceValidator(Wsdl wsdl) {
        super(wsdl);
    }

    public void validate() {
        checkRequiredAttributes(wsdlV4.getWsdlService(), "name");
        Element port = getRequiredChildElement(WSDL_PORT, wsdlV4.getWsdlService());
        checkRequiredAttributes(port, "binding", "name");
        String bindingValue = port.getAttribute("binding");
        checkQualifiedAttributeForTargetNamespace(bindingValue);
        //check reference to wsdl:binding
        checkRequiredAttributeValue(bindingValue.split(":")[1], "name", wsdlV4.getWsdlBinding());
    }
}
