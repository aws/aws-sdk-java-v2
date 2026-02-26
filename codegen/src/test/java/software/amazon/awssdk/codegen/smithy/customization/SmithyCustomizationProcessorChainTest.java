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

package software.amazon.awssdk.codegen.smithy.customization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StringShape;

/**
 * Tests for {@link SmithyCustomizationProcessorChain}.
 *
 * <p><b>Property 2: Chain Threading and Ordering</b> — verify processors execute in order,
 * each receives the previous processor's output Model, and ServiceShape is re-resolved
 * after each step.
 * <p><b>Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.5</b>
 */
class SmithyCustomizationProcessorChainTest {

    private static final ShapeId SERVICE_ID = ShapeId.from("com.example#TestService");

    private ServiceShape service;
    private Model model;

    @BeforeEach
    void setUp() {
        service = ServiceShape.builder()
                              .id(SERVICE_ID)
                              .version("2024-01-01")
                              .build();
        model = Model.builder()
                     .addShape(service)
                     .build();
    }

    /**
     * Empty chain: preprocess returns input model unchanged.
     * Validates: Requirement 2.5
     */
    @Test
    void preprocess_emptyChain_returnsInputModelUnchanged() {
        SmithyCustomizationProcessorChain chain = new SmithyCustomizationProcessorChain();

        Model result = chain.preprocess(model, service);

        assertThat(result).isSameAs(model);
    }

    /**
     * Empty chain: postprocess is a no-op (no exceptions thrown).
     * Validates: Requirement 2.5
     */
    @Test
    void postprocess_emptyChain_isNoOp() {
        SmithyCustomizationProcessorChain chain = new SmithyCustomizationProcessorChain();
        IntermediateModel intermediateModel = new IntermediateModel();

        // Should not throw
        chain.postprocess(intermediateModel);
    }

    /**
     * Null constructor argument: treated as empty chain.
     * Validates: Requirement 2.5
     */
    @Test
    void preprocess_nullProcessors_returnsInputModelUnchanged() {
        SmithyCustomizationProcessorChain chain = new SmithyCustomizationProcessorChain(
            (SmithyCustomizationProcessor[]) null);

        Model result = chain.preprocess(model, service);

        assertThat(result).isSameAs(model);
    }

    /**
     * Single processor: preprocess delegates to the single processor.
     * Validates: Requirements 2.1, 2.2
     */
    @Test
    void preprocess_singleProcessor_delegatesToProcessor() {
        Model transformedModel = Model.builder()
                                      .addShape(service)
                                      .addShape(StringShape.builder()
                                                           .id("com.example#ExtraShape")
                                                           .build())
                                      .build();

        SmithyCustomizationProcessor processor = mock(SmithyCustomizationProcessor.class);
        when(processor.preprocess(eq(model), eq(service))).thenReturn(transformedModel);

        SmithyCustomizationProcessorChain chain = new SmithyCustomizationProcessorChain(processor);
        Model result = chain.preprocess(model, service);

        assertThat(result).isSameAs(transformedModel);
        verify(processor).preprocess(model, service);
    }

    /**
     * Single processor: postprocess delegates to the single processor.
     * Validates: Requirement 2.4
     */
    @Test
    void postprocess_singleProcessor_delegatesToProcessor() {
        SmithyCustomizationProcessor processor = mock(SmithyCustomizationProcessor.class);
        IntermediateModel intermediateModel = new IntermediateModel();

        SmithyCustomizationProcessorChain chain = new SmithyCustomizationProcessorChain(processor);
        chain.postprocess(intermediateModel);

        verify(processor).postprocess(intermediateModel);
    }

    /**
     * Multiple processors: preprocess threads model through each in order,
     * each gets previous output.
     * Validates: Requirements 2.1, 2.2
     */
    @Test
    void preprocess_multipleProcessors_threadsModelInOrder() {
        Model model1 = Model.builder()
                            .addShape(service)
                            .addShape(StringShape.builder().id("com.example#Shape1").build())
                            .build();
        Model model2 = Model.builder()
                            .addShape(service)
                            .addShape(StringShape.builder().id("com.example#Shape2").build())
                            .build();
        Model model3 = Model.builder()
                            .addShape(service)
                            .addShape(StringShape.builder().id("com.example#Shape3").build())
                            .build();

        SmithyCustomizationProcessor p1 = mock(SmithyCustomizationProcessor.class, "processor1");
        SmithyCustomizationProcessor p2 = mock(SmithyCustomizationProcessor.class, "processor2");
        SmithyCustomizationProcessor p3 = mock(SmithyCustomizationProcessor.class, "processor3");

        when(p1.preprocess(any(Model.class), any(ServiceShape.class))).thenReturn(model1);
        when(p2.preprocess(any(Model.class), any(ServiceShape.class))).thenReturn(model2);
        when(p3.preprocess(any(Model.class), any(ServiceShape.class))).thenReturn(model3);

        SmithyCustomizationProcessorChain chain = new SmithyCustomizationProcessorChain(p1, p2, p3);
        Model result = chain.preprocess(model, service);

        assertThat(result).isSameAs(model3);

        // Verify ordering and that each processor received the previous processor's output
        InOrder inOrder = inOrder(p1, p2, p3);
        inOrder.verify(p1).preprocess(eq(model), any(ServiceShape.class));
        inOrder.verify(p2).preprocess(eq(model1), any(ServiceShape.class));
        inOrder.verify(p3).preprocess(eq(model2), any(ServiceShape.class));
    }

    /**
     * ServiceShape re-resolution: after a processor transforms the model,
     * the next processor gets a ServiceShape re-resolved from the new model.
     * Validates: Requirement 2.3
     */
    @Test
    void preprocess_multipleProcessors_reResolvesServiceShapeAfterEachStep() {
        // First processor returns a new model that still contains the service.
        // The chain should re-resolve the ServiceShape from the new model,
        // meaning the second processor gets a ServiceShape instance from model1,
        // not the original service reference.
        Model model1 = Model.builder()
                            .addShape(service)
                            .addShape(StringShape.builder().id("com.example#Added").build())
                            .build();

        // Capture the ServiceShape passed to the second processor
        ArgumentCaptor<ServiceShape> serviceCaptor = ArgumentCaptor.forClass(ServiceShape.class);

        SmithyCustomizationProcessor p1 = mock(SmithyCustomizationProcessor.class, "processor1");
        SmithyCustomizationProcessor p2 = mock(SmithyCustomizationProcessor.class, "processor2");

        when(p1.preprocess(any(Model.class), any(ServiceShape.class))).thenReturn(model1);
        when(p2.preprocess(any(Model.class), serviceCaptor.capture())).thenReturn(model1);

        SmithyCustomizationProcessorChain chain = new SmithyCustomizationProcessorChain(p1, p2);
        chain.preprocess(model, service);

        // The ServiceShape passed to p2 should be the one from model1 (re-resolved),
        // not the original service reference. It should have the same ID.
        ServiceShape reResolvedService = serviceCaptor.getValue();
        assertThat(reResolvedService.getId()).isEqualTo(SERVICE_ID);
        // The re-resolved service should be the instance from model1
        assertThat(reResolvedService).isSameAs(model1.expectShape(SERVICE_ID, ServiceShape.class));
    }

    /**
     * Postprocess ordering: postprocess calls each processor in order.
     * Validates: Requirement 2.4
     */
    @Test
    void postprocess_multipleProcessors_callsInOrder() {
        SmithyCustomizationProcessor p1 = mock(SmithyCustomizationProcessor.class, "processor1");
        SmithyCustomizationProcessor p2 = mock(SmithyCustomizationProcessor.class, "processor2");
        SmithyCustomizationProcessor p3 = mock(SmithyCustomizationProcessor.class, "processor3");
        IntermediateModel intermediateModel = new IntermediateModel();

        SmithyCustomizationProcessorChain chain = new SmithyCustomizationProcessorChain(p1, p2, p3);
        chain.postprocess(intermediateModel);

        InOrder inOrder = inOrder(p1, p2, p3);
        inOrder.verify(p1).postprocess(intermediateModel);
        inOrder.verify(p2).postprocess(intermediateModel);
        inOrder.verify(p3).postprocess(intermediateModel);
    }
}
