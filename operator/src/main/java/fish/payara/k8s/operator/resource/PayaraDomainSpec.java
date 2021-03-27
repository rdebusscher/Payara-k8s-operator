package fish.payara.k8s.operator.resource;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.HashMap;
import java.util.Map;

@JsonDeserialize()
public class PayaraDomainSpec {

    @JsonProperty("application-image")
    private String applicationImage;

    @JsonProperty("instance-image")
    private String instanceImage;

    @JsonProperty("application")
    private String application;

    @JsonProperty("instances")
    private int instances;

    @JsonProperty("config-script")
    private String configScript;

    public String getApplicationImage() {
        return applicationImage;
    }

    public String getInstanceImage() {
        return instanceImage;
    }

    public String getApplication() {
        return application;
    }

    public String geDeploymentGroup() {
        return "dg_" + application;
    }

    public int getInstances() {
        return instances;
    }

    public String getConfigScript() {
        return configScript;
    }

    public Map<String, String> asTemplateVariables() {
        Map<String, String> result = new HashMap<>();
        result.put("application_image", applicationImage);
        result.put("application", application);
        result.put("instances", String.valueOf(instances));
        result.put("config_script", configScript);
        return result;
    }

    /*
    public Map<String, String> asTemplateVariablesForNode(String dasIP) {
        Map<String, String> result = new HashMap<>();
        //result.put("payara_image", payaraImage.replace("payara/server-full", "server-node-k8s"));
        result.put("application", application);
        result.put("instances", String.valueOf(instances));
        result.put("config_script", configScript);
        //result.put("artifact", artifact);
        result.put("deployment_group", geDeploymentGroup());
        result.put("das_host", dasIP);
        return result;
    }

     */

    @Override
    public String toString() {
        return "PayaraDomainSpec{" +
                "applicationImage='" + applicationImage + '\'' +
                ", instanceImage='" + instanceImage + '\'' +
                ", application='" + application + '\'' +
                ", instances=" + instances +
                ", configScript='" + configScript + '\'' +
                '}';
    }
}
