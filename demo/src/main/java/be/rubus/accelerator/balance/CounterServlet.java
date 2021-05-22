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
package be.rubus.accelerator.balance;

import fish.payara.cdi.jsr107.impl.NamedCache;

import javax.cache.Cache;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

@WebServlet("/counter")
public class CounterServlet extends HttpServlet {

    private static final String CACHE_KEY = "RequestCount";

    @NamedCache(cacheName = "externalCache")
    @Inject
    private Cache<String, Long> examplesCache;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        StringBuilder response = new StringBuilder();

        response.append(getHost());
        response.append(getSizing());
        response.append(tick());
        response.append("\n");

        resp.getWriter().println(response.toString());
    }

    private String tick() {
        // Store stuff in JSR 107 JCache

        Long cacheCount = examplesCache.get(CACHE_KEY);
        if (cacheCount == null) {
            cacheCount = 0L;
        }
        examplesCache.put(CACHE_KEY, cacheCount + 1);

        return String.format("\nCounter value %d", cacheCount);

    }

    private String getSizing() {
        int processors = Runtime.getRuntime().availableProcessors();
        long memory = Runtime.getRuntime().maxMemory() / 1024 / 1024;
        return String.format("\nExecuted with %s processors and %s Mb ", processors, memory);

    }

    private String getHost() {
        try {
            return "\nExecuted on " + InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "Unable to retrieve host";
        }
    }

}
