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

import java.util.ArrayList;
import java.util.List;

public class StressTest {

    private static List<TargetCaller> users = new ArrayList<>();
    private static final String target = "http://localhost:28080/testapp/prime";

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
