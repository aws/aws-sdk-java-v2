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
import software.amazon.awssdk.codegen.model.config.customization.MetadataConfig;
import software.amazon.awssdk.codegen.model.config.customization.SmithyMetadataConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.Metadata;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Tests for {@link MetadataModifiersProcessor}.
 *
 * <p><b>Property 17: Metadata Protocol Override</b> — verify protocol trait applied to service shape.
 * <p><b>Validates: Requirements 12.1, 12.2, 12.3, 12.4, 30.5</b>
 */
class MetadataModifiersProcessorTest {

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

    // -----------------------------------------------------------------------
    // Property 17: Metadata Protocol Override
    // Validates: Requirement 12.1
    // -----------------------------------------------------------------------

    /**
     * Property 17: Metadata Protocol Override.
     * New config with protocol → protocol trait applied to service shape.
     * Validates: Requirement 12.1
     */
    @Test
    void preprocess_when_newConfigWithProtocol_protocolTraitAppliedToServiceShape() {
        SmithyMetadataConfig newConfig = new SmithyMetadataConfig();
        newConfig.setProtocol("aws.protocols#restJson1");

        MetadataModifiersProcessor processor =
            new MetadataModifiersProcessor(null, newConfig);

        Model result = processor.preprocess(model, service);

        ServiceShape resultService = result.expectShape(SERVICE_ID, ServiceShape.class);
        assertThat(resultService.hasTrait(ShapeId.from("aws.protocols#restJson1"))).isTrue();
    }

    /**
     * Property 17: Metadata Protocol Override.
     * New config with protocol and contentType → protocol trait applied, contentType stored.
     * Validates: Requirements 12.1, 12.2
     */
    @Test
    void preprocess_when_newConfigWithProtocolAndContentType_bothApplied() {
        SmithyMetadataConfig newConfig = new SmithyMetadataConfig();
        newConfig.setProtocol("aws.protocols#restJson1");
        newConfig.setContentType("application/x-amz-json-1.0");

        MetadataModifiersProcessor processor =
            new MetadataModifiersProcessor(null, newConfig);

        Model result = processor.preprocess(model, service);

        ServiceShape resultService = result.expectShape(SERVICE_ID, ServiceShape.class);
        assertThat(resultService.hasTrait(ShapeId.from("aws.protocols#restJson1"))).isTrue();

        // Verify contentType is applied in postprocess
        IntermediateModel intermediateModel = new IntermediateModel();
        intermediateModel.setMetadata(new Metadata());
        processor.postprocess(intermediateModel);
        assertThat(intermediateModel.getMetadata().getContentType())
            .isEqualTo("application/x-amz-json-1.0");
    }

    // -----------------------------------------------------------------------
    // contentType stored for postprocess
    // Validates: Requirement 12.2
    // -----------------------------------------------------------------------

    /**
     * New config with only contentType → model unchanged, contentType applied in postprocess.
     * Validates: Requirement 12.2
     */
    @Test
    void preprocess_when_newConfigWithOnlyContentType_modelUnchangedAndContentTypeAppliedInPostprocess() {
        SmithyMetadataConfig newConfig = new SmithyMetadataConfig();
        newConfig.setContentType("application/x-amz-json-1.1");

        MetadataModifiersProcessor processor =
            new MetadataModifiersProcessor(null, newConfig);

        Model result = processor.preprocess(model, service);

        // Protocol was null, so model should still have the service shape without new traits
        ServiceShape resultService = result.expectShape(SERVICE_ID, ServiceShape.class);
        assertThat(resultService.getAllTraits()).isEqualTo(service.getAllTraits());

        // Verify contentType is applied in postprocess
        IntermediateModel intermediateModel = new IntermediateModel();
        intermediateModel.setMetadata(new Metadata());
        processor.postprocess(intermediateModel);
        assertThat(intermediateModel.getMetadata().getContentType())
            .isEqualTo("application/x-amz-json-1.1");
    }

    /**
     * Postprocess without contentType does not modify metadata.
     * Validates: Requirement 12.2
     */
    @Test
    void postprocess_when_noContentType_metadataUnchanged() {
        SmithyMetadataConfig newConfig = new SmithyMetadataConfig();
        newConfig.setProtocol("aws.protocols#restJson1");

        MetadataModifiersProcessor processor =
            new MetadataModifiersProcessor(null, newConfig);
        processor.preprocess(model, service);

        IntermediateModel intermediateModel = new IntermediateModel();
        Metadata metadata = new Metadata();
        intermediateModel.setMetadata(metadata);
        processor.postprocess(intermediateModel);

        assertThat(intermediateModel.getMetadata().getContentType()).isNull();
    }

    // -----------------------------------------------------------------------
    // C2J protocol string conversion to Smithy trait ShapeId
    // Validates: Requirements 12.3, 30.5
    // -----------------------------------------------------------------------

    /**
     * Old config with "rest-json" → converted to "aws.protocols#restJson1" and applied.
     * Validates: Requirements 12.3, 30.5
     */
    @Test
    void preprocess_when_oldConfigWithRestJson_convertedToSmithyProtocol() {
        MetadataConfig oldConfig = new MetadataConfig();
        oldConfig.setProtocol("rest-json");

        MetadataModifiersProcessor processor =
            new MetadataModifiersProcessor(oldConfig, null);

        Model result = processor.preprocess(model, service);

        ServiceShape resultService = result.expectShape(SERVICE_ID, ServiceShape.class);
        assertThat(resultService.hasTrait(ShapeId.from("aws.protocols#restJson1"))).isTrue();
    }

    /**
     * Old config with "json" → converted to "aws.protocols#awsJson1_1".
     * Validates: Requirements 12.3, 30.5
     */
    @Test
    void preprocess_when_oldConfigWithJson_convertedToAwsJson1_1() {
        MetadataConfig oldConfig = new MetadataConfig();
        oldConfig.setProtocol("json");

        MetadataModifiersProcessor processor =
            new MetadataModifiersProcessor(oldConfig, null);

        Model result = processor.preprocess(model, service);

        ServiceShape resultService = result.expectShape(SERVICE_ID, ServiceShape.class);
        assertThat(resultService.hasTrait(ShapeId.from("aws.protocols#awsJson1_1"))).isTrue();
    }

    /**
     * Old config with "rest-xml" → converted to "aws.protocols#restXml".
     * Validates: Requirements 12.3, 30.5
     */
    @Test
    void preprocess_when_oldConfigWithRestXml_convertedToRestXml() {
        MetadataConfig oldConfig = new MetadataConfig();
        oldConfig.setProtocol("rest-xml");

        MetadataModifiersProcessor processor =
            new MetadataModifiersProcessor(oldConfig, null);

        Model result = processor.preprocess(model, service);

        ServiceShape resultService = result.expectShape(SERVICE_ID, ServiceShape.class);
        assertThat(resultService.hasTrait(ShapeId.from("aws.protocols#restXml"))).isTrue();
    }

    /**
     * Old config with "query" → converted to "aws.protocols#awsQuery".
     * Validates: Requirements 12.3, 30.5
     */
    @Test
    void preprocess_when_oldConfigWithQuery_convertedToAwsQuery() {
        MetadataConfig oldConfig = new MetadataConfig();
        oldConfig.setProtocol("query");

        MetadataModifiersProcessor processor =
            new MetadataModifiersProcessor(oldConfig, null);

        Model result = processor.preprocess(model, service);

        ServiceShape resultService = result.expectShape(SERVICE_ID, ServiceShape.class);
        assertThat(resultService.hasTrait(ShapeId.from("aws.protocols#awsQuery"))).isTrue();
    }

    /**
     * Old config with "ec2" → converted to "aws.protocols#ec2Query".
     * Validates: Requirements 12.3, 30.5
     */
    @Test
    void preprocess_when_oldConfigWithEc2_convertedToEc2Query() {
        MetadataConfig oldConfig = new MetadataConfig();
        oldConfig.setProtocol("ec2");

        MetadataModifiersProcessor processor =
            new MetadataModifiersProcessor(oldConfig, null);

        Model result = processor.preprocess(model, service);

        ServiceShape resultService = result.expectShape(SERVICE_ID, ServiceShape.class);
        assertThat(resultService.hasTrait(ShapeId.from("aws.protocols#ec2Query"))).isTrue();
    }

    /**
     * Old config with "cbor" → converted to "smithy.protocols#rpcv2Cbor".
     * Validates: Requirements 12.3, 30.5
     */
    @Test
    void preprocess_when_oldConfigWithCbor_convertedToRpcv2Cbor() {
        MetadataConfig oldConfig = new MetadataConfig();
        oldConfig.setProtocol("cbor");

        MetadataModifiersProcessor processor =
            new MetadataModifiersProcessor(oldConfig, null);

        Model result = processor.preprocess(model, service);

        ServiceShape resultService = result.expectShape(SERVICE_ID, ServiceShape.class);
        assertThat(resultService.hasTrait(ShapeId.from("smithy.protocols#rpcv2Cbor"))).isTrue();
    }

    /**
     * Old config with "smithy-rpc-v2-cbor" → converted to "smithy.protocols#rpcv2Cbor".
     * Validates: Requirements 12.3, 30.5
     */
    @Test
    void preprocess_when_oldConfigWithSmithyRpcV2Cbor_convertedToRpcv2Cbor() {
        MetadataConfig oldConfig = new MetadataConfig();
        oldConfig.setProtocol("smithy-rpc-v2-cbor");

        MetadataModifiersProcessor processor =
            new MetadataModifiersProcessor(oldConfig, null);

        Model result = processor.preprocess(model, service);

        ServiceShape resultService = result.expectShape(SERVICE_ID, ServiceShape.class);
        assertThat(resultService.hasTrait(ShapeId.from("smithy.protocols#rpcv2Cbor"))).isTrue();
    }

    /**
     * Old config with unknown protocol → throws IllegalStateException.
     * Validates: Requirements 12.3, 30.5
     */
    @Test
    void preprocess_when_oldConfigWithUnknownProtocol_throwsIllegalStateException() {
        MetadataConfig oldConfig = new MetadataConfig();
        oldConfig.setProtocol("unknown-protocol");

        MetadataModifiersProcessor processor =
            new MetadataModifiersProcessor(oldConfig, null);

        assertThatThrownBy(() -> processor.preprocess(model, service))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("unknown-protocol");
    }

    /**
     * Old config with contentType → contentType passed through to postprocess.
     * Validates: Requirements 12.3, 30.5
     */
    @Test
    void preprocess_when_oldConfigWithContentType_contentTypePassedThrough() {
        MetadataConfig oldConfig = new MetadataConfig();
        oldConfig.setContentType("application/x-amz-json-1.0");

        MetadataModifiersProcessor processor =
            new MetadataModifiersProcessor(oldConfig, null);
        processor.preprocess(model, service);

        IntermediateModel intermediateModel = new IntermediateModel();
        intermediateModel.setMetadata(new Metadata());
        processor.postprocess(intermediateModel);

        assertThat(intermediateModel.getMetadata().getContentType())
            .isEqualTo("application/x-amz-json-1.0");
    }

    // -----------------------------------------------------------------------
    // Null/empty config returns model unchanged
    // Validates: Requirement 12.4
    // -----------------------------------------------------------------------

    /**
     * Both configs null → returns model unchanged.
     * Validates: Requirement 12.4
     */
    @Test
    void preprocess_when_bothConfigsNull_returnsModelUnchanged() {
        MetadataModifiersProcessor processor =
            new MetadataModifiersProcessor(null, null);

        Model result = processor.preprocess(model, service);

        assertThat(result).isSameAs(model);
    }

    /**
     * Both configs have null fields → returns model unchanged.
     * Validates: Requirement 12.4
     */
    @Test
    void preprocess_when_bothConfigsHaveNullFields_returnsModelUnchanged() {
        MetadataConfig oldConfig = new MetadataConfig();
        SmithyMetadataConfig newConfig = new SmithyMetadataConfig();

        MetadataModifiersProcessor processor =
            new MetadataModifiersProcessor(oldConfig, newConfig);

        Model result = processor.preprocess(model, service);

        assertThat(result).isSameAs(model);
    }

    // -----------------------------------------------------------------------
    // Dual-config mutual exclusion (inherited from AbstractDualConfigProcessor)
    // Validates: Requirement 12.5
    // -----------------------------------------------------------------------

    /**
     * Both old and new config set → throws IllegalStateException.
     * Validates: Requirement 12.5
     */
    @Test
    void preprocess_when_bothConfigsSet_throwsIllegalStateException() {
        MetadataConfig oldConfig = new MetadataConfig();
        oldConfig.setProtocol("rest-json");

        SmithyMetadataConfig newConfig = new SmithyMetadataConfig();
        newConfig.setProtocol("aws.protocols#restJson1");

        MetadataModifiersProcessor processor =
            new MetadataModifiersProcessor(oldConfig, newConfig);

        assertThatThrownBy(() -> processor.preprocess(model, service))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("customServiceMetadata")
            .hasMessageContaining("smithyCustomServiceMetadata");
    }
}
