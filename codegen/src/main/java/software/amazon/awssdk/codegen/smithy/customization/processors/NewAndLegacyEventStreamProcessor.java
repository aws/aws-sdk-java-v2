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

package software.amazon.awssdk.codegen.smithy.customization.processors;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.smithy.customization.SmithyCustomizationProcessor;
import software.amazon.smithy.aws.traits.ServiceTrait;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Smithy equivalent of {@link software.amazon.awssdk.codegen.customization.processors.NewAndLegacyEventStreamProcessor}.
 *
 * <p>This is a Category B processor (direct Smithy equivalent). Services that have "legacy"
 * event streams (Kinesis and Transcribe Streaming) should fail the build if there is a new
 * event stream that codegen doesn't know about. This allows the team to decide whether to
 * generate the new stream in the legacy style or the new style.
 *
 * <p>In Smithy, event streams are represented as union shapes with the {@code @streaming} trait,
 * unlike C2J where shapes have an {@code isEventstream} flag.
 */
public final class NewAndLegacyEventStreamProcessor implements SmithyCustomizationProcessor {

    private static final ShapeId STREAMING_TRAIT_ID = ShapeId.from("smithy.api#streaming");

    // Map from service SDK ID -> set of approved event stream shape names
    private static final Map<String, Set<String>> APPROVED_EVENT_STREAMS;

    static {
        Map<String, Set<String>> approvedEventStreams = new HashMap<>();

        approvedEventStreams.put("Kinesis",
                new HashSet<>(Arrays.asList("SubscribeToShardEventStream")));
        approvedEventStreams.put("Transcribe Streaming",
                new HashSet<>(Arrays.asList("AudioStream",
                                            "TranscriptResultStream",
                                            "MedicalTranscriptResultStream",
                                            "CallAnalyticsTranscriptResultStream",
                                            "MedicalScribeInputStream",
                                            "MedicalScribeResultStream")));

        APPROVED_EVENT_STREAMS = Collections.unmodifiableMap(approvedEventStreams);
    }

    @Override
    public Model preprocess(Model model, ServiceShape service) {
        String serviceId = service.getTrait(ServiceTrait.class)
                .map(ServiceTrait::getSdkId)
                .orElse(null);

        if (serviceId == null || !APPROVED_EVENT_STREAMS.containsKey(serviceId)) {
            return model;
        }

        Set<String> approvedStreams = APPROVED_EVENT_STREAMS.get(serviceId);

        for (Shape shape : model.toSet()) {
            if (shape.isUnionShape() && shape.hasTrait(STREAMING_TRAIT_ID)) {
                String name = shape.getId().getName();
                if (!approvedStreams.contains(name)) {
                    throw unknownStreamError(serviceId, name);
                }
            }
        }

        return model;
    }

    @Override
    public void postprocess(IntermediateModel intermediateModel) {
        // no-op
    }

    private static RuntimeException unknownStreamError(String serviceId, String eventstreamName) {
        String msg = String.format("Encountered a new eventstream for service %s: %s. This service contains "
                + "evenstreams that are code generated using an older style that requires a customization. Please "
                + "contact the Java SDK maintainers for assistance.", serviceId, eventstreamName);

        return new RuntimeException(msg);
    }
}
