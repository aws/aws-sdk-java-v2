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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import software.amazon.awssdk.codegen.emitters.GeneratorTask;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;
import software.amazon.awssdk.codegen.poet.builder.BaseClientBuilderClass;
import software.amazon.awssdk.codegen.poet.builder.BaseClientBuilderInterface;

/**
 * Task for classes shared by {@link AsyncClientGeneratorTasks} and {@link SyncClientGeneratorTasks}.
 */
public class CommonClientGeneratorTasks extends BaseGeneratorTasks {
    public CommonClientGeneratorTasks(GeneratorTaskParams dependencies) {
        super(dependencies);
    }

    @Override
    protected List<GeneratorTask> createTasks() throws Exception {
        info("Emitting common client classes");
        return Arrays.asList(createBaseBuilderTask(),
                             createBaseBuilderInterfaceTask());
    }

    private GeneratorTask createBaseBuilderTask() throws IOException {
        return createPoetGeneratorTask(new BaseClientBuilderClass(model));
    }

    private GeneratorTask createBaseBuilderInterfaceTask() throws IOException {
        return createPoetGeneratorTask(new BaseClientBuilderInterface(model));
    }
}
