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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import software.amazon.awssdk.codegen.IntermediateModelShapeProcessor;
import software.amazon.awssdk.codegen.internal.TypeUtils;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeType;
import software.amazon.awssdk.codegen.model.intermediate.ShapeUnmarshaller;
import software.amazon.awssdk.codegen.naming.NamingStrategy;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.ErrorTrait;
import software.amazon.smithy.model.traits.XmlFlattenedTrait;

/**
 * Builds a {@link ShapeModel} for every structure, union, and enum reachable from the service's
 * operations that the input, output, and exception processors did not already produce.
 *
 * <p>Reachability walks outward from each operation's input, output, and error shapes; shapes no
 * operation references are not translated.
 */
final class AddSmithyModelShapes extends AddSmithyShapes implements IntermediateModelShapeProcessor {

    AddSmithyModelShapes(Model model,
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
        Map<String, ShapeModel> newShapes = new HashMap<>();
        Model model = getModel();
        NamingStrategy naming = getNamingStrategy();
        TopDownIndex topDown = TopDownIndex.of(model);

        Set<ShapeId> processed = new HashSet<>();
        Deque<ShapeId> queue = new ArrayDeque<>();

        for (OperationShape op : topDown.getContainedOperations(getService())) {
            queue.add(op.getInputShape());
            queue.add(op.getOutputShape());
            queue.addAll(op.getErrorsSet());
        }
        queue.addAll(getService().getErrorsSet());

        while (!queue.isEmpty()) {
            ShapeId shapeId = queue.poll();
            if (!processed.add(shapeId)) {
                continue;
            }
            // Prelude sentinels (Unit, primitives) have no generated shape.
            if ("smithy.api".equals(shapeId.getNamespace())) {
                continue;
            }

            Shape shape = model.getShape(shapeId).orElse(null);
            if (shape == null) {
                continue;
            }

            for (MemberShape m : shape.members()) {
                queue.add(m.getTarget());
            }
            if (shape.isListShape() || shape.isMapShape()) {
                continue;
            }

            if (!isTranslatableAsModelShape(shape)) {
                continue;
            }

            String javaClassName = naming.getShapeClassName(shapeId.getName());
            if (currentShapes.containsKey(javaClassName) || newShapes.containsKey(javaClassName)) {
                continue;
            }
            // Handled by AddSmithyExceptionShapes.
            if (shape.hasTrait(ErrorTrait.class)) {
                continue;
            }

            ShapeModel shapeModel = generateShapeModel(javaClassName, shape, null);
            shapeModel.setType(isEnumKind(shape) ? ShapeType.Enum.getValue() : ShapeType.Model.getValue());

            ShapeUnmarshaller unmarshaller = new ShapeUnmarshaller();
            unmarshaller.setFlattened(shape.hasTrait(XmlFlattenedTrait.class));
            shapeModel.setUnmarshaller(unmarshaller);

            newShapes.put(javaClassName, shapeModel);
        }

        return newShapes;
    }

    // intEnum is excluded deliberately: C2J models it as a plain integer with no generated shape.
    private static boolean isTranslatableAsModelShape(Shape shape) {
        return shape.isStructureShape()
               || shape.isUnionShape()
               || shape.isEnumShape();
    }

    private static boolean isEnumKind(Shape shape) {
        return shape.isEnumShape();
    }
}
