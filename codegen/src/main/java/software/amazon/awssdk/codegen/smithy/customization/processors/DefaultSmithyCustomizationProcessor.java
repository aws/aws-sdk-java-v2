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

import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.smithy.customization.SmithyCustomizationProcessor;
import software.amazon.awssdk.codegen.smithy.customization.SmithyCustomizationProcessorChain;

/**
 * Factory that creates the standard chain of Smithy customization processors for a given
 * {@link CustomizationConfig}. This mirrors the C2J
 * {@link software.amazon.awssdk.codegen.customization.processors.DefaultCustomizationProcessor}
 * but operates on Smithy's immutable {@code Model}.
 *
 * <p>The processor ordering matches the C2J pipeline exactly:
 * <ol>
 *   <li>MetadataModifiers (Category C)</li>
 *   <li>RenameShapes (Category B)</li>
 *   <li>ShapeModifiers (Category C)</li>
 *   <li>ShapeSubstitutions (Category C)</li>
 *   <li>CustomSdkShapes (Category C)</li>
 *   <li>OperationModifiers (Category C)</li>
 *   <li>RpcV2CborProtocol (Category D — no-op)</li>
 *   <li>RemoveExceptionMessage (Category A)</li>
 *   <li>UseLegacyEventGenerationScheme (Category A)</li>
 *   <li>NewAndLegacyEventStream (Category B)</li>
 *   <li>EventStreamSharedEvent (Category C)</li>
 *   <li>S3RemoveBucketFromUri (Category B)</li>
 *   <li>S3ControlRemoveAccountIdHostPrefix (Category B)</li>
 *   <li>ExplicitStringPayloadQueryProtocol (Category B)</li>
 *   <li>LowercaseShapeValidator (Category B)</li>
 * </ol>
 *
 * <p>Category C processors receive both old (deprecated) and new (Smithy-native) config fields
 * for dual-config resolution. Category B processors receive their direct config fields.
 * Category A processors are postprocess-only and take no config. Category D is a no-op placeholder.
 */
public final class DefaultSmithyCustomizationProcessor {

    private DefaultSmithyCustomizationProcessor() {
    }

    /**
     * Creates a {@link SmithyCustomizationProcessorChain} containing all 15 Smithy processors
     * wired with the appropriate config fields from the given {@link CustomizationConfig}.
     *
     * @param config the customization config for the service being generated
     * @return a composite processor that applies all customizations in the correct order
     */
    public static SmithyCustomizationProcessor getProcessorFor(CustomizationConfig config) {
        return new SmithyCustomizationProcessorChain(
            // Category C: receives both old + new config for dual-config resolution
            new MetadataModifiersProcessor(
                config.getCustomServiceMetadata(),
                config.getSmithyCustomServiceMetadata()),
            // Category B: direct Smithy equivalent, no dual-config needed
            new RenameShapesProcessor(config.getRenameShapes()),
            // Category C processors with dual-config
            new ShapeModifiersProcessor(
                config.getShapeModifiers(),
                config.getSmithyShapeModifiers()),
            new ShapeSubstitutionsProcessor(
                config.getShapeSubstitutions(),
                config.getSmithyShapeSubstitutions()),
            new CustomSdkShapesProcessor(
                config.getCustomSdkShapes(),
                config.getSmithyCustomSdkShapes()),
            new OperationModifiersProcessor(
                config.getOperationModifiers(),
                config.getSmithyOperationModifiers()),
            // Category D: no-op with TODO
            new RpcV2CborProtocolProcessor(),
            // Category A: postprocess-only, reused as-is
            new RemoveExceptionMessageProcessor(),
            new UseLegacyEventGenerationSchemeProcessor(),
            new NewAndLegacyEventStreamProcessor(),
            // Category C: dual-config
            new EventStreamSharedEventProcessor(
                config.getDuplicateAndRenameSharedEvents(),
                config.getSmithyDuplicateAndRenameSharedEvents()),
            // Category B: direct Smithy equivalents
            new S3RemoveBucketFromUriProcessor(),
            new S3ControlRemoveAccountIdHostPrefixProcessor(),
            new ExplicitStringPayloadQueryProtocolProcessor(),
            new LowercaseShapeValidatorProcessor()
        );
    }
}
