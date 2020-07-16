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

package software.amazon.awssdk.codegen.poet.eventstream;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;

/**
 * Utils for event streaming code generation.
 */
public class EventStreamUtils {

    private EventStreamUtils() {
    }

    /**
     * Get eventstream member from a request shape model. If shape doesn't have any eventstream member,
     * throw an IllegalStateException.
     *
     * @param requestShape request shape of an operation
     * @return eventstream member (shape with "eventstream" trait set to true) in the request shape.
     * If there is no eventstream member, thrown IllegalStateException.
     */
    public static ShapeModel getEventStreamInRequest(ShapeModel requestShape) {
        return eventStreamFrom(requestShape);
    }

    /**
     * Get eventstream member from a response shape model. If shape doesn't have any eventstream member,
     * throw an IllegalStateException.
     *
     * @param responseShape response shape of an operation
     * @return eventstream member (shape with "eventstream" trait set to true) in the response shape.
     * If there is no eventstream member, thrown IllegalStateException.
     */
    public static ShapeModel getEventStreamInResponse(ShapeModel responseShape) {
        return eventStreamFrom(responseShape);
    }

    /**
     * Get event stream shape from a request/response shape model. Otherwise, throw
     *
     * @param shapeModel request or response shape of an operation
     * @return the EventStream shape
     * @throws IllegalStateException if there is no associated event stream shape
     */
    private static ShapeModel eventStreamFrom(ShapeModel shapeModel) {
        if (shapeModel == null || shapeModel.getMembers() == null) {
            return null;
        }

        return shapeModel.getMembers()
                         .stream()
                         .map(MemberModel::getShape)
                         .filter(Objects::nonNull)
                         .filter(ShapeModel::isEventStream)
                         .findFirst()
                         .orElseThrow(() -> new IllegalStateException(String.format(
                             "%s has no event stream member", shapeModel.getC2jName())));
    }

    /**
     * Returns the event stream {@link ShapeModel} that contains the given event.
     *
     * @param model Intermediate model
     * @param eventShape shape with "event: true" trait
     * @return the event stream shape (eventstream: true) that contains the given event, or an empty optional if the C2J shape
     * is marked as an event but the intermediate model representation is not used by an event stream
     */
    public static Optional<ShapeModel> getBaseEventStreamShape(IntermediateModel model, ShapeModel eventShape) {
        return model.getShapes().values()
                    .stream()
                    .filter(ShapeModel::isEventStream)
                    .filter(s -> s.getMembers().stream().anyMatch(m -> m.getShape().equals(eventShape)))
                    .findFirst();
    }

    /**
     * Returns the stream of event member shapes ('event: true') excluding exceptions
     * from the input event stream shape ('eventstream: true').
     */
    public static Stream<ShapeModel> getEvents(ShapeModel eventStreamShape) {
        return getEventMembers(eventStreamShape).map(MemberModel::getShape);
    }

    /**
     * Returns the stream of event members ('event: true') excluding exceptions
     * from the input event stream shape ('eventstream: true').
     */
    public static Stream<MemberModel> getEventMembers(ShapeModel eventStreamShape) {
        if (eventStreamShape == null || eventStreamShape.getMembers() == null) {
            return Stream.empty();
        }

        return eventStreamShape.getMembers()
                               .stream()
                               .filter(m -> m.getShape() != null && m.getShape().isEvent());
    }

    /**
     * Returns the all operations that contain the given event stream shape. The event stream can be in operation
     * request or response shape.
     */
    public static Collection<OperationModel> findOperationsWithEventStream(IntermediateModel model, ShapeModel eventStreamShape) {
        Collection<OperationModel> operations = model.getOperations().values()
                    .stream()
                    .filter(op -> operationContainsEventStream(op, eventStreamShape))
                    .collect(Collectors.toList());

        if (operations.isEmpty()) {
            throw new IllegalStateException(String.format(
                "%s is an event shape but has no corresponding operation in the model", eventStreamShape.getC2jName()));
        }

        return operations;
    }

    /**
     * Returns true if the #childEventStreamShape is a member of the #parentShape. Otherwise false.
     */
    public static boolean doesShapeContainsEventStream(ShapeModel parentShape, ShapeModel childEventStreamShape) {
        return parentShape != null
               && parentShape.getMembers() != null
               && parentShape.getMembers().stream()
                             .filter(m -> m.getShape() != null)
                             .filter(m -> m.getShape().equals(childEventStreamShape))
                             .anyMatch(m -> m.getShape().isEventStream());

    }

    /**
     * Returns true if the given event shape is a sub-member of any operation request.
     */
    public static boolean isRequestEvent(IntermediateModel model, ShapeModel eventShape) {
        return getBaseEventStreamShape(model, eventShape)
            .map(stream -> model.getOperations().values()
                                .stream()
                                .anyMatch(o -> doesShapeContainsEventStream(o.getInputShape(), stream)))
            .orElse(false);
    }

    private static boolean operationContainsEventStream(OperationModel opModel, ShapeModel eventStreamShape) {
        return doesShapeContainsEventStream(opModel.getInputShape(), eventStreamShape) ||
               doesShapeContainsEventStream(opModel.getOutputShape(), eventStreamShape);
    }

    /**
     * @return true if the provide model is a request/response shape model that contains event stream shape.
     * Otherwise return false.
     */
    public static boolean isEventStreamParentModel(ShapeModel shapeModel) {
        return containsEventStream(shapeModel);
    }

    private static boolean containsEventStream(ShapeModel shapeModel) {
        return shapeModel != null
               && shapeModel.getMembers() != null
               && shapeModel.getMembers().stream()
                            .filter(m -> m.getShape() != null)
                            .anyMatch(m -> m.getShape().isEventStream());
    }
}
