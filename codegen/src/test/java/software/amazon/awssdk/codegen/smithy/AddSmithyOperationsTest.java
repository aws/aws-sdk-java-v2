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

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Arrays;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.service.AuthType;
import software.amazon.awssdk.codegen.naming.DefaultSmithyNamingStrategy;
import software.amazon.awssdk.codegen.naming.NamingStrategy;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;

/**
 * Unit tests for {@link AddSmithyOperations} at the trait -> {@link OperationModel} boundary.
 */
class AddSmithyOperationsTest {

    private static final String USES =
        "use aws.api#service\n"
        + "use aws.auth#sigv4\n"
        + "use aws.protocols#restJson1\n"
        + "\n";

    private static final String SERVICE_HEADER =
        "@service(sdkId: \"Demo\", arnNamespace: \"demo\")\n"
        + "@restJson1\n"
        + "@sigv4(name: \"demo\")\n"
        + "service DemoService { version: \"2024-01-01\", operations: [Op] }\n\n";

    private static Model modelOf(String body) {
        return modelOf(SERVICE_HEADER, body);
    }

    private static Model modelOf(String header, String body) {
        return Model.assembler()
                    .discoverModels(Model.class.getClassLoader())
                    .addUnparsedModel("test.smithy",
                                      "$version: \"2.0\"\nnamespace demo\n\n" + USES + header + body)
                    .assemble()
                    .unwrap();
    }

    private static Map<String, OperationModel> translate(Model model, String protocol) {
        return translate(model, protocol, CustomizationConfig.create());
    }

    private static Map<String, OperationModel> translate(Model model, String protocol, CustomizationConfig config) {
        ServiceShape service = model.getServiceShapes().iterator().next();
        NamingStrategy naming = new DefaultSmithyNamingStrategy(model, service, CustomizationConfig.create());
        return new AddSmithyOperations(model, service, naming, config, protocol).constructOperations();
    }

    private static OperationModel op(Model model, String protocol) {
        return translate(model, protocol).get("Op");
    }

    // ---- Basic fields -----------------------------------------------------

    @Test
    void basicOperation_populatesNameProtocolInputOutput() {
        Model model = modelOf(
            "@http(method: \"POST\", uri: \"/op\")\n"
            + "operation Op { input: OpRequest, output: OpResponse }\n"
            + "structure OpRequest { id: String }\n"
            + "structure OpResponse { name: String }\n");
        OperationModel op = op(model, "rest-json");

        assertThat(op.getOperationName()).isEqualTo("Op");
        assertThat(op.getServiceProtocol()).isEqualTo("rest-json");
        assertThat(op.getInput().getVariableType()).isEqualTo("OpRequest");
        assertThat(op.getInput().getVariableName()).isEqualTo("opRequest");
        assertThat(op.getReturnType().getReturnType()).isEqualTo("OpResponse");
    }

    @Test
    void operationWithoutInputOutput_synthesizesRequestAndResponse() {
        Model model = modelOf(
            "@http(method: \"POST\", uri: \"/op\")\n"
            + "operation Op {}\n");
        OperationModel op = op(model, "rest-json");

        // Unit input/output still resolve to the request/response class names, matching C2J.
        assertThat(op.getInput().getVariableType()).isEqualTo("OpRequest");
        assertThat(op.getReturnType().getReturnType()).isEqualTo("OpResponse");
    }

    @Test
    void deprecatedOperation_setsFlagAndMessage() {
        Model model = modelOf(
            "@deprecated(message: \"use NewOp\")\n"
            + "@http(method: \"POST\", uri: \"/op\")\n"
            + "operation Op { input: OpRequest, output: OpResponse }\n"
            + "structure OpRequest {}\n"
            + "structure OpResponse {}\n");
        OperationModel op = op(model, "rest-json");

        assertThat(op.isDeprecated()).isTrue();
        assertThat(op.getDeprecatedMessage()).isEqualTo("use NewOp");
    }

    @Test
    void documentation_fromOperationTrait() {
        Model model = modelOf(
            "/// Does a thing.\n"
            + "@http(method: \"POST\", uri: \"/op\")\n"
            + "operation Op { input: OpRequest, output: OpResponse }\n"
            + "structure OpRequest {}\n"
            + "structure OpResponse {}\n");
        assertThat(op(model, "rest-json").getDocumentation()).isEqualTo("Does a thing.");
    }

    @Test
    void inputAndOutputDocumentation_fromStructureTraits() {
        Model model = modelOf(
            "@http(method: \"POST\", uri: \"/op\")\n"
            + "operation Op { input: OpRequest, output: OpResponse }\n"
            + "/// The request.\n"
            + "structure OpRequest {}\n"
            + "/// The response.\n"
            + "structure OpResponse {}\n");
        OperationModel op = op(model, "rest-json");

        assertThat(op.getInput().getDocumentation()).isEqualTo("The request.");
        assertThat(op.getReturnType().getDocumentation()).isEqualTo("The response.");
    }

    // ---- Payload flags ----------------------------------------------------

    @Test
    void blobPayloadOutput_setsHasBlobMemberAsPayload() {
        Model model = modelOf(
            "@http(method: \"POST\", uri: \"/op\")\n"
            + "operation Op { input: OpRequest, output: OpResponse }\n"
            + "structure OpRequest {}\n"
            + "structure OpResponse {\n"
            + "    @httpPayload body: Blob\n"
            + "}\n");
        OperationModel op = op(model, "rest-json");

        assertThat(op.getHasBlobMemberAsPayload()).isTrue();
        assertThat(op.getHasStringMemberAsPayload()).isFalse();
    }

    @Test
    void stringPayloadOutput_setsHasStringMemberAsPayload() {
        Model model = modelOf(
            "@http(method: \"POST\", uri: \"/op\")\n"
            + "operation Op { input: OpRequest, output: OpResponse }\n"
            + "structure OpRequest {}\n"
            + "structure OpResponse {\n"
            + "    @httpPayload body: String\n"
            + "}\n");
        OperationModel op = op(model, "rest-json");

        assertThat(op.getHasStringMemberAsPayload()).isTrue();
        assertThat(op.getHasBlobMemberAsPayload()).isFalse();
    }

    // ---- Exceptions -------------------------------------------------------

    @Test
    void exceptions_populatedWithDocAndHttpStatus() {
        Model model = modelOf(
            "@http(method: \"POST\", uri: \"/op\")\n"
            + "operation Op { input: OpRequest, output: OpResponse, errors: [Boom] }\n"
            + "structure OpRequest {}\n"
            + "structure OpResponse {}\n"
            + "/// It went boom.\n"
            + "@error(\"client\") @httpError(429)\n"
            + "structure Boom { message: String }\n");
        OperationModel op = op(model, "rest-json");

        assertThat(op.getExceptions()).hasSize(1);
        assertThat(op.getExceptions().get(0).getExceptionName()).isEqualTo("BoomException");
        assertThat(op.getExceptions().get(0).getDocumentation()).isEqualTo("It went boom.");
        assertThat(op.getExceptions().get(0).getHttpStatusCode()).isEqualTo(429);
    }

    @Test
    void exceptions_skipDeprecatedShapes() {
        Model model = modelOf(
            "@http(method: \"POST\", uri: \"/op\")\n"
            + "operation Op { input: OpRequest, output: OpResponse, errors: [Boom] }\n"
            + "structure OpRequest {}\n"
            + "structure OpResponse {}\n"
            + "@error(\"client\")\n"
            + "structure Boom { message: String }\n");
        CustomizationConfig config = CustomizationConfig.create();
        config.setDeprecatedShapes(Arrays.asList("Boom"));

        OperationModel op = translate(model, "rest-json", config).get("Op");
        assertThat(op.getExceptions()).isEmpty();
    }

    @Test
    void exceptions_mergeServiceLevelErrors() {
        Model model = modelOf(
            "@service(sdkId: \"Demo\", arnNamespace: \"demo\")\n"
            + "@restJson1\n"
            + "@sigv4(name: \"demo\")\n"
            + "service DemoService {\n"
            + "    version: \"2024-01-01\",\n"
            + "    operations: [Op],\n"
            + "    errors: [ServiceLevelError]\n"
            + "}\n\n",
            "@http(method: \"POST\", uri: \"/op\")\n"
            + "operation Op { input: OpRequest, output: OpResponse, errors: [OpError] }\n"
            + "structure OpRequest {}\n"
            + "structure OpResponse {}\n"
            + "@error(\"server\")\n"
            + "structure ServiceLevelError { message: String }\n"
            + "@error(\"client\")\n"
            + "structure OpError { message: String }\n");
        OperationModel op = op(model, "rest-json");

        assertThat(op.getExceptions()).extracting("exceptionName")
                                      .containsExactlyInAnyOrder("ServiceLevelErrorException", "OpErrorException");
    }

    // ---- Endpoint host prefix ---------------------------------------------

    @Test
    void endpointTrait_hostPrefixTranslated() {
        Model model = modelOf(
            "@endpoint(hostPrefix: \"data-\")\n"
            + "@http(method: \"POST\", uri: \"/op\")\n"
            + "operation Op { input: OpRequest, output: OpResponse }\n"
            + "structure OpRequest {}\n"
            + "structure OpResponse {}\n");
        assertThat(op(model, "rest-json").getEndpointTrait().getHostPrefix()).isEqualTo("data-");
    }

    // ---- Checksums / compression ------------------------------------------

    @Test
    void httpChecksumRequired_setsFlag() {
        Model model = modelOf(
            "@httpChecksumRequired\n"
            + "@http(method: \"POST\", uri: \"/op\")\n"
            + "operation Op { input: OpRequest, output: OpResponse }\n"
            + "structure OpRequest {}\n"
            + "structure OpResponse {}\n");
        assertThat(op(model, "rest-json").isHttpChecksumRequired()).isTrue();
    }

    @Test
    void httpChecksum_traitFieldsTranslated() {
        Model model = modelOf(
            "use aws.protocols#httpChecksum\n\n"
            + "@service(sdkId: \"Demo\", arnNamespace: \"demo\")\n"
            + "@restJson1\n"
            + "@sigv4(name: \"demo\")\n"
            + "service DemoService { version: \"2024-01-01\", operations: [Op] }\n\n",
            "@httpChecksum(requestChecksumRequired: true, requestAlgorithmMember: \"checksumAlgorithm\")\n"
            + "@http(method: \"POST\", uri: \"/op\")\n"
            + "operation Op { input: OpRequest, output: OpResponse }\n"
            + "structure OpRequest {\n"
            + "    checksumAlgorithm: ChecksumAlgorithm\n"
            + "}\n"
            + "structure OpResponse {}\n"
            + "enum ChecksumAlgorithm { CRC32 }\n");
        OperationModel op = op(model, "rest-json");

        assertThat(op.getHttpChecksum()).isNotNull();
        assertThat(op.getHttpChecksum().isRequestChecksumRequired()).isTrue();
        assertThat(op.getHttpChecksum().getRequestAlgorithmMember()).isEqualTo("checksumAlgorithm");
    }

    @Test
    void requestCompression_encodingsTranslated() {
        Model model = modelOf(
            "@requestCompression(encodings: [\"gzip\"])\n"
            + "@http(method: \"POST\", uri: \"/op\")\n"
            + "operation Op { input: OpRequest, output: OpResponse }\n"
            + "structure OpRequest {}\n"
            + "structure OpResponse {}\n");
        assertThat(op(model, "rest-json").getRequestcompression().getEncodings()).containsExactly("gzip");
    }

    // ---- Context params ---------------------------------------------------

    @Test
    void staticContextParams_valueConvertedToTreeNode() {
        Model model = modelOf(
            "@smithy.rules#staticContextParams(UseFips: { value: true })\n"
            + "@http(method: \"POST\", uri: \"/op\")\n"
            + "operation Op { input: OpRequest, output: OpResponse }\n"
            + "structure OpRequest {}\n"
            + "structure OpResponse {}\n");
        OperationModel op = op(model, "rest-json");

        assertThat(op.getStaticContextParams()).containsKey("UseFips");
        assertThat(op.getStaticContextParams().get("UseFips").getValue().toString()).isEqualTo("true");
    }

    @Test
    void operationContextParams_pathConvertedToTextNode() {
        Model model = modelOf(
            "@smithy.rules#operationContextParams(Keys: { path: \"keys[*]\" })\n"
            + "@http(method: \"POST\", uri: \"/op\")\n"
            + "operation Op { input: OpRequest, output: OpResponse }\n"
            + "structure OpRequest { keys: StringList }\n"
            + "structure OpResponse {}\n"
            + "list StringList { member: String }\n");
        OperationModel op = op(model, "rest-json");

        assertThat(op.getOperationContextParams()).containsKey("Keys");
        assertThat(((JsonNode) op.getOperationContextParams().get("Keys").getPath()).asText()).isEqualTo("keys[*]");
    }

    // ---- Auth (parity-first: operation-explicit only) ---------------------

    @Test
    void noExplicitAuth_defaultsAuthenticatedWithEmptyAuthList() {
        Model model = modelOf(
            "@http(method: \"POST\", uri: \"/op\")\n"
            + "operation Op { input: OpRequest, output: OpResponse }\n"
            + "structure OpRequest {}\n"
            + "structure OpResponse {}\n");
        OperationModel op = op(model, "rest-json");

        assertThat(op.isAuthenticated()).isTrue();
        assertThat(op.getAuthType()).isNull();
        assertThat(op.getAuth()).isEmpty();
    }

    @Test
    void optionalAuth_mapsToNoneAndUnauthenticated() {
        Model model = modelOf(
            "@optionalAuth\n"
            + "@auth([])\n"
            + "@http(method: \"POST\", uri: \"/op\")\n"
            + "operation Op { input: OpRequest, output: OpResponse }\n"
            + "structure OpRequest {}\n"
            + "structure OpResponse {}\n");
        OperationModel op = op(model, "rest-json");

        assertThat(op.getAuth()).containsExactly(AuthType.NONE);
        assertThat(op.getAuthType()).isEqualTo(AuthType.NONE);
        assertThat(op.isAuthenticated()).isFalse();
    }

    @Test
    void explicitAuthTrait_populatesAuthList() {
        Model model = modelOf(
            "@auth([sigv4])\n"
            + "@http(method: \"POST\", uri: \"/op\")\n"
            + "operation Op { input: OpRequest, output: OpResponse }\n"
            + "structure OpRequest {}\n"
            + "structure OpResponse {}\n");
        OperationModel op = op(model, "rest-json");

        assertThat(op.getAuth()).containsExactly(AuthType.V4);
        assertThat(op.getAuthType()).isEqualTo(AuthType.V4);
        assertThat(op.isAuthenticated()).isTrue();
    }

    @Test
    void unsignedPayload_setsFlag() {
        Model model = modelOf(
            "use aws.auth#unsignedPayload\n\n"
            + "@service(sdkId: \"Demo\", arnNamespace: \"demo\")\n"
            + "@restJson1\n"
            + "@sigv4(name: \"demo\")\n"
            + "service DemoService { version: \"2024-01-01\", operations: [Op] }\n\n",
            "@unsignedPayload\n"
            + "@http(method: \"POST\", uri: \"/op\")\n"
            + "operation Op { input: OpRequest, output: OpResponse }\n"
            + "structure OpRequest {}\n"
            + "structure OpResponse {}\n");
        assertThat(op(model, "rest-json").isUnsignedPayload()).isTrue();
    }

    // ---- Pagination (deferred to PR 15) -----------------------------------

    @Test
    void paginatedTrait_doesNotSetPaginated() {
        Model model = modelOf(
            "@paginated(inputToken: \"nextToken\", outputToken: \"nextToken\", items: \"items\")\n"
            + "@http(method: \"POST\", uri: \"/op\")\n"
            + "operation Op { input: OpRequest, output: OpResponse }\n"
            + "structure OpRequest { nextToken: String }\n"
            + "structure OpResponse { nextToken: String, items: StringList }\n"
            + "list StringList { member: String }\n");
        // Pagination is deferred to PR 15; isPaginated stays false here.
        assertThat(op(model, "rest-json").isPaginated()).isFalse();
    }

    // ---- Endpoint discovery -----------------------------------------------

    @Test
    void endpointDiscovery_operationFlagAndRequired() {
        Model model = Model.assembler()
            .discoverModels(Model.class.getClassLoader())
            .addUnparsedModel("test.smithy",
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
                + "structure GetItemIn {}\n"
                + "structure GetItemOut {}\n\n"
                + "@error(\"client\")\n"
                + "structure BadRequestException { message: String }\n")
            .assemble()
            .unwrap();

        Map<String, OperationModel> ops = translate(model, "rest-json");

        assertThat(ops.get("DescribeEndpoints").isEndpointOperation()).isTrue();
        assertThat(ops.get("GetItem").isEndpointOperation()).isFalse();
        assertThat(ops.get("GetItem").getEndpointDiscovery()).isNotNull();
        assertThat(ops.get("GetItem").getEndpointDiscovery().isRequired()).isTrue();
    }
}
