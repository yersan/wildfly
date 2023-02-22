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

import org.jboss.as.controller.PersistentResourceXMLParser;
import org.jboss.as.controller.PersistentResourceXMLDescription;

import static org.jboss.as.controller.PersistentResourceXMLDescription.builder;
import static org.wildfly.extension.microprofile.lra.participant.MicroProfileLRAParticipantSubsystemDefinition.URL;


public class MicroProfileLRAParticipantParser_1_0  extends PersistentResourceXMLParser {
    private static final PersistentResourceXMLDescription xmlDescription;

    static {
        xmlDescription = builder(MicroProfileLRAParticipantExtension.SUBSYSTEM_PATH,
            Namespace.LRA_PARTICIPANT_1_0.getUriString())
            .addAttributes(URL)
            .build();
    }

    @Override
    public PersistentResourceXMLDescription getParserDescription() {
        return xmlDescription;
    }

}