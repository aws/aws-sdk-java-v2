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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.config.customization.MetadataConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.Protocol;
import software.amazon.awssdk.codegen.validation.ModelInvalidException;
import software.amazon.smithy.model.Model;

/**
 * Covers the C2J preprocess behavior with no customization config that the Smithy builder reproduces: source-model
 * validation and the configured protocol override.
 */
class SmithySourceModelValidationTest {

    @Test
    void lowercaseStructureName_fails() {
        assertThatThrownBy(() -> build(restJson("structure OpRequest { detail: detail }\nstructure detail {}\n"),
                                       CustomizationConfig.create()))
            .isInstanceOf(ModelInvalidException.class)
            .hasMessageContaining("Shape name 'detail' starts with a lowercase character");
    }

    @Test
    void uppercaseStructureNames_pass() {
        IntermediateModel model = build(restJson("structure OpRequest { detail: Detail }\nstructure Detail {}\n"),
                                        CustomizationConfig.create());

        assertThat(model.getShapes()).containsKey("Detail");
    }

    @Test
    void unapprovedEventStreamForLegacyService_fails() {
        assertThatThrownBy(() -> build(eventStreamService("Kinesis", "NewEventStream"), CustomizationConfig.create()))
            .hasMessageContaining("Encountered a new eventstream for service Kinesis: NewEventStream");
    }

    @Test
    void approvedEventStreamForLegacyService_passes() {
        IntermediateModel model = build(eventStreamService("Kinesis", "SubscribeToShardEventStream"),
                                        CustomizationConfig.create());

        assertThat(model.getShapes().get("SubscribeToShardEventStream").isEventStream()).isTrue();
    }

    @Test
    void eventStreamForOtherService_passes() {
        IntermediateModel model = build(eventStreamService("Demo", "NewEventStream"), CustomizationConfig.create());

        assertThat(model.getShapes().get("NewEventStream").isEventStream()).isTrue();
    }

    @Test
    void explicitStringPayloadOnQueryProtocol_fails() {
        String smithy =
            "$version: \"2.0\"\nnamespace demo\n\n"
            + "use aws.api#service\nuse aws.auth#sigv4\nuse aws.protocols#awsQuery\n"
            + "@service(sdkId: \"Demo\", arnNamespace: \"demo\")\n@sigv4(name: \"demo\")\n"
            + "@awsQuery @xmlNamespace(uri: \"https://demo.amazonaws.com/doc/2024-01-01/\")\n"
            + "service DemoService { version: \"2024-01-01\", operations: [Op] }\n"
            + "operation Op { input: OpRequest, output: OpResponse }\n"
            + "structure OpRequest { @httpPayload body: String }\n"
            + "structure OpResponse {}\n";

        assertThatThrownBy(() -> build(smithy, CustomizationConfig.create()))
            .hasMessage("Operations with explicit String payloads are not supported for Query protocols. "
                        + "Unsupported operation: Op");
    }

    @Test
    void explicitStringPayloadOnRestProtocol_passes() {
        IntermediateModel model = build(restJson("structure OpRequest { @httpPayload body: String }\n"),
                                        CustomizationConfig.create());

        assertThat(model.getOperation("Op")).isNotNull();
    }

    @Test
    void protocolOverride_selectsLegacyCborForTheWholeModel() {
        IntermediateModel model = build(awsJson("Kinesis"), protocolOverride("cbor"));

        assertThat(model.getMetadata().getProtocol()).isEqualTo(Protocol.CBOR);
        assertThat(model.getMetadata().getJsonVersion()).isEqualTo("1.1");
        assertThat(model.getOperation("Op").getServiceProtocol()).isEqualTo("cbor");
        assertThat(model.getShapes().get("OpRequest").getMarshaller().getTarget()).isEqualTo("DemoService.Op");
    }

    @Test
    void protocolOverride_restJson_honorsHttpBindings() {
        String smithy = awsJson("Demo").replace("operation Op {",
                                                "@http(method: \"POST\", uri: \"/op/{Id}\")\noperation Op {")
                                       .replace("structure OpRequest {}",
                                                "structure OpRequest { @required @httpLabel Id: String }");

        IntermediateModel model = build(smithy, protocolOverride("rest-json"));

        assertThat(model.getMetadata().getProtocol()).isEqualTo(Protocol.REST_JSON);
        assertThat(model.getShapes().get("OpRequest").getMarshaller().getRequestUri()).isEqualTo("/op/{Id}");
    }

    @Test
    void unsupportedProtocolOverride_fails() {
        assertThatThrownBy(() -> build(awsJson("Demo"), protocolOverride("api-gateway")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("The SDK does not support any of provided protocols: [api-gateway]");
    }

    private static CustomizationConfig protocolOverride(String protocol) {
        MetadataConfig metadata = new MetadataConfig();
        metadata.setProtocol(protocol);
        CustomizationConfig config = CustomizationConfig.create();
        config.setCustomServiceMetadata(metadata);
        return config;
    }

    private static String restJson(String requestShapes) {
        return "$version: \"2.0\"\nnamespace demo\n\n"
               + "use aws.api#service\nuse aws.auth#sigv4\nuse aws.protocols#restJson1\n"
               + "@service(sdkId: \"Demo\", arnNamespace: \"demo\")\n@sigv4(name: \"demo\")\n@restJson1\n"
               + "service DemoService { version: \"2024-01-01\", operations: [Op] }\n"
               + "@http(method: \"POST\", uri: \"/op\")\n"
               + "operation Op { input: OpRequest, output: OpResponse }\n"
               + requestShapes
               + "structure OpResponse {}\n";
    }

    private static String awsJson(String sdkId) {
        return "$version: \"2.0\"\nnamespace demo\n\n"
               + "use aws.api#service\nuse aws.auth#sigv4\nuse aws.protocols#awsJson1_1\n"
               + "@service(sdkId: \"" + sdkId + "\", arnNamespace: \"demo\")\n@sigv4(name: \"demo\")\n@awsJson1_1\n"
               + "service DemoService { version: \"2024-01-01\", operations: [Op] }\n"
               + "operation Op { input: OpRequest, output: OpResponse }\n"
               + "structure OpRequest {}\n"
               + "structure OpResponse {}\n";
    }

    private static String eventStreamService(String sdkId, String streamName) {
        return "$version: \"2.0\"\nnamespace demo\n\n"
               + "use aws.api#service\nuse aws.auth#sigv4\nuse aws.protocols#restJson1\n"
               + "@service(sdkId: \"" + sdkId + "\", arnNamespace: \"demo\")\n@sigv4(name: \"demo\")\n@restJson1\n"
               + "service DemoService { version: \"2024-01-01\", operations: [Subscribe] }\n"
               + "@http(method: \"POST\", uri: \"/subscribe\")\n"
               + "operation Subscribe { input: SubscribeRequest, output: SubscribeResponse }\n"
               + "structure SubscribeRequest {}\n"
               + "structure SubscribeResponse { @httpPayload events: " + streamName + " }\n"
               + "@streaming union " + streamName + " { Event: Event }\n"
               + "structure Event { value: String }\n";
    }

    private static IntermediateModel build(String smithy, CustomizationConfig config) {
        Model model = Model.assembler()
                           .discoverModels(Model.class.getClassLoader())
                           .addUnparsedModel("test.smithy", smithy)
                           .assemble()
                           .unwrap();
        return new SmithyIntermediateModelBuilder(SmithyModels.builder()
                                                              .model(model)
                                                              .customizationConfig(config)
                                                              .build())
            .build();
    }
}
