package org.wildfly.test.integration.web.annotationsmodule;

import javax.ejb.Stateless;

@Stateless
public class TestEjb {

    public static final String TEST_EJB = "TestEjb";

    public String hello() {
        return TEST_EJB;
    }
}
