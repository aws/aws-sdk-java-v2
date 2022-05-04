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

package software.amazon.awssdk.codegen.lite.maven.plugin;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import software.amazon.awssdk.codegen.lite.CodeGenerator;
import software.amazon.awssdk.codegen.lite.defaultsmode.DefaultConfiguration;
import software.amazon.awssdk.codegen.lite.defaultsmode.DefaultsLoader;
import software.amazon.awssdk.codegen.lite.defaultsmode.DefaultsModeConfigurationGenerator;
import software.amazon.awssdk.codegen.lite.defaultsmode.DefaultsModeGenerator;
import software.amazon.awssdk.utils.StringUtils;

/**
 * The Maven mojo to generate defaults mode related classes.
 */
@Mojo(name = "generate-defaults-mode")
public class DefaultsModeGenerationMojo extends AbstractMojo {

    private static final String DEFAULTS_MODE_BASE = "software.amazon.awssdk.awscore.defaultsmode";
    private static final String DEFAULTS_MODE_CONFIGURATION_BASE = "software.amazon.awssdk.awscore.internal.defaultsmode";

    @Parameter(property = "outputDirectory", defaultValue = "${project.build.directory}")
    private String outputDirectory;

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @Parameter(property = "defaultConfigurationFile", defaultValue =
        "${basedir}/src/main/resources/software/amazon/awssdk/awscore/internal/defaults/sdk-default-configuration.json")
    private File defaultConfigurationFile;

    public void execute() {
        Path baseSourcesDirectory = Paths.get(outputDirectory).resolve("generated-sources").resolve("sdk");
        Path testsDirectory = Paths.get(outputDirectory).resolve("generated-test-sources").resolve("sdk-tests");

        DefaultConfiguration configuration = DefaultsLoader.load(defaultConfigurationFile);

        generateDefaultsModeClass(baseSourcesDirectory, configuration);
        generateDefaultsModeConfiguartionClass(baseSourcesDirectory, configuration);

        project.addCompileSourceRoot(baseSourcesDirectory.toFile().getAbsolutePath());
        project.addTestCompileSourceRoot(testsDirectory.toFile().getAbsolutePath());
    }

    public void generateDefaultsModeClass(Path baseSourcesDirectory, DefaultConfiguration configuration) {
        Path sourcesDirectory = baseSourcesDirectory.resolve(StringUtils.replace(DEFAULTS_MODE_BASE, ".", "/"));
        new CodeGenerator(sourcesDirectory.toString(), new DefaultsModeGenerator(DEFAULTS_MODE_BASE, configuration)).generate();
    }

    public void generateDefaultsModeConfiguartionClass(Path baseSourcesDirectory, DefaultConfiguration configuration) {
        Path sourcesDirectory = baseSourcesDirectory.resolve(StringUtils.replace(DEFAULTS_MODE_CONFIGURATION_BASE, ".", "/"));
        new CodeGenerator(sourcesDirectory.toString(), new DefaultsModeConfigurationGenerator(DEFAULTS_MODE_CONFIGURATION_BASE,
                                                                                              DEFAULTS_MODE_BASE,
                                                                                              configuration)).generate();
    }
}
