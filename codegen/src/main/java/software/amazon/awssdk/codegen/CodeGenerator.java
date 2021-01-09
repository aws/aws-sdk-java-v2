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

package software.amazon.awssdk.codegen;

import com.squareup.javapoet.ClassName;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.ForkJoinTask;
import software.amazon.awssdk.codegen.emitters.GeneratorTask;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;
import software.amazon.awssdk.codegen.emitters.tasks.AwsGeneratorTasks;
import software.amazon.awssdk.codegen.internal.Jackson;
import software.amazon.awssdk.codegen.internal.Utils;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;

public class CodeGenerator {

    private static final String MODEL_DIR_NAME = "models";

    private final C2jModels models;
    private final String sourcesDirectory;
    private final String testsDirectory;

    /**
     * The prefix for the file name that contains the intermediate model.
     */
    private final String fileNamePrefix;

    static {
        // Make sure ClassName is statically initialized before we do anything in parallel.
        // Parallel static initialization of ClassName and TypeName can result in a deadlock:
        // https://github.com/square/javapoet/issues/799
        ClassName.get(Object.class);
    }

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
            IntermediateModel intermediateModel = new IntermediateModelBuilder(models).build();

            if (fileNamePrefix != null) {
                writeIntermediateModel(intermediateModel);
            }

            emitCode(intermediateModel);

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to generate code. Exception message : " + e.getMessage(), e);

        }
    }

    private void writeIntermediateModel(IntermediateModel model) throws IOException {
        File modelDir = getModelDirectory(sourcesDirectory);
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
            Jackson.writeWithObjectMapper(model, writer);
        } finally {
            if (writer != null) {
                writer.flush();
                writer.close();
            }
        }
    }

    private void emitCode(IntermediateModel intermediateModel) {
        ForkJoinTask.invokeAll(createGeneratorTasks(intermediateModel));
    }

    private GeneratorTask createGeneratorTasks(IntermediateModel intermediateModel) {
        return new AwsGeneratorTasks(GeneratorTaskParams.create(intermediateModel, sourcesDirectory, testsDirectory));

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

        public Builder intermediateModelFileNamePrefix(String fileNamePrefix) {
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
