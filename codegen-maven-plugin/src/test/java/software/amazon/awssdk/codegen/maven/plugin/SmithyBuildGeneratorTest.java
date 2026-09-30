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

package software.amazon.awssdk.codegen.maven.plugin;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static software.amazon.awssdk.codegen.maven.plugin.SmithyTestModule.PLUGIN;
import static software.amazon.awssdk.codegen.maven.plugin.SmithyTestModule.REQUEST;
import static software.amazon.awssdk.codegen.maven.plugin.SmithyTestModule.SERVICE;
import static software.amazon.awssdk.codegen.maven.plugin.SmithyTestModule.SOURCES;
import static software.amazon.awssdk.codegen.maven.plugin.SmithyTestModule.config;
import static software.amazon.awssdk.codegen.maven.plugin.SmithyTestModule.containsFileEndingWith;
import static software.amazon.awssdk.codegen.maven.plugin.SmithyTestModule.project;
import static software.amazon.awssdk.codegen.maven.plugin.SmithyTestModule.write;

import java.nio.file.Path;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SmithyBuildGeneratorTest {

    @TempDir
    Path moduleDir;

    @Test
    void missingSourcesAndImports_fails() throws Exception {
        Path configFile = write(moduleDir.resolve("smithy-build.json"),
                                "{ \"version\": \"1.0\", \"projections\": { \"sdk\": { "
                                + "\"plugins\": { \"" + PLUGIN + "\": {} } } } }");

        MojoExecutionException e = assertThrows(MojoExecutionException.class,
                                                () -> generate(project(moduleDir), configFile));
        assertTrue(e.getMessage().contains("No 'sources' or 'imports'"), e.getMessage());
    }

    @Test
    void traitWithoutDefinition_generationContinues() throws Exception {
        write(moduleDir.resolve("model.smithy"),
              SERVICE + "@custom#notDefined\n" + REQUEST);
        Path configFile = write(moduleDir.resolve("smithy-build.json"), config(SOURCES, PLUGIN));

        generate(project(moduleDir), configFile);

        assertTrue(containsFileEndingWith(outputDirectory(), "Client.java"));
    }

    @Test
    void otherValidationError_failsNamingTheError() throws Exception {
        write(moduleDir.resolve("model.smithy"),
              SERVICE + "structure OpRequest { name: Bad }\n@length(min: 5, max: 1)\nstring Bad\n");
        Path configFile = write(moduleDir.resolve("smithy-build.json"), config(SOURCES, PLUGIN));

        MojoExecutionException e = assertThrows(MojoExecutionException.class,
                                                () -> generate(project(moduleDir), configFile));
        String messages = messages(e);
        assertTrue(messages.contains("demo#Bad") && messages.contains("length"), messages);
    }

    @Test
    void sourceReferencingShapeFromImport_generates() throws Exception {
        write(moduleDir.resolve("model.smithy"), SERVICE + "structure OpRequest { name: Name }\n");
        write(moduleDir.resolve("shared.smithy"), "$version: \"2.0\"\nnamespace demo\nstring Name\n");
        Path configFile = write(moduleDir.resolve("smithy-build.json"),
                                config(SOURCES + ", \"imports\": [\"shared.smithy\"]", PLUGIN));

        generate(project(moduleDir), configFile);

        assertTrue(containsFileEndingWith(outputDirectory(), "Client.java"));
    }

    @Test
    void labeledPlugin_registersLabeledOutput() throws Exception {
        write(moduleDir.resolve("model.smithy"), SERVICE + REQUEST);
        Path configFile = write(moduleDir.resolve("smithy-build.json"), config(SOURCES, PLUGIN + "::demo"));
        MavenProject project = project(moduleDir);

        generate(project, configFile);

        Path generatedSources = outputDirectory().resolve("sdk").resolve("demo").resolve("generated-sources");
        assertTrue(project.getCompileSourceRoots().contains(generatedSources.toAbsolutePath().toString()),
                   "expected " + generatedSources + " in " + project.getCompileSourceRoots());
    }

    private void generate(MavenProject project, Path configFile) throws MojoExecutionException {
        new SmithyBuildGenerator(project, new SystemStreamLog()).generate(configFile, outputDirectory());
    }

    private Path outputDirectory() {
        return moduleDir.resolve("target").resolve("smithyprojections");
    }

    private static String messages(Throwable t) {
        StringBuilder messages = new StringBuilder();
        for (Throwable cause = t; cause != null; cause = cause.getCause()) {
            messages.append(cause.getMessage()).append('\n');
        }
        return messages.toString();
    }
}
