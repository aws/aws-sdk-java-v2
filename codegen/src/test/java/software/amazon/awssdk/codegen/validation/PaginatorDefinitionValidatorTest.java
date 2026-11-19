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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.service.PaginatorDefinition;

public class PaginatorDefinitionValidatorTest {
    private final ModelValidator validator = new PaginatorDefinitionValidator();

    @Test
    void validateModels_noPaginators_noValidationErrors() {
        IntermediateModel model = new IntermediateModel();
        model.setPaginators(Collections.emptyMap());

        assertThat(runValidation(model)).isEmpty();
    }

    @Test
    void validateModels_validPaginator_noValidationErrors() {
        IntermediateModel model = modelWithPaginator("ListItems",
                                                     paginatorDef(Arrays.asList("NextToken"),
                                                                  Arrays.asList("NextToken"),
                                                                  Arrays.asList("Items"),
                                                                  null, null),
                                                     inputShapeWith("NextToken"),
                                                     outputShapeWith("NextToken", "Items"));

        assertThat(runValidation(model)).isEmpty();
    }

    @Test
    void validateModels_operationDoesNotExist_emitsValidationError() {
        IntermediateModel model = new IntermediateModel();
        model.setOperations(Collections.emptyMap());

        PaginatorDefinition def = paginatorDef(Arrays.asList("NextToken"),
                                               Arrays.asList("NextToken"),
                                               null, null, null);
        Map<String, PaginatorDefinition> paginators = new HashMap<>();
        paginators.put("ListSpacesForPrincipal", def);
        model.setPaginators(paginators);

        List<ValidationEntry> entries = runValidation(model);

        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).getErrorId()).isEqualTo(ValidationErrorId.UNKNOWN_OPERATION);
        assertThat(entries.get(0).getSeverity()).isEqualTo(ValidationErrorSeverity.DANGER);
        assertThat(entries.get(0).getDetailMessage()).contains("ListSpacesForPrincipal");
    }

    @Test
    void validateModels_invalidInputToken_emitsValidationError() {
        IntermediateModel model = modelWithPaginator("ListItems",
                                                     paginatorDef(Arrays.asList("NonExistentToken"),
                                                                  Arrays.asList("NextToken"),
                                                                  null, null, null),
                                                     inputShapeWith("NextToken"),
                                                     outputShapeWith("NextToken"));

        List<ValidationEntry> entries = runValidation(model);

        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).getErrorId()).isEqualTo(ValidationErrorId.INVALID_PAGINATOR_DEFINITION);
        assertThat(entries.get(0).getDetailMessage()).contains("input_token");
        assertThat(entries.get(0).getDetailMessage()).contains("NonExistentToken");
    }

    @Test
    void validateModels_invalidOutputToken_emitsValidationError() {
        IntermediateModel model = modelWithPaginator("ListItems",
                                                     paginatorDef(Arrays.asList("NextToken"),
                                                                  Arrays.asList("NonExistentToken"),
                                                                  null, null, null),
                                                     inputShapeWith("NextToken"),
                                                     outputShapeWith("NextToken"));

        List<ValidationEntry> entries = runValidation(model);

        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).getErrorId()).isEqualTo(ValidationErrorId.INVALID_PAGINATOR_DEFINITION);
        assertThat(entries.get(0).getDetailMessage()).contains("output_token");
        assertThat(entries.get(0).getDetailMessage()).contains("NonExistentToken");
    }

    @Test
    void validateModels_invalidResultKey_emitsValidationError() {
        IntermediateModel model = modelWithPaginator("ListItems",
                                                     paginatorDef(Arrays.asList("NextToken"),
                                                                  Arrays.asList("NextToken"),
                                                                  Arrays.asList("NonExistentKey"),
                                                                  null, null),
                                                     inputShapeWith("NextToken"),
                                                     outputShapeWith("NextToken"));

        List<ValidationEntry> entries = runValidation(model);

        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).getErrorId()).isEqualTo(ValidationErrorId.INVALID_PAGINATOR_DEFINITION);
        assertThat(entries.get(0).getDetailMessage()).contains("result_key");
        assertThat(entries.get(0).getDetailMessage()).contains("NonExistentKey");
    }

    @Test
    void validateModels_invalidMoreResults_emitsValidationError() {
        IntermediateModel model = modelWithPaginator("ListItems",
                                                     paginatorDef(Arrays.asList("NextToken"),
                                                                  Arrays.asList("NextToken"),
                                                                  null, "NonExistentField", null),
                                                     inputShapeWith("NextToken"),
                                                     outputShapeWith("NextToken"));

        List<ValidationEntry> entries = runValidation(model);

        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).getErrorId()).isEqualTo(ValidationErrorId.INVALID_PAGINATOR_DEFINITION);
        assertThat(entries.get(0).getDetailMessage()).contains("more_results");
        assertThat(entries.get(0).getDetailMessage()).contains("NonExistentField");
    }

    @Test
    void validateModels_invalidLimitKey_emitsValidationError() {
        IntermediateModel model = modelWithPaginator("ListItems",
                                                     paginatorDef(Arrays.asList("NextToken"),
                                                                  Arrays.asList("NextToken"),
                                                                  null, null, "NonExistentLimit"),
                                                     inputShapeWith("NextToken"),
                                                     outputShapeWith("NextToken"));

        List<ValidationEntry> entries = runValidation(model);

        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).getErrorId()).isEqualTo(ValidationErrorId.INVALID_PAGINATOR_DEFINITION);
        assertThat(entries.get(0).getDetailMessage()).contains("limit_key");
        assertThat(entries.get(0).getDetailMessage()).contains("NonExistentLimit");
    }

    @Test
    void validateModels_invalidPaginatorSkipped_noValidationErrors() {
        // A paginator that fails hasAllRequiredFields() should be skipped (not validated further).
        // This is legacy behavior: codegen silently skips incomplete paginator definitions.
        IntermediateModel model = new IntermediateModel();
        model.setOperations(Collections.emptyMap());

        // No inputToken/outputToken means hasAllRequiredFields() returns false
        PaginatorDefinition def = new PaginatorDefinition();
        Map<String, PaginatorDefinition> paginators = new HashMap<>();
        paginators.put("NonExistentOperation", def);
        model.setPaginators(paginators);

        assertThat(runValidation(model)).isEmpty();
    }

    @Test
    void validateModels_nestedOutputToken_validatesCorrectly() {
        // Test dotted path like "StreamDescription.LastEvaluatedShardId"
        ShapeModel innerShape = new ShapeModel();
        MemberModel innerMember = new MemberModel();
        innerMember.setName("LastEvaluatedShardId");
        innerMember.setC2jName("LastEvaluatedShardId");
        innerShape.setMembers(Arrays.asList(innerMember));

        MemberModel outerMember = new MemberModel();
        outerMember.setName("StreamDescription");
        outerMember.setC2jName("StreamDescription");
        outerMember.setShape(innerShape);

        ShapeModel outputShape = new ShapeModel();
        outputShape.setShapeName("ListItemsResponse");
        outputShape.setMembers(Arrays.asList(outerMember));

        IntermediateModel model = modelWithPaginator("ListItems",
                                                     paginatorDef(Arrays.asList("NextToken"),
                                                                  Arrays.asList("StreamDescription.LastEvaluatedShardId"),
                                                                  null, null, null),
                                                     inputShapeWith("NextToken"),
                                                     outputShape);

        assertThat(runValidation(model)).isEmpty();
    }

    @Test
    void validateModels_invalidNestedOutputToken_emitsValidationError() {
        // Test invalid dotted path
        MemberModel outerMember = new MemberModel();
        outerMember.setName("StreamDescription");
        outerMember.setC2jName("StreamDescription");
        ShapeModel innerShape = new ShapeModel();
        innerShape.setMembers(Collections.emptyList());
        outerMember.setShape(innerShape);

        ShapeModel outputShape = new ShapeModel();
        outputShape.setShapeName("ListItemsResponse");
        outputShape.setMembers(Arrays.asList(outerMember));

        IntermediateModel model = modelWithPaginator("ListItems",
                                                     paginatorDef(Arrays.asList("NextToken"),
                                                                  Arrays.asList("StreamDescription.NonExistent"),
                                                                  null, null, null),
                                                     inputShapeWith("NextToken"),
                                                     outputShape);

        List<ValidationEntry> entries = runValidation(model);

        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).getErrorId()).isEqualTo(ValidationErrorId.INVALID_PAGINATOR_DEFINITION);
        assertThat(entries.get(0).getDetailMessage()).contains("output_token");
        assertThat(entries.get(0).getDetailMessage()).contains("StreamDescription.NonExistent");
    }

    @Test
    void validateModels_nestedPathWithNullIntermediateShape_emitsValidationError() {
        // A member whose getShape() returns null but is not the last segment
        MemberModel outerMember = new MemberModel();
        outerMember.setName("StreamDescription");
        outerMember.setC2jName("StreamDescription");
        outerMember.setShape(null); // No nested shape

        ShapeModel outputShape = new ShapeModel();
        outputShape.setShapeName("ListItemsResponse");
        outputShape.setMembers(Arrays.asList(outerMember));

        IntermediateModel model = modelWithPaginator("ListItems",
                                                     paginatorDef(Arrays.asList("NextToken"),
                                                                  Arrays.asList("StreamDescription.NestedField"),
                                                                  null, null, null),
                                                     inputShapeWith("NextToken"),
                                                     outputShape);

        List<ValidationEntry> entries = runValidation(model);

        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).getErrorId()).isEqualTo(ValidationErrorId.INVALID_PAGINATOR_DEFINITION);
        assertThat(entries.get(0).getDetailMessage()).contains("StreamDescription.NestedField");
    }

    @Test
    void validateModels_multipleErrors_emitsAllValidationErrors() {
        IntermediateModel model = modelWithPaginator("ListItems",
                                                     paginatorDef(Arrays.asList("BadInput"),
                                                                  Arrays.asList("BadOutput"),
                                                                  Arrays.asList("BadResult"),
                                                                  "BadMore", "BadLimit"),
                                                     inputShapeWith("NextToken"),
                                                     outputShapeWith("NextToken"));

        List<ValidationEntry> entries = runValidation(model);

        // Should have errors for: input_token, output_token, result_key, more_results, limit_key
        assertThat(entries).hasSize(5);
        assertThat(entries).allMatch(e -> e.getSeverity() == ValidationErrorSeverity.DANGER);
    }

    private List<ValidationEntry> runValidation(IntermediateModel model) {
        ModelValidationContext ctx = ModelValidationContext.builder()
                                                          .intermediateModel(model)
                                                          .build();
        return validator.validateModels(ctx);
    }

    private PaginatorDefinition paginatorDef(List<String> inputToken, List<String> outputToken,
                                             List<String> resultKey, String moreResults, String limitKey) {
        PaginatorDefinition def = new PaginatorDefinition();
        def.setInputToken(inputToken);
        def.setOutputToken(outputToken);
        def.setResultKey(resultKey);
        def.setMoreResults(moreResults);
        def.setLimitKey(limitKey);
        return def;
    }

    private ShapeModel inputShapeWith(String... memberNames) {
        ShapeModel shape = new ShapeModel();
        shape.setShapeName("ListItemsRequest");
        List<MemberModel> members = new java.util.ArrayList<>();
        for (String name : memberNames) {
            MemberModel member = new MemberModel();
            member.setName(name);
            member.setC2jName(name);
            members.add(member);
        }
        shape.setMembers(members);
        return shape;
    }

    private ShapeModel outputShapeWith(String... memberNames) {
        ShapeModel shape = new ShapeModel();
        shape.setShapeName("ListItemsResponse");
        List<MemberModel> members = new java.util.ArrayList<>();
        for (String name : memberNames) {
            MemberModel member = new MemberModel();
            member.setName(name);
            member.setC2jName(name);
            members.add(member);
        }
        shape.setMembers(members);
        return shape;
    }

    private IntermediateModel modelWithPaginator(String operationName, PaginatorDefinition def,
                                                 ShapeModel inputShape, ShapeModel outputShape) {
        IntermediateModel model = new IntermediateModel();

        OperationModel opModel = new OperationModel();
        opModel.setOperationName(operationName);
        opModel.setInputShape(inputShape);
        opModel.setOutputShape(outputShape);

        Map<String, OperationModel> operations = new HashMap<>();
        operations.put(operationName, opModel);
        model.setOperations(operations);

        Map<String, PaginatorDefinition> paginators = new HashMap<>();
        paginators.put(operationName, def);
        model.setPaginators(paginators);

        return model;
    }
}
