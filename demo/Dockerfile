FROM payara/server-full:5.2021.2

RUN mkdir /opt/payara/k8s

ADD target/testapp.war /opt/payara/k8s
ADD test-script /opt/payara/k8s
