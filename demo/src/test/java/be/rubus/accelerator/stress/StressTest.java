package be.rubus.accelerator.stress;

import java.util.ArrayList;
import java.util.List;

public class StressTest {

    private static List<TargetCaller> users = new ArrayList<>();
    private static final String target = "http://localhost:30808/testapp/prime";

    public static void main(String[] args) {
        scaleThreads(5);
    }

    private static void scaleThreads(Integer count) {
        if (count > users.size()) {
            for (int i = users.size(); i < count; i++) {
                TargetCaller targetCaller = new TargetCaller(target);
                users.add(targetCaller);
                new Thread(targetCaller).start();
            }
        }
        if (count < users.size()) {
            for (int i = count; i < users.size(); i++) {
                users.get(i).stopThread();
            }

        }
    }

}
