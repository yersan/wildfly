package org.jboss.as.mail.extension;

import static org.jboss.as.mail.extension.MailTransformers.MODEL_VERSION_EAP8;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;


public class MailTransformersTestCase extends AbstractSubsystemBaseTest {

    public MailTransformersTestCase() {
        super(MailExtension.SUBSYSTEM_NAME, new MailExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("mail_5_0-transformers.xml");
    }

    @Test
    public void testTransformerEAP800() throws Exception {
        testTransformation(ModelTestControllerVersion.WILDFLY_31_0_0, MODEL_VERSION_EAP8);
    }

    /**
     *
     * @param controllerVersion The Controller version to test
     * @param version The Subsystem Model version used on the controller version to test
     * @throws Exception
     */
    private void testTransformation(ModelTestControllerVersion controllerVersion, ModelVersion version) throws Exception {
        //kernel services builder
        KernelServicesBuilder builder = this.createKernelServicesBuilder(new AdditionalInitialization.ManagementAdditionalInitialization(controllerVersion))
                .setSubsystemXml(this.getSubsystemXml());

        // Creates the kernel services for the legacy version
        builder.createLegacyKernelServicesBuilder(new AdditionalInitialization.ManagementAdditionalInitialization(controllerVersion), controllerVersion, version)
                .addMavenResourceURL(getMailGav(controllerVersion))
                .skipReverseControllerCheck()
                .dontPersistXml();

        KernelServices services = builder.build();
        Assert.assertTrue(ModelTestControllerVersion.MASTER + " boot failed", services.isSuccessfulBoot());
        Assert.assertTrue(controllerVersion.getMavenGavVersion() + " boot failed", services.getLegacyServices(version).isSuccessfulBoot());


        // check that both versions of the legacy model are the same and valid
        checkSubsystemModelTransformation(services, version, null, false);

        ModelNode transformed = services.readTransformedModel(version);
        assertNotNull(transformed);
    }

    private static String getMailGav(final ModelTestControllerVersion controller){
        String artifact = controller.getCoreVersion().equals(controller.getMavenGavVersion()) ? "jboss-as-mail" : "wildfly-mail";
        return controller.getMavenGroupId() + ":"+artifact+":" + controller.getMavenGavVersion();
    }
}
