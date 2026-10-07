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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
import software.amazon.smithy.model.knowledge.OperationIndex;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.ErrorTrait;
import software.amazon.smithy.model.traits.HttpErrorTrait;
import software.amazon.smithy.model.traits.StreamingTrait;

/**
 * Builds an exception {@link ShapeModel} for every error shape referenced by a reachable
 * operation or by an event stream in an operation's input or output. An error shared by several
 * operations is translated once.
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
        OperationIndex operationIndex = OperationIndex.of(model);

        for (OperationShape op : topDown.getContainedOperations(getService())) {
            // Merged (service + operation) errors, matching the source AddSmithyOperations uses for
            // OperationModel.exceptions. Reading op.getErrors() would skip service-level errors and
            // leave the operation referencing an exception class with no shape.
            for (StructureShape errorShape : operationIndex.getErrors(getService(), op)) {
                ShapeId errorId = errorShape.getId();
                String javaClassName = naming.getExceptionName(errorId.getName());
                if (shapes.containsKey(javaClassName) || currentShapes.containsKey(javaClassName)) {
                    continue;
                }

                shapes.put(javaClassName, exceptionShapeModel(javaClassName, errorShape, bindingIndex));
            }
        }

        // Errors that appear only in an event stream are not listed by any operation.
        for (StructureShape errorShape : eventStreamErrors(topDown)) {
            String javaClassName = naming.getExceptionName(errorShape.getId().getName());
            if (shapes.containsKey(javaClassName) || currentShapes.containsKey(javaClassName)) {
                continue;
            }
            shapes.put(javaClassName, exceptionShapeModel(javaClassName, errorShape, bindingIndex));
        }

        return shapes;
    }

    private ShapeModel exceptionShapeModel(String javaClassName,
                                           StructureShape errorShape,
                                           HttpBindingIndex bindingIndex) {
        ShapeModel shapeModel = generateShapeModel(javaClassName, errorShape,
                                                   httpBindingsHonored()
                                                       ? bindingIndex.getResponseBindings(errorShape.getId())
                                                       : Collections.emptyMap());
        shapeModel.setType(ShapeType.Exception.getValue());
        shapeModel.setErrorCode(resolveErrorCode(errorShape));
        errorShape.getTrait(HttpErrorTrait.class)
                  .ifPresent(t -> shapeModel.setHttpStatusCode(t.getCode()));
        return shapeModel;
    }

    private List<StructureShape> eventStreamErrors(TopDownIndex topDown) {
        Model model = getModel();
        List<StructureShape> errors = new ArrayList<>();
        for (OperationShape op : topDown.getContainedOperations(getService())) {
            for (ShapeId ioShape : Arrays.asList(op.getInputShape(), op.getOutputShape())) {
                for (MemberShape member : model.expectShape(ioShape).members()) {
                    Shape target = model.expectShape(member.getTarget());
                    if (!target.isUnionShape() || !target.hasTrait(StreamingTrait.class)) {
                        continue;
                    }
                    for (MemberShape streamMember : target.members()) {
                        model.expectShape(streamMember.getTarget())
                             .asStructureShape()
                             .filter(shape -> shape.hasTrait(ErrorTrait.class))
                             .ifPresent(errors::add);
                    }
                }
            }
        }
        return errors;
    }

    /**
     * The wire error code: {@link AwsQueryErrorTrait} where the protocol allows an override,
     * otherwise the error shape's name.
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
        // awsJson and rpcv2Cbor always use the shape name as the code.
        return !"json".equals(protocol) && !"smithy-rpc-v2-cbor".equals(protocol);
    }
}
