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
