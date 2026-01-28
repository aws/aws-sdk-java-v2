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

package software.amazon.awssdk.codegen.customization.processors;

import software.amazon.awssdk.codegen.customization.CodegenCustomizationProcessor;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.service.ServiceModel;
import software.amazon.awssdk.codegen.validation.ModelInvalidException;
import software.amazon.awssdk.codegen.validation.ValidationEntry;
import software.amazon.awssdk.codegen.validation.ValidationErrorId;
import software.amazon.awssdk.codegen.validation.ValidationErrorSeverity;

/**
 * A processor that validates shape names in service models to ensure they start with uppercase letters.
 * This validation is necessary because shapes of type "structure" are converted to Java classes,
 * which must start with uppercase letters according to Java naming conventions.
 */
public class LowercaseShapeValidatorProcessor implements CodegenCustomizationProcessor {

    @Override
    public void preprocess(ServiceModel serviceModel) {

        serviceModel.getShapes().forEach((shapeName, shape) -> {
            if ("structure".equals(shape.getType()) && Character.isLowerCase(shapeName.charAt(0))) {
                String errorMsg = String.format("Shape name '%s' starts with a lowercase character. Shape names must start with"
                                                + " an uppercase character. Please update the shape name in your service model",
                                                shapeName);
                ValidationEntry entry = ValidationEntry.create(ValidationErrorId.INVALID_IDENTIFIER_NAME,
                                                               ValidationErrorSeverity.DANGER, errorMsg);
                throw ModelInvalidException.fromEntry(entry);
            }
        });
    }

    @Override
    public void postprocess(IntermediateModel intermediateModel) {
        // no-op
    }
}
