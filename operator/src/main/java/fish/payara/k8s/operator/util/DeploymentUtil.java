package fish.payara.k8s.operator.util;

import fish.payara.k8s.operator.resource.PayaraDomainResource;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.api.model.apps.Deployment;
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
            newDeployment.getMetadata().getOwnerReferences().get(0).setUid(ResourceType.DAS.name() + payaraDomainResource.getMetadata().getUid());
            newDeployment.getMetadata().getOwnerReferences().get(0).setName(payaraDomainResource.getMetadata().getName());

            // Apply the Deployment to K8S.
            deployments.create(newDeployment);
            // With all info from K8S
            LogHelper.log("Created new K8S Domain Deployment");

            inputStream.close();
            // Return a AliveDetector so tat we can wait until DAS is up and running.
            return waitServerStarted();
        } else {
            LogHelper.log("K8S Domain Deployment already available");
        }
        return null;
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
                .filter(d -> d.getMetadata().getOwnerReferences().stream()
                        .anyMatch(ownerReference -> ownerReference.getUid().equals(type.name() + payaraDomainResource.getMetadata().getUid())))
                .findAny();
    }

    private AliveDetector waitServerStarted() {
        AliveDetector detector = new AliveDetector(podUtil);

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

            //LogHelper.log("Removed Domain Deployment " + deployment.get());  // With all info from K8S
            LogHelper.log("Removed Domain K8S Deployment ");

        }
    }

    /**
     * Add a K8S Deployment for the Instances.
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
            newDeployment.getMetadata().getOwnerReferences().get(0).setUid(ResourceType.INSTANCE.name() + payaraDomainResource.getMetadata().getUid());
            newDeployment.getMetadata().getOwnerReferences().get(0).setName(payaraDomainResource.getMetadata().getName());


            deployments.create(newDeployment);
            // LogHelper.log("Created new Deployment " + newDeployment);
            LogHelper.log("Created new K8S Deployment for Node");
            inputStream.close();
        } else {
            LogHelper.log("Deployment already available");
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

            // LogHelper.log("Removed Deployment Node" + deployment.get());  // With all info from K8S
            LogHelper.log("Removed Node K8S Deployment ");

        } else {
            LogHelper.log("No deployment found for ");
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
            //LogHelper.log("Created new Service " + newService);   // With all info from K8S
            LogHelper.log("Created new K8S Node Service ");
            inputStream.close();
        } else {
            LogHelper.log("Service already available");
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

            //LogHelper.log("Removed Service " + service.get());  // With all info from K8S
            LogHelper.log("Removed K8S Node Service ");

        }
    }

    /**
     * Add a Horizontal Pod Scaler to to domain to control the Payara instances.
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

                LogHelper.log("Added Horizontal Scaler to Node Deployment");
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
     * @param payaraDomainResource
     */
    public void removeAutoscale(PayaraDomainResource payaraDomainResource) {
        Optional<HorizontalPodAutoscaler> autoscaler = findAutoscaler(payaraDomainResource);
        if (autoscaler.isPresent()) {
            MixedOperation<HorizontalPodAutoscaler, HorizontalPodAutoscalerList, Resource<HorizontalPodAutoscaler>> scalers = client.autoscaling().v1().horizontalPodAutoscalers();
            scalers.delete(autoscaler.get());

            LogHelper.log("Removed K8S Node Horizontal Scaler ");

        }
    }
}
