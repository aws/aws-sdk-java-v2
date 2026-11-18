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

package software.amazon.awssdk.core.internal.chunked;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.http.SdkHttpFullResponse;

class AwsChunkedResponseDecodingUtilsTest {

    @ParameterizedTest(name = "Content-Encoding: {0}")
    @CsvSource(delimiter = '|', value = {
        "aws-chunked       | true",
        "gzip,aws-chunked  | true",
        "gzip, aws-chunked | true",
        "AWS-CHUNKED       | true",
        "gzip              | false"
    })
    void shouldDecode_whenEnabled_dependsOnContentEncoding(String contentEncoding, boolean expected) {
        SdkHttpFullResponse response = response().putHeader("Content-Encoding", contentEncoding).build();

        assertThat(AwsChunkedResponseDecodingUtils.shouldDecode(response, enabled())).isEqualTo(expected);
    }

    @Test
    void shouldDecode_whenEncodingsSplitAcrossHeaders_usesLastValue() {
        SdkHttpFullResponse response = response().appendHeader("Content-Encoding", "gzip")
                                                 .appendHeader("Content-Encoding", "aws-chunked")
                                                 .build();

        assertThat(AwsChunkedResponseDecodingUtils.shouldDecode(response, enabled())).isTrue();
    }

    @ParameterizedTest(name = "Content-Encoding: {0}")
    @ValueSource(strings = {"aws-chunked,gzip", "aws-chunked,aws-chunked"})
    void shouldDecode_whenAwsChunkedNotLastOrRepeated_throws(String contentEncoding) {
        SdkHttpFullResponse response = response().putHeader("Content-Encoding", contentEncoding).build();

        assertThatThrownBy(() -> AwsChunkedResponseDecodingUtils.shouldDecode(response, enabled()))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("aws-chunked must appear exactly once, as the last Content-Encoding value");
    }

    @Test
    void shouldDecode_whenNotEnabledForOperation_returnsFalse() {
        SdkHttpFullResponse response = response().putHeader("Content-Encoding", "aws-chunked").build();

        assertThat(AwsChunkedResponseDecodingUtils.shouldDecode(response, new ExecutionAttributes())).isFalse();
    }

    @Test
    void shouldDecode_whenNoContentEncoding_returnsFalse() {
        assertThat(AwsChunkedResponseDecodingUtils.shouldDecode(response().build(), enabled())).isFalse();
    }

    @Test
    void decodedContentLength_whenValid_returnsValue() {
        SdkHttpFullResponse response = response().putHeader("x-amz-decoded-content-length", "11").build();

        assertThat(AwsChunkedResponseDecodingUtils.decodedContentLength(response)).isEqualTo(11);
    }

    @Test
    void decodedContentLength_whenMissing_throws() {
        assertThatThrownBy(() -> AwsChunkedResponseDecodingUtils.decodedContentLength(response().build()))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("x-amz-decoded-content-length header is missing");
    }

    @ParameterizedTest(name = "x-amz-decoded-content-length: \"{0}\"")
    @ValueSource(strings = {"abc", "-1", "1.5"})
    void decodedContentLength_whenInvalid_throws(String value) {
        SdkHttpFullResponse response = response().putHeader("x-amz-decoded-content-length", value).build();

        assertThatThrownBy(() -> AwsChunkedResponseDecodingUtils.decodedContentLength(response))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("not a valid non-negative integer");
    }

    @Test
    void declaredTrailers_whenPresent_returnsLowerCaseNames() {
        SdkHttpFullResponse response = response().appendHeader("x-amz-trailer", "X-Amz-Checksum-CRC32, other ,")
                                                 .appendHeader("x-amz-trailer", "third")
                                                 .build();

        assertThat(AwsChunkedResponseDecodingUtils.declaredTrailers(response))
            .containsExactlyInAnyOrder("x-amz-checksum-crc32", "other", "third");
    }

    @Test
    void declaredTrailers_whenAbsent_returnsEmptySet() {
        assertThat(AwsChunkedResponseDecodingUtils.declaredTrailers(response().build())).isEmpty();
    }

    @Test
    void rewriteHeaders_whenOnlyAwsChunked_removesFramingHeaders() {
        SdkHttpFullResponse response = response().putHeader("Content-Encoding", "aws-chunked")
                                                 .putHeader("Content-Length", "64")
                                                 .putHeader("x-amz-decoded-content-length", "11")
                                                 .putHeader("x-amz-trailer", "x-amz-checksum-crc32")
                                                 .putHeader("ETag", "abc")
                                                 .build();

        SdkHttpFullResponse rewritten = AwsChunkedResponseDecodingUtils.rewriteHeaders(response, 11);

        assertThat(rewritten.statusCode()).isEqualTo(200);
        assertThat(rewritten.firstMatchingHeader("Content-Length")).hasValue("11");
        assertThat(rewritten.firstMatchingHeader("Content-Encoding")).isEmpty();
        assertThat(rewritten.firstMatchingHeader("x-amz-decoded-content-length")).isEmpty();
        assertThat(rewritten.firstMatchingHeader("x-amz-trailer")).isEmpty();
        assertThat(rewritten.firstMatchingHeader("ETag")).hasValue("abc");
    }

    @Test
    void rewriteHeaders_whenOtherEncodingsPresent_keepsThem() {
        SdkHttpFullResponse response = response().putHeader("Content-Encoding", "gzip, aws-chunked").build();

        SdkHttpFullResponse rewritten = AwsChunkedResponseDecodingUtils.rewriteHeaders(response, 11);

        assertThat(rewritten.matchingHeaders("Content-Encoding")).containsExactly("gzip");
    }

    @Test
    void completedTrailers_whenFutureNotDone_throws() {
        assertThatThrownBy(() -> AwsChunkedResponseDecodingUtils.completedTrailers(new CompletableFuture<>()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("only available after");
    }

    @Test
    void completedTrailers_whenFutureFailed_throws() {
        CompletableFuture<Map<String, String>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("boom"));

        assertThatThrownBy(() -> AwsChunkedResponseDecodingUtils.completedTrailers(future))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("only available after");
    }

    @Test
    void completedTrailers_whenFutureCompleted_returnsTrailers() {
        Map<String, String> trailers = Collections.singletonMap("x-amz-checksum-crc32", "abc==");

        assertThat(AwsChunkedResponseDecodingUtils.completedTrailers(CompletableFuture.completedFuture(trailers)))
            .isSameAs(trailers);
    }

    private static SdkHttpFullResponse.Builder response() {
        return SdkHttpFullResponse.builder().statusCode(200);
    }

    private static ExecutionAttributes enabled() {
        ExecutionAttributes attributes = new ExecutionAttributes();
        attributes.putAttribute(SdkInternalExecutionAttribute.AWS_CHUNKED_RESPONSE_DECODING, true);
        return attributes;
    }
}
