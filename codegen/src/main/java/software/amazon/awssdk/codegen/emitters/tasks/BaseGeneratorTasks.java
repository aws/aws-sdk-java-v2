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
import java.util.List;
import java.util.concurrent.ForkJoinTask;
import org.slf4j.Logger;
import software.amazon.awssdk.codegen.emitters.GeneratorTask;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;
import software.amazon.awssdk.codegen.emitters.PoetGeneratorTask;
import software.amazon.awssdk.codegen.internal.Utils;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;

public abstract class BaseGeneratorTasks extends GeneratorTask {
    protected final String baseDirectory;
    protected final String testDirectory;
    protected final IntermediateModel model;
    protected final Logger log;

    public BaseGeneratorTasks(GeneratorTaskParams dependencies) {
        this.baseDirectory = dependencies.getPathProvider().getSourceDirectory();
        this.testDirectory = dependencies.getPathProvider().getTestDirectory();
        this.model = dependencies.getModel();
        this.log = dependencies.getLog();
    }

    protected void info(String message) {
        log.info(message);
    }

    /**
     * Hook to allow subclasses to indicate they have no tasks so they can assume when createTasks is called there's something to
     * emit.
     */
    protected boolean hasTasks() {
        return true;
    }

    protected final GeneratorTask createPoetGeneratorTask(ClassSpec classSpec) throws IOException {
        String targetDirectory = baseDirectory + '/' + Utils.packageToDirectory(classSpec.className().packageName());
        return new PoetGeneratorTask(targetDirectory, model.getFileHeader(), classSpec);
    }

    protected final GeneratorTask createPoetGeneratorTestTask(ClassSpec classSpec) throws IOException {
        String targetDirectory = testDirectory + '/' + Utils.packageToDirectory(classSpec.className().packageName());
        return new PoetGeneratorTask(targetDirectory, model.getFileHeader(), classSpec);
    }

    protected abstract List<GeneratorTask> createTasks() throws Exception;

    @Override
    protected void compute() {
        try {
            if (hasTasks()) {
                String taskName = getClass().getSimpleName();
                log.info("Starting " + taskName + "...");
                ForkJoinTask.invokeAll(createTasks());
                log.info("  Completed " + taskName + ".");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
