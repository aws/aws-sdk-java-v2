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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.apache.maven.project.MavenProject;

/**
 * Builds a minimal service module on disk for tests: a one-operation Smithy model and a {@code smithy-build.json}.
 */
final class SmithyTestModule {

    static final String PLUGIN = "aws-sdk-java-v2-codegen";
    static final String SOURCES = "\"sources\": [\"model.smithy\"]";

    /**
     * A service with one operation. {@code OpRequest} is not defined, so each test supplies it.
     */
    static final String SERVICE =
        "$version: \"2.0\"\nnamespace demo\n\n"
        + "use aws.api#service\n"
        + "use aws.auth#sigv4\n"
        + "use aws.protocols#restJson1\n"
        + "@service(sdkId: \"Demo\", arnNamespace: \"demo\")\n"
        + "@sigv4(name: \"demo\")\n"
        + "@restJson1\n"
        + "service DemoService { version: \"2024-01-01\", operations: [Op] }\n\n"
        + "@http(method: \"POST\", uri: \"/op\")\n"
        + "operation Op { input: OpRequest, output: OpResponse }\n"
        + "structure OpResponse { name: String }\n";

    static final String REQUEST = "structure OpRequest { name: String }\n";

    private SmithyTestModule() {
    }

    /**
     * Returns a {@code smithy-build.json} with the given model-file settings and one {@code sdk} projection that runs
     * the given plugin ID.
     */
    static String config(String modelFiles, String pluginId) {
        return "{ \"version\": \"1.0\", " + modelFiles + ", "
               + "\"projections\": { \"sdk\": { \"plugins\": { \"" + pluginId + "\": {} } } } }";
    }

    static MavenProject project(Path moduleDir) {
        MavenProject project = new MavenProject();
        project.setVersion("2.0.0");
        // setFile derives the base directory from the file's parent, so getBasedir() resolves to moduleDir.
        project.setFile(new File(moduleDir.toFile(), "pom.xml"));
        return project;
    }

    static Path write(Path path, String content) throws IOException {
        Files.write(path, content.getBytes(StandardCharsets.UTF_8));
        return path;
    }

    static boolean containsFileEndingWith(Path root, String suffix) throws IOException {
        if (!Files.isDirectory(root)) {
            return false;
        }
        try (Stream<Path> files = Files.walk(root)) {
            return files.filter(Files::isRegularFile)
                        .anyMatch(p -> p.getFileName().toString().endsWith(suffix));
        }
    }
}
