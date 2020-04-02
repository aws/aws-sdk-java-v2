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

package software.amazon.awssdk.codegen.emitters;

import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.codegen.emitters.tasks.SharedModelsTaskParamsValidator;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.PoetExtensions;

/**
 * Parameters for generator tasks.
 */
public class GeneratorTaskParams {
    private static final Consumer<GeneratorTaskParams> TASK_PARAMS_VALIDATORS = new SharedModelsTaskParamsValidator();

    private final IntermediateModel model;
    private final GeneratorPathProvider pathProvider;
    private final PoetExtensions poetExtensions;
    private final Logger log = LoggerFactory.getLogger(GeneratorTaskParams.class);

    private GeneratorTaskParams(IntermediateModel model,
                                GeneratorPathProvider pathProvider) {
        this.model = model;
        this.pathProvider = pathProvider;
        this.poetExtensions = new PoetExtensions(model);
    }

    public static GeneratorTaskParams create(IntermediateModel model, String sourceDirectory, String testDirectory) {
        GeneratorPathProvider pathProvider = new GeneratorPathProvider(model, sourceDirectory, testDirectory);
        GeneratorTaskParams params = new GeneratorTaskParams(model, pathProvider);
        TASK_PARAMS_VALIDATORS.accept(params);
        return params;
    }

    /**
     * @return Built intermediate model
     */
    public IntermediateModel getModel() {
        return model;
    }

    /**
     * @return Provider for common paths.
     */
    public GeneratorPathProvider getPathProvider() {
        return pathProvider;
    }

    /**
     * @return Extensions and convenience methods for Java Poet.
     */
    public PoetExtensions getPoetExtensions() {
        return poetExtensions;
    }

    /**
     * @return Logger
     */
    public Logger getLog() {
        return log;
    }
}
