package fish.payara.k8s.operator;

import fish.payara.k8s.operator.resource.PayaraDomainResource;
import fish.payara.k8s.operator.util.AliveDetector;
import fish.payara.k8s.operator.util.DeploymentUtil;
import fish.payara.k8s.operator.util.LogHelper;
import fish.payara.k8s.operator.util.PayaraUtil;
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
                e.printStackTrace(); // fixme
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
                domainDetector.waitUntilReady();  // Waits until the domain is up.
                if (domainDetector.isUpAndRunning()) {
                    payaraUtil.prepareDomain(domainDetector.getPod(), payaraDomainResource);
                }
            } catch (Exception e) {
                e.printStackTrace();  // FIXME
            }
            LogHelper.log("Finished ADD for resource " + payaraDomainResource);
        }
        if (action == Watcher.Action.DELETED) {

            deploymentUtil.removeDeploymentDomain(payaraDomainResource);

            LogHelper.log("Finished DELETE for resource " + payaraDomainResource);
        }
        if (action == Watcher.Action.MODIFIED) {
            LogHelper.log("TODO: Modification not implemented yet");

            LogHelper.log("Finished MODIFY for resource " + payaraDomainResource);
        }
    }
}
