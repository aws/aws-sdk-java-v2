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

package software.amazon.awssdk.codegen;

import static software.amazon.awssdk.codegen.AddMetadata.constructMetadata;
import static software.amazon.awssdk.codegen.RemoveUnusedShapes.removeUnusedShapes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.codegen.customization.CodegenCustomizationProcessor;
import software.amazon.awssdk.codegen.customization.processors.DefaultCustomizationProcessor;
import software.amazon.awssdk.codegen.internal.Constant;
import software.amazon.awssdk.codegen.internal.TypeUtils;
import software.amazon.awssdk.codegen.internal.Utils;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.service.AuthType;
import software.amazon.awssdk.codegen.model.service.Operation;
import software.amazon.awssdk.codegen.model.service.Paginators;
import software.amazon.awssdk.codegen.model.service.ServiceModel;
import software.amazon.awssdk.codegen.model.service.Waiters;
import software.amazon.awssdk.codegen.naming.DefaultNamingStrategy;
import software.amazon.awssdk.codegen.naming.NamingStrategy;
import software.amazon.awssdk.utils.CollectionUtils;

/**
 * Builds an intermediate model to be used by the templates from the service model and
 * customizations.
 */
public class IntermediateModelBuilder {

    private static final Logger log = LoggerFactory.getLogger(IntermediateModelBuilder.class);
    private final CustomizationConfig customConfig;
    private final ServiceModel service;
    private final NamingStrategy namingStrategy;
    private final TypeUtils typeUtils;
    private final List<IntermediateModelShapeProcessor> shapeProcessors;
    private final Paginators paginators;
    private final Waiters waiters;

    public IntermediateModelBuilder(C2jModels models) {
        this.customConfig = models.customizationConfig();
        this.service = models.serviceModel();
        this.namingStrategy = new DefaultNamingStrategy(service, customConfig);
        this.typeUtils = new TypeUtils(namingStrategy);
        this.shapeProcessors = createShapeProcessors();
        this.paginators = models.paginatorsModel();
        this.waiters = models.waitersModel();
    }


    /**
     * Create default shape processors.
     */
    private List<IntermediateModelShapeProcessor> createShapeProcessors() {
        List<IntermediateModelShapeProcessor> processors = new ArrayList<>();
        processors.add(new AddInputShapes(this));
        processors.add(new AddOutputShapes(this));
        processors.add(new AddExceptionShapes(this));
        processors.add(new AddModelShapes(this));
        processors.add(new AddEmptyInputShape(this));
        processors.add(new AddEmptyOutputShape(this));
        return processors;
    }

    public IntermediateModel build() {
        CodegenCustomizationProcessor customization = DefaultCustomizationProcessor
            .getProcessorFor(customConfig);

        customization.preprocess(service);

        Map<String, ShapeModel> shapes = new HashMap<>();

        Map<String, OperationModel> operations = new TreeMap<>(new AddOperations(this).constructOperations());

        // Iterate through every operation and build an 'endpointOperation' if at least one operation that supports
        // endpoint discovery is found. If -any operations that require- endpoint discovery are found, then the flag
        // 'endpointCacheRequired' will be set on the 'endpointOperation'. This 'endpointOperation' summary is then
        // passed directly into the constructor of the intermediate model and is referred to by the codegen.
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

        for (IntermediateModelShapeProcessor processor : shapeProcessors) {
            shapes.putAll(processor.process(Collections.unmodifiableMap(operations),
                                            Collections.unmodifiableMap(shapes)));
        }

        // Remove deprecated operations and their paginators
        operations.entrySet().removeIf(e -> customConfig.getDeprecatedOperations().contains(e.getKey()));
        paginators.getPagination().entrySet().removeIf(e -> customConfig.getDeprecatedOperations().contains(e.getKey()));

        log.info("{} shapes found in total.", shapes.size());

        IntermediateModel fullModel = new IntermediateModel(
            constructMetadata(service, customConfig), operations, shapes,
            customConfig, endpointOperation, paginators.getPagination(), namingStrategy,
            waiters.getWaiters());

        customization.postprocess(fullModel);

        log.info("{} shapes remained after applying customizations.", fullModel.getShapes().size());

        Map<String, ShapeModel> trimmedShapes = removeUnusedShapes(fullModel);
        // Remove deprecated shapes
        trimmedShapes.entrySet().removeIf(e -> customConfig.getDeprecatedShapes().contains(e.getKey()));

        log.info("{} shapes remained after removing unused shapes.", trimmedShapes.size());

        IntermediateModel trimmedModel = new IntermediateModel(fullModel.getMetadata(),
                                                               fullModel.getOperations(),
                                                               trimmedShapes,
                                                               fullModel.getCustomizationConfig(),
                                                               endpointOperation,
                                                               fullModel.getPaginators(),
                                                               namingStrategy,
                                                               fullModel.getWaiters());

        linkMembersToShapes(trimmedModel);
        linkOperationsToInputOutputShapes(trimmedModel);
        linkCustomAuthorizationToRequestShapes(trimmedModel);

        setSimpleMethods(trimmedModel);

        namingStrategy.validateCustomerVisibleNaming(trimmedModel);

        return trimmedModel;
    }

    /**
     * Link the member to it's corresponding shape (if it exists).
     *
     * @param model Final IntermediateModel
     */
    private void linkMembersToShapes(IntermediateModel model) {
        for (Map.Entry<String, ShapeModel> entry : model.getShapes().entrySet()) {
            if (entry.getValue().getMembers() != null) {
                for (MemberModel member : entry.getValue().getMembers()) {
                    member.setShape(Utils.findMemberShapeModelByC2jNameIfExists(model, member.getC2jShape()));
                }
            }
        }
    }

    private void linkOperationsToInputOutputShapes(IntermediateModel model) {
        for (Map.Entry<String, OperationModel> entry : model.getOperations().entrySet()) {

            Operation operation = service.getOperations().get(entry.getKey());

            if (entry.getValue().getInput() != null) {
                entry.getValue().setInputShape(model.getShapes().get(entry.getValue().getInput().getSimpleType()));
            }

            if (operation.getOutput() != null) {
                String outputShapeName = operation.getOutput().getShape();
                ShapeModel outputShape =
                    model.getShapeByNameAndC2jName(entry.getValue().getReturnType().getReturnType(), outputShapeName);
                entry.getValue().setOutputShape(outputShape);
            }
        }
    }

    private void linkCustomAuthorizationToRequestShapes(IntermediateModel model) {
        model.getOperations().values().stream()
             .filter(OperationModel::isAuthenticated)
             .forEach(operation -> {
                 Operation c2jOperation = service.getOperation(operation.getOperationName());

                 ShapeModel shape = operation.getInputShape();
                 if (shape == null) {
                     throw new RuntimeException(String.format("Operation %s has unknown input shape",
                                                              operation.getOperationName()));
                 }

                 linkAuthorizationToRequestShapeForAwsProtocol(c2jOperation.getAuthtype(), shape);
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

                boolean methodIsNotBlacklisted = !config.getBlacklistedSimpleMethods().contains(methodName) ||
                                                 config.getBlacklistedSimpleMethods().stream().noneMatch(m -> m.equals("*"));
                boolean methodHasNoRequiredMembers = !CollectionUtils.isNullOrEmpty(inputShape.getRequired());
                boolean methodIsNotStreaming = !operation.isStreaming();
                boolean methodHasSimpleMethodVerb = methodName.matches(Constant.APPROVED_SIMPLE_METHOD_VERBS);

                if (methodIsNotBlacklisted && methodHasNoRequiredMembers && methodIsNotStreaming && methodHasSimpleMethodVerb) {
                    log.warn("A potential simple method exists that isn't whitelisted or blacklisted: " + methodName);
                }
            }
        });
    }

    public CustomizationConfig getCustomConfig() {
        return customConfig;
    }

    public ServiceModel getService() {
        return service;
    }

    public NamingStrategy getNamingStrategy() {
        return namingStrategy;
    }

    public TypeUtils getTypeUtils() {
        return typeUtils;
    }

    public Paginators getPaginators() {
        return paginators;
    }
}
