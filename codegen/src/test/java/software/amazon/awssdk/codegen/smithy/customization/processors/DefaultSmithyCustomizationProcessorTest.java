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

import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.smithy.customization.SmithyCustomizationProcessor;
import software.amazon.awssdk.codegen.smithy.customization.SmithyCustomizationProcessorChain;

/**
 * Tests for {@link DefaultSmithyCustomizationProcessor}.
 *
 * <p><b>Validates: Requirements 3.1, 3.2</b>
 */
class DefaultSmithyCustomizationProcessorTest {

    /**
     * Extracts the private {@code processorChain} array from a
     * {@link SmithyCustomizationProcessorChain} via reflection.
     */
    private static SmithyCustomizationProcessor[] getProcessorChain(
            SmithyCustomizationProcessorChain chain) throws Exception {
        Field field = SmithyCustomizationProcessorChain.class.getDeclaredField("processorChain");
        field.setAccessible(true);
        return (SmithyCustomizationProcessor[]) field.get(chain);
    }

    /**
     * Factory returns a chain containing exactly 15 processors.
     * Validates: Requirement 3.1
     */
    @Test
    void getProcessorFor_returnsChainWithAll15Processors() throws Exception {
        CustomizationConfig config = CustomizationConfig.create();

        SmithyCustomizationProcessor result = DefaultSmithyCustomizationProcessor.getProcessorFor(config);

        assertThat(result).isInstanceOf(SmithyCustomizationProcessorChain.class);
        SmithyCustomizationProcessor[] processors = getProcessorChain((SmithyCustomizationProcessorChain) result);
        assertThat(processors).hasSize(15);
    }

    /**
     * Processor ordering matches the C2J {@code DefaultCustomizationProcessor} sequence.
     * Validates: Requirement 3.2
     */
    @Test
    void getProcessorFor_processorsAreInCorrectOrder() throws Exception {
        CustomizationConfig config = CustomizationConfig.create();

        SmithyCustomizationProcessor result = DefaultSmithyCustomizationProcessor.getProcessorFor(config);
        SmithyCustomizationProcessor[] processors = getProcessorChain((SmithyCustomizationProcessorChain) result);

        assertThat(processors[0]).isInstanceOf(MetadataModifiersProcessor.class);
        assertThat(processors[1]).isInstanceOf(RenameShapesProcessor.class);
        assertThat(processors[2]).isInstanceOf(ShapeModifiersProcessor.class);
        assertThat(processors[3]).isInstanceOf(ShapeSubstitutionsProcessor.class);
        assertThat(processors[4]).isInstanceOf(CustomSdkShapesProcessor.class);
        assertThat(processors[5]).isInstanceOf(OperationModifiersProcessor.class);
        assertThat(processors[6]).isInstanceOf(RpcV2CborProtocolProcessor.class);
        assertThat(processors[7]).isInstanceOf(RemoveExceptionMessageProcessor.class);
        assertThat(processors[8]).isInstanceOf(UseLegacyEventGenerationSchemeProcessor.class);
        assertThat(processors[9]).isInstanceOf(NewAndLegacyEventStreamProcessor.class);
        assertThat(processors[10]).isInstanceOf(EventStreamSharedEventProcessor.class);
        assertThat(processors[11]).isInstanceOf(S3RemoveBucketFromUriProcessor.class);
        assertThat(processors[12]).isInstanceOf(S3ControlRemoveAccountIdHostPrefixProcessor.class);
        assertThat(processors[13]).isInstanceOf(ExplicitStringPayloadQueryProtocolProcessor.class);
        assertThat(processors[14]).isInstanceOf(LowercaseShapeValidatorProcessor.class);
    }
}
