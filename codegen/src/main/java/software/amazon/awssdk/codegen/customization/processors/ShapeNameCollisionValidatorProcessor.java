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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.codegen.customization.CodegenCustomizationProcessor;
import software.amazon.awssdk.codegen.internal.Utils;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.service.ServiceModel;
import software.amazon.awssdk.codegen.validation.ModelInvalidException;
import software.amazon.awssdk.codegen.validation.ValidationEntry;
import software.amazon.awssdk.codegen.validation.ValidationErrorId;
import software.amazon.awssdk.codegen.validation.ValidationErrorSeverity;

/**
 * Rejects shape names that differ only by first character case, which otherwise collide on their generated Java class name.
 */
public class ShapeNameCollisionValidatorProcessor implements CodegenCustomizationProcessor {

    @Override
    public void preprocess(ServiceModel serviceModel) {
        Map<String, String> shapeByClassName = new LinkedHashMap<>();
        List<ValidationEntry> collisions = new ArrayList<>();

        serviceModel.getShapes().keySet().forEach(shapeName -> {
            String className = Utils.capitalize(shapeName);
            String existing = shapeByClassName.putIfAbsent(className, shapeName);
            if (existing != null) {
                String errorMsg = String.format(
                    "Shape names '%s' and '%s' collide because they map to the same generated class name '%s'. Please rename "
                    + "one of them in your service model.",
                    existing, shapeName, className);
                collisions.add(ValidationEntry.create(ValidationErrorId.INVALID_IDENTIFIER_NAME,
                                                      ValidationErrorSeverity.DANGER, errorMsg));
            }
        });

        if (!collisions.isEmpty()) {
            throw ModelInvalidException.builder().validationEntries(collisions).build();
        }
    }

    @Override
    public void postprocess(IntermediateModel intermediateModel) {
        // no-op
    }
}
