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
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Tests for {@link AbstractDualConfigProcessor}.
 *
 * <p><b>Property 4: Dual-Config Mutual Exclusion</b> — verify both configs set throws
 * {@code IllegalStateException} with both field names in message.
 * <p><b>Validates: Requirements 5.1, 31.3</b>
 *
 * <p><b>Property 5: Dual-Config Deprecation Path</b> — verify only old config logs warning
 * and converts to new form.
 * <p><b>Validates: Requirements 5.2, 5.3</b>
 *
 * <p><b>Property 6: Dual-Config New-Only Path</b> — verify only new config applies directly
 * without warning.
 * <p><b>Validates: Requirement 5.4</b>
 *
 * <p>Test neither config set returns input model unchanged.
 * <p><b>Validates: Requirement 5.5</b>
 */
class AbstractDualConfigProcessorTest {

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
     * Property 4: Dual-Config Mutual Exclusion.
     * Both old and new configs set throws IllegalStateException with both field names.
     * Validates: Requirements 5.1, 31.3
     */
    @Test
    void preprocess_bothConfigsSet_throwsIllegalStateExceptionWithBothFieldNames() {
        TestProcessor processor = new TestProcessor("oldValue", "newValue");

        assertThatThrownBy(() -> processor.preprocess(model, service))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("oldField")
            .hasMessageContaining("smithyNewField");
    }

    /**
     * Property 5: Dual-Config Deprecation Path.
     * Only old config set converts to new form and calls applySmithyLogic with converted value.
     * The deprecation warning is logged internally (LogCaptor not available in this module).
     * Validates: Requirements 5.2, 5.3
     */
    @Test
    void preprocess_onlyOldConfigSet_convertsToNewFormAndAppliesSmithyLogic() {
        TestProcessor processor = new TestProcessor("oldValue", null);

        Model result = processor.preprocess(model, service);

        assertThat(result).isSameAs(model);
        assertThat(processor.applySmithyLogicCalled).isTrue();
        assertThat(processor.appliedConfig).isEqualTo("converted:oldValue");
    }

    /**
     * Property 6: Dual-Config New-Only Path.
     * Only new config set applies directly without conversion.
     * Validates: Requirement 5.4
     */
    @Test
    void preprocess_onlyNewConfigSet_appliesDirectlyWithoutConversion() {
        TestProcessor processor = new TestProcessor(null, "newValue");

        Model result = processor.preprocess(model, service);

        assertThat(result).isSameAs(model);
        assertThat(processor.applySmithyLogicCalled).isTrue();
        assertThat(processor.appliedConfig).isEqualTo("newValue");
    }

    /**
     * Neither config set returns input model unchanged and applySmithyLogic is not called.
     * Validates: Requirement 5.5
     */
    @Test
    void preprocess_neitherConfigSet_returnsInputModelUnchanged() {
        TestProcessor processor = new TestProcessor(null, null);

        Model result = processor.preprocess(model, service);

        assertThat(result).isSameAs(model);
        assertThat(processor.applySmithyLogicCalled).isFalse();
    }

    /**
     * Empty strings treated as unset by the test processor's isSet logic.
     * Validates: Requirement 5.5
     */
    @Test
    void preprocess_emptyStrings_returnsInputModelUnchanged() {
        TestProcessor processor = new TestProcessor("", "");

        Model result = processor.preprocess(model, service);

        assertThat(result).isSameAs(model);
        assertThat(processor.applySmithyLogicCalled).isFalse();
    }

    /**
     * Default postprocess is a no-op (does not throw).
     * Validates: Requirement 5.6
     */
    @Test
    void postprocess_default_isNoOp() {
        TestProcessor processor = new TestProcessor(null, null);
        IntermediateModel intermediateModel = new IntermediateModel();

        // Should not throw
        processor.postprocess(intermediateModel);
    }

    /**
     * Concrete test subclass of AbstractDualConfigProcessor for testing the base class behavior.
     */
    private static class TestProcessor extends AbstractDualConfigProcessor<String, String> {
        private boolean applySmithyLogicCalled = false;
        private String appliedConfig;

        TestProcessor(String oldConfig, String newConfig) {
            super(oldConfig, newConfig, "oldField", "smithyNewField");
        }

        @Override
        protected boolean isSet(Object config) {
            return config != null && !((String) config).isEmpty();
        }

        @Override
        protected String convertOldToNew(String oldConfig, Model model, ServiceShape service) {
            return "converted:" + oldConfig;
        }

        @Override
        protected Model applySmithyLogic(Model model, ServiceShape service, String config) {
            applySmithyLogicCalled = true;
            appliedConfig = config;
            return model;
        }
    }
}
