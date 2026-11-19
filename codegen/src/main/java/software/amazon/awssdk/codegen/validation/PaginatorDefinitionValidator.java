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

package software.amazon.awssdk.codegen.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.service.PaginatorDefinition;

/**
 * Validates paginator definitions against the service model. Checks that:
 * <ul>
 *     <li>Each paginator references an operation that exists in the model</li>
 *     <li>Input tokens reference valid members in the request shape</li>
 *     <li>Output tokens reference valid members in the response shape</li>
 *     <li>Result keys reference valid members in the response shape</li>
 *     <li>The more_results field, if specified, references a valid member in the response shape</li>
 *     <li>The limit_key field, if specified, references a valid member in the request shape</li>
 * </ul>
 */
public final class PaginatorDefinitionValidator implements ModelValidator {

    @Override
    public List<ValidationEntry> validateModels(ModelValidationContext context) {
        List<ValidationEntry> entries = new ArrayList<>();
        IntermediateModel model = context.intermediateModel();
        Map<String, PaginatorDefinition> paginators = model.getPaginators();

        if (paginators == null || paginators.isEmpty()) {
            return entries;
        }

        for (Map.Entry<String, PaginatorDefinition> entry : paginators.entrySet()) {
            String operationName = entry.getKey();
            PaginatorDefinition definition = entry.getValue();

            // Skip paginators that don't have the minimum required fields (inputToken, outputToken).
            // This is legacy behavior: codegen has historically silently skipped these incomplete definitions
            // rather than generating code for them. We mirror that filter here so we only validate paginators
            // that will actually be used during code generation.
            if (!definition.hasAllRequiredFields()) {
                continue;
            }

            OperationModel operationModel = model.getOperation(operationName);
            if (operationModel == null) {
                entries.add(ValidationEntry.create(
                    ValidationErrorId.UNKNOWN_OPERATION,
                    ValidationErrorSeverity.DANGER,
                    String.format("Invalid paginator definition - The service model does not contain the "
                                  + "referenced operation '%s'", operationName)
                ));
                continue;
            }

            ShapeModel inputShape = operationModel.getInputShape();
            ShapeModel outputShape = operationModel.getOutputShape();

            validateInputTokens(entries, operationName, definition, inputShape);
            validateOutputTokens(entries, operationName, definition, outputShape);
            validateResultKeys(entries, operationName, definition, outputShape);
            validateMoreResults(entries, operationName, definition, outputShape);
            validateLimitKey(entries, operationName, definition, inputShape);
        }

        return entries;
    }

    private void validateInputTokens(List<ValidationEntry> entries, String operationName,
                                     PaginatorDefinition definition, ShapeModel inputShape) {
        if (definition.getInputToken() == null || inputShape == null) {
            return;
        }

        for (String inputToken : definition.getInputToken()) {
            if (!isValidMemberPath(inputShape, inputToken)) {
                entries.add(ValidationEntry.create(
                    ValidationErrorId.INVALID_PAGINATOR_DEFINITION,
                    ValidationErrorSeverity.DANGER,
                    String.format("Paginator for operation '%s' references input_token '%s' which does not "
                                  + "exist in the request shape '%s'", operationName, inputToken,
                                  inputShape.getShapeName())
                ));
            }
        }
    }

    private void validateOutputTokens(List<ValidationEntry> entries, String operationName,
                                      PaginatorDefinition definition, ShapeModel outputShape) {
        if (definition.getOutputToken() == null || outputShape == null) {
            return;
        }

        for (String outputToken : definition.getOutputToken()) {
            if (!isValidMemberPath(outputShape, outputToken)) {
                entries.add(ValidationEntry.create(
                    ValidationErrorId.INVALID_PAGINATOR_DEFINITION,
                    ValidationErrorSeverity.DANGER,
                    String.format("Paginator for operation '%s' references output_token '%s' which does not "
                                  + "exist in the response shape '%s'", operationName, outputToken,
                                  outputShape.getShapeName())
                ));
            }
        }
    }

    private void validateResultKeys(List<ValidationEntry> entries, String operationName,
                                    PaginatorDefinition definition, ShapeModel outputShape) {
        if (definition.getResultKey() == null || outputShape == null) {
            return;
        }

        for (String resultKey : definition.getResultKey()) {
            if (!isValidMemberPath(outputShape, resultKey)) {
                entries.add(ValidationEntry.create(
                    ValidationErrorId.INVALID_PAGINATOR_DEFINITION,
                    ValidationErrorSeverity.DANGER,
                    String.format("Paginator for operation '%s' references result_key '%s' which does not "
                                  + "exist in the response shape '%s'", operationName, resultKey,
                                  outputShape.getShapeName())
                ));
            }
        }
    }

    private void validateMoreResults(List<ValidationEntry> entries, String operationName,
                                     PaginatorDefinition definition, ShapeModel outputShape) {
        if (definition.getMoreResults() == null || outputShape == null) {
            return;
        }

        if (!isValidMemberPath(outputShape, definition.getMoreResults())) {
            entries.add(ValidationEntry.create(
                ValidationErrorId.INVALID_PAGINATOR_DEFINITION,
                ValidationErrorSeverity.DANGER,
                String.format("Paginator for operation '%s' references more_results '%s' which does not "
                              + "exist in the response shape '%s'", operationName, definition.getMoreResults(),
                              outputShape.getShapeName())
            ));
        }
    }

    private void validateLimitKey(List<ValidationEntry> entries, String operationName,
                                  PaginatorDefinition definition, ShapeModel inputShape) {
        if (definition.getLimitKey() == null || inputShape == null) {
            return;
        }

        if (!isValidMemberPath(inputShape, definition.getLimitKey())) {
            entries.add(ValidationEntry.create(
                ValidationErrorId.INVALID_PAGINATOR_DEFINITION,
                ValidationErrorSeverity.DANGER,
                String.format("Paginator for operation '%s' references limit_key '%s' which does not "
                              + "exist in the request shape '%s'", operationName, definition.getLimitKey(),
                              inputShape.getShapeName())
            ));
        }
    }

    /**
     * Validates that a dotted member path (e.g. "StreamDescription.Shards") resolves to a valid member
     * in the given shape hierarchy.
     */
    private boolean isValidMemberPath(ShapeModel shape, String memberPath) {
        if (shape == null || memberPath == null || memberPath.isEmpty()) {
            return false;
        }

        String[] segments = memberPath.split("\\.");
        ShapeModel currentShape = shape;

        for (int i = 0; i < segments.length; i++) {
            MemberModel member = currentShape.getMemberByC2jName(segments[i]);
            if (member == null) {
                return false;
            }
            if (i < segments.length - 1) {
                currentShape = member.getShape();
                if (currentShape == null) {
                    return false;
                }
            }
        }

        return true;
    }
}
