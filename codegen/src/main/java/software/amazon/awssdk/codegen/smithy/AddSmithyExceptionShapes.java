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
import software.amazon.awssdk.codegen.IntermediateModelShapeProcessor;
import software.amazon.awssdk.codegen.internal.TypeUtils;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeType;
import software.amazon.awssdk.codegen.naming.NamingStrategy;
import software.amazon.smithy.aws.traits.protocols.AwsQueryErrorTrait;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.HttpBindingIndex;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.HttpErrorTrait;

/**
 * Builds an exception {@link ShapeModel} for every error shape referenced by
 * an operation reachable from the service. An error referenced by more than
 * one operation is translated once.
 *
 * <p>Each exception shape records its wire error code and, when present, the
 * HTTP status code from the {@code @httpError} trait.
 */
final class AddSmithyExceptionShapes extends AddSmithyShapes implements IntermediateModelShapeProcessor {

    AddSmithyExceptionShapes(Model model,
                             ServiceShape service,
                             NamingStrategy namingStrategy,
                             CustomizationConfig customConfig,
                             String protocol,
                             TypeUtils typeUtils) {
        super(model, service, namingStrategy, customConfig, protocol, typeUtils);
    }

    @Override
    public Map<String, ShapeModel> process(Map<String, OperationModel> currentOperations,
                                           Map<String, ShapeModel> currentShapes) {
        Map<String, ShapeModel> shapes = new HashMap<>();
        Model model = getModel();
        NamingStrategy naming = getNamingStrategy();
        HttpBindingIndex bindingIndex = HttpBindingIndex.of(model);
        TopDownIndex topDown = TopDownIndex.of(model);

        for (OperationShape op : topDown.getContainedOperations(getService())) {
            for (ShapeId errorId : op.getErrors()) {
                String javaClassName = naming.getExceptionName(errorId.getName());
                if (shapes.containsKey(javaClassName) || currentShapes.containsKey(javaClassName)) {
                    continue;
                }

                StructureShape errorShape = model.expectShape(errorId, StructureShape.class);
                ShapeModel shapeModel = generateShapeModel(javaClassName, errorShape,
                                                           bindingIndex.getResponseBindings(errorId));
                shapeModel.setType(ShapeType.Exception.getValue());
                shapeModel.setErrorCode(resolveErrorCode(errorShape));
                errorShape.getTrait(HttpErrorTrait.class)
                          .ifPresent(t -> shapeModel.setHttpStatusCode(t.getCode()));

                shapes.put(javaClassName, shapeModel);
            }
        }

        return shapes;
    }

    /**
     * Resolves the wire error code for an exception. Protocols that allow the
     * code to differ from the shape name honor {@link AwsQueryErrorTrait} when
     * it is present; every other case uses the error shape's own name.
     */
    private String resolveErrorCode(StructureShape errorShape) {
        if (protocolSupportsErrorCodeOverride()) {
            String override = errorShape.getTrait(AwsQueryErrorTrait.class)
                                        .map(AwsQueryErrorTrait::getCode)
                                        .orElse(null);
            if (override != null && !override.isEmpty()) {
                return override;
            }
        }
        return errorShape.getId().getName();
    }

    private boolean protocolSupportsErrorCodeOverride() {
        String protocol = getProtocol();
        // The awsJson family and smithy-rpc-v2-cbor use the shape name as the code.
        // Every other protocol allows AwsQueryErrorTrait to override it.
        return !"json".equals(protocol) && !"smithy-rpc-v2-cbor".equals(protocol);
    }
}
