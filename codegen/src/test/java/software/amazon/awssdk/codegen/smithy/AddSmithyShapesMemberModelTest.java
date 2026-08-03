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
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.service.Location;
import software.amazon.awssdk.codegen.naming.DefaultSmithyNamingStrategy;
import software.amazon.awssdk.codegen.naming.NamingStrategy;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.HttpBinding;
import software.amazon.smithy.model.knowledge.HttpBindingIndex;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;

/**
 * Unit tests for {@code AddSmithyShapes.generateMemberModel}.
 *
 * <p>Each test constructs a tiny Smithy model containing a service and the
 * shape(s) under test, invokes the translator via a package-private probe
 * subclass, and asserts specific {@link MemberModel} fields. Nothing runs
 * through the full IntermediateModel builder — that first end-to-end
 * signal is PR 5's job.
 */
class AddSmithyShapesMemberModelTest {

    private static final String USES =
        "use aws.api#service\n"
        + "use aws.auth#sigv4\n"
        + "use aws.protocols#restJson1\n"
        + "use aws.protocols#ec2QueryName\n"
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

    /**
     * Concrete probe subclass — {@code AddSmithyShapes} is abstract so tests
     * need a way to invoke the protected translator without a real processor.
     */
    private static final class Probe extends AddSmithyShapes {
        Probe(Model model, ServiceShape service, String protocol) {
            super(model,
                  service,
                  new DefaultSmithyNamingStrategy(model, service, CustomizationConfig.create()),
                  CustomizationConfig.create(),
                  protocol,
                  new TypeUtils(namingStrategyFor(model, service)));
        }

        MemberModel translate(MemberShape member,
                              Shape parentShape,
                              Map<String, HttpBinding> bindings) {
            return generateMemberModel(member, parentShape, bindings);
        }

        private static NamingStrategy namingStrategyFor(Model model, ServiceShape service) {
            return new DefaultSmithyNamingStrategy(model, service, CustomizationConfig.create());
        }
    }

    /**
     * Builds a Smithy model wrapping {@code body} in a demo service.
     */
    private static Model modelOf(String body) {
        return modelOf(SERVICE_HEADER, body);
    }

    private static Model modelWithOp(String body) {
        return modelOf(SERVICE_HEADER_WITH_OP, body);
    }

    private static Model modelOf(String header, String body) {
        return Model.assembler()
                    .discoverModels(Model.class.getClassLoader())
                    .addUnparsedModel("test.smithy",
                                      "$version: \"2.0\"\nnamespace demo\n\n" + USES + header + body)
                    .assemble()
                    .unwrap();
    }

    private static Probe probe(Model model, String protocol) {
        return new Probe(model, model.getServiceShapes().iterator().next(), protocol);
    }

    private static StructureShape struct(Model model, String name) {
        return model.expectShape(ShapeId.from("demo#" + name), StructureShape.class);
    }

    private static Map<String, HttpBinding> requestBindings(Model model, String opName) {
        return HttpBindingIndex.of(model).getRequestBindings(
            model.expectShape(ShapeId.from("demo#" + opName), OperationShape.class).getId());
    }

    // ---- Naming, scalars, documentation -----------------------------------

    @Test
    void scalarMember_populatesNamesAndVariableModel() {
        Model model = modelOf(
            "structure Parent {\n"
            + "    /// A friendly description.\n"
            + "    tableName: String\n"
            + "}\n");
        Probe probe = probe(model, "rest-json");

        StructureShape parent = struct(model, "Parent");
        MemberShape member = parent.getMember("tableName").get();

        MemberModel mm = probe.translate(member, parent, null);

        assertThat(mm.getC2jName()).isEqualTo("tableName");
        assertThat(mm.getC2jShape()).isEqualTo("String");
        assertThat(mm.getName()).isEqualTo("TableName");
        assertThat(mm.getVariable().getVariableName()).isEqualTo("tableName");
        assertThat(mm.getVariable().getVariableType()).isEqualTo("String");
        assertThat(mm.getDocumentation()).isEqualTo("A friendly description.");
    }

    // ---- Required / nullable ----------------------------------------------

    @Test
    void requiredMember_setRequiredTrue() {
        Model model = modelOf(
            "structure Parent {\n"
            + "    @required\n"
            + "    id: String\n"
            + "}\n");
        MemberModel mm = probe(model, "rest-json").translate(
            struct(model, "Parent").getMember("id").get(), struct(model, "Parent"), null);
        assertThat(mm.isRequired()).isTrue();
    }

    @Test
    void optionalMember_setRequiredFalse() {
        Model model = modelOf(
            "structure Parent { id: String }\n");
        MemberModel mm = probe(model, "rest-json").translate(
            struct(model, "Parent").getMember("id").get(), struct(model, "Parent"), null);
        assertThat(mm.isRequired()).isFalse();
    }

    // ---- Deprecated -------------------------------------------------------

    @Test
    void deprecatedMember_carriesFlagAndMessage() {
        Model model = modelOf(
            "structure Parent {\n"
            + "    @deprecated(message: \"use newerField\")\n"
            + "    oldField: String\n"
            + "}\n");
        MemberModel mm = probe(model, "rest-json").translate(
            struct(model, "Parent").getMember("oldField").get(), struct(model, "Parent"), null);
        assertThat(mm.isDeprecated()).isTrue();
        assertThat(mm.getDeprecatedMessage()).isEqualTo("use newerField");
    }

    // ---- Sensitive --------------------------------------------------------

    // Smithy's @sensitive trait applies to shapes only (not members). The
    // translator's isSensitive helper checks the member for parity with C2J
    // (where sensitive could appear on either), but real Smithy models
    // express sensitivity on the target shape only.
    @Test
    void sensitiveOnTarget_carriesFlag() {
        Model model = modelOf(
            "@sensitive string SecretString\n"
            + "structure Parent { secret: SecretString }\n");
        MemberModel mm = probe(model, "rest-json").translate(
            struct(model, "Parent").getMember("secret").get(), struct(model, "Parent"), null);
        assertThat(mm.isSensitive()).isTrue();
    }

    @Test
    void sensitiveOnListElementTarget_carriesFlag() {
        Model model = modelOf(
            "@sensitive string SecretString\n"
            + "list SecretList { member: SecretString }\n"
            + "structure Parent { secrets: SecretList }\n");
        MemberModel mm = probe(model, "rest-json").translate(
            struct(model, "Parent").getMember("secrets").get(), struct(model, "Parent"), null);
        assertThat(mm.isSensitive()).isTrue();
    }

    // ---- Idempotency token ------------------------------------------------

    @Test
    void idempotencyToken_onStringMember_isMarked() {
        Model model = modelOf(
            "structure Parent {\n"
            + "    @idempotencyToken\n"
            + "    token: String\n"
            + "}\n");
        MemberModel mm = probe(model, "rest-json").translate(
            struct(model, "Parent").getMember("token").get(), struct(model, "Parent"), null);
        assertThat(mm.isIdempotencyToken()).isTrue();
    }

    // Note: Smithy's own IdempotencyTokenTrait selector already rejects
    // non-string targets at model-assembly time, so the runtime check inside
    // generateMemberModel is a defense-in-depth guard rather than a
    // reachable failure path. No unit test needed here.

    // ---- Timestamp format translation -------------------------------------

    @Test
    void timestampFormat_smithyNamesTranslateToC2jNames() {
        Model model = modelOf(
            "structure Parent {\n"
            + "    @timestampFormat(\"date-time\")   iso: Timestamp\n"
            + "    @timestampFormat(\"epoch-seconds\") epoch: Timestamp\n"
            + "    @timestampFormat(\"http-date\")   http: Timestamp\n"
            + "}\n");
        Probe p = probe(model, "rest-json");
        StructureShape parent = struct(model, "Parent");

        assertThat(p.translate(parent.getMember("iso").get(), parent, null).getTimestampFormat())
            .isEqualTo("iso8601");
        assertThat(p.translate(parent.getMember("epoch").get(), parent, null).getTimestampFormat())
            .isEqualTo("unixTimestamp");
        assertThat(p.translate(parent.getMember("http").get(), parent, null).getTimestampFormat())
            .isEqualTo("rfc822");
    }

    // ---- Enum type reference ----------------------------------------------

    @Test
    void enumTarget_setsEnumTypeAndKeepsVariableTypeAsString() {
        Model model = modelOf(
            "enum Status { A, B }\n"
            + "structure Parent { status: Status }\n");
        MemberModel mm = probe(model, "rest-json").translate(
            struct(model, "Parent").getMember("status").get(), struct(model, "Parent"), null);

        assertThat(mm.getEnumType()).isEqualTo("Status");
        assertThat(mm.getVariable().getVariableType()).isEqualTo("String");
    }

    @Test
    void intEnumTarget_treatedAsPlainInteger_noEnumType() {
        // C2J has no int-enum concept: an intEnum target is a plain integer. The member must be a
        // plain Integer with no enumType, matching what C2J produces for the same field.
        Model model = modelOf(
            "intEnum Priority {\n"
            + "    LOW = 1\n"
            + "    HIGH = 2\n"
            + "}\n"
            + "structure Parent { priority: Priority }\n");
        MemberModel mm = probe(model, "rest-json").translate(
            struct(model, "Parent").getMember("priority").get(), struct(model, "Parent"), null);

        assertThat(mm.getEnumType()).isNull();
        assertThat(mm.getVariable().getVariableType()).isEqualTo("Integer");
    }

    // ---- List / map -------------------------------------------------------

    @Test
    void listTarget_populatesListModelWithElementMember() {
        Model model = modelOf(
            "list StringList { member: String }\n"
            + "structure Parent { items: StringList }\n");
        MemberModel mm = probe(model, "rest-json").translate(
            struct(model, "Parent").getMember("items").get(), struct(model, "Parent"), null);

        assertThat(mm.getListModel()).isNotNull();
        assertThat(mm.getListModel().getMemberType()).isEqualTo("String");
        assertThat(mm.getListModel().getListMemberModel().getC2jName()).isEqualTo("member");
    }

    @Test
    void mapTarget_populatesMapModelWithKeyAndValueMembers() {
        Model model = modelOf(
            "map StringMap { key: String, value: String }\n"
            + "structure Parent { entries: StringMap }\n");
        MemberModel mm = probe(model, "rest-json").translate(
            struct(model, "Parent").getMember("entries").get(), struct(model, "Parent"), null);

        assertThat(mm.getMapModel()).isNotNull();
        assertThat(mm.getMapModel().getKeyModel().getC2jName()).isEqualTo("key");
        assertThat(mm.getMapModel().getValueModel().getC2jName()).isEqualTo("value");
    }

    // ---- HTTP bindings ----------------------------------------------------

    @Test
    void httpHeader_locationAndName_populated() {
        Model model = modelWithOp(
            "@http(method: \"GET\", uri: \"/things\")\n"
            + "operation Op { input: In, output: Out }\n"
            + "structure In {\n"
            + "    @httpHeader(\"X-Trace-Id\")\n"
            + "    trace: String\n"
            + "}\n"
            + "structure Out {}\n");
        MemberModel mm = probe(model, "rest-json").translate(
            struct(model, "In").getMember("trace").get(),
            struct(model, "In"),
            requestBindings(model, "Op"));

        assertThat(mm.getHttp().getLocation()).isEqualTo(Location.HEADER);
        assertThat(mm.getHttp().getMarshallLocationName()).isEqualTo("X-Trace-Id");
        assertThat(mm.getHttp().getUnmarshallLocationName()).isEqualTo("X-Trace-Id");
    }

    @Test
    void httpLabel_mapsToUriLocation() {
        Model model = modelWithOp(
            "@http(method: \"GET\", uri: \"/things/{id}\")\n"
            + "operation Op { input: In, output: Out }\n"
            + "structure In {\n"
            + "    @required @httpLabel\n"
            + "    id: String\n"
            + "}\n"
            + "structure Out {}\n");
        MemberModel mm = probe(model, "rest-json").translate(
            struct(model, "In").getMember("id").get(),
            struct(model, "In"),
            requestBindings(model, "Op"));

        assertThat(mm.getHttp().getLocation()).isEqualTo(Location.URI);
    }

    @Test
    void httpPayload_setsPayloadFlagAndClearsLocation() {
        Model model = modelWithOp(
            "@http(method: \"PUT\", uri: \"/things\")\n"
            + "operation Op { input: In, output: Out }\n"
            + "structure In {\n"
            + "    @httpPayload\n"
            + "    body: String\n"
            + "}\n"
            + "structure Out {}\n");
        MemberModel mm = probe(model, "rest-json").translate(
            struct(model, "In").getMember("body").get(),
            struct(model, "In"),
            requestBindings(model, "Op"));

        assertThat(mm.getHttp().getIsPayload()).isTrue();
        assertThat(mm.getHttp().getLocation()).isNull();
    }

    @Test
    void nestedMember_noBindings_hasNoLocation() {
        Model model = modelOf(
            "structure Parent { child: String }\n");
        MemberModel mm = probe(model, "rest-json").translate(
            struct(model, "Parent").getMember("child").get(), struct(model, "Parent"), null);

        assertThat(mm.getHttp().getLocation()).isNull();
        assertThat(mm.getHttp().getIsPayload()).isFalse();
        assertThat(mm.getHttp().getMarshallLocationName()).isEqualTo("child");
    }

    // ---- Streaming / flatten / requiresLength -----------------------------

    @Test
    void streamingBlobTarget_setsStreamingFlag() {
        Model model = modelOf(
            "@streaming blob StreamBlob\n"
            + "structure Parent {\n"
            + "    @required\n"
            + "    body: StreamBlob\n"
            + "}\n");
        MemberModel mm = probe(model, "rest-json").translate(
            struct(model, "Parent").getMember("body").get(), struct(model, "Parent"), null);

        assertThat(mm.getHttp().getIsStreaming()).isTrue();
    }

    @Test
    void xmlFlattenedTrait_setsFlattenedFlag() {
        Model model = modelOf(
            "list StringList { member: String }\n"
            + "structure Parent {\n"
            + "    @xmlFlattened\n"
            + "    items: StringList\n"
            + "}\n");
        MemberModel mm = probe(model, "rest-xml").translate(
            struct(model, "Parent").getMember("items").get(), struct(model, "Parent"), null);

        assertThat(mm.getHttp().isFlattened()).isTrue();
    }

    // ---- Wire-name overrides ----------------------------------------------

    @Test
    void jsonName_overridesMarshallAndUnmarshallLocationName_forRestJson() {
        Model model = modelOf(
            "structure Parent {\n"
            + "    @jsonName(\"my_field\")\n"
            + "    myField: String\n"
            + "}\n");
        MemberModel mm = probe(model, "rest-json").translate(
            struct(model, "Parent").getMember("myField").get(), struct(model, "Parent"), null);

        assertThat(mm.getHttp().getMarshallLocationName()).isEqualTo("my_field");
        assertThat(mm.getHttp().getUnmarshallLocationName()).isEqualTo("my_field");
    }

    @Test
    void xmlName_overridesLocationName_forRestXml() {
        Model model = modelOf(
            "structure Parent {\n"
            + "    @xmlName(\"MyField\")\n"
            + "    myField: String\n"
            + "}\n");
        MemberModel mm = probe(model, "rest-xml").translate(
            struct(model, "Parent").getMember("myField").get(), struct(model, "Parent"), null);

        assertThat(mm.getHttp().getMarshallLocationName()).isEqualTo("MyField");
    }

    // ---- EC2 uppercase-first-char convention ------------------------------

    @Test
    void ec2Protocol_uppercasesFirstCharOfMemberName() {
        Model model = modelOf(
            "structure Parent { instanceId: String }\n");
        MemberModel mm = probe(model, "ec2").translate(
            struct(model, "Parent").getMember("instanceId").get(), struct(model, "Parent"), null);

        assertThat(mm.getHttp().getMarshallLocationName()).isEqualTo("InstanceId");
    }

    @Test
    void ec2Protocol_prefersEc2QueryNameTrait() {
        Model model = modelOf(
            "structure Parent {\n"
            + "    @ec2QueryName(\"OverrideName\")\n"
            + "    instanceId: String\n"
            + "}\n");
        MemberModel mm = probe(model, "ec2").translate(
            struct(model, "Parent").getMember("instanceId").get(), struct(model, "Parent"), null);

        assertThat(mm.getHttp().getMarshallLocationName()).isEqualTo("OverrideName");
    }

    @Test
    void ec2Protocol_marshallUppercasesXmlNameStem_whenNoEc2QueryName() {
        // Member name and @xmlName stems differ (plural vs singular). C2J upper-cases the
        // locationName (@xmlName), not the member name: TagSpecification, not TagSpecifications.
        Model model = modelOf(
            "list TagSpecificationList { member: String }\n"
            + "structure Parent {\n"
            + "    @xmlName(\"TagSpecification\")\n"
            + "    tagSpecifications: TagSpecificationList\n"
            + "}\n");
        MemberModel mm = probe(model, "ec2").translate(
            struct(model, "Parent").getMember("tagSpecifications").get(), struct(model, "Parent"), null);

        assertThat(mm.getHttp().getMarshallLocationName()).isEqualTo("TagSpecification");
    }

    @Test
    void ec2Protocol_unmarshallUsesXmlNameVerbatim() {
        // EC2 response element names come from @xmlName (C2J locationName), lower-cased on the wire.
        Model model = modelOf(
            "structure Parent {\n"
            + "    @xmlName(\"min\")\n"
            + "    min: Integer\n"
            + "}\n");
        MemberModel mm = probe(model, "ec2").translate(
            struct(model, "Parent").getMember("min").get(), struct(model, "Parent"), null);

        assertThat(mm.getHttp().getUnmarshallLocationName()).isEqualTo("min");
        // Marshall still upper-cases the xmlName stem.
        assertThat(mm.getHttp().getMarshallLocationName()).isEqualTo("Min");
    }

    // ---- Event stream member traits ---------------------------------------

    @Test
    void eventPayloadAndEventHeader_traitsSurfaceOnMemberModel() {
        Model model = modelOf(
            "structure Event {\n"
            + "    @eventHeader header: String\n"
            + "    @eventPayload payload: String\n"
            + "}\n");
        Probe p = probe(model, "rest-json");
        StructureShape parent = struct(model, "Event");

        assertThat(p.translate(parent.getMember("header").get(), parent, null).isEventHeader())
            .isTrue();
        assertThat(p.translate(parent.getMember("payload").get(), parent, null).isEventPayload())
            .isTrue();
    }

    // ---- XML attribute / namespace ----------------------------------------

    @Test
    void xmlAttributeTrait_carriesFlag() {
        Model model = modelOf(
            "structure Parent {\n"
            + "    @xmlAttribute\n"
            + "    id: String\n"
            + "}\n");
        MemberModel mm = probe(model, "rest-xml").translate(
            struct(model, "Parent").getMember("id").get(), struct(model, "Parent"), null);

        assertThat(mm.isXmlAttribute()).isTrue();
    }

    @Test
    void xmlNamespaceOnMember_setsUri() {
        Model model = modelOf(
            "structure Parent {\n"
            + "    @xmlNamespace(uri: \"https://example.com/ns\")\n"
            + "    inner: String\n"
            + "}\n");
        MemberModel mm = probe(model, "rest-xml").translate(
            struct(model, "Parent").getMember("inner").get(), struct(model, "Parent"), null);

        assertThat(mm.getXmlNameSpaceUri()).isEqualTo("https://example.com/ns");
    }

    // ---- Endpoint rules contextParam (member level, added in PR 4) --------

    @Test
    void contextParam_setsNameFromTrait() {
        Model model = modelWithOp(
            "@http(method: \"POST\", uri: \"/op\")\n"
            + "operation Op { input: In, output: Out }\n"
            + "structure In {\n"
            + "    @smithy.rules#contextParam(name: \"TableName\")\n"
            + "    table: String\n"
            + "}\n"
            + "structure Out {}\n");
        MemberModel mm = probe(model, "rest-json").translate(
            struct(model, "In").getMember("table").get(), struct(model, "In"),
            requestBindings(model, "Op"));

        assertThat(mm.getContextParam()).isNotNull();
        assertThat(mm.getContextParam().getName()).isEqualTo("TableName");
    }

    @Test
    void noContextParam_leavesFieldNull() {
        Model model = modelOf(
            "structure Parent { id: String }\n");
        MemberModel mm = probe(model, "rest-json").translate(
            struct(model, "Parent").getMember("id").get(), struct(model, "Parent"), null);

        assertThat(mm.getContextParam()).isNull();
    }

    // ---- Endpoint discovery id (member level, added in PR 4) --------------

    @Test
    void endpointDiscoveryId_markedFromTrait() {
        // A full endpoint-discovery setup is required for the model to validate:
        // the service declares @clientEndpointDiscovery, the operation is
        // @clientDiscoveredEndpoint, and the input member carries the id trait.
        Model model = Model.assembler()
            .discoverModels(Model.class.getClassLoader())
            .addUnparsedModel("test.smithy",
                "$version: \"2.0\"\nnamespace demo\n\n"
                + "use aws.api#service\n"
                + "use aws.auth#sigv4\n"
                + "use aws.protocols#restJson1\n"
                + "use aws.api#clientEndpointDiscovery\n"
                + "use aws.api#clientDiscoveredEndpoint\n"
                + "use aws.api#clientEndpointDiscoveryId\n\n"
                + "@service(sdkId: \"Demo\", arnNamespace: \"demo\")\n"
                + "@restJson1\n"
                + "@sigv4(name: \"demo\")\n"
                + "@clientEndpointDiscovery(operation: DescribeEndpoints, error: BadRequestException)\n"
                + "service DemoService { version: \"2024-01-01\", operations: [DescribeEndpoints, GetItem] }\n\n"
                + "@http(method: \"POST\", uri: \"/endpoints\")\n"
                + "operation DescribeEndpoints {\n"
                + "    input: DescribeEndpointsIn,\n"
                + "    output: DescribeEndpointsOut,\n"
                + "    errors: [BadRequestException]\n"
                + "}\n"
                + "structure DescribeEndpointsIn {}\n"
                + "structure DescribeEndpointsOut { @required Endpoints: Endpoints }\n"
                + "list Endpoints { member: Endpoint }\n"
                + "structure Endpoint {\n"
                + "    @required Address: String\n"
                + "    @required CachePeriodInMinutes: Long\n"
                + "}\n\n"
                + "@clientDiscoveredEndpoint(required: true)\n"
                + "@http(method: \"POST\", uri: \"/get\")\n"
                + "operation GetItem { input: GetItemIn, output: GetItemOut, errors: [BadRequestException] }\n"
                + "structure GetItemIn {\n"
                + "    @required\n"
                + "    @clientEndpointDiscoveryId\n"
                + "    key: String\n"
                + "}\n"
                + "structure GetItemOut {}\n\n"
                + "@error(\"client\")\n"
                + "structure BadRequestException { message: String }\n")
            .assemble()
            .unwrap();

        StructureShape getItemIn = struct(model, "GetItemIn");
        MemberModel mm = probe(model, "rest-json").translate(
            getItemIn.getMember("key").get(), getItemIn,
            HttpBindingIndex.of(model).getRequestBindings(ShapeId.from("demo#GetItem")));

        assertThat(mm.isEndpointDiscoveryId()).isTrue();
    }
}
