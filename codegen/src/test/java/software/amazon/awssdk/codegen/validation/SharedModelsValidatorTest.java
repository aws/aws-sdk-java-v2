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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.Metadata;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.poet.ClientTestModels;

public class SharedModelsValidatorTest {
    private final ModelValidator validator = new SharedModelsValidator();

    @Test
    void validateModels_noTargetService_noValidationErrors() {
        assertThat(runValidation(ClientTestModels.awsJsonServiceModels(), null)).isEmpty();
    }

    @Test
    void validateModels_targetServiceTriviallyIdentical_noValidationErrors() {
        assertThat(runValidation(ClientTestModels.awsJsonServiceModels(), ClientTestModels.awsJsonServiceModels())).isEmpty();
    }

    @Test
    void validateModels_noSharedShapes_noValidationErrors() {
        IntermediateModel target = ClientTestModels.awsJsonServiceModels();
        Map<String, ShapeModel> renamedShapes = target.getShapes()
                                                      .entrySet()
                                                      .stream()
                                                      .collect(Collectors.toMap(e -> "Copy" + e.getKey(), Map.Entry::getValue));
        target.setShapes(renamedShapes);

        assertThat(runValidation(ClientTestModels.awsJsonServiceModels(), target)).isEmpty();
    }

    @Test
    void validateModels_sharedShapesNotIdentical_emitsValidationError() {
        IntermediateModel target = ClientTestModels.awsJsonServiceModels();
        Map<String, ShapeModel> modifiedShapes = target.getShapes()
                                                      .entrySet()
                                                      .stream()
                                                      .collect(Collectors.toMap(Map.Entry::getKey,
                                                                                e -> {
                                                                                    ShapeModel shapeModel = e.getValue();
                                                                                    shapeModel.setDeprecated(!shapeModel.isDeprecated());
                                                                                    return shapeModel;
                                                                                }));

        target.setShapes(modifiedShapes);

        List<ValidationEntry> validationEntries = runValidation(ClientTestModels.awsJsonServiceModels(), target);

        assertThat(validationEntries).hasSize(modifiedShapes.size());

        assertThat(validationEntries).allMatch(e -> e.getErrorId() == ValidationErrorId.SHARED_MODELS_DIFFER
                                                    && e.getSeverity() == ValidationErrorSeverity.DANGER);
    }

    @Test
    void validateModels_shapesDontHaveSameMemberNames_emitsValidationError() {
        IntermediateModel fooService = new IntermediateModel();
        fooService.setMetadata(new Metadata().withServiceName("Foo"));

        IntermediateModel barService = new IntermediateModel();
        barService.setMetadata(new Metadata().withServiceName("Bar"));

        String shapeName = "TestShape";

        ShapeModel shape1 = new ShapeModel();
        MemberModel member1 = new MemberModel();
        member1.setName("Shape1Member");
        shape1.setMembers(Arrays.asList(member1));

        ShapeModel shape2 = new ShapeModel();
        MemberModel member2 = new MemberModel();
        member2.setName("Shape2Member");
        shape2.setMembers(Arrays.asList(member2));

        Map<String, ShapeModel> fooServiceShapes = new HashMap<>();
        fooServiceShapes.put(shapeName, shape1);
        fooService.setShapes(fooServiceShapes);

        Map<String, ShapeModel> barServiceShapes = new HashMap<>();
        barServiceShapes.put(shapeName, shape2);
        barService.setShapes(barServiceShapes);

        List<ValidationEntry> validationEntries = runValidation(fooService, barService);

        assertThat(validationEntries).hasSize(1);
    }

    @Test
    void validateModels_shapesDontHaveSameMembers_emitsValidationError() {
        IntermediateModel fooService = new IntermediateModel();
        fooService.setMetadata(new Metadata().withServiceName("Foo"));

        IntermediateModel barService = new IntermediateModel();
        barService.setMetadata(new Metadata().withServiceName("Bar"));

        String shapeName = "TestShape";
        ShapeModel shape1 = new ShapeModel();

        ShapeModel shape2 = new ShapeModel();
        shape2.setMembers(Arrays.asList(new MemberModel(), new MemberModel()));

        Map<String, ShapeModel> fooServiceShapes = new HashMap<>();
        fooServiceShapes.put(shapeName, shape1);
        fooService.setShapes(fooServiceShapes);

        Map<String, ShapeModel> barServiceShapes = new HashMap<>();
        barServiceShapes.put(shapeName, shape2);
        barService.setShapes(barServiceShapes);

        List<ValidationEntry> validationEntries = runValidation(fooService, barService);

        assertThat(validationEntries).hasSize(1);
    }

    private List<ValidationEntry> runValidation(IntermediateModel m1, IntermediateModel m2) {
        ModelValidationContext ctx = ModelValidationContext.builder()
            .intermediateModel(m1)
            .shareModelsTarget(m2)
            .build();

        return validator.validateModels(ctx);
    }
}
