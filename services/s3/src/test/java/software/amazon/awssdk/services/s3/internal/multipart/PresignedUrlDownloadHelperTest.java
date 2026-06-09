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

package software.amazon.awssdk.services.s3.internal.multipart;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.presignedurl.model.PresignedUrlDownloadRequest;

class PresignedUrlDownloadHelperTest {

    @Test
    void validatePartResponse_validResponse_shouldReturnEmpty() {
        GetObjectResponse response = GetObjectResponse.builder()
                                                      .contentRange("bytes 0-15/32")
                                                      .contentLength(16L)
                                                      .build();

        Optional<SdkClientException> result = PresignedUrlDownloadHelper.validatePartResponse(
            response, 0, 16L, 32L, 2);

        assertThat(result).isEmpty();
    }

    @Test
    void validatePartResponse_missingContentRange_shouldReturnError() {
        GetObjectResponse response = GetObjectResponse.builder()
                                                      .contentLength(16L)
                                                      .build();

        Optional<SdkClientException> result = PresignedUrlDownloadHelper.validatePartResponse(
            response, 0, 16L, 32L, 2);

        assertThat(result).isPresent();
        assertThat(result.get().getMessage()).contains("No Content-Range header");
    }

    @Test
    void validatePartResponse_invalidContentLength_shouldReturnError() {
        GetObjectResponse response = GetObjectResponse.builder()
                                                      .contentRange("bytes 0-15/32")
                                                      .contentLength(-1L)
                                                      .build();

        Optional<SdkClientException> result = PresignedUrlDownloadHelper.validatePartResponse(
            response, 0, 16L, 32L, 2);

        assertThat(result).isPresent();
        assertThat(result.get().getMessage()).contains("Invalid or missing Content-Length");
    }

    @Test
    void validatePartResponse_contentRangeMismatch_shouldReturnError() {
        GetObjectResponse response = GetObjectResponse.builder()
                                                      .contentRange("bytes 5-20/32")
                                                      .contentLength(16L)
                                                      .build();

        Optional<SdkClientException> result = PresignedUrlDownloadHelper.validatePartResponse(
            response, 0, 16L, 32L, 2);

        assertThat(result).isPresent();
        assertThat(result.get().getMessage()).contains("Content-Range mismatch for part 0");
    }

    @Test
    void validatePartResponse_contentLengthMismatch_shouldReturnError() {
        GetObjectResponse response = GetObjectResponse.builder()
                                                      .contentRange("bytes 0-15/32")
                                                      .contentLength(10L)
                                                      .build();

        Optional<SdkClientException> result = PresignedUrlDownloadHelper.validatePartResponse(
            response, 0, 16L, 32L, 2);

        assertThat(result).isPresent();
        assertThat(result.get().getMessage()).contains("content length validation failed");
    }

    @Test
    void validatePartResponse_lastPartSmallerSize_shouldPass() {
        // 30-byte object, 16-byte parts → part 1 is bytes 16-29 (14 bytes)
        GetObjectResponse response = GetObjectResponse.builder()
                                                      .contentRange("bytes 16-29/30")
                                                      .contentLength(14L)
                                                      .build();

        Optional<SdkClientException> result = PresignedUrlDownloadHelper.validatePartResponse(
            response, 1, 16L, 30L, 2);

        assertThat(result).isEmpty();
    }

    @Test
    void validatePartResponse_nullTotalContentLength_shouldStillValidateRange() {
        GetObjectResponse response = GetObjectResponse.builder()
                                                      .contentRange("bytes 0-15/32")
                                                      .contentLength(16L)
                                                      .build();

        // When totalContentLength is null, content-length check is skipped but range check still works
        Optional<SdkClientException> result = PresignedUrlDownloadHelper.validatePartResponse(
            response, 0, 16L, null, null);

        assertThat(result).isEmpty();
    }

    @Test
    void createRangedGetRequest_firstPart_shouldNotIncludeIfMatch() throws MalformedURLException {
        URL url = new URL("https://bucket.s3.amazonaws.com/key?X-Amz-Signature=abc");
        PresignedUrlDownloadRequest original = PresignedUrlDownloadRequest.builder()
                                                                         .presignedUrl(url)
                                                                         .build();

        PresignedUrlDownloadRequest result = PresignedUrlDownloadHelper.createRangedGetRequest(
            original, 0, 16L, 32L, "\"etag\"");

        assertThat(result.range()).isEqualTo("bytes=0-15");
        assertThat(result.ifMatch()).isNull();
        assertThat(result.presignedUrl()).isEqualTo(url);
    }

    @Test
    void createRangedGetRequest_secondPart_shouldIncludeIfMatch() throws MalformedURLException {
        URL url = new URL("https://bucket.s3.amazonaws.com/key?X-Amz-Signature=abc");
        PresignedUrlDownloadRequest original = PresignedUrlDownloadRequest.builder()
                                                                         .presignedUrl(url)
                                                                         .build();

        PresignedUrlDownloadRequest result = PresignedUrlDownloadHelper.createRangedGetRequest(
            original, 1, 16L, 32L, "\"etag\"");

        assertThat(result.range()).isEqualTo("bytes=16-31");
        assertThat(result.ifMatch()).isEqualTo("\"etag\"");
    }

    @Test
    void createRangedGetRequest_lastPartClamped_shouldNotExceedTotalSize() throws MalformedURLException {
        URL url = new URL("https://bucket.s3.amazonaws.com/key?X-Amz-Signature=abc");
        PresignedUrlDownloadRequest original = PresignedUrlDownloadRequest.builder()
                                                                         .presignedUrl(url)
                                                                         .build();

        // 30-byte object, 16-byte parts → part 1 should be bytes=16-29 (not 16-31)
        PresignedUrlDownloadRequest result = PresignedUrlDownloadHelper.createRangedGetRequest(
            original, 1, 16L, 30L, "\"etag\"");

        assertThat(result.range()).isEqualTo("bytes=16-29");
    }

    @Test
    void createRangedGetRequest_nullTotalContentLength_shouldUseFullPartSize() throws MalformedURLException {
        URL url = new URL("https://bucket.s3.amazonaws.com/key?X-Amz-Signature=abc");
        PresignedUrlDownloadRequest original = PresignedUrlDownloadRequest.builder()
                                                                         .presignedUrl(url)
                                                                         .build();

        // First part when total size unknown — uses full part size without clamping
        PresignedUrlDownloadRequest result = PresignedUrlDownloadHelper.createRangedGetRequest(
            original, 0, 16L, null, null);

        assertThat(result.range()).isEqualTo("bytes=0-15");
    }
}
