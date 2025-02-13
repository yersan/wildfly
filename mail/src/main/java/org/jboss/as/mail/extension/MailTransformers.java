/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.jboss.as.mail.extension;

import static org.jboss.as.mail.extension.MailExtension.CURRENT_MODEL_VERSION;
import static org.jboss.as.mail.extension.MailExtension.MAIL_SESSION_PATH;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.transform.ExtensionTransformerRegistration;
import org.jboss.as.controller.transform.SubsystemTransformerRegistration;
import org.jboss.as.controller.transform.description.ChainedTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;

/**
 * @author Tomaz Cerar (c) 2017 Red Hat Inc.
 */
public class MailTransformers implements ExtensionTransformerRegistration {
//    static final ModelVersion MODEL_VERSION_EAP74 = ModelVersion.create(4, 0, 0);
    static final ModelVersion MODEL_VERSION_EAP8 = ModelVersion.create(4, 0, 0);

    @Override
    public String getSubsystemName() {
        return MailExtension.SUBSYSTEM_NAME;
    }

    @Override
    public void registerTransformers(SubsystemTransformerRegistration subsystem) {
        ChainedTransformationDescriptionBuilder chained = ResourceTransformationDescriptionBuilder.Factory.createChainedSubystemInstance(CURRENT_MODEL_VERSION);

        // EAP 8.0.0
        ResourceTransformationDescriptionBuilder builder80 = chained.createBuilder(CURRENT_MODEL_VERSION, MODEL_VERSION_EAP8);
        ResourceTransformationDescriptionBuilder sessionBuilder80 = builder80.addChildResource(MAIL_SESSION_PATH);
        sessionBuilder80.getAttributeBuilder()
//                .addRejectCheck(new RejectAttributeChecker.SimpleRejectAttributeChecker(ModelNode.TRUE), MailSessionDefinition.TEST)
//                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(ModelNode.FALSE), MailSessionDefinition.TEST)
//                .end();

                .setDiscard(DiscardAttributeChecker.ALWAYS, MailSessionDefinition.TEST)
                .end();
//
//        ResourceTransformationDescriptionBuilder sessionBuilder74 = builder80.addChildResource(MAIL_SESSION_PATH);
//        sessionBuilder74.getAttributeBuilder()
//                .setDiscard(DiscardAttributeChecker.ALWAYS, MailSessionDefinition.TEST)
//                .end();

        /// Should register the actual version???
        chained.buildAndRegister(subsystem, new ModelVersion[]{MODEL_VERSION_EAP8});
    }
}
