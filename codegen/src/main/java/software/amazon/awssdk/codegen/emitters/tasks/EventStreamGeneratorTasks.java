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

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.awssdk.codegen.emitters.GeneratorTask;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;
import software.amazon.awssdk.codegen.emitters.PoetGeneratorTask;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.eventstream.EventStreamResponseHandlerBuilderSpec;
import software.amazon.awssdk.codegen.poet.eventstream.EventStreamResponseHandlerSpec;
import software.amazon.awssdk.codegen.poet.eventstream.EventStreamVisitorBuilder;

/**
 * Generator tasks for event streaming operations.
 */
class EventStreamGeneratorTasks extends BaseGeneratorTasks {

    private final GeneratorTaskParams params;

    EventStreamGeneratorTasks(GeneratorTaskParams dependencies) {
        super(dependencies);
        this.params = dependencies;
    }

    @Override
    protected List<GeneratorTask> createTasks() throws Exception {
        info("Emitting event stream classes");

        String fileHeader = model.getFileHeader();
        String modelDirectory = params.getPathProvider().getModelDirectory();
        return model.getOperations().values().stream()
                    .filter(e -> e.getOutputShape() != null)
                    .filter(e -> e.getOutputShape().hasEventStreamMember())
                    .flatMap(this::eventStreamClassSpecs)
                    .map(spec -> new PoetGeneratorTask(modelDirectory, fileHeader, spec))
                    .collect(Collectors.toList());
    }

    private Stream<ClassSpec> eventStreamClassSpecs(OperationModel opModel) {
        return Stream.of(
            new EventStreamResponseHandlerSpec(params, opModel),
            new EventStreamResponseHandlerBuilderSpec(params, opModel),
            new EventStreamVisitorBuilder(params, opModel));
    }
}
