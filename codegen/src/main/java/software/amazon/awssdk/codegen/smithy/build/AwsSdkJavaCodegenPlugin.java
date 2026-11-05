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

package software.amazon.awssdk.codegen.smithy.build;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.CodeGenerator;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.smithy.SmithyIntermediateModelBuilder;
import software.amazon.awssdk.codegen.smithy.SmithyModels;
import software.amazon.awssdk.codegen.utils.ModelLoaderUtils;
import software.amazon.smithy.build.FileManifest;
import software.amazon.smithy.build.PluginContext;
import software.amazon.smithy.build.SmithyBuildPlugin;
import software.amazon.smithy.model.Model;

/**
 * Generates AWS SDK for Java sources from a Smithy build projection.
 */
@SdkInternalApi
public final class AwsSdkJavaCodegenPlugin implements SmithyBuildPlugin {

    /** Plugin name. */
    public static final String NAME = "aws-sdk-java-v2-codegen";

    private static final Logger log = LoggerFactory.getLogger(AwsSdkJavaCodegenPlugin.class);

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean requiresValidModel() {
        return false;
    }

    @Override
    public void execute(PluginContext context) {
        Model model = context.getModel();
        AwsSdkJavaCodegenSettings settings = AwsSdkJavaCodegenSettings.fromNode(context.getSettings());
        CustomizationConfig customizationConfig = loadCustomizationConfig(settings);

        IntermediateModel intermediateModel =
            new SmithyIntermediateModelBuilder(SmithyModels.builder()
                                                           .model(model)
                                                           .customizationConfig(customizationConfig)
                                                           .build())
                .build();

        FileManifest manifest = context.getFileManifest();
        Path base = manifest.getBaseDir();

        CodeGenerator.builder()
                     .intermediateModel(intermediateModel)
                     .sourcesDirectory(base.resolve("generated-sources").toString())
                     .resourcesDirectory(base.resolve("generated-resources").toString())
                     .testsDirectory(base.resolve("generated-test-sources").toString())
                     .build()
                     .execute();

        registerGeneratedFiles(manifest, base);
        log.info("{} generated {} files for service {}",
                 NAME, manifest.getFiles().size(), intermediateModel.getMetadata().getServiceName());
    }

    private CustomizationConfig loadCustomizationConfig(AwsSdkJavaCodegenSettings settings) {
        Path location = settings.customizationConfig();
        if (location == null) {
            return CustomizationConfig.create();
        }
        if (!Files.exists(location)) {
            throw new IllegalArgumentException(String.format(
                "The configured '%s' does not exist: %s",
                AwsSdkJavaCodegenSettings.CUSTOMIZATION_CONFIG, location.toAbsolutePath()));
        }
        return ModelLoaderUtils.loadModel(CustomizationConfig.class, location.toFile());
    }

    private void registerGeneratedFiles(FileManifest manifest, Path base) {
        if (!Files.isDirectory(base)) {
            return;
        }
        try (Stream<Path> files = Files.walk(base)) {
            files.filter(Files::isRegularFile).forEach(manifest::addFile);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to register generated files with the manifest", e);
        }
    }
}
