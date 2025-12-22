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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;
import software.amazon.awssdk.codegen.internal.ExampleMetadataProvider;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.Metadata;

public class PackageInfoGeneratorTasksTest {

    private static final String TEST_EXAMPLE_META_PATH = "software/amazon/awssdk/codegen/test-example-meta.json";

    @AfterEach
    void cleanupCache() {
        ExampleMetadataProvider.clearCache();
    }

    @Test
    public void exampleMetadataService_withExamples_returnsCorrectExamples() {
        ExampleMetadataProvider provider = ExampleMetadataProvider.getInstance(TEST_EXAMPLE_META_PATH);

        List<ExampleMetadataProvider.ExampleData> result = provider.getServiceCodeExamples(createTestMetadata("s3"));

        assertThat(result).hasSize(5);
        assertThat(result.get(0).getTitle()).isEqualTo("Get an object from a bucket");
        assertThat(result.get(0).getCategory()).isEqualTo("Api");
        assertThat(result.get(0).getUrl()).contains("s3_example_s3_GetObject_section.html");
    }

    @Test
    public void exampleMetadataService_withoutExamples_returnsEmptyList() {
        ExampleMetadataProvider provider = ExampleMetadataProvider.getInstance(TEST_EXAMPLE_META_PATH);

        List<ExampleMetadataProvider.ExampleData> result = provider.getServiceCodeExamples(createTestMetadata("empty-service"));

        assertThat(result).isEmpty();
    }

    @Test
    public void exampleMetadataService_withNonExistentService_returnsEmptyList() {
        ExampleMetadataProvider provider = ExampleMetadataProvider.getInstance(TEST_EXAMPLE_META_PATH);

        List<ExampleMetadataProvider.ExampleData> result = provider.getServiceCodeExamples(createTestMetadata("nonexistent"));

        assertThat(result).isEmpty();
    }

    @Test
    public void exampleMetadataService_withMissingExampleFile_returnsEmptyList() {
        ExampleMetadataProvider provider = ExampleMetadataProvider.getInstance("nonexistent/path.json");

        List<ExampleMetadataProvider.ExampleData> result = provider.getServiceCodeExamples(createTestMetadata("s3"));

        assertThat(result).isEmpty();
    }

    @Test
    public void exampleMetadataService_withMedicalImagingService_returnsCorrectExamples() {
        ExampleMetadataProvider provider = ExampleMetadataProvider.getInstance(TEST_EXAMPLE_META_PATH);

        List<ExampleMetadataProvider.ExampleData> result = provider.getServiceCodeExamples(createTestMetadata("medicalimaging"));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Get image set properties");
        assertThat(result.get(0).getCategory()).isEqualTo("Api");
        assertThat(result.get(0).getUrl()).contains("medical-imaging_example_medical-imaging_GetImageSet_section.html");
    }

    @Test
    public void buildPackageInfoContent_withS3Examples_generatesExpectedContent() {
        String actualContent = generatePackageInfoContent("s3");
        String expectedContent = loadFixtureFile("s3-package-info.java");

        assertThat(actualContent).isEqualToIgnoringWhitespace(expectedContent);
    }

    @Test
    public void buildPackageInfoContent_withMedicalImagingExamples_generatesExpectedContent() {
        String actualContent = generatePackageInfoContent("medicalimaging");
        String expectedContent = loadFixtureFile("medical-imaging-package-info.java");

        assertThat(actualContent).isEqualToIgnoringWhitespace(expectedContent);
    }

    @Test
    public void buildPackageInfoContent_withNoExamples_generatesContentWithoutCodeExamples() {
        String actualContent = generatePackageInfoContent("empty-service");
        String expectedContent = loadFixtureFile("empty-service-package-info.java");

        assertThat(actualContent).isEqualToIgnoringWhitespace(expectedContent);
    }

    @Test
    public void buildPackageInfoContent_withNonExistentService_generatesContentWithoutCodeExamples() {
        String actualContent = generatePackageInfoContent("nonexistent");
        String expectedContent = loadFixtureFile("empty-service-package-info.java");

        assertThat(actualContent).isEqualToIgnoringWhitespace(expectedContent);
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
