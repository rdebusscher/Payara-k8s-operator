package fish.payara.k8s.operator.util;

import fish.payara.k8s.operator.resource.PayaraDomainResource;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class providing the Template variable values (for Thymeleaf engine)
 */
public class TemplateVariableProvider {

    private final PayaraDomainResource payaraDomainResource;
    private final NamingUtil namingUtil;

    public TemplateVariableProvider(PayaraDomainResource payaraDomainResource) {
        this.payaraDomainResource = payaraDomainResource;
        namingUtil = new NamingUtil(payaraDomainResource);
    }

    /**
     * For the DAS image (PayaraDomainDeployment.yaml).
     * @return
     */
    public Map<String, String> mainTemplateVariables() {
        Map<String, String> result = new HashMap<>();
        result.put("application_image", payaraDomainResource.getSpec().getApplicationImage());
        return result;
    }

    /**
     * For the Instance image (PayaraNodeDeployment.yaml).
     * @param dasIP
     * @return
     */
    public Map<String, String> nodeTemplateVariables(String dasIP) {
        Map<String, String> result = new HashMap<>();

        result.put("instance_image", payaraDomainResource.getSpec().getInstanceImage());
        result.put("instances", String.valueOf(payaraDomainResource.getSpec().getInstances()));
        result.put("deployment_group", namingUtil.defineDeploymentGroupName());
        result.put("config_name", namingUtil.defineConfigName());

        result.put("das_host", dasIP);
        return result;
    }
}
