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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeType;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Tests for {@link RemoveExceptionMessageProcessor}.
 *
 * <p><b>Property 21: Exception Message Removal Idempotence</b> — verify message member
 * removed from exception shapes, and second application produces no further changes.
 * <p><b>Validates: Requirements 14.2</b>
 */
class RemoveExceptionMessageProcessorTest {

    private RemoveExceptionMessageProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new RemoveExceptionMessageProcessor();
    }

    /**
     * Preprocess returns input model unchanged (postprocess-only processor).
     * Validates: Requirement 14.1
     */
    @Test
    void preprocess_returnsInputModelUnchanged() {
        ServiceShape service = ServiceShape.builder()
                                           .id(ShapeId.from("com.example#TestService"))
                                           .version("2024-01-01")
                                           .build();
        Model model = Model.builder().addShape(service).build();

        Model result = processor.preprocess(model, service);

        assertThat(result).isSameAs(model);
    }

    /**
     * Property 21: Exception Message Removal Idempotence.
     * Postprocess removes "message" member from exception shapes.
     * Validates: Requirement 14.2
     */
    @Test
    void postprocess_when_exceptionShapeHasMessageMember_removesIt() {
        ShapeModel exceptionShape = createExceptionShape("TestException", "message", "code");
        IntermediateModel intermediateModel = createIntermediateModel("TestException", exceptionShape);

        processor.postprocess(intermediateModel);

        assertThat(exceptionShape.getMemberByC2jName("message")).isNull();
        assertThat(exceptionShape.getMemberByC2jName("code")).isNotNull();
    }

    /**
     * Property 21: Exception Message Removal Idempotence.
     * Applying postprocess a second time produces no further changes.
     * Validates: Requirement 14.2
     */
    @Test
    void postprocess_when_appliedTwice_isIdempotent() {
        ShapeModel exceptionShape = createExceptionShape("TestException", "message", "code");
        IntermediateModel intermediateModel = createIntermediateModel("TestException", exceptionShape);

        processor.postprocess(intermediateModel);
        int membersAfterFirst = exceptionShape.getMembers().size();

        processor.postprocess(intermediateModel);
        int membersAfterSecond = exceptionShape.getMembers().size();

        assertThat(membersAfterSecond).isEqualTo(membersAfterFirst);
        assertThat(exceptionShape.getMemberByC2jName("message")).isNull();
    }

    /**
     * Case-insensitive removal: "Message" (uppercase M) should also be removed.
     * Validates: Requirement 14.2
     */
    @Test
    void postprocess_when_exceptionShapeHasUppercaseMessageMember_removesIt() {
        ShapeModel exceptionShape = createExceptionShape("TestException", "Message", "code");
        IntermediateModel intermediateModel = createIntermediateModel("TestException", exceptionShape);

        processor.postprocess(intermediateModel);

        assertThat(exceptionShape.getMemberByC2jName("Message")).isNull();
        assertThat(exceptionShape.getMemberByC2jName("code")).isNotNull();
    }

    /**
     * Non-exception shapes are not affected by postprocess.
     * Validates: Requirement 14.2
     */
    @Test
    void postprocess_when_nonExceptionShapeHasMessageMember_doesNotRemoveIt() {
        ShapeModel modelShape = createModelShape("SomeModel", "message", "data");
        IntermediateModel intermediateModel = createIntermediateModel("SomeModel", modelShape);

        processor.postprocess(intermediateModel);

        assertThat(modelShape.getMemberByC2jName("message")).isNotNull();
        assertThat(modelShape.getMemberByC2jName("data")).isNotNull();
    }

    /**
     * Exception shape without a message member is unaffected.
     * Validates: Requirement 14.2
     */
    @Test
    void postprocess_when_exceptionShapeHasNoMessageMember_isNoOp() {
        ShapeModel exceptionShape = createExceptionShape("TestException", "code", "detail");
        IntermediateModel intermediateModel = createIntermediateModel("TestException", exceptionShape);

        int membersBefore = exceptionShape.getMembers().size();
        processor.postprocess(intermediateModel);
        int membersAfter = exceptionShape.getMembers().size();

        assertThat(membersAfter).isEqualTo(membersBefore);
    }

    /**
     * Multiple exception shapes: message removed from all of them.
     * Validates: Requirement 14.2
     */
    @Test
    void postprocess_when_multipleExceptionShapes_removesMessageFromAll() {
        ShapeModel exception1 = createExceptionShape("Exception1", "message", "code");
        ShapeModel exception2 = createExceptionShape("Exception2", "Message", "detail");
        Map<String, ShapeModel> shapes = new HashMap<>();
        shapes.put("Exception1", exception1);
        shapes.put("Exception2", exception2);
        IntermediateModel intermediateModel = new IntermediateModel();
        intermediateModel.setShapes(shapes);

        processor.postprocess(intermediateModel);

        assertThat(exception1.getMemberByC2jName("message")).isNull();
        assertThat(exception1.getMemberByC2jName("code")).isNotNull();
        assertThat(exception2.getMemberByC2jName("Message")).isNull();
        assertThat(exception2.getMemberByC2jName("detail")).isNotNull();
    }

    private static ShapeModel createExceptionShape(String name, String... memberC2jNames) {
        ShapeModel shape = new ShapeModel(name);
        shape.setShapeName(name);
        shape.setType(ShapeType.Exception);
        shape.setMembers(createMembers(memberC2jNames));
        return shape;
    }

    private static ShapeModel createModelShape(String name, String... memberC2jNames) {
        ShapeModel shape = new ShapeModel(name);
        shape.setShapeName(name);
        shape.setType(ShapeType.Model);
        shape.setMembers(createMembers(memberC2jNames));
        return shape;
    }

    private static List<MemberModel> createMembers(String... c2jNames) {
        List<MemberModel> members = new ArrayList<>();
        for (String c2jName : c2jNames) {
            MemberModel member = new MemberModel();
            member.setC2jName(c2jName);
            member.setName(c2jName);
            members.add(member);
        }
        return members;
    }

    private static IntermediateModel createIntermediateModel(String shapeName, ShapeModel shape) {
        Map<String, ShapeModel> shapes = new HashMap<>();
        shapes.put(shapeName, shape);
        IntermediateModel model = new IntermediateModel();
        model.setShapes(shapes);
        return model;
    }
}
