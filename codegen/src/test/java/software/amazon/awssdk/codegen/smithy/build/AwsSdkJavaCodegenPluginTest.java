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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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

    @Test
    void paginatedAndOverriddenOperations_generatePaginators(@TempDir Path outputDir) throws IOException {
        String paginatedModel =
            "$version: \"2.0\"\nnamespace demo\n\n"
            + "use aws.api#service\n"
            + "use aws.auth#sigv4\n"
            + "use aws.protocols#restJson1\n"
            + "use software.amazon.awssdk.codegen#paginatorOverride\n"
            + "@service(sdkId: \"Demo\", arnNamespace: \"demo\")\n"
            + "@sigv4(name: \"demo\")\n"
            + "@restJson1\n"
            + "service DemoService { version: \"2024-01-01\", operations: [ListThings, ListParts] }\n\n"
            + "@paginated(inputToken: \"nextToken\", outputToken: \"nextToken\", items: \"things\", "
            + "pageSize: \"maxResults\")\n"
            + "@readonly @http(method: \"GET\", uri: \"/things\")\n"
            + "operation ListThings { input: ListThingsRequest, output: ListThingsResponse }\n"
            + "structure ListThingsRequest {\n"
            + "  @httpQuery(\"nextToken\") nextToken: String\n"
            + "  @httpQuery(\"maxResults\") maxResults: Integer\n"
            + "}\n"
            + "structure ListThingsResponse { nextToken: String, things: Names }\n"
            + "@paginatorOverride(inputToken: [\"marker\"], outputToken: [\"nextMarker\"], moreResults: \"isTruncated\", "
            + "resultKey: [\"parts\", \"prefixes\"], limitKey: \"maxParts\")\n"
            + "@readonly @http(method: \"GET\", uri: \"/parts\")\n"
            + "operation ListParts { input: ListPartsRequest, output: ListPartsResponse }\n"
            + "structure ListPartsRequest {\n"
            + "  @httpQuery(\"marker\") marker: String\n"
            + "  @httpQuery(\"maxParts\") maxParts: Integer\n"
            + "}\n"
            + "structure ListPartsResponse { nextMarker: String, isTruncated: Boolean, parts: Names, prefixes: Names }\n"
            + "list Names { member: String }\n";
        Model model = Model.assembler(LOADER)
                           .discoverModels(LOADER)
                           .addUnparsedModel("test.smithy", paginatedModel)
                           .assemble()
                           .unwrap();
        ProjectionConfig sdk = ProjectionConfig.builder()
                                               .plugins(Collections.singletonMap(AwsSdkJavaCodegenPlugin.NAME,
                                                                                 Node.objectNode()))
                                               .build();
        SmithyBuildResult result = SmithyBuild.create(LOADER)
                                              .config(SmithyBuildConfig.builder()
                                                                       .version("1.0")
                                                                       .projections(Collections.singletonMap("sdk", sdk))
                                                                       .build())
                                              .model(model)
                                              .outputDirectory(outputDir)
                                              .build();

        assertThat(result.anyBroken()).as("smithy-build should not be broken").isFalse();
        Map<String, Path> generated = result.getProjectionResult("sdk").get()
                                            .getPluginManifest(AwsSdkJavaCodegenPlugin.NAME).get()
                                            .getFiles().stream()
                                            .collect(Collectors.toMap(p -> p.getFileName().toString(), p -> p,
                                                                      (first, second) -> first));
        assertThat(generated).containsKeys("ListThingsIterable.java", "ListThingsPublisher.java",
                                           "ListPartsIterable.java", "ListPartsPublisher.java");
        // The override's moreResults decides when to stop, and each result key gets its own item method.
        String listParts = new String(Files.readAllBytes(generated.get("ListPartsIterable.java")),
                                      StandardCharsets.UTF_8);
        assertThat(listParts).contains("previousPage.isTruncated() != null && previousPage.isTruncated().booleanValue()")
                             .contains("public final SdkIterable<String> parts()")
                             .contains("public final SdkIterable<String> prefixes()");
        String client = new String(Files.readAllBytes(generated.get("DemoClient.java")), StandardCharsets.UTF_8);
        assertThat(client).contains("listThingsPaginator(").contains("listPartsPaginator(");
    }
}
