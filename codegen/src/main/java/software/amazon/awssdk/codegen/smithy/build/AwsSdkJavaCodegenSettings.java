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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.smithy.model.node.ExpectationNotMetException;
import software.amazon.smithy.model.node.ObjectNode;

/**
 * Settings for the {@value AwsSdkJavaCodegenPlugin#NAME} plugin.
 */
@SdkInternalApi
public final class AwsSdkJavaCodegenSettings {

    static final String CUSTOMIZATION_CONFIG = "customizationConfig";
    static final String BASE_DIR = "baseDir";
    static final String SOURCES_DIRECTORY = "sourcesDirectory";
    static final String RESOURCES_DIRECTORY = "resourcesDirectory";
    static final String TESTS_DIRECTORY = "testsDirectory";

    private static final List<String> OUTPUT_DIRECTORIES =
        Arrays.asList(SOURCES_DIRECTORY, RESOURCES_DIRECTORY, TESTS_DIRECTORY);

    private final Path customizationConfig;
    private final Path sourcesDirectory;
    private final Path resourcesDirectory;
    private final Path testsDirectory;

    private AwsSdkJavaCodegenSettings(Path customizationConfig, Path sourcesDirectory, Path resourcesDirectory,
                                      Path testsDirectory) {
        this.customizationConfig = customizationConfig;
        this.sourcesDirectory = sourcesDirectory;
        this.resourcesDirectory = resourcesDirectory;
        this.testsDirectory = testsDirectory;
    }

    /**
     * Creates settings from plugin configuration.
     */
    public static AwsSdkJavaCodegenSettings fromNode(ObjectNode node) {
        node.expectNoAdditionalProperties(Arrays.asList(CUSTOMIZATION_CONFIG, BASE_DIR, SOURCES_DIRECTORY,
                                                        RESOURCES_DIRECTORY, TESTS_DIRECTORY));

        Path baseDir = node.getStringMember(BASE_DIR)
                           .map(s -> Paths.get(s.getValue()))
                           .orElseGet(() -> Paths.get(""));

        Path customizationConfig = node.getStringMember(CUSTOMIZATION_CONFIG)
                                       .map(s -> baseDir.resolve(s.getValue()))
                                       .orElse(null);

        List<String> setDirectories = OUTPUT_DIRECTORIES.stream()
                                                        .filter(key -> node.getMember(key).isPresent())
                                                        .collect(Collectors.toList());
        if (!setDirectories.isEmpty() && setDirectories.size() != OUTPUT_DIRECTORIES.size()) {
            throw new ExpectationNotMetException(String.format(
                "Set all of %s or none of them, but only %s are set.", OUTPUT_DIRECTORIES, setDirectories), node);
        }

        return new AwsSdkJavaCodegenSettings(customizationConfig,
                                             directory(node, SOURCES_DIRECTORY, baseDir),
                                             directory(node, RESOURCES_DIRECTORY, baseDir),
                                             directory(node, TESTS_DIRECTORY, baseDir));
    }

    private static Path directory(ObjectNode node, String key, Path baseDir) {
        return node.getStringMember(key).map(s -> baseDir.resolve(s.getValue())).orElse(null);
    }

    /**
     * Configured customization file, if any.
     */
    public Path customizationConfig() {
        return customizationConfig;
    }

    /**
     * Directory for generated sources, if set. When the output directories are not set, the plugin writes into the
     * smithy-build projection instead.
     */
    public Optional<Path> sourcesDirectory() {
        return Optional.ofNullable(sourcesDirectory);
    }

    /**
     * Directory for generated resources, if set. Set together with {@link #sourcesDirectory()}.
     */
    public Optional<Path> resourcesDirectory() {
        return Optional.ofNullable(resourcesDirectory);
    }

    /**
     * Directory for generated test sources, if set. Set together with {@link #sourcesDirectory()}.
     */
    public Optional<Path> testsDirectory() {
        return Optional.ofNullable(testsDirectory);
    }

    /**
     * Whether the output directories are set.
     */
    boolean hasOutputDirectories() {
        return Stream.of(sourcesDirectory, resourcesDirectory, testsDirectory).allMatch(p -> p != null);
    }
}
