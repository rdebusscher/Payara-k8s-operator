package fish.payara.k8s.operator;

import fish.payara.k8s.operator.resource.PayaraDomainResource;
import fish.payara.k8s.operator.resource.PayaraDomainResourceList;
import fish.payara.k8s.operator.util.LogHelper;
import fish.payara.k8s.operator.util.VersionReader;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.*;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.internal.KubernetesDeserializer;

import java.util.Map;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Kubernetes operator to read a Custom resource file and setup a Payara Deployment Group setup.
 * This is not a fully functional version, it was created for the presentation "Creating a Kubernetes Operator In java" https://www.slideshare.net/rdebusscher/creating-a-kubernetes-operator-in-java
 */
public class PayaraOperator {

    // Keep latest version of the Custom Resource cached.
    private final Map<String, PayaraDomainResource> cache = new ConcurrentHashMap<>();

    // Client to access the Custom resource
    private NonNamespaceOperation<PayaraDomainResource, PayaraDomainResourceList, Resource<PayaraDomainResource>> customResourceClient;

    private final BlockingDeque<EventItem> transferQueue = new LinkedBlockingDeque<>();

    public static void main(String args[]) {
        VersionReader versionReader = new VersionReader("payara-operator");
        LogHelper.log("Running version " + versionReader.getReleaseVersion());
        LogHelper.log("Build at " + versionReader.getBuildTime());

        Config config = new ConfigBuilder().build();
        KubernetesClient client = new DefaultKubernetesClient(config);

        String namespace = client.getNamespace();
        LogHelper.log("Running in Namespace " + namespace);

        // Register the Custom Resource so that Client will use PayaraDomainResource for the YAML.
        KubernetesDeserializer.registerCustomKind(HasMetadata.getApiVersion(PayaraDomainResource.class), "Domain", PayaraDomainResource.class);


        // Get the Custom Resource, If not registered, our Operator can't work properly so throw an Exception.
        client
                .apiextensions().v1().customResourceDefinitions()
                .list()
                .getItems()
                .stream()
                .filter(d -> "domains.poc.payara.fish".equals(d.getMetadata().getName()))
                .findAny()
                .orElseThrow(
                        () -> new RuntimeException("Deployment error: Custom resource definition Domain for payara.fish not found."));


        // Start the Operator as Daemon thread (since using Watch)
        new PayaraOperator(namespace, client).performWork();

    }

    public PayaraOperator(String namespace,
                          KubernetesClient client) {
        this.customResourceClient = client
                .customResources(PayaraDomainResource.class, PayaraDomainResourceList.class)
                .inNamespace(namespace);
        new Thread(new ResourceEventProcessor(client, namespace, transferQueue)).start();
    }

    private void performWork() {

        try {
            // watch
            customResourceClient.watch(new Watcher<PayaraDomainResource>() {
                @Override
                public void eventReceived(Action action, PayaraDomainResource resource) {
                    try {
                        // Get the UID for the Custom Resource instance.
                        String uid = resource.getMetadata().getUid();
                        if (cache.containsKey(uid)) {
                            // This Custom Resource instance is already known. Check if it is a updated version.
                            int knownResourceVersion = Integer.parseInt(cache.get(uid).getMetadata().getResourceVersion());
                            int receivedResourceVersion = Integer.parseInt(resource.getMetadata().getResourceVersion());
                            if (knownResourceVersion > receivedResourceVersion) {
                                // We have already a version which is more recent, ignore this event.
                                return;
                            }
                        }
                        LogHelper.log("received " + action + " for resource " + resource);
                        if (action == Action.ADDED || action == Action.MODIFIED) {
                            cache.put(uid, resource);
                        }
                        // Queue this event for processing by the operator.
                        transferQueue.offer(new EventItem(resource, action));
                        if (action == Action.DELETED) {
                            // We have handled the resource so now we can remove it from the cache
                            cache.remove(uid);
                        }
                    } catch (Exception e) {
                        e.printStackTrace(); // FIXME
                        System.exit(-1);
                    }
                }

                @Override
                public void onClose(WatcherException cause) {
                    cause.printStackTrace();// FIXME
                    System.exit(-1);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();// FIXME
            System.exit(-1);
        }
    }
}
