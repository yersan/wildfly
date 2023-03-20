/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.test.integration.domain.mixed.eap740;

import org.jboss.as.test.integration.domain.mixed.KernelBehaviorTestSuite;
import org.jboss.as.test.integration.domain.mixed.MixedDomainTestSuite;
import org.jboss.as.test.integration.domain.mixed.Version;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Testsuite for tests that uses a minimal domain config in order
 * to not have to deal with subsystem configuration compatibility issues
 * across releases in tests that are focused on the behavior of the kernel.
 *
 * @author Brian Stansberry
 */
@RunWith(Suite.class)
@Suite.SuiteClasses(value= {RBACConfig740TestCase.class, WildcardReads740TestCase.class})
@Version(Version.AsVersion.EAP_7_4_0)
public class KernelBehavior740TestSuite extends KernelBehaviorTestSuite {

    private static boolean initializedLocally = false;

    @BeforeClass
    public static void initSuite() {
        initializedLocally = true;
        KernelBehaviorTestSuite.getSupport(KernelBehavior740TestSuite.class);
    }

    @AfterClass
    public static void tearDownSuite() {
        MixedDomainTestSuite.afterClass();
    }

    // This can only be called from tests as part of this suite
    public static synchronized void createSupport(Class<?> testClass) {
        KernelBehaviorTestSuite.getSupport(testClass);
    }

    // This can only be called from tests as part of this suite
    public static synchronized void stopSupport() {
        if(! initializedLocally) {
            MixedDomainTestSuite.afterClass();
        }
    }
}
