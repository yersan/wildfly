package org.wildfly.test.integration.web.annotationsmodule;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author Stuart Douglas
 */
@WebServlet(name = "ModuleServlet2", urlPatterns = "/servlet2")
public class ModuleServlet2 extends HttpServlet {

    public static final String MODULE_SERVLET = "Module Servlet2";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.getWriter().write(MODULE_SERVLET);
    }
}
