package fish.payara.k8s.operator.util;

import fish.payara.k8s.operator.resource.PayaraDomainResource;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;

import java.util.Arrays;
import java.util.Locale;

public class PayaraUtil {

    private final PodUtil podUtil;

    public PayaraUtil(KubernetesClient client, String namespace) {
        podUtil = new PodUtil(client, namespace);
    }

    public void prepareDomain(Pod pod, PayaraDomainResource payaraDomainResource) {

        ensureConfig(pod, payaraDomainResource);
        createDeploymentGroup(pod, payaraDomainResource);
    }

    private void createDeploymentGroup(Pod pod, PayaraDomainResource payaraDomainResource) {
        String deploymentGroupName = payaraDomainResource.getMetadata().getName().toLowerCase(Locale.ENGLISH) + "-dg";

        String command = "${PAYARA_DIR}/bin/asadmin --user=${ADMIN_USER} --passwordfile=${PASSWORD_FILE} create-deployment-group  "
                + deploymentGroupName;

        podUtil.executeWithinPod(pod, command);
        // FIXME check if successful executed

    }

    private void ensureConfig(Pod pod, PayaraDomainResource payaraDomainResource) {
        String configName = payaraDomainResource.getMetadata().getName().toLowerCase(Locale.ENGLISH) + "-config";

        String command = "${PAYARA_DIR}/bin/asadmin --user=${ADMIN_USER} --passwordfile=${PASSWORD_FILE} list-configs";
        String configsOutput = podUtil.executeWithinPod(pod, command);
        // FIXME check if successful executed

        String[] lines = configsOutput.split("\n");
        boolean configExists = Arrays.asList(Arrays.copyOfRange(lines, 0, lines.length - 2)).contains(configName);

        if (!configExists) {
            command = "${PAYARA_DIR}/bin/asadmin --user=${ADMIN_USER} --passwordfile=${PASSWORD_FILE} copy-config default-config  "
                    + configName;
            String copyOutput = podUtil.executeWithinPod(pod, command);
            // FIXME check if successful executed
            LogHelper.log(copyOutput);

        }
    }
}
