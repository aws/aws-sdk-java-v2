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
import software.amazon.awssdk.codegen.model.intermediate.ShapeUnmarshaller;
import software.amazon.awssdk.codegen.model.intermediate.VariableModel;
import software.amazon.awssdk.codegen.naming.NamingStrategy;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.HttpBindingIndex;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.XmlFlattenedTrait;

/**
 * Builds the response {@link ShapeModel} for each operation reachable from the
 * service. When an operation has no output structure it targets Smithy's
 * {@code smithy.api#Unit} sentinel, and this processor synthesizes an empty
 * response shape for it.
 *
 * <p>The {@code resultWrapper} field is intentionally left unset. The
 * query-protocol result wrapper is expressed structurally in Smithy (a
 * single-member output shape) and is applied by the protocol layer at
 * runtime rather than recorded on the shape. See
 * {@code smithy-migration-pr3-context.md} for the rationale.
 */
final class AddSmithyOutputShapes extends AddSmithyShapes implements IntermediateModelShapeProcessor {

    private static final ShapeId UNIT = ShapeId.from("smithy.api#Unit");

    AddSmithyOutputShapes(Model model,
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
            String opName = op.toShapeId().getName();
            String javaClassName = naming.getResponseClassName(opName);

            ShapeId outputId = op.getOutputShape();
            ShapeModel shapeModel;
            if (UNIT.equals(outputId)) {
                shapeModel = synthesizeEmptyResponse(javaClassName);
            } else {
                StructureShape outputShape = model.expectShape(outputId, StructureShape.class);
                shapeModel = generateShapeModel(javaClassName, outputShape,
                                                bindingIndex.getResponseBindings(op.getId()));
                shapeModel.setUnmarshaller(buildUnmarshaller(outputShape));
            }

            if (shapeModel.getUnmarshaller() == null) {
                shapeModel.setUnmarshaller(new ShapeUnmarshaller());
            }
            shapeModel.setType(ShapeType.Response.getValue());

            shapes.put(javaClassName, shapeModel);
        }

        return shapes;
    }

    private ShapeModel synthesizeEmptyResponse(String javaClassName) {
        ShapeModel shape = new ShapeModel(javaClassName);
        shape.setShapeName(javaClassName);
        shape.setVariable(new VariableModel(getNamingStrategy().getVariableName(javaClassName),
                                            javaClassName));
        return shape;
    }

    private static ShapeUnmarshaller buildUnmarshaller(StructureShape outputShape) {
        ShapeUnmarshaller unmarshaller = new ShapeUnmarshaller();
        unmarshaller.setFlattened(outputShape.hasTrait(XmlFlattenedTrait.class));
        // resultWrapper intentionally left null. See class javadoc.
        return unmarshaller;
    }
}
