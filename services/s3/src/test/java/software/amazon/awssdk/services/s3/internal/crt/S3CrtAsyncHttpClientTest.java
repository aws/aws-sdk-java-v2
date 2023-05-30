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

package software.amazon.awssdk.services.s3.internal.crt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.http.Header.CONTENT_LENGTH;
import static software.amazon.awssdk.services.s3.internal.crt.S3InternalSdkHttpExecutionAttribute.HTTP_CHECKSUM;
import static software.amazon.awssdk.services.s3.internal.crt.S3InternalSdkHttpExecutionAttribute.OPERATION_NAME;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import org.assertj.core.internal.Integers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import software.amazon.awssdk.core.interceptor.trait.HttpChecksum;
import software.amazon.awssdk.crt.http.HttpRequest;
import software.amazon.awssdk.crt.s3.ChecksumAlgorithm;
import software.amazon.awssdk.crt.s3.S3Client;
import software.amazon.awssdk.crt.s3.S3MetaRequest;
import software.amazon.awssdk.crt.s3.S3MetaRequestOptions;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;
import software.amazon.awssdk.http.async.SdkHttpContentPublisher;
import software.amazon.awssdk.services.s3.S3AsyncClient;

public class S3CrtAsyncHttpClientTest {
    private static final URI DEFAULT_ENDPOINT = URI.create("https://127.0.0.1:443");

    private S3CrtAsyncHttpClient asyncHttpClient;
    private S3NativeClientConfiguration s3NativeClientConfiguration;

    private S3Client s3Client;

    private SdkAsyncHttpResponseHandler responseHandler;

    private SdkHttpContentPublisher contentPublisher;

    @BeforeEach
    public void methodSetup() {
        s3Client = Mockito.mock(S3Client.class);
        responseHandler = Mockito.mock(SdkAsyncHttpResponseHandler.class);
        contentPublisher = Mockito.mock(SdkHttpContentPublisher.class);
        s3NativeClientConfiguration = S3NativeClientConfiguration.builder()
                                                                 .endpointOverride(DEFAULT_ENDPOINT)
                                                                 .credentialsProvider(null)
                                                                 .checksumValidationEnabled(true)
                                                                 .build();

        asyncHttpClient = new S3CrtAsyncHttpClient(s3Client, s3NativeClientConfiguration);
    }

    private static Stream<Integer> ports() {
        return Stream.of(null, 1234, 443);
    }

    @ParameterizedTest
    @MethodSource("ports")
    public void defaultRequest_shouldSetMetaRequestOptionsCorrectly(Integer port) {
        AsyncExecuteRequest asyncExecuteRequest = getExecuteRequestBuilder(port).build();

        S3MetaRequestOptions actual = makeRequest(asyncExecuteRequest);
        assertThat(actual.getMetaRequestType()).isEqualTo(S3MetaRequestOptions.MetaRequestType.DEFAULT);
        assertThat(actual.getCredentialsProvider()).isNull();
        String expectedEndpoint = port == null || port.equals(443) ?
                                  DEFAULT_ENDPOINT.getScheme() + "://" + DEFAULT_ENDPOINT.getHost() :
                                  DEFAULT_ENDPOINT.getScheme() + "://" + DEFAULT_ENDPOINT.getHost() + ":" + port;
        assertThat(actual.getEndpoint()).hasToString(expectedEndpoint);


        HttpRequest httpRequest = actual.getHttpRequest();
        assertThat(httpRequest.getEncodedPath()).isEqualTo("/key");

        Map<String, String> headers = httpRequest.getHeaders()
                                                 .stream()
                                                 .collect(HashMap::new, (m, h) -> m.put(h.getName(), h.getValue())
                                                     , Map::putAll);

        String expectedPort = port == null || port.equals(443)  ? "" : ":" + port;
        assertThat(headers).hasSize(4)
                           .containsEntry("Host", DEFAULT_ENDPOINT.getHost() + expectedPort)
                           .containsEntry("custom-header", "foobar")
                           .containsEntry("amz-sdk-invocation-id", "1234")
                           .containsEntry("Content-Length", "100");
    }

    @Test
    public void getObject_shouldSetMetaRequestTypeCorrectly() {
        AsyncExecuteRequest asyncExecuteRequest = getExecuteRequestBuilder().putHttpExecutionAttribute(OPERATION_NAME,
                                                                                                          "GetObject").build();

        S3MetaRequestOptions actual = makeRequest(asyncExecuteRequest);
        assertThat(actual.getMetaRequestType()).isEqualTo(S3MetaRequestOptions.MetaRequestType.GET_OBJECT);
    }

    @Test
    public void putObject_shouldSetMetaRequestTypeCorrectly() {
        AsyncExecuteRequest asyncExecuteRequest = getExecuteRequestBuilder().putHttpExecutionAttribute(OPERATION_NAME,
                                                                                                          "PutObject").build();

        S3MetaRequestOptions actual = makeRequest(asyncExecuteRequest);
        assertThat(actual.getMetaRequestType()).isEqualTo(S3MetaRequestOptions.MetaRequestType.PUT_OBJECT);
    }

    @Test
    public void cancelRequest_shouldForwardCancellation() {
        AsyncExecuteRequest asyncExecuteRequest = getExecuteRequestBuilder().build();
        S3MetaRequest metaRequest = Mockito.mock(S3MetaRequest.class);
        when(s3Client.makeMetaRequest(any(S3MetaRequestOptions.class))).thenReturn(metaRequest);

        CompletableFuture<Void> future = asyncHttpClient.execute(asyncExecuteRequest);

        future.cancel(false);

        verify(metaRequest).cancel();
    }

    @Test
    public void nonStreamingOperation_noChecksumAlgoProvided_shouldNotSetByDefault() {
        HttpChecksum httpChecksum = HttpChecksum.builder()
                                                .isRequestStreaming(false)
                                                .build();

        AsyncExecuteRequest asyncExecuteRequest = getExecuteRequestBuilder().putHttpExecutionAttribute(OPERATION_NAME,
                                                                                                       "NonStreaming")

                                                                            .putHttpExecutionAttribute(HTTP_CHECKSUM,
                                                                                                       httpChecksum)
                                                                            .build();

        S3MetaRequestOptions actual = makeRequest(asyncExecuteRequest);
        assertThat(actual.getChecksumConfig().getChecksumAlgorithm()).isEqualTo(ChecksumAlgorithm.NONE);
    }

    @Test
    public void nonStreamingOperation_checksumAlgoProvided_shouldNotPassToCrt() {
        HttpChecksum httpChecksum = HttpChecksum.builder()
                                                .isRequestStreaming(false)
                                                .requestAlgorithm("SHA1")
                                                .build();
        AsyncExecuteRequest asyncExecuteRequest = getExecuteRequestBuilder().putHttpExecutionAttribute(OPERATION_NAME,
                                                                                                       "NonStreaming")

                                                                            .putHttpExecutionAttribute(HTTP_CHECKSUM,
                                                                                                       httpChecksum)
                                                                            .build();

        S3MetaRequestOptions actual = makeRequest(asyncExecuteRequest);
        assertThat(actual.getChecksumConfig().getChecksumAlgorithm()).isEqualTo(ChecksumAlgorithm.NONE);
    }

    @Test
    public void checksumRequiredOperation_noChecksumAlgoProvided_shouldSetByDefault() {
        HttpChecksum httpChecksum = HttpChecksum.builder()
                                                .requestChecksumRequired(true)
                                                .build();
        AsyncExecuteRequest asyncExecuteRequest = getExecuteRequestBuilder().putHttpExecutionAttribute(OPERATION_NAME,
                                                                                                       "PutObject")

                                                                            .putHttpExecutionAttribute(HTTP_CHECKSUM,
                                                                                                       httpChecksum)
                                                                            .build();

        S3MetaRequestOptions actual = makeRequest(asyncExecuteRequest);
        assertThat(actual.getChecksumAlgorithm()).isEqualTo(ChecksumAlgorithm.CRC32);
    }

    @Test
    public void streamingOperation_noChecksumAlgoProvided_shouldSetByDefault() {
        HttpChecksum httpChecksum = HttpChecksum.builder()
                                                .isRequestStreaming(true)
                                                .build();
        AsyncExecuteRequest asyncExecuteRequest = getExecuteRequestBuilder().putHttpExecutionAttribute(OPERATION_NAME,
                                                                                                       "PutObject")

                                                                            .putHttpExecutionAttribute(HTTP_CHECKSUM,
                                                                                                       httpChecksum)
                                                                            .build();

        S3MetaRequestOptions actual = makeRequest(asyncExecuteRequest);
        assertThat(actual.getChecksumAlgorithm()).isEqualTo(ChecksumAlgorithm.CRC32);
    }

    @Test
    public void operationWithResponseAlgorithms_validateNotSpecified_shouldValidateByDefault() {
        HttpChecksum httpChecksum = HttpChecksum.builder()
                                                .responseAlgorithms("CRC32")
                                                .build();
        AsyncExecuteRequest asyncExecuteRequest = getExecuteRequestBuilder().putHttpExecutionAttribute(OPERATION_NAME,
                                                                                                       "GetObject")

                                                                            .putHttpExecutionAttribute(HTTP_CHECKSUM,
                                                                                                       httpChecksum)
                                                                            .build();

        S3MetaRequestOptions actual = makeRequest(asyncExecuteRequest);
        assertThat(actual.getValidateChecksum()).isTrue();
    }

    @Test
    public void operationWithResponseAlgorithms_optOutValidationFromClient_shouldHonor() {
        HttpChecksum httpChecksum = HttpChecksum.builder()
                                                .responseAlgorithms("CRC32")
                                                .build();

        s3NativeClientConfiguration = S3NativeClientConfiguration.builder()
                                                                 .endpointOverride(DEFAULT_ENDPOINT)
                                                                 .credentialsProvider(null)
                                                                 .checksumValidationEnabled(false)
                                                                 .build();

        asyncHttpClient = new S3CrtAsyncHttpClient(s3Client, s3NativeClientConfiguration);

        AsyncExecuteRequest asyncExecuteRequest = getExecuteRequestBuilder().putHttpExecutionAttribute(OPERATION_NAME,
                                                                                                       "GetObject")

                                                                            .putHttpExecutionAttribute(HTTP_CHECKSUM,
                                                                                                       httpChecksum)
                                                                            .build();

        S3MetaRequestOptions actual = makeRequest(asyncExecuteRequest);
        assertThat(actual.getValidateChecksum()).isFalse();
    }

    @Test
    public void operationWithResponseAlgorithms_optInFromRequest_shouldHonor() {
        HttpChecksum httpChecksum = HttpChecksum.builder()
                                                .responseAlgorithms("CRC32")
                                                .requestValidationMode("Enabled")
                                                .build();

        s3NativeClientConfiguration = S3NativeClientConfiguration.builder()
                                                                 .endpointOverride(DEFAULT_ENDPOINT)
                                                                 .credentialsProvider(null)
                                                                 .checksumValidationEnabled(false)
                                                                 .build();

        asyncHttpClient = new S3CrtAsyncHttpClient(s3Client, s3NativeClientConfiguration);

        AsyncExecuteRequest asyncExecuteRequest = getExecuteRequestBuilder().putHttpExecutionAttribute(OPERATION_NAME,
                                                                                                       "GetObject")

                                                                            .putHttpExecutionAttribute(HTTP_CHECKSUM,
                                                                                                       httpChecksum)
                                                                            .build();

        S3MetaRequestOptions actual = makeRequest(asyncExecuteRequest);
        assertThat(actual.getValidateChecksum()).isTrue();
    }

    private S3MetaRequestOptions makeRequest(AsyncExecuteRequest asyncExecuteRequest) {
        ArgumentCaptor<S3MetaRequestOptions> s3MetaRequestOptionsArgumentCaptor =
            ArgumentCaptor.forClass(S3MetaRequestOptions.class);

        asyncHttpClient.execute(asyncExecuteRequest);

        verify(s3Client).makeMetaRequest(s3MetaRequestOptionsArgumentCaptor.capture());

        return s3MetaRequestOptionsArgumentCaptor.getValue();
    }

    @Test
    public void streamingOperation_checksumAlgoProvided_shouldTakePrecedence() {
        HttpChecksum httpChecksum = HttpChecksum.builder()
                                                .isRequestStreaming(true)
                                                .requestAlgorithm("SHA1")
                                                .build();
        AsyncExecuteRequest asyncExecuteRequest = getExecuteRequestBuilder().putHttpExecutionAttribute(OPERATION_NAME,
                                                                                                       "PutObject")

                                                                            .putHttpExecutionAttribute(HTTP_CHECKSUM,
                                                                                                       httpChecksum)
                                                                            .build();

        S3MetaRequestOptions actual = makeRequest(asyncExecuteRequest);
        assertThat(actual.getChecksumAlgorithm()).isEqualTo(ChecksumAlgorithm.SHA1);
    }

    @Test
    public void closeHttpClient_shouldCloseUnderlyingResources() {
        asyncHttpClient.close();
        verify(s3Client).close();
        s3NativeClientConfiguration.close();
    }

    private AsyncExecuteRequest.Builder getExecuteRequestBuilder() {
        return getExecuteRequestBuilder(443);
    }

    private AsyncExecuteRequest.Builder getExecuteRequestBuilder(Integer port) {
        return AsyncExecuteRequest.builder()
                                  .responseHandler(responseHandler)
                                  .requestContentPublisher(contentPublisher)
                                  .request(SdkHttpRequest.builder()
                                                         .protocol(DEFAULT_ENDPOINT.getScheme())
                                                         .method(SdkHttpMethod.GET)
                                                         .host(DEFAULT_ENDPOINT.getHost())
                                                         .port(port)
                                                         .encodedPath("/key")
                                                         .putHeader(CONTENT_LENGTH, "100")
                                                         .putHeader("amz-sdk-invocation-id",
                                                                    "1234")
                                                         .putHeader("custom-header", "foobar")
                                                         .build());
    }
}
