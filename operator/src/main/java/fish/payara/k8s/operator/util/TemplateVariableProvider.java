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
        result.put("name", payaraDomainResource.getMetadata().getName());
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

        result.put("name", payaraDomainResource.getMetadata().getName());
        result.put("instance_image", payaraDomainResource.getSpec().getInstanceImage());
        result.put("instances", String.valueOf(payaraDomainResource.getSpec().getInstances()));
        result.put("deployment_group", namingUtil.defineDeploymentGroupName());
        result.put("config_name", namingUtil.defineConfigName());
        result.put("cpu", String.valueOf(payaraDomainResource.getSpec().getCpu()));
        result.put("memory", String.valueOf(payaraDomainResource.getSpec().getMemory()));
        result.put("max_instances", String.valueOf(payaraDomainResource.getSpec().getMaxInstances()));
        result.put("cpu_target", String.valueOf(payaraDomainResource.getSpec().getCpuTarget()));

        result.put("das_host", dasIP);
        return result;
    }
}
