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

import javax.annotation.PostConstruct;
import javax.enterprise.concurrent.ManagedThreadFactory;
import javax.enterprise.context.ApplicationScoped;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Random;

@ApplicationScoped
public class PrimeService {


    private ManagedThreadFactory threadFactory;

    @PostConstruct
    public void init() {
        try {
            InitialContext ctx = new InitialContext();
            threadFactory = (ManagedThreadFactory) ctx.lookup("java:comp/DefaultManagedThreadFactory");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    public String getPrime() {
        int length = 55;
        BigInteger value = BigInteger.probablePrime(length, new Random());

        PrimeData data = new PrimeData(value);
        // This multithreaded stuff will not make it faster, only use much more CPUs (which we want here, just create load)
        for (int i = 0; i < 8; i++) {
            threadFactory.newThread(new PrimeThread(data)).start();
        }
        data.waitFinishCalculations();

        String isPrime = data.getPrime() ? "" : "NOT";
        try {
            return String.format("Number %s is %s a prime (%s) \n", value, isPrime, getServerInfo());
        } catch (UnknownHostException e) {
            return e.getMessage();
        }


    }

    private String getServerInfo() throws UnknownHostException {
        return String.format("Executed From Server: %s", InetAddress.getLocalHost().getHostName());
    }

}
