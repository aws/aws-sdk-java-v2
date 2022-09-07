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
import software.amazon.awssdk.codegen.emitters.GeneratorTask;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;
import software.amazon.awssdk.codegen.emitters.PoetGeneratorTask;
import software.amazon.awssdk.codegen.poet.rules.EndpointParametersClassSpec;
import software.amazon.awssdk.codegen.poet.rules.EndpointProviderInterceptorSpec;
import software.amazon.awssdk.codegen.poet.rules.EndpointProviderInterfaceSpec;
import software.amazon.awssdk.codegen.poet.rules.EndpointProviderSpec;

public final class EndpointProviderTasks extends BaseGeneratorTasks {
    private final GeneratorTaskParams generatorTaskParams;

    public EndpointProviderTasks(GeneratorTaskParams dependencies) {
        super(dependencies);
        this.generatorTaskParams = dependencies;
    }

    @Override
    protected List<GeneratorTask> createTasks() throws Exception {
        List<GeneratorTask> tasks = new ArrayList<>();
        tasks.add(generateInterface());
        tasks.add(generateParams());
        tasks.add(generateDefaultProvider());
        tasks.add(generateInterceptor());
        return tasks;
    }

    private GeneratorTask generateInterface() {
        return new PoetGeneratorTask(endpointRulesDir(), model.getFileHeader(), new EndpointProviderInterfaceSpec(model));
    }

    private GeneratorTask generateParams() {
        return new PoetGeneratorTask(endpointRulesDir(), model.getFileHeader(), new EndpointParametersClassSpec(model));
    }

    private GeneratorTask generateDefaultProvider() {
        return new PoetGeneratorTask(endpointRulesInternalDir(), model.getFileHeader(), new EndpointProviderSpec(model), true);
    }

    private GeneratorTask generateInterceptor() {
        return new PoetGeneratorTask(endpointRulesInternalDir(), model.getFileHeader(),
                                     new EndpointProviderInterceptorSpec(model));
    }

    private String endpointRulesDir() {
        return generatorTaskParams.getPathProvider().getEndpointRulesDirectory();
    }

    private String endpointRulesInternalDir() {
        return generatorTaskParams.getPathProvider().getEndpointRulesInternalDirectory();
    }

}
