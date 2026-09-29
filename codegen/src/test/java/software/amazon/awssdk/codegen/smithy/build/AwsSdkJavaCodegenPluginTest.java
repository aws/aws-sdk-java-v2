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

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.smithy.build.FileManifest;
import software.amazon.smithy.build.ProjectionResult;
import software.amazon.smithy.build.SmithyBuild;
import software.amazon.smithy.build.SmithyBuildPlugin;
import software.amazon.smithy.build.SmithyBuildResult;
import software.amazon.smithy.build.model.ProjectionConfig;
import software.amazon.smithy.build.model.SmithyBuildConfig;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;

class AwsSdkJavaCodegenPluginTest {

    private static final String MODEL =
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
        + "structure OpRequest { name: String }\n"
        + "structure OpResponse { name: String }\n";

    private static final ClassLoader LOADER = AwsSdkJavaCodegenPluginTest.class.getClassLoader();

    @Test
    void plugin_isDiscoverableByName() {
        AwsSdkJavaCodegenPlugin plugin = new AwsSdkJavaCodegenPlugin();
        assertThat(plugin.getName()).isEqualTo("aws-sdk-java-v2-codegen");
        assertThat(plugin.requiresValidModel()).isFalse();
        assertThat(SmithyBuildPlugin.createServiceFactory(LOADER).apply(AwsSdkJavaCodegenPlugin.NAME))
            .as("plugin resolves through META-INF/services")
            .isPresent();
    }

    @Test
    void smithyBuild_drivesCodegen_andReportsGeneratedFiles(@TempDir Path outputDir) {
        Model model = Model.assembler()
                           .discoverModels(LOADER)
                           .addUnparsedModel("test.smithy", MODEL)
                           .assemble()
                           .unwrap();

        ProjectionConfig sdk = ProjectionConfig.builder()
                                               .plugins(Collections.singletonMap(AwsSdkJavaCodegenPlugin.NAME,
                                                                                 Node.objectNode()))
                                               .build();
        SmithyBuildConfig config = SmithyBuildConfig.builder()
                                                    .version("1.0")
                                                    .projections(Collections.singletonMap("sdk", sdk))
                                                    .build();

        SmithyBuildResult result = SmithyBuild.create(LOADER)
                                              .config(config)
                                              .model(model)
                                              .outputDirectory(outputDir)
                                              .build();

        assertThat(result.anyBroken()).as("smithy-build should not be broken").isFalse();

        ProjectionResult sdkResult = result.getProjectionResult("sdk")
                                           .orElseThrow(() -> new AssertionError("no 'sdk' projection result"));

        FileManifest manifest = sdkResult.getPluginManifest(AwsSdkJavaCodegenPlugin.NAME)
                                         .orElseThrow(() -> new AssertionError("plugin manifest missing"));

        List<String> generated = manifest.getFiles().stream()
                                         .map(p -> p.getFileName().toString())
                                         .collect(Collectors.toList());

        assertThat(generated).as("generator produced sources").isNotEmpty();
        assertThat(generated).as("generated a client from the projected model")
                             .anyMatch(f -> f.endsWith("Client.java"));
    }
}
