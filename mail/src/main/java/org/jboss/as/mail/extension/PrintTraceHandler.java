package org.jboss.as.mail.extension;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author <a href="mailto:yborgess@redhat.com">Yeray Borges</a>
 */
public class PrintTraceHandler implements OperationStepHandler {

    static final SimpleAttributeDefinition VERBOSE = new SimpleAttributeDefinitionBuilder("verbose", ModelType.BOOLEAN, true)
            .setAllowExpression(false)
            .setDefaultValue(ModelNode.TRUE)
            .build();

    public static final SimpleOperationDefinition PRINT_DEFINITION = new SimpleOperationDefinitionBuilder("print-trace", MailExtension.getResourceDescriptionResolver(MailSubsystemModel.MAIL_SESSION))
            .setParameters(VERBOSE)
            .build();

    public static OperationStepHandler INSTANCE = new PrintTraceHandler();

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        context.addStep(new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                MailLogger.ROOT_LOGGER.info("operation=" + operation);
            }
        }, OperationContext.Stage.RUNTIME);
    }
}
