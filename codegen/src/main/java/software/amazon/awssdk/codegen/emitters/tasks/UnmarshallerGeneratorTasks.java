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

package software.amazon.awssdk.codegen.emitters.tasks;

import static software.amazon.awssdk.utils.FunctionalUtils.safeFunction;

import freemarker.template.Template;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.awssdk.codegen.emitters.FreemarkerGeneratorTask;
import software.amazon.awssdk.codegen.emitters.GeneratorTask;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;
import software.amazon.awssdk.codegen.model.intermediate.Metadata;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeType;
import software.amazon.awssdk.core.util.ImmutableMapParameter;

public class UnmarshallerGeneratorTasks extends BaseGeneratorTasks {

    private final String transformClassDir;
    private final Metadata metadata;

    public UnmarshallerGeneratorTasks(GeneratorTaskParams dependencies) {
        super(dependencies);
        this.transformClassDir = dependencies.getPathProvider().getTransformDirectory();
        this.metadata = dependencies.getModel().getMetadata();
    }

    @Override
    protected List<GeneratorTask> createTasks() throws Exception {
        info("Emitting unmarshaller classes");
        return model.getShapes().entrySet().stream()
                .filter(e -> shouldGenerate(e.getValue()))
                .map(safeFunction(e -> createTask(e.getKey(), e.getValue())))
                .collect(Collectors.toList());
    }

    private GeneratorTask createTask(String javaShapeName, ShapeModel shapeModel) throws Exception {
        final Template template = freemarker.getModelUnmarshallerTemplate();
        final ShapeType shapeType = shapeModel.getShapeType();
        Map<String, Object> dataModel = ImmutableMapParameter.<String, Object>builder()
                .put("fileHeader", model.getFileHeader())
                .put("shape", shapeModel)
                .put("metadata", metadata)
                .put("transformPackage", model.getMetadata().getFullTransformPackageName())
                .put("exceptionUnmarshallerImpl", model.getExceptionUnmarshallerImpl())
                .build();

        switch (shapeType) {
            case Response:
            case Model: {
                return new FreemarkerGeneratorTask(transformClassDir,
                                         javaShapeName + "Unmarshaller",
                                                   template,
                                                   dataModel);
            }
            case Exception: {
                return new FreemarkerGeneratorTask(transformClassDir,
                                         javaShapeName + "Unmarshaller",
                                                   freemarker.getExceptionUnmarshallerTemplate(),
                                                   dataModel);
            }
            default:
                // If shape doesn't need an umarshaller generated it should have been filtered out already
                throw new IllegalStateException(shapeModel.getC2jName() + " is not supported for unmarshaller generation");
        }
    }

    private boolean shouldGenerate(ShapeModel shapeModel) {
        if (shapeModel.getCustomization().isSkipGeneratingUnmarshaller()) {
            info("Skip generating unmarshaller class for " + shapeModel.getShapeName());
            return false;
        }
        switch (shapeModel.getShapeType()) {
            case Response:
            case Model:
                // The event stream shape is a container for event subtypes and isn't something that needs to ever be unmarshalled
                return !shapeModel.isEventStream();
            case Exception:
                // Generating Exception Unmarshallers is not required for the JSON protocol
                return !metadata.isJsonProtocol();
            default:
                return false;
        }
    }

}
