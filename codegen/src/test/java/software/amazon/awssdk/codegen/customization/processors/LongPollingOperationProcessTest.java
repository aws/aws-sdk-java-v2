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

    @ParameterizedTest
    @MethodSource("nonJsonProtocols")
    void postprocess_serviceInMap_serviceNotJson_throws(Protocol protocol) {
        C2jModels c2jModels = ClientTestModels.awsJsonServiceC2jModels();

        ServiceMetadata metadata = c2jModels.serviceModel().getMetadata();
        metadata.setProtocols(Collections.singletonList(protocol.getValue()));

        IntermediateModel intermediateModel = new IntermediateModelBuilder(c2jModels).build();

        Map<String, List<String>> serviceToOperations = new HashMap<>();
        serviceToOperations.put(metadata.getServiceId(), Collections.emptyList());
        LongPollingOperationProcessor processor = new LongPollingOperationProcessor(serviceToOperations);

        assertThatThrownBy(() -> processor.postprocess(intermediateModel))
            .hasMessage("Currently only AWS-JSON services can use the longPoll trait");
    }

    @Test
    void postprocess_operationNotFound_throws() {
        IntermediateModel intermediateModel = ClientTestModels.awsJsonServiceModels();

        Map<String, List<String>> serviceToOperations = new HashMap<>();
        serviceToOperations.put(intermediateModel.getMetadata().getServiceId(), Collections.singletonList("SomeOperation"));
        LongPollingOperationProcessor processor = new LongPollingOperationProcessor(serviceToOperations);

        assertThatThrownBy(() -> processor.postprocess(intermediateModel))
            .hasMessage("Operation SomeOperation not found for service Json Service");
    }

    private static Stream<Protocol> nonJsonProtocols() {
        return Stream.of(Protocol.values()).filter(p -> p != Protocol.AWS_JSON);
    }
}
