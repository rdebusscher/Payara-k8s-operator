package fish.payara.k8s.operator.util;

import fish.payara.k8s.operator.resource.PayaraDomainResource;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentList;
import io.fabric8.kubernetes.api.model.autoscaling.v1.HorizontalPodAutoscaler;
import io.fabric8.kubernetes.api.model.autoscaling.v1.HorizontalPodAutoscalerList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;

public class DeploymentUtil {

    private final KubernetesClient client;
    private final String namespace;
    private final PodUtil podUtil;

    public DeploymentUtil(KubernetesClient client, String namespace) {
        this.client = client;
        this.namespace = namespace;
        podUtil = new PodUtil(client, namespace);
    }

    public AliveDetector addNewDeploymentDomain(PayaraDomainResource payaraDomainResource) throws IOException {
        if (noDeploymentYet(payaraDomainResource, ResourceType.DAS)) {
            // There is no deployment yet

            TemplateVariableProvider templateVariableProvider = new TemplateVariableProvider(payaraDomainResource);

            // Process the payaraDomainDeployment.yaml file with ThymeLeaf so that it is customized with info from the Custom Resource
            String processed = ThymeleafEngine.getInstance().processFile("/payaraDomainDeployment.yaml", templateVariableProvider.mainTemplateVariables());
            ByteArrayInputStream inputStream = new ByteArrayInputStream(processed.getBytes());
            NonNamespaceOperation<Deployment, DeploymentList, RollableScalableResource<Deployment>> deployments = client.apps().deployments().inNamespace(namespace);

            // Load the Deployment into Kubernetes (not executed yet)
            Deployment newDeployment = deployments.load(inputStream).get();
            // Add some metadata so that we can link this deployment to the Custom Resource.
            newDeployment.getMetadata().getOwnerReferences().get(0).setUid(payaraDomainResource.getMetadata().getUid());
            newDeployment.getMetadata().getOwnerReferences().get(0).setName(payaraDomainResource.getMetadata().getName());

            // Apply the Deployment to K8S.
            deployments.create(newDeployment);
            // With all info from K8S
            if (payaraDomainResource.getSpec().isVerbose()) {
                LogHelper.log(String.format("Created new K8S Deployment for '%s' Domain", payaraDomainResource.getMetadata().getName()));
            }

            inputStream.close();
            // Return a AliveDetector so tat we can wait until DAS is up and running.
            return waitServerStarted(payaraDomainResource.getMetadata().getName(), payaraDomainResource.getSpec().isVerbose());
        } else {
            if (payaraDomainResource.getSpec().isVerbose()) {
                LogHelper.log(String.format("K8S Deployment for '%s' Domain already exists", payaraDomainResource.getMetadata().getName()));
            }
        }
        return null;
    }

    public void updateDeploymentDomain(PayaraDomainResource payaraDomainResource) throws IOException {
        Optional<Deployment> deployment = findDeployment(payaraDomainResource, ResourceType.INSTANCE);

        if (deployment.isPresent()) {
            Deployment deploymentDAS = deployment.get();

            NonNamespaceOperation<Deployment, DeploymentList, RollableScalableResource<Deployment>> deployments = client.apps().deployments().inNamespace(namespace);

            deployments.withName(deploymentDAS.getMetadata().getName())
                    .edit(d -> new DeploymentBuilder(deploymentDAS)
                            .editSpec().withNewReplicas(payaraDomainResource.getSpec().getInstances())
                            .endSpec().build());


        } else {
            if (payaraDomainResource.getSpec().isVerbose()) {
                LogHelper.log(String.format("K8S Deployment for '%s' Domain does not exists", payaraDomainResource.getMetadata().getName()));
            }
        }
    }

    private boolean noDeploymentYet(PayaraDomainResource payaraDomainResource, ResourceType type) {
        return !findDeployment(payaraDomainResource, type).isPresent();
    }

    private Optional<Deployment> findDeployment(PayaraDomainResource payaraDomainResource, ResourceType type) {
        return client.apps().deployments()
                .inNamespace(namespace)
                .list()
                .getItems()
                .stream()
                .filter(d -> d.getMetadata().getLabels().get("app").equals(type.getAppLabel()))
                .filter(d -> d.getMetadata().getOwnerReferences().stream()
                        .anyMatch(ownerReference -> ownerReference.getUid().equals(payaraDomainResource.getMetadata().getUid())))
                .findAny();
    }

    private AliveDetector waitServerStarted(String name, boolean verbose) {
        AliveDetector detector = new AliveDetector(podUtil, name, verbose);

        // Do checks asynchronous.
        new Thread(detector).start();
        return detector;
    }

    /**
     * Remove the deployment for the DAS.
     *
     * @param payaraDomainResource
     */
    public void removeDeploymentDomain(PayaraDomainResource payaraDomainResource) {
        Optional<Deployment> deployment = findDeployment(payaraDomainResource, ResourceType.DAS);
        if (deployment.isPresent()) {
            NonNamespaceOperation<Deployment, DeploymentList, RollableScalableResource<Deployment>> deployments = client.apps().deployments().inNamespace(namespace);
            deployments.delete(deployment.get());

            if (payaraDomainResource.getSpec().isVerbose()) {
                LogHelper.log(String.format("Removed K8S Deployment for '%s' Domain", payaraDomainResource.getMetadata().getName()));
            }

        } else {
            if (payaraDomainResource.getSpec().isVerbose()) {
                LogHelper.log(String.format("K8S Deployment for '%s' Domain not found", payaraDomainResource.getMetadata().getName()));
            }

        }
    }

    /**
     * Add a K8S Deployment for the Instances.
     *
     * @param payaraDomainResource
     * @param podDAS
     * @throws IOException
     */
    public void addNewDeploymentNode(PayaraDomainResource payaraDomainResource, Pod podDAS) throws IOException {
        if (noDeploymentYet(payaraDomainResource, ResourceType.INSTANCE)) {

            TemplateVariableProvider templateVariableProvider = new TemplateVariableProvider(payaraDomainResource);

            String dasIP = podUtil.lookupIP(podDAS);
            // Add deployment for the Instances, similar to addNewDeploymentDomain().
            String processed = ThymeleafEngine.getInstance().processFile("/payaraNodeDeployment.yaml", templateVariableProvider.nodeTemplateVariables(dasIP));
            ByteArrayInputStream inputStream = new ByteArrayInputStream(processed.getBytes());
            NonNamespaceOperation<Deployment, DeploymentList, RollableScalableResource<Deployment>> deployments = client.apps().deployments().inNamespace(namespace);

            Deployment newDeployment = deployments.load(inputStream).get();
            newDeployment.getMetadata().getOwnerReferences().get(0).setUid(payaraDomainResource.getMetadata().getUid());
            newDeployment.getMetadata().getOwnerReferences().get(0).setName(payaraDomainResource.getMetadata().getName());


            deployments.create(newDeployment);
            if (payaraDomainResource.getSpec().isVerbose()) {
                LogHelper.log(String.format("Created new K8S Deployment for '%s' Nodes", payaraDomainResource.getMetadata().getName()));
            }

            inputStream.close();
        } else {
            if (payaraDomainResource.getSpec().isVerbose()) {
                LogHelper.log(String.format("K8S Deployment for '%s' Nodes already exists", payaraDomainResource.getMetadata().getName()));
            }
        }
    }

    /**
     * Remove the deployment for the Payara Instances.
     *
     * @param payaraDomainResource
     */
    public void removeDeploymentNode(PayaraDomainResource payaraDomainResource) {
        Optional<Deployment> deployment = findDeployment(payaraDomainResource, ResourceType.INSTANCE);
        if (deployment.isPresent()) {
            NonNamespaceOperation<Deployment, DeploymentList, RollableScalableResource<Deployment>> deployments = client.apps().deployments().inNamespace(namespace);
            deployments.delete(deployment.get());

            if (payaraDomainResource.getSpec().isVerbose()) {
                LogHelper.log(String.format("Removed K8S Deployment for '%s' Nodes", payaraDomainResource.getMetadata().getName()));
            }

        } else {
            if (payaraDomainResource.getSpec().isVerbose()) {
                LogHelper.log(String.format("No K8S Deployment for '%s' Nodes found", payaraDomainResource.getMetadata().getName()));
            }
        }
    }

    public void addNewServiceNode(PayaraDomainResource payaraDomainResource) throws IOException {
        if (noServiceYet(payaraDomainResource)) {
            //There is no service yet

            TemplateVariableProvider templateVariableProvider = new TemplateVariableProvider(payaraDomainResource);

            // Process the payaraNodeService.yaml file with ThymeLeaf so that it is customized with info from the Custom Resource
            String processed = ThymeleafEngine.getInstance().processFile("//payaraNodeService.yaml", templateVariableProvider.nodeTemplateVariables(null));
            ByteArrayInputStream inputStream = new ByteArrayInputStream(processed.getBytes());
            NonNamespaceOperation<Service, ServiceList, ServiceResource<Service>> services = client.services().inNamespace(namespace);

            // Load the Service into Kubernetes (not executed yet)
            Service newService = services.load(inputStream).get();
            // Add some metadata so that we can link this Service to the Custom Resource.
            newService.getMetadata().getOwnerReferences().get(0).setUid(payaraDomainResource.getMetadata().getUid());
            newService.getMetadata().getOwnerReferences().get(0).setName(payaraDomainResource.getMetadata().getName());

            // Apply the Service to K8S.
            services.create(newService);

            if (payaraDomainResource.getSpec().isVerbose()) {
                LogHelper.log(String.format("Created K8S Service for '%s' Nodes", payaraDomainResource.getMetadata().getName()));
            }

            inputStream.close();
        } else {
            if (payaraDomainResource.getSpec().isVerbose()) {
                LogHelper.log(String.format("K8S Service for '%s' Nodes already available", payaraDomainResource.getMetadata().getName()));
            }
        }
    }

    private boolean noServiceYet(PayaraDomainResource payaraDomainResource) {
        return !findService(payaraDomainResource).isPresent();
    }

    /**
     * Find the Kubernetes Service.
     *
     * @param payaraDomainResource
     * @return
     */
    private Optional<Service> findService(PayaraDomainResource payaraDomainResource) {
        return client.services()
                .inNamespace(namespace)
                .list()
                .getItems()
                .stream()
                .filter(d -> d.getMetadata().getOwnerReferences().stream()
                        .anyMatch(ownerReference -> ownerReference.getUid().equals(payaraDomainResource.getMetadata().getUid())))
                .findFirst();
    }

    /**
     * Remove the Service for the Node.
     *
     * @param payaraDomainResource
     */
    public void removeServiceNode(PayaraDomainResource payaraDomainResource) {
        Optional<Service> service = findService(payaraDomainResource);

        if (service.isPresent()) {
            NonNamespaceOperation<Service, ServiceList, ServiceResource<Service>> services = client.services().inNamespace(namespace);
            services.delete(service.get());

            if (payaraDomainResource.getSpec().isVerbose()) {
                LogHelper.log(String.format("Removed K8S Service for '%s' Nodes", payaraDomainResource.getMetadata().getName()));
            }

        } else {
            if (payaraDomainResource.getSpec().isVerbose()) {
                LogHelper.log(String.format("K8S Service for '%s' Nodes not found", payaraDomainResource.getMetadata().getName()));
            }

        }
    }

    /**
     * Add a Horizontal Pod Scaler to to domain to control the Payara instances.
     *
     * @param payaraDomainResource
     * @throws IOException
     */
    public void addAutoscale(PayaraDomainResource payaraDomainResource) throws IOException {
        if (payaraDomainResource.getSpec().getMaxInstances() != 2) {
            if (noScalerYet(payaraDomainResource)) {

                TemplateVariableProvider templateVariableProvider = new TemplateVariableProvider(payaraDomainResource);

                String processed = ThymeleafEngine.getInstance().processFile("//payaraNodeScaler.yaml", templateVariableProvider.nodeTemplateVariables(null));
                ByteArrayInputStream inputStream = new ByteArrayInputStream(processed.getBytes());
                NonNamespaceOperation<HorizontalPodAutoscaler, HorizontalPodAutoscalerList, Resource<HorizontalPodAutoscaler>> scalers = client.autoscaling().v1().horizontalPodAutoscalers().inNamespace(namespace);

                // Load the AutoScaler into Kubernetes (not executed yet)
                HorizontalPodAutoscaler newScaler = scalers.load(inputStream).get();
                // Add some metadata so that we can link this Service to the Custom Resource.
                newScaler.getMetadata().getOwnerReferences().get(0).setUid(payaraDomainResource.getMetadata().getUid());
                newScaler.getMetadata().getOwnerReferences().get(0).setName(payaraDomainResource.getMetadata().getName());

                // Apply the Scaler to K8S.
                scalers.create(newScaler);
                inputStream.close();

                if (payaraDomainResource.getSpec().isVerbose()) {
                    LogHelper.log(String.format("Created K8S Horizontal Scaler for '%s' Nodes", payaraDomainResource.getMetadata().getName()));
                }
            } else {
                if (payaraDomainResource.getSpec().isVerbose()) {
                    LogHelper.log(String.format("K8S Horizontal Scaler for '%s' Nodes already exists", payaraDomainResource.getMetadata().getName()));
                }

            }
        }
    }

    /**
     * Find the Kubernetes Horizontal scaler.
     *
     * @param payaraDomainResource
     * @return
     */
    private Optional<HorizontalPodAutoscaler> findAutoscaler(PayaraDomainResource payaraDomainResource) {
        return client.autoscaling().v1().horizontalPodAutoscalers()
                .inNamespace(namespace)
                .list()
                .getItems()
                .stream()
                .filter(d -> d.getMetadata().getOwnerReferences().stream()
                        .anyMatch(ownerReference -> ownerReference.getUid().equals(payaraDomainResource.getMetadata().getUid())))
                .findFirst();
    }

    private boolean noScalerYet(PayaraDomainResource payaraDomainResource) {
        return !findAutoscaler(payaraDomainResource).isPresent();
    }


    /**
     * Remove the Horizontal Pod Scaler that is created for the Payara Domain Resource.
     *
     * @param payaraDomainResource
     */
    public void removeAutoscale(PayaraDomainResource payaraDomainResource) {
        Optional<HorizontalPodAutoscaler> autoscaler = findAutoscaler(payaraDomainResource);
        if (autoscaler.isPresent()) {
            MixedOperation<HorizontalPodAutoscaler, HorizontalPodAutoscalerList, Resource<HorizontalPodAutoscaler>> scalers = client.autoscaling().v1().horizontalPodAutoscalers();
            scalers.delete(autoscaler.get());

            if (payaraDomainResource.getSpec().isVerbose()) {
                LogHelper.log(String.format("Removed K8S Horizontal Scaler for '%s' Nodes", payaraDomainResource.getMetadata().getName()));
            }

        } else {
            if (payaraDomainResource.getSpec().isVerbose()) {
                LogHelper.log(String.format("K8S Horizontal Scaler for '%s' Nodes not found", payaraDomainResource.getMetadata().getName()));
            }

        }
    }
}
