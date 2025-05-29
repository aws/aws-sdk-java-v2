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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ForkJoinTask;
import software.amazon.awssdk.codegen.emitters.GeneratorTask;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;
import software.amazon.awssdk.codegen.emitters.tasks.AwsGeneratorTasks;
import software.amazon.awssdk.codegen.internal.Jackson;
import software.amazon.awssdk.codegen.internal.Utils;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.validation.ModelValidationContext;
import software.amazon.awssdk.codegen.validation.ModelValidationReport;
import software.amazon.awssdk.codegen.validation.ModelValidator;
import software.amazon.awssdk.codegen.validation.ValidationEntry;
import software.amazon.awssdk.utils.Logger;

public class CodeGenerator {
    private static final Logger log = Logger.loggerFor(CodeGenerator.class);
    private static final String MODEL_DIR_NAME = "models";

    // TODO: add validators
    private static final List<ModelValidator> DEFAULT_MODEL_VALIDATORS = Collections.emptyList();

    private final C2jModels c2jModels;

    private final IntermediateModel intermediateModel;
    private final IntermediateModel shareModelsTarget;
    private final String sourcesDirectory;
    private final String resourcesDirectory;
    private final String testsDirectory;

    /**
     * The prefix for the file name that contains the intermediate model.
     */
    private final String fileNamePrefix;

    private final List<ModelValidator> modelValidators;
    private final boolean emitValidationReport;

    static {
        // Make sure ClassName is statically initialized before we do anything in parallel.
        // Parallel static initialization of ClassName and TypeName can result in a deadlock:
        // https://github.com/square/javapoet/issues/799
        ClassName.get(Object.class);
    }

    public CodeGenerator(Builder builder) {
        this.c2jModels = builder.models;
        this.intermediateModel = builder.intermediateModel;

        if (this.c2jModels != null && this.intermediateModel != null) {
            throw new IllegalArgumentException("Only one of c2jModels and intermediateModel must be specified");
        }

        this.shareModelsTarget = builder.shareModelsTarget;
        this.sourcesDirectory = builder.sourcesDirectory;
        this.testsDirectory = builder.testsDirectory;
        this.resourcesDirectory = builder.resourcesDirectory != null ? builder.resourcesDirectory
                                                                     : builder.sourcesDirectory;
        this.fileNamePrefix = builder.fileNamePrefix;
        this.modelValidators = builder.modelValidators == null ? DEFAULT_MODEL_VALIDATORS : builder.modelValidators;
        this.emitValidationReport = builder.emitValidationReport;
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
        ModelValidationReport report = new ModelValidationReport();

        IntermediateModel modelToGenerate;
        if (c2jModels != null) {
            modelToGenerate = new IntermediateModelBuilder(c2jModels).build();
        } else {
            modelToGenerate = intermediateModel;
        }

        List<ValidationEntry> validatorEntries = runModelValidators(modelToGenerate);
        report.setValidationEntries(validatorEntries);

        if (emitValidationReport) {
            writeValidationReport(report);
        }

        if (!validatorEntries.isEmpty()) {
            throw new RuntimeException("Validation failed. See validation report for details.");
        }

        try {
            if (fileNamePrefix != null) {
                writeIntermediateModel(modelToGenerate);
            }
            emitCode(modelToGenerate);

        } catch (Exception e) {
            log.error(() -> "Failed to generate code. ", e);
            throw new RuntimeException(
                    "Failed to generate code. Exception message : " + e.getMessage(), e);
        }
    }

    private List<ValidationEntry> runModelValidators(IntermediateModel intermediateModel) {
        ModelValidationContext ctx = ModelValidationContext.builder()
                                                           .intermediateModel(intermediateModel)
                                                           .shareModelsTarget(shareModelsTarget)
                                                           .build();

        List<ValidationEntry> validationEntries = new ArrayList<>();

        modelValidators.forEach(v -> validationEntries.addAll(v.validateModels(ctx)));

        return validationEntries;
    }

    private void writeValidationReport(ModelValidationReport report) {
        try {
            writeModel(report, "validation-report.json");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void writeIntermediateModel(IntermediateModel model) throws IOException {
        writeModel(model, fileNamePrefix + "-intermediate.json");
    }

    private void writeModel(Object model, String name) throws IOException {
        File modelDir = getModelDirectory(sourcesDirectory);
        PrintWriter writer = null;
        try {
            File outDir = new File(sourcesDirectory);
            if (!outDir.exists() && !outDir.mkdirs()) {
                throw new RuntimeException("Failed to create " + outDir.getAbsolutePath());
            }

            File outputFile = new File(modelDir, name);

            if (!outputFile.exists() && !outputFile.createNewFile()) {
                throw new RuntimeException("Error creating file " + outputFile.getAbsolutePath());
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
        return new AwsGeneratorTasks(GeneratorTaskParams.create(intermediateModel,
                                                                sourcesDirectory,
                                                                testsDirectory,
                                                                resourcesDirectory));

    }

    /**
     * Builder for a {@link CodeGenerator}.
     */
    public static final class Builder {

        private C2jModels models;
        private IntermediateModel intermediateModel;
        private IntermediateModel shareModelsTarget;
        private String sourcesDirectory;
        private String resourcesDirectory;
        private String testsDirectory;
        private String fileNamePrefix;
        private List<ModelValidator> modelValidators;
        private boolean emitValidationReport;

        private Builder() {
        }

        public Builder models(C2jModels models) {
            this.models = models;
            return this;
        }

        public Builder intermediateModel(IntermediateModel intermediateModel) {
            this.intermediateModel = intermediateModel;
            return this;
        }

        public Builder shareModelsTarget(IntermediateModel shareModelsTarget) {
            this.shareModelsTarget = shareModelsTarget;
            return this;
        }

        public Builder sourcesDirectory(String sourcesDirectory) {
            this.sourcesDirectory = sourcesDirectory;
            return this;
        }

        public Builder resourcesDirectory(String resourcesDirectory) {
            this.resourcesDirectory = resourcesDirectory;
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

        public Builder modelValidators(List<ModelValidator> modelValidators) {
            this.modelValidators = modelValidators;
            return this;
        }

        public Builder emitValidationReport(boolean emitValidationReport) {
            this.emitValidationReport = emitValidationReport;
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
