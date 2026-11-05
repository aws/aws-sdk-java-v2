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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static software.amazon.awssdk.codegen.maven.plugin.SmithyTestModule.PLUGIN;
import static software.amazon.awssdk.codegen.maven.plugin.SmithyTestModule.REQUEST;
import static software.amazon.awssdk.codegen.maven.plugin.SmithyTestModule.SERVICE;
import static software.amazon.awssdk.codegen.maven.plugin.SmithyTestModule.SOURCES;
import static software.amazon.awssdk.codegen.maven.plugin.SmithyTestModule.config;
import static software.amazon.awssdk.codegen.maven.plugin.SmithyTestModule.containsFileEndingWith;
import static software.amazon.awssdk.codegen.maven.plugin.SmithyTestModule.project;
import static software.amazon.awssdk.codegen.maven.plugin.SmithyTestModule.write;

import java.lang.reflect.Field;
import java.nio.file.Path;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GenerationMojoSmithyRoutingTest {

    @Test
    void smithyBuildConfigPresent_routesToSmithyBuild(@TempDir Path moduleDir) throws Exception {
        write(moduleDir.resolve("model.smithy"), SERVICE + REQUEST);
        write(moduleDir.resolve("smithy-build.json"), config(SOURCES, PLUGIN));

        MavenProject project = project(moduleDir);
        Path outputDirectory = moduleDir.resolve("target");

        GenerationMojo mojo = new GenerationMojo();
        // Maven injects these private @Parameter fields at runtime. The Smithy branch reads only these two.
        setField(mojo, "project", project);
        setField(mojo, "outputDirectory", outputDirectory.toString());

        mojo.execute();

        Path generatedSources = outputDirectory.resolve("smithyprojections")
                                               .resolve("sdk")
                                               .resolve(PLUGIN)
                                               .resolve("generated-sources");
        assertTrue(project.getCompileSourceRoots().contains(generatedSources.toAbsolutePath().toString()),
                   "expected " + generatedSources + " in " + project.getCompileSourceRoots());
        assertTrue(containsFileEndingWith(generatedSources, "Client.java"),
                   "execute() should have driven the plugin through smithy-build and produced a client");
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
