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

package software.amazon.awssdk.codegen.smithy;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.Metadata;
import software.amazon.awssdk.codegen.naming.DefaultSmithyNamingStrategy;
import software.amazon.awssdk.codegen.naming.NamingStrategy;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.ServiceIndex;
import software.amazon.smithy.model.shapes.ServiceShape;

/**
 * Unit tests for {@link AddSmithyMetadata}, covering the metadata fields whose Smithy source is not
 * a straight copy of a single service trait.
 */
class AddSmithyMetadataTest {

    private static Model modelOf(String protocolUse, String serviceTraits, String extraShapes) {
        String src =
            "$version: \"2.0\"\nnamespace demo\n\n"
            + "use aws.api#service\n"
            + "use aws.auth#sigv4\n"
            + protocolUse
            + "\n"
            + "@service(sdkId: \"Demo\", arnNamespace: \"demo\")\n"
            + "@sigv4(name: \"demo\")\n"
            + serviceTraits
            + "service DemoService { version: \"2024-01-01\", operations: [Op] }\n\n"
            + "operation Op { input: OpRequest, output: OpResponse }\n"
            + "structure OpRequest {}\n"
            + "structure OpResponse {}\n"
            + extraShapes;
        return Model.assembler()
                    .discoverModels(Model.class.getClassLoader())
                    .addUnparsedModel("test.smithy", src)
                    .assemble()
                    .unwrap();
    }

    private static Metadata metadataOf(Model model) {
        ServiceShape service = model.getServiceShapes().iterator().next();
        NamingStrategy naming = new DefaultSmithyNamingStrategy(model, service, CustomizationConfig.create());
        return AddSmithyMetadata.constructMetadata(model, service, ServiceIndex.of(model), naming,
                                                   CustomizationConfig.create());
    }

    // ---- jsonVersion ------------------------------------------------------

    @Test
    void awsJson1_0_jsonVersionIs10() {
        Metadata metadata = metadataOf(modelOf(
            "use aws.protocols#awsJson1_0\n", "@awsJson1_0\n", ""));
        assertThat(metadata.getJsonVersion()).isEqualTo("1.0");
    }

    @Test
    void awsJson1_1_jsonVersionIs11() {
        Metadata metadata = metadataOf(modelOf(
            "use aws.protocols#awsJson1_1\n", "@awsJson1_1\n", ""));
        assertThat(metadata.getJsonVersion()).isEqualTo("1.1");
    }

    @Test
    void restJson_jsonVersionDefaultsTo11() {
        Metadata metadata = metadataOf(modelOf(
            "use aws.protocols#restJson1\n", "@restJson1\n", ""));
        assertThat(metadata.getJsonVersion()).isEqualTo("1.1");
    }

    @Test
    void queryProtocol_hasNoJsonVersion() {
        Metadata metadata = metadataOf(modelOf(
            "use aws.protocols#awsQuery\nuse aws.protocols#awsQueryError\n",
            "@awsQuery\n@xmlNamespace(uri: \"https://demo.amazonaws.com/doc/2024-01-01/\")\n", ""));
        assertThat(metadata.getJsonVersion()).isNull();
    }

    // ---- awsQueryCompatible -----------------------------------------------

    @Test
    void awsQueryCompatibleTrait_mapsToEmptyMap() {
        Metadata metadata = metadataOf(modelOf(
            "use aws.protocols#awsJson1_0\nuse aws.protocols#awsQueryCompatible\n",
            "@awsJson1_0\n@awsQueryCompatible\n", ""));
        assertThat(metadata.getAwsQueryCompatible()).isNotNull().isEmpty();
    }

    @Test
    void noAwsQueryCompatibleTrait_isNull() {
        Metadata metadata = metadataOf(modelOf(
            "use aws.protocols#awsJson1_0\n", "@awsJson1_0\n", ""));
        assertThat(metadata.getAwsQueryCompatible()).isNull();
    }

    // ---- uid --------------------------------------------------------------

    @Test
    void noDocId_uidDerivedFromSdkIdAndVersion() {
        Metadata metadata = metadataOf(modelWithServiceTrait(
            "@service(sdkId: \"Demo Widget\", arnNamespace: \"demo\")"));
        assertThat(metadata.getUid()).isEqualTo("demo-widget-2024-01-01");
    }

    @Test
    void explicitDocId_overridesDerivedUid() {
        Metadata metadata = metadataOf(modelWithServiceTrait(
            "@service(sdkId: \"Demo Widget\", arnNamespace: \"demo\", docId: \"demowidget-2024-01-01\")"));
        assertThat(metadata.getUid()).isEqualTo("demowidget-2024-01-01");
    }

    // ---- supportsH2 -------------------------------------------------------

    // C2J equivalent: "h2": "eventstream".
    @Test
    void eventStreamHttpListsH2_supportsH2() {
        Metadata metadata = metadataOf(modelOf(
            "use aws.protocols#restJson1\n",
            "@restJson1(http: [\"http/1.1\", \"h2\"], eventStreamHttp: [\"h2\"])\n", ""));
        assertThat(metadata.supportsH2()).isTrue();
    }

    // C2J equivalent: "h2": "required".
    @Test
    void httpListsOnlyH2_supportsH2() {
        Metadata metadata = metadataOf(modelOf(
            "use aws.protocols#restJson1\n", "@restJson1(http: [\"h2\"])\n", ""));
        assertThat(metadata.supportsH2()).isTrue();
    }

    @Test
    void httpListsWithoutH2_doesNotSupportH2() {
        Metadata metadata = metadataOf(modelOf(
            "use aws.protocols#restJson1\n",
            "@restJson1(http: [\"http/1.1\"], eventStreamHttp: [\"http/1.1\"])\n", ""));
        assertThat(metadata.supportsH2()).isFalse();
    }

    @Test
    void noHttpConfiguration_doesNotSupportH2() {
        Metadata metadata = metadataOf(modelOf(
            "use aws.protocols#restJson1\n", "@restJson1\n", ""));
        assertThat(metadata.supportsH2()).isFalse();
    }

    // rpcv2Cbor inherits the two members from a different base trait class than the aws.protocols
    // traits, so this only passes when they are read off the trait node.
    @Test
    void rpcV2CborEventStreamHttpListsH2_supportsH2() {
        Metadata metadata = metadataOf(modelOf(
            "use smithy.protocols#rpcv2Cbor\n",
            "@rpcv2Cbor(http: [\"http/1.1\", \"h2\"], eventStreamHttp: [\"h2\"])\n", ""));
        assertThat(metadata.supportsH2()).isTrue();
    }

    private static Model modelWithServiceTrait(String serviceTrait) {
        String src =
            "$version: \"2.0\"\nnamespace demo\n\n"
            + "use aws.api#service\n"
            + "use aws.auth#sigv4\n"
            + "use aws.protocols#awsJson1_0\n\n"
            + serviceTrait + "\n"
            + "@sigv4(name: \"demo\")\n"
            + "@awsJson1_0\n"
            + "service DemoService { version: \"2024-01-01\", operations: [Op] }\n\n"
            + "operation Op { input: OpRequest, output: OpResponse }\n"
            + "structure OpRequest {}\n"
            + "structure OpResponse {}\n";
        return Model.assembler()
                    .discoverModels(Model.class.getClassLoader())
                    .addUnparsedModel("test.smithy", src)
                    .assemble()
                    .unwrap();
    }
}
