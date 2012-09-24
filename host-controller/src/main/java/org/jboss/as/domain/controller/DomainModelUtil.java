/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.domain.controller;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONCURRENT_GROUPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.IN_SERIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_CLIENT_CONTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_SUBSYSTEM_ENDPOINT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MAX_FAILED_SERVERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MAX_FAILURE_PERCENTAGE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROFILE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLBACK_ACROSS_GROUPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLING_TO_SERVERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLOUT_PLAN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLOUT_PLANS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_PORT_OFFSET;
import static org.jboss.as.domain.controller.DomainControllerMessages.MESSAGES;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.extension.ExtensionResourceDefinition;
import org.jboss.as.controller.operations.common.InterfaceAddHandler;
import org.jboss.as.controller.operations.common.InterfaceRemoveHandler;
import org.jboss.as.controller.operations.global.WriteAttributeHandlers;
import org.jboss.as.controller.operations.global.WriteAttributeHandlers.IntRangeValidatingHandler;
import org.jboss.as.controller.operations.validation.AbstractParameterValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.persistence.ExtensibleConfigurationPersister;
import org.jboss.as.controller.registry.AttributeAccess.Storage;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.resource.InterfaceDefinition;
import org.jboss.as.controller.resource.SocketBindingGroupResourceDefinition;
import org.jboss.as.controller.services.path.PathManagerService;
import org.jboss.as.controller.services.path.PathResourceDefinition;
import org.jboss.as.domain.controller.descriptions.DomainDescriptionProviders;
import org.jboss.as.domain.controller.descriptions.DomainRootDescription;
import org.jboss.as.domain.controller.operations.DomainServerLifecycleHandlers;
import org.jboss.as.domain.controller.operations.DomainSocketBindingGroupRemoveHandler;
import org.jboss.as.domain.controller.operations.ServerGroupAddHandler;
import org.jboss.as.domain.controller.operations.ServerGroupProfileWriteAttributeHandler;
import org.jboss.as.domain.controller.operations.ServerGroupRemoveHandler;
import org.jboss.as.domain.controller.operations.SocketBindingGroupAddHandler;
import org.jboss.as.domain.controller.operations.deployment.ServerGroupDeploymentReplaceHandler;
import org.jboss.as.domain.controller.resource.DomainDeploymentResourceDescription;
import org.jboss.as.domain.controller.resource.SocketBindingResourceDefinition;
import org.jboss.as.domain.controller.resources.ProfileResourceDefinition;
import org.jboss.as.host.controller.ignored.IgnoredDomainResourceRegistry;
import org.jboss.as.host.controller.model.jvm.JvmResourceDefinition;
import org.jboss.as.management.client.content.ManagedDMRContentResourceDefinition;
import org.jboss.as.management.client.content.ManagedDMRContentTypeResourceDefinition;
import org.jboss.as.repository.ContentRepository;
import org.jboss.as.repository.HostFileRepository;
import org.jboss.as.server.controller.resources.DeploymentAttributes;
import org.jboss.as.server.controller.resources.SystemPropertyResourceDefinition;
import org.jboss.as.server.controller.resources.SystemPropertyResourceDefinition.Location;
import org.jboss.as.server.deploymentoverlay.ContentDefinition;
import org.jboss.as.server.deploymentoverlay.DeploymentOverlayDefinition;
import org.jboss.as.server.deploymentoverlay.DeploymentOverlayDeploymentDefinition;
import org.jboss.as.server.deploymentoverlay.service.DeploymentOverlayPriority;
import org.jboss.as.server.services.net.LocalDestinationOutboundSocketBindingResourceDefinition;
import org.jboss.as.server.services.net.RemoteDestinationOutboundSocketBindingResourceDefinition;
import org.jboss.dmr.ModelNode;


/**
 * Utilities related to the domain model.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class DomainModelUtil {

    public static void initializeDomainRegistry(final ManagementResourceRegistration root, final ExtensibleConfigurationPersister configurationPersister,
                                                 final ContentRepository contentRepo, final HostFileRepository fileRepository, final boolean isMaster,
                                                 final LocalHostControllerInfo hostControllerInfo,
                                                 final ExtensionRegistry extensionRegistry, final IgnoredDomainResourceRegistry ignoredDomainResourceRegistry,
                                                 final PathManagerService pathManager) {

        final EnumSet<OperationEntry.Flag> masterOnly = EnumSet.of(OperationEntry.Flag.MASTER_HOST_CONTROLLER_ONLY);


        // System Properties
        root.registerSubModel(SystemPropertyResourceDefinition.createForDomainOrHost(Location.DOMAIN));

        final ManagementResourceRegistration interfaces = root.registerSubModel(new InterfaceDefinition(
                InterfaceAddHandler.NAMED_INSTANCE,
                InterfaceRemoveHandler.INSTANCE,
                false
        ));
        /*interfaces.registerOperationHandler(ADD, InterfaceAddHandler.NAMED_INSTANCE, new DefaultResourceAddDescriptionProvider(interfaces, CommonDescriptions.getResourceDescriptionResolver()), false);
        interfaces.registerOperationHandler(REMOVE, InterfaceRemoveHandler.INSTANCE, new DefaultResourceRemoveDescriptionProvider(CommonDescriptions.getResourceDescriptionResolver()), false);*/


        final ManagementResourceRegistration profile = root.registerSubModel(new ProfileResourceDefinition());

        root.registerSubModel(PathResourceDefinition.createNamed(pathManager));

        final ManagementResourceRegistration socketBindingGroup = root.registerSubModel(new SocketBindingGroupResourceDefinition(SocketBindingGroupAddHandler.INSTANCE, DomainSocketBindingGroupRemoveHandler.INSTANCE, true));
        socketBindingGroup.registerSubModel(SocketBindingResourceDefinition.INSTANCE);
        // outbound-socket-binding (for remote destination)
        socketBindingGroup.registerSubModel(RemoteDestinationOutboundSocketBindingResourceDefinition.INSTANCE);
        // outbound-socket-binding (for local destination)
        socketBindingGroup.registerSubModel(LocalDestinationOutboundSocketBindingResourceDefinition.INSTANCE);


        final ManagementResourceRegistration serverGroups = root.registerSubModel(PathElement.pathElement(SERVER_GROUP), DomainDescriptionProviders.SERVER_GROUP);
        serverGroups.registerOperationHandler(ADD, ServerGroupAddHandler.INSTANCE, ServerGroupAddHandler.INSTANCE, false);
        serverGroups.registerOperationHandler(REMOVE, ServerGroupRemoveHandler.INSTANCE, ServerGroupRemoveHandler.INSTANCE, false);
        serverGroups.registerReadWriteAttribute(SOCKET_BINDING_GROUP, null, WriteAttributeHandlers.WriteAttributeOperationHandler.INSTANCE, Storage.CONFIGURATION);
        serverGroups.registerReadWriteAttribute(SOCKET_BINDING_PORT_OFFSET, null, new IntRangeValidatingHandler(0, true), Storage.CONFIGURATION);
        serverGroups.registerReadWriteAttribute(PROFILE, null, ServerGroupProfileWriteAttributeHandler.INSTANCE, Storage.CONFIGURATION);
        serverGroups.registerReadOnlyAttribute(MANAGEMENT_SUBSYSTEM_ENDPOINT, null, Storage.CONFIGURATION);
        DomainServerLifecycleHandlers.registerServerGroupHandlers(serverGroups);

        serverGroups.registerSubModel(JvmResourceDefinition.GLOBAL);

        serverGroups.registerOperationHandler(DeploymentAttributes.SERVER_GROUP_REPLACE_DEPLOYMENT_DEFINITION, new ServerGroupDeploymentReplaceHandler(fileRepository));
        serverGroups.registerSubModel(DomainDeploymentResourceDescription.createForServerGroup(contentRepo, fileRepository));


        // Server Group System Properties
        serverGroups.registerSubModel(SystemPropertyResourceDefinition.createForDomainOrHost(Location.SERVER_GROUP));

        // Root Deployments
        root.registerSubModel(DomainDeploymentResourceDescription.createForDomainRoot(isMaster, contentRepo, fileRepository));


        //deployment overlays
        final ManagementResourceRegistration deploymentOverlays = root.registerSubModel(DeploymentOverlayDefinition.INSTANCE);
        deploymentOverlays.registerSubModel(new ContentDefinition(contentRepo, fileRepository));

        //server group deployment overlay links
        final ManagementResourceRegistration serverGroupDeploymentOverlay = serverGroups.registerSubModel(DeploymentOverlayDefinition.INSTANCE);
        serverGroupDeploymentOverlay.registerSubModel(new DeploymentOverlayDeploymentDefinition(DeploymentOverlayPriority.SERVER_GROUP));

        // Management client content
        ManagedDMRContentTypeResourceDefinition plansDef = new ManagedDMRContentTypeResourceDefinition(contentRepo, ROLLOUT_PLAN,
                PathElement.pathElement(MANAGEMENT_CLIENT_CONTENT, ROLLOUT_PLANS), DomainRootDescription.getResourceDescriptionResolver(ROLLOUT_PLANS));
        ManagementResourceRegistration mgmtContent = root.registerSubModel(plansDef);
        ParameterValidator contentValidator = new AbstractParameterValidator(){
            @Override
            public void validateParameter(String parameterName, ModelNode value) throws OperationFailedException {
                validateRolloutPlanStructure(value);
            }};
        ManagedDMRContentResourceDefinition planDef = ManagedDMRContentResourceDefinition.create(ROLLOUT_PLAN, contentValidator, DomainRootDescription.getResourceDescriptionResolver(ROLLOUT_PLAN));
        mgmtContent.registerSubModel(planDef);

        // Extensions
        root.registerSubModel(new ExtensionResourceDefinition(extensionRegistry, true, !isMaster));
        extensionRegistry.setSubsystemParentResourceRegistrations(profile, null);


        // Initialize the domain transformers
        DomainTransformers.initializeDomainRegistry(extensionRegistry.getTransformerRegistry());

    }

    public static void validateRolloutPlanStructure(ModelNode plan) throws OperationFailedException {
        if(plan == null) {
            throw new OperationFailedException(MESSAGES.nullVar("plan").getLocalizedMessage());
        }
        if(!plan.hasDefined(ROLLOUT_PLAN)) {
            throw new OperationFailedException(MESSAGES.requiredChildIsMissing(ROLLOUT_PLAN, ROLLOUT_PLAN, plan.toString()));
        }
        ModelNode rolloutPlan1 = plan.get(ROLLOUT_PLAN);

        final Set<String> keys;
        try {
            keys = rolloutPlan1.keys();
        } catch (IllegalArgumentException e) {
            throw new OperationFailedException(MESSAGES.requiredChildIsMissing(ROLLOUT_PLAN, IN_SERIES, plan.toString()));
        }
        if(!keys.contains(IN_SERIES)) {
            throw new OperationFailedException(MESSAGES.requiredChildIsMissing(ROLLOUT_PLAN, IN_SERIES, plan.toString()));
        }
        if(keys.size() > 2 || keys.size() == 2 && !keys.contains(ROLLBACK_ACROSS_GROUPS)) {
            throw new OperationFailedException(MESSAGES.unrecognizedChildren(ROLLOUT_PLAN, IN_SERIES + ", " + ROLLBACK_ACROSS_GROUPS, plan.toString()));
        }

        final ModelNode inSeries = rolloutPlan1.get(IN_SERIES);
        if(!inSeries.isDefined()) {
            throw new OperationFailedException(MESSAGES.requiredChildIsMissing(ROLLOUT_PLAN, IN_SERIES, plan.toString()));
        }

        final List<ModelNode> groups = inSeries.asList();
        if(groups.isEmpty()) {
            throw new OperationFailedException(MESSAGES.inSeriesIsMissingGroups(plan.toString()));
        }

        for(ModelNode group : groups) {
            if(group.hasDefined(SERVER_GROUP)) {
                final ModelNode serverGroup = group.get(SERVER_GROUP);
                final Set<String> groupKeys;
                try {
                    groupKeys = serverGroup.keys();
                } catch(IllegalArgumentException e) {
                    throw new OperationFailedException(MESSAGES.serverGroupExpectsSingleChild(plan.toString()));
                }
                if(groupKeys.size() != 1) {
                    throw new OperationFailedException(MESSAGES.serverGroupExpectsSingleChild(plan.toString()));
                }
                validateInSeriesServerGroup(serverGroup.asProperty().getValue());
            } else if(group.hasDefined(CONCURRENT_GROUPS)) {
                final ModelNode concurrent = group.get(CONCURRENT_GROUPS);
                for(ModelNode child: concurrent.asList()) {
                    validateInSeriesServerGroup(child.asProperty().getValue());
                }
            } else {
                throw new OperationFailedException(MESSAGES.unexpectedInSeriesGroup(plan.toString()));
            }
        }
    }

    private static final List<String> ALLOWED_SERVER_GROUP_CHILDREN = Arrays.asList(ROLLING_TO_SERVERS, MAX_FAILURE_PERCENTAGE, MAX_FAILED_SERVERS);

    private static void validateInSeriesServerGroup(ModelNode serverGroup) throws OperationFailedException {
        if(serverGroup.isDefined()) {
            try {
                final Set<String> specKeys = serverGroup.keys();
                if(!ALLOWED_SERVER_GROUP_CHILDREN.containsAll(specKeys)) {
                    throw new OperationFailedException(MESSAGES.unrecognizedChildren(SERVER_GROUP, ALLOWED_SERVER_GROUP_CHILDREN.toString(), specKeys.toString()));
                }
            } catch(IllegalArgumentException e) {// ignore?
            }
        }
    }
}
