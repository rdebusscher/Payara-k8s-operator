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

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.ExecListener;
import io.fabric8.kubernetes.client.dsl.ExecWatch;
import okhttp3.Response;

import java.io.ByteArrayOutputStream;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class PodUtil {

    private KubernetesClient client;
    private String namespace;

    public PodUtil(KubernetesClient client, String namespace) {
        this.client = client;
        this.namespace = namespace;
    }

    /**
     * Lookup a pod running the DAS based on the Payara Domain name.
     * @param name
     * @return
     */
    public Pod lookupPod(String name) {
        String podLabel = "domain-" + name;
        Pod result = null;
        Optional<Pod> pod = client.pods().inNamespace(namespace).list().getItems()
                .stream()
                .filter(p -> hasPodLabel(p, "app", podLabel))
                .findAny();

        if (pod.isPresent()) {
            result = pod.get();
        } else {
            LogHelper.log(String.format("Pod with app label '%s' not found", podLabel));
        }

        return result;
    }

    private boolean hasPodLabel(Pod pod, String key, String value) {
        return pod.getMetadata().getLabels().entrySet().stream()
                .anyMatch(e -> key.equals(e.getKey()) && value.equals(e.getValue()));

    }

    public String lookupIP(Pod pod) {
        // FIXME We should check if container is ready and ip is something meaningful
        return pod.getStatus().getPodIP();
    }

    /**
     * Execute a OS command in the POD and get the output of it.
     * @param pod
     * @param command
     * @return
     */
    public String executeWithinPod(Pod pod, String command, CommandOutputCheck check, boolean verbose) {
        if (verbose) {
            LogHelper.log(command);
        }

        final CountDownLatch execLatch = new CountDownLatch(1);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ExecWatch watch = client.pods().withName(pod.getMetadata().getName()).writingOutput(baos).usingListener(new ExecListener() {
            @Override
            public void onOpen(Response response) {
            }

            @Override
            public void onFailure(Throwable t, Response response) {
                execLatch.countDown();
            }

            @Override
            public void onClose(int code, String reason) {
                execLatch.countDown();
            }
        }).exec("/bin/bash", "-c", command);

        try {
            // Don't wait forever until command is executed (can be stuck for example) so max 1 min.
            execLatch.await(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            LogHelper.exception(e);
        }
        String commandOutput = baos.toString();
        if (!check.isCommandExecutedSuccessful(commandOutput)) {
            LogHelper.log(String.format("command '%s' failed with:", command));
            LogHelper.log(commandOutput);
        }
        return commandOutput;
    }

    public KubernetesClient getClient() {
        return client;
    }

    public String getNamespace() {
        return namespace;
    }
}
