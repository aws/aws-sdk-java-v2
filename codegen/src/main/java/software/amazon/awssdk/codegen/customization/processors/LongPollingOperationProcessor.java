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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.codegen.customization.CodegenCustomizationProcessor;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.service.ServiceModel;

// TODO: Remove this when the long polling trait is formalized as a c2j trait.

/**
 * Marks specific service operations as having the long polling trait.
 */
public class LongPollingOperationProcessor implements CodegenCustomizationProcessor {
    private static final Logger log = LoggerFactory.getLogger(LongPollingOperationProcessor.class);

    // Note: static mapping instead of exposed via CustomizationConfig to avoid exposing it for wider use unless necessary.
    private static final Map<String, List<String>> SERVICE_ID_TO_OPERATIONS_MAP;

    static {
        Map<String, List<String>> serviceIdToOperationsMap = new HashMap<>();

        serviceIdToOperationsMap.put("SQS", Collections.singletonList("ReceiveMessage"));
        serviceIdToOperationsMap.put("SFN", Collections.singletonList("GetActivityTask"));
        serviceIdToOperationsMap.put("SWF", Collections.unmodifiableList(Arrays.asList("PollForActivityTask",
                                                                                       "PollForDecisionTask")));

        SERVICE_ID_TO_OPERATIONS_MAP = Collections.unmodifiableMap(serviceIdToOperationsMap);
    }

    private final Map<String, List<String>> serviceIdToOperations;

    public LongPollingOperationProcessor() {
        this(SERVICE_ID_TO_OPERATIONS_MAP);
    }

    @SdkTestInternalApi
    LongPollingOperationProcessor(Map<String, List<String>> serviceIdToOperations) {
        this.serviceIdToOperations = serviceIdToOperations;
    }

    @Override
    public void preprocess(ServiceModel serviceModel) {
        // no-op
    }

    @Override
    public void postprocess(IntermediateModel intermediateModel) {
        String serviceId = intermediateModel.getMetadata().getServiceId();

        if (!serviceIdToOperations.containsKey(serviceId)) {
            return;
        }

        // The long polling flag is only emitted by JsonProtocolSpec (see LongPollTrait). SyncClientClass#getProtocolSpecs
        // routes exactly the protocols where isJsonProtocol() is true to JsonProtocolSpec. Update this check if that routing
        // changes, so the flag is never silently dropped.
        if (!intermediateModel.getMetadata().isJsonProtocol()) {
            throw new IllegalArgumentException("Currently only JSON-based protocol services (AWS JSON, REST JSON, CBOR, "
                                               + "Smithy RPC v2 CBOR) can use the longPoll trait. Service " + serviceId
                                               + " uses protocol: " + intermediateModel.getMetadata().getProtocol().getValue());
        }

        List<String> longPollingOperations = serviceIdToOperations.getOrDefault(serviceId, Collections.emptyList());

        for (String longPollingOperation : longPollingOperations) {
            OperationModel opModel = intermediateModel.getOperation(longPollingOperation);
            if (opModel != null) {
                log.info("Setting the longPoll trait for {}#{}", serviceId, longPollingOperation);
                opModel.setLongPolling(true);
            } else {
                throw new RuntimeException("Operation " + longPollingOperation + " not found for service " + serviceId);
            }
        }
    }
}
