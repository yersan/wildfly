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

package org.wildfly.extension.microprofile.lra.coordinator;

import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.undertow.Constants;

import static org.wildfly.extension.microprofile.lra.coordinator.MicroProfileLRACoordinatorExtension.SUBSYSTEM_NAME;
import static org.wildfly.extension.microprofile.lra.coordinator.MicroProfileLRACoordinatorExtension.SUBSYSTEM_PATH;


public class MicroProfileLRACoordinatorSubsystemDefinition extends SimpleResourceDefinition {

    static final String LRA_COORDINATOR_CAPABILITY_NAME = "org.wildfly.microprofile.lra.coordinator";
    static final String LRA_RECOVERY_SERVICE_CAPABILITY_NAME = "org.wildfly.microprofile.lra.recovery";
    static final String REF_JTA_RECOVERY_CAPABILITY = "org.wildfly.transactions.xa-resource-recovery-registry";

    public static final RuntimeCapability<Void> LRA_COORDINATOR_CAPABILITY = RuntimeCapability.Builder
        .of(LRA_COORDINATOR_CAPABILITY_NAME)
        .setServiceType(Void.class)
        .build();
    public static final RuntimeCapability<Void> LRA_RECOVERY_SERVICE_CAPABILITY = RuntimeCapability.Builder
        .of(LRA_RECOVERY_SERVICE_CAPABILITY_NAME)
        .setServiceType(Void.class)
        .build();

    protected static final SimpleAttributeDefinition SERVER =
        new SimpleAttributeDefinitionBuilder(CommonAttributes.SERVER, ModelType.STRING, true)
            .setAllowExpression(true)
            .setXmlName(CommonAttributes.SERVER)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setDefaultValue(new ModelNode(Constants.DEFAULT_SERVER))
            .build();

    protected static final SimpleAttributeDefinition HOST =
        new SimpleAttributeDefinitionBuilder(CommonAttributes.HOST, ModelType.STRING, true)
            .setAllowExpression(true)
            .setXmlName(CommonAttributes.HOST)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setDefaultValue(new ModelNode(Constants.DEFAULT_HOST))
            .build();


    public MicroProfileLRACoordinatorSubsystemDefinition() {
        super(
            new SimpleResourceDefinition.Parameters(
                SUBSYSTEM_PATH, MicroProfileLRACoordinatorExtension.getResourceDescriptionResolver(SUBSYSTEM_NAME))
                .setAddHandler(MicroProfileLRACoordinatorAdd.INSTANCE)
                .setRemoveHandler(ReloadRequiredRemoveStepHandler.INSTANCE)
                .setCapabilities(LRA_COORDINATOR_CAPABILITY, LRA_RECOVERY_SERVICE_CAPABILITY)
        );
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadWriteAttribute(SERVER, null, new ReloadRequiredWriteAttributeHandler(SERVER));
        resourceRegistration.registerReadWriteAttribute(HOST, null, new ReloadRequiredWriteAttributeHandler(HOST));
    }

}