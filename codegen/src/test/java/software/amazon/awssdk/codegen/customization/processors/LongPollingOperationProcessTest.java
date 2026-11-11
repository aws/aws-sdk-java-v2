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

package software.amazon.awssdk.codegen.customization.processors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.codegen.C2jModels;
import software.amazon.awssdk.codegen.IntermediateModelBuilder;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.Protocol;
import software.amazon.awssdk.codegen.model.service.ServiceMetadata;
import software.amazon.awssdk.codegen.poet.ClientTestModels;

public class LongPollingOperationProcessTest {
    private static final String LONG_POLLING_OPERATION = "APostOperation";

    @ParameterizedTest
    @MethodSource("nonJsonProtocols")
    void postprocess_serviceInMap_serviceNotJson_throws(Protocol protocol) {
        IntermediateModel intermediateModel = intermediateModelWithProtocol(protocol);

        LongPollingOperationProcessor processor = processorFor(intermediateModel, Collections.emptyList());

        assertThatThrownBy(() -> processor.postprocess(intermediateModel))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Currently only JSON-based protocol services (AWS JSON, REST JSON, CBOR, Smithy RPC v2 CBOR) can use "
                        + "the longPoll trait. Service Json Service uses protocol: " + protocol.getValue());
    }

    @ParameterizedTest
    @MethodSource("jsonProtocols")
    void postprocess_serviceInMap_serviceJson_setsLongPolling(Protocol protocol) {
        IntermediateModel intermediateModel = intermediateModelWithProtocol(protocol);
        assertThat(intermediateModel.getMetadata().getProtocol()).isEqualTo(protocol);

        LongPollingOperationProcessor processor =
            processorFor(intermediateModel, Collections.singletonList(LONG_POLLING_OPERATION));

        processor.postprocess(intermediateModel);

        assertThat(intermediateModel.getOperation(LONG_POLLING_OPERATION).isLongPolling()).isTrue();
    }

    @Test
    void postprocess_operationNotFound_throws() {
        IntermediateModel intermediateModel = ClientTestModels.awsJsonServiceModels();

        LongPollingOperationProcessor processor =
            processorFor(intermediateModel, Collections.singletonList("SomeOperation"));

        assertThatThrownBy(() -> processor.postprocess(intermediateModel))
            .hasMessage("Operation SomeOperation not found for service Json Service");
    }

    private static IntermediateModel intermediateModelWithProtocol(Protocol protocol) {
        C2jModels c2jModels = ClientTestModels.awsJsonServiceC2jModels();

        ServiceMetadata metadata = c2jModels.serviceModel().getMetadata();
        metadata.setProtocols(Collections.singletonList(protocol.getValue()));

        return new IntermediateModelBuilder(c2jModels).build();
    }

    private static LongPollingOperationProcessor processorFor(IntermediateModel intermediateModel, List<String> operations) {
        Map<String, List<String>> serviceToOperations = new HashMap<>();
        serviceToOperations.put(intermediateModel.getMetadata().getServiceId(), operations);
        return new LongPollingOperationProcessor(serviceToOperations);
    }

    private static Stream<Protocol> nonJsonProtocols() {
        return Stream.of(Protocol.values()).filter(p -> !isJsonProtocol(p));
    }

    private static Stream<Protocol> jsonProtocols() {
        return Stream.of(Protocol.values()).filter(LongPollingOperationProcessTest::isJsonProtocol);
    }

    private static boolean isJsonProtocol(Protocol protocol) {
        return protocol == Protocol.SMITHY_RPC_V2_CBOR
               || protocol == Protocol.CBOR
               || protocol == Protocol.AWS_JSON
               || protocol == Protocol.REST_JSON;
    }
}
