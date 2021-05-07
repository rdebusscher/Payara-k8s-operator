package fish.payara.k8s.operator.util;

public enum ResourceType {

    DAS("domain"), INSTANCE("node");

    private String appLabel;  // Matches the app label in the deployment

    ResourceType(String appLabel) {
        this.appLabel = appLabel;
    }

    public String getAppLabel() {
        return appLabel;
    }
}
