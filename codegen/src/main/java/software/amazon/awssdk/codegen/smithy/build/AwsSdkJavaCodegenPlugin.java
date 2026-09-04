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
import software.amazon.awssdk.codegen.internal.Utils;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.smithy.SmithyIntermediateModelBuilder;
import software.amazon.awssdk.codegen.smithy.SmithyModelWithCustomizations;
import software.amazon.awssdk.codegen.utils.ModelLoaderUtils;
import software.amazon.smithy.build.FileManifest;
import software.amazon.smithy.build.PluginContext;
import software.amazon.smithy.build.SmithyBuildPlugin;

/**
 * A {@code smithy-build} plugin that generates an AWS SDK for Java v2 client from a Smithy model.
 *
 * <p>The plugin is discovered through Java SPI, so it becomes available to {@code smithy-build} simply by being on
 * the classpath. That makes it usable from the Smithy CLI and the Smithy Gradle plugins in addition to the SDK's
 * own Maven build.
 *
 * <p>Output is written relative to the plugin's {@link FileManifest} base directory:
 * <ul>
 *     <li>{@code java/} - generated client sources</li>
 *     <li>{@code resources/} - generated resources</li>
 *     <li>{@code tests/} - generated test sources</li>
 * </ul>
 *
 * @see AwsSdkJavaCodegenSettings for the supported settings
 */
@SdkInternalApi
public final class AwsSdkJavaCodegenPlugin implements SmithyBuildPlugin {
    /**
     * The name used to reference this plugin from {@code smithy-build.json}.
     */
    public static final String NAME = "aws-sdk-java-v2-codegen";

    static final String SOURCES_DIR = "java";
    static final String RESOURCES_DIR = "resources";
    static final String TESTS_DIR = "tests";

    private static final Logger log = LoggerFactory.getLogger(AwsSdkJavaCodegenPlugin.class);

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void execute(PluginContext context) {
        AwsSdkJavaCodegenSettings settings = AwsSdkJavaCodegenSettings.fromNode(context.getSettings());
        FileManifest manifest = context.getFileManifest();
        Path baseDir = manifest.getBaseDir();

        Path sources = baseDir.resolve(SOURCES_DIR);
        Path resources = baseDir.resolve(RESOURCES_DIR);
        Path tests = baseDir.resolve(TESTS_DIR);
        createDirectories(sources, resources, tests);

        SmithyModelWithCustomizations model =
            SmithyModelWithCustomizations.builder()
                                         .smithyModel(context.getModel())
                                         .service(settings.service())
                                         .customizationConfig(loadCustomizationConfig(settings))
                                         .build();

        IntermediateModel intermediateModel = new SmithyIntermediateModelBuilder(model).build();

        log.info("Generating AWS SDK for Java v2 client for service '{}' into {}",
                 intermediateModel.getMetadata().getServiceName(), baseDir);

        CodeGenerator.builder()
                     .intermediateModel(intermediateModel)
                     .sourcesDirectory(sources.toString())
                     .resourcesDirectory(resources.toString())
                     .testsDirectory(tests.toString())
                     .intermediateModelFileNamePrefix(settings.writeIntermediateModel()
                                                      ? Utils.getFileNamePrefix(intermediateModel)
                                                      : null)
                     .build()
                     .execute();

        registerGeneratedFiles(manifest, baseDir);
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
        return ModelLoaderUtils.loadModel(CustomizationConfig.class, location.toFile(), true);
    }

    private static void createDirectories(Path... directories) {
        for (Path directory : directories) {
            try {
                Files.createDirectories(directory);
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to create output directory " + directory, e);
            }
        }
    }

    /**
     * Records generated files with the manifest. {@link CodeGenerator} writes directly to the filesystem rather than
     * through the manifest, so the files are collected afterwards to keep the manifest an accurate description of
     * what this plugin produced.
     */
    private static void registerGeneratedFiles(FileManifest manifest, Path baseDir) {
        try (Stream<Path> files = Files.walk(baseDir)) {
            files.filter(Files::isRegularFile).forEach(manifest::addFile);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to enumerate generated files under " + baseDir, e);
        }
    }
}
