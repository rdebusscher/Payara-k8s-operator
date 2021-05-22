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
package be.rubus.accelerator.balance.prime;

import java.math.BigInteger;

public class PrimeThread implements Runnable {

    private PrimeData primeData;

    public PrimeThread(PrimeData primeData) {
        this.primeData = primeData;
    }

    @Override
    public void run() {
        BigInteger divider = primeData.getNextDivider();
        while (!divider.equals(BigInteger.ZERO)) {
            if (primeData.getPrimeCandidate().remainder(divider).equals(BigInteger.ZERO)) {
                primeData.dividerFound();
            }
            divider = primeData.getNextDivider();
        }
    }
}
