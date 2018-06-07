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

import static software.amazon.awssdk.codegen.internal.Constant.PROPERTIES_FILE_NAME_SUFFIX;

import java.util.Arrays;
import java.util.List;
import software.amazon.awssdk.codegen.emitters.CodeWriter;
import software.amazon.awssdk.codegen.emitters.FreemarkerGeneratorTask;
import software.amazon.awssdk.codegen.emitters.GeneratorTask;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;

public class SmokeTestsGeneratorTasks extends BaseGeneratorTasks {

    private final String smokeTestsPackageDir;

    public SmokeTestsGeneratorTasks(GeneratorTaskParams params) {
        super(params);
        this.smokeTestsPackageDir = params.getPathProvider().getSmokeTestDirectory();
    }

    @Override
    protected boolean hasTasks() {
        return !model.getCustomizationConfig().isSkipSmokeTests();
    }

    @Override
    public List<GeneratorTask> createTasks() throws Exception {
        info("Emitting smoke test files");
        return Arrays.asList(
                new FreemarkerGeneratorTask(
                        smokeTestsPackageDir,
                        model.getMetadata().getCucumberModuleInjectorClassName(),
                        freemarker.getCucumberModuleInjectorTemplate(),
                        model),
                new FreemarkerGeneratorTask(
                        smokeTestsPackageDir,
                        "RunCucumberTest",
                        freemarker.getCucumberTestTemplate(),
                        model),
                new FreemarkerGeneratorTask(
                        new CodeWriter(smokeTestsPackageDir, "cucumber", PROPERTIES_FILE_NAME_SUFFIX),
                        freemarker.getCucumberPropertiesTemplate(),
                        model));
    }
}
