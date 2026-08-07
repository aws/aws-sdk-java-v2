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

import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.internal.TypeUtils;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeMarshaller;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.naming.DefaultSmithyNamingStrategy;
import software.amazon.awssdk.codegen.naming.NamingStrategy;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.validation.ValidatedResult;

/**
 * Tests for the rest-xml request marshaller's {@code locationName} and {@code xmlNameSpaceUri},
 * and for RPC protocols ignoring HTTP bindings. Surfaced by the route53 and kendraranking fixtures.
 */
class AddSmithyInputShapesTest {

    private static Model modelOf(String protocolUse, String serviceTraits, String body) {
        return modelOf(protocolUse, serviceTraits, body, false);
    }

    /**
     * @param tolerateAdvisories take the model even if validation events were raised, as the parity
     *                           harness does when an RPC service carries HTTP bindings.
     */
    private static Model modelOf(String protocolUse, String serviceTraits, String body, boolean tolerateAdvisories) {
        String src =
            "$version: \"2.0\"\nnamespace demo\n\n"
            + "use aws.api#service\n"
            + "use aws.auth#sigv4\n"
            + protocolUse
            + "\n"
            + "@service(sdkId: \"Demo\", arnNamespace: \"demo\")\n"
            + "@sigv4(name: \"demo\")\n"
            + serviceTraits
            + "service DemoService { version: \"2024-01-01\", operations: [BodyOp, UriOnlyOp] }\n\n"
            + body;
        ValidatedResult<Model> result = Model.assembler()
                                             .discoverModels(Model.class.getClassLoader())
                                             .addUnparsedModel("test.smithy", src)
                                             .assemble();
        return tolerateAdvisories
               ? result.getResult().orElseThrow(() -> new IllegalStateException("model did not assemble"))
               : result.unwrap();
    }

    private static Map<String, ShapeModel> inputs(Model model, String protocol) {
        ServiceShape service = model.getServiceShapes().iterator().next();
        NamingStrategy naming = new DefaultSmithyNamingStrategy(model, service, CustomizationConfig.create());
        TypeUtils typeUtils = new TypeUtils(naming);
        return new AddSmithyInputShapes(model, service, naming, CustomizationConfig.create(), protocol, typeUtils)
            .process(Collections.emptyMap(), Collections.emptyMap());
    }

    private static final String REST_XML_OPS =
        // Body-bearing request: a member serialized in the XML body plus a URI label.
        "@http(method: \"POST\", uri: \"/zone/{ZoneId}\")\n"
        + "operation BodyOp { input: BodyOpRequest, output: BodyOpResponse }\n"
        + "structure BodyOpRequest {\n"
        + "    @required @httpLabel ZoneId: String,\n"
        + "    Comment: String\n"
        + "}\n"
        + "structure BodyOpResponse {}\n"
        // URI-only request: no body members.
        + "@http(method: \"GET\", uri: \"/zone/{ZoneId}\")\n"
        + "operation UriOnlyOp { input: UriOnlyOpRequest, output: UriOnlyOpResponse }\n"
        + "structure UriOnlyOpRequest { @required @httpLabel ZoneId: String }\n"
        + "structure UriOnlyOpResponse {}\n";

    @Test
    void restXml_bodyRequest_setsLocationNameAndServiceXmlNamespace() {
        Model model = modelOf(
            "use aws.protocols#restXml\n",
            "@restXml\n@xmlNamespace(uri: \"https://demo.amazonaws.com/doc/2024-01-01/\")\n",
            REST_XML_OPS);
        ShapeMarshaller marshaller = inputs(model, "rest-xml").get("BodyOpRequest").getMarshaller();

        assertThat(marshaller.getLocationName()).isEqualTo("BodyOpRequest");
        assertThat(marshaller.getXmlNameSpaceUri()).isEqualTo("https://demo.amazonaws.com/doc/2024-01-01/");
    }

    @Test
    void restXml_uriOnlyRequest_hasNoLocationNameOrNamespace() {
        Model model = modelOf(
            "use aws.protocols#restXml\n",
            "@restXml\n@xmlNamespace(uri: \"https://demo.amazonaws.com/doc/2024-01-01/\")\n",
            REST_XML_OPS);
        ShapeMarshaller marshaller = inputs(model, "rest-xml").get("UriOnlyOpRequest").getMarshaller();

        // No body member -> no XML root element name / namespace, matching C2J, which authors
        // input.locationName / input.xmlNamespace only on body-bearing operations.
        assertThat(marshaller.getLocationName()).isNull();
        assertThat(marshaller.getXmlNameSpaceUri()).isNull();
    }

    @Test
    void restXml_bodyRequest_honorsXmlNameOverrideForRootElement() {
        Model model = modelOf(
            "use aws.protocols#restXml\n",
            "@restXml\n@xmlNamespace(uri: \"https://demo.amazonaws.com/doc/2024-01-01/\")\n",
            "@http(method: \"POST\", uri: \"/zone/{ZoneId}\")\n"
            + "operation BodyOp { input: BodyOpRequest, output: BodyOpResponse }\n"
            + "@xmlName(\"CustomRoot\")\n"
            + "structure BodyOpRequest { @required @httpLabel ZoneId: String, Comment: String }\n"
            + "structure BodyOpResponse {}\n"
            + "@http(method: \"GET\", uri: \"/zone/{ZoneId}\")\n"
            + "operation UriOnlyOp { input: UriOnlyOpRequest, output: UriOnlyOpResponse }\n"
            + "structure UriOnlyOpRequest { @required @httpLabel ZoneId: String }\n"
            + "structure UriOnlyOpResponse {}\n");
        ShapeMarshaller marshaller = inputs(model, "rest-xml").get("BodyOpRequest").getMarshaller();

        assertThat(marshaller.getLocationName()).isEqualTo("CustomRoot");
    }

    @Test
    void rpcProtocol_ignoresHttpBindings_postToRootWithBodyMembers() {
        // An RPC service may still carry @http/@httpLabel; the bindings must be ignored, so every
        // member goes in the body. The rpcv2Cbor URI is applied later by the deferred processor.
        Model model = modelOf(
            "use smithy.protocols#rpcv2Cbor\n",
            "@rpcv2Cbor\n",
            "@http(method: \"DELETE\", uri: \"/plan/{Id}\")\n"
            + "operation BodyOp { input: BodyOpRequest, output: BodyOpResponse }\n"
            + "structure BodyOpRequest { @required @httpLabel Id: String, Comment: String }\n"
            + "structure BodyOpResponse {}\n"
            + "@http(method: \"GET\", uri: \"/plan/{Id}\")\n"
            + "operation UriOnlyOp { input: UriOnlyOpRequest, output: UriOnlyOpResponse }\n"
            + "structure UriOnlyOpRequest { @required @httpLabel Id: String }\n"
            + "structure UriOnlyOpResponse {}\n",
            true);
        ShapeModel req = inputs(model, "smithy-rpc-v2-cbor").get("BodyOpRequest");

        assertThat(req.getMarshaller().getVerb()).isEqualTo("POST");
        assertThat(req.getMarshaller().getRequestUri()).isEqualTo("/");

        MemberModel id = req.getMembersAsMap().get("Id");
        // @httpLabel ignored: no URI location, and the wire name is the member name, not a label.
        assertThat(id.getHttp().getLocation()).isNull();
        assertThat(id.getHttp().getMarshallLocationName()).isEqualTo("Id");
    }

    @Test
    void discoveredEndpointOperation_setsEndpointDiscoveryOnRequestShape() {
        // SyncClientClass/AsyncClientClass dereference this off the request shape, guarded only by
        // the operation-level field, so a null here NPEs codegen.
        Model model = Model.assembler()
            .discoverModels(Model.class.getClassLoader())
            .addUnparsedModel("discovery.smithy",
                "$version: \"2.0\"\nnamespace demo\n\n"
                + "use aws.api#service\n"
                + "use aws.auth#sigv4\n"
                + "use aws.protocols#restJson1\n"
                + "use aws.api#clientEndpointDiscovery\n"
                + "use aws.api#clientDiscoveredEndpoint\n\n"
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
                + "structure GetItemIn { Key: String }\n"
                + "structure GetItemOut {}\n\n"
                + "@error(\"client\")\n"
                + "structure BadRequestException { message: String }\n")
            .assemble()
            .unwrap();

        Map<String, ShapeModel> shapes = inputs(model, "rest-json");

        assertThat(shapes.get("GetItemRequest").getEndpointDiscovery()).isNotNull();
        assertThat(shapes.get("GetItemRequest").getEndpointDiscovery().isRequired()).isTrue();
        // The discovery provider is not itself a consumer.
        assertThat(shapes.get("DescribeEndpointsRequest").getEndpointDiscovery()).isNull();
    }

    @Test
    void nonRestXmlProtocol_doesNotSetServiceNamespaceOnMarshaller() {
        // ec2Query carries a service @xmlNamespace, but C2J does not propagate it to the request
        // marshaller; only rest-xml does. Guards against regressing the ec2 fixture.
        Model model = modelOf(
            "use aws.protocols#ec2Query\n",
            "@ec2Query\n@xmlNamespace(uri: \"https://demo.amazonaws.com/doc/2024-01-01/\")\n",
            REST_XML_OPS);
        ShapeMarshaller marshaller = inputs(model, "ec2").get("BodyOpRequest").getMarshaller();

        assertThat(marshaller.getLocationName()).isNull();
        assertThat(marshaller.getXmlNameSpaceUri()).isNull();
    }
}
