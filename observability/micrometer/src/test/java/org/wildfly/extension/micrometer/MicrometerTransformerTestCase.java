package org.wildfly.extension.micrometer;

import static org.wildfly.extension.micrometer.MicrometerSubsystemModel.VERSION_1_1_0;
import static org.wildfly.extension.micrometer.MicrometerSubsystemModel.VERSION_2_0_0;

import java.util.EnumSet;
import java.util.Objects;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/*
 * Currently ignore as I keep getting this error:
 * [ERROR] testTransformation[0](org.wildfly.extension.micrometer.MicrometerTransformerTestCase)  Time elapsed: 0.945 s  <<< ERROR!
 * java.lang.IllegalStateException: Url file:/home/jdlee/.m2/repository/org/wildfly/core/wildfly-subsystem-test-framework/23.0.0.Beta5/wildfly-subsystem-test-framework-23.0.0.Beta5.jar which is the code source for the following classes has already been set up via other means: [org.jboss.as.subsystem.test.AdditionalInitialization]
 *
 * I don't know why, but, for now, I'd like to get a full CI run to verify the other changes. Will come back to this
 * "soon".
 */
@RunWith(value = Parameterized.class)
public class MicrometerTransformerTestCase extends AbstractSubsystemTest {

    private final ModelTestControllerVersion controller;
    private final ModelVersion version;

    @Parameters
    public static Iterable<ModelTestControllerVersion> parameters() {
        return EnumSet.of(ModelTestControllerVersion.EAP_8_0_0);
    }

    public MicrometerTransformerTestCase(ModelTestControllerVersion controller) {
        super(MicrometerConfigurationConstants.NAME, new MicrometerExtension());
        this.controller = controller;
        version = VERSION_2_0_0.getVersion();
    }

    @Test
    public void testTransformation() throws Exception {
        String subsystemXmlResource = String.format("micrometer-transform-%d_%d_%d.xml",
                version.getMajor(), version.getMinor(), version.getMicro());

        // create builder for current subsystem version
        AdditionalInitialization additionalInitialization = createAdditionalInitialization();
        KernelServicesBuilder builder = createKernelServicesBuilder(additionalInitialization)
                .setSubsystemXmlResource(subsystemXmlResource);

        // initialize the legacy services and add required jars
        builder.createLegacyKernelServicesBuilder(additionalInitialization, controller, VERSION_1_1_0.getVersion())
                .skipReverseControllerCheck()
                .addMavenResourceURL(getDependencies())
                .dontPersistXml();

        KernelServices services = builder.build();

        Assert.assertTrue(services.isSuccessfulBoot());
        Assert.assertTrue(services.getLegacyServices(VERSION_1_1_0.getVersion()).isSuccessfulBoot());

        // check that both versions of the legacy model are the same and valid
        checkSubsystemModelTransformation(services, VERSION_1_1_0.getVersion(), null, false);
    }

    @Test
    public void testRejections() throws Exception {

    }

    private AdditionalInitialization createAdditionalInitialization() {
        return AdditionalInitialization.MANAGEMENT;
    }

    private MicrometerSubsystemModel getModelVersion() {
        if (Objects.requireNonNull(controller) == ModelTestControllerVersion.EAP_8_0_0) {
            return VERSION_1_1_0;
        }
        throw new IllegalArgumentException();
    }

    private String[] getDependencies() {
        if (Objects.requireNonNull(controller) == ModelTestControllerVersion.EAP_8_0_0) {
            return new String[]{
                    formatSubsystemArtifact()
            };
        }
        throw new IllegalArgumentException();
    }

    private String formatSubsystemArtifact() {
        return formatArtifact("org.wildfly:wildfly-micrometer:%s");
    }

    private String formatArtifact(String pattern) {
        return String.format(pattern, controller.getMavenGavVersion());
    }
}
