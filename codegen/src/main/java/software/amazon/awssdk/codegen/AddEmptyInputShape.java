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

import static software.amazon.awssdk.codegen.internal.Constant.REQUEST_CLASS_SUFFIX;
import static software.amazon.awssdk.codegen.internal.Utils.createInputShapeMarshaller;
import static software.amazon.awssdk.codegen.internal.Utils.unCapitalize;

import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeType;
import software.amazon.awssdk.codegen.model.intermediate.VariableModel;
import software.amazon.awssdk.codegen.model.service.Input;
import software.amazon.awssdk.codegen.model.service.Operation;
import software.amazon.awssdk.codegen.model.service.ServiceModel;
import software.amazon.awssdk.codegen.naming.NamingStrategy;

/**
 * This class adds empty input shapes to those operations which doesn't accept any input params. It
 * also creates a shape model for the shape with no members in it.
 */
final class AddEmptyInputShape implements IntermediateModelShapeProcessor {

    private final ServiceModel serviceModel;
    private final NamingStrategy namingStrategy;

    AddEmptyInputShape(IntermediateModelBuilder builder) {
        this.serviceModel = builder.getService();
        this.namingStrategy = builder.getNamingStrategy();
    }

    @Override
    public Map<String, ShapeModel> process(Map<String, OperationModel> currentOperations,
                                           Map<String, ShapeModel> currentShapes) {
        return addEmptyInputShapes(currentOperations);
    }

    private Map<String, ShapeModel> addEmptyInputShapes(
            Map<String, OperationModel> javaOperationMap) {
        Map<String, Operation> operations = serviceModel.getOperations();

        Map<String, ShapeModel> emptyInputShapes = new HashMap<>();

        for (Map.Entry<String, Operation> entry : operations.entrySet()) {
            String operationName = entry.getKey();
            Operation operation = entry.getValue();

            Input input = operation.getInput();
            if (input == null) {
                String inputShape = operationName + REQUEST_CLASS_SUFFIX;
                OperationModel operationModel = javaOperationMap.get(operationName);

                operationModel.setInput(new VariableModel(unCapitalize(inputShape), inputShape));

                ShapeModel shape = new ShapeModel(inputShape)
                        .withType(ShapeType.Request.getValue());
                shape.setShapeName(inputShape);

                VariableModel inputVariable = new VariableModel(
                        namingStrategy.getVariableName(inputShape), inputShape);
                shape.setVariable(inputVariable);

                shape.setMarshaller(
                        createInputShapeMarshaller(serviceModel.getMetadata(), operation));

                emptyInputShapes.put(inputShape, shape);

            }
        }
        return emptyInputShapes;
    }
}
