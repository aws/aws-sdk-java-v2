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

package software.amazon.awssdk.codegen;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import software.amazon.awssdk.codegen.emitters.CodeEmitter;
import software.amazon.awssdk.codegen.emitters.GeneratorTask;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskExecutor;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;
import software.amazon.awssdk.codegen.emitters.tasks.ApiGatewayGeneratorTasks;
import software.amazon.awssdk.codegen.emitters.tasks.AwsGeneratorTasks;
import software.amazon.awssdk.codegen.internal.Jackson;
import software.amazon.awssdk.codegen.internal.Utils;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.Protocol;

public class CodeGenerator {

    private static final String MODEL_DIR_NAME = "models";

    private final C2jModels models;
    private final String sourcesDirectory;
    private final String testsDirectory;
    /**
     * The prefix for the file name that contains the intermediate model.
     */
    private final String fileNamePrefix;

    public CodeGenerator(Builder builder) {
        this.models = builder.models;
        this.sourcesDirectory = builder.sourcesDirectory;
        this.testsDirectory = builder.testsDirectory;
        this.fileNamePrefix = builder.fileNamePrefix;
    }

    public static File getModelDirectory(String outputDirectory) {
        File dir = new File(outputDirectory, MODEL_DIR_NAME);
        Utils.createDirectory(dir);
        return dir;
    }

    /**
     * @return Builder instance to construct a {@link CodeGenerator}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * load ServiceModel. load code gen configuration from individual client. generate intermediate model. generate
     * code.
     */
    public void execute() {
        try {
            final IntermediateModel intermediateModel = new IntermediateModelBuilder(models).build();

            // Dump the intermediate model to a file
            writeIntermediateModel(intermediateModel);

            emitCode(intermediateModel);

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to generate code. Exception message : " + e.getMessage(), e);

        }
    }

    private void writeIntermediateModel(IntermediateModel model)
            throws IOException {
        final File modelDir = getModelDirectory(sourcesDirectory);
        PrintWriter writer = null;
        try {
            File outDir = new File(sourcesDirectory);
            if (!outDir.exists()) {
                if (!outDir.mkdirs()) {
                    throw new RuntimeException("Failed to create "
                                               + outDir.getAbsolutePath());
                }
            }

            File outputFile = new File(modelDir, fileNamePrefix + "-intermediate.json");

            if (!outputFile.exists()) {
                if (!outputFile.createNewFile()) {
                    throw new RuntimeException("Error creating file "
                                               + outputFile.getAbsolutePath());
                }
            }

            writer = new PrintWriter(outputFile, "UTF-8");
            Jackson.write(model, writer);
        } finally {
            if (writer != null) {
                writer.flush();
                writer.close();
            }
        }
    }

    private void emitCode(IntermediateModel intermediateModel) {
        final Iterable<GeneratorTask> generatorTasks = createGeneratorTasks(intermediateModel);
        try (CodeEmitter emitter = new CodeEmitter(generatorTasks, new GeneratorTaskExecutor())) {
            emitter.emit();
        }
    }

    private Iterable<GeneratorTask> createGeneratorTasks(IntermediateModel intermediateModel) {
        // For clients built internally, the output directory and source directory are the same.
        GeneratorTaskParams params = GeneratorTaskParams.create(intermediateModel, sourcesDirectory, testsDirectory);

        if (params.getModel().getMetadata().getProtocol() == Protocol.API_GATEWAY) {
            return new ApiGatewayGeneratorTasks(params);
        } else {
            return new AwsGeneratorTasks(params);
        }
    }

    /**
     * Builder for a {@link CodeGenerator}.
     */
    public static final class Builder {

        private C2jModels models;
        private String sourcesDirectory;
        private String testsDirectory;
        private String fileNamePrefix;

        private Builder() {
        }

        public Builder models(C2jModels models) {
            this.models = models;
            return this;
        }

        public Builder sourcesDirectory(String sourcesDirectory) {
            this.sourcesDirectory = sourcesDirectory;
            return this;
        }

        public Builder testsDirectory(String smokeTestsDirectory) {
            this.testsDirectory = smokeTestsDirectory;
            return this;
        }

        public Builder fileNamePrefix(String fileNamePrefix) {
            this.fileNamePrefix = fileNamePrefix;
            return this;
        }

        /**
         * @return An immutable {@link CodeGenerator} object.
         */
        public CodeGenerator build() {
            return new CodeGenerator(this);
        }
    }

}
