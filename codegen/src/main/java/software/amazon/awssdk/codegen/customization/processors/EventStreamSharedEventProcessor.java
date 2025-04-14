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

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.codegen.customization.CodegenCustomizationProcessor;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.service.Member;
import software.amazon.awssdk.codegen.model.service.ServiceModel;
import software.amazon.awssdk.codegen.model.service.Shape;

/**
 * Processor for eventstreams with shared events.  This Processor does two things: 1. Apply the duplicateAndRenameSharedEvents
 * customization 2. Raise helpful error messages on untransfromed shared events.
 */
public final class EventStreamSharedEventProcessor implements CodegenCustomizationProcessor {
    private static final Logger log = LoggerFactory.getLogger(EventStreamSharedEventProcessor.class);

    private final Map<String, Map<String, String>> duplicateAndRenameSharedEvents;

    public EventStreamSharedEventProcessor(Map<String, Map<String, String>> duplicateAndRenameSharedEvents) {
        this.duplicateAndRenameSharedEvents = duplicateAndRenameSharedEvents;
    }

    @Override
    public void preprocess(ServiceModel serviceModel) {
        if (duplicateAndRenameSharedEvents == null || duplicateAndRenameSharedEvents.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Map<String, String>> eventStreamEntry : duplicateAndRenameSharedEvents.entrySet()) {

            String eventStreamName = eventStreamEntry.getKey();
            Shape eventStreamShape = serviceModel.getShapes().get(eventStreamName);

            validateIsEventStream(eventStreamShape, eventStreamName);

            Map<String, Member> eventStreamMembers = eventStreamShape.getMembers();
            for (Map.Entry<String, String> eventEntry : eventStreamEntry.getValue().entrySet()) {
                Member eventMemberToModify = eventStreamMembers.get(eventEntry.getKey());

                if (eventMemberToModify == null) {
                    throw new IllegalStateException(
                        String.format("Cannot find event member [%s] in the eventstream [%s] when processing "
                                      + "customization config duplicateAndRenameSharedEvents.%s",
                                      eventEntry.getKey(), eventStreamName, eventStreamName));
                }

                String shapeToDuplicate = eventMemberToModify.getShape();
                Shape eventMemberShape = serviceModel.getShape(shapeToDuplicate);

                if (eventMemberShape == null || !eventMemberShape.isEvent()) {
                    throw new IllegalStateException(
                        String.format("Error: %s must be an Event shape when processing "
                                      + "customization config duplicateAndRenameSharedEvents.%s",
                                      eventEntry.getKey(), eventStreamName));
                }

                String newShapeName = eventEntry.getValue();
                if (serviceModel.getShapes().containsKey(newShapeName)) {
                    throw new IllegalStateException(
                        String.format("Error: %s is already in the model when processing "
                                      + "customization config duplicateAndRenameSharedEvents.%s",
                                      newShapeName, eventStreamName));
                }
                serviceModel.getShapes().put(newShapeName, eventMemberShape);
                eventMemberToModify.setShape(newShapeName);
                log.info("Duplicated and renamed event member on {} from {} -> {}",
                         eventStreamName, shapeToDuplicate, newShapeName);
            }
        }
    }

    private static void validateIsEventStream(Shape shape, String name) {
        if (shape == null) {
            throw new IllegalStateException(
                String.format("Cannot find eventstream shape [%s] in the model when processing "
                              + "customization config duplicateAndRenameSharedEvents.%s", name, name));
        }
        if (!shape.isEventstream()) {
            throw new IllegalStateException(
                String.format("Error: %s must be an EventStream when processing "
                              + "customization config duplicateAndRenameSharedEvents.%s", name, name));
        }
    }

    @Override
    public void postprocess(IntermediateModel intermediateModel) {
        // validate that there are no events shared between multiple eventstreams.
        // events may be used multiple times in the same eventstream.
        Map<String, String> seenEvents = new HashMap<>();

        for (ShapeModel shapeModel : intermediateModel.getShapes().values()) {
            if (shapeModel.isEventStream()) {
                shapeModel.getMembers().forEach(m -> {
                    ShapeModel memberShape = intermediateModel.getShapes().get(m.getC2jShape());
                    if (memberShape != null && memberShape.isEvent()) {
                        if (seenEvents.containsKey(memberShape.getShapeName())
                            && !seenEvents.get(memberShape.getShapeName()).equals(shapeModel.getShapeName())) {
                            throw new IllegalStateException(
                                String.format("Event %s is shared between multiple EventStreams. Apply the "
                                              + "duplicateAndRenameSharedEvents customization to resolve the issue.",
                                              memberShape.getShapeName()));
                        }
                        seenEvents.put(memberShape.getShapeName(), shapeModel.getShapeName());
                    }
                });
            }
        }
    }
}
