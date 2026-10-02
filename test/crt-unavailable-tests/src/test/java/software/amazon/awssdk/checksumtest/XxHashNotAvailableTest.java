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

package software.amazon.awssdk.checksumtest;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static software.amazon.awssdk.services.protocolrestjson.model.ChecksumAlgorithm.XXHASH128;
import static software.amazon.awssdk.services.protocolrestjson.model.ChecksumAlgorithm.XXHASH3;
import static software.amazon.awssdk.services.protocolrestjson.model.ChecksumAlgorithm.XXHASH64;

import io.reactivex.Flowable;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.checksums.ChecksumValidation;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonAsyncClient;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonAsyncClientBuilder;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClient;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClientBuilder;
import software.amazon.awssdk.services.protocolrestjson.model.ChecksumAlgorithm;
import software.amazon.awssdk.services.protocolrestjson.model.ChecksumMode;
import software.amazon.awssdk.services.protocolrestjson.model.OperationWithCustomRequestChecksumRequest;

public class XxHashNotAvailableTest {
    private SdkHttpClient httpClient;
    private SdkAsyncHttpClient httpAsyncClient;
    private ProtocolRestJsonAsyncClient asyncClient;
    private ProtocolRestJsonClient client;

    @BeforeEach
    public void setup() throws IOException {
        httpClient = Mockito.mock(SdkHttpClient.class);
        httpAsyncClient = Mockito.mock(SdkAsyncHttpClient.class);
        client = initializeSync().build();
        asyncClient = initializeAsync().build();
    }

    private void stubResponse(SdkHttpFullResponse.Builder responseBuilder) throws IOException {
        SdkHttpFullResponse successfulHttpResponse = responseBuilder
            .statusCode(200)
            .putHeader("Content-Length", "0")
            .build();

        ExecutableHttpRequest request = Mockito.mock(ExecutableHttpRequest.class);
        Mockito.when(request.call()).thenReturn(HttpExecuteResponse.builder()
                                                                   .response(successfulHttpResponse)
                                                                   .build());
        Mockito.when(httpClient.prepareRequest(any())).thenReturn(request);

        Mockito.when(httpAsyncClient.execute(any())).thenAnswer(invocation -> {
            AsyncExecuteRequest asyncExecuteRequest = invocation.getArgument(0, AsyncExecuteRequest.class);
            asyncExecuteRequest.responseHandler().onHeaders(successfulHttpResponse);
            asyncExecuteRequest.responseHandler().onStream(Flowable.empty());
            return CompletableFuture.completedFuture(null);
        });
    }

    private ProtocolRestJsonAsyncClientBuilder initializeAsync() {
        return ProtocolRestJsonAsyncClient.builder().httpClient(httpAsyncClient)
                                          .credentialsProvider(AnonymousCredentialsProvider.create())
                                          .overrideConfiguration(
                                              o -> o.addExecutionInterceptor(new CaptureChecksumValidationInterceptor()))
                                          .region(Region.US_WEST_2);
    }

    private ProtocolRestJsonClientBuilder initializeSync() {
        return ProtocolRestJsonClient.builder().httpClient(httpClient)
                                     .credentialsProvider(AnonymousCredentialsProvider.create())
                                     .overrideConfiguration(
                                         o -> o.addExecutionInterceptor(new CaptureChecksumValidationInterceptor()))
                                     .region(Region.US_WEST_2);
    }

    @ParameterizedTest
    @MethodSource("xxHashServiceAlgorithms")
    public void asyncChecksumCalculation_xxHashNotAvailable_shouldThrowException(
        ChecksumAlgorithm serviceAlgorithm) throws IOException {
        stubResponse(SdkHttpFullResponse.builder());
        assertThatThrownBy(() -> asyncClient.operationWithCustomRequestChecksum(
            OperationWithCustomRequestChecksumRequest.builder()
                                                     .checksumAlgorithm(serviceAlgorithm)
                                                     .build()).join())
            .hasMessageContaining("Add dependency on 'software.amazon.awssdk.crt:aws-crt' module");
    }

    @ParameterizedTest
    @MethodSource("xxHashServiceAlgorithms")
    public void syncChecksumCalculation_xxHashNotAvailable_shouldThrowException(
        ChecksumAlgorithm serviceAlgorithm) throws IOException {
        stubResponse(SdkHttpFullResponse.builder());
        assertThatThrownBy(() -> client.operationWithCustomRequestChecksum(
            OperationWithCustomRequestChecksumRequest.builder()
                                                     .checksumAlgorithm(serviceAlgorithm)
                                                     .build()))
            .hasMessageContaining("Add dependency on 'software.amazon.awssdk.crt:aws-crt' module");
    }

    @ParameterizedTest
    @MethodSource("xxHashValidationHeaders")
    public void syncChecksumValidation_onlyHasXxHash_shouldSkipValidation(String headerName) throws IOException {
        stubResponse(SdkHttpFullResponse.builder().putHeader(headerName, "foobar"));

        client.getOperationWithChecksum(
            r -> r.checksumMode(ChecksumMode.ENABLED),
            ResponseTransformer.toBytes());

        assertThat(CaptureChecksumValidationInterceptor.checksumValidation).isEqualTo(ChecksumValidation.CHECKSUM_ALGORITHM_NOT_FOUND);
        assertThat(CaptureChecksumValidationInterceptor.expectedAlgorithm).isNull();
    }

    @ParameterizedTest
    @MethodSource("xxHashValidationHeaders")
    public void asyncChecksumValidation_onlyHasXxHash_shouldSkipValidation(String headerName) throws IOException {
        stubResponse(SdkHttpFullResponse.builder().putHeader(headerName, "foobar"));

        asyncClient.getOperationWithChecksum(
            r -> r.checksumMode(ChecksumMode.ENABLED),
            AsyncResponseTransformer.toBytes()).join();

        assertThat(CaptureChecksumValidationInterceptor.checksumValidation).isEqualTo(ChecksumValidation.CHECKSUM_ALGORITHM_NOT_FOUND);
        assertThat(CaptureChecksumValidationInterceptor.expectedAlgorithm).isNull();
    }

    static Stream<ChecksumAlgorithm> xxHashServiceAlgorithms() {
        return Stream.of(XXHASH64, XXHASH3, XXHASH128);
    }

    static Stream<String> xxHashValidationHeaders() {
        return Stream.of("x-amz-checksum-xxhash64", "x-amz-checksum-xxhash3", "x-amz-checksum-xxhash128");
    }

    private static class CaptureChecksumValidationInterceptor implements ExecutionInterceptor {
        private static software.amazon.awssdk.checksums.spi.ChecksumAlgorithm expectedAlgorithm;
        private static ChecksumValidation checksumValidation;

        @Override
        public void afterExecution(Context.AfterExecution context, ExecutionAttributes executionAttributes) {
            expectedAlgorithm =
                executionAttributes.getOptionalAttribute(SdkExecutionAttribute.HTTP_CHECKSUM_VALIDATION_ALGORITHM_V2).orElse(null);
            checksumValidation =
                executionAttributes.getOptionalAttribute(SdkExecutionAttribute.HTTP_RESPONSE_CHECKSUM_VALIDATION).orElse(null);
        }
    }
}
