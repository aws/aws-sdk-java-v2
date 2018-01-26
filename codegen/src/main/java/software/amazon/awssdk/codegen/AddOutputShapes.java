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

import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeType;
import software.amazon.awssdk.codegen.model.intermediate.ShapeUnmarshaller;
import software.amazon.awssdk.codegen.model.service.Operation;
import software.amazon.awssdk.codegen.model.service.Output;
import software.amazon.awssdk.codegen.model.service.Shape;

/**
 * Constructs the result shapes for the intermediate model. Analyzes the operations in the service
 * model to identify the result shapes that are to be generated.
 */
final class AddOutputShapes extends AddShapes implements IntermediateModelShapeProcessor {

    AddOutputShapes(IntermediateModelBuilder builder) {
        super(builder);
    }

    @Override
    public Map<String, ShapeModel> process(Map<String, OperationModel> currentOperations,
                                           Map<String, ShapeModel> currentShapes) {
        return constructOutputShapes();
    }

    private Map<String, ShapeModel> constructOutputShapes() {
        // C2j model components
        final Map<String, Operation> operations = getServiceModel().getOperations();
        final Map<String, Shape> c2jShapes = getServiceModel().getShapes();

        // Java shape models, to be constructed
        final Map<String, ShapeModel> javaShapes = new HashMap<String, ShapeModel>();

        for (Map.Entry<String, Operation> entry : operations.entrySet()) {

            String operationName = entry.getKey();
            Operation operation = entry.getValue();

            Output output = operation.getOutput();

            if (output != null) {

                String javaResponseClassName = getNamingStrategy()
                        .getResponseClassName(operationName);

                ShapeModel outputShape = generateOutputShapeModel(operation, javaResponseClassName,
                                                                  c2jShapes);

                if (outputShape.getDocumentation() == null) {
                    outputShape.setDocumentation(output.getDocumentation());
                }

                javaShapes.put(javaResponseClassName, outputShape);
            }
        }

        return javaShapes;
    }

    private ShapeModel generateOutputShapeModel(Operation c2jOperationModel,
                                                      String javaOutputShapeNameOverride,
                                                      Map<String, Shape> c2jShapes) {

        final Output c2jOutputModel = c2jOperationModel.getOutput();
        final String c2jOutputShapeName = c2jOutputModel.getShape();

        ShapeModel shapeModel = generateShapeModel(javaOutputShapeNameOverride, c2jOutputShapeName);

        shapeModel.setType(ShapeType.Response.getValue());

        // Set up unmarshaller metadata
        ShapeUnmarshaller shapeUnmarshaller = new ShapeUnmarshaller()
                .withFlattened(c2jShapes.get(c2jOutputShapeName).isFlattened())
                .withResultWrapper(c2jOutputModel.getResultWrapper());

        shapeModel.setUnmarshaller(shapeUnmarshaller);

        return shapeModel;
    }
}
