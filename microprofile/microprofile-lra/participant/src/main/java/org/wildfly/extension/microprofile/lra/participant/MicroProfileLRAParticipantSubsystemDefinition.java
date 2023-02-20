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

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

import java.util.Arrays;
import java.util.Collection;

import static org.wildfly.extension.microprofile.lra.participant.MicroProfileLRAParticipantExtension.SUBSYSTEM_NAME;
import static org.wildfly.extension.microprofile.lra.participant.MicroProfileLRAParticipantExtension.SUBSYSTEM_PATH;

public class MicroProfileLRAParticipantSubsystemDefinition extends PersistentResourceDefinition {

    private static final String LRA_PARTICIPANT_CAPABILITY_NAME = "org.wildfly.microprofile.lra.participant";

    public static final RuntimeCapability<Void> LRA_PARTICIPANT_CAPABILITY = RuntimeCapability.Builder
        .of(LRA_PARTICIPANT_CAPABILITY_NAME)
        .setServiceType(Void.class)
        .build();

    protected static final SimpleAttributeDefinition URL =
        new SimpleAttributeDefinitionBuilder(CommonAttributes.URL, ModelType.STRING, true)
            .setRequired(false)
            .setDefaultValue(new ModelNode(CommonAttributes.DEFAULT_URL))
            .setAllowExpression(true)
            .setXmlName(CommonAttributes.URL)
            .setRestartAllServices()
            .build();

    public MicroProfileLRAParticipantSubsystemDefinition() {
        super(
            new SimpleResourceDefinition.Parameters(
                SUBSYSTEM_PATH,
                MicroProfileLRAParticipantExtension.getResourceDescriptionResolver(SUBSYSTEM_NAME))
                .setAddHandler(MicroProfileLRAParticipantAdd.INSTANCE)
                .setRemoveHandler(new ReloadRequiredRemoveStepHandler())
                .setCapabilities(LRA_PARTICIPANT_CAPABILITY)
        );
    }

    static final AttributeDefinition[] ATTRIBUTES = {URL};

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadWriteAttribute(URL, null, new ReloadRequiredWriteAttributeHandler(URL));
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Arrays.asList(ATTRIBUTES);
    }


}