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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.codegen.customization.CodegenCustomizationProcessor;
import software.amazon.awssdk.codegen.model.config.customization.LegacyEventGenerationMode;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.service.Member;
import software.amazon.awssdk.codegen.model.service.ServiceModel;
import software.amazon.awssdk.codegen.model.service.Shape;

/**
 * Processor for eventstreams with shared events.  This Processor does two things: 1. Apply the duplicateAndRenameSharedEvents
 * customization 2. Raise helpful error messages on untransfromed shared events.
 */
public final class EventStreamSharedEventProcessor implements CodegenCustomizationProcessor {
    private static final Logger log = LoggerFactory.getLogger(EventStreamSharedEventProcessor.class);

    private final Map<String, Map<String, LegacyEventGenerationMode>> useLegacyEventGenerationScheme;

    public EventStreamSharedEventProcessor(Map<String, Map<String, LegacyEventGenerationMode>> useLegacyEventGenerationScheme) {
        this.useLegacyEventGenerationScheme = useLegacyEventGenerationScheme;
    }

    @Override
    public void preprocess(ServiceModel serviceModel) {
        Map<String, Shape> newEventShapes = new HashMap<>();
        for (Map.Entry<String, Shape> shapeEntry : serviceModel.getShapes().entrySet()) {
            if (shapeEntry.getValue().isEventstream()) {
                Shape eventStreamShape = shapeEntry.getValue();
                Map<String, LegacyEventGenerationMode> eventLegacyModes = useLegacyEventGenerationScheme
                    .getOrDefault(shapeEntry.getKey(), Collections.emptyMap());
                for (Map.Entry<String, Member> memberEntry : eventStreamShape.getMembers().entrySet()) {
                    Shape memberTargetShape = serviceModel.getShape(memberEntry.getValue().getShape());
                    LegacyEventGenerationMode legacyEventGenerationMode = eventLegacyModes
                        .getOrDefault(memberEntry.getKey(), LegacyEventGenerationMode.DISABLED);

                    if (memberTargetShape.isEvent() && legacyEventGenerationMode == LegacyEventGenerationMode.DISABLED) {
                        String newShapeName = memberEntry.getValue().getShape() + shapeEntry.getKey();
                        newEventShapes.put(newShapeName, memberTargetShape);
                        memberEntry.getValue().setShape(newShapeName);
                    }
                }
            }
        }
        serviceModel.getShapes().putAll(newEventShapes);
    }

    @Override
    public void postprocess(IntermediateModel intermediateModel) {
        // no-op
    }
}
