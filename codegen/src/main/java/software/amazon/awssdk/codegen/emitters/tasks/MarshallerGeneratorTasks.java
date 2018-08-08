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

import static software.amazon.awssdk.codegen.model.intermediate.Protocol.AWS_JSON;
import static software.amazon.awssdk.utils.FunctionalUtils.safeFunction;

import freemarker.template.Template;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.awssdk.codegen.emitters.FreemarkerGeneratorTask;
import software.amazon.awssdk.codegen.emitters.GeneratorTask;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;
import software.amazon.awssdk.codegen.model.intermediate.Metadata;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeType;
import software.amazon.awssdk.codegen.poet.transform.JsonModelMarshallerSpec;
import software.amazon.awssdk.codegen.poet.transform.MarshallerSpec;
import software.amazon.awssdk.utils.ImmutableMap;

public class MarshallerGeneratorTasks extends BaseGeneratorTasks {

    private final String transformClassDir;
    private final Metadata metadata;
    private final Map<String, ShapeModel> shapes;

    public MarshallerGeneratorTasks(GeneratorTaskParams dependencies) {
        super(dependencies);
        this.transformClassDir = dependencies.getPathProvider().getTransformDirectory();
        this.metadata = model.getMetadata();
        this.shapes = model.getShapes();
    }

    @Override
    protected List<GeneratorTask> createTasks() throws Exception {
        info("Emitting marshaller classes");
        return model.getShapes().entrySet().stream()
                    .filter(e -> shouldGenerate(e.getValue()))
                    .flatMap(safeFunction(e -> createTask(e.getKey(), e.getValue())))
                    .collect(Collectors.toList());
    }

    private boolean shouldGenerate(ShapeModel shapeModel) {
        if (shapeModel.getCustomization().isSkipGeneratingMarshaller()) {
            info("Skip generating marshaller class for " + shapeModel.getShapeName());
            return false;
        }
        ShapeType shapeType = shapeModel.getShapeType();
        return (ShapeType.Request == shapeType || (ShapeType.Model == shapeType && metadata.isJsonProtocol()))
               // The event stream shape is a container for event subtypes and isn't something that needs to ever be marshalled
               && !shapeModel.isEventStream();
    }

    private Stream<GeneratorTask> createTask(String javaShapeName, ShapeModel shapeModel) throws Exception {
        if (metadata.isJsonProtocol()) {
            return ShapeType.Request == shapeModel.getShapeType() ?
                   Stream.of(createPoetGeneratorTask(new JsonModelMarshallerSpec(model, shapeModel, "ModelMarshaller")),
                             createPoetGeneratorTask(new MarshallerSpec(model, shapeModel))) :
                   Stream.of(createPoetGeneratorTask(new JsonModelMarshallerSpec(model, shapeModel, "Marshaller")));
        }

        return Stream.of(
            createMarshallerTask(javaShapeName,
                                 freemarker.getModelMarshallerTemplate(),
                                 javaShapeName + "Marshaller",
                                 transformClassDir));
    }

    private GeneratorTask createMarshallerTask(String javaShapeName, Template template,
                                               String marshallerClassName, String marshallerDirectory) throws IOException {
        Map<String, Object> marshallerDataModel = ImmutableMap.<String, Object>builder()
            .put("fileHeader", model.getFileHeader())
            .put("shapeName", javaShapeName)
            .put("shapes", shapes)
            .put("metadata", metadata)
            .put("transformPackage", model.getMetadata().getFullTransformPackageName())
            .put("requestTransformPackage", model.getMetadata().getFullRequestTransformPackageName())
            .put("customConfig", model.getCustomizationConfig())
            .put("className", marshallerClassName)
            .put("protocolEnum", getProtocolEnumName())
            .build();

        return new FreemarkerGeneratorTask(marshallerDirectory,
                                           marshallerClassName,
                                           template,
                                           marshallerDataModel);
    }

    private String getProtocolEnumName() {
        switch (metadata.getProtocol()) {
            case CBOR:
            case ION:
            case AWS_JSON:
                return AWS_JSON.name();
            default:
                return metadata.getProtocol().name();
        }
    }
}
