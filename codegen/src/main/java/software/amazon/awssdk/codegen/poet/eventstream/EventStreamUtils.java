/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import com.squareup.javapoet.ClassName;
import java.util.Objects;
import java.util.stream.Stream;
import software.amazon.awssdk.codegen.internal.Utils;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.poet.PoetExtensions;

/**
 * Utils for event streaming code generation.
 */
public class EventStreamUtils {

    private final OperationModel operation;
    private final PoetExtensions poetExt;

    private EventStreamUtils(PoetExtensions poetExt, OperationModel eventStreamOperation) {
        this.operation = eventStreamOperation;
        this.poetExt = poetExt;
    }

    /**
     * @return The correctly cased name of the API.
     */
    public String getApiName() {
        return Utils.capitalize(operation.getOperationName());
    }

    /**
     * Retrieve the event stream {@link ShapeModel} for the operation. I.E. The shape that has the 'eventstream: true' trait
     * applied.
     *
     * @return ShapeModel for event stream member.
     */
    public ShapeModel getEventStreamShape() {
        ShapeModel outputShape = operation.getOutputShape();
        return outputShape.getMembers()
                          .stream()
                          .map(MemberModel::getShape)
                          .filter(Objects::nonNull)
                          .filter(ShapeModel::isEventStream)
                          .findFirst()
                          .orElseThrow(() -> new IllegalStateException("Did not find event stream member on " +
                                                                       outputShape.getC2jName()));
    }

    /**
     * Collect all subtypes defined in the event stream. These are all members of the event stream shape and
     * have the 'event: true' trait applied.
     *
     * @return Stream of event subtypes {@link ShapeModel}s.
     */
    public Stream<ShapeModel> getEventSubTypes() {
        return getEventStreamMembers()
            .map(MemberModel::getShape);
    }

    /**
     * Collect all members defined in the event stream shape. These are all members of the event stream shape and
     * have the 'event: true' trait applied.
     *
     * @return Stream of event subtypes {@link MemberModel}s.
     */
    public Stream<MemberModel> getEventStreamMembers() {
        return getEventStreamShape()
            .getMembers()
            .stream()
            .filter(m -> m.getShape() != null && m.getShape().isEvent());
    }

    /**
     * @return The {@link ClassName} for the response pojo.
     */
    public ClassName responsePojoType() {
        return poetExt.getModelClass(operation.getOutputShape().getShapeName());
    }

    /**
     * @return {@link ClassName} for the base class of all event sub types. This will be the 'eventstream' shape, i.e. the tagged
     * union like structure containing all event types.
     */
    public ClassName eventStreamBaseClass() {
        return poetExt.getModelClass(getEventStreamShape().getShapeName());
    }

    /**
     * @return {@link ClassName} for generated event stream response handler interface.
     */
    public ClassName responseHandlerType() {
        return poetExt.getModelClass(getApiName() + "ResponseHandler");
    }

    /**
     * @return {@link ClassName} for the builder interface for the response handler interface
     */
    public ClassName responseHandlerBuilderType() {
        return responseHandlerType().nestedClass("Builder");
    }

    /**
     * @return {@link ClassName} for the event stream visitor interface.
     */
    public ClassName responseHandlerVisitorType() {
        return responseHandlerType().nestedClass("Visitor");
    }

    /**
     * @return {@link ClassName} for the builder interface for the event stream visitor interface.
     */
    public ClassName responseHandlerVisitorBuilderType() {
        return responseHandlerVisitorType().nestedClass("Builder");
    }

    /**
     * Creates a new util object.
     *
     * @param poetExt Poet extensions.
     * @param eventStreamOperation The event stream operation being generated.
     * @return New {@link EventStreamUtils}.
     */
    public static EventStreamUtils create(PoetExtensions poetExt, OperationModel eventStreamOperation) {
        return new EventStreamUtils(poetExt, eventStreamOperation);
    }

    /**
     * Creates an {@link EventStreamUtils} from a given eventstream shape (i.e. the tagged union like container of event shapes).
     *
     * @param poetExt Poet extensions.
     * @param model Intermediate model.
     * @param eventStreamShape Eventstream shape.
     * @return New {@link EventStreamUtils}.
     */
    public static EventStreamUtils createFromEventStreamShape(PoetExtensions poetExt,
                                                              IntermediateModel model,
                                                              ShapeModel eventStreamShape) {
        return new EventStreamUtils(poetExt, findEventStreamOperation(model, eventStreamShape));
    }

    /**
     * Creates an {@link EventStreamUtils} from a given event sub type shape.
     *
     * @param poetExt Poet extensions.
     * @param model Intermediate model.
     * @param eventSubTypeShape Event sub type (i.e. member of eventstream shape).
     * @return New {@link EventStreamUtils}.
     */
    public static EventStreamUtils createFromEventShape(PoetExtensions poetExt,
                                                        IntermediateModel model,
                                                        ShapeModel eventSubTypeShape) {
        ShapeModel parentEventStreamShape = findBaseEventStreamShape(model, eventSubTypeShape);
        return createFromEventStreamShape(poetExt, model, parentEventStreamShape);
    }

    private static ShapeModel findBaseEventStreamShape(IntermediateModel model, ShapeModel eventStreamShape) {
        return model.getShapes().values()
                    .stream()
                    .filter(ShapeModel::isEventStream)
                    .filter(s -> s.getMembers().stream().anyMatch(m -> m.getShape().equals(eventStreamShape)))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                        String.format("Event shape %s not referenced in model by eventstream shape",
                                      eventStreamShape.getC2jName())));
    }

    private static OperationModel findEventStreamOperation(IntermediateModel model, ShapeModel eventSubTypeShape) {
        return model.getOperations().values()
                    .stream()
                    .filter(o -> o.getOutputShape() != null && o.getOutputShape().getMembers() != null)
                    .filter(o -> o.getOutputShape().getMembers()
                                  .stream()
                                  .filter(m -> m.getShape() != null)
                                  .filter(m -> m.getShape().equals(eventSubTypeShape))
                                  .anyMatch(m -> m.getShape().isEventStream()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(String.format(
                        "%s is an eventstream shape but has no corresponding operation in the model",
                        eventSubTypeShape.getC2jName())));
    }

}
