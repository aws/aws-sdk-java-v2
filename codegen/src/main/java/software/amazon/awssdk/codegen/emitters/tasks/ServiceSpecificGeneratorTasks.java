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
import software.amazon.awssdk.codegen.poet.service.s3.BucketRequestParameterSpec;

/**
 * Common generator tasks.
 */
class ServiceSpecificGeneratorTasks extends BaseGeneratorTasks {

    ServiceSpecificGeneratorTasks(GeneratorTaskParams dependencies) {
        super(dependencies);
    }

    @Override
    protected List<GeneratorTask> createTasks() throws Exception {
        List<GeneratorTask> generatorTasks = new ArrayList<>();
        if (isS3CrossRegionEnabled()) {
            generatorTasks.add(createS3RequestParameterTask());
        }
        return generatorTasks;
    }

    private GeneratorTask createS3RequestParameterTask() throws IOException {
        return createPoetGeneratorTask(new BucketRequestParameterSpec(model));
    }

    private boolean isS3CrossRegionEnabled() {
        return "s3".equalsIgnoreCase(model.getMetadata().getServiceName()) &&
               (model.getCustomizationConfig().isDelegateAsyncClientClass() ||
                model.getCustomizationConfig().isDelegateSyncClientClass());
    }
}
