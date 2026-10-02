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
import software.amazon.smithy.model.traits.HttpQueryTrait;
import software.amazon.smithy.model.traits.HttpTrait;
import software.amazon.smithy.model.traits.JsonNameTrait;

class RenameMembersTransformerTest {

    private static final Model MODEL = model(
        REST_JSON_PREFIX
        + "service DemoService { version: \"2024-01-01\", operations: [Get] }\n"
        + "@http(method: \"POST\", uri: \"/things/{Id}\")\n"
        + "operation Get { input: GetRequest, output: GetResponse }\n"
        + "structure GetRequest {\n"
        + "  @required @httpLabel Id: String\n"
        + "  @documentation(\"fields to return\") @httpQuery(\"return\") return: String\n"
        + "  other: String\n"
        + "}\n"
        + "structure GetResponse { RequestId: String, value: String }\n");

    private final RenameMembersTransformer transformer = new RenameMembersTransformer();

    @Test
    void name_isRenameMembers() {
        assertThat(transformer.getName()).isEqualTo("renameMembers");
    }

    @Test
    void renamedMember_keepsTraitsAndAddsConfiguredTraits() {
        Model result = apply(transformer, MODEL,
                             "{\"renamed\": {\"demo#GetResponse$RequestId\": {"
                             + "\"name\": \"directoryRequestId\","
                             + "\"traits\": {\"smithy.api#jsonName\": \"RequestId\"}}}}");

        assertThat(result.getShape(ShapeId.from("demo#GetResponse$RequestId"))).isEmpty();
        MemberShape renamed = result.expectShape(ShapeId.from("demo#GetResponse$directoryRequestId"), MemberShape.class);
        assertThat(renamed.expectTrait(JsonNameTrait.class).getValue()).isEqualTo("RequestId");
        assertThat(result.expectShape(ShapeId.from("demo#GetResponse")).getMemberNames())
            .containsExactly("value", "directoryRequestId");
    }

    @Test
    void renamedBoundMember_keepsItsBinding() {
        Model result = apply(transformer, MODEL,
                             "{\"renamed\": {\"demo#GetRequest$return\": {\"name\": \"returnValues\"}}}");

        MemberShape renamed = result.expectShape(ShapeId.from("demo#GetRequest$returnValues"), MemberShape.class);
        assertThat(renamed.expectTrait(HttpQueryTrait.class).getValue()).isEqualTo("return");
        assertThat(renamed.expectTrait(DocumentationTrait.class).getValue()).isEqualTo("fields to return");
    }

    @Test
    void renamedUriLabel_isRenamedInTheOperationUri() {
        Model result = apply(transformer, MODEL,
                             "{\"renamed\": {\"demo#GetRequest$Id\": {\"name\": \"ThingId\"}}}");

        assertThat(result.expectShape(ShapeId.from("demo#Get")).expectTrait(HttpTrait.class).getUri().toString())
            .isEqualTo("/things/{ThingId}");
    }

    @Test
    void missingMember_fails() {
        assertThatThrownBy(() -> apply(transformer, MODEL,
                                       "{\"renamed\": {\"demo#GetResponse$Nope\": {\"name\": \"x\"}}}"))
            .hasMessageContaining("renameMembers")
            .hasMessageContaining("demo#GetResponse$Nope");
    }

    @Test
    void shapeIdWithoutMember_fails() {
        assertThatThrownBy(() -> apply(transformer, MODEL,
                                       "{\"renamed\": {\"demo#GetResponse\": {\"name\": \"x\"}}}"))
            .hasMessageContaining("expected a member ID");
    }

    @Test
    void nameCollidingWithAnotherMember_failsIgnoringCase() {
        assertThatThrownBy(() -> apply(transformer, MODEL,
                                       "{\"renamed\": {\"demo#GetResponse$RequestId\": {\"name\": \"Value\"}}}"))
            .hasMessageContaining("already has a member named 'value'");
    }

    @Test
    void invalidName_fails() {
        assertThatThrownBy(() -> apply(transformer, MODEL,
                                       "{\"renamed\": {\"demo#GetResponse$RequestId\": {\"name\": \"not-valid\"}}}"))
            .hasMessageContaining("is not a valid member name");
    }

    @Test
    void unknownTrait_fails() {
        assertThatThrownBy(() -> apply(transformer, MODEL,
                                       "{\"renamed\": {\"demo#GetResponse$RequestId\": {\"name\": \"x\","
                                       + "\"traits\": {\"demo#noSuchTrait\": {}}}}}"))
            .hasMessageContaining("unknown trait 'demo#noSuchTrait'");
    }

    @Test
    void unknownSetting_fails() {
        assertThatThrownBy(() -> apply(transformer, MODEL, "{\"renamed\": {}, \"extra\": true}"))
            .hasMessageContaining("extra");
    }
}
