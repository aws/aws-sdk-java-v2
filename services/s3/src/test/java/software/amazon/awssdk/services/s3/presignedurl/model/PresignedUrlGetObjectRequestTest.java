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

package software.amazon.awssdk.services.s3.presignedurl.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class PresignedUrlGetObjectRequestTest {

    @Test
    void equalsAndHashCode_shouldFollowContract() {
        EqualsVerifier.forClass(PresignedUrlGetObjectRequest.class)
                      .verify();
    }

    @Test
    void builder_shouldCreateRequestWithAllFields() throws Exception {
        URL url = new URL("https://example.com");
        PresignedUrlGetObjectRequest request = PresignedUrlGetObjectRequest.builder()
                                                                           .presignedUrl(url)
                                                                           .range("bytes=0-100")
                                                                           .build();

        assertThat(request.presignedUrl()).isEqualTo(url);
        assertThat(request.range()).isEqualTo("bytes=0-100");
    }

    @Test
    void builder_shouldCreateRequestWithOnlyRequiredFields() throws Exception {
        URL url = new URL("https://example.com");
        PresignedUrlGetObjectRequest request = PresignedUrlGetObjectRequest.builder()
                                                                           .presignedUrl(url)
                                                                           .build();

        assertThat(request.presignedUrl()).isEqualTo(url);
        assertThat(request.range()).isNull();
    }

    @Test
    void toBuilder_shouldCreateBuilderFromExistingRequest() throws Exception {
        URL url = new URL("https://example.com");
        PresignedUrlGetObjectRequest original = PresignedUrlGetObjectRequest.builder()
                                                                            .presignedUrl(url)
                                                                            .range("bytes=0-100")
                                                                            .build();

        PresignedUrlGetObjectRequest copy = original.toBuilder().build();

        assertThat(copy.presignedUrl()).isEqualTo(original.presignedUrl());
        assertThat(copy.range()).isEqualTo(original.range());
    }

    @Test
    void toBuilder_shouldAllowModification() throws Exception {
        URL url1 = new URL("https://example.com");
        URL url2 = new URL("https://other.com");
        PresignedUrlGetObjectRequest original = PresignedUrlGetObjectRequest.builder()
                                                                            .presignedUrl(url1)
                                                                            .range("bytes=0-100")
                                                                            .build();

        PresignedUrlGetObjectRequest modified = original.toBuilder()
                                                        .presignedUrl(url2)
                                                        .range("bytes=200-300")
                                                        .build();

        assertThat(modified.presignedUrl()).isEqualTo(url2);
        assertThat(modified.range()).isEqualTo("bytes=200-300");
        // Original unchanged
        assertThat(original.presignedUrl()).isEqualTo(url1);
        assertThat(original.range()).isEqualTo("bytes=0-100");
    }

    @Test
    void toString_shouldContainActualFieldValues() throws Exception {
        URL url = new URL("https://example.com");
        String range = "bytes=0-100";

        PresignedUrlGetObjectRequest request = PresignedUrlGetObjectRequest.builder()
                                                                           .presignedUrl(url)
                                                                           .range(range)
                                                                           .build();

        String result = request.toString();

        assertThat(result)
            .isNotNull()
            .isNotEmpty()
            .contains(request.presignedUrl().toString())
            .contains(request.range());

    }

    @Test
    void serializableBuilderClass_shouldReturnCorrectClass() {
        assertThat(PresignedUrlGetObjectRequest.serializableBuilderClass())
            .isEqualTo(PresignedUrlGetObjectRequest.BuilderImpl.class);
    }
}
