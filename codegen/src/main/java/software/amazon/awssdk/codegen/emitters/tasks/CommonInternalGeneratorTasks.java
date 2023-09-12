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

import java.util.Arrays;
import java.util.List;
import software.amazon.awssdk.codegen.emitters.GeneratorTask;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;
import software.amazon.awssdk.codegen.emitters.PoetGeneratorTask;
import software.amazon.awssdk.codegen.poet.client.SdkClientOptions;
import software.amazon.awssdk.codegen.poet.common.UserAgentUtilsSpec;

public class CommonInternalGeneratorTasks extends BaseGeneratorTasks {
    private final GeneratorTaskParams params;

    public CommonInternalGeneratorTasks(GeneratorTaskParams params) {
        super(params);
        this.params = params;
    }

    @Override
    protected List<GeneratorTask> createTasks() throws Exception {
        return Arrays.asList(createClientOptionTask(), createUserAgentTask());
    }

    private PoetGeneratorTask createClientOptionTask() {
        return new PoetGeneratorTask(clientOptionsDir(), params.getModel().getFileHeader(),
                                         new SdkClientOptions(params.getModel()));
    }

    private PoetGeneratorTask createUserAgentTask() {
        return new PoetGeneratorTask(clientOptionsDir(), params.getModel().getFileHeader(),
                                     new UserAgentUtilsSpec(params.getModel()));
    }

    private String clientOptionsDir() {
        return params.getPathProvider().getClientInternalDirectory();
    }
}
