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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.validation.ModelInvalidException;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;

/**
 * Tests for {@link LowercaseShapeValidatorProcessor}.
 *
 * <p><b>Property 19: Lowercase Shape Validation</b> — verify lowercase structure shape name
 * throws {@link ModelInvalidException}, all-uppercase model returns unchanged.
 * <p><b>Validates: Requirements 20.1, 20.2, 31.4</b>
 */
class LowercaseShapeValidatorProcessorTest {

    private static final String NAMESPACE = "com.example.testservice";
    private static final ShapeId SERVICE_ID = ShapeId.from(NAMESPACE + "#TestService");

    private LowercaseShapeValidatorProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new LowercaseShapeValidatorProcessor();
    }

    /**
     * All structure shapes start with uppercase → returns model unchanged (isSameAs).
     * Validates: Requirement 20.2
     */
    @Test
    void preprocess_when_allStructureShapesUppercase_returnsModelUnchanged() {
        ShapeId inputId = ShapeId.from(NAMESPACE + "#GetItemInput");
        ShapeId outputId = ShapeId.from(NAMESPACE + "#GetItemOutput");
        ShapeId opId = ShapeId.from(NAMESPACE + "#GetItem");

        StructureShape input = StructureShape.builder().id(inputId).build();
        StructureShape output = StructureShape.builder().id(outputId).build();
        OperationShape operation = OperationShape.builder()
                                                 .id(opId)
                                                 .input(inputId)
                                                 .output(outputId)
                                                 .build();
        ServiceShape service = ServiceShape.builder()
                                           .id(SERVICE_ID)
                                           .version("2024-01-01")
                                           .addOperation(opId)
                                           .build();
        Model model = Model.builder()
                           .addShapes(service, operation, input, output)
                           .build();
        ServiceShape resolvedService = model.expectShape(SERVICE_ID, ServiceShape.class);

        Model result = processor.preprocess(model, resolvedService);

        assertThat(result).isSameAs(model);
    }

    /**
     * A structure shape starting with lowercase → throws ModelInvalidException with shape name in message.
     * Validates: Requirements 20.1, 31.4
     */
    @Test
    void preprocess_when_lowercaseStructureShape_throwsModelInvalidException() {
        ShapeId lowercaseId = ShapeId.from(NAMESPACE + "#lowercaseShape");
        ShapeId opId = ShapeId.from(NAMESPACE + "#GetItem");

        StructureShape lowercaseStruct = StructureShape.builder().id(lowercaseId).build();
        OperationShape operation = OperationShape.builder()
                                                 .id(opId)
                                                 .input(lowercaseId)
                                                 .build();
        ServiceShape service = ServiceShape.builder()
                                           .id(SERVICE_ID)
                                           .version("2024-01-01")
                                           .addOperation(opId)
                                           .build();
        Model model = Model.builder()
                           .addShapes(service, operation, lowercaseStruct)
                           .build();
        ServiceShape resolvedService = model.expectShape(SERVICE_ID, ServiceShape.class);

        assertThatThrownBy(() -> processor.preprocess(model, resolvedService))
            .isInstanceOf(ModelInvalidException.class)
            .hasMessageContaining("lowercaseShape");
    }

    /**
     * Non-structure shapes starting with lowercase (e.g., string shapes) in the service closure
     * → returns model unchanged. Only structure shapes are validated.
     * Validates: Requirement 20.1 (only structures are checked)
     */
    @Test
    void preprocess_when_lowercaseNonStructureShapeInClosure_returnsModelUnchanged() {
        ShapeId stringId = ShapeId.from(NAMESPACE + "#lowercaseString");
        ShapeId inputId = ShapeId.from(NAMESPACE + "#GetItemInput");
        ShapeId memberId = ShapeId.from(NAMESPACE + "#GetItemInput$field");
        ShapeId opId = ShapeId.from(NAMESPACE + "#GetItem");

        StringShape lowercaseString = StringShape.builder().id(stringId).build();
        MemberShape member = MemberShape.builder().id(memberId).target(stringId).build();
        StructureShape input = StructureShape.builder().id(inputId).addMember(member).build();
        OperationShape operation = OperationShape.builder()
                                                 .id(opId)
                                                 .input(inputId)
                                                 .build();
        ServiceShape service = ServiceShape.builder()
                                           .id(SERVICE_ID)
                                           .version("2024-01-01")
                                           .addOperation(opId)
                                           .build();
        Model model = Model.builder()
                           .addShapes(service, operation, input, lowercaseString)
                           .build();
        ServiceShape resolvedService = model.expectShape(SERVICE_ID, ServiceShape.class);

        Model result = processor.preprocess(model, resolvedService);

        assertThat(result).isSameAs(model);
    }

    /**
     * Service with no structure shapes in its closure → returns model unchanged.
     * Validates: Requirement 20.2
     */
    @Test
    void preprocess_when_noStructureShapesInServiceClosure_returnsModelUnchanged() {
        ShapeId opId = ShapeId.from(NAMESPACE + "#SimpleOp");

        OperationShape operation = OperationShape.builder().id(opId).build();
        ServiceShape service = ServiceShape.builder()
                                           .id(SERVICE_ID)
                                           .version("2024-01-01")
                                           .addOperation(opId)
                                           .build();
        Model model = Model.builder()
                           .addShapes(service, operation)
                           .build();
        ServiceShape resolvedService = model.expectShape(SERVICE_ID, ServiceShape.class);

        Model result = processor.preprocess(model, resolvedService);

        assertThat(result).isSameAs(model);
    }

    /**
     * Postprocess is a no-op.
     */
    @Test
    void postprocess_isNoOp() {
        processor.postprocess(null);
    }
}
