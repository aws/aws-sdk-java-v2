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

import static software.amazon.awssdk.codegen.RemoveUnusedShapes.removeUnusedShapes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.codegen.IntermediateModelShapeProcessor;
import software.amazon.awssdk.codegen.internal.Constant;
import software.amazon.awssdk.codegen.internal.TypeUtils;
import software.amazon.awssdk.codegen.internal.Utils;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.rules.endpoints.EndpointTestSuiteModel;
import software.amazon.awssdk.codegen.model.service.AuthType;
import software.amazon.awssdk.codegen.model.service.ClientContextParam;
import software.amazon.awssdk.codegen.model.service.EndpointRuleSetModel;
import software.amazon.awssdk.codegen.naming.DefaultSmithyNamingStrategy;
import software.amazon.awssdk.codegen.naming.NamingStrategy;
import software.amazon.awssdk.codegen.utils.ProtocolUtils;
import software.amazon.awssdk.codegen.validation.MemberShapeTargetValidator;
import software.amazon.awssdk.utils.CollectionUtils;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.ServiceIndex;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.rulesengine.traits.ClientContextParamsTrait;

/**
 * Builds an {@link IntermediateModel} from a Smithy {@link Model}. Smithy counterpart to
 * {@link software.amazon.awssdk.codegen.IntermediateModelBuilder}.
 *
 * <p>Customizations, paginators, and waiters are not applied yet, so this is limited to services
 * that need none of them.
 */
public final class SmithyIntermediateModelBuilder {

    private static final Logger log = LoggerFactory.getLogger(SmithyIntermediateModelBuilder.class);
    private static final ShapeId UNIT = ShapeId.from("smithy.api#Unit");

    private final Model model;
    private final ServiceShape service;
    private final CustomizationConfig customConfig;
    private final NamingStrategy namingStrategy;
    private final TypeUtils typeUtils;
    private final ServiceIndex serviceIndex;
    private final String protocol;
    private final EndpointRuleSetModel endpointRuleSet;
    private final EndpointTestSuiteModel endpointTestSuiteModel;
    private final List<IntermediateModelShapeProcessor> shapeProcessors;

    public SmithyIntermediateModelBuilder(SmithyModels models) {
        this.model = models.model();
        this.service = resolveService(model);
        this.customConfig = models.customizationConfig();
        this.namingStrategy = new DefaultSmithyNamingStrategy(model, service, customConfig);
        this.typeUtils = new TypeUtils(namingStrategy);
        this.serviceIndex = ServiceIndex.of(model);
        this.protocol = ProtocolUtils.resolveProtocol(serviceIndex, service);
        this.endpointRuleSet = AddSmithyEndpoints.endpointRuleSet(service);
        this.endpointTestSuiteModel = AddSmithyEndpoints.endpointTests(service);
        this.shapeProcessors = createShapeProcessors();
    }

    private static ServiceShape resolveService(Model model) {
        Set<ServiceShape> services = model.getServiceShapes();
        if (services.size() != 1) {
            throw new IllegalArgumentException(
                "Expected exactly one service in the Smithy model but found " + services.size());
        }
        return services.iterator().next();
    }

    /**
     * Four processors where C2J has six: the input and output processors synthesize the empty
     * request and response shapes themselves instead of leaving them to separate passes.
     */
    private List<IntermediateModelShapeProcessor> createShapeProcessors() {
        List<IntermediateModelShapeProcessor> processors = new ArrayList<>();
        processors.add(new AddSmithyInputShapes(model, service, namingStrategy, customConfig, protocol, typeUtils));
        processors.add(new AddSmithyOutputShapes(model, service, namingStrategy, customConfig, protocol, typeUtils));
        processors.add(new AddSmithyExceptionShapes(model, service, namingStrategy, customConfig, protocol, typeUtils));
        processors.add(new AddSmithyModelShapes(model, service, namingStrategy, customConfig, protocol, typeUtils));
        return processors;
    }

    public IntermediateModel build() {
        Map<String, OperationModel> operations =
            new TreeMap<>(new AddSmithyOperations(model, service, namingStrategy, customConfig, protocol)
                              .constructOperations());

        OperationModel endpointOperation = null;
        boolean endpointCacheRequired = false;
        for (OperationModel o : operations.values()) {
            if (o.isEndpointOperation()) {
                endpointOperation = o;
            }
            if (o.getEndpointDiscovery() != null && o.getEndpointDiscovery().isRequired()) {
                endpointCacheRequired = true;
            }
        }
        if (endpointOperation != null) {
            endpointOperation.setEndpointCacheRequired(endpointCacheRequired);
        }

        Map<String, ShapeModel> shapes = new HashMap<>();
        for (IntermediateModelShapeProcessor processor : shapeProcessors) {
            shapes.putAll(processor.process(Collections.unmodifiableMap(operations),
                                            Collections.unmodifiableMap(shapes)));
        }

        operations.entrySet().removeIf(e -> customConfig.getDeprecatedOperations().contains(e.getKey()));

        log.info("{} shapes found in total.", shapes.size());

        Map<String, ClientContextParam> clientContextParams = clientContextParams();

        IntermediateModel fullModel = new IntermediateModel(
            AddSmithyMetadata.constructMetadata(model, service, serviceIndex, namingStrategy, customConfig),
            operations, shapes, customConfig, endpointOperation,
            Collections.emptyMap(), namingStrategy, Collections.emptyMap(),
            endpointRuleSet, endpointTestSuiteModel, clientContextParams);

        Map<String, ShapeModel> trimmedShapes = removeUnusedShapes(fullModel);
        trimmedShapes.entrySet().removeIf(e -> customConfig.getDeprecatedShapes().contains(e.getKey()));

        log.info("{} shapes remained after removing unused shapes.", trimmedShapes.size());

        IntermediateModel trimmedModel = new IntermediateModel(fullModel.getMetadata(),
                                                               fullModel.getOperations(),
                                                               trimmedShapes,
                                                               customConfig,
                                                               endpointOperation,
                                                               fullModel.getPaginators(),
                                                               namingStrategy,
                                                               fullModel.getWaiters(),
                                                               endpointRuleSet,
                                                               endpointTestSuiteModel,
                                                               clientContextParams);

        linkMembersToShapes(trimmedModel);
        MemberShapeTargetValidator.validate(trimmedModel);
        linkOperationsToInputOutputShapes(trimmedModel);
        linkCustomAuthorizationToRequestShapes(trimmedModel);
        setSimpleMethods(trimmedModel);
        namingStrategy.validateCustomerVisibleNaming(trimmedModel);
        return trimmedModel;
    }

    /**
     * Null rather than an empty map when the trait is absent, matching C2J, whose field stays null
     * when the key is missing. Sorted by name so the generated builder settings keep a stable
     * order; the trait's own key order is arbitrary.
     */
    private Map<String, ClientContextParam> clientContextParams() {
        if (!service.hasTrait(ClientContextParamsTrait.class)) {
            return null;
        }

        Map<String, ClientContextParam> params = new TreeMap<>();
        service.expectTrait(ClientContextParamsTrait.class).getParameters().forEach((name, definition) -> {
            ClientContextParam param = new ClientContextParam();
            param.setType(definition.getType().toString());
            definition.getDocumentation().ifPresent(param::setDocumentation);
            params.put(name, param);
        });
        return params;
    }

    private void linkMembersToShapes(IntermediateModel model) {
        for (Map.Entry<String, ShapeModel> entry : model.getShapes().entrySet()) {
            if (entry.getValue().getMembers() != null) {
                for (MemberModel member : entry.getValue().getMembers()) {
                    member.setShape(Utils.findMemberShapeModelByC2jNameIfExists(model, member.getC2jShape()));
                }
            }
        }
    }

    /**
     * Output linking is skipped for {@code smithy.api#Unit}, matching how C2J treats an operation
     * that declares no output shape.
     */
    private void linkOperationsToInputOutputShapes(IntermediateModel model) {
        Map<String, OperationShape> smithyOps = new HashMap<>();
        for (OperationShape op : TopDownIndex.of(this.model).getContainedOperations(service)) {
            smithyOps.put(op.getId().getName(), op);
        }

        for (Map.Entry<String, OperationModel> entry : model.getOperations().entrySet()) {
            OperationModel operationModel = entry.getValue();
            OperationShape op = smithyOps.get(entry.getKey());

            if (operationModel.getInput() != null) {
                operationModel.setInputShape(model.getShapes().get(operationModel.getInput().getSimpleType()));
            }

            if (op != null && !UNIT.equals(op.getOutputShape())) {
                String outputShapeName = op.getOutputShape().getName();
                ShapeModel outputShape =
                    model.getShapeByNameAndC2jName(operationModel.getReturnType().getReturnType(), outputShapeName);
                operationModel.setOutputShape(outputShape);
            }
        }
    }

    private void linkCustomAuthorizationToRequestShapes(IntermediateModel model) {
        model.getOperations().values().stream()
             .filter(OperationModel::isAuthenticated)
             .forEach(operation -> {
                 ShapeModel shape = operation.getInputShape();
                 if (shape == null) {
                     throw new RuntimeException(String.format("Operation %s has unknown input shape",
                                                              operation.getOperationName()));
                 }
                 linkAuthorizationToRequestShapeForAwsProtocol(operation.getAuthType(), shape);
             });
    }

    private void linkAuthorizationToRequestShapeForAwsProtocol(AuthType authType, ShapeModel shape) {
        if (authType == null) {
            return;
        }

        switch (authType) {
            case V4:
                shape.setRequestSignerClassFqcn("software.amazon.awssdk.auth.signer.Aws4Signer");
                break;
            case V4_UNSIGNED_BODY:
                shape.setRequestSignerClassFqcn("software.amazon.awssdk.auth.signer.Aws4UnsignedPayloadSigner");
                break;
            case BEARER:
                shape.setRequestSignerClassFqcn("software.amazon.awssdk.auth.token.signer.aws.BearerTokenSigner");
                break;
            case NONE:
                break;
            default:
                throw new IllegalArgumentException("Unsupported authtype for AWS Request: " + authType);
        }
    }

    private void setSimpleMethods(IntermediateModel model) {
        CustomizationConfig config = model.getCustomizationConfig();
        model.getOperations().values().forEach(operation -> {
            ShapeModel inputShape = operation.getInputShape();
            String methodName = operation.getMethodName();

            if (config.getVerifiedSimpleMethods().contains(methodName)) {
                inputShape.setSimpleMethod(true);
            } else {
                inputShape.setSimpleMethod(false);

                boolean methodIsNotExcluded = !config.getExcludedSimpleMethods().contains(methodName) ||
                                              config.getExcludedSimpleMethods().stream().noneMatch(m -> m.equals("*")) ||
                                              !config.getBlacklistedSimpleMethods().contains(methodName) ||
                                              config.getBlacklistedSimpleMethods().stream().noneMatch(m -> m.equals("*"));
                boolean methodHasNoRequiredMembers = CollectionUtils.isNullOrEmpty(inputShape.getRequired());
                boolean methodIsNotStreaming = !operation.isStreaming();
                boolean methodHasSimpleMethodVerb = methodName.matches(Constant.APPROVED_SIMPLE_METHOD_VERBS);

                if (methodIsNotExcluded && methodHasNoRequiredMembers && methodIsNotStreaming && methodHasSimpleMethodVerb) {
                    log.warn("A potential simple method exists that isn't explicitly excluded or included: " + methodName);
                }
            }
        });
    }
}
