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

import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.emitters.GeneratorTask;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;
import software.amazon.awssdk.codegen.emitters.PoetGeneratorTask;
import software.amazon.awssdk.codegen.poet.batchmanager.BatchFunctionsClassSpec;

@SdkInternalApi
public class BatchManagerGeneratorTasks extends BaseGeneratorTasks {
    private final GeneratorTaskParams generatorTaskParams;

    public BatchManagerGeneratorTasks(GeneratorTaskParams dependencies) {
        super(dependencies);
        this.generatorTaskParams = dependencies;
    }

    @Override
    protected boolean hasTasks() {
        return model.getCustomizationConfig().getBatchManager() != null;
    }

    @Override
    protected List<GeneratorTask> createTasks() {
        List<GeneratorTask> generatorTasks = new ArrayList<>();
        generatorTasks.add(createBatchFunctions());
        return generatorTasks;
    }

    private GeneratorTask createBatchFunctions() {
        return new PoetGeneratorTask(batchManagerInternalClassDir(), model.getFileHeader(),
                                     new BatchFunctionsClassSpec(model));
    }

    private String batchManagerInternalClassDir() {
        return generatorTaskParams.getPathProvider().getBatchManagerInternalDirectory();
    }
}
