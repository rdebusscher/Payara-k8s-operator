/*
 * Copyright (c) [2021] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

    @JsonProperty("max-instances")
    private int maxInstances;

    @JsonProperty("cpu-target")
    private int cpuTarget;

    @JsonProperty("verbose")
    private boolean verbose;

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

    public int getMaxInstances() {
        return maxInstances;
    }

    public int getCpuTarget() {
        return cpuTarget;
    }

    public boolean isVerbose() {
        return verbose;
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
                ", maxInstances=" + maxInstances +
                ", cpuTarget=" + cpuTarget +
                ", verbose=" + verbose +
                '}';
    }
}
