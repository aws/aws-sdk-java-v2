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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClientTestModels;
import software.amazon.awssdk.codegen.validation.ModelValidator;

public class CodeGeneratorTest {
    private static final String VALIDATION_REPORT_NAME = "validation-report.json";

    private Path outputDir;

    @BeforeEach
    void methodSetup() throws IOException {
        outputDir = Files.createTempDirectory(null);
    }

    @AfterEach
    void methodTeardown() throws IOException {
        deleteDirectory(outputDir);
    }

    @Test
    void build_cj2ModelsAndIntermediateModelSet_throws() {
        assertThatThrownBy(() -> CodeGenerator.builder()
                                              .models(C2jModels.builder().build())
                                              .intermediateModel(new IntermediateModel())
                                              .build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Only one of");
    }

    @Test
    void execute_emitValidationReportIsFalse_doesNotEmitValidationReport() throws IOException {
        generateCodeFromC2jModels(ClientTestModels.awsJsonServiceC2jModels(), outputDir);
        assertThat(Files.exists(validationReportPath(outputDir))).isFalse();
    }

    @Test
    void execute_emitValidationReportIsTrue_emitsValidationReport() throws IOException {
        generateCodeFromC2jModels(ClientTestModels.awsJsonServiceC2jModels(), outputDir, true, null);
        assertThat(Files.exists(validationReportPath(outputDir))).isTrue();
    }

    @Test
    void execute_invokesModelValidators() {
        ModelValidator mockValidator = mock(ModelValidator.class);
        when(mockValidator.validateModels(any())).thenReturn(Collections.emptyList());

        generateCodeFromC2jModels(ClientTestModels.awsJsonServiceC2jModels(), outputDir, true,
                                  Collections.singletonList(mockValidator));

        verify(mockValidator).validateModels(any());
    }

    @Test
    void execute_c2jModelsAndIntermediateModel_generateSameCode() throws IOException {
        Path c2jModelsOutputDir = outputDir.resolve("c2jModels");
        generateCodeFromC2jModels(ClientTestModels.awsJsonServiceC2jModels(), c2jModelsOutputDir, false, Collections.emptyList());

        Path intermediateModelOutputDir = outputDir.resolve("intermediate-model");
        generateCodeFromIntermediateModel(ClientTestModels.awsJsonServiceModels(), intermediateModelOutputDir);

        List<Path> c2jModels_generatedFiles = Files.walk(c2jModelsOutputDir)
                                                   .sorted()
                                                   .map(c2jModelsOutputDir::relativize)
                                                   .collect(Collectors.toList());

        List<Path> intermediateModels_generatedFiles = Files.walk(intermediateModelOutputDir)
                                                            .sorted()
                                                            .map(intermediateModelOutputDir::relativize)
                                                            .collect(Collectors.toList());

        assertThat(c2jModels_generatedFiles).isNotEmpty();

        // Ensure same exact set of files
        assertThat(c2jModels_generatedFiles).isEqualTo(intermediateModels_generatedFiles);

        // All files should be exactly the same
        for (Path generatedFile : c2jModels_generatedFiles) {
            Path c2jGenerated = c2jModelsOutputDir.resolve(generatedFile);
            Path intermediateGenerated = intermediateModelOutputDir.resolve(generatedFile);

            if (Files.isDirectory(c2jGenerated)) {
                assertThat(Files.isDirectory(intermediateGenerated)).isTrue();
            } else {
                assertThat(readToString(c2jGenerated)).isEqualTo(readToString(intermediateGenerated));
            }
        }
    }

    private void generateCodeFromC2jModels(C2jModels c2jModels, Path outputDir) {
        generateCodeFromC2jModels(c2jModels, outputDir, false, null);
    }

    private void generateCodeFromC2jModels(C2jModels c2jModels, Path outputDir,
                                           boolean emitValidationReport,
                                           List<ModelValidator> modelValidators) {
        Path sources = outputDir.resolve("generated-sources").resolve("sdk");
        Path resources = outputDir.resolve("generated-resources").resolve("sdk-resources");
        Path tests = outputDir.resolve("generated-test-sources").resolve("sdk-tests");

        CodeGenerator.builder()
                     .models(c2jModels)
                     .sourcesDirectory(sources.toAbsolutePath().toString())
                     .resourcesDirectory(resources.toAbsolutePath().toString())
                     .testsDirectory(tests.toAbsolutePath().toString())
                     .emitValidationReport(emitValidationReport)
                     .modelValidators(modelValidators)
                     .build()
                     .execute();
    }

    private void generateCodeFromIntermediateModel(IntermediateModel intermediateModel, Path outputDir) {
        Path sources = outputDir.resolve("generated-sources").resolve("sdk");
        Path resources = outputDir.resolve("generated-resources").resolve("sdk-resources");
        Path tests = outputDir.resolve("generated-test-sources").resolve("sdk-tests");

        CodeGenerator.builder()
                     .intermediateModel(intermediateModel)
                     .sourcesDirectory(sources.toAbsolutePath().toString())
                     .resourcesDirectory(resources.toAbsolutePath().toString())
                     .testsDirectory(tests.toAbsolutePath().toString())
                     .build()
                     .execute();
    }

    private static String readToString(Path p) throws IOException {
        ByteBuffer bb = ByteBuffer.wrap(Files.readAllBytes(p));
        return StandardCharsets.UTF_8.decode(bb).toString();
    }

    private static Path validationReportPath(Path root) {
        return root.resolve(Paths.get("generated-sources", "sdk", "models", VALIDATION_REPORT_NAME));
    }

    private static void deleteDirectory(Path dir) throws IOException {
        Files.walkFileTree(dir, new FileVisitor<Path>() {

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                return FileVisitResult.TERMINATE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
