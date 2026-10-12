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

package software.amazon.awssdk.codegen.smithy.transform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.codegen.smithy.transform.TransformTestSupport.REST_JSON_PREFIX;
import static software.amazon.awssdk.codegen.smithy.transform.TransformTestSupport.apply;
import static software.amazon.awssdk.codegen.smithy.transform.TransformTestSupport.model;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.model.traits.HttpHeaderTrait;
import software.amazon.smithy.model.traits.RequiredTrait;

class InjectMembersTransformerTest {

    private static final Model MODEL = model(
        REST_JSON_PREFIX
        + "service DemoService { version: \"2024-01-01\", operations: [Get] }\n"
        + "@http(method: \"POST\", uri: \"/get\")\n"
        + "operation Get { input: GetRequest, output: GetResponse }\n"
        + "structure GetRequest {}\n"
        + "structure GetResponse { @httpHeader(\"Expires\") Expires: String }\n"
        + "timestamp Expiration\n");

    private final InjectMembersTransformer transformer = new InjectMembersTransformer();

    @Test
    void name_isInjectMembers() {
        assertThat(transformer.getName()).isEqualTo("injectMembers");
    }

    @Test
    void injectedMember_keepsTargetAndTraitsAndIsAppended() {
        Model result = apply(transformer, MODEL,
                             "{\"members\": {\"demo#GetResponse$ExpiresString\": {"
                             + "\"target\": \"demo#Expiration\","
                             + "\"traits\": {\"smithy.api#documentation\": \"no longer cacheable\","
                             + "\"smithy.api#httpHeader\": \"ExpiresString\"}}}}");

        MemberShape injected = result.expectShape(ShapeId.from("demo#GetResponse$ExpiresString"), MemberShape.class);
        assertThat(injected.getTarget()).isEqualTo(ShapeId.from("demo#Expiration"));
        assertThat(injected.expectTrait(HttpHeaderTrait.class).getValue()).isEqualTo("ExpiresString");
        assertThat(injected.expectTrait(DocumentationTrait.class).getValue()).isEqualTo("no longer cacheable");
        assertThat(injected.hasTrait(RequiredTrait.class)).isFalse();
        assertThat(result.expectShape(ShapeId.from("demo#GetResponse")).getMemberNames())
            .containsExactly("Expires", "ExpiresString");
    }

    @Test
    void preludeTarget_isAllowed() {
        Model result = apply(transformer, MODEL,
                             "{\"members\": {\"demo#GetRequest$SourceRegion\": {\"target\": \"smithy.api#String\"}}}");

        assertThat(result.expectShape(ShapeId.from("demo#GetRequest$SourceRegion"), MemberShape.class).getTarget())
            .isEqualTo(ShapeId.from("smithy.api#String"));
    }

    @Test
    void duplicateMember_failsIgnoringCase() {
        assertThatThrownBy(() -> apply(transformer, MODEL,
                                       "{\"members\": {\"demo#GetResponse$expires\": {\"target\": \"smithy.api#String\"}}}"))
            .hasMessageContaining("already has a member named 'Expires'");
    }

    @Test
    void missingContainer_fails() {
        assertThatThrownBy(() -> apply(transformer, MODEL,
                                       "{\"members\": {\"demo#Nope$x\": {\"target\": \"smithy.api#String\"}}}"))
            .hasMessageContaining("shape 'demo#Nope' does not exist");
    }

    @Test
    void nonAggregateContainer_fails() {
        assertThatThrownBy(() -> apply(transformer, MODEL,
                                       "{\"members\": {\"demo#Expiration$x\": {\"target\": \"smithy.api#String\"}}}"))
            .hasMessageContaining("only structures and unions are supported");
    }

    @Test
    void missingTarget_fails() {
        assertThatThrownBy(() -> apply(transformer, MODEL,
                                       "{\"members\": {\"demo#GetRequest$x\": {\"target\": \"demo#Nope\"}}}"))
            .hasMessageContaining("shape 'demo#Nope' does not exist");
    }

    @Test
    void operationTarget_fails() {
        assertThatThrownBy(() -> apply(transformer, MODEL,
                                       "{\"members\": {\"demo#GetRequest$x\": {\"target\": \"demo#Get\"}}}"))
            .hasMessageContaining("cannot be a member target");
    }
}
