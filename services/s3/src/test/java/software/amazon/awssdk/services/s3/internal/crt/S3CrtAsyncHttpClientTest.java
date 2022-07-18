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
import static software.amazon.awssdk.services.s3.internal.crt.S3InternalSdkHttpExecutionAttribute.CHECKSUM_SPECS;
import static software.amazon.awssdk.services.s3.internal.crt.S3InternalSdkHttpExecutionAttribute.OPERATION_NAME;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.core.checksums.Algorithm;
import software.amazon.awssdk.core.checksums.ChecksumSpecs;
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

@RunWith(MockitoJUnitRunner.class)
public class S3CrtAsyncHttpClientTest {
    private static final URI DEFAULT_ENDPOINT = URI.create("https://127.0.0.1:443");

    private S3CrtAsyncHttpClient asyncHttpClient;
    private S3NativeClientConfiguration s3NativeClientConfiguration;

    @Mock
    private S3Client s3Client;

    @Mock
    private SdkAsyncHttpResponseHandler responseHandler;

    @Mock
    private SdkHttpContentPublisher contentPublisher;

    @Before
    public void methodSetup() {

        s3NativeClientConfiguration = S3NativeClientConfiguration.builder()
                                                                 .endpointOverride(DEFAULT_ENDPOINT)
                                                                 .credentialsProvider(null)
                                                                 .build();

        asyncHttpClient = new S3CrtAsyncHttpClient(s3Client, s3NativeClientConfiguration);
    }

    @Test
    public void defaultRequest_shouldSetMetaRequestOptionsCorrectly() {
        AsyncExecuteRequest asyncExecuteRequest = getExecuteRequestBuilder().build();

        ArgumentCaptor<S3MetaRequestOptions> s3MetaRequestOptionsArgumentCaptor =
            ArgumentCaptor.forClass(S3MetaRequestOptions.class);

        asyncHttpClient.execute(asyncExecuteRequest);

        verify(s3Client).makeMetaRequest(s3MetaRequestOptionsArgumentCaptor.capture());

        S3MetaRequestOptions actual = s3MetaRequestOptionsArgumentCaptor.getValue();
        assertThat(actual.getMetaRequestType()).isEqualTo(S3MetaRequestOptions.MetaRequestType.DEFAULT);
        assertThat(actual.getCredentialsProvider()).isNull();
        assertThat(actual.getEndpoint().equals(DEFAULT_ENDPOINT));
        assertThat(actual.getChecksumAlgorithm()).isEqualTo(ChecksumAlgorithm.CRC32);

        HttpRequest httpRequest = actual.getHttpRequest();
        assertThat(httpRequest.getEncodedPath()).isEqualTo("/key");

        Map<String, String> headers = httpRequest.getHeaders()
                                                 .stream()
                                                 .collect(HashMap::new, (m, h) -> m.put(h.getName(), h.getValue())
                                                     , Map::putAll);

        assertThat(headers).hasSize(4)
                           .containsEntry("Host", DEFAULT_ENDPOINT.getHost())
                           .containsEntry("custom-header", "foobar")
                           .containsEntry("amz-sdk-invocation-id", "1234")
                           .containsEntry("Content-Length", "100");
    }

    @Test
    public void getObject_shouldSetMetaRequestTypeCorrectly() {
        AsyncExecuteRequest asyncExecuteRequest = getExecuteRequestBuilder().putHttpExecutionAttribute(OPERATION_NAME,
                                                                                                          "GetObject").build();

        ArgumentCaptor<S3MetaRequestOptions> s3MetaRequestOptionsArgumentCaptor =
            ArgumentCaptor.forClass(S3MetaRequestOptions.class);

        asyncHttpClient.execute(asyncExecuteRequest);

        verify(s3Client).makeMetaRequest(s3MetaRequestOptionsArgumentCaptor.capture());

        S3MetaRequestOptions actual = s3MetaRequestOptionsArgumentCaptor.getValue();
        assertThat(actual.getMetaRequestType()).isEqualTo(S3MetaRequestOptions.MetaRequestType.GET_OBJECT);
    }

    @Test
    public void putObject_shouldSetMetaRequestTypeCorrectly() {
        AsyncExecuteRequest asyncExecuteRequest = getExecuteRequestBuilder().putHttpExecutionAttribute(OPERATION_NAME,
                                                                                                          "PutObject").build();

        ArgumentCaptor<S3MetaRequestOptions> s3MetaRequestOptionsArgumentCaptor =
            ArgumentCaptor.forClass(S3MetaRequestOptions.class);

        asyncHttpClient.execute(asyncExecuteRequest);

        verify(s3Client).makeMetaRequest(s3MetaRequestOptionsArgumentCaptor.capture());

        S3MetaRequestOptions actual = s3MetaRequestOptionsArgumentCaptor.getValue();
        assertThat(actual.getMetaRequestType()).isEqualTo(S3MetaRequestOptions.MetaRequestType.PUT_OBJECT);
    }

    @Test
    public void putObject_shouldSetChecksumAlgorithmCorrectly() {
        ChecksumSpecs checksumSpecs = ChecksumSpecs.builder().algorithm(Algorithm.SHA1).build();
        AsyncExecuteRequest asyncExecuteRequest = getExecuteRequestBuilder().putHttpExecutionAttribute(CHECKSUM_SPECS,
                                                                                                       checksumSpecs).build();

        ArgumentCaptor<S3MetaRequestOptions> s3MetaRequestOptionsArgumentCaptor =
            ArgumentCaptor.forClass(S3MetaRequestOptions.class);

        asyncHttpClient.execute(asyncExecuteRequest);

        verify(s3Client).makeMetaRequest(s3MetaRequestOptionsArgumentCaptor.capture());

        S3MetaRequestOptions actual = s3MetaRequestOptionsArgumentCaptor.getValue();
        assertThat(actual.getChecksumAlgorithm()).isEqualTo(ChecksumAlgorithm.SHA1);
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
    public void closeHttpClient_shouldCloseUnderlyingResources() {
        asyncHttpClient.close();
        verify(s3Client).close();
        s3NativeClientConfiguration.close();
    }

    private AsyncExecuteRequest.Builder getExecuteRequestBuilder() {
        return AsyncExecuteRequest.builder()
                                  .responseHandler(responseHandler)
                                  .requestContentPublisher(contentPublisher)
                                  .request(SdkHttpRequest.builder()
                                                         .protocol(DEFAULT_ENDPOINT.getScheme())
                                                         .method(SdkHttpMethod.GET)
                                                         .host(DEFAULT_ENDPOINT.getHost())
                                                         .port(DEFAULT_ENDPOINT.getPort())
                                                         .encodedPath("/key")
                                                         .putHeader(CONTENT_LENGTH, "100")
                                                         .putHeader("amz-sdk-invocation-id",
                                                                    "1234")
                                                         .putHeader("custom-header", "foobar")
                                                         .build());
    }
}
