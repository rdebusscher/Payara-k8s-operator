# Payara Kubernetes Operator


## Installation

The installation is performed using the `kubectl` tool and can be done from the console and the current working directory is the _/dist_ folder of the repository.

    kubectl apply -f define-payara-domain-resource.yaml

Installs the Custom Resource Document that defines the Payara Kubernetes document.  Install this in the same _Namespace_ you want to use the Payara Kubernetes Operator.

Next, prepare the Payara Kubernetes Operator. From within the operator directory, create the JAR file

    mvn clean package

And copy the resulting `payara-operator.jar` to the _dist_ directory. You can now build the Docker Image.

    docker build -t payara-operator:0.5 operator

Builds the Docker Image containing the Payara operator. The Operator can be installed using the command

    kubectl apply -f payara-operator.yaml

## Prepare

Before you can use the operator, you need to prepare 2 images.

**Instance image**

The instance image is used for the Payara Instances, and this will run your application. They need to be based on the Server Node Payara image.  You can create this image using the command

    docker build -t server-node-k8s:5.2021.2 server-node-k8s

Make sure that you use the correct Payara version in the _server-node-k8s/Dockerfile_ since the Payara version running the instances and the Domain (DAS) should match to avoid communication issues.

The name of the image (`server-node-k8s:5.2021.2`) can freely be chosen but needs to be used in the Payara Operator CRD (see further on).

**Application Image**

The Application image must contain the Payara Server and the application that needs to be deployed (and optionally the configuration script for the domain).  The Payara Server version must match the one defined in the Instance image.

## Payara Operator CRD

The Payara Operator CRD instructs the Operator to create the Payara environment within the Kubernetes cluster. The YAML file to use looks like this:

    apiVersion: poc.payara.fish/v1alpha
    kind: Domain
    metadata:
      name: test
    spec:
      application-image: k8s-demo:0.5
      instance-image: server-node-k8s:5.2021.2
      instances: 2
      application: testapp.war
      cpu: 2000
      memory: 512
      config-script: /opt/payara/k8s/test-script
      verbose: false

Here is the explanation of the properties

### name

(Required, String)

Name of the Payara environment. It determines also the name of the Kubernetes and Payara objects that are created.

### application-image

(Required, String)

The name of the Docker image that contains the Payara server and the application that needs to be deployed. It can optionally contain a configuration script to adapt the Payara Server configuration after it is started.  See also in a previous section for more information on the Application Image.

### instance-image

(Required, String)

The name of the Docker image of the Instance image as described in a previous section.

### instances

(Optional, Integer, default value = 2)

The number of instances of Payara that need to be created and will be part of the Deployment Group.  The instances are added to a Deployment Group called `<name>-dg` and will be based on a configuration called `<name>-config`.  This configuration can be created in the Payara Server during the creation of the Docker Image or it will be created based on the `default-config` configuration of Payara Server where memory settings are adapted to use the Container limits.

### application

(Optional, String)

Name of the Application that needs to be deployed. It is either a relative path from `/opt/payara/k8s/` or an absolute file within the Application Image.  
When omitted, the application is expected to be available in the `/opt/payara/k8s/<name>.war` file.

### cpu

(Optional, Integer)

CPU limits for the containers running the Instance Image. The value is specified in milliCPU and the default value is 2000 (= 2vCores).

### memory

(Optional, Integer)

The Memory limits for the containers running the Instance Image. The value is specified in MB and the default value is 512.

### max-instances

(Optional, Integer, default value = 2)

When specified and a value larger than 2, the Operator creates a Horizontal Pod Scaler so that the number of instances in the environment will be scaled automatically between the values `instances` and `max-instances` (both values included).

### cpu-target

(Optional, Integer, default value 80)

The CPU target value used by the Horizontal Pod Scaler.  The default value is 80% for the CPU (property accepts values between 30 and 90)

### config-script

(Optional, String)

An optional file containing the Asadmin CLI commands that need to be executed after the server is up and running but before the application is deployed.  This file is submitted using the asadmin multimode command.  

### verbose

(Optional, boolean default false)

Indicated if the Payara Operator will write out verbose logging information. By default, value `false`, it only logs basic information about the events it receives for the Payara Operator CRD from the Kubernetes Cluster.

## Other information

- The Payara Operator doesn't need to be running once it Payara Environment is active. It is only required to create, modify and delete the Payara Environment.

## Known issues / Future changes

- In a future version of the Server Node image, the issue with the `entrypoint.sh` script will be fixed so that this property becomes optional and will be determined based on the Payara server version in the Application Image.
- Support for setting Environment variables and secrets will be included.
- Support for a web application stored outside of the Application Image will be provided.
- Support for updating the application using the rolling upgrade capabilities of Kubernetes.
