/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import software.amazon.awssdk.codegen.model.intermediate.AuthorizerModel;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.intermediate.Protocol;
import software.amazon.awssdk.codegen.model.intermediate.ServiceExamples;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.service.AuthType;
import software.amazon.awssdk.codegen.model.service.Operation;
import software.amazon.awssdk.codegen.model.service.Paginators;
import software.amazon.awssdk.codegen.model.service.ServiceModel;
import software.amazon.awssdk.codegen.naming.DefaultNamingStrategy;
import software.amazon.awssdk.codegen.naming.NamingStrategy;

/**
 * Builds an intermediate model to be used by the templates from the service model and
 * customizations.
 */
public class IntermediateModelBuilder {

    private static final Logger log = LoggerFactory.getLogger(IntermediateModelBuilder.class);
    private final CustomizationConfig customConfig;
    private final ServiceModel service;
    private final ServiceExamples examples;
    private final NamingStrategy namingStrategy;
    private final TypeUtils typeUtils;
    private final List<IntermediateModelShapeProcessor> shapeProcessors;
    private final Paginators paginators;

    public IntermediateModelBuilder(C2jModels models) {
        this.customConfig = models.customizationConfig();
        this.service = models.serviceModel();
        this.examples = models.examplesModel();
        this.namingStrategy = new DefaultNamingStrategy(service, customConfig);
        this.typeUtils = new TypeUtils(namingStrategy);
        this.shapeProcessors = createShapeProcessors();
        this.paginators = models.paginatorsModel();
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
        // Note: This needs to come before any pre/post processing of the
        // models, as the transformer must have access to the original shapes,
        // before any customizations have been applied (which modifies them).
        log.info("Applying customizations to examples...");
        new ExamplesCustomizer(service, customConfig).applyCustomizationsToExamples(examples);
        log.info("Examples customized.");

        CodegenCustomizationProcessor customization = DefaultCustomizationProcessor
            .getProcessorFor(customConfig);

        customization.preprocess(service);

        Map<String, OperationModel> operations = new TreeMap<>();
        Map<String, ShapeModel> shapes = new HashMap<>();
        Map<String, AuthorizerModel> authorizers = new HashMap<>();

        operations.putAll(new AddOperations(this).constructOperations());
        authorizers.putAll(new AddCustomAuthorizers(this.service, getNamingStrategy()).constructAuthorizers());

        OperationModel endpointOperation = null;

        for (OperationModel o : operations.values()) {
            if (o.isEndpointOperation()) {
                endpointOperation = o;
                break;
            }
        }

        for (IntermediateModelShapeProcessor processor : shapeProcessors) {
            shapes.putAll(processor.process(Collections.unmodifiableMap(operations),
                                            Collections.unmodifiableMap(shapes)));
        }

        // Remove deprecated operations and their paginators
        operations.entrySet().removeIf(e -> customConfig.getDeprecatedOperations().contains(e.getKey()));
        paginators.getPaginators().entrySet().removeIf(e -> customConfig.getDeprecatedOperations().contains(e.getKey()));

        log.info("{} shapes found in total.", shapes.size());

        IntermediateModel fullModel = new IntermediateModel(
            constructMetadata(service, customConfig), operations, shapes,
            customConfig, examples, endpointOperation, authorizers, paginators.getPaginators(), namingStrategy);

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
                                                               fullModel.getExamples(),
                                                               endpointOperation,
                                                               fullModel.getCustomAuthorizers(),
                                                               fullModel.getPaginators(),
                                                               namingStrategy);

        linkMembersToShapes(trimmedModel);
        linkOperationsToInputOutputShapes(trimmedModel);
        linkCustomAuthorizationToRequestShapes(trimmedModel);

        setSimpleMethods(trimmedModel);

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
                    member.setShape(
                        Utils.findShapeModelByC2jNameIfExists(model, member.getC2jShape()));
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
                entry.getValue().setOutputShape(model.getShapeByC2jName(outputShapeName));
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

                 if (model.getMetadata().getProtocol() == Protocol.API_GATEWAY) {
                     linkAuthorizationToRequestShapeForApiGatewayProtocol(model, c2jOperation, shape);
                 } else {
                     linkAuthorizationToRequestShapeForAwsProtocol(c2jOperation.getAuthType(), shape);
                 }
             });
    }

    private void linkAuthorizationToRequestShapeForApiGatewayProtocol(IntermediateModel model,
                                                                      Operation c2jOperation,
                                                                      ShapeModel shape) {
        if (AuthType.CUSTOM.equals(c2jOperation.getAuthType())) {
            AuthorizerModel auth = model.getCustomAuthorizers().get(c2jOperation.getAuthorizer());
            if (auth == null) {
                throw new RuntimeException(String.format("Required custom auth not defined: %s",
                                                         c2jOperation.getAuthorizer()));
            }
            shape.setRequestSignerClassFqcn(model.getMetadata().getAuthPolicyPackageName() + '.' +
                                            auth.getInterfaceName());
        } else if (AuthType.IAM.equals(c2jOperation.getAuthType())) {
            model.getMetadata().setRequiresIamSigners(true);
            // TODO IamRequestSigner does not exist
            shape.setRequestSignerClassFqcn("software.amazon.awssdk.opensdk.protect.auth.IamRequestSigner");
        }
    }

    private void linkAuthorizationToRequestShapeForAwsProtocol(AuthType authType, ShapeModel shape) {
        switch (authType) {
            case V4:
                shape.setRequestSignerClassFqcn("software.amazon.awssdk.auth.signer.Aws4Signer");
                break;
            case V4_UNSIGNED_BODY:
                shape.setRequestSignerClassFqcn("software.amazon.awssdk.auth.signer.Aws4UnsignedPayloadSigner");
                break;
            case NONE:
            case IAM:
                // just ignore this, this is the default value but only applicable to APIG generated clients
                break;
            default:
                throw new IllegalArgumentException("Unsupported authtype for AWS Request: " + authType);
        }
    }

    private void setSimpleMethods(IntermediateModel model) {
        model.getOperations().entrySet().forEach(m -> {

            ShapeModel inputShape = m.getValue().getInputShape();
            String methodName = m.getValue().getMethodName();
            CustomizationConfig config = model.getCustomizationConfig();

            if (inputShape.getRequired() == null
                && !config.getBlacklistedSimpleMethods().contains(methodName)
                && !(config.getBlacklistedSimpleMethods().size() == 1 && config.getBlacklistedSimpleMethods().get(0).equals("*"))
                && !m.getValue().hasStreamingInput()
                && !m.getValue().hasStreamingOutput()) {

                if (!methodName.matches(Constant.APPROVED_SIMPLE_METHOD_VERBS) &&
                    !config.getVerifiedSimpleMethods().contains(methodName)) {
                    // TODO: How do we prevent these from being missed before services launch?
                    log.warn("Simple method encountered that is not approved or blacklisted: " + methodName);
                } else {
                    inputShape.setSimpleMethod(true);
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

    public ServiceExamples getExamples() {
        return examples;
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
