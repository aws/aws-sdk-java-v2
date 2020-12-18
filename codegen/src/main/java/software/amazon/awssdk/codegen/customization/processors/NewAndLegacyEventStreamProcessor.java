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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import software.amazon.awssdk.codegen.customization.CodegenCustomizationProcessor;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.service.ServiceModel;

/**
 * Services that have "legacy" streams, Kinesis and Transcribe Streaming builds should fail if there is a new
 * evenstream added, that codegen doesn't know about. This is so we can decide if we want to generate the new stream in
 * the legacy style so they look like the existing streams, or if we use the new style (e.g. because it won't work with
 * the old style).
 */
public final class NewAndLegacyEventStreamProcessor implements CodegenCustomizationProcessor {
    // Map from service ID -> list of event streams we know about and approve
    private static final Map<String, Set<String>> APPROVED_EVENT_STREAMS;

    static {
        Map<String, Set<String>> approvedEventStreams = new HashMap<>();

        approvedEventStreams.put("Kinesis", new HashSet<>(Arrays.asList("SubscribeToShardEventStream")));
        approvedEventStreams.put("Transcribe Streaming",
                new HashSet<>(Arrays.asList("AudioStream", "TranscriptResultStream", "MedicalTranscriptResultStream")));

        APPROVED_EVENT_STREAMS = Collections.unmodifiableMap(approvedEventStreams);
    }


    @Override
    public void preprocess(ServiceModel serviceModel) {
        String serviceId = serviceModel.getMetadata().getServiceId();
        if (!APPROVED_EVENT_STREAMS.containsKey(serviceId)) {
            return;
        }

        Set<String> approvedStreams = APPROVED_EVENT_STREAMS.get(serviceId);

        serviceModel.getShapes().entrySet().stream()
                .filter(e -> e.getValue().isEventstream())
                .forEach(e -> {
                    String name = e.getKey();
                    if (!approvedStreams.contains(name)) {
                        throw unknownStreamError(serviceId, name);
                    }
                });
    }

    @Override
    public void postprocess(IntermediateModel intermediateModel) {
        // no-op
    }

    private static RuntimeException unknownStreamError(String serviceId, String evenstreamName) {
        String msg = String.format("Encountered a new eventstream for service %s: %s. This service contains " +
                "evenstreams that are code generated using an older style that requires a customization. Please " +
                "contact the Java SDK maintainers for assistance.", serviceId, evenstreamName);

        return new RuntimeException(msg);
    }
}
