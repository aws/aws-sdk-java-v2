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

package software.amazon.awssdk.core.client.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.exception.AbortedException;
import software.amazon.awssdk.core.exception.NonRetryableException;
import software.amazon.awssdk.core.exception.RetryableException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.protocol.VoidSdkResponse;
import software.amazon.awssdk.core.runtime.transform.Marshaller;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.retries.DefaultRetryStrategy;
import utils.HttpTestUtils;
import utils.ValidSdkObjects;

@RunWith(MockitoJUnitRunner.class)
public class SyncClientHandlerTest {
    private SdkSyncClientHandler syncClientHandler;

    @Mock
    private SdkRequest request;

    @Mock
    private Marshaller<SdkRequest> marshaller;

    private SdkHttpFullRequest marshalledRequest = ValidSdkObjects.sdkHttpFullRequest().build();

    @Mock
    private SdkHttpClient httpClient;

    @Mock
    private ExecutableHttpRequest httpClientCall;

    @Mock
    private HttpResponseHandler<SdkResponse> responseHandler;

    @Mock
    private HttpResponseHandler<SdkServiceException> errorResponseHandler;

    @Mock
    private ResponseTransformer<SdkResponse, ?> responseTransformer;

    @Before
    public void setup() {
        this.syncClientHandler = new SdkSyncClientHandler(clientConfiguration());
        when(request.overrideConfiguration()).thenReturn(Optional.empty());
    }

    @Test
    public void successfulExecutionCallsResponseHandler() throws Exception {

        SdkResponse expected = VoidSdkResponse.builder().build();
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("foo", Arrays.asList("bar"));

        // Given
        expectRetrievalFromMocks();
        when(httpClientCall.call()).thenReturn(HttpExecuteResponse.builder()
                                                                  .response(SdkHttpResponse.builder()
                                                                                       .statusCode(200)
                                                                                       .headers(headers)
                                                                                       .build())
                                                                  .build()); // Successful HTTP call
        when(responseHandler.handle(any(), any())).thenReturn(expected); // Response handler call

        // When
        SdkResponse actual = syncClientHandler.execute(clientExecutionParams());

        // Then
        verifyNoMoreInteractions(errorResponseHandler); // No error handler calls
        assertThat(actual.sdkHttpResponse().statusCode()).isEqualTo(200);
        assertThat(actual.sdkHttpResponse().headers()).isEqualTo(headers);
    }

    @Test
    public void failedExecutionCallsErrorResponseHandler() throws Exception {
        SdkServiceException exception = SdkServiceException.builder().message("Uh oh!").statusCode(500).numAttempts(1).build();

        Map<String, List<String>> headers = new HashMap<>();
        headers.put("foo", Arrays.asList("bar"));

        // Given
        expectRetrievalFromMocks();
        when(httpClientCall.call()).thenReturn(HttpExecuteResponse.builder()
                                                                  .response(SdkHttpResponse.builder()
                                                                                       .statusCode(500)
                                                                                       .headers(headers)
                                                                                       .build())
                                                                  .build()); // Failed HTTP call
        when(errorResponseHandler.handle(any(), any())).thenReturn(exception); // Error response handler call

        // When
        assertThatThrownBy(() -> syncClientHandler.execute(clientExecutionParams())).isEqualToComparingFieldByField(exception);

        // Then
        verifyNoMoreInteractions(responseHandler); // No response handler calls
    }

    @Test
    public void responseTransformerThrowsRetryableException_shouldPropogate() throws Exception {
        mockSuccessfulApiCall();
        when(responseTransformer.transform(any(SdkResponse.class), any(AbortableInputStream.class))).thenThrow(
            RetryableException.create("test"));

        assertThatThrownBy(() -> syncClientHandler.execute(clientExecutionParams(), responseTransformer))
            .isInstanceOf(RetryableException.class);
    }

    @Test
    public void responseTransformerThrowsInterruptedException_shouldPropagate() throws Exception {
        try {
            verifyResponseTransformerPropagateException(new InterruptedException());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    public void responseTransformerThrowsAbortedException_shouldPropagate() throws Exception {
        verifyResponseTransformerPropagateException(AbortedException.create(""));
    }

    @Test
    public void responseTransformerThrowsOtherException_shouldWrapWithNonRetryableException() throws Exception {
        mockSuccessfulApiCall();
        when(responseTransformer.transform(any(SdkResponse.class), any(AbortableInputStream.class))).thenThrow(
            new RuntimeException());

        assertThatThrownBy(() -> syncClientHandler.execute(clientExecutionParams(), responseTransformer))
            .hasCauseInstanceOf(NonRetryableException.class);
    }

    @Test
    public void execute_whenConcatenatedGzipHasTransientZeroAvailable_decodesAllMembers() throws Exception {
        mockSuccessfulStreamingCall(concatenatedGzip("member-0", "member-1", "member-2", "member-3", "member-4"));
        when(responseTransformer.needsConnectionLeftOpen()).thenReturn(true); // customer-held stream, e.g. toInputStream()
        when(responseTransformer.transform(any(SdkResponse.class), any(AbortableInputStream.class)))
            .thenAnswer(invocation -> readAllGzip(invocation.getArgument(1)));

        Object decoded = syncClientHandler.execute(clientExecutionParams(), responseTransformer);

        assertThat(decoded).isEqualTo("member-0member-1member-2member-3member-4");
    }

    @Test
    public void execute_whenConcatenatedGzipSupportDisabled_doesNotCoerceAvailable() throws Exception {
        mockSuccessfulStreamingCall(concatenatedGzip("member-0", "member-1"));
        AtomicInteger availableAfterHeader = new AtomicInteger(-1);
        when(responseTransformer.transform(any(SdkResponse.class), any(AbortableInputStream.class)))
            .thenAnswer(invocation -> {
                AbortableInputStream stream = invocation.getArgument(1);
                stream.read();
                stream.read();
                stream.read();
                availableAfterHeader.set(stream.available());
                return null;
            });
        ClientExecutionParams<SdkRequest, SdkResponse> params = clientExecutionParams()
            .putExecutionAttribute(SdkInternalExecutionAttribute.CONCATENATED_GZIP_STREAM_SUPPORT_ENABLED, false);

        syncClientHandler.execute(params, responseTransformer);

        assertThat(availableAfterHeader.get()).isZero();
    }

    @Test
    public void execute_whenCustomTransformerDoesNotLeaveConnectionOpen_decodesAllGzipMembers() throws Exception {
        mockSuccessfulStreamingCall(concatenatedGzip("member-0", "member-1", "member-2"));
        ResponseTransformer<SdkResponse, String> customTransformer =
            (response, inputStream) -> readAllGzip(inputStream);

        String decoded = syncClientHandler.execute(clientExecutionParams(), customTransformer);

        assertThat(decoded).isEqualTo("member-0member-1member-2");
    }

    private void verifyResponseTransformerPropagateException(Exception exception) throws Exception {
        mockSuccessfulApiCall();
        when(responseTransformer.transform(any(SdkResponse.class), any(AbortableInputStream.class))).thenThrow(
            exception);

        assertThatThrownBy(() -> syncClientHandler.execute(clientExecutionParams(), responseTransformer))
            .hasCauseInstanceOf(exception.getClass());
    }

    private void mockSuccessfulApiCall() throws Exception {
        expectRetrievalFromMocks();
        when(httpClientCall.call()).thenReturn(HttpExecuteResponse.builder()
                                                                  .responseBody(AbortableInputStream.create(new ByteArrayInputStream("TEST".getBytes())))
                                                                  .response(SdkHttpResponse.builder().statusCode(200).build())
                                                                  .build());
        when(responseHandler.handle(any(), any())).thenReturn(VoidSdkResponse.builder().build());
    }

    private void expectRetrievalFromMocks() {
        when(marshaller.marshall(request)).thenReturn(marshalledRequest);
        when(httpClient.prepareRequest(any())).thenReturn(httpClientCall);
    }

    private void mockSuccessfulStreamingCall(byte[] body) throws Exception {
        expectRetrievalFromMocks();
        when(httpClientCall.call()).thenReturn(HttpExecuteResponse.builder()
                                                                  .responseBody(AbortableInputStream.create(zeroAvailableDripStream(body)))
                                                                  .response(SdkHttpResponse.builder().statusCode(200).build())
                                                                  .build());
        when(responseHandler.handle(any(), any())).thenReturn(VoidSdkResponse.builder().build());
    }

    /** Serves one byte per read and always reports {@code available()==0}, mimicking a socket with no bytes buffered. */
    private static InputStream zeroAvailableDripStream(byte[] data) {
        return new InputStream() {
            private int pos;

            @Override
            public int read() {
                return pos < data.length ? data[pos++] & 0xff : -1;
            }

            @Override
            public int read(byte[] b, int off, int len) {
                if (len == 0) {
                    return 0;
                }
                if (pos >= data.length) {
                    return -1;
                }
                b[off] = (byte) (data[pos++] & 0xff);
                return 1;
            }

            @Override
            public int available() {
                return 0;
            }
        };
    }

    private static byte[] concatenatedGzip(String... members) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (String member : members) {
            ByteArrayOutputStream one = new ByteArrayOutputStream();
            try (GZIPOutputStream gz = new GZIPOutputStream(one)) {
                gz.write(member.getBytes(StandardCharsets.UTF_8));
            }
            out.write(one.toByteArray());
        }
        return out.toByteArray();
    }

    private static String readAllGzip(InputStream in) throws IOException {
        try (GZIPInputStream gz = new GZIPInputStream(in)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[64];
            int n;
            while ((n = gz.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private ClientExecutionParams<SdkRequest, SdkResponse> clientExecutionParams() {
        return new ClientExecutionParams<SdkRequest, SdkResponse>()
                .withInput(request)
                .withMarshaller(marshaller)
                .withResponseHandler(responseHandler)
                .withErrorResponseHandler(errorResponseHandler);
    }

    public SdkClientConfiguration clientConfiguration() {
        return HttpTestUtils.testClientConfiguration().toBuilder()
                            .option(SdkClientOption.SYNC_HTTP_CLIENT, httpClient)
                            .option(SdkClientOption.RETRY_STRATEGY, DefaultRetryStrategy.doNotRetry())
                            .build();
    }
}
