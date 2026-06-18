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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URL;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

class PresignedUrlDownloadRequestTest {

    @Test
    void equalsAndHashCode_shouldFollowContract() {
        EqualsVerifier.forClass(PresignedUrlDownloadRequest.class)
                      .verify();
    }

    @Test
    void build_withAllFields_shouldCreateRequest() throws Exception {
        URL url = new URL("https://example.com");
        PresignedUrlDownloadRequest request = PresignedUrlDownloadRequest.builder()
                                                                           .presignedUrl(url)
                                                                           .range("bytes=0-100")
                                                                           .build();

        assertThat(request.presignedUrl()).isEqualTo(url);
        assertThat(request.range()).isEqualTo("bytes=0-100");
    }

    @Test
    void build_withOnlyRequiredFields_shouldHaveNullOptionals() throws Exception {
        URL url = new URL("https://example.com");
        PresignedUrlDownloadRequest request = PresignedUrlDownloadRequest.builder()
                                                                           .presignedUrl(url)
                                                                           .build();

        assertThat(request.presignedUrl()).isEqualTo(url);
        assertThat(request.range()).isNull();
    }

    @Test
    void toBuilder_withAllFields_shouldPreserveValues() throws Exception {
        URL url = new URL("https://example.com");
        PresignedUrlDownloadRequest original = PresignedUrlDownloadRequest.builder()
                                                                          .presignedUrl(url)
                                                                          .range("bytes=0-100")
                                                                          .ifMatch("\"etag\"")
                                                                          .ifNoneMatch("\"other\"")
                                                                          .ifModifiedSince("Wed, 17 Jun 2026 17:46:28 GMT")
                                                                          .ifUnmodifiedSince("Wed, 17 Jun 2026 17:46:28 GMT")
                                                                          .build();

        PresignedUrlDownloadRequest copy = original.toBuilder().build();

        assertThat(copy.presignedUrl()).isEqualTo(original.presignedUrl());
        assertThat(copy.range()).isEqualTo(original.range());
        assertThat(copy.ifMatch()).isEqualTo("\"etag\"");
        assertThat(copy.ifNoneMatch()).isEqualTo("\"other\"");
        assertThat(copy.ifModifiedSince()).isEqualTo("Wed, 17 Jun 2026 17:46:28 GMT");
        assertThat(copy.ifUnmodifiedSince()).isEqualTo("Wed, 17 Jun 2026 17:46:28 GMT");
    }

    @Test
    void toBuilder_withModification_shouldNotAffectOriginal() throws Exception {
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
    void toString_shouldContainFieldValues() throws Exception {
        URL url = new URL("https://example.com");
        String range = "bytes=0-100";

        PresignedUrlDownloadRequest request = PresignedUrlDownloadRequest.builder()
                                                                           .presignedUrl(url)
                                                                           .range(range)
                                                                           .build();

        assertThat(request.toString())
            .contains(url.toString())
            .contains("bytes=0-100");
    }

    @Test
    void serializableBuilderClass_shouldReturnBuilderImpl() {
        assertThat(PresignedUrlDownloadRequest.serializableBuilderClass())
            .isEqualTo(PresignedUrlDownloadRequest.BuilderImpl.class);
    }

    @Test
    void build_withPresignedUrlHavingRangeInSignedHeaders_shouldThrow() throws Exception {
        URL url = new URL("https://bucket.s3.us-east-2.amazonaws.com/key"
                          + "?X-Amz-Algorithm=AWS4-HMAC-SHA256"
                          + "&X-Amz-SignedHeaders=host%3Brange"
                          + "&X-Amz-Signature=abc123");

        assertThatThrownBy(() -> PresignedUrlDownloadRequest.builder().presignedUrl(url).build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("range")
            .hasMessageContaining("presignedGetObjectRequest()");
    }

    @Test
    void build_withPresignedUrlHavingIfMatchInSignedHeaders_shouldThrow() throws Exception {
        URL url = new URL("https://bucket.s3.us-east-2.amazonaws.com/key"
                          + "?X-Amz-Algorithm=AWS4-HMAC-SHA256"
                          + "&X-Amz-SignedHeaders=host%3Bif-match"
                          + "&X-Amz-Signature=abc123");

        assertThatThrownBy(() -> PresignedUrlDownloadRequest.builder().presignedUrl(url).build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("if-match")
            .hasMessageContaining("presignedGetObjectRequest()");
    }

    @Test
    void build_withConflictingRange_shouldThrow() throws Exception {
        PresignedGetObjectRequest presigned = createPresignedGetObjectRequest(
            mapOf("host", "bucket.s3.amazonaws.com", "range", "bytes=0-1023"));

        assertThatThrownBy(() ->
                               PresignedUrlDownloadRequest.builder()
                                                          .presignedGetObjectRequest(presigned)
                                                          .range("bytes=500-999")
                                                          .build()
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("conflicts with signed value");
    }

    @Test
    void build_withBothPresignedUrlAndPresignedGetObjectRequest_shouldThrow() throws Exception {
        PresignedGetObjectRequest presigned = createPresignedGetObjectRequest(
            mapOf("host", "bucket.s3.amazonaws.com"));

        assertThatThrownBy(() ->
                               PresignedUrlDownloadRequest.builder()
                                                          .presignedGetObjectRequest(presigned)
                                                          .presignedUrl(new URL("https://bucket.s3.amazonaws.com/key"))
                                                          .build()
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("Cannot set both");
    }

    @Test
    void build_withPresignedGetObjectRequest_shouldExtractUrlAndSignedHeaders() throws Exception {
        Map<String, List<String>> headers = mapOf("host", "bucket.s3.amazonaws.com", "range", "bytes=0-1023");
        PresignedGetObjectRequest presigned = createPresignedGetObjectRequest(headers);

        PresignedUrlDownloadRequest request = PresignedUrlDownloadRequest.builder()
                                                                         .presignedGetObjectRequest(presigned)
                                                                         .build();

        assertThat(request.presignedUrl()).isEqualTo(presigned.url());
        assertThat(request.signedHeaders()).containsKey("range");
        assertThat(request.signedHeaders().get("range")).containsExactly("bytes=0-1023");
    }

    private static PresignedGetObjectRequest createPresignedGetObjectRequest(
        Map<String, List<String>> signedHeaders) throws Exception {
        return PresignedGetObjectRequest.builder()
                                        .expiration(Instant.now().plusSeconds(600))
                                        .isBrowserExecutable(false)
                                        .signedHeaders(signedHeaders)
                                        .httpRequest(SdkHttpFullRequest.builder()
                                                                       .method(SdkHttpMethod.GET)
                                                                       .protocol("https")
                                                                       .host("bucket.s3.amazonaws.com")
                                                                       .encodedPath("/key")
                                                                       .build())
                                        .build();
    }

    private static Map<String, List<String>> mapOf(String... keyValues) {
        Map<String, List<String>> map = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(keyValues[i], Arrays.asList(keyValues[i + 1]));
        }
        return map;
    }
}
