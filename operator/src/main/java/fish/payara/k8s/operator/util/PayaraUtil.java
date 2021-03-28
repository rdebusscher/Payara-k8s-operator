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
        String deploymentGroupName = defineDeploymentGroupName(payaraDomainResource);

        String command = "${PAYARA_DIR}/bin/asadmin --user=${ADMIN_USER} --passwordfile=${PASSWORD_FILE} create-deployment-group  "
                + deploymentGroupName;

        podUtil.executeWithinPod(pod, command);
        // FIXME check if successful executed

    }

    private String defineDeploymentGroupName(PayaraDomainResource payaraDomainResource) {
        return payaraDomainResource.getMetadata().getName().toLowerCase(Locale.ENGLISH) + "-dg";
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

    /**
     * Deploy the application to the Deployment Group.
     *
     * @param pod
     * @param payaraDomainResource
     */
    public boolean deployApplication(Pod pod, PayaraDomainResource payaraDomainResource) {
        String applicationFile = payaraDomainResource.getSpec().getApplication();
        if (applicationFile == null || applicationFile.trim().isEmpty()) {
            applicationFile = "/opt/payara/k8s/" + payaraDomainResource.getMetadata().getName() + ".war";
        }
        if (!applicationFile.startsWith("/") && !applicationFile.startsWith(".")) {
            applicationFile = "/opt/payara/k8s/" + applicationFile;
        }

        boolean result = false;

        String command = "test -f " + applicationFile + " && echo \"FILE exists.\"";
        String fileExists = podUtil.executeWithinPod(pod, command);
        if (fileExists.contains("FILE exists")) {
            String deploymentGroupName = defineDeploymentGroupName(payaraDomainResource);
            command = "${PAYARA_DIR}/bin/asadmin --user=${ADMIN_USER} --passwordfile=${PASSWORD_FILE} deploy --target " + deploymentGroupName + " " + applicationFile;
            podUtil.executeWithinPod(pod, command);
            // FIXME Check if deploy successful.
            result = true;
        }
        return result;
    }
}
