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
package be.rubus.accelerator.stress;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class TargetCaller implements Runnable {

    private String target;
    private Random random;

    private boolean active;

    public TargetCaller(String target) {
        this.target = target;
        random = new Random();
        active = true;

    }

    @Override
    public void run() {

        while (active) {
            try {
                sendGet(target);

                int wait = random.nextInt(300);

                Thread.sleep(wait);

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    private void sendGet(String url) throws Exception {


        URL target = new URL(url);
        HttpURLConnection con = (HttpURLConnection) target.openConnection();

        // optional default is GET
        con.setRequestMethod("GET");

        int responseCode = con.getResponseCode();

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        System.out.printf("Time %s - URL : %s - response code %s - value %s%n", new Date(), url, responseCode, response);


    }

    public void stopThread() {
        active = false;
    }
}
