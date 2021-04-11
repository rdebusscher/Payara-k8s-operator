package fish.payara.k8s.operator.resource;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

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

    @JsonProperty("cpu")
    private int cpu;

    @JsonProperty("memory")
    private int memory;

    public String getApplicationImage() {
        return applicationImage;
    }

    public String getInstanceImage() {
        return instanceImage;
    }

    public String getApplication() {
        return application;
    }

    public int getInstances() {
        return instances;
    }

    public String getConfigScript() {
        return configScript;
    }

    public int getCpu() {
        return cpu;
    }

    public int getMemory() {
        return memory;
    }

    @Override
    public String toString() {
        return "PayaraDomainSpec{" +
                "applicationImage='" + applicationImage + '\'' +
                ", instanceImage='" + instanceImage + '\'' +
                ", application='" + application + '\'' +
                ", instances=" + instances +
                ", configScript='" + configScript + '\'' +
                ", cpu=" + cpu +
                ", memory=" + memory +
                '}';
    }
}
