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

package software.amazon.awssdk.codegen.emitters.tasks;

import static software.amazon.awssdk.utils.FunctionalUtils.safeFunction;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.awssdk.codegen.emitters.GeneratorTask;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;
import software.amazon.awssdk.codegen.model.intermediate.Metadata;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeType;
import software.amazon.awssdk.codegen.poet.eventstream.EventStreamUtils;
import software.amazon.awssdk.codegen.poet.transform.MarshallerSpec;

public class MarshallerGeneratorTasks extends BaseGeneratorTasks {

    private final Metadata metadata;

    public MarshallerGeneratorTasks(GeneratorTaskParams dependencies) {
        super(dependencies);
        this.metadata = model.getMetadata();
    }

    @Override
    protected List<GeneratorTask> createTasks() {
        return model.getShapes().entrySet().stream()
                    .filter(e -> shouldGenerate(e.getValue()))
                    .flatMap(safeFunction(e -> createTask(e.getKey(), e.getValue())))
                    .collect(Collectors.toList());
    }

    private boolean shouldGenerate(ShapeModel shapeModel) {
        if (shapeModel.getCustomization().isSkipGeneratingMarshaller()) {
            info("Skipping generating marshaller class for " + shapeModel.getShapeName());
            return false;
        }

        ShapeType shapeType = shapeModel.getShapeType();
        return (ShapeType.Request == shapeType || (ShapeType.Model == shapeType && metadata.isJsonProtocol()))
               // The event stream shape is a container for event subtypes and isn't something that needs to ever be marshalled
               && !shapeModel.isEventStream();
    }

    private Stream<GeneratorTask> createTask(String javaShapeName, ShapeModel shapeModel) throws Exception {
        return ShapeType.Request == shapeModel.getShapeType() || (ShapeType.Model == shapeModel.getShapeType()
                                                                  && shapeModel.isEvent()
                                                                  && EventStreamUtils.isRequestEvent(model, shapeModel))
                   ? Stream.of(createPoetGeneratorTask(new MarshallerSpec(model, shapeModel)))
                   : Stream.empty();
    }
}
