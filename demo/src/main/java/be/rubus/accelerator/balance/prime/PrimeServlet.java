package be.rubus.accelerator.balance.prime;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/prime")
public class PrimeServlet extends HttpServlet {

    @Inject
    private PrimeService primeService;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        String result = primeService.getPrime();
        resp.getWriter().println(result);
    }
}
