package fish.payara.k8s.operator;

import fish.payara.k8s.operator.resource.PayaraDomainResource;
import fish.payara.k8s.operator.util.AliveDetector;
import fish.payara.k8s.operator.util.DeploymentUtil;
import fish.payara.k8s.operator.util.LogHelper;
import fish.payara.k8s.operator.util.PayaraUtil;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watcher;

import java.util.concurrent.BlockingDeque;

public class ResourceEventProcessor implements Runnable {
    private final KubernetesClient client;
    private final String namespace;
    private final BlockingDeque<EventItem> transferQueue;

    private DeploymentUtil deploymentUtil;
    private PayaraUtil payaraUtil;

    public ResourceEventProcessor(KubernetesClient client, String namespace, BlockingDeque<EventItem> transferQueue) {
        this.client = client;
        this.namespace = namespace;
        this.transferQueue = transferQueue;

        initUtils();
    }

    private void initUtils() {
        deploymentUtil = new DeploymentUtil(client, namespace);
        payaraUtil = new PayaraUtil(client, namespace);
    }

    @Override
    public void run() {
        while (true) {
            try {
                // Handle each event that appears on the queue.
                handleEvent(transferQueue.take());
            } catch (InterruptedException e) {
                LogHelper.exception(e);
            }
        }
    }

    private void handleEvent(EventItem eventItem) {
        Watcher.Action action = eventItem.getAction();

        PayaraDomainResource payaraDomainResource = eventItem.getResource();
        // payaraDomainResource.getMetadata().getName(); // The name of the custom resource

        if (action == Watcher.Action.ADDED) {
            // New Yaml file, Add DAS and a number of Instances and join them in a Deployment Group.
            try {
                AliveDetector domainDetector = deploymentUtil.addNewDeploymentDomain(payaraDomainResource);
                if (domainDetector == null) {
                    // Domain already running
                    return;
                }
                domainDetector.waitUntilReady();  // Waits until the domain is up.
                if (domainDetector.isUpAndRunning()) {
                    Pod podDAS = domainDetector.getPod();
                    payaraUtil.prepareDomain(podDAS, payaraDomainResource);
                    if (payaraDomainResource.getSpec().getConfigScript() != null && !payaraDomainResource.getSpec().getConfigScript().isEmpty()) {
                        payaraUtil.executeConfigScript(podDAS, payaraDomainResource);
                    }
                    if (payaraUtil.deployApplication(podDAS, payaraDomainResource)) {
                        // Deploy instances.
                        deploymentUtil.addNewDeploymentNode(payaraDomainResource, podDAS);
                        deploymentUtil.addNewServiceNode(payaraDomainResource);
                        deploymentUtil.addAutoscale(payaraDomainResource);
                    }
                }
            } catch (Exception e) {
                LogHelper.exception(e);
            }
        }
        if (action == Watcher.Action.DELETED) {

            deploymentUtil.removeAutoscale(payaraDomainResource);
            deploymentUtil.removeDeploymentNode(payaraDomainResource);
            deploymentUtil.removeServiceNode(payaraDomainResource);
            deploymentUtil.removeDeploymentDomain(payaraDomainResource);
        }
        if (action == Watcher.Action.MODIFIED) {

            try {
                deploymentUtil.updateDeploymentDomain(payaraDomainResource);
            } catch (Exception e) {
                LogHelper.exception(e);
            }
        }
        logEndEvent(action, payaraDomainResource);
    }

    private void logEndEvent(Watcher.Action action, PayaraDomainResource payaraDomainResource) {
        LogHelper.log(String.format("Finished processing event '%s' for Payara Domain '%s'", action, payaraDomainResource.getMetadata().getName()));
        if (payaraDomainResource.getSpec().isVerbose()) {
            LogHelper.log(String.format("Finished event '%s' for Resource '%s'", action, payaraDomainResource));
        }
    }
}
