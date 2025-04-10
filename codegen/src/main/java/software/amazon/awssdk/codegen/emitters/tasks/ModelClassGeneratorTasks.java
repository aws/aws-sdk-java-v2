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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import software.amazon.awssdk.codegen.emitters.GeneratorTask;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;
import software.amazon.awssdk.codegen.emitters.PoetGeneratorTask;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.Metadata;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeType;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.common.EnumClass;
import software.amazon.awssdk.codegen.poet.model.AwsServiceBaseRequestSpec;
import software.amazon.awssdk.codegen.poet.model.AwsServiceBaseResponseSpec;
import software.amazon.awssdk.codegen.poet.model.AwsServiceModel;
import software.amazon.awssdk.codegen.poet.model.EventModelSpec;
import software.amazon.awssdk.codegen.poet.model.EventStreamSpecHelper;
import software.amazon.awssdk.codegen.poet.model.ResponseMetadataSpec;
import software.amazon.awssdk.codegen.poet.model.ServiceModelCopiers;

class ModelClassGeneratorTasks extends BaseGeneratorTasks {
    private final String modelClassDir;
    private final IntermediateModel intermediateModel;

    ModelClassGeneratorTasks(GeneratorTaskParams dependencies) {
        super(dependencies);
        this.modelClassDir = dependencies.getPathProvider().getModelDirectory();
        this.intermediateModel = dependencies.getModel();
    }

    @Override
    protected List<GeneratorTask> createTasks() {
        List<GeneratorTask> tasks = new ArrayList<>();

        tasks.add(new PoetGeneratorTask(modelClassDir, model.getFileHeader(), new AwsServiceBaseRequestSpec(model)));
        tasks.add(new PoetGeneratorTask(modelClassDir, model.getFileHeader(), new AwsServiceBaseResponseSpec(model)));

        model.getShapes().values().stream()
                    .filter(this::shouldGenerateShape)
                    .map(safeFunction(this::createTask))
                    .forEach(tasks::add);

        tasks.addAll(eventModelGenerationTasks());

        new ServiceModelCopiers(model).copierSpecs().stream()
                .map(safeFunction(spec -> new PoetGeneratorTask(modelClassDir, model.getFileHeader(), spec)))
                .forEach(tasks::add);

        tasks.add(new PoetGeneratorTask(modelClassDir, model.getFileHeader(), new ResponseMetadataSpec(model)));

        return tasks;
    }

    private boolean shouldGenerateShape(ShapeModel shapeModel) {
        if (shapeModel.getCustomization().isSkipGeneratingModelClass()) {
            info("Skip generating class " + shapeModel.getShapeName());
            return false;
        }
        return true;
    }

    private GeneratorTask createTask(ShapeModel shapeModel) {
        Metadata metadata = model.getMetadata();
        ClassSpec classSpec;
        if (shapeModel.getShapeType() == ShapeType.Enum) {
            classSpec = new EnumClass(metadata.getFullModelPackageName(), shapeModel);
        } else {
            classSpec = new AwsServiceModel(model, shapeModel);
        }
        return new PoetGeneratorTask(modelClassDir, model.getFileHeader(), classSpec);
    }

    private List<GeneratorTask> eventModelGenerationTasks() {
        return model.getShapes().values().stream()
                .filter(ShapeModel::isEventStream)
                .flatMap(eventStream -> {
                    EventStreamSpecHelper eventStreamSpecHelper = new EventStreamSpecHelper(eventStream,
                            intermediateModel);

                    return eventStream.getMembers().stream()
                            .filter(e -> e.getShape().isEvent())
                            .filter(e -> eventStreamSpecHelper.legacyEventGenerationMode()
                                         != CustomizationConfig.LegacyEventGenerationMode.NO_ES_EVENT_IMPL)
                            .map(e -> createEventGenerationTask(e, eventStream));
                })
                .collect(Collectors.toList());
    }

    private GeneratorTask createEventGenerationTask(MemberModel memberModel, ShapeModel eventStream) {
        EventModelSpec spec = new EventModelSpec(memberModel, eventStream, model);
        String outputDir = modelClassDir + "/" + eventStream.getShapeName().toLowerCase(Locale.ENGLISH);
        return new PoetGeneratorTask(outputDir, model.getFileHeader(), spec);
    }
}
