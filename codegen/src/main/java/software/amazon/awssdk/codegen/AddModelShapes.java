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

import static software.amazon.awssdk.codegen.internal.Utils.isStructure;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeType;
import software.amazon.awssdk.codegen.model.intermediate.ShapeUnmarshaller;
import software.amazon.awssdk.codegen.model.service.Shape;

/**
 * Constructs the shapes (other than request, response and exception) from the service model.
 */
final class AddModelShapes extends AddShapes implements IntermediateModelShapeProcessor {

    AddModelShapes(IntermediateModelBuilder builder) {
        super(builder);
    }

    @Override
    public Map<String, ShapeModel> process(Map<String, OperationModel> currentOperations,
                                           Map<String, ShapeModel> currentShapes) {
        // Only need to construct model shapes for shapes that have not been previously processed
        return constructModelShapes(currentShapes.keySet());
    }

    private Map<String, ShapeModel> constructModelShapes(Set<String> shapesToSkip) {
        // Java output shape models, to be constructed
        Map<String, ShapeModel> javaShapes = new HashMap<>();

        for (Map.Entry<String, Shape> entry : getServiceModel().getShapes().entrySet()) {
            String shapeName = entry.getKey();
            Shape shape = entry.getValue();

            ShapeType shapeType = getModelShapeType(shape);

            if (shapeType != null) {
                String javaClassName = getNamingStrategy().getJavaClassName(shapeName);
                if (shapesToSkip.contains(javaClassName)) {
                    continue;
                }

                ShapeModel modelShape = generateShapeModel(javaClassName, shapeName);
                modelShape.setType(shapeType);

                // We need unmarshaller metadata for all shapes
                ShapeUnmarshaller unmarshaller = new ShapeUnmarshaller();
                unmarshaller.setFlattened(shape.isFlattened());
                modelShape.setUnmarshaller(unmarshaller);

                javaShapes.put(javaClassName, modelShape);
            }
        }

        return javaShapes;
    }

    /**
     * @return null if the given shape is neither a structure nor enum model.
     */
    private ShapeType getModelShapeType(final Shape shape) {
        if (shape.isException()) {
            return null;
        }
        if (isStructure(shape)) {
            return ShapeType.Model;

        } else if (shape.getEnumValues() != null) {
            return ShapeType.Enum;
        }
        return null;
    }
}
