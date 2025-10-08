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
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;

public class StreamingRequestInterceptorTest {
    private StreamingRequestInterceptor interceptor = new StreamingRequestInterceptor();

    @Test
    public void modifyHttpRequest_setsExpect100Continue_whenSdkRequestIsPutObject() {

        final SdkHttpRequest modifiedRequest = interceptor.modifyHttpRequest(modifyHttpRequestContext(PutObjectRequest.builder().build()),
                                                                             new ExecutionAttributes());

        assertThat(modifiedRequest.firstMatchingHeader("Expect")).hasValue("100-continue");
    }

    @Test
    public void modifyHttpRequest_setsExpect100Continue_whenSdkRequestIsUploadPart() {

        final SdkHttpRequest modifiedRequest =
            interceptor.modifyHttpRequest(modifyHttpRequestContext(UploadPartRequest.builder().build()),
                                                                             new ExecutionAttributes());

        assertThat(modifiedRequest.firstMatchingHeader("Expect")).hasValue("100-continue");
    }

    @Test
    public void modifyHttpRequest_doesNotSetExpect_whenSdkRequestIsNotPutObject() {

        SdkHttpRequest modifiedRequest = interceptor.modifyHttpRequest(modifyHttpRequestContext(GetObjectRequest.builder().build()),
                                                                             new ExecutionAttributes());

        assertThat(modifiedRequest.firstMatchingHeader("Expect")).isNotPresent();
    }

    @ParameterizedTest(name = "{0} with {1}={2} should not set Expect header")
    @MethodSource("zeroContentLengthProvider")
    void modifyHttpRequest_doesNotSetExpect_whenContentLengthIsZero(
        String requestType, String headerName, String headerValue, SdkRequest sdkRequest) {

        SdkHttpRequest httpRequest = buildHttpRequest(headerName, headerValue);

        SdkHttpRequest modifiedRequest = interceptor.modifyHttpRequest(
            modifyHttpRequestContextWithHttpRequest(sdkRequest, httpRequest),
            new ExecutionAttributes());

        assertThat(modifiedRequest.firstMatchingHeader("Expect"))
            .as("Expect header should not be present for zero-length content per RFC 9110")
            .isNotPresent();
    }

    @ParameterizedTest(name = "{0} with {1}={2} should set Expect header")
    @MethodSource("nonZeroContentLengthProvider")
    void modifyHttpRequest_setsExpect_whenContentLengthIsNonZero(
        String requestType, String headerName, String headerValue, SdkRequest sdkRequest) {

        SdkHttpRequest httpRequest = buildHttpRequest(headerName, headerValue);

        SdkHttpRequest modifiedRequest = interceptor.modifyHttpRequest(
            modifyHttpRequestContextWithHttpRequest(sdkRequest, httpRequest),
            new ExecutionAttributes());

        assertThat(modifiedRequest.firstMatchingHeader("Expect")).hasValue("100-continue");
    }

    @Test
    void modifyHttpRequest_prioritizesDecodedContentLength_overContentLength() {
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

    // Helper method to build HTTP request with specific header
    private SdkHttpRequest buildHttpRequest(String headerName, String headerValue) {
        return SdkHttpFullRequest.builder()
                                 .uri(URI.create("http://localhost:8080"))
                                 .method(SdkHttpMethod.PUT)
                                 .putHeader(headerName, headerValue)
                                 .build();
    }

    // Test data providers
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
}
