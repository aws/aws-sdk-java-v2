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
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.DeprecatedTrait;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.model.traits.SensitiveTrait;

class AddOrOverrideTraitsTransformerTest {

    private static final Model MODEL = model(
        REST_JSON_PREFIX
        + "service DemoService { version: \"2024-01-01\", operations: [Copy] }\n"
        + "@http(method: \"POST\", uri: \"/copy\")\n"
        + "operation Copy { input: CopyRequest, output: CopyResponse }\n"
        + "structure CopyRequest { @documentation(\"old docs\") CopySource: String }\n"
        + "structure CopyResponse {}\n"
        + "string Secret\n");

    private static final Model MIXIN_MODEL = model(
        "$version: \"2.0\"\nnamespace demo\n"
        + "@mixin\n"
        + "structure Base { a: String }\n"
        + "structure Uses with [Base] { b: String }\n");

    private final AddOrOverrideTraitsTransformer transformer = new AddOrOverrideTraitsTransformer();

    @Test
    void name_isAddOrOverrideTraits() {
        assertThat(transformer.getName()).isEqualTo("addOrOverrideTraits");
    }

    @Test
    void memberTrait_isAddedAndExistingTraitIsReplaced() {
        Model result = apply(transformer, MODEL,
                             "{\"traits\": {\"demo#CopyRequest$CopySource\": {"
                             + "\"smithy.api#deprecated\": {\"message\": \"Use SourceBucket instead.\"},"
                             + "\"smithy.api#documentation\": \"new docs\"}}}");

        ShapeId member = ShapeId.from("demo#CopyRequest$CopySource");
        assertThat(result.expectShape(member).expectTrait(DeprecatedTrait.class).getMessage())
            .hasValue("Use SourceBucket instead.");
        assertThat(result.expectShape(member).expectTrait(DocumentationTrait.class).getValue()).isEqualTo("new docs");
    }

    @Test
    void shapeTrait_isAdded() {
        Model result = apply(transformer, MODEL, "{\"traits\": {\"demo#Secret\": {\"smithy.api#sensitive\": {}}}}");

        assertThat(result.expectShape(ShapeId.from("demo#Secret")).hasTrait(SensitiveTrait.class)).isTrue();
    }

    @Test
    void missingShape_fails() {
        assertThatThrownBy(() -> apply(transformer, MODEL,
                                       "{\"traits\": {\"demo#CopyRequest$Nope\": {\"smithy.api#sensitive\": {}}}}"))
            .hasMessageContaining("shape 'demo#CopyRequest$Nope' does not exist");
    }

    @Test
    void memberInheritedFromMixin_fails() {
        assertThatThrownBy(() -> apply(transformer, MIXIN_MODEL,
                                       "{\"traits\": {\"demo#Uses$a\": {\"smithy.api#sensitive\": {}}}}"))
            .hasMessage("Transform 'addOrOverrideTraits': 'demo#Uses$a' belongs to 'demo#Uses', which uses mixins; "
                        + "mixins are not supported");
    }

    @Test
    void mixinMember_fails() {
        assertThatThrownBy(() -> apply(transformer, MIXIN_MODEL,
                                       "{\"traits\": {\"demo#Base$a\": {\"smithy.api#sensitive\": {}}}}"))
            .hasMessage("Transform 'addOrOverrideTraits': 'demo#Base$a' belongs to 'demo#Base', which is a mixin; "
                        + "mixins are not supported");
    }

    @Test
    void shapeThatUsesMixins_fails() {
        assertThatThrownBy(() -> apply(transformer, MIXIN_MODEL,
                                       "{\"traits\": {\"demo#Uses\": {\"smithy.api#documentation\": \"docs\"}}}"))
            .hasMessage("Transform 'addOrOverrideTraits': 'demo#Uses' uses mixins; mixins are not supported");
    }

    @Test
    void unknownTrait_fails() {
        assertThatThrownBy(() -> apply(transformer, MODEL, "{\"traits\": {\"demo#Secret\": {\"demo#nope\": {}}}}"))
            .hasMessageContaining("unknown trait 'demo#nope'");
    }
}
