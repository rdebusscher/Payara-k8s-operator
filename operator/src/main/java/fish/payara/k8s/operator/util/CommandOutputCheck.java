package fish.payara.k8s.operator.util;

@FunctionalInterface
public interface CommandOutputCheck {

    boolean isCommandExecutedSuccessful(String output);
}
