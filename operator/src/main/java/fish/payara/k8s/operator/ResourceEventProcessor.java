package fish.payara.k8s.operator;

import fish.payara.k8s.operator.resource.PayaraDomainResource;
import io.fabric8.kubernetes.client.Watcher;

import java.util.concurrent.BlockingDeque;

public class ResourceEventProcessor implements Runnable {
    private BlockingDeque<EventItem> transferQueue;

    public ResourceEventProcessor(BlockingDeque<EventItem> transferQueue) {
        this.transferQueue = transferQueue;
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
        LogHelper.log(" action = " + action.name());

        PayaraDomainResource payaraDomainResource = eventItem.getResource();
        // payaraDomainResource.getMetadata().getName(); // The name of the custom resource

        if (action == Watcher.Action.ADDED) {
            // New Yaml file, Add DAS and a number of Instances and join them in a Deployment Group.
            try {
                // TODO
            } catch (Exception e) {
                e.printStackTrace();  // FIXME
            }
            LogHelper.log("ADDED a resource");
        }
        if (action == Watcher.Action.DELETED) {

            // TODO
            LogHelper.log("DELETED a resource");
        }
        if (action == Watcher.Action.MODIFIED) {
            LogHelper.log("TODO: Modification not implemented yet");

            LogHelper.log("MODIFIED a resource");
        }
    }
}
