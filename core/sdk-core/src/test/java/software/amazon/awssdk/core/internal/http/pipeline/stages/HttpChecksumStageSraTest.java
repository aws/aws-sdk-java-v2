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
import static software.amazon.awssdk.core.HttpChecksumConstant.HEADER_FOR_TRAILER_REFERENCE;
import static software.amazon.awssdk.core.HttpChecksumConstant.SIGNING_METHOD;
import static software.amazon.awssdk.core.interceptor.SdkExecutionAttribute.RESOLVED_CHECKSUM_SPECS;
import static software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute.AUTH_SCHEMES;
import static software.amazon.awssdk.core.internal.signer.SigningMethod.UNSIGNED_PAYLOAD;
import static software.amazon.awssdk.http.Header.CONTENT_LENGTH;
import static software.amazon.awssdk.http.Header.CONTENT_MD5;

import java.util.HashMap;
import java.util.stream.Stream;
import org.junit.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.checksums.DefaultChecksumAlgorithm;
import software.amazon.awssdk.core.ClientType;
import software.amazon.awssdk.core.async.AsyncRequestBody;
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
import software.amazon.awssdk.http.auth.aws.internal.signer.util.ChecksumUtil;
import utils.ValidSdkObjects;

public class HttpChecksumStageSraTest {
    private static final String CHECKSUM_SPECS_HEADER = "x-amz-checksum-sha256";
    private static final RequestBody REQUEST_BODY = RequestBody.fromString("TestBody");
    private static final AsyncRequestBody ASYNC_REQUEST_BODY = AsyncRequestBody.fromString("TestBody");

    public static Stream<ClientType> clientTypes() {
        return Stream.of(ClientType.SYNC, ClientType.ASYNC);
    }

    @ParameterizedTest
    @MethodSource("clientTypes")
    public void legacyHttpRequiredTrait_shouldUpdateResolvedChecksumSpec(ClientType clientType) throws Exception {
        SdkHttpFullRequest.Builder requestBuilder = createHttpRequestBuilder();
        RequestExecutionContext ctx = legacyHttpRequiredRequestContext();

        new HttpChecksumStage(clientType).execute(requestBuilder, ctx);
        assertThat(requestBuilder.headers().get(CONTENT_MD5)).isNull();

        ChecksumSpecs checksumSpecs = ctx.executionAttributes().getAttribute(RESOLVED_CHECKSUM_SPECS);
        assertThat(checksumSpecs).isNotNull();
        assertThat(checksumSpecs.algorithmV2()).isEqualTo(DefaultChecksumAlgorithm.CRC32);
    }

    @ParameterizedTest
    @MethodSource("clientTypes")
    public void nonStreamingRequestChecksumRequired_checksumNotProvided_shouldUpdateResolvedChecksumSpec(ClientType clientType) throws Exception {
        SdkHttpFullRequest.Builder requestBuilder = createHttpRequestBuilder();
        RequestExecutionContext ctx =
            flexibleChecksumRequestContext(clientType, ChecksumSpecs.builder().isRequestChecksumRequired(true), false);

        new HttpChecksumStage(clientType).execute(requestBuilder, ctx);

        assertThat(requestBuilder.headers().get(CONTENT_MD5)).isNull();

        ChecksumSpecs checksumSpecs = ctx.executionAttributes().getAttribute(RESOLVED_CHECKSUM_SPECS);
        assertThat(checksumSpecs).isNotNull();
        assertThat(checksumSpecs.algorithmV2()).isEqualTo(DefaultChecksumAlgorithm.CRC32);
    }

    @ParameterizedTest
    @MethodSource("clientTypes")
    public void nonStreamingRequestChecksumRequired_checksumProvided_shouldNotUpdateResolvedChecksumSpec(ClientType clientType) throws Exception {

        SdkHttpFullRequest.Builder requestBuilder = createHttpRequestBuilder().putHeader("x-amz-checksum-sha256", "1234");
        RequestExecutionContext ctx = flexibleChecksumRequestContext(clientType,
                                                                     ChecksumSpecs.builder().isRequestChecksumRequired(true),
                                                                     false);

        ChecksumSpecs expectedChecksumSpecs = ctx.executionAttributes().getAttribute(RESOLVED_CHECKSUM_SPECS);

        new HttpChecksumStage(clientType).execute(requestBuilder, ctx);

        assertThat(requestBuilder.headers().get(CONTENT_MD5)).isNull();

        ChecksumSpecs checksumSpecs = ctx.executionAttributes().getAttribute(RESOLVED_CHECKSUM_SPECS);
        assertThat(checksumSpecs).isEqualTo(expectedChecksumSpecs);
    }

    @Test
    public void sync_flexibleChecksumInTrailer_shouldUpdateResolvedChecksumSpec() throws Exception {
        SdkHttpFullRequest.Builder requestBuilder = createHttpRequestBuilder();
        RequestExecutionContext ctx = flexibleChecksumRequestContext(ClientType.SYNC,
                                                                     ChecksumSpecs.builder().algorithmV2(DefaultChecksumAlgorithm.SHA1)
                                                                                  .headerName(ChecksumUtil.checksumHeaderName(DefaultChecksumAlgorithm.SHA1)),
                                                                     false);

        new HttpChecksumStage(ClientType.SYNC).execute(requestBuilder, ctx);

        assertThat(requestBuilder.headers().get(CONTENT_MD5)).isNull();

        ChecksumSpecs checksumSpecs = ctx.executionAttributes().getAttribute(RESOLVED_CHECKSUM_SPECS);
        assertThat(checksumSpecs).isNotNull();
        assertThat(checksumSpecs.algorithmV2()).isEqualTo(DefaultChecksumAlgorithm.SHA1);
    }

    @Test
    public void async_flexibleChecksumInTrailer_addsFlexibleChecksumInTrailer() throws Exception {
        SdkHttpFullRequest.Builder requestBuilder = createHttpRequestBuilder();
        boolean isStreaming = true;
        RequestExecutionContext ctx = flexibleChecksumRequestContext(ClientType.ASYNC,
                                                                     ChecksumSpecs.builder()
                                                                                  .algorithmV2(DefaultChecksumAlgorithm.SHA256)
                                                                                  .headerName(ChecksumUtil.checksumHeaderName(DefaultChecksumAlgorithm.SHA1)),
                                                                     isStreaming);

        new HttpChecksumStage(ClientType.ASYNC).execute(requestBuilder, ctx);

        assertThat(requestBuilder.headers().get(HEADER_FOR_TRAILER_REFERENCE)).containsExactly(CHECKSUM_SPECS_HEADER);
        assertThat(requestBuilder.headers().get("Content-encoding")).containsExactly("aws-chunked");
        assertThat(requestBuilder.headers().get("x-amz-content-sha256")).containsExactly("STREAMING-UNSIGNED-PAYLOAD-TRAILER");
        assertThat(requestBuilder.headers().get("x-amz-decoded-content-length")).containsExactly("8");
        assertThat(requestBuilder.headers().get(CONTENT_LENGTH)).containsExactly("86");

        assertThat(requestBuilder.firstMatchingHeader(CONTENT_MD5)).isEmpty();
        assertThat(requestBuilder.firstMatchingHeader(CHECKSUM_SPECS_HEADER)).isEmpty();
    }

    private SdkHttpFullRequest.Builder createHttpRequestBuilder() {
        return SdkHttpFullRequest.builder().contentStreamProvider(REQUEST_BODY.contentStreamProvider());
    }

    private RequestExecutionContext legacyHttpRequiredRequestContext() {
        ExecutionAttributes executionAttributes =
            ExecutionAttributes.builder()
                               .put(SdkInternalExecutionAttribute.HTTP_CHECKSUM_REQUIRED, HttpChecksumRequired.create())
                               .put(SIGNING_METHOD, UNSIGNED_PAYLOAD)
                               .put(AUTH_SCHEMES, new HashMap<>())
                               .build();

        InterceptorContext interceptorContext =
            InterceptorContext.builder()
                              .request(NoopTestRequest.builder().build())
                              .httpRequest(ValidSdkObjects.sdkHttpFullRequest().build())
                              .requestBody(REQUEST_BODY)
                              .build();

        return createRequestExecutionContext(executionAttributes, interceptorContext, false);
    }

    private RequestExecutionContext flexibleChecksumRequestContext(ClientType clientType,
                                                                   ChecksumSpecs.Builder checksumSpecsBuilder,
                                                                   boolean isStreaming) {
        ChecksumSpecs checksumSpecs = checksumSpecsBuilder
            .isRequestStreaming(isStreaming)
            .build();

        ExecutionAttributes executionAttributes =
            ExecutionAttributes.builder()
                               .put(SdkExecutionAttribute.RESOLVED_CHECKSUM_SPECS, checksumSpecs)
                               .put(SdkExecutionAttribute.CLIENT_TYPE, clientType)
                               .put(SIGNING_METHOD, UNSIGNED_PAYLOAD)
                               .put(AUTH_SCHEMES, new HashMap<>())
                               .build();

        InterceptorContext.Builder interceptorContextBuilder =
            InterceptorContext.builder()
                              .request(NoopTestRequest.builder().build())
                              .httpRequest(ValidSdkObjects.sdkHttpFullRequest().build())
                              .requestBody(REQUEST_BODY);


        if (isStreaming) {
            switch (clientType) {
                case ASYNC:
                    interceptorContextBuilder.asyncRequestBody(ASYNC_REQUEST_BODY);
                    break;
                case SYNC:
                    interceptorContextBuilder.requestBody(REQUEST_BODY);
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported client type" + clientType.toString());
            }

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
