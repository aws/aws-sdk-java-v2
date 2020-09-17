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
import software.amazon.awssdk.codegen.poet.waiters.AsyncWaiterClassSpec;
import software.amazon.awssdk.codegen.poet.waiters.AsyncWaiterInterfaceSpec;
import software.amazon.awssdk.codegen.poet.waiters.WaiterClassSpec;
import software.amazon.awssdk.codegen.poet.waiters.WaiterInterfaceSpec;

@SdkInternalApi
public class WaitersGeneratorTasks extends BaseGeneratorTasks {
    private final GeneratorTaskParams generatorTaskParams;

    public WaitersGeneratorTasks(GeneratorTaskParams dependencies) {
        super(dependencies);
        this.generatorTaskParams = dependencies;
    }

    @Override
    protected boolean hasTasks() {
        return model.hasWaiters();
    }

    @Override
    protected List<GeneratorTask> createTasks() {
        List<GeneratorTask> generatorTasks = new ArrayList<>();
        generatorTasks.addAll(createSyncTasks());
        generatorTasks.addAll(createAsyncTasks());
        generatorTasks.add(new WaitersRuntimeGeneratorTask(generatorTaskParams));
        return generatorTasks;
    }

    private List<GeneratorTask> createSyncTasks() {
        List<GeneratorTask> syncTasks = new ArrayList<>();
        syncTasks.add(new PoetGeneratorTask(waitersClassDir(), model.getFileHeader(),
                                            new WaiterInterfaceSpec(model)));
        syncTasks.add(new PoetGeneratorTask(waitersClassDir(), model.getFileHeader(),
                                            new WaiterClassSpec(model)));
        return syncTasks;
    }

    private List<GeneratorTask> createAsyncTasks() {
        List<GeneratorTask> asyncTasks = new ArrayList<>();
        asyncTasks.add(new PoetGeneratorTask(waitersClassDir(), model.getFileHeader(),
                                             new AsyncWaiterInterfaceSpec(model)));
        asyncTasks.add(new PoetGeneratorTask(waitersClassDir(), model.getFileHeader(),
                                             new AsyncWaiterClassSpec(model)));
        return asyncTasks;
    }

    private String waitersClassDir() {
        return generatorTaskParams.getPathProvider().getWaitersDirectory();
    }
}
