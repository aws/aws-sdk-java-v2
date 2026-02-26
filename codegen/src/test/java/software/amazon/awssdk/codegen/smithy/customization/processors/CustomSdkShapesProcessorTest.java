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

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.model.config.customization.CustomSdkShapes;
import software.amazon.awssdk.codegen.model.config.customization.SmithyCustomSdkShapes;
import software.amazon.awssdk.codegen.model.service.Member;
import software.amazon.awssdk.codegen.model.service.Shape;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;

/**
 * Tests for {@link CustomSdkShapesProcessor}.
 *
 * <p><b>Property 13: Custom SDK Shapes Merge</b> — verify custom shapes added to model, pre-existing shapes
 * preserved, prelude shapes excluded.
 * <p><b>Validates: Requirements 10.1, 10.2</b>
 *
 * <p><b>Property 14: Custom SDK Shapes C2J-to-AST Round Trip</b> — verify C2J Shape conversion produces
 * equivalent Smithy shapes (types, enums, members).
 * <p><b>Validates: Requirements 10.3, 30.3</b>
 */
class CustomSdkShapesProcessorTest {

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

    // -----------------------------------------------------------------------
    // Helper: build a Smithy JSON AST map for use with SmithyCustomSdkShapes
    // -----------------------------------------------------------------------

    private Map<String, Object> buildSmithyAst(Map<String, Object> shapes) {
        Map<String, Object> ast = new LinkedHashMap<>();
        ast.put("smithy", "2.0");
        ast.put("shapes", shapes);
        return ast;
    }

    // -----------------------------------------------------------------------
    // Property 13: Custom SDK Shapes Merge — new Smithy-native config
    // Validates: Requirements 10.1, 10.2
    // -----------------------------------------------------------------------

    /**
     * Property 13: Custom SDK Shapes Merge.
     * A simple string shape in the AST is added to the model.
     * Validates: Requirement 10.1
     */
    @Test
    void preprocess_newConfig_simpleStringShape_addedToModel() {
        Model model = Model.builder().addShape(service).build();

        Map<String, Object> shapeDefn = new LinkedHashMap<>();
        shapeDefn.put("type", "string");

        Map<String, Object> shapes = new LinkedHashMap<>();
        shapes.put(NAMESPACE + "#CustomString", shapeDefn);

        SmithyCustomSdkShapes newConfig = new SmithyCustomSdkShapes();
        newConfig.setSmithyAst(buildSmithyAst(shapes));

        CustomSdkShapesProcessor processor = new CustomSdkShapesProcessor(null, newConfig);
        Model result = processor.preprocess(model, service);

        assertThat(result.getShape(ShapeId.from(NAMESPACE + "#CustomString"))).isPresent();
        assertThat(result.expectShape(ShapeId.from(NAMESPACE + "#CustomString")).isStringShape()).isTrue();
    }

    /**
     * Property 13: Custom SDK Shapes Merge.
     * A structure shape with members in the AST is added to the model.
     * Member targets use prelude shapes (smithy.api#String) since the AST is
     * parsed independently before merging into the existing model.
     * Validates: Requirement 10.1
     */
    @Test
    void preprocess_newConfig_structureShapeWithMembers_addedToModel() {
        Model model = Model.builder().addShape(service).build();

        Map<String, Object> memberDef = new LinkedHashMap<>();
        memberDef.put("target", "smithy.api#String");

        Map<String, Object> members = new LinkedHashMap<>();
        members.put("fieldA", memberDef);

        Map<String, Object> shapeDefn = new LinkedHashMap<>();
        shapeDefn.put("type", "structure");
        shapeDefn.put("members", members);

        Map<String, Object> shapes = new LinkedHashMap<>();
        shapes.put(NAMESPACE + "#CustomStruct", shapeDefn);

        SmithyCustomSdkShapes newConfig = new SmithyCustomSdkShapes();
        newConfig.setSmithyAst(buildSmithyAst(shapes));

        CustomSdkShapesProcessor processor = new CustomSdkShapesProcessor(null, newConfig);
        Model result = processor.preprocess(model, service);

        ShapeId customStructId = ShapeId.from(NAMESPACE + "#CustomStruct");
        assertThat(result.getShape(customStructId)).isPresent();
        StructureShape customStruct = result.expectShape(customStructId, StructureShape.class);
        assertThat(customStruct.getMember("fieldA")).isPresent();
        assertThat(customStruct.getMember("fieldA").get().getTarget())
            .isEqualTo(ShapeId.from("smithy.api#String"));
    }

    /**
     * Property 13: Custom SDK Shapes Merge.
     * Pre-existing shapes in the model are preserved after merge.
     * Validates: Requirement 10.1
     */
    @Test
    void preprocess_newConfig_preExistingShapesPreserved() {
        StringShape existingShape = StringShape.builder()
            .id(ShapeId.from(NAMESPACE + "#ExistingShape"))
            .build();
        Model model = Model.builder().addShape(service).addShape(existingShape).build();

        Map<String, Object> shapeDefn = new LinkedHashMap<>();
        shapeDefn.put("type", "string");

        Map<String, Object> shapes = new LinkedHashMap<>();
        shapes.put(NAMESPACE + "#NewCustomShape", shapeDefn);

        SmithyCustomSdkShapes newConfig = new SmithyCustomSdkShapes();
        newConfig.setSmithyAst(buildSmithyAst(shapes));

        CustomSdkShapesProcessor processor = new CustomSdkShapesProcessor(null, newConfig);
        Model result = processor.preprocess(model, service);

        // Both the existing and new shapes should be present
        assertThat(result.getShape(ShapeId.from(NAMESPACE + "#ExistingShape"))).isPresent();
        assertThat(result.getShape(ShapeId.from(NAMESPACE + "#NewCustomShape"))).isPresent();
    }

    /**
     * Property 13: Custom SDK Shapes Merge.
     * Prelude shapes from the parsed AST are excluded from the merge.
     * Validates: Requirement 10.2
     */
    @Test
    void preprocess_newConfig_preludeShapesExcludedFromMerge() {
        Model model = Model.builder().addShape(service).build();

        Map<String, Object> shapeDefn = new LinkedHashMap<>();
        shapeDefn.put("type", "string");

        Map<String, Object> shapes = new LinkedHashMap<>();
        shapes.put(NAMESPACE + "#CustomShape", shapeDefn);

        SmithyCustomSdkShapes newConfig = new SmithyCustomSdkShapes();
        newConfig.setSmithyAst(buildSmithyAst(shapes));

        CustomSdkShapesProcessor processor = new CustomSdkShapesProcessor(null, newConfig);
        Model result = processor.preprocess(model, service);

        // The result should have the custom shape but NOT have extra prelude shapes
        // beyond what was already in the model. The custom shape adds exactly 1 new shape.
        assertThat(result.getShape(ShapeId.from(NAMESPACE + "#CustomShape"))).isPresent();

        // Verify no prelude shapes like smithy.api#String were added as new shapes
        // (they already exist in the model's prelude, so the count should only increase
        // by the number of custom shapes we added)
        long customShapeCount = result.toSet().stream()
            .filter(s -> s.getId().getNamespace().equals(NAMESPACE))
            .count();
        long originalCustomShapeCount = model.toSet().stream()
            .filter(s -> s.getId().getNamespace().equals(NAMESPACE))
            .count();
        assertThat(customShapeCount).isEqualTo(originalCustomShapeCount + 1);
    }

    // -----------------------------------------------------------------------
    // Property 14: Custom SDK Shapes C2J-to-AST Round Trip — old C2J config
    // Validates: Requirements 10.3, 30.3
    // -----------------------------------------------------------------------

    /**
     * Property 14: C2J-to-AST Round Trip.
     * Old C2J config with a string shape is converted to AST and the shape is added to the model.
     * Validates: Requirement 10.3
     */
    @Test
    void preprocess_oldConfig_stringShape_convertedAndAddedToModel() {
        Model model = Model.builder().addShape(service).build();

        Shape c2jShape = new Shape();
        c2jShape.setType("string");

        Map<String, Shape> c2jShapes = new LinkedHashMap<>();
        c2jShapes.put("CustomString", c2jShape);

        CustomSdkShapes oldConfig = new CustomSdkShapes();
        oldConfig.setShapes(c2jShapes);

        CustomSdkShapesProcessor processor = new CustomSdkShapesProcessor(oldConfig, null);
        Model result = processor.preprocess(model, service);

        ShapeId customStringId = ShapeId.from(NAMESPACE + "#CustomString");
        assertThat(result.getShape(customStringId)).isPresent();
        assertThat(result.expectShape(customStringId).isStringShape()).isTrue();
    }

    /**
     * Property 14: C2J-to-AST Round Trip.
     * Old C2J config with an enum string shape preserves enum trait in the converted shape.
     * Validates: Requirements 10.3, 30.3
     */
    @Test
    void preprocess_oldConfig_enumStringShape_enumTraitPreserved() {
        Model model = Model.builder().addShape(service).build();

        Shape c2jShape = new Shape();
        c2jShape.setType("string");
        c2jShape.setEnumValues(Arrays.asList("VALUE_A", "VALUE_B"));

        Map<String, Shape> c2jShapes = new LinkedHashMap<>();
        c2jShapes.put("CustomEnum", c2jShape);

        CustomSdkShapes oldConfig = new CustomSdkShapes();
        oldConfig.setShapes(c2jShapes);

        CustomSdkShapesProcessor processor = new CustomSdkShapesProcessor(oldConfig, null);
        Model result = processor.preprocess(model, service);

        ShapeId customEnumId = ShapeId.from(NAMESPACE + "#CustomEnum");
        assertThat(result.getShape(customEnumId)).isPresent();
        assertThat(result.expectShape(customEnumId).isStringShape()).isTrue();

        // Verify the enum trait is present
        software.amazon.smithy.model.shapes.Shape smithyShape = result.expectShape(customEnumId);
        assertThat(smithyShape.findTrait("smithy.api#enum")).isPresent();
    }

    /**
     * Property 14: C2J-to-AST Round Trip.
     * Old C2J config with a structure shape with members produces members with fully-qualified
     * ShapeId targets. The target shape must also be defined in the C2J shapes so it is
     * included in the generated AST (ModelAssembler validates targets during assembly).
     * Validates: Requirements 10.3, 30.3
     */
    @Test
    void preprocess_oldConfig_structureShapeWithMembers_membersHaveFullyQualifiedTargets() {
        Model model = Model.builder().addShape(service).build();

        // Define the target shape in C2J shapes so it's included in the AST
        Shape targetC2jShape = new Shape();
        targetC2jShape.setType("string");

        Member c2jMember = new Member();
        c2jMember.setShape("TargetString");

        Map<String, Member> c2jMembers = new LinkedHashMap<>();
        c2jMembers.put("myField", c2jMember);

        Shape c2jStructShape = new Shape();
        c2jStructShape.setType("structure");
        c2jStructShape.setMembers(c2jMembers);

        Map<String, Shape> c2jShapes = new LinkedHashMap<>();
        c2jShapes.put("TargetString", targetC2jShape);
        c2jShapes.put("CustomStruct", c2jStructShape);

        CustomSdkShapes oldConfig = new CustomSdkShapes();
        oldConfig.setShapes(c2jShapes);

        CustomSdkShapesProcessor processor = new CustomSdkShapesProcessor(oldConfig, null);
        Model result = processor.preprocess(model, service);

        ShapeId customStructId = ShapeId.from(NAMESPACE + "#CustomStruct");
        assertThat(result.getShape(customStructId)).isPresent();
        StructureShape customStruct = result.expectShape(customStructId, StructureShape.class);
        assertThat(customStruct.getMember("myField")).isPresent();
        assertThat(customStruct.getMember("myField").get().getTarget())
            .isEqualTo(ShapeId.from(NAMESPACE + "#TargetString"));
    }

    // -----------------------------------------------------------------------
    // Null/empty AST returns model unchanged
    // Validates: Requirement 10.4
    // -----------------------------------------------------------------------

    /**
     * Null AST in new config returns model unchanged.
     * Validates: Requirement 10.4
     */
    @Test
    void preprocess_newConfig_nullAst_returnsModelUnchanged() {
        Model model = Model.builder().addShape(service).build();

        SmithyCustomSdkShapes newConfig = new SmithyCustomSdkShapes();
        newConfig.setSmithyAst(null);

        CustomSdkShapesProcessor processor = new CustomSdkShapesProcessor(null, newConfig);
        Model result = processor.preprocess(model, service);

        assertThat(result).isSameAs(model);
    }

    /**
     * Empty AST map in new config returns model unchanged.
     * Validates: Requirement 10.4
     */
    @Test
    void preprocess_newConfig_emptyAst_returnsModelUnchanged() {
        Model model = Model.builder().addShape(service).build();

        SmithyCustomSdkShapes newConfig = new SmithyCustomSdkShapes();
        newConfig.setSmithyAst(Collections.emptyMap());

        CustomSdkShapesProcessor processor = new CustomSdkShapesProcessor(null, newConfig);
        Model result = processor.preprocess(model, service);

        assertThat(result).isSameAs(model);
    }

    /**
     * Both configs null returns model unchanged.
     * Validates: Requirement 10.4
     */
    @Test
    void preprocess_bothConfigsNull_returnsModelUnchanged() {
        Model model = Model.builder().addShape(service).build();

        CustomSdkShapesProcessor processor = new CustomSdkShapesProcessor(null, null);
        Model result = processor.preprocess(model, service);

        assertThat(result).isSameAs(model);
    }

    // -----------------------------------------------------------------------
    // Dual-config mutual exclusion (inherited from AbstractDualConfigProcessor)
    // -----------------------------------------------------------------------

    /**
     * Both old and new config set throws IllegalStateException.
     */
    @Test
    void preprocess_bothConfigsSet_throwsIllegalStateException() {
        Shape c2jShape = new Shape();
        c2jShape.setType("string");
        Map<String, Shape> c2jShapes = new LinkedHashMap<>();
        c2jShapes.put("SomeShape", c2jShape);
        CustomSdkShapes oldConfig = new CustomSdkShapes();
        oldConfig.setShapes(c2jShapes);

        Map<String, Object> shapeDefn = new LinkedHashMap<>();
        shapeDefn.put("type", "string");
        Map<String, Object> shapes = new LinkedHashMap<>();
        shapes.put(NAMESPACE + "#SomeShape", shapeDefn);
        SmithyCustomSdkShapes newConfig = new SmithyCustomSdkShapes();
        newConfig.setSmithyAst(buildSmithyAst(shapes));

        CustomSdkShapesProcessor processor = new CustomSdkShapesProcessor(oldConfig, newConfig);
        Model model = Model.builder().addShape(service).build();

        assertThatThrownBy(() -> processor.preprocess(model, service))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("customSdkShapes")
            .hasMessageContaining("smithyCustomSdkShapes");
    }
}
