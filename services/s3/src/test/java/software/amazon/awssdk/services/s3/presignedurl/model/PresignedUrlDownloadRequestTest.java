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
import java.util.Collections;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class PresignedUrlDownloadRequestTest {

    @Test
    void equalsAndHashCode_shouldFollowContract() {
        EqualsVerifier.forClass(PresignedUrlDownloadRequest.class)
                      .verify();
    }

    @Test
    void builder_shouldCreateRequestWithAllFields() throws Exception {
        URL url = new URL("https://example.com");
        PresignedUrlDownloadRequest request = PresignedUrlDownloadRequest.builder()
                                                                           .presignedUrl(url)
                                                                           .putHeader("Range", "bytes=0-100")
                                                                           .build();

        assertThat(request.presignedUrl()).isEqualTo(url);
        assertThat(request.headers().get("Range")).isEqualTo(Collections.singletonList("bytes=0-100"));
    }

    @Test
    void builder_shouldCreateRequestWithOnlyRequiredFields() throws Exception {
        URL url = new URL("https://example.com");
        PresignedUrlDownloadRequest request = PresignedUrlDownloadRequest.builder()
                                                                           .presignedUrl(url)
                                                                           .build();

        assertThat(request.presignedUrl()).isEqualTo(url);
        assertThat(request.headers()).isEmpty();
    }

    @Test
    void toBuilder_shouldCreateBuilderFromExistingRequest() throws Exception {
        URL url = new URL("https://example.com");
        PresignedUrlDownloadRequest original = PresignedUrlDownloadRequest.builder()
                                                                            .presignedUrl(url)
                                                                            .putHeader("Range", "bytes=0-100")
                                                                            .build();

        PresignedUrlDownloadRequest copy = original.toBuilder().build();

        assertThat(copy.presignedUrl()).isEqualTo(original.presignedUrl());
        assertThat(copy.headers()).isEqualTo(original.headers());
    }

    @Test
    void toBuilder_shouldAllowModification() throws Exception {
        URL url1 = new URL("https://example.com");
        URL url2 = new URL("https://other.com");
        PresignedUrlDownloadRequest original = PresignedUrlDownloadRequest.builder()
                                                                            .presignedUrl(url1)
                                                                            .putHeader("Range", "bytes=0-100")
                                                                            .build();

        PresignedUrlDownloadRequest modified = original.toBuilder()
                                                        .presignedUrl(url2)
                                                        .putHeader("Range", "bytes=200-300")
                                                        .build();

        assertThat(modified.presignedUrl()).isEqualTo(url2);
        assertThat(modified.headers().get("Range")).isEqualTo(Collections.singletonList("bytes=200-300"));
        // Original unchanged
        assertThat(original.presignedUrl()).isEqualTo(url1);
        assertThat(original.headers().get("Range")).isEqualTo(Collections.singletonList("bytes=0-100"));
    }

    @Test
    void toString_shouldContainActualFieldValues() throws Exception {
        URL url = new URL("https://example.com");

        PresignedUrlDownloadRequest request = PresignedUrlDownloadRequest.builder()
                                                                           .presignedUrl(url)
                                                                           .putHeader("Range", "bytes=0-100")
                                                                           .build();

        String result = request.toString();

        assertThat(result)
            .isNotNull()
            .isNotEmpty()
            .contains(request.presignedUrl().toString());
    }

    @Test
    void serializableBuilderClass_shouldReturnCorrectClass() {
        assertThat(PresignedUrlDownloadRequest.serializableBuilderClass())
            .isEqualTo(PresignedUrlDownloadRequest.BuilderImpl.class);
    }
}
