/*
 * Copyright (c) [2021] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fish.payara.k8s.operator.util;

import io.fabric8.kubernetes.api.model.Pod;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;

public class AliveDetector implements Runnable {

    private final CountDownLatch upSignal = new CountDownLatch(1);

    private final PodUtil podUtil;
    private final String name;
    private final boolean verbose;

    boolean upAndRunning = false;

    public AliveDetector(PodUtil podUtil, String name, boolean verbose) {
        this.podUtil = podUtil;
        this.name = name;
        this.verbose = verbose;
    }

    @Override
    public void run() {
        // First we have to make sure the pod has an IP, so get the IP (or wait until we have one)
        String ip = waitForIP();
        if (ip == null) { // if we can't find the pod or can't determine the IP Address
            // Stop this thread
            upAndRunning = true;
        }
        if (verbose) {
            LogHelper.log("Wait until domain is up at " + ip);
        }
        // FIXME Define some timeout so that we do not wait indefinitely.
        try {
            while (!upAndRunning) {
                Thread.sleep(500);
                upAndRunning = isRunning(ip, 4848, 500);
            }
            upAndRunning = true;
        } catch (InterruptedException e) {
            LogHelper.exception(e);
        } finally {

            upSignal.countDown();  // Using countdownLatch to have efficient waiting without Thread.sleep.
        }

    }

    private String waitForIP() {
        boolean ready = false;
        Pod pod = null;
        while (!ready) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // We always need to get the latest info about the POD as otherwise we would not see te status change.
            pod = podUtil.lookupPod(name);
            if (pod == null) {
                // TODO Check when this can happen and the consequences.
                return null;
            }
            if (!pod.getStatus().getContainerStatuses().isEmpty()) {
                ready = pod.getStatus().getContainerStatuses().get(0).getReady();  // Checks only 1 container in the pod! Ok for now.
            }
        }
        return podUtil.lookupIP(pod);
    }

    public void waitUntilReady() throws InterruptedException {
        upSignal.await();  // Efficient waiting until the Server is up, see run()
    }

    /**
     * Call Host and IP to see if we get a connection. If so, the DAS is up and running.
     *
     * @param host
     * @param port
     * @param timeoutMilliseconds
     * @return
     */
    private static boolean isRunning(String host, int port, int timeoutMilliseconds) {
        try (Socket server = new Socket()) {
            if (host == null) {
                host = InetAddress.getByName(null).getHostName();
            }

            InetSocketAddress whom = new InetSocketAddress(host, port);
            server.connect(whom, timeoutMilliseconds);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public boolean isUpAndRunning() {
        return upAndRunning;
    }

}
