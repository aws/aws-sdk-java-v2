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

package software.amazon.awssdk.services.s3.internal.handlers;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.services.s3.utils.InterceptorTestUtils.modifyHttpRequestContext;
import static software.amazon.awssdk.services.s3.utils.InterceptorTestUtils.modifyHttpRequestContextWithHttpRequest;

import java.net.URI;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;

class StreamingRequestInterceptorTest {
    private final StreamingRequestInterceptor interceptor = new StreamingRequestInterceptor();

    // -----------------------------------------------------------------------
    // Basic behavior: request type filtering
    // -----------------------------------------------------------------------

    @Test
    void modifyHttpRequest_whenPutObjectRequest_shouldAddExpectHeader() {
        SdkHttpRequest modifiedRequest = interceptor.modifyHttpRequest(
            modifyHttpRequestContext(PutObjectRequest.builder().build()), new ExecutionAttributes());

        assertThat(modifiedRequest.firstMatchingHeader("Expect")).hasValue("100-continue");
    }

    @Test
    void modifyHttpRequest_whenUploadPartRequest_shouldAddExpectHeader() {
        SdkHttpRequest modifiedRequest = interceptor.modifyHttpRequest(
            modifyHttpRequestContext(UploadPartRequest.builder().build()), new ExecutionAttributes());

        assertThat(modifiedRequest.firstMatchingHeader("Expect")).hasValue("100-continue");
    }

    @Test
    void modifyHttpRequest_whenGetObjectRequest_shouldNotAddExpectHeader() {
        SdkHttpRequest modifiedRequest = interceptor.modifyHttpRequest(
            modifyHttpRequestContext(GetObjectRequest.builder().build()), new ExecutionAttributes());

        assertThat(modifiedRequest.firstMatchingHeader("Expect")).isNotPresent();
    }

    // -----------------------------------------------------------------------
    // Content-Length handling
    // -----------------------------------------------------------------------

    @ParameterizedTest(name = "{0} with {1}={2} should not set Expect header")
    @MethodSource("zeroContentLengthProvider")
    void modifyHttpRequest_whenContentLengthIsZero_shouldNotAddExpectHeader(
            String requestType, String headerName, String headerValue, SdkRequest sdkRequest) {
        SdkHttpRequest httpRequest = buildHttpRequest(headerName, headerValue);

        SdkHttpRequest modifiedRequest = interceptor.modifyHttpRequest(
            modifyHttpRequestContextWithHttpRequest(sdkRequest, httpRequest), new ExecutionAttributes());

        assertThat(modifiedRequest.firstMatchingHeader("Expect"))
            .as("Expect header should not be present for zero-length content per RFC 9110")
            .isNotPresent();
    }

    @ParameterizedTest(name = "{0} with {1}={2} should set Expect header")
    @MethodSource("nonZeroContentLengthProvider")
    void modifyHttpRequest_whenContentLengthIsNonZero_shouldAddExpectHeader(
            String requestType, String headerName, String headerValue, SdkRequest sdkRequest) {
        SdkHttpRequest httpRequest = buildHttpRequest(headerName, headerValue);

        SdkHttpRequest modifiedRequest = interceptor.modifyHttpRequest(
            modifyHttpRequestContextWithHttpRequest(sdkRequest, httpRequest), new ExecutionAttributes());

        assertThat(modifiedRequest.firstMatchingHeader("Expect")).hasValue("100-continue");
    }

    @Test
    void modifyHttpRequest_whenBothContentLengthHeaders_shouldPrioritizeDecodedContentLength() {
        SdkHttpRequest httpRequest = SdkHttpFullRequest.builder()
                                                       .uri(URI.create("http://localhost:8080"))
                                                       .method(SdkHttpMethod.PUT)
                                                       .putHeader("x-amz-decoded-content-length", "0")
                                                       .putHeader("Content-Length", "1024")
                                                       .build();

        SdkHttpRequest modifiedRequest = interceptor.modifyHttpRequest(
            modifyHttpRequestContextWithHttpRequest(PutObjectRequest.builder().build(), httpRequest),
            new ExecutionAttributes());

        assertThat(modifiedRequest.firstMatchingHeader("Expect"))
            .as("x-amz-decoded-content-length should take priority over Content-Length")
            .isNotPresent();
    }

    // -----------------------------------------------------------------------
    // S3Configuration.expectContinueEnabled
    // -----------------------------------------------------------------------

    @ParameterizedTest(name = "{0}")
    @MethodSource("expectContinueConfigProvider")
    void modifyHttpRequest_whenExpectContinueConfigured_shouldMatchExpectedBehavior(
            String testName, SdkRequest sdkRequest, ExecutionAttributes attrs,
            SdkHttpRequest httpRequest, boolean expectPresent) {
        Context.ModifyHttpRequest context = httpRequest != null
            ? modifyHttpRequestContextWithHttpRequest(sdkRequest, httpRequest)
            : modifyHttpRequestContext(sdkRequest);

        SdkHttpRequest modifiedRequest = interceptor.modifyHttpRequest(context, attrs);

        if (expectPresent) {
            assertThat(modifiedRequest.firstMatchingHeader("Expect")).hasValue("100-continue");
        } else {
            assertThat(modifiedRequest.firstMatchingHeader("Expect")).isNotPresent();
        }
    }

    // -----------------------------------------------------------------------
    // Data providers
    // -----------------------------------------------------------------------

    private static Stream<Arguments> expectContinueConfigProvider() {
        ExecutionAttributes disabledAttrs = withExpectContinue(false);
        ExecutionAttributes enabledAttrs = withExpectContinue(true);
        ExecutionAttributes defaultConfigAttrs = new ExecutionAttributes();
        defaultConfigAttrs.putAttribute(SdkExecutionAttribute.SERVICE_CONFIG, S3Configuration.builder().build());
        ExecutionAttributes noConfigAttrs = new ExecutionAttributes();
        SdkHttpRequest nonZeroContentLength = buildHttpRequest("Content-Length", "1024");
        SdkHttpRequest zeroContentLength = buildHttpRequest("Content-Length", "0");

        return Stream.of(
            Arguments.of("PutObject - enabled=false",
                         PutObjectRequest.builder().build(), disabledAttrs, null, false),
            Arguments.of("UploadPart - enabled=false",
                         UploadPartRequest.builder().build(), disabledAttrs, null, false),
            Arguments.of("PutObject - enabled=true",
                         PutObjectRequest.builder().build(), enabledAttrs, null, true),
            Arguments.of("PutObject - default S3Config (enabled=true)",
                         PutObjectRequest.builder().build(), defaultConfigAttrs, null, true),
            Arguments.of("PutObject - no service config",
                         PutObjectRequest.builder().build(), noConfigAttrs, null, true),
            Arguments.of("GetObject - enabled=true (non-streaming, never adds header)",
                         GetObjectRequest.builder().build(), enabledAttrs, null, false),
            Arguments.of("PutObject - enabled=false with non-zero Content-Length",
                         PutObjectRequest.builder().build(), disabledAttrs, nonZeroContentLength, false),
            Arguments.of("PutObject - enabled=true with zero Content-Length",
                         PutObjectRequest.builder().build(), enabledAttrs, zeroContentLength, false)
        );
    }

    private static Stream<Arguments> zeroContentLengthProvider() {
        return Stream.of(
            Arguments.of("PutObject", "Content-Length", "0",
                         PutObjectRequest.builder().build()),
            Arguments.of("PutObject", "x-amz-decoded-content-length", "0",
                         PutObjectRequest.builder().build()),
            Arguments.of("UploadPart", "Content-Length", "0",
                         UploadPartRequest.builder().build()),
            Arguments.of("UploadPart", "x-amz-decoded-content-length", "0",
                         UploadPartRequest.builder().build())
        );
    }

    private static Stream<Arguments> nonZeroContentLengthProvider() {
        return Stream.of(
            Arguments.of("PutObject", "Content-Length", "1024",
                         PutObjectRequest.builder().build()),
            Arguments.of("PutObject", "x-amz-decoded-content-length", "1024",
                         PutObjectRequest.builder().build()),
            Arguments.of("UploadPart", "Content-Length", "1024",
                         UploadPartRequest.builder().build()),
            Arguments.of("UploadPart", "x-amz-decoded-content-length", "1024",
                         UploadPartRequest.builder().build())
        );
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static ExecutionAttributes withExpectContinue(boolean enabled) {
        ExecutionAttributes attrs = new ExecutionAttributes();
        attrs.putAttribute(SdkExecutionAttribute.SERVICE_CONFIG,
                           S3Configuration.builder()
                                          .expectContinueEnabled(enabled)
                                          .build());
        return attrs;
    }

    private static SdkHttpRequest buildHttpRequest(String headerName, String headerValue) {
        return SdkHttpFullRequest.builder()
                                 .uri(URI.create("http://localhost:8080"))
                                 .method(SdkHttpMethod.PUT)
                                 .putHeader(headerName, headerValue)
                                 .build();
    }
}
