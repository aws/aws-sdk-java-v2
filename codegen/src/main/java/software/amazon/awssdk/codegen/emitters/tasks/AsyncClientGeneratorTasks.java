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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.codegen.emitters.GeneratorTask;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;
import software.amazon.awssdk.codegen.poet.builder.AsyncClientBuilderClass;
import software.amazon.awssdk.codegen.poet.builder.AsyncClientBuilderInterface;
import software.amazon.awssdk.codegen.poet.client.AsyncClientClass;
import software.amazon.awssdk.codegen.poet.client.AsyncClientInterface;
import software.amazon.awssdk.codegen.poet.endpointdiscovery.EndpointDiscoveryAsyncCacheLoaderGenerator;

public class AsyncClientGeneratorTasks extends BaseGeneratorTasks {

    private final GeneratorTaskParams generatorTaskParams;

    public AsyncClientGeneratorTasks(GeneratorTaskParams dependencies) {
        super(dependencies);
        this.generatorTaskParams = dependencies;
    }

    @Override
    protected List<GeneratorTask> createTasks() throws Exception {
        List<GeneratorTask> generatorTasks = new ArrayList<>();
        generatorTasks.add(createClientClassTask());
        generatorTasks.add(createClientBuilderTask());
        generatorTasks.add(createClientBuilderInterfaceTask());
        generatorTasks.add(createClientInterfaceTask());
        if (model.getEndpointOperation().isPresent()) {
            generatorTasks.add(createEndpointDiscoveryCacheLoaderTask());
        }

        return generatorTasks;
    }

    private GeneratorTask createClientClassTask() throws IOException {
        return createPoetGeneratorTask(new AsyncClientClass(generatorTaskParams));
    }

    private GeneratorTask createClientBuilderTask() throws IOException {
        return createPoetGeneratorTask(new AsyncClientBuilderClass(model));
    }

    private GeneratorTask createClientBuilderInterfaceTask() throws IOException {
        return createPoetGeneratorTask(new AsyncClientBuilderInterface(model));
    }

    private GeneratorTask createClientInterfaceTask() throws IOException {
        return createPoetGeneratorTask(new AsyncClientInterface(model));
    }

    private GeneratorTask createEndpointDiscoveryCacheLoaderTask() throws IOException {
        return createPoetGeneratorTask(new EndpointDiscoveryAsyncCacheLoaderGenerator(generatorTaskParams));
    }
}
