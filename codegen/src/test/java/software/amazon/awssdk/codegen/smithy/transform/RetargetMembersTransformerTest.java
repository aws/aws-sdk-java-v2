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

class RetargetMembersTransformerTest {

    private static final Model MODEL = model(
        REST_JSON_PREFIX
        + "service DemoService { version: \"2024-01-01\", operations: [Get] }\n"
        + "@http(method: \"POST\", uri: \"/get\")\n"
        + "operation Get { input: GetRequest, output: GetResponse }\n"
        + "structure GetRequest {}\n"
        + "structure GetResponse { @documentation(\"the amount\") Amount: NumericString }\n"
        + "string NumericString\n");

    private final RetargetMembersTransformer transformer = new RetargetMembersTransformer();

    @Test
    void name_isRetargetMembers() {
        assertThat(transformer.getName()).isEqualTo("retargetMembers");
    }

    @Test
    void retargetedMember_keepsItsTraits() {
        Model result = apply(transformer, MODEL,
                             "{\"targets\": {\"demo#GetResponse$Amount\": \"smithy.api#BigDecimal\"}}");

        MemberShape member = result.expectShape(ShapeId.from("demo#GetResponse$Amount"), MemberShape.class);
        assertThat(member.getTarget()).isEqualTo(ShapeId.from("smithy.api#BigDecimal"));
        assertThat(member.expectTrait(DocumentationTrait.class).getValue()).isEqualTo("the amount");
    }

    @Test
    void missingMember_fails() {
        assertThatThrownBy(() -> apply(transformer, MODEL,
                                       "{\"targets\": {\"demo#GetResponse$Nope\": \"smithy.api#Long\"}}"))
            .hasMessageContaining("shape 'demo#GetResponse$Nope' does not exist");
    }

    @Test
    void missingTarget_fails() {
        assertThatThrownBy(() -> apply(transformer, MODEL,
                                       "{\"targets\": {\"demo#GetResponse$Amount\": \"demo#Nope\"}}"))
            .hasMessageContaining("shape 'demo#Nope' does not exist");
    }

    @Test
    void memberTarget_fails() {
        assertThatThrownBy(() -> apply(transformer, MODEL,
                                       "{\"targets\": {\"demo#GetResponse$Amount\": \"demo#GetResponse$Amount\"}}"))
            .hasMessageContaining("expected a shape ID without a member");
    }
}
