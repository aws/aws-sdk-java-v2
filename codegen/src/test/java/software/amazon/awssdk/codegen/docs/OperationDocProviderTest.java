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

package software.amazon.awssdk.codegen.docs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.internal.ExampleMetadataProvider;
import software.amazon.awssdk.codegen.model.intermediate.Metadata;

public class OperationDocProviderTest {

    private static final String TEST_EXAMPLE_META_PATH = "software/amazon/awssdk/codegen/test-example-meta.json";

    @AfterEach
    void cleanupCache() {
        ExampleMetadataProvider.clearCache();
    }

    @Test
    public void exampleMetadataService_createLinkToCodeExample_withValidExample_returnsCorrectLink() {
        ExampleMetadataProvider provider = ExampleMetadataProvider.getInstance(TEST_EXAMPLE_META_PATH);

        Optional<String> result = provider.createLinkToCodeExample(createTestMetadata("s3"), "GetObject");

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo("<a href=\"https://docs.aws.amazon.com/code-library/latest/ug/s3_example_s3_GetObject_section.html\" target=\"_top\">Code Example</a>");
    }

    @Test
    public void exampleMetadataService_createLinkToCodeExample_withNonExistentExample_returnsEmpty() {
        ExampleMetadataProvider provider = ExampleMetadataProvider.getInstance(TEST_EXAMPLE_META_PATH);

        Optional<String> result = provider.createLinkToCodeExample(createTestMetadata("s3"), "NonExistentOperation");

        assertThat(result).isEmpty();
    }

    @Test
    public void registryPattern_withMultiplePaths_maintainsSeparateInstances() {
        ExampleMetadataProvider provider1 = ExampleMetadataProvider.getInstance(TEST_EXAMPLE_META_PATH);
        ExampleMetadataProvider provider2 = ExampleMetadataProvider.getInstance("nonexistent/path.json");

        assertThat(provider1).isNotSameAs(provider2);

        ExampleMetadataProvider provider1Again = ExampleMetadataProvider.getInstance(TEST_EXAMPLE_META_PATH);
        assertThat(provider1).isSameAs(provider1Again);
    }

    @Test
    public void registryPattern_threadSafety_handlesNullPath() {
        assertThatThrownBy(() -> ExampleMetadataProvider.getInstance(null))
            .as("getInstance should reject null paths")
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("exampleMetaPath cannot be null");
    }

    @Test
    public void exampleMetadataService_createLinkToCodeExample_withMedicalImagingService_returnsCorrectLink() {
        ExampleMetadataProvider provider = ExampleMetadataProvider.getInstance(TEST_EXAMPLE_META_PATH);

        Optional<String> result = provider.createLinkToCodeExample(createTestMetadata("medicalimaging"), "GetImageSet");

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo("<a href=\"https://docs.aws.amazon.com/code-library/latest/ug/medical-imaging_example_medical-imaging_GetImageSet_section.html\" target=\"_top\">Code Example</a>");
    }

    @Test
    public void exampleMetadataService_createLinkToCodeExample_withNonExistentService_returnsEmpty() {
        ExampleMetadataProvider provider = ExampleMetadataProvider.getInstance(TEST_EXAMPLE_META_PATH);

        Optional<String> result = provider.createLinkToCodeExample(createTestMetadata("nonexistent-service"), "GetObject");

        assertThat(result).isEmpty();
    }

    private Metadata createTestMetadata(String serviceName) {
        Metadata metadata = new Metadata();
        metadata.setServiceName(serviceName);
        metadata.setDocumentation("Test service documentation.");
        metadata.setRootPackageName("software.amazon.awssdk.services");
        metadata.setClientPackageName("testservice");
        return metadata;
    }
}
