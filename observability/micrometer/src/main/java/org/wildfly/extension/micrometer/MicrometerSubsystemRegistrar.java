/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.micrometer;

import static org.jboss.as.controller.PathAddress.EMPTY_ADDRESS;
import static org.jboss.as.server.deployment.Phase.DEPENDENCIES;
import static org.jboss.as.server.deployment.Phase.DEPENDENCIES_MICROMETER;
import static org.jboss.as.server.deployment.Phase.POST_MODULE;
import static org.jboss.as.server.deployment.Phase.POST_MODULE_MICROMETER;
import static org.wildfly.extension.micrometer.MicrometerConfigurationConstants.MICROMETER_API_MODULE;
import static org.wildfly.extension.micrometer.MicrometerConfigurationConstants.MICROMETER_MODULE;
import static org.wildfly.extension.micrometer.MicrometerExtensionLogger.MICROMETER_LOGGER;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelControllerClientFactory;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProcessStateNotifier;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ParentResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.SubsystemResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceName;
import org.wildfly.extension.micrometer.jmx.JmxMicrometerCollector;
import org.wildfly.extension.micrometer.metrics.MicrometerCollector;
import org.wildfly.extension.micrometer.otlp.OtlpRegistryDefinitionRegistrar;
import org.wildfly.extension.micrometer.registry.WildFlyCompositeRegistry;
import org.wildfly.subsystem.resource.AttributeTranslation;
import org.wildfly.subsystem.resource.ManagementResourceRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.SubsystemResourceDefinitionRegistrar;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;

public class MicrometerSubsystemRegistrar implements SubsystemResourceDefinitionRegistrar, ResourceServiceConfigurator {
    static final String CLIENT_FACTORY_CAPABILITY = "org.wildfly.management.model-controller-client-factory";
    static final String MANAGEMENT_EXECUTOR = "org.wildfly.management.executor";
    static final String PROCESS_STATE_NOTIFIER = "org.wildfly.management.process-state-notifier";

    static final PathElement PATH = SubsystemResourceDefinitionRegistrar.pathElement(MicrometerConfigurationConstants.NAME);
    public static final ParentResourceDescriptionResolver RESOLVER =
            new SubsystemResourceDescriptionResolver(MicrometerConfigurationConstants.NAME, MicrometerSubsystemRegistrar.class);

    static final RuntimeCapability<Void> MICROMETER_COLLECTOR_RUNTIME_CAPABILITY =
            RuntimeCapability.Builder.of(MICROMETER_MODULE + ".wildfly-collector", MicrometerCollector.class)
                    .addRequirements(CLIENT_FACTORY_CAPABILITY, MANAGEMENT_EXECUTOR, PROCESS_STATE_NOTIFIER)
                    .build();
    static final ServiceName MICROMETER_COLLECTOR = MICROMETER_COLLECTOR_RUNTIME_CAPABILITY.getCapabilityServiceName();
    static final String[] MODULES = {
    };

    static final String[] EXPORTED_MODULES = {
            MICROMETER_API_MODULE,
            "io.opentelemetry.otlp",
            "io.micrometer"
    };

    @Deprecated
    public static final SimpleAttributeDefinition ENDPOINT = SimpleAttributeDefinitionBuilder
            .create(MicrometerConfigurationConstants.ENDPOINT, ModelType.STRING)
            .setRequired(false)
            .addFlag(AttributeAccess.Flag.ALIAS)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    @Deprecated
    public static final SimpleAttributeDefinition STEP = SimpleAttributeDefinitionBuilder
            .create(MicrometerConfigurationConstants.STEP, ModelType.LONG, true)
            .setDefaultValue(new ModelNode(TimeUnit.MINUTES.toSeconds(1)))
            .setMeasurementUnit(MeasurementUnit.SECONDS)
            .addFlag(AttributeAccess.Flag.ALIAS)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    static final StringListAttributeDefinition EXPOSED_SUBSYSTEMS =
            new StringListAttributeDefinition.Builder("exposed-subsystems")
                    .setDefaultValue(ModelNode.fromJSONString("[\"*\"]"))
                    .setRequired(false)
                    .setRestartAllServices()
                    .build();

    static final Collection<AttributeDefinition> ATTRIBUTES = List.of(EXPOSED_SUBSYSTEMS);

    private final AtomicReference<MicrometerDeploymentConfiguration> deploymentConfig = new AtomicReference<>();
    private final WildFlyCompositeRegistry wildFlyRegistry = new WildFlyCompositeRegistry();

    @Override
    public ManagementResourceRegistration register(SubsystemRegistration parent,
                                                   ManagementResourceRegistrationContext context) {
        ManagementResourceRegistration registration =
                parent.registerSubsystemModel(ResourceDefinition.builder(ResourceRegistration.of(PATH), RESOLVER).build());
        UnaryOperator<PathAddress> translator =
                pathElements -> pathElements.append(OtlpRegistryDefinitionRegistrar.PATH);
        ResourceDescriptor descriptor = ResourceDescriptor.builder(RESOLVER)
                .withRuntimeHandler(ResourceOperationRuntimeHandler.configureService(this))
                .addCapability(MICROMETER_COLLECTOR_RUNTIME_CAPABILITY)
                .addAttributes(ATTRIBUTES)
                .translateAttribute(ENDPOINT, AttributeTranslation.relocate(ENDPOINT, translator))
                .translateAttribute(STEP, AttributeTranslation.relocate(STEP, translator))
                .withAddResourceOperationTransformation(new TranslateOtlpHandler())
                .withDeploymentChainContributor(target -> {
                    target.addDeploymentProcessor(MicrometerConfigurationConstants.NAME, DEPENDENCIES, DEPENDENCIES_MICROMETER,
                            new MicrometerDependencyProcessor());
                    target.addDeploymentProcessor(MicrometerConfigurationConstants.NAME, POST_MODULE, POST_MODULE_MICROMETER,
                            new MicrometerDeploymentProcessor(deploymentConfig.get()));
                })
                .withAddOperationRestartFlag(OperationEntry.Flag.RESTART_ALL_SERVICES)
                .withRemoveOperationRestartFlag(OperationEntry.Flag.RESTART_ALL_SERVICES)
                .build();

        ManagementResourceRegistrar.of(descriptor).register(registration);
        new OtlpRegistryDefinitionRegistrar(wildFlyRegistry).register(registration, context);

        return registration;
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        List<String> exposedSubsystems = MicrometerSubsystemRegistrar.EXPOSED_SUBSYSTEMS.unwrap(context, model);
        boolean exposeAnySubsystem = exposedSubsystems.remove("*");

        try {
            new JmxMicrometerCollector(wildFlyRegistry).init();
        } catch (IOException e) {
            throw MICROMETER_LOGGER.failedInitializeJMXRegistrar(e);
        }

        ServiceDependency<ModelControllerClientFactory> mccf = ServiceDependency.on(CLIENT_FACTORY_CAPABILITY, ModelControllerClientFactory.class);
        ServiceDependency<Executor> executor = ServiceDependency.on(MANAGEMENT_EXECUTOR, Executor.class);
        ServiceDependency<ProcessStateNotifier> processStateNotifier = ServiceDependency.on(PROCESS_STATE_NOTIFIER, ProcessStateNotifier.class);

        Supplier<MicrometerCollector> collectorSupplier = () ->
                new MicrometerCollector(mccf.get().createClient(executor.get()), processStateNotifier.get(), wildFlyRegistry);

        deploymentConfig.set(new MicrometerDeploymentConfiguration() {
            @Override
            public WildFlyCompositeRegistry getRegistry() {
                return wildFlyRegistry;
            }

            @Override
            public Predicate<String> getSubsystemFilter() {
                return subsystem -> exposeAnySubsystem || exposedSubsystems.contains(subsystem);
            }

            @Override
            public Supplier<MicrometerCollector> getCollectorSupplier() {
                return collectorSupplier;
            }
        });


        AtomicReference<MicrometerCollector> captor = new AtomicReference<>();

        context.addStep((operationContext, modelNode) -> {
            MicrometerCollector collector = captor.get();
            // Given that this step runs in the VERIFY stage, and our collector service was started eagerly, the
            // collector reference _should_ be non-null.
            if (collector != null) {
                ImmutableManagementResourceRegistration rootResourceRegistration = context.getRootResourceRegistration();
                Resource rootResource = context.readResourceFromRoot(EMPTY_ADDRESS);

                collector.collectResourceMetrics(rootResource, rootResourceRegistration,
                        Function.identity(), deploymentConfig.get().getSubsystemFilter());
            }
        }, OperationContext.Stage.VERIFY);

        return CapabilityServiceInstaller.builder(MICROMETER_COLLECTOR_RUNTIME_CAPABILITY, collectorSupplier)
                .requires(List.of(mccf, executor, processStateNotifier))
                .withCaptor(captor::set) // capture the provided value
                .asActive() // Start actively
                .build();
    }

    private static class TranslateOtlpHandler implements UnaryOperator<OperationStepHandler> {
        @Override
        public OperationStepHandler apply(OperationStepHandler handler) {
            return (context, operation) -> {
                ModelNode endpoint = operation.remove(ENDPOINT.getName());
                ModelNode step = operation.remove(STEP.getName());
                Map<String, ModelNode> parameters = new TreeMap<>();

                if (endpoint != null) {
                    parameters.put(OtlpRegistryDefinitionRegistrar.ENDPOINT.getName(), endpoint);
                }
                if (step != null) {
                    parameters.put(OtlpRegistryDefinitionRegistrar.STEP.getName(), step);
                }

                if (!parameters.isEmpty()) {
                    ModelNode otlpOperation = Util.createAddOperation(
                            context.getCurrentAddress().append(OtlpRegistryDefinitionRegistrar.PATH), parameters);
                    context.addStep(otlpOperation, context.getResourceRegistration().getOperationEntry(
                                    PathAddress.pathAddress(OtlpRegistryDefinitionRegistrar.PATH),
                                    ModelDescriptionConstants.ADD).getOperationHandler(),
                            OperationContext.Stage.MODEL, true);
                }
                handler.execute(context, operation);
            };
        }
    }

    public static interface MicrometerDeploymentConfiguration {
        WildFlyCompositeRegistry getRegistry();

        Predicate<String> getSubsystemFilter();

        Supplier<MicrometerCollector> getCollectorSupplier();
    }
}
