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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.awssdk.codegen.emitters.GeneratorTask;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;
import software.amazon.awssdk.codegen.emitters.PoetGeneratorTask;
import software.amazon.awssdk.codegen.model.service.PaginatorDefinition;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.paginators.AsyncResponseClassSpec;
import software.amazon.awssdk.codegen.poet.paginators.SyncResponseClassSpec;

public class PaginatorsGeneratorTasks extends BaseGeneratorTasks {

    private final String paginatorsClassDir;

    public PaginatorsGeneratorTasks(GeneratorTaskParams dependencies) {
        super(dependencies);
        this.paginatorsClassDir = dependencies.getPathProvider().getPaginatorsDirectory();
    }

    @Override
    protected boolean hasTasks() {
        return model.hasPaginators();
    }

    @Override
    protected List<GeneratorTask> createTasks() throws Exception {
        return model.getPaginators().entrySet().stream()
                    .filter(entry -> entry.getValue().isValid())
                    .flatMap(safeFunction(this::createSyncAndAsyncTasks))
                    .collect(Collectors.toList());
    }

    private Stream<GeneratorTask> createSyncAndAsyncTasks(Map.Entry<String, PaginatorDefinition> entry) throws IOException {
        return Arrays.asList(createSyncTask(entry), createAsyncTask(entry))
                     .stream();
    }

    private GeneratorTask createSyncTask(Map.Entry<String, PaginatorDefinition> entry) throws IOException {
        ClassSpec classSpec = new SyncResponseClassSpec(model, entry.getKey(), entry.getValue());

        return new PoetGeneratorTask(paginatorsClassDir, model.getFileHeader(), classSpec);
    }

    private GeneratorTask createAsyncTask(Map.Entry<String, PaginatorDefinition> entry) throws IOException {
        ClassSpec classSpec = new AsyncResponseClassSpec(model, entry.getKey(), entry.getValue());

        return new PoetGeneratorTask(paginatorsClassDir, model.getFileHeader(), classSpec);
    }
}
