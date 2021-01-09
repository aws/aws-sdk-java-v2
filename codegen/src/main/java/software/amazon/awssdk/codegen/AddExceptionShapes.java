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
import software.amazon.awssdk.codegen.internal.Utils;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeType;
import software.amazon.awssdk.codegen.model.service.ErrorTrait;
import software.amazon.awssdk.codegen.model.service.Shape;

/**
 * Constructs the exception shapes for the intermediate model. Analyzes the operations in the
 * service model to identify the exception shapes that are to be generated.
 */
final class AddExceptionShapes extends AddShapes implements IntermediateModelShapeProcessor {

    AddExceptionShapes(IntermediateModelBuilder builder) {
        super(builder);
    }

    @Override
    public Map<String, ShapeModel> process(Map<String, OperationModel> currentOperations,
                                           Map<String, ShapeModel> currentShapes) {
        return constructExceptionShapes();
    }

    private Map<String, ShapeModel> constructExceptionShapes() {
        // Java shape models, to be constructed
        Map<String, ShapeModel> javaShapes = new HashMap<>();

        for (Map.Entry<String, Shape> shape : getServiceModel().getShapes().entrySet()) {
            if (shape.getValue().isException()) {
                String errorShapeName = shape.getKey();
                String javaClassName = getNamingStrategy().getExceptionName(errorShapeName);

                ShapeModel exceptionShapeModel = generateShapeModel(javaClassName,
                                                                    errorShapeName);

                exceptionShapeModel.setType(ShapeType.Exception.getValue());
                exceptionShapeModel.setErrorCode(getErrorCode(errorShapeName));
                exceptionShapeModel.setHttpStatusCode(getHttpStatusCode(errorShapeName));
                if (exceptionShapeModel.getDocumentation() == null) {
                    exceptionShapeModel.setDocumentation(shape.getValue().getDocumentation());
                }

                javaShapes.put(javaClassName, exceptionShapeModel);
            }
        }

        return javaShapes;
    }

    /**
     * The error code may be overridden for query or rest protocols via the error trait on the
     * exception shape. If the error code isn't overridden and for all other protocols other than
     * query or rest the error code should just be the shape name
     */
    private String getErrorCode(String errorShapeName) {
        ErrorTrait errorTrait = getServiceModel().getShapes().get(errorShapeName).getError();
        if (isErrorCodeOverridden(errorTrait)) {
            return errorTrait.getCode();
        } else {
            return errorShapeName;
        }
    }

    private boolean isErrorCodeOverridden(ErrorTrait errorTrait) {
        return errorTrait != null && !Utils.isNullOrEmpty(errorTrait.getCode());
    }

    private Integer getHttpStatusCode(String errorShapeName) {
        ErrorTrait errorTrait = getServiceModel().getShapes().get(errorShapeName).getError();
        return errorTrait != null ? errorTrait.getHttpStatusCode() : null;
    }
}
