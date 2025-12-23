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
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.codegen.internal.ExampleMetadataProvider;
import software.amazon.awssdk.codegen.model.intermediate.Metadata;

public class OperationDocProviderTest {

    private static final String TEST_EXAMPLE_META_PATH = "software/amazon/awssdk/codegen/test-example-meta.json";

    @ParameterizedTest
    @MethodSource("createLinkToCodeExampleTestCases")
    public void exampleMetadataService_createLinkToCodeExample_returnsExpectedResult(
            String serviceName, String operationName, String expectedUrl) {
        ExampleMetadataProvider provider = new ExampleMetadataProvider(TEST_EXAMPLE_META_PATH);

        Optional<String> result = provider.createLinkToCodeExample(createTestMetadata(serviceName), operationName);

        if (expectedUrl == null) {
            assertThat(result).isEmpty();
        } else {
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(String.format("<a href=\"%s\" target=\"_top\">Code Example</a>", expectedUrl));
        }
    }

    private static Stream<Arguments> createLinkToCodeExampleTestCases() {
        return Stream.of(
                Arguments.of("s3", "GetObject",
                        "https://docs.aws.amazon.com/code-library/latest/ug/s3_example_s3_GetObject_section.html"),
                Arguments.of("medicalimaging", "GetImageSet",
                        "https://docs.aws.amazon.com/code-library/latest/ug/medical-imaging_example_medical-imaging_GetImageSet_section.html"),
                Arguments.of("s3", "NonExistentOperation", null),
                Arguments.of("nonexistent-service", "GetObject", null)
        );
    }

    @Test
    public void constructor_withNullPath_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> new ExampleMetadataProvider(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("exampleMetaPath cannot be null");
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
