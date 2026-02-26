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

import java.util.LinkedHashMap;
import java.util.Map;
import software.amazon.awssdk.codegen.smithy.customization.ShapeIdResolver;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.UnionShape;

/**
 * Smithy equivalent of {@link software.amazon.awssdk.codegen.customization.processors.EventStreamSharedEventProcessor}.
 *
 * <p>This is a Category C processor (dual-config pattern). It duplicates shared event shapes
 * and renames them as specified in the config, so that event stream shapes that share events
 * across multiple streams get their own copies with unique names.
 *
 * <p>In Smithy, event streams are represented as union shapes with the {@code @streaming} trait.
 * Each member of the union represents an event, and the member's target is the event shape.
 * This processor duplicates the target event shape with a new name and updates the union member
 * to reference the duplicated shape.
 *
 * <p>Follows the dual-config pattern: accepts both old C2J {@code duplicateAndRenameSharedEvents}
 * and new Smithy-native {@code smithyDuplicateAndRenameSharedEvents} config fields. Old config
 * uses simple event stream names as outer keys; new config uses full ShapeId strings.
 */
public class EventStreamSharedEventProcessor
        extends AbstractDualConfigProcessor<Map<String, Map<String, String>>, Map<String, Map<String, String>>> {

    public EventStreamSharedEventProcessor(Map<String, Map<String, String>> oldConfig,
                                                  Map<String, Map<String, String>> newConfig) {
        super(oldConfig, newConfig, "duplicateAndRenameSharedEvents", "smithyDuplicateAndRenameSharedEvents");
    }

    @Override
    protected boolean isSet(Object config) {
        if (config instanceof Map) {
            return !((Map<?, ?>) config).isEmpty();
        }
        return config != null;
    }

    // -----------------------------------------------------------------------
    // convertOldToNew: resolve simple event stream name keys to full ShapeIds
    // -----------------------------------------------------------------------

    @Override
    protected Map<String, Map<String, String>> convertOldToNew(Map<String, Map<String, String>> old,
                                                                Model model,
                                                                ServiceShape service) {
        Map<String, Map<String, String>> result = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, String>> entry : old.entrySet()) {
            ShapeId resolved = ShapeIdResolver.resolve(model, service, entry.getKey());
            result.put(resolved.toString(), entry.getValue());
        }
        return result;
    }

    // -----------------------------------------------------------------------
    // applySmithyLogic: duplicate shared event shapes and rename
    // -----------------------------------------------------------------------

    @Override
    protected Model applySmithyLogic(Model model, ServiceShape service,
                                      Map<String, Map<String, String>> config) {
        Model current = model;
        for (Map.Entry<String, Map<String, String>> eventStreamEntry : config.entrySet()) {
            String eventStreamShapeIdStr = eventStreamEntry.getKey();
            ShapeId eventStreamShapeId = ShapeId.from(eventStreamShapeIdStr);
            UnionShape eventStreamShape = current.expectShape(eventStreamShapeId, UnionShape.class);

            for (Map.Entry<String, String> eventEntry : eventStreamEntry.getValue().entrySet()) {
                String oldEventMemberName = eventEntry.getKey();
                String newEventName = eventEntry.getValue();

                MemberShape eventMember = eventStreamShape.getMember(oldEventMemberName)
                    .orElseThrow(() -> new IllegalStateException(
                        String.format("Cannot find event member '%s' in event stream '%s'.",
                                      oldEventMemberName, eventStreamShapeIdStr)));

                // Get the shared event shape that this member targets
                ShapeId sharedEventShapeId = eventMember.getTarget();
                Shape sharedEventShape = current.expectShape(sharedEventShapeId);

                // Create a duplicated shape with the new name in the same namespace
                String namespace = sharedEventShapeId.getNamespace();
                ShapeId newShapeId = ShapeId.from(namespace + "#" + newEventName);

                Shape duplicatedShape = Shape.shapeToBuilder(sharedEventShape)
                                             .id(newShapeId)
                                             .build();

                // Update the event stream union member to target the new shape
                MemberShape updatedMember = eventMember.toBuilder()
                    .target(newShapeId)
                    .build();

                eventStreamShape = eventStreamShape.toBuilder()
                    .removeMember(oldEventMemberName)
                    .addMember(updatedMember)
                    .build();

                current = current.toBuilder()
                    .addShape(duplicatedShape)
                    .addShape(eventStreamShape)
                    .build();
            }
        }
        return current;
    }
}
