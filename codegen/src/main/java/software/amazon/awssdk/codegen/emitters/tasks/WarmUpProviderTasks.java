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
import software.amazon.awssdk.codegen.emitters.WarmUpProviderRegistrationTask;
import software.amazon.awssdk.codegen.poet.crac.WarmUpProviderSpec;

/**
 * Emits the per-service {@code SdkWarmUpProvider} implementation and its {@code META-INF/services} registration.
 */
public final class WarmUpProviderTasks extends BaseGeneratorTasks {

    private final GeneratorTaskParams params;

    public WarmUpProviderTasks(GeneratorTaskParams params) {
        super(params);
        this.params = params;
    }

    @Override
    protected List<GeneratorTask> createTasks() throws Exception {
        List<GeneratorTask> tasks = new ArrayList<>();

        WarmUpProviderSpec spec = new WarmUpProviderSpec(model);

        tasks.add(new PoetGeneratorTask(warmUpProviderDir(), model.getFileHeader(), spec));
        tasks.add(new WarmUpProviderRegistrationTask(params.getPathProvider().getResourcesDirectory(),
                                                     spec.className().toString()));
        return tasks;
    }

    private String warmUpProviderDir() {
        return params.getPathProvider().getWarmUpProviderDirectory();
    }
}
