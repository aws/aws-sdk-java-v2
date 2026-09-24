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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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
                                                                           .range("bytes=0-100")
                                                                           .build();

        assertThat(request.presignedUrl()).isEqualTo(url);
        assertThat(request.range()).isEqualTo("bytes=0-100");
    }

    @Test
    void builder_shouldCreateRequestWithOnlyRequiredFields() throws Exception {
        URL url = new URL("https://example.com");
        PresignedUrlDownloadRequest request = PresignedUrlDownloadRequest.builder()
                                                                           .presignedUrl(url)
                                                                           .build();

        assertThat(request.presignedUrl()).isEqualTo(url);
        assertThat(request.range()).isNull();
    }

    @Test
    void toBuilder_shouldCreateBuilderFromExistingRequest() throws Exception {
        URL url = new URL("https://example.com");
        PresignedUrlDownloadRequest original = PresignedUrlDownloadRequest.builder()
                                                                            .presignedUrl(url)
                                                                            .range("bytes=0-100")
                                                                            .build();

        PresignedUrlDownloadRequest copy = original.toBuilder().build();

        assertThat(copy.presignedUrl()).isEqualTo(original.presignedUrl());
        assertThat(copy.range()).isEqualTo(original.range());
    }

    @Test
    void toBuilder_shouldAllowModification() throws Exception {
        URL url1 = new URL("https://example.com");
        URL url2 = new URL("https://other.com");
        PresignedUrlDownloadRequest original = PresignedUrlDownloadRequest.builder()
                                                                            .presignedUrl(url1)
                                                                            .range("bytes=0-100")
                                                                            .build();

        PresignedUrlDownloadRequest modified = original.toBuilder()
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
    void toString_redactsPresignedUrlQueryString() throws Exception {
        URL url = new URL("https://bucket.s3.us-east-1.amazonaws.com/dir/key.txt?" +
                          "X-Amz-Algorithm=AWS4-HMAC-SHA256&" +
                          "X-Amz-Credential=AKIAEXAMPLE%2F20240101%2Fus-east-1%2Fs3%2Faws4_request&" +
                          "X-Amz-Date=20240101T000000Z&" +
                          "X-Amz-Expires=604800&" +
                          "X-Amz-Security-Token=SESSIONTOKENEXAMPLE&" +
                          "X-Amz-SignedHeaders=host&" +
                          "X-Amz-Signature=deadbeefdeadbeef");

        PresignedUrlDownloadRequest request = PresignedUrlDownloadRequest.builder()
                                                                           .presignedUrl(url)
                                                                           .range("bytes=0-100")
                                                                           .ifMatch("etag")
                                                                           .build();

        String result = request.toString();

        assertThat(result)
            .isNotNull()
            .isNotEmpty()
            .doesNotContain("X-Amz-Signature")
            .doesNotContain("deadbeef")
            .doesNotContain("X-Amz-Credential")
            .doesNotContain("AKIAEXAMPLE")
            .doesNotContain("X-Amz-Security-Token")
            .doesNotContain("SESSIONTOKENEXAMPLE")
            .doesNotContain(url.getQuery())
            .contains("https://bucket.s3.us-east-1.amazonaws.com/dir/key.txt")
            .contains("*** Sensitive Data Redacted ***")
            .contains("bytes=0-100")
            .contains("etag");
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "https://example.com/key                              | https://example.com/key",
        "https://example.com                                  | https://example.com",
        "https://host:8443/key?X-Amz-Signature=abc            | https://host:8443/key?*** Sensitive Data Redacted ***",
        "https://***@host/key?X-Amz-Signature=abc#frag| https://host/key?*** Sensitive Data Redacted ***",
        "https://host/key#X-Amz-Signature=abc                 | https://host/key",
        "https://host/key?                                    | https://host/key?*** Sensitive Data Redacted ***"
    })
    void toString_rendersUrlWithoutQueryString(String url, String expectedUrlRendering) throws Exception {
        assertThat(toStringOf(new URL(url)))
            .isEqualTo("PresignedUrlDownloadRequest(PresignedUrl=" + expectedUrlRendering + ")");
    }

    @Test
    void serializableBuilderClass_shouldReturnCorrectClass() {
        assertThat(PresignedUrlDownloadRequest.serializableBuilderClass())
            .isEqualTo(PresignedUrlDownloadRequest.BuilderImpl.class);
    }

    private String toStringOf(URL url) {
        return PresignedUrlDownloadRequest.builder()
                                         .presignedUrl(url)
                                         .build()
                                         .toString();
    }
}
