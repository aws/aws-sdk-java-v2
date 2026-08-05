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

package software.amazon.awssdk.codegen.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.junit.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.codegen.model.service.ServiceMetadata;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.ServiceIndex;
import software.amazon.smithy.model.shapes.ServiceShape;

public class ProtocolUtilsTest {

    @ParameterizedTest
    @MethodSource("protocolsValues")
    public void protocolSelection(List<String> protocols, String expectedProtocol) {
        ServiceMetadata serviceMetadata = serviceMetadata(protocols);
        String selectedProtocol = ProtocolUtils.resolveProtocol(serviceMetadata);
        assertThat(selectedProtocol).isEqualTo(expectedProtocol);
    }

    @Test
    public void emptyProtocolsWithPresentProtocol() {
        ServiceMetadata serviceMetadata = new ServiceMetadata();
        serviceMetadata.setProtocol("json");
        String selectedProtocol = ProtocolUtils.resolveProtocol(serviceMetadata);
        assertThat(selectedProtocol).isEqualTo("json");
    }

    @Test
    public void protocolsWithJson_protocolCborV2_selectsJson() {
        ServiceMetadata serviceMetadata = new ServiceMetadata();
        serviceMetadata.setProtocols(Collections.singletonList("json"));
        serviceMetadata.setProtocol("smithy-rpc-v2-cbor");
        String selectedProtocol = ProtocolUtils.resolveProtocol(serviceMetadata);
        assertThat(selectedProtocol).isEqualTo("json");
    }

    @Test
    public void protocolsWithCborV1_protocolJson_selectsCborV1() {
        ServiceMetadata serviceMetadata = new ServiceMetadata();
        serviceMetadata.setProtocols(Collections.singletonList("cbor"));
        serviceMetadata.setProtocol("json");
        String selectedProtocol = ProtocolUtils.resolveProtocol(serviceMetadata);
        assertThat(selectedProtocol).isEqualTo("cbor");
    }

    private static Stream<Arguments> protocolsValues() {
        return Stream.of(Arguments.of(Arrays.asList("smithy-rpc-v2-cbor", "json"), "smithy-rpc-v2-cbor"),
                         Arguments.of(Collections.singletonList("smithy-rpc-v2-cbor"), "smithy-rpc-v2-cbor"),
                         Arguments.of(Arrays.asList("smithy-rpc-v2-cbor", "json", "query"), "smithy-rpc-v2-cbor"),
                         Arguments.of(Arrays.asList("json", "query"), "json"),
                         Arguments.of(Collections.singletonList("query"), "query"));
    }

    private static ServiceMetadata serviceMetadata(List<String> protocols) {
        ServiceMetadata serviceMetadata = new ServiceMetadata();
        serviceMetadata.setProtocols(protocols);
        return serviceMetadata;
    }

    // ---- Smithy overload (added in PR 4) ----------------------------------

    @ParameterizedTest
    @MethodSource("smithyProtocolValues")
    public void smithyProtocolSelection(String protocolTraits, String expectedProtocol) {
        Model model = smithyModel(protocolTraits);
        ServiceShape service = model.getServiceShapes().iterator().next();
        String selected = ProtocolUtils.resolveProtocol(ServiceIndex.of(model), service);
        assertThat(selected).isEqualTo(expectedProtocol);
    }

    private static Stream<Arguments> smithyProtocolValues() {
        return Stream.of(
            Arguments.of("@restJson1", "rest-json"),
            Arguments.of("@restXml", "rest-xml"),
            Arguments.of("@awsJson1_0", "json"),
            Arguments.of("@awsJson1_1", "json"),
            Arguments.of("@ec2Query", "ec2"),
            // SQS/DynamoDB carry both awsQuery and awsJson1_0; priority order picks json.
            Arguments.of("@awsQuery\n@awsJson1_0", "json"),
            Arguments.of("@awsQuery", "query"));
    }

    private static Model smithyModel(String protocolTraits) {
        String src =
            "$version: \"2.0\"\n"
            + "namespace demo\n\n"
            + "use aws.api#service\n"
            + "use aws.auth#sigv4\n"
            + "use aws.protocols#restJson1\n"
            + "use aws.protocols#restXml\n"
            + "use aws.protocols#awsJson1_0\n"
            + "use aws.protocols#awsJson1_1\n"
            + "use aws.protocols#awsQuery\n"
            + "use aws.protocols#ec2Query\n\n"
            + "@service(sdkId: \"Demo\", arnNamespace: \"demo\")\n"
            + "@sigv4(name: \"demo\")\n"
            // awsQuery / ec2Query selectors require the service to carry @xmlNamespace.
            + "@xmlNamespace(uri: \"https://demo.example.com\")\n"
            + protocolTraits + "\n"
            + "service DemoService { version: \"2024-01-01\" }\n";
        return Model.assembler()
                    .discoverModels(Model.class.getClassLoader())
                    .addUnparsedModel("test.smithy", src)
                    .assemble()
                    .unwrap();
    }
}
