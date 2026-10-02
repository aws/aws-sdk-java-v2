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

package software.amazon.awssdk.codegen.emitters.tasks;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.codegen.poet.ClientTestModels.restJsonServiceModels;

import java.io.InputStream;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;
import software.amazon.awssdk.codegen.internal.ExampleMetadataProvider;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.Metadata;

public class PackageInfoGeneratorTasksTest {

    private static final String TEST_EXAMPLE_META_PATH = "software/amazon/awssdk/codegen/test-example-meta.json";

    @ParameterizedTest
    @MethodSource("getServiceCodeExamplesTestCases")
    public void exampleMetadataService_getServiceCodeExamples_returnsExpectedResult(
            String metadataPath, String serviceName,
            Integer expectedSize, String expectedFirstTitle, String expectedFirstCategory, String expectedFirstUrl) {
        ExampleMetadataProvider provider = new ExampleMetadataProvider(metadataPath);

        List<ExampleMetadataProvider.ExampleData> result = provider.getServiceCodeExamples(createTestMetadata(serviceName));

        if (expectedSize == null || expectedSize == 0) {
            assertThat(result).isEmpty();
        } else {
            assertThat(result).hasSize(expectedSize);
            if (expectedFirstTitle != null) {
                assertThat(result.get(0).getTitle()).isEqualTo(expectedFirstTitle);
            }
            if (expectedFirstCategory != null) {
                assertThat(result.get(0).getCategory()).isEqualTo(expectedFirstCategory);
            }
            if (expectedFirstUrl != null) {
                assertThat(result.get(0).getUrl()).isEqualTo(expectedFirstUrl);
            }
        }
    }

    private static Stream<Arguments> getServiceCodeExamplesTestCases() {
        return Stream.of(
                Arguments.of(TEST_EXAMPLE_META_PATH, "s3",
                        5, "Get an object from a bucket", "Api", 
                        "https://docs.aws.amazon.com/code-library/latest/ug/s3_example_s3_GetObject_section.html"),
                Arguments.of(TEST_EXAMPLE_META_PATH, "medicalimaging",
                        1, "Get image set properties", "Api", 
                        "https://docs.aws.amazon.com/code-library/latest/ug/medical-imaging_example_medical-imaging_GetImageSet_section.html"),
                Arguments.of(TEST_EXAMPLE_META_PATH, "empty-service",
                        0, null, null, null),
                Arguments.of(TEST_EXAMPLE_META_PATH, "nonexistent",
                        0, null, null, null),
                Arguments.of("nonexistent/path.json", "s3",
                        0, null, null, null)
        );
    }

    @ParameterizedTest
    @MethodSource("buildPackageInfoContentTestCases")
    public void buildPackageInfoContent_generatesExpectedContent(
            String serviceName, String expectedFixtureFile) {
        String actualContent = generatePackageInfoContent(serviceName);
        String expectedContent = loadFixtureFile(expectedFixtureFile);

        assertThat(actualContent).isEqualToIgnoringWhitespace(expectedContent);
    }

    private static Stream<Arguments> buildPackageInfoContentTestCases() {
        return Stream.of(
                Arguments.of("s3", "s3-package-info.java"),
                Arguments.of("medicalimaging", "medical-imaging-package-info.java"),
                Arguments.of("empty-service", "empty-service-package-info.java"),
                Arguments.of("nonexistent", "empty-service-package-info.java")
        );
    }
    
    private PackageInfoGeneratorTasks createTestGenerator() {
        IntermediateModel model = restJsonServiceModels();
        GeneratorTaskParams dependencies = GeneratorTaskParams.create(model, "sources/", "tests/", "resources/");
        return new PackageInfoGeneratorTasks(dependencies);
    }

    private Metadata createTestMetadata(String serviceName) {
        Metadata metadata = new Metadata();
        metadata.setServiceName(serviceName);
        metadata.setDocumentation("Test service documentation.");
        metadata.setRootPackageName("software.amazon.awssdk.services");
        metadata.setClientPackageName("testservice");
        return metadata;
    }

    private String generatePackageInfoContent(String serviceName) {
        PackageInfoGeneratorTasks generator = createTestGenerator();
        Metadata metadata = createTestMetadata(serviceName);
        return generator.buildPackageInfoContent(metadata, TEST_EXAMPLE_META_PATH);
    }

    private String loadFixtureFile(String filename) {
        InputStream is = getClass().getResourceAsStream(filename);
        if (is == null) {
            throw new RuntimeException("Fixture file not found: " + filename);
        }
        return new Scanner(is).useDelimiter("\\A").next();
    }
}
