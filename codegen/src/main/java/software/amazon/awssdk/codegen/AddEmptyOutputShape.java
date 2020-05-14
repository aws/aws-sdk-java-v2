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

import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.intermediate.ReturnTypeModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeType;
import software.amazon.awssdk.codegen.model.intermediate.ShapeUnmarshaller;
import software.amazon.awssdk.codegen.model.intermediate.VariableModel;
import software.amazon.awssdk.codegen.model.service.Operation;
import software.amazon.awssdk.codegen.model.service.Output;
import software.amazon.awssdk.codegen.model.service.ServiceModel;
import software.amazon.awssdk.codegen.naming.NamingStrategy;

public class AddEmptyOutputShape implements IntermediateModelShapeProcessor {

    private final ServiceModel serviceModel;
    private final NamingStrategy namingStrategy;

    public AddEmptyOutputShape(IntermediateModelBuilder builder) {
        this.serviceModel = builder.getService();
        this.namingStrategy = builder.getNamingStrategy();
    }

    @Override
    public Map<String, ShapeModel> process(Map<String, OperationModel> currentOperations,
                                           Map<String, ShapeModel> currentShapes) {
        return addEmptyOutputShapes(currentOperations);
    }

    private Map<String, ShapeModel> addEmptyOutputShapes(
        Map<String, OperationModel> currentOperations) {
        Map<String, Operation> operations = serviceModel.getOperations();

        Map<String, ShapeModel> emptyOutputShapes = new HashMap<>();

        for (Map.Entry<String, Operation> entry : operations.entrySet()) {
            String operationName = entry.getKey();
            Operation operation = entry.getValue();

            Output output = operation.getOutput();
            if (output == null) {
                String outputShape = namingStrategy.getResponseClassName(operationName);
                OperationModel operationModel = currentOperations.get(operationName);

                operationModel.setReturnType(new ReturnTypeModel(outputShape));

                ShapeModel shape = new ShapeModel(outputShape)
                    .withType(ShapeType.Response.getValue());
                shape.setShapeName(outputShape);

                VariableModel outputVariable = new VariableModel(
                    namingStrategy.getVariableName(outputShape), outputShape);
                shape.setVariable(outputVariable);
                shape.setUnmarshaller(new ShapeUnmarshaller());

                emptyOutputShapes.put(outputShape, shape);
            }
        }
        return emptyOutputShapes;
    }

}
