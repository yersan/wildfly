/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.microprofile.lra.participant;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.service.CompositeDependency;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SupplierDependency;
import org.wildfly.extension.microprofile.lra.participant._private.MicroProfileLRAParticipantLogger;
import org.wildfly.extension.microprofile.lra.participant.deployment.LRAParticipantDeploymentDependencyProcessor;
import org.wildfly.extension.microprofile.lra.participant.deployment.LRAParticipantDeploymentSetupProcessor;
import org.wildfly.extension.microprofile.lra.participant.deployment.LRAParticipantResourceDeploymentUnitProcessor;
import org.wildfly.extension.microprofile.lra.participant.service.LRAParticipantService;
import org.wildfly.extension.undertow.Capabilities;
import org.wildfly.extension.undertow.Constants;
import org.wildfly.extension.undertow.Host;
import org.wildfly.extension.undertow.UndertowService;

import java.util.Arrays;

import static org.jboss.as.controller.OperationContext.Stage.RUNTIME;
import static org.jboss.as.server.deployment.Phase.DEPENDENCIES;
import static org.jboss.as.server.deployment.Phase.STRUCTURE;
import static org.wildfly.extension.microprofile.lra.participant.MicroProfileLRAParticipantExtension.SUBSYSTEM_NAME;
import static org.wildfly.extension.microprofile.lra.participant.MicroProfileLRAParticipantSubsystemDefinition.ATTRIBUTES;

public class MicroProfileLRAParticipantAdd extends AbstractBoottimeAddStepHandler {
    static MicroProfileLRAParticipantAdd INSTANCE = new MicroProfileLRAParticipantAdd();

    private MicroProfileLRAParticipantAdd() {
        super(Arrays.asList(ATTRIBUTES));
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        MicroProfileLRAParticipantLogger.LOGGER.trace("MicroProfileLRAParticipantAdd.populateModel");
        MicroProfileLRAParticipantSubsystemDefinition.URL.validateAndSet(operation, model);
    }

    @Override
    protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        super.performBoottime(context, operation, model);
        final String url = MicroProfileLRAParticipantSubsystemDefinition.URL.resolveModelAttribute(context, model).asString();
        System.setProperty("lra.coordinator.url", url);

        context.addStep(new AbstractDeploymentChainStep() {
            public void execute(DeploymentProcessorTarget processorTarget) {

                // TODO Put these into Phase.java https://issues.redhat.com/browse/WFCORE-5559
                final int STRUCTURE_MICROPROFILE_LRA_PARTICIPANT = 0x2400;
                final int DEPENDENCIES_MICROPROFILE_LRA_PARTICIPANT = 0x18D0;

                processorTarget.addDeploymentProcessor(SUBSYSTEM_NAME, STRUCTURE, STRUCTURE_MICROPROFILE_LRA_PARTICIPANT, new LRAParticipantDeploymentSetupProcessor());
                processorTarget.addDeploymentProcessor(SUBSYSTEM_NAME, DEPENDENCIES, DEPENDENCIES_MICROPROFILE_LRA_PARTICIPANT, new LRAParticipantDeploymentDependencyProcessor());
                processorTarget.addDeploymentProcessor(SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_JAXRS_SCANNING, new LRAParticipantResourceDeploymentUnitProcessor());
            }
        }, RUNTIME);

        registerParticipantProxyService(context, model);

        MicroProfileLRAParticipantLogger.LOGGER.activatingSubsystem(url);
    }

    private void registerParticipantProxyService(final OperationContext context, final ModelNode model) throws OperationFailedException {
        CapabilityServiceBuilder<?> builder = context.getCapabilityServiceTarget()
            .addCapability(MicroProfileLRAParticipantSubsystemDefinition.LRA_PARTICIPANT_CAPABILITY);

        builder.requiresCapability(Capabilities.CAPABILITY_UNDERTOW, UndertowService.class);
        String serverModelValue = model.get(CommonAttributes.SERVER).asString(Constants.DEFAULT_SERVER);
        String hostModelValue = model.get(CommonAttributes.HOST).asString(Constants.DEFAULT_HOST);
        ServiceName hostService = UndertowService.virtualHostName(serverModelValue, hostModelValue);
        SupplierDependency<Host> hostSupplier = new ServiceSupplierDependency<>(hostService);
        new CompositeDependency(hostSupplier).register(builder);

        final LRAParticipantService lraParticipantProxyService = new LRAParticipantService(hostSupplier);

        builder.setInstance(lraParticipantProxyService);
        builder.setInitialMode(ServiceController.Mode.ACTIVE).install();
    }
}