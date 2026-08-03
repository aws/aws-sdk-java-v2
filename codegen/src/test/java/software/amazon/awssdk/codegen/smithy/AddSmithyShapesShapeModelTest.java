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

import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.internal.TypeUtils;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.EnumModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.naming.DefaultSmithyNamingStrategy;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.HttpBinding;
import software.amazon.smithy.model.knowledge.HttpBindingIndex;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Unit tests for {@code AddSmithyShapes.generateShapeModel}.
 *
 * <p>Focuses on shape-level fields ({@link ShapeModel} attributes) —
 * per-member translations are covered by
 * {@code AddSmithyShapesMemberModelTest}.
 */
class AddSmithyShapesShapeModelTest {

    private static final String USES =
        "use aws.api#service\n"
        + "use aws.auth#sigv4\n"
        + "use aws.protocols#restJson1\n"
        + "\n";

    private static final String SERVICE_HEADER =
        "@service(sdkId: \"Demo\", arnNamespace: \"demo\")\n"
        + "@restJson1\n"
        + "@sigv4(name: \"demo\")\n"
        + "service DemoService { version: \"2024-01-01\" }\n\n";

    private static final String SERVICE_HEADER_WITH_OP =
        "@service(sdkId: \"Demo\", arnNamespace: \"demo\")\n"
        + "@restJson1\n"
        + "@sigv4(name: \"demo\")\n"
        + "service DemoService { version: \"2024-01-01\", operations: [Op] }\n\n";

    private static final class Probe extends AddSmithyShapes {
        Probe(Model model, ServiceShape service, String protocol) {
            super(model,
                  service,
                  new DefaultSmithyNamingStrategy(model, service, CustomizationConfig.create()),
                  CustomizationConfig.create(),
                  protocol,
                  new TypeUtils(new DefaultSmithyNamingStrategy(model, service, CustomizationConfig.create())));
        }

        ShapeModel translate(String javaClassName, Shape shape, Map<String, HttpBinding> bindings) {
            return generateShapeModel(javaClassName, shape, bindings);
        }
    }

    private static Model modelOf(String header, String body) {
        return Model.assembler()
                    .discoverModels(Model.class.getClassLoader())
                    .addUnparsedModel("test.smithy",
                                      "$version: \"2.0\"\nnamespace demo\n\n" + USES + header + body)
                    .assemble()
                    .unwrap();
    }

    private static Model modelOf(String body) {
        return modelOf(SERVICE_HEADER, body);
    }

    private static Model modelWithOp(String body) {
        return modelOf(SERVICE_HEADER_WITH_OP, body);
    }

    private static Probe probe(Model model, String protocol) {
        return new Probe(model, model.getServiceShapes().iterator().next(), protocol);
    }

    private static Shape shape(Model model, String name) {
        return model.expectShape(ShapeId.from("demo#" + name));
    }

    // ---- Basic structure --------------------------------------------------

    @Test
    void structure_populatesNamesAndDocumentation() {
        Model model = modelOf(
            "/// A widget structure.\n"
            + "structure Widget {\n"
            + "    id: String\n"
            + "    name: String\n"
            + "}\n");
        ShapeModel sm = probe(model, "rest-json").translate("Widget", shape(model, "Widget"), null);

        assertThat(sm.getC2jName()).isEqualTo("Widget");
        assertThat(sm.getShapeName()).isEqualTo("Widget");
        assertThat(sm.getDocumentation()).isEqualTo("A widget structure.");
        assertThat(sm.getMembers()).extracting("c2jName")
                                   .containsExactlyInAnyOrder("id", "name");
    }

    @Test
    void nullDocumentation_normalizedToEmptyStringByModel() {
        // ShapeModel.setDocumentation normalizes null → "" via the codegen's
        // escape/normalize pass. This matches C2J behavior — every call site
        // passes through setDocumentation regardless of whether the source
        // model has a docstring.
        Model model = modelOf(
            "structure Widget { id: String }\n");
        ShapeModel sm = probe(model, "rest-json").translate("Widget", shape(model, "Widget"), null);

        assertThat(sm.getDocumentation()).isEqualTo("");
    }

    // ---- Required list ---------------------------------------------------

    @Test
    void requiredList_collectsMemberNamesInModelOrder() {
        Model model = modelOf(
            "structure Widget {\n"
            + "    @required id: String\n"
            + "    name: String\n"
            + "    @required kind: String\n"
            + "}\n");
        ShapeModel sm = probe(model, "rest-json").translate("Widget", shape(model, "Widget"), null);

        assertThat(sm.getRequired()).containsExactlyInAnyOrder("id", "kind");
    }

    @Test
    void requiredList_isNullWhenNoRequiredMembers() {
        Model model = modelOf(
            "structure Widget { id: String }\n");
        ShapeModel sm = probe(model, "rest-json").translate("Widget", shape(model, "Widget"), null);

        assertThat(sm.getRequired()).isNull();
    }

    // ---- Deprecation ------------------------------------------------------

    @Test
    void deprecatedShape_setsFlagAndMessage() {
        Model model = modelOf(
            "@deprecated(message: \"use NewWidget\")\n"
            + "structure Widget { id: String }\n");
        ShapeModel sm = probe(model, "rest-json").translate("Widget", shape(model, "Widget"), null);

        assertThat(sm.isDeprecated()).isTrue();
        assertThat(sm.getDeprecatedMessage()).isEqualTo("use NewWidget");
    }

    // ---- Union ------------------------------------------------------------

    @Test
    void union_setsUnionFlag() {
        Model model = modelOf(
            "union Payload {\n"
            + "    text: String\n"
            + "    number: Integer\n"
            + "}\n");
        ShapeModel sm = probe(model, "rest-json").translate("Payload", shape(model, "Payload"), null);

        assertThat(sm.isUnion()).isTrue();
    }

    // ---- Event stream detection -------------------------------------------

    @Test
    void streamingUnion_isEventStream_and_membersAreEvents() {
        Model model = modelWithOp(
            "@http(method: \"POST\", uri: \"/subscribe\")\n"
            + "operation Op { input: In, output: Out }\n"
            + "structure In {}\n"
            + "structure Out {\n"
            + "    @httpPayload events: EventStream\n"
            + "}\n"
            + "\n"
            + "@streaming\n"
            + "union EventStream {\n"
            + "    tick: TickEvent\n"
            + "    tock: TockEvent\n"
            + "}\n"
            + "structure TickEvent { seq: Integer }\n"
            + "structure TockEvent { seq: Integer }\n");

        Probe p = probe(model, "rest-json");
        ShapeModel unionModel = p.translate("EventStream", shape(model, "EventStream"), null);
        ShapeModel tickModel = p.translate("TickEvent", shape(model, "TickEvent"), null);
        ShapeModel plainModel = p.translate("In", shape(model, "In"), null);

        assertThat(unionModel.isEventStream()).isTrue();
        assertThat(tickModel.isEvent()).isTrue();
        assertThat(plainModel.isEvent()).isFalse();
        assertThat(plainModel.isEventStream()).isFalse();
    }

    // ---- Exception fields -------------------------------------------------

    @Test
    void exceptionShape_setsFaultAndRetryable() {
        Model model = modelOf(
            "@error(\"server\") @retryable(throttling: true)\n"
            + "structure Boom { message: String }\n");
        ShapeModel sm = probe(model, "rest-json").translate("BoomException", shape(model, "Boom"), null);

        assertThat(sm.isFault()).isTrue();      // @error("server") → fault
        assertThat(sm.isRetryable()).isTrue();
        assertThat(sm.isThrottling()).isTrue();
    }

    @Test
    void clientErrorShape_faultIsFalse() {
        Model model = modelOf(
            "@error(\"client\")\n"
            + "structure BadRequest { message: String }\n");
        ShapeModel sm = probe(model, "rest-json").translate("BadRequestException",
                                                            shape(model, "BadRequest"), null);

        assertThat(sm.isFault()).isFalse();
    }

    // ---- XML namespace ----------------------------------------------------

    @Test
    void xmlNamespaceOnShape_isTranslated() {
        Model model = modelOf(
            "@xmlNamespace(uri: \"https://example.com/ns\", prefix: \"ex\")\n"
            + "structure Widget { id: String }\n");
        ShapeModel sm = probe(model, "rest-xml").translate("Widget", shape(model, "Widget"), null);

        assertThat(sm.getXmlNamespace()).isNotNull();
        assertThat(sm.getXmlNamespace().getUri()).isEqualTo("https://example.com/ns");
        assertThat(sm.getXmlNamespace().getPrefix()).isEqualTo("ex");
    }

    // ---- Enum shapes ------------------------------------------------------

    @Test
    void enumShape_populatesEnumModelList() {
        Model model = modelOf(
            "enum Status { A, B }\n");
        ShapeModel sm = probe(model, "rest-json").translate("Status", shape(model, "Status"), null);

        assertThat(sm.getEnums()).extracting(EnumModel::getValue)
                                 .containsExactlyInAnyOrder("A", "B");
    }

    @Test
    void intEnumShape_isNotTranslatedAsEnum() {
        // C2J has no int-enum concept: an intEnum is written as a plain integer shape with no enum
        // values. To preserve parity we do not emit an EnumModel list for it.
        Model model = modelOf(
            "intEnum Priority {\n"
            + "    LOW = 1\n"
            + "    HIGH = 2\n"
            + "}\n");
        ShapeModel sm = probe(model, "rest-json").translate("Priority", shape(model, "Priority"), null);

        assertThat(sm.getEnums()).isNullOrEmpty();
    }

    // ---- Aggregate flags --------------------------------------------------

    @Test
    void aggregateFlags_computedFromMembers() {
        Model model = modelWithOp(
            "@http(method: \"POST\", uri: \"/things\")\n"
            + "operation Op { input: In, output: Out }\n"
            + "structure In {\n"
            + "    @httpHeader(\"X-Meta\") meta: String\n"
            + "    @httpPayload body: String\n"
            + "}\n"
            + "structure Out {\n"
            + "    @httpResponseCode status: Integer\n"
            + "    @httpHeader(\"X-Trace\") trace: String\n"
            + "}\n");

        Map<String, HttpBinding> inputBindings = HttpBindingIndex.of(model).getRequestBindings(
            model.expectShape(ShapeId.from("demo#Op"), OperationShape.class).getId());
        Map<String, HttpBinding> outputBindings = HttpBindingIndex.of(model).getResponseBindings(
            model.expectShape(ShapeId.from("demo#Op"), OperationShape.class).getId());

        Probe p = probe(model, "rest-json");
        ShapeModel inSm = p.translate("In", shape(model, "In"), inputBindings);
        ShapeModel outSm = p.translate("Out", shape(model, "Out"), outputBindings);

        assertThat(inSm.isHasHeaderMember()).isTrue();
        assertThat(inSm.isHasPayloadMember()).isTrue();

        assertThat(outSm.isHasHeaderMember()).isTrue();
        assertThat(outSm.isHasStatusCodeMember()).isTrue();
    }
}
