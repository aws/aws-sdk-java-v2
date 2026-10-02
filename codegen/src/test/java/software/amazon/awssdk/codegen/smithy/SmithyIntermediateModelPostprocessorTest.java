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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.internal.Utils;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.smithy.model.Model;

/**
 * Covers the intermediate-model customizations the Smithy builder applies after translation, which C2J applies in the
 * postprocess half of its customization chain.
 */
class SmithyIntermediateModelPostprocessorTest {

    private static final String REST_JSON_SERVICE =
        "$version: \"2.0\"\nnamespace demo\n\n"
        + "use aws.api#service\n"
        + "use aws.auth#sigv4\n"
        + "use aws.protocols#restJson1\n"
        + "@service(sdkId: \"Demo\", arnNamespace: \"demo\")\n"
        + "@sigv4(name: \"demo\")\n"
        + "@restJson1\n";

    @Test
    void exceptionMessageMember_isRemovedIgnoringCase() {
        IntermediateModel model = build(
            REST_JSON_SERVICE
            + "service DemoService { version: \"2024-01-01\", operations: [Op] }\n"
            + "@http(method: \"POST\", uri: \"/op\")\n"
            + "operation Op { input: OpRequest, output: OpResponse, errors: [LowerError, UpperError] }\n"
            + "structure OpRequest {}\n"
            + "structure OpResponse {}\n"
            + "@error(\"client\") structure LowerError { message: String, code: String }\n"
            + "@error(\"client\") structure UpperError { Message: String, Reason: String }\n",
            CustomizationConfig.create());

        assertThat(memberNames(model, "LowerError")).containsExactly("code");
        assertThat(memberNames(model, "UpperError")).containsExactly("Reason");
    }

    @Test
    void nonExceptionMessageMember_isKept() {
        IntermediateModel model = build(
            REST_JSON_SERVICE
            + "service DemoService { version: \"2024-01-01\", operations: [Op] }\n"
            + "@http(method: \"POST\", uri: \"/op\")\n"
            + "operation Op { input: OpRequest, output: OpResponse }\n"
            + "structure OpRequest { message: String }\n"
            + "structure OpResponse {}\n",
            CustomizationConfig.create());

        assertThat(memberNames(model, "OpRequest")).containsExactly("message");
    }

    @Test
    void longPollingOperation_isMarked() {
        IntermediateModel model = build(awsJsonService("SQS", "ReceiveMessage"), CustomizationConfig.create());

        assertThat(model.getOperation("ReceiveMessage").isLongPolling()).isTrue();
    }

    @Test
    void otherOperations_areNotMarkedLongPolling() {
        IntermediateModel model = build(awsJsonService("Demo", "ReceiveMessage"), CustomizationConfig.create());

        assertThat(model.getOperation("ReceiveMessage").isLongPolling()).isFalse();
    }

    @Test
    void longPollingService_missingOperation_fails() {
        assertThatThrownBy(() -> build(awsJsonService("SQS", "SendMessage"), CustomizationConfig.create()))
            .hasMessage("Operation ReceiveMessage not found for service SQS");
    }

    @Test
    void legacyEventGenerationScheme_validConfig_passes() {
        CustomizationConfig config = legacyEventConfig("EventOne");

        IntermediateModel model = build(eventStreamService(), config);

        assertThat(model.getShapes().get("EventStream").isEventStream()).isTrue();
    }

    @Test
    void legacyEventGenerationScheme_twoMembersSharingAShape_fails() {
        CustomizationConfig config = legacyEventConfig("EventOne", "EventTwo");

        assertThatThrownBy(() -> build(eventStreamService(), config))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("targets more than one member with the shape");
    }

    private static String awsJsonService(String sdkId, String operation) {
        return "$version: \"2.0\"\nnamespace demo\n\n"
               + "use aws.api#service\n"
               + "use aws.auth#sigv4\n"
               + "use aws.protocols#awsJson1_0\n"
               + "@service(sdkId: \"" + sdkId + "\", arnNamespace: \"demo\")\n"
               + "@sigv4(name: \"demo\")\n"
               + "@awsJson1_0\n"
               + "service DemoService { version: \"2024-01-01\", operations: [" + operation + "] }\n"
               + "operation " + operation + " { input: OpRequest, output: OpResponse }\n"
               + "structure OpRequest {}\n"
               + "structure OpResponse {}\n";
    }

    private static String eventStreamService() {
        return REST_JSON_SERVICE
               + "service DemoService { version: \"2024-01-01\", operations: [Subscribe] }\n"
               + "@http(method: \"POST\", uri: \"/subscribe\")\n"
               + "operation Subscribe { input: SubscribeRequest, output: SubscribeResponse }\n"
               + "structure SubscribeRequest {}\n"
               + "structure SubscribeResponse { @httpPayload events: EventStream }\n"
               + "@streaming union EventStream { EventOne: Event, EventTwo: Event }\n"
               + "structure Event { value: String }\n";
    }

    private static CustomizationConfig legacyEventConfig(String... members) {
        CustomizationConfig config = CustomizationConfig.create();
        config.setUseLegacyEventGenerationScheme(Collections.singletonMap("EventStream", Arrays.asList(members)));
        return config;
    }

    private static List<String> memberNames(IntermediateModel model, String c2jName) {
        return Utils.findShapeModelByC2jName(model, c2jName).getMembers().stream()
                    .map(MemberModel::getC2jName)
                    .collect(Collectors.toList());
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
