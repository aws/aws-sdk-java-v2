/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.codegen.smithy;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.codegen.internal.TypeUtils;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.Metadata;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.intermediate.Protocol;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.service.Paginators;
import software.amazon.awssdk.codegen.model.service.Waiters;
import software.amazon.awssdk.codegen.naming.DefaultSmithyNamingStrategy;
import software.amazon.awssdk.codegen.naming.NamingStrategy;
import software.amazon.awssdk.codegen.utils.ProtocolUtils;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.ServiceIndex;
import software.amazon.smithy.model.shapes.ServiceShape;

/**
 * Builds an intermediate model to be used by the templates from the service model and
 * customizations.
 */
public class SmithyIntermediateModelBuilder {
    private static final Logger log = LoggerFactory.getLogger(SmithyIntermediateModelBuilder.class);

    private final Model smithyModel;
    private final ServiceShape service;
    private final CustomizationConfig customizationConfig;
    private final NamingStrategy namingStrategy;
    private final TypeUtils typeUtils;
    private final ServiceIndex serviceIndex;

    public SmithyIntermediateModelBuilder(SmithyModelWithCustomizations modelWithCustomizations) {
        this.smithyModel = modelWithCustomizations.getSmithyModel();
        this.service = getServiceShape(this.smithyModel);
        this.customizationConfig = modelWithCustomizations.getCustomizationConfig();
        this.namingStrategy = new DefaultSmithyNamingStrategy(smithyModel, service, customizationConfig);
        this.typeUtils = new TypeUtils(namingStrategy);
        this.serviceIndex = ServiceIndex.of(smithyModel);
    }

    private ServiceShape getServiceShape(Model model) {
        if (model.getServiceShapes().size() != 1) {
            throw new IllegalArgumentException("Smithy model must have exactly one service shape");
        }
        return model.getServiceShapes().iterator().next();
    }


    public IntermediateModel build() {
        // TODO: create a DefaultSmithyCustomizationProcessor and preprocess the model here

        Paginators paginators = translatePaginators();
        Waiters waiters = translateWaiters();

        Map<String, ShapeModel> shapes = new HashMap<>();

        Map<String, OperationModel> operations = new TreeMap<>(new AddOperations(this, paginators).constructOperations());
        
        Metadata metadata = constructMetadata();
        
        // For now, return a basic IntermediateModel with operations and metadata
        // We'll add shapes and other features incrementally
        return new IntermediateModel(
            metadata,
            operations,
            shapes,
            customizationConfig,
            null, // endpointOperation - will be computed later
            paginators.getPagination(),
            namingStrategy,
            waiters.getWaiters(),
            null, // endpointRuleSetModel - will be added later
            null, // endpointTestSuiteModel - will be added later
            null  // clientContextParams - will be added later
        );
    }
    
    private Metadata constructMetadata() {
        Metadata metadata = new Metadata();
        
        String serviceName = namingStrategy.getServiceName();
        String protocol = ProtocolUtils.resolveProtocol(serviceIndex, service);
        
        // Configure package names
        configurePackageName(metadata, serviceName);
        
        // Set basic metadata
        metadata.withApiVersion(service.getVersion())
                .withAsyncClient(String.format("Default%sAsyncClient", serviceName))
                .withAsyncInterface(String.format("%sAsyncClient", serviceName))
                .withAsyncBuilder(String.format("Default%sAsyncClientBuilder", serviceName))
                .withAsyncBuilderInterface(String.format("%sAsyncClientBuilder", serviceName))
                .withBaseBuilderInterface(String.format("%sBaseClientBuilder", serviceName))
                .withBaseBuilder(String.format("Default%sBaseClientBuilder", serviceName))
                .withServiceName(serviceName)
                .withSyncClient(String.format("Default%sClient", serviceName))
                .withSyncInterface(String.format("%sClient", serviceName))
                .withSyncBuilder(String.format("Default%sClientBuilder", serviceName))
                .withSyncBuilderInterface(String.format("%sClientBuilder", serviceName))
                .withBaseExceptionName(String.format("%sException", serviceName))
                .withBaseRequestName(String.format("%sRequest", serviceName))
                .withBaseResponseName(String.format("%sResponse", serviceName))
                .withProtocol(Protocol.fromValue(protocol));
        
        // Extract service-specific traits
        service.getTrait(software.amazon.smithy.aws.traits.ServiceTrait.class).ifPresent(serviceTrait -> {
            metadata.withServiceId(serviceTrait.getSdkId())
                    .withServiceAbbreviation(serviceTrait.getSdkId())
                    .withServiceFullName(serviceTrait.getSdkId())
                    .withEndpointPrefix(serviceTrait.getArnNamespace());
        });
        
        service.getTrait(software.amazon.smithy.model.traits.DocumentationTrait.class).ifPresent(doc -> {
            metadata.withDocumentation(doc.getValue());
        });
        
        service.getTrait(software.amazon.smithy.model.traits.TitleTrait.class).ifPresent(title -> {
            // Only set if not already set by ServiceTrait
            if (metadata.getDescriptiveServiceName() == null) {
                metadata.withServiceFullName(title.getValue());
            }
        });
        
        service.getTrait(software.amazon.smithy.aws.traits.auth.SigV4Trait.class).ifPresent(sigv4 -> {
            metadata.withSigningName(sigv4.getName());
        });
        
        return metadata;
    }
    
    private void configurePackageName(Metadata metadata, String serviceName) {
        String rootPackage = customizationConfig.getRootPackageName();
        if (rootPackage == null) {
            rootPackage = "software.amazon.awssdk.services";
        }
        
        metadata.withRootPackageName(rootPackage)
                .withClientPackageName(namingStrategy.getClientPackageName(serviceName))
                .withModelPackageName(namingStrategy.getModelPackageName(serviceName))
                .withTransformPackageName(namingStrategy.getTransformPackageName(serviceName))
                .withRequestTransformPackageName(namingStrategy.getRequestTransformPackageName(serviceName))
                .withPaginatorsPackageName(namingStrategy.getPaginatorsPackageName(serviceName))
                .withWaitersPackageName(namingStrategy.getWaitersPackageName(serviceName))
                .withEndpointRulesPackageName(namingStrategy.getEndpointRulesPackageName(serviceName))
                .withAuthSchemePackageName(namingStrategy.getAuthSchemePackageName(serviceName))
                .withJmesPathPackageName(namingStrategy.getJmesPathPackageName(serviceName));
    }

    public Model getSmithyModel() {
        return smithyModel;
    }

    public ServiceShape getService() {
        return this.service;
    }

    public ServiceIndex getServiceIndex() {
        return this.serviceIndex;
    }

    public CustomizationConfig getCustomizationConfig() {
        return customizationConfig;
    }

    public NamingStrategy getNamingStrategy() {
        return namingStrategy;
    }

    public TypeUtils getTypeUtils() {
        return typeUtils;
    }

    private static Waiters translateWaiters() {
        // TODO - transform these!
        return Waiters.none();
    }

    private static Paginators translatePaginators() {
        // TODO - transform these!'
        return Paginators.none();
    }
}
