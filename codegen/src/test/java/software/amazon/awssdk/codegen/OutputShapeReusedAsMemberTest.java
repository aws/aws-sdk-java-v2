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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.service.ServiceModel;
import software.amazon.awssdk.codegen.utils.ModelLoaderUtils;

/**
 * Verifies that a structure which serves as both an operation output and a member of another structure is generated
 * correctly. This is a valid Smithy modeling pattern that client generators must support. Reproduces JAVA-9165.
 */
public class OutputShapeReusedAsMemberTest {

    private Path outputDir;

    @BeforeEach
    void setup() throws IOException {
        outputDir = Files.createTempDirectory("output-shape-reused-as-member");
    }

    @AfterEach
    void teardown() throws IOException {
        deleteDirectory(outputDir);
    }

    @Test
    void build_outputShapeReusedAsMember_resolvesMemberToResponseShape() {
        IntermediateModel model = new IntermediateModelBuilder(loadModels()).build();

        ShapeModel analyticsResponse = model.getShapes().get("GetAppAnalyticsSummaryResponse");
        assertThat(analyticsResponse).isNotNull();

        MemberModel directMember = analyticsResponse.findMemberModelByC2jName("SubscriptionSummary");
        assertThat(directMember.getShape())
            .as("output shape reused as a direct member should resolve to a shape")
            .isNotNull();
        assertThat(directMember.getShape().getC2jName()).isEqualTo("GetAppSubscriptionSummaryResponse");
        assertThat(directMember.getVariable().getVariableType()).isEqualTo("GetAppSubscriptionSummaryResponse");

        MemberModel listMember = analyticsResponse.findMemberModelByC2jName("SubscriptionSummaries");
        assertThat(listMember.getListModel().getListMemberModel().getC2jShape())
            .isEqualTo("GetAppSubscriptionSummaryResponse");

        assertThat(model.getShapes().get("GetAppSubscriptionSummaryResponse")).isNotNull();
    }

    @Test
    void generateCode_outputShapeReusedAsMember_doesNotThrow() {
        Path sources = outputDir.resolve("generated-sources").resolve("sdk");
        Path resources = outputDir.resolve("generated-resources").resolve("sdk-resources");
        Path tests = outputDir.resolve("generated-test-sources").resolve("sdk-tests");

        assertThatCode(() -> CodeGenerator.builder()
                                          .models(loadModels())
                                          .sourcesDirectory(sources.toAbsolutePath().toString())
                                          .resourcesDirectory(resources.toAbsolutePath().toString())
                                          .testsDirectory(tests.toAbsolutePath().toString())
                                          .build()
                                          .execute())
            .doesNotThrowAnyException();
    }

    private C2jModels loadModels() {
        File serviceModelFile = new File(OutputShapeReusedAsMemberTest.class
            .getResource("poet/client/c2j/output-shape-reused-as-member/service-2.json").getFile());

        return C2jModels.builder()
                        .serviceModel(ModelLoaderUtils.loadModel(ServiceModel.class, serviceModelFile))
                        .customizationConfig(CustomizationConfig.create())
                        .build();
    }

    private static void deleteDirectory(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
