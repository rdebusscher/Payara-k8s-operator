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

    private String getHost() {
        try {
            return "\nExecuted on "+InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "Unable to retrieve host";
        }
    }

}
