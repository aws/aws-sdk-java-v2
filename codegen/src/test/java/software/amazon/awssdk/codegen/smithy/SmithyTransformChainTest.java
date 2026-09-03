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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;

class SmithyTransformChainTest {

    private static final String IDL =
        "$version: \"2\"\n"
        + "namespace com.example\n"
        + "service Demo { version: \"2024-01-01\", operations: [GetThing] }\n"
        + "operation GetThing { input: GetThingInput, output: GetThingOutput }\n"
        + "structure GetThingInput { id: String }\n"
        + "structure GetThingOutput { thing: Thing, doomed: String }\n"
        + "structure Thing { name: String }\n"
        + "list ThingList { member: Thing }\n";

    private static final ClassLoader LOADER = SmithyTransformChainTest.class.getClassLoader();

    @Test
    void noTransformConfig_returnsModelUnchanged(@TempDir Path dir) {
        Model model = model();
        assertThat(SmithyTransformChain.applyIfPresent(model, dir, LOADER)).isSameAs(model);
    }

    @Test
    void renameShapes_rewiresStructureAndListReferences(@TempDir Path dir) throws IOException {
        write(dir, transformConfig(
            "{ \"name\": \"renameShapes\", \"args\": { \"renamed\": "
            + "{ \"com.example#Thing\": \"com.example#DemoThing\" } } }"));

        Model out = SmithyTransformChain.applyIfPresent(model(), dir, LOADER);

        assertThat(out.getShape(ShapeId.from("com.example#DemoThing"))).isPresent();
        assertThat(out.getShape(ShapeId.from("com.example#Thing"))).isEmpty();

        // The reference rewiring C2J's RenameShapesProcessor hand-walks comes for free.
        StructureShape parent = out.expectShape(ShapeId.from("com.example#GetThingOutput"), StructureShape.class);
        assertThat(parent.getMember("thing").get().getTarget())
            .isEqualTo(ShapeId.from("com.example#DemoThing"));
        assertThat(out.expectShape(ShapeId.from("com.example#ThingList"), ListShape.class).getMember().getTarget())
            .isEqualTo(ShapeId.from("com.example#DemoThing"));
    }

    /**
     * A member id passes {@code renameShapes}' own existence check, because a member is a shape, and is
     * then ignored by the underlying model transform. The rename silently does not happen, which is why
     * {@code shapeModifiers.modify.emitPropertyName} cannot be expressed with this transform.
     */
    @Test
    void renameShapes_silentlyIgnoresMemberIds(@TempDir Path dir) throws IOException {
        write(dir, transformConfig(
            "{ \"name\": \"renameShapes\", \"args\": { \"renamed\": "
            + "{ \"com.example#GetThingOutput$doomed\": \"com.example#GetThingOutput$renamed\" } } }"));

        Model out = SmithyTransformChain.applyIfPresent(model(), dir, LOADER);

        StructureShape shape = out.expectShape(ShapeId.from("com.example#GetThingOutput"), StructureShape.class);
        assertThat(shape.getMemberNames()).contains("doomed").doesNotContain("renamed");
    }

    /**
     * Dropping a single member is expressible, but not with an {@code $member} id: the selector grammar
     * rejects {@code $} inside {@code [id=...]}, so it has to be written relationally.
     */
    @Test
    void excludeShapesBySelector_canDropASingleMember(@TempDir Path dir) throws IOException {
        write(dir, transformConfig(
            "{ \"name\": \"excludeShapesBySelector\", \"args\": { \"selector\": "
            + "\"structure[id=com.example#GetThingOutput] > member[id|member=doomed]\" } }"));

        Model out = SmithyTransformChain.applyIfPresent(model(), dir, LOADER);

        StructureShape shape = out.expectShape(ShapeId.from("com.example#GetThingOutput"), StructureShape.class);
        assertThat(shape.getMemberNames()).contains("thing").doesNotContain("doomed");
    }

    @Test
    void transformsRunInDeclaredOrder(@TempDir Path dir) throws IOException {
        write(dir, transformConfig(
            "{ \"name\": \"renameShapes\", \"args\": { \"renamed\": "
            + "{ \"com.example#Thing\": \"com.example#Middle\" } } },"
            + "{ \"name\": \"renameShapes\", \"args\": { \"renamed\": "
            + "{ \"com.example#Middle\": \"com.example#Final\" } } }"));

        Model out = SmithyTransformChain.applyIfPresent(model(), dir, LOADER);

        assertThat(out.getShape(ShapeId.from("com.example#Final"))).isPresent();
        assertThat(out.getShape(ShapeId.from("com.example#Middle"))).isEmpty();
        assertThat(out.getShape(ShapeId.from("com.example#Thing"))).isEmpty();
    }

    @Test
    void unknownTransformName_failsRatherThanBeingSkipped(@TempDir Path dir) throws IOException {
        write(dir, transformConfig("{ \"name\": \"notATransform\", \"args\": {} }"));

        assertThatThrownBy(() -> SmithyTransformChain.applyIfPresent(model(), dir, LOADER))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("notATransform");
    }

    @Test
    void missingShapeInRename_failsTheBuild(@TempDir Path dir) throws IOException {
        write(dir, transformConfig(
            "{ \"name\": \"renameShapes\", \"args\": { \"renamed\": "
            + "{ \"com.example#NoSuchShape\": \"com.example#Whatever\" } } }"));

        assertThatThrownBy(() -> SmithyTransformChain.applyIfPresent(model(), dir, LOADER))
            .hasMessageContaining("NoSuchShape");
    }

    @Test
    void wrongProjectionName_isReported(@TempDir Path dir) throws IOException {
        Files.write(dir.resolve(SmithyTransformChain.TRANSFORM_CONFIG_FILE),
                    ("{ \"version\": \"1.0\", \"projections\": { \"other\": { \"transforms\": [] } } }")
                        .getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> SmithyTransformChain.applyIfPresent(model(), dir, LOADER))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining(SmithyTransformChain.SDK_PROJECTION);
    }

    private static Model model() {
        return Model.assembler(LOADER)
                    .discoverModels(LOADER)
                    .addUnparsedModel("test.smithy", IDL)
                    .assemble()
                    .unwrap();
    }

    private static String transformConfig(String transforms) {
        return "{ \"version\": \"1.0\", \"projections\": { \"" + SmithyTransformChain.SDK_PROJECTION
               + "\": { \"transforms\": [" + transforms + "] } } }";
    }

    private static void write(Path dir, String content) throws IOException {
        Files.write(dir.resolve(SmithyTransformChain.TRANSFORM_CONFIG_FILE),
                    content.getBytes(StandardCharsets.UTF_8));
    }
}
