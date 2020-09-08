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

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.awssdk.codegen.emitters.GeneratorTask;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;
import software.amazon.awssdk.codegen.emitters.PoetGeneratorTask;
import software.amazon.awssdk.codegen.model.service.WaiterDefinition;
import software.amazon.awssdk.codegen.poet.waiters.WaitersClassSpec;

public class WaitersGeneratorTasks extends BaseGeneratorTasks {

    private final String waitersClassDir;

    public WaitersGeneratorTasks(GeneratorTaskParams dependencies) {
        super(dependencies);
        this.waitersClassDir = dependencies.getPathProvider().getWaitersDirectory();
    }

    @Override
    protected boolean hasTasks() {
        return model.hasWaiters();
    }

    @Override
    protected List<GeneratorTask> createTasks() throws Exception {
        return model.getWaiters().entrySet().stream()
                    .flatMap(safeFunction(this::createSyncAndAsyncTasks))
                    .collect(Collectors.toList());
    }

    private Stream<GeneratorTask> createSyncAndAsyncTasks(Map.Entry<String, WaiterDefinition> entry) throws IOException {
        return Stream.of(createSyncTask(entry), createAsyncTask(entry));
    }

    private GeneratorTask createSyncTask(Map.Entry<String, WaiterDefinition> entry) throws IOException {
        return new PoetGeneratorTask(waitersClassDir, model.getFileHeader(),
                                     new WaitersClassSpec(model, entry.getKey(), entry.getValue()));
    }

    private GeneratorTask createAsyncTask(Map.Entry<String, WaiterDefinition> entry) throws IOException {
        throw new UnsupportedOperationException();
    }
}
