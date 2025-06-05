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
    public void protocolsWithJson_ProtocolCbor_selectsJson() {
        ServiceMetadata serviceMetadata = new ServiceMetadata();
        serviceMetadata.setProtocols(Collections.singletonList("json"));
        serviceMetadata.setProtocol("smithy-rpc-v2-cbor");
        String selectedProtocol = ProtocolUtils.resolveProtocol(serviceMetadata);
        assertThat(selectedProtocol).isEqualTo("json");
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
}
