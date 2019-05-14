package org.wildfly.test.integration.web.annotationsmodule;

import org.jboss.as.naming.InitialContext;

import javax.annotation.Resource;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet(urlPatterns = "/test")
public class TestServlet extends HttpServlet {
    @Resource
    private InitialContext initialContext;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        TestEjb ejb = lookup();
        try(PrintWriter writer = resp.getWriter()) {
            if (ejb != null) {
                writer.write(ejb.hello());
                resp.setStatus(200);
            } else {
                resp.setStatus(500);
            }
        }
    }

    private TestEjb lookup() {
        try {
            Object lookup = InitialContext.doLookup("java:app/web/TestEjb!org.wildfly.test.integration.web.annotationsmodule.TestEjb");
            return (TestEjb) lookup;
        } catch (NamingException e) {
            e.printStackTrace();
        }
        return null;
    }
}
