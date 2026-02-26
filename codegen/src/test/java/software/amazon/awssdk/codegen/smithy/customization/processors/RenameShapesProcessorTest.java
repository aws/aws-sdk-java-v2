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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;

/**
 * Tests for {@link RenameShapesProcessor}.
 *
 * <p><b>Property 7: Rename Bijectivity</b> — verify renamed shapes exist at new names,
 * old names removed, all references updated.
 * <p><b>Validates: Requirements 7.1, 7.2, 7.3, 7.4, 7.5, 31.2</b>
 */
class RenameShapesProcessorTest {

    private static final String NAMESPACE = "com.example.testservice";
    private static final ShapeId SERVICE_ID = ShapeId.from(NAMESPACE + "#TestService");

    private ServiceShape service;

    @BeforeEach
    void setUp() {
        service = ServiceShape.builder()
                              .id(SERVICE_ID)
                              .version("2024-01-01")
                              .build();
    }

    /**
     * Null config returns model unchanged.
     * Validates: Requirement 7.5
     */
    @Test
    void preprocess_when_nullConfig_returnsModelUnchanged() {
        Model model = Model.builder().addShape(service).build();
        RenameShapesProcessor processor = new RenameShapesProcessor(null);

        Model result = processor.preprocess(model, service);

        assertThat(result).isSameAs(model);
    }

    /**
     * Empty config returns model unchanged.
     * Validates: Requirement 7.5
     */
    @Test
    void preprocess_when_emptyConfig_returnsModelUnchanged() {
        Model model = Model.builder().addShape(service).build();
        RenameShapesProcessor processor = new RenameShapesProcessor(Collections.emptyMap());

        Model result = processor.preprocess(model, service);

        assertThat(result).isSameAs(model);
    }

    /**
     * Property 7: Rename Bijectivity — single rename: shape exists at new name,
     * old name gone.
     * Validates: Requirement 7.1
     */
    @Test
    void preprocess_when_singleRename_shapeExistsAtNewName() {
        StringShape originalShape = StringShape.builder()
                                               .id(ShapeId.from(NAMESPACE + "#OldName"))
                                               .build();
        Model model = Model.builder()
                           .addShape(service)
                           .addShape(originalShape)
                           .build();

        Map<String, String> renameConfig = Collections.singletonMap("OldName", "NewName");
        RenameShapesProcessor processor = new RenameShapesProcessor(renameConfig);

        Model result = processor.preprocess(model, service);

        assertThat(result.getShape(ShapeId.from(NAMESPACE + "#NewName"))).isPresent();
        assertThat(result.getShape(ShapeId.from(NAMESPACE + "#OldName"))).isNotPresent();
    }

    /**
     * Property 7: Rename Bijectivity — references to renamed shape are updated.
     * A structure member targeting the old shape should now target the new shape.
     * Validates: Requirement 7.2
     */
    @Test
    void preprocess_when_singleRename_referencesUpdated() {
        StringShape targetShape = StringShape.builder()
                                             .id(ShapeId.from(NAMESPACE + "#OldTarget"))
                                             .build();
        MemberShape member = MemberShape.builder()
                                        .id(ShapeId.from(NAMESPACE + "#MyStruct$myField"))
                                        .target(targetShape.getId())
                                        .build();
        StructureShape struct = StructureShape.builder()
                                              .id(ShapeId.from(NAMESPACE + "#MyStruct"))
                                              .addMember(member)
                                              .build();
        Model model = Model.builder()
                           .addShape(service)
                           .addShape(targetShape)
                           .addShape(struct)
                           .build();

        Map<String, String> renameConfig = Collections.singletonMap("OldTarget", "NewTarget");
        RenameShapesProcessor processor = new RenameShapesProcessor(renameConfig);

        Model result = processor.preprocess(model, service);

        StructureShape resultStruct = result.expectShape(ShapeId.from(NAMESPACE + "#MyStruct"),
                                                         StructureShape.class);
        MemberShape resultMember = resultStruct.getMember("myField").get();
        assertThat(resultMember.getTarget()).isEqualTo(ShapeId.from(NAMESPACE + "#NewTarget"));
    }

    /**
     * Collision detection: new name already exists → throws IllegalStateException.
     * Validates: Requirements 7.3, 31.2
     */
    @Test
    void preprocess_when_newNameCollides_throwsIllegalStateException() {
        StringShape shapeA = StringShape.builder()
                                        .id(ShapeId.from(NAMESPACE + "#ShapeA"))
                                        .build();
        StringShape shapeB = StringShape.builder()
                                        .id(ShapeId.from(NAMESPACE + "#ShapeB"))
                                        .build();
        Model model = Model.builder()
                           .addShape(service)
                           .addShape(shapeA)
                           .addShape(shapeB)
                           .build();

        Map<String, String> renameConfig = Collections.singletonMap("ShapeA", "ShapeB");
        RenameShapesProcessor processor = new RenameShapesProcessor(renameConfig);

        assertThatThrownBy(() -> processor.preprocess(model, service))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("ShapeA")
            .hasMessageContaining("ShapeB")
            .hasMessageContaining(NAMESPACE);
    }

    /**
     * Missing shape: old name doesn't exist → throws IllegalStateException from ShapeIdResolver.
     * Validates: Requirement 7.4
     */
    @Test
    void preprocess_when_oldNameMissing_throwsIllegalStateException() {
        Model model = Model.builder().addShape(service).build();

        Map<String, String> renameConfig = Collections.singletonMap("NonExistent", "NewName");
        RenameShapesProcessor processor = new RenameShapesProcessor(renameConfig);

        assertThatThrownBy(() -> processor.preprocess(model, service))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("NonExistent")
            .hasMessageContaining(NAMESPACE);
    }

    /**
     * Multiple renames: all shapes renamed correctly.
     * Validates: Requirements 7.1, 7.2
     */
    @Test
    void preprocess_when_multipleRenames_allShapesRenamed() {
        StringShape shape1 = StringShape.builder()
                                        .id(ShapeId.from(NAMESPACE + "#Alpha"))
                                        .build();
        StringShape shape2 = StringShape.builder()
                                        .id(ShapeId.from(NAMESPACE + "#Beta"))
                                        .build();
        Model model = Model.builder()
                           .addShape(service)
                           .addShape(shape1)
                           .addShape(shape2)
                           .build();

        Map<String, String> renameConfig = new HashMap<>();
        renameConfig.put("Alpha", "AlphaRenamed");
        renameConfig.put("Beta", "BetaRenamed");
        RenameShapesProcessor processor = new RenameShapesProcessor(renameConfig);

        Model result = processor.preprocess(model, service);

        assertThat(result.getShape(ShapeId.from(NAMESPACE + "#AlphaRenamed"))).isPresent();
        assertThat(result.getShape(ShapeId.from(NAMESPACE + "#BetaRenamed"))).isPresent();
        assertThat(result.getShape(ShapeId.from(NAMESPACE + "#Alpha"))).isNotPresent();
        assertThat(result.getShape(ShapeId.from(NAMESPACE + "#Beta"))).isNotPresent();
    }

    /**
     * Postprocess is a no-op.
     */
    @Test
    void postprocess_isNoOp() {
        RenameShapesProcessor processor = new RenameShapesProcessor(null);
        // Should not throw — just verifying it's callable without error
        processor.postprocess(null);
    }
}
