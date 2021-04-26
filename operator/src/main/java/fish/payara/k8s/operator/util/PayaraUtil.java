package fish.payara.k8s.operator.util;

import fish.payara.k8s.operator.resource.PayaraDomainResource;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PayaraUtil {

    private static final Pattern SUCCESS_PATTERN = Pattern.compile("Command .* executed successfully.");

    private static final CommandOutputCheck DEFAULT_CHECK = (output) -> {
        String[] lines = output.split("\n");
        Matcher matcher = SUCCESS_PATTERN.matcher(lines[lines.length - 1]);
        return matcher.matches();
    };

    private static final CommandOutputCheck NOOP_CHECK = (output) -> true;

    private final PodUtil podUtil;

    public PayaraUtil(KubernetesClient client, String namespace) {
        podUtil = new PodUtil(client, namespace);
    }

    public void prepareDomain(Pod pod, PayaraDomainResource payaraDomainResource) {
        NamingUtil namingUtil = new NamingUtil(payaraDomainResource);
        ensureConfig(pod, namingUtil);
        createDeploymentGroup(pod, namingUtil);
    }

    private void createDeploymentGroup(Pod pod, NamingUtil namingUtil) {

        String command = "${PAYARA_DIR}/bin/asadmin --user=${ADMIN_USER} --passwordfile=${PASSWORD_FILE} create-deployment-group  "
                + namingUtil.defineDeploymentGroupName();

        podUtil.executeWithinPod(pod, command, DEFAULT_CHECK);

    }

    private void ensureConfig(Pod pod, NamingUtil namingUtil) {
        String configName = namingUtil.defineConfigName();

        String command = "${PAYARA_DIR}/bin/asadmin --user=${ADMIN_USER} --passwordfile=${PASSWORD_FILE} list-configs";
        String configsOutput = podUtil.executeWithinPod(pod, command, DEFAULT_CHECK);

        String[] lines = configsOutput.split("\n");
        boolean configExists = Arrays.asList(Arrays.copyOfRange(lines, 0, lines.length - 1)).contains(configName);

        if (!configExists) {
            command = "${PAYARA_DIR}/bin/asadmin --user=${ADMIN_USER} --passwordfile=${PASSWORD_FILE} copy-config default-config  "
                    + configName;
            String copyOutput = podUtil.executeWithinPod(pod, command, DEFAULT_CHECK);

            updateJVMOptionsMemory(pod, configName);
        }
    }

    private void updateJVMOptionsMemory(Pod pod, String configName) {

        String command = "${PAYARA_DIR}/bin/asadmin --user=${ADMIN_USER} --passwordfile=${PASSWORD_FILE} list-jvm-options --target=" + configName;
        String jvmOptionsOutput = podUtil.executeWithinPod(pod, command, DEFAULT_CHECK);
        String[] lines = jvmOptionsOutput.split("\n");
        Arrays.stream(lines).
                filter(o -> o.startsWith("-Xss") || o.startsWith("-Xmx") || o.startsWith("-Xms"))
                .forEach(o -> {
                    String command2 = "${PAYARA_DIR}/bin/asadmin --user=${ADMIN_USER} --passwordfile=${PASSWORD_FILE} delete-jvm-options --target=" + configName + " " + o;
                    podUtil.executeWithinPod(pod, command2, DEFAULT_CHECK);

                });

        command = "${PAYARA_DIR}/bin/asadmin --user=${ADMIN_USER} --passwordfile=${PASSWORD_FILE} create-jvm-options --target=" + configName + " '-XX\\:+UseContainerSupport:-XX\\:MaxRAMPercentage=${ENV=MEM_MAX_RAM_PERCENTAGE}:-Xss${ENV=MEM_XSS}'";
        podUtil.executeWithinPod(pod, command, DEFAULT_CHECK);

    }

    /**
     * Deploy the application to the Deployment Group.
     *
     * @param pod
     * @param payaraDomainResource
     */
    public boolean deployApplication(Pod pod, PayaraDomainResource payaraDomainResource) {
        NamingUtil namingUtil = new NamingUtil(payaraDomainResource);

        String applicationFile = payaraDomainResource.getSpec().getApplication();
        if (applicationFile == null || applicationFile.trim().isEmpty()) {
            applicationFile = "/opt/payara/k8s/" + payaraDomainResource.getMetadata().getName() + ".war";
        }
        if (!applicationFile.startsWith("/") && !applicationFile.startsWith(".")) {
            applicationFile = "/opt/payara/k8s/" + applicationFile;
        }

        boolean result = false;

        String command = "test -f " + applicationFile + " && echo \"FILE exists.\"";
        String fileExists = podUtil.executeWithinPod(pod, command, NOOP_CHECK);
        if (fileExists.contains("FILE exists")) {
            String deploymentGroupName = namingUtil.defineDeploymentGroupName();
            command = "${PAYARA_DIR}/bin/asadmin --user=${ADMIN_USER} --passwordfile=${PASSWORD_FILE} deploy --virtualservers=server --target " + deploymentGroupName + " " + applicationFile;
            podUtil.executeWithinPod(pod, command, DEFAULT_CHECK);
            result = true;
        }
        return result;
    }
}
