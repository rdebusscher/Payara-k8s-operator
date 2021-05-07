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
