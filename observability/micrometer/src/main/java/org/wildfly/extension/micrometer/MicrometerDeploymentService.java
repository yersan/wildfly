/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.micrometer;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBDEPLOYMENT;

import java.util.function.Predicate;
import java.util.function.Supplier;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.msc.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.wildfly.extension.micrometer.metrics.MetricRegistration;
import org.wildfly.extension.micrometer.metrics.MicrometerCollector;

class MicrometerDeploymentService implements Service {
    private final Resource rootResource;
    private final ManagementResourceRegistration managementResourceRegistration;
    private final PathAddress deploymentAddress;
    private final Supplier<MicrometerCollector> metricCollector;
    private final Predicate<String> subsystemFilter;

    private volatile MetricRegistration registration;

    MicrometerDeploymentService(Resource rootResource,
                                        ManagementResourceRegistration managementResourceRegistration,
                                        PathAddress deploymentAddress,
                                        Supplier<MicrometerCollector> metricCollectorSupplier,
                                        Predicate<String> subsystemFilter) {
        this.rootResource = rootResource;
        this.managementResourceRegistration = managementResourceRegistration;
        this.deploymentAddress = deploymentAddress;
        this.metricCollector = metricCollectorSupplier;
        this.subsystemFilter = subsystemFilter;
    }

    static PathAddress createDeploymentAddressPrefix(DeploymentUnit deploymentUnit) {
        if (deploymentUnit.getParent() == null) {
            return PathAddress.pathAddress(DEPLOYMENT, deploymentUnit.getAttachment(Attachments.MANAGEMENT_NAME));
        } else {
            return createDeploymentAddressPrefix(deploymentUnit.getParent()).append(SUBDEPLOYMENT, deploymentUnit.getName());
        }
    }

    @Override
    public void start(StartContext context) {
        registration = metricCollector.get()
                .collectResourceMetrics(rootResource,
                        managementResourceRegistration,
                        // prepend the deployment address to the subsystem resource address
                        deploymentAddress::append,
                        subsystemFilter);
    }

    @Override
    public void stop(StopContext context) {
        registration.unregister();
    }
}
