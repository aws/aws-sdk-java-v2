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

package software.amazon.awssdk.codegen.smithy.customization.processors;

import java.util.Set;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.smithy.customization.SmithyCustomizationProcessor;
import software.amazon.awssdk.codegen.validation.ModelInvalidException;
import software.amazon.awssdk.codegen.validation.ValidationEntry;
import software.amazon.awssdk.codegen.validation.ValidationErrorId;
import software.amazon.awssdk.codegen.validation.ValidationErrorSeverity;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.neighbor.Walker;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;

/**
 * Smithy equivalent of {@link software.amazon.awssdk.codegen.customization.processors.LowercaseShapeValidatorProcessor}.
 *
 * <p>This is a Category B processor (direct Smithy equivalent). It validates that all structure
 * shape names in the service closure start with an uppercase letter, which is required because
 * structure shapes are converted to Java classes that must follow Java naming conventions.
 */
public final class LowercaseShapeValidatorProcessor implements SmithyCustomizationProcessor {

    @Override
    public Model preprocess(Model model, ServiceShape service) {
        Set<Shape> serviceShapes = new Walker(model).walkShapes(service);
        for (Shape shape : serviceShapes) {
            if (shape.isStructureShape()
                    && Character.isLowerCase(shape.getId().getName().charAt(0))) {
                String errorMsg = String.format("Shape name '%s' starts with a lowercase character. Shape names must start with"
                                                + " an uppercase character. Please update the shape name in your service model",
                                                shape.getId().getName());
                ValidationEntry entry = ValidationEntry.create(ValidationErrorId.INVALID_IDENTIFIER_NAME,
                                                               ValidationErrorSeverity.DANGER, errorMsg);
                throw ModelInvalidException.fromEntry(entry);
            }
        }
        return model;
    }

    @Override
    public void postprocess(IntermediateModel intermediateModel) {
        // no-op
    }
}
