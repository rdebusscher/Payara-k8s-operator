package fish.payara.k8s.operator.util;

import fish.payara.k8s.operator.resource.PayaraDomainResource;

import java.util.Locale;

/**
 * Utility to determine some derived names based on the Custom resource Name.
 */
public class NamingUtil {

    private PayaraDomainResource payaraDomainResource;

    public NamingUtil(PayaraDomainResource payaraDomainResource) {
        this.payaraDomainResource = payaraDomainResource;
    }

    /**
     * Returns the DeploymentGroup Name.
     * @return
     */
    public String defineDeploymentGroupName() {
        return payaraDomainResource.getMetadata().getName().toLowerCase(Locale.ENGLISH) + "-dg";
    }

    /**
     * Returns the Configuration name.
     * @return
     */
    public String defineConfigName() {
        return payaraDomainResource.getMetadata().getName().toLowerCase(Locale.ENGLISH) + "-config";
    }
}
