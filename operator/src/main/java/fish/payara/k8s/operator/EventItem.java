package fish.payara.k8s.operator;

import fish.payara.k8s.operator.resource.PayaraDomainResource;
import io.fabric8.kubernetes.client.Watcher;

public class EventItem {

    private PayaraDomainResource resource;
    private Watcher.Action action;

    public EventItem(PayaraDomainResource resource, Watcher.Action action) {
        this.resource = resource;
        this.action = action;
    }

    public PayaraDomainResource getResource() {
        return resource;
    }

    public Watcher.Action getAction() {
        return action;
    }
}
