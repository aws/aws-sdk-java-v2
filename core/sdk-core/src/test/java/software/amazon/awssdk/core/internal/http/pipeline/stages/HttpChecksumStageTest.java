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

package software.amazon.awssdk.core.internal.http.pipeline.stages;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static software.amazon.awssdk.core.HttpChecksumConstant.HEADER_FOR_TRAILER_REFERENCE;
import static software.amazon.awssdk.core.HttpChecksumConstant.SIGNING_METHOD;
import static software.amazon.awssdk.core.internal.signer.SigningMethod.UNSIGNED_PAYLOAD;
import static software.amazon.awssdk.http.Header.CONTENT_LENGTH;
import static software.amazon.awssdk.http.Header.CONTENT_MD5;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.core.ClientType;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.checksums.Algorithm;
import software.amazon.awssdk.core.checksums.ChecksumSpecs;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.http.NoopTestRequest;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.InterceptorContext;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.interceptor.trait.HttpChecksumRequired;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import utils.ValidSdkObjects;

@RunWith(MockitoJUnitRunner.class)
public class HttpChecksumStageTest {
    private static final String CHECKSUM_SPECS_HEADER = "ChecksumHeader";
    private static final RequestBody REQUEST_BODY = RequestBody.fromString("TestBody");
    private static final AsyncRequestBody ASYNC_REQUEST_BODY = AsyncRequestBody.fromString("TestBody");
    private final HttpChecksumStage syncStage = new HttpChecksumStage(ClientType.SYNC);
    private final HttpChecksumStage asyncStage = new HttpChecksumStage(ClientType.ASYNC);

    @Test
    public void sync_md5Required_addsMd5Checksum_doesNotAddFlexibleChecksums() throws Exception {
        SdkHttpFullRequest.Builder requestBuilder = createHttpRequestBuilder();
        boolean isAsyncStreaming = false;
        RequestExecutionContext ctx = md5RequiredRequestContext(isAsyncStreaming);

        syncStage.execute(requestBuilder, ctx);

        assertThat(requestBuilder.headers().get(CONTENT_MD5)).containsExactly("9dzKaiLL99all2ZyHa76RA==");

        assertThat(requestBuilder.firstMatchingHeader(HEADER_FOR_TRAILER_REFERENCE)).isEmpty();
        assertThat(requestBuilder.firstMatchingHeader("Content-encoding")).isEmpty();
        assertThat(requestBuilder.firstMatchingHeader("x-amz-content-sha256")).isEmpty();
        assertThat(requestBuilder.firstMatchingHeader("x-amz-decoded-content-length")).isEmpty();
        assertThat(requestBuilder.firstMatchingHeader(CONTENT_LENGTH)).isEmpty();
        assertThat(requestBuilder.firstMatchingHeader(CHECKSUM_SPECS_HEADER)).isEmpty();
    }

    @Test
    public void async_nonStreaming_md5Required_addsMd5Checksum_doesNotAddFlexibleChecksums() throws Exception {
        SdkHttpFullRequest.Builder requestBuilder = createHttpRequestBuilder();
        boolean isAsyncStreaming = false;
        RequestExecutionContext ctx = md5RequiredRequestContext(isAsyncStreaming);

        asyncStage.execute(requestBuilder, ctx);

        assertThat(requestBuilder.headers().get(CONTENT_MD5)).containsExactly("9dzKaiLL99all2ZyHa76RA==");

        assertThat(requestBuilder.firstMatchingHeader(HEADER_FOR_TRAILER_REFERENCE)).isEmpty();
        assertThat(requestBuilder.firstMatchingHeader("Content-encoding")).isEmpty();
        assertThat(requestBuilder.firstMatchingHeader("x-amz-content-sha256")).isEmpty();
        assertThat(requestBuilder.firstMatchingHeader("x-amz-decoded-content-length")).isEmpty();
        assertThat(requestBuilder.firstMatchingHeader(CONTENT_LENGTH)).isEmpty();
        assertThat(requestBuilder.firstMatchingHeader(CHECKSUM_SPECS_HEADER)).isEmpty();
    }

    @Test
    public void async_streaming_md5Required_throws_IllegalArgumentException() throws Exception {
        SdkHttpFullRequest.Builder requestBuilder = createHttpRequestBuilder();
        boolean isAsyncStreaming = true;
        RequestExecutionContext ctx = md5RequiredRequestContext(isAsyncStreaming);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            asyncStage.execute(requestBuilder, ctx);
        });

        assertThat(exception.getMessage()).isEqualTo("This operation requires a content-MD5 checksum, but one cannot be "
                                                     + "calculated for non-blocking content.");
    }

    @Test
    public void sync_flexibleChecksumInTrailerRequired_addsFlexibleChecksumInTrailer_doesNotAddMd5ChecksumAndFlexibleChecksumInHeader() throws Exception {
        SdkHttpFullRequest.Builder requestBuilder = createHttpRequestBuilder();
        boolean isStreaming = true;
        RequestExecutionContext ctx = syncFlexibleChecksumRequiredRequestContext(isStreaming);

        syncStage.execute(requestBuilder, ctx);

        assertThat(requestBuilder.headers().get(HEADER_FOR_TRAILER_REFERENCE)).containsExactly(CHECKSUM_SPECS_HEADER);
        assertThat(requestBuilder.headers().get("Content-encoding")).containsExactly("aws-chunked");
        assertThat(requestBuilder.headers().get("x-amz-content-sha256")).containsExactly("STREAMING-UNSIGNED-PAYLOAD-TRAILER");
        assertThat(requestBuilder.headers().get("x-amz-decoded-content-length")).containsExactly("8");
        assertThat(requestBuilder.headers().get(CONTENT_LENGTH)).containsExactly("79");

        assertThat(requestBuilder.firstMatchingHeader(CONTENT_MD5)).isEmpty();
        assertThat(requestBuilder.firstMatchingHeader(CHECKSUM_SPECS_HEADER)).isEmpty();
    }

    @Test
    public void async_flexibleChecksumInTrailerRequired_addsFlexibleChecksumInTrailer_doesNotAddMd5ChecksumAndFlexibleChecksumInHeader() throws Exception {
        SdkHttpFullRequest.Builder requestBuilder = createHttpRequestBuilder();
        boolean isStreaming = true;
        RequestExecutionContext ctx = asyncFlexibleChecksumRequiredRequestContext(isStreaming);

        asyncStage.execute(requestBuilder, ctx);

        assertThat(requestBuilder.headers().get(HEADER_FOR_TRAILER_REFERENCE)).containsExactly(CHECKSUM_SPECS_HEADER);
        assertThat(requestBuilder.headers().get("Content-encoding")).containsExactly("aws-chunked");
        assertThat(requestBuilder.headers().get("x-amz-content-sha256")).containsExactly("STREAMING-UNSIGNED-PAYLOAD-TRAILER");
        assertThat(requestBuilder.headers().get("x-amz-decoded-content-length")).containsExactly("8");
        assertThat(requestBuilder.headers().get(CONTENT_LENGTH)).containsExactly("79");

        assertThat(requestBuilder.firstMatchingHeader(CONTENT_MD5)).isEmpty();
        assertThat(requestBuilder.firstMatchingHeader(CHECKSUM_SPECS_HEADER)).isEmpty();
    }

    @Test
    public void sync_flexibleChecksumInHeaderRequired_addsFlexibleChecksumInHeader_doesNotAddMd5ChecksumAndFlexibleChecksumInTrailer() throws Exception {
        SdkHttpFullRequest.Builder requestBuilder = createHttpRequestBuilder();
        boolean isStreaming = false;
        RequestExecutionContext ctx = syncFlexibleChecksumRequiredRequestContext(isStreaming);

        syncStage.execute(requestBuilder, ctx);

        assertThat(requestBuilder.headers().get(CHECKSUM_SPECS_HEADER)).containsExactly("/T5YuTxNWthvWXg+TJMwl60XKcAnLMrrOZe/jA9Y+eI=");

        assertThat(requestBuilder.firstMatchingHeader(HEADER_FOR_TRAILER_REFERENCE)).isEmpty();
        assertThat(requestBuilder.firstMatchingHeader("Content-encoding")).isEmpty();
        assertThat(requestBuilder.firstMatchingHeader("x-amz-content-sha256")).isEmpty();
        assertThat(requestBuilder.firstMatchingHeader("x-amz-decoded-content-length")).isEmpty();
        assertThat(requestBuilder.firstMatchingHeader(CONTENT_LENGTH)).isEmpty();
        assertThat(requestBuilder.firstMatchingHeader(CONTENT_MD5)).isEmpty();
    }

    @Test
    public void async_flexibleChecksumInHeaderRequired_addsFlexibleChecksumInHeader_doesNotAddMd5ChecksumAndFlexibleChecksumInTrailer() throws Exception {
        SdkHttpFullRequest.Builder requestBuilder = createHttpRequestBuilder();
        boolean isStreaming = false;
        RequestExecutionContext ctx = asyncFlexibleChecksumRequiredRequestContext(isStreaming);

        asyncStage.execute(requestBuilder, ctx);

        assertThat(requestBuilder.headers().get(CHECKSUM_SPECS_HEADER)).containsExactly("/T5YuTxNWthvWXg+TJMwl60XKcAnLMrrOZe/jA9Y+eI=");

        assertThat(requestBuilder.firstMatchingHeader(HEADER_FOR_TRAILER_REFERENCE)).isEmpty();
        assertThat(requestBuilder.firstMatchingHeader("Content-encoding")).isEmpty();
        assertThat(requestBuilder.firstMatchingHeader("x-amz-content-sha256")).isEmpty();
        assertThat(requestBuilder.firstMatchingHeader("x-amz-decoded-content-length")).isEmpty();
        assertThat(requestBuilder.firstMatchingHeader(CONTENT_LENGTH)).isEmpty();
        assertThat(requestBuilder.firstMatchingHeader(CONTENT_MD5)).isEmpty();
    }

    private SdkHttpFullRequest.Builder createHttpRequestBuilder() {
        return SdkHttpFullRequest.builder().contentStreamProvider(REQUEST_BODY.contentStreamProvider());
    }

    private RequestExecutionContext md5RequiredRequestContext(boolean isAsyncStreaming) {
        ExecutionAttributes executionAttributes =
            ExecutionAttributes.builder()
                               .put(SdkInternalExecutionAttribute.HTTP_CHECKSUM_REQUIRED, HttpChecksumRequired.create())
                               .build();

        InterceptorContext interceptorContext =
            InterceptorContext.builder()
                              .request(NoopTestRequest.builder().build())
                              .httpRequest(ValidSdkObjects.sdkHttpFullRequest().build())
                              .requestBody(REQUEST_BODY)
                              .build();

        return createRequestExecutionContext(executionAttributes, interceptorContext, isAsyncStreaming);
    }

    private RequestExecutionContext syncFlexibleChecksumRequiredRequestContext(boolean isStreaming) {
        ChecksumSpecs checksumSpecs = ChecksumSpecs.builder()
                                                   .headerName(CHECKSUM_SPECS_HEADER)
                                                   // true = trailer, false = header
                                                   .isRequestStreaming(isStreaming)
                                                   .algorithm(Algorithm.SHA256)
                                                   .build();

        ExecutionAttributes executionAttributes =
            ExecutionAttributes.builder()
                               .put(SdkExecutionAttribute.RESOLVED_CHECKSUM_SPECS, checksumSpecs)
                               .put(SdkExecutionAttribute.CLIENT_TYPE, ClientType.SYNC)
                               .put(SIGNING_METHOD, UNSIGNED_PAYLOAD)
                               .build();

        InterceptorContext interceptorContext =
            InterceptorContext.builder()
                              .request(NoopTestRequest.builder().build())
                              .httpRequest(ValidSdkObjects.sdkHttpFullRequest().build())
                              .requestBody(REQUEST_BODY)
                              .build();

        return createRequestExecutionContext(executionAttributes, interceptorContext, false);
    }

    private RequestExecutionContext asyncFlexibleChecksumRequiredRequestContext(boolean isStreaming) {
        ChecksumSpecs checksumSpecs = ChecksumSpecs.builder()
                                                   .headerName(CHECKSUM_SPECS_HEADER)
                                                   // true = trailer, false = header
                                                   .isRequestStreaming(isStreaming)
                                                   .algorithm(Algorithm.SHA256)
                                                   .build();

        ExecutionAttributes executionAttributes =
            ExecutionAttributes.builder()
                               .put(SdkExecutionAttribute.RESOLVED_CHECKSUM_SPECS, checksumSpecs)
                               .put(SdkExecutionAttribute.CLIENT_TYPE, ClientType.ASYNC)
                               .put(SIGNING_METHOD, UNSIGNED_PAYLOAD)
                               .build();

        InterceptorContext.Builder interceptorContextBuilder =
            InterceptorContext.builder()
                              .request(NoopTestRequest.builder().build())
                              .httpRequest(ValidSdkObjects.sdkHttpFullRequest().build())
                              .requestBody(REQUEST_BODY);

        if (isStreaming) {
            interceptorContextBuilder.asyncRequestBody(ASYNC_REQUEST_BODY);
        }

        return createRequestExecutionContext(executionAttributes, interceptorContextBuilder.build(), isStreaming);
    }

    private RequestExecutionContext createRequestExecutionContext(ExecutionAttributes executionAttributes,
                                                                  InterceptorContext interceptorContext,
                                                                  boolean isAsyncStreaming) {
        ExecutionContext executionContext = ExecutionContext.builder()
                                                            .executionAttributes(executionAttributes)
                                                            .interceptorContext(interceptorContext)
                                                            .build();
        RequestExecutionContext.Builder builder = RequestExecutionContext.builder()
                                                                         .executionContext(executionContext)
                                                                         .originalRequest(NoopTestRequest.builder().build());
        if (isAsyncStreaming) {
            builder.requestProvider(ASYNC_REQUEST_BODY);
        }
        return builder.build();
    }
}
