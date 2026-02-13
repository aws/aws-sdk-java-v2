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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.codegen.internal.Jackson;
import software.amazon.awssdk.codegen.internal.TypeUtils;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.Metadata;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.intermediate.Protocol;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.rules.endpoints.EndpointTestSuiteModel;
import software.amazon.awssdk.codegen.model.service.AuthType;
import software.amazon.awssdk.codegen.model.service.EndpointRuleSetModel;
import software.amazon.awssdk.codegen.model.service.Paginators;
import software.amazon.awssdk.codegen.model.service.Waiters;
import software.amazon.awssdk.codegen.naming.DefaultSmithyNamingStrategy;
import software.amazon.awssdk.codegen.naming.NamingStrategy;
import software.amazon.awssdk.codegen.utils.ProtocolUtils;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.ServiceIndex;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.rulesengine.traits.EndpointRuleSetTrait;
import software.amazon.smithy.rulesengine.traits.EndpointTestsTrait;

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

        Map<String, OperationModel> operations = new TreeMap<>(new AddOperations(this, paginators).constructOperations());
        
        // Add shapes
        Map<String, ShapeModel> shapes = new AddSmithyShapes(this).constructShapes();
        
        Metadata metadata = constructMetadata();
        
        EndpointRuleSetModel endpointRuleSetModel = translateEndpointRuleSet();
        EndpointTestSuiteModel endpointTestSuiteModel = translateEndpointTestSuite();

        return new IntermediateModel(
            metadata,
            operations,
            shapes,
            customizationConfig,
            null, // endpointOperation - will be computed later
            paginators.getPagination(),
            namingStrategy,
            waiters.getWaiters(),
            endpointRuleSetModel,
            endpointTestSuiteModel,
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
                .withProtocol(Protocol.fromValue(protocol))
                .withBatchmanagerPackageName(namingStrategy.getBatchManagerPackageName(serviceName));

        // Extract service-specific traits
        service.getTrait(software.amazon.smithy.aws.traits.ServiceTrait.class).ifPresent(serviceTrait -> {
            String sdkId = serviceTrait.getSdkId();
            metadata.withServiceId(sdkId)
                    .withEndpointPrefix(serviceTrait.getArnNamespace());

            // Derive uid from sdkId and version: "Account" + "2021-02-01" -> "account-2021-02-01"
            String uid = sdkId.toLowerCase().replace(' ', '-') + "-" + service.getVersion();
            metadata.withUid(uid);
        });

        // Set service full name and abbreviation from @title trait (matches C2J serviceFullName)
        service.getTrait(software.amazon.smithy.model.traits.TitleTrait.class).ifPresent(title -> {
            metadata.withServiceFullName(title.getValue());
            metadata.withServiceAbbreviation(title.getValue());
        });

        service.getTrait(software.amazon.smithy.model.traits.DocumentationTrait.class).ifPresent(doc -> {
            metadata.withDocumentation(doc.getValue());
        });

        service.getTrait(software.amazon.smithy.aws.traits.auth.SigV4Trait.class).ifPresent(sigv4 -> {
            metadata.withSigningName(sigv4.getName());
        });

        // Set authType and auth list from service auth traits
        Map<ShapeId, Trait> serviceAuthSchemes = serviceIndex.getEffectiveAuthSchemes(service);
        if (!serviceAuthSchemes.isEmpty()) {
            // authType is derived from the first/primary auth scheme (typically sigv4 -> "v4")
            ShapeId primaryAuthScheme = serviceAuthSchemes.keySet().iterator().next();
            metadata.withAuthType(AuthType.fromValue(primaryAuthScheme.toString()));

            // auth list contains all auth scheme IDs
            List<AuthType> authList = new ArrayList<>();
            for (ShapeId authSchemeId : serviceAuthSchemes.keySet()) {
                authList.add(AuthType.fromValue(authSchemeId.toString()));
            }
            metadata.withAuth(authList);
        }

        // Set jsonVersion for JSON protocols (default to "1.1" like C2J does)
        Protocol resolvedProtocol = Protocol.fromValue(protocol);
        if (resolvedProtocol == Protocol.AWS_JSON || resolvedProtocol == Protocol.REST_JSON) {
            metadata.withJsonVersion("1.1");
        }

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

    /**
     * Extracts the endpoint rule set from the service's {@code @smithy.rules#endpointRuleSet} trait.
     * The trait's Node is serialized to JSON and deserialized into the codegen EndpointRuleSetModel.
     * Returns null if the trait is not present.
     */
    private EndpointRuleSetModel translateEndpointRuleSet() {
        return service.getTrait(EndpointRuleSetTrait.class)
                      .map(trait -> {
                          try {
                              String json = Node.printJson(trait.getRuleSet());
                              return Jackson.load(EndpointRuleSetModel.class, json);
                          } catch (IOException e) {
                              throw new RuntimeException("Failed to deserialize endpoint rule set from Smithy model", e);
                          }
                      })
                      .orElse(null);
    }

    /**
     * Extracts the endpoint test suite from the service's {@code @smithy.rules#endpointTests} trait.
     * The trait's Node is serialized to JSON and deserialized into the codegen EndpointTestSuiteModel.
     * Returns null if the trait is not present.
     */
    private EndpointTestSuiteModel translateEndpointTestSuite() {
        return service.getTrait(EndpointTestsTrait.class)
                      .map(trait -> {
                          try {
                              String json = Node.printJson(trait.toNode());
                              return Jackson.load(EndpointTestSuiteModel.class, json);
                          } catch (IOException e) {
                              throw new RuntimeException("Failed to deserialize endpoint test suite from Smithy model", e);
                          }
                      })
                      .orElse(null);
    }
}
