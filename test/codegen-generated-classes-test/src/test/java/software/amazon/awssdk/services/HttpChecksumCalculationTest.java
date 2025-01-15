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

package software.amazon.awssdk.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static software.amazon.awssdk.auth.signer.S3SignerExecutionAttribute.ENABLE_CHUNKED_ENCODING;
import static software.amazon.awssdk.core.HttpChecksumConstant.HTTP_CHECKSUM_HEADER_PREFIX;

import io.reactivex.Flowable;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.core.HttpChecksumConstant;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonAsyncClient;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonAsyncClientBuilder;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClient;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClientBuilder;
import software.amazon.awssdk.services.protocolrestjson.model.ChecksumAlgorithm;
import software.amazon.awssdk.services.protocolrestjson.model.OperationWithCustomRequestChecksumRequest;
import software.amazon.awssdk.services.protocolrestjson.model.PutOperationWithChecksumRequest;

/**
 * Verify HTTP checksum calculation behavior with different requestChecksumCalculation settings.
 */
public class HttpChecksumCalculationTest {
    private SdkHttpClient httpClient;
    private SdkAsyncHttpClient httpAsyncClient;

    private ProtocolRestJsonClientBuilder initializeSync(RequestChecksumCalculation calculation) {
        return ProtocolRestJsonClient.builder().httpClient(httpClient)
                                     .credentialsProvider(AnonymousCredentialsProvider.create())
                                     .requestChecksumCalculation(calculation)
                                     .overrideConfiguration(
                                         //     // TODO(sra-identity-and-auth): we should remove these
                                         //     //  overrides once we set up codegen to set chunk-encoding to true
                                         //     //  for requests that are streaming and checksum-enabled
                                         o -> o.addExecutionInterceptor(new EnableChunkedEncodingInterceptor()))
                                     .region(Region.US_WEST_2);
    }

    private ProtocolRestJsonAsyncClientBuilder initializeAsync(RequestChecksumCalculation calculation) {
        return ProtocolRestJsonAsyncClient.builder().httpClient(httpAsyncClient)
                                          .credentialsProvider(AnonymousCredentialsProvider.create())
                                          .requestChecksumCalculation(calculation)
                                          .overrideConfiguration(
                                              // TODO(sra-identity-and-auth): we should remove these
                                              //  overrides once we set up codegen to set chunk-encoding to true
                                              //  for requests that are streaming and checksum-enabled
                                              o -> o.addExecutionInterceptor(new EnableChunkedEncodingInterceptor()))
                                          .region(Region.US_WEST_2);
    }

    private static final class EnableChunkedEncodingInterceptor implements ExecutionInterceptor {
        public void beforeExecution(Context.BeforeExecution context, ExecutionAttributes executionAttributes) {
            SdkRequest request = context.request();

            if (request instanceof PutOperationWithChecksumRequest) {
                executionAttributes.putAttributeIfAbsent(ENABLE_CHUNKED_ENCODING, true);
            }
        }
    }

    @BeforeEach
    public void setup() throws IOException {
        httpClient = Mockito.mock(SdkHttpClient.class);
        httpAsyncClient = Mockito.mock(SdkAsyncHttpClient.class);

        SdkHttpFullResponse successfulHttpResponse = SdkHttpResponse.builder()
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

    public static Stream<Arguments> streamingInputChecksumCalculationParams() {
        return Stream.of(Arguments.of(RequestChecksumCalculation.WHEN_SUPPORTED, null, "x-amz-checksum-crc32",
                                      "requestChecksumWhenSupported_checksumAlgorithmNotProvided_shouldAddCrc32ChecksumTrailerByDefault"),

                         Arguments.of(RequestChecksumCalculation.WHEN_SUPPORTED, ChecksumAlgorithm.SHA1,
                                      "x-amz-checksum-sha1",
                                      "requestChecksumWhenSupported_checksumAlgorithmProvided_shouldHonor"),

                         Arguments.of(RequestChecksumCalculation.WHEN_REQUIRED, null, null,
                                      "requestChecksumWhenRequired_checksumAlgorithmNotProvided_shouldNotAddChecksum"),

                         Arguments.of(RequestChecksumCalculation.WHEN_REQUIRED, ChecksumAlgorithm.CRC32_C,
                                      "x-amz-checksum-crc32c",
                                      "requestChecksumWhenRequired_checksumAlgorithmProvided_shouldAddChecksumTrailer"));
    }

    public static Stream<Arguments> checksumInHeaderRequiredParams() {
        return Stream.of(Arguments.of(RequestChecksumCalculation.WHEN_SUPPORTED, null, "x-amz-checksum-crc32", "o6a/Qw==",
                                      "requestChecksumWhenSupported_checksumAlgorithmNotProvided_shouldAddCrc32ChecksumTrailerByDefault"),

                         Arguments.of(RequestChecksumCalculation.WHEN_SUPPORTED, ChecksumAlgorithm.SHA1,
                                      "x-amz-checksum-sha1", "vyGp6PvFo4RvsFtPoIWeCReyIC8=",
                                      "requestChecksumWhenSupported_checksumAlgorithmProvided_shouldHonor"),

                         Arguments.of(RequestChecksumCalculation.WHEN_REQUIRED, null, "x-amz-checksum-crc32", "o6a/Qw==",
                                      "requestChecksumWhenRequired_checksumAlgorithmNotProvided_shouldAddChecksum"),

                         Arguments.of(RequestChecksumCalculation.WHEN_REQUIRED, ChecksumAlgorithm.CRC32_C,
                                      "x-amz-checksum-crc32c", "KXvQqg==",
                                      "requestChecksumWhenRequired_checksumAlgorithmProvided_shouldAddChecksum"));
    }

    @ParameterizedTest(name = "{index} {3}")
    @MethodSource("streamingInputChecksumCalculationParams")
    public void syncStreamingInput_checksumCalculation(RequestChecksumCalculation requestChecksumCalculation,
                                                       ChecksumAlgorithm checksumAlgorithm,
                                                       String expectedTrailer,
                                                       String description) {

        try (ProtocolRestJsonClient client = initializeSync(requestChecksumCalculation).build()) {
            client.putOperationWithChecksum(PutOperationWithChecksumRequest.builder()
                                                                           .checksumAlgorithm(checksumAlgorithm)
                                                                           .build(),
                                            RequestBody.fromString("Hello world"));

            SdkHttpRequest request = getSyncRequest();
            validateChecksumTrailerHeader(expectedTrailer, request);
        }
    }

    @ParameterizedTest(name = "{index} {3}")
    @MethodSource("streamingInputChecksumCalculationParams")
    public void asyncStreamingInput_checksumCalculation(RequestChecksumCalculation requestChecksumCalculation,
                                                        ChecksumAlgorithm checksumAlgorithm,
                                                        String expectedTrailer,
                                                        String description) {

        try (ProtocolRestJsonAsyncClient client = initializeAsync(requestChecksumCalculation).build()) {
            client.putOperationWithChecksum(PutOperationWithChecksumRequest.builder()
                                                                           .checksumAlgorithm(checksumAlgorithm)
                                                                           .build(),
                                            AsyncRequestBody.fromString("Hello world"));

            SdkHttpRequest request = getAsyncRequest();
            validateChecksumTrailerHeader(expectedTrailer, request);
        }
    }

    @ParameterizedTest(name = "{index} {4}")
    @MethodSource("checksumInHeaderRequiredParams")
    public void syncChecksumInHeaderRequired_checksumCalculation(RequestChecksumCalculation requestChecksumCalculation,
                                                                 ChecksumAlgorithm checksumAlgorithm,
                                                                 String expectedChecksumHeader,
                                                                 String expectedChecksumValue,
                                                                 String description) {

        try (ProtocolRestJsonClient client = initializeSync(requestChecksumCalculation).build()) {
            client.operationWithCustomRequestChecksum(OperationWithCustomRequestChecksumRequest.builder()
                                                                                               .checksumAlgorithm(checksumAlgorithm)
                                                                                               .build());
            SdkHttpRequest request = getSyncRequest();
            validateChecksumHeader(expectedChecksumHeader, expectedChecksumValue, request);
        }
    }

    @ParameterizedTest(name = "{index} {4}")
    @MethodSource("checksumInHeaderRequiredParams")
    public void asyncChecksumInHeaderRequired_checksumCalculation(RequestChecksumCalculation requestChecksumCalculation,
                                                                  ChecksumAlgorithm checksumAlgorithm,
                                                                  String expectedChecksumHeader,
                                                                  String expectedChecksumValue,
                                                                  String description) {

        try (ProtocolRestJsonAsyncClient client = initializeAsync(requestChecksumCalculation).build()) {
            client.operationWithCustomRequestChecksum(OperationWithCustomRequestChecksumRequest.builder()
                                                                                               .checksumAlgorithm(checksumAlgorithm)
                                                                                               .build()).join();
            SdkHttpRequest request = getAsyncRequest();
            validateChecksumHeader(expectedChecksumHeader, expectedChecksumValue, request);
        }
    }

    private static void validateChecksumHeader(String expectedChecksumHeader,
                                               String expectedChecksumValue,
                                               SdkHttpRequest request) {
        assertThat(request.firstMatchingHeader(HttpChecksumConstant.X_AMZ_TRAILER)).isEmpty();
        List<String> checksumHeaders = request.headers()
                                              .keySet()
                                              .stream()
                                              .filter(header -> header.contains(HTTP_CHECKSUM_HEADER_PREFIX) && !header.equals(
                                                  "x-amz-checksum-algorithm"))
                                              .collect(Collectors.toList());

        if (expectedChecksumHeader != null) {
            assertThat(checksumHeaders).containsExactly(expectedChecksumHeader);
            assertThat(request.firstMatchingHeader(expectedChecksumHeader)).contains(expectedChecksumValue);
            assertThat(request.firstMatchingHeader("x-amz-sdk-checksum-algorithm")).isNotEmpty();
        } else {
            assertThat(checksumHeaders).isEmpty();
            assertThat(request.firstMatchingHeader("x-amz-sdk-checksum-algorithm")).isEmpty();
        }
    }

    private static void validateChecksumTrailerHeader(String expectedTrailer, SdkHttpRequest request) {
        if (expectedTrailer != null) {
            assertThat(request.firstMatchingHeader(HttpChecksumConstant.X_AMZ_TRAILER)).contains(expectedTrailer);
            assertThat(request.firstMatchingHeader("x-amz-sdk-checksum-algorithm")).isNotEmpty();
            assertThat(request.firstMatchingHeader("x-amz-content-sha256")).contains("STREAMING-UNSIGNED-PAYLOAD-TRAILER");
        } else {
            assertThat(request.firstMatchingHeader(HttpChecksumConstant.X_AMZ_TRAILER)).isEmpty();
            assertThat(request.firstMatchingHeader("x-amz-sdk-checksum-algorithm")).isEmpty();
        }
    }

    private SdkHttpRequest getSyncRequest() {
        ArgumentCaptor<HttpExecuteRequest> captor = ArgumentCaptor.forClass(HttpExecuteRequest.class);
        Mockito.verify(httpClient).prepareRequest(captor.capture());
        return captor.getValue().httpRequest();
    }

    private SdkHttpRequest getAsyncRequest() {
        ArgumentCaptor<AsyncExecuteRequest> captor = ArgumentCaptor.forClass(AsyncExecuteRequest.class);
        Mockito.verify(httpAsyncClient).execute(captor.capture());
        return captor.getValue().request();
    }
}