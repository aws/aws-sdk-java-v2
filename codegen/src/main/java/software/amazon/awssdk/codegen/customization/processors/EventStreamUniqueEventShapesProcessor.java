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
import software.amazon.awssdk.codegen.customization.CodegenCustomizationProcessor;
import software.amazon.awssdk.codegen.model.config.customization.LegacyEventGenerationMode;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.service.ServiceModel;
import software.amazon.awssdk.codegen.model.service.Shape;
import software.amazon.awssdk.utils.Logger;

/**
 * Processor for eventstreams that ensures that all eventstream event shapes are unique - for each eventstream/event it creates a
 * new shape with a unique name constructed from the EventStream and Event shape names: `[ShapeName][EventStreamName]`. Any legacy
 * eventstream/events (configured with the useLegacyEventGenerationScheme customization) are skipped. When an event shape is
 * shared between multiple eventstreams, it causes SDK generation/compilation failures. The top level shape POJO implements the
 * event stream interface for each stream and the return type of the sdkEventType method conflicts.
 */
public final class EventStreamUniqueEventShapesProcessor implements CodegenCustomizationProcessor {
    private static final Logger log = Logger.loggerFor(EventStreamUniqueEventShapesProcessor.class);

    private final Map<String, Map<String, LegacyEventGenerationMode>> useLegacyEventGenerationScheme;

    public EventStreamUniqueEventShapesProcessor(
        Map<String, Map<String, LegacyEventGenerationMode>> useLegacyEventGenerationScheme) {
        this.useLegacyEventGenerationScheme = useLegacyEventGenerationScheme;
    }

    @Override
    public void preprocess(ServiceModel serviceModel) {
        Map<String, Shape> newEventShapes = new HashMap<>();
        serviceModel.getShapes().forEach((name, shape) -> {
            if (!shape.isEventstream()) {
                return;
            }

            preprocessEventStream(serviceModel, name, shape, newEventShapes);
        });
        serviceModel.getShapes().putAll(newEventShapes);
    }

    private void preprocessEventStream(ServiceModel serviceModel, String eventStreamName, Shape eventStreamShape, Map<String,
        Shape> newEventShapes) {
        Map<String, LegacyEventGenerationMode> eventLegacyModes = useLegacyEventGenerationScheme
            .getOrDefault(eventStreamName, Collections.emptyMap());

        eventStreamShape.getMembers().forEach((memberName, member) -> {
            String eventShapeName = member.getShape();
            Shape memberTargetShape = serviceModel.getShape(eventShapeName);
            LegacyEventGenerationMode legacyEventGenerationMode = eventLegacyModes
                .getOrDefault(memberName, LegacyEventGenerationMode.DISABLED);

            if (memberTargetShape.isEvent() && legacyEventGenerationMode == LegacyEventGenerationMode.DISABLED) {
                String newShapeName = eventShapeName + eventStreamName;
                if (serviceModel.getShapes().containsKey(newShapeName)) {
                    // TODO: This could be an error instead.  Its unlikely we'll run into this.  And if we do, not creating the
                    //  unique name is only an issue when/if the event is shared with another event stream whos event/eventstream
                    // shape name is also in the model.
                    log.warn(() -> String.format("Shape name conflict, unable to create a new unique event shape name for %s in"
                                                 + " eventstream %s because %s already exists in the model.  Skipping.",
                                                 eventShapeName, eventStreamName, newShapeName));
                } else {
                    log.debug(() -> String.format("Creating new, unique, event shape for %s in eventstream %s: %s",
                                                  eventShapeName, eventStreamName, newShapeName));
                    newEventShapes.put(newShapeName, memberTargetShape);
                    member.setShape(newShapeName);
                }
            }
        });
    }

    @Override
    public void postprocess(IntermediateModel intermediateModel) {
        // no-op
    }
}
