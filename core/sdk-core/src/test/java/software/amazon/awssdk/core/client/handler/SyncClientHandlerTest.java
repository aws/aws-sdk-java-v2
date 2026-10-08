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

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.protocol.VoidSdkResponse;
import software.amazon.awssdk.core.runtime.transform.Marshaller;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.endpoints.EndpointUrl;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpRequest;
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

    /**
     * The pre-modify snapshot must never encode the marshalled request's query parameters. For the query and ec2
     * protocols those parameters still hold the entire request payload when the snapshot is taken, so encoding them
     * costs a full pass over the payload on every API call, and retaining them keeps the payload alive for the rest of
     * the call. Guards the regression fixed in this change.
     */
    @Test
    public void snapshottedEndpoint_doesNotEncodeOrRetainQueryParameters() throws Exception {
        SdkHttpFullRequest.Builder marshalled =
            ValidSdkObjects.sdkHttpFullRequest(8080)
                           .encodedPath("/2015-03-31/functions/my-function/invocations")
                           .putRawQueryParameter("Qualifier", "prod");
        // A handful of payload-shaped parameters is enough: the assertion is that the query string is absent
        // entirely, so any of these appearing in the snapshot is a failure.
        for (int i = 0; i < 20; i++) {
            marshalled.putRawQueryParameter("MetricData.member." + i + ".Value", "12345.6789");
        }

        List<EndpointUrl> capturedEndpoint = new ArrayList<>();
        List<URI> capturedUri = new ArrayList<>();
        ExecutionInterceptor interceptor = new ExecutionInterceptor() {
            @Override
            public SdkHttpRequest modifyHttpRequest(Context.ModifyHttpRequest context, ExecutionAttributes attrs) {
                capturedEndpoint.add(
                    attrs.getAttribute(SdkInternalExecutionAttribute.HTTP_REQUEST_ENDPOINT_BEFORE_MODIFY));
                capturedUri.add(attrs.getAttribute(SdkInternalExecutionAttribute.HTTP_REQUEST_URI_BEFORE_MODIFY));
                return context.httpRequest();
            }
        };

        SdkSyncClientHandler handler = new SdkSyncClientHandler(
            clientConfiguration().toBuilder()
                                 .option(SdkClientOption.EXECUTION_INTERCEPTORS, singletonList(interceptor))
                                 .build());

        when(marshaller.marshall(request)).thenReturn(marshalled.build());
        when(httpClient.prepareRequest(any())).thenReturn(httpClientCall);
        when(httpClientCall.call()).thenReturn(HttpExecuteResponse.builder()
                                                                 .response(SdkHttpResponse.builder()
                                                                                          .statusCode(200)
                                                                                          .build())
                                                                 .build());
        when(responseHandler.handle(any(), any())).thenReturn(VoidSdkResponse.builder().build());

        handler.execute(clientExecutionParams());

        // The endpoint snapshot holds components only; nothing references the query parameters.
        assertThat(capturedEndpoint).hasSize(1);
        assertThat(capturedEndpoint.get(0).host()).isEqualTo("localhost");
        assertThat(capturedEndpoint.get(0).encodedPath()).isEqualTo("/2015-03-31/functions/my-function/invocations");

        // The deprecated URI view carries no query string, so the payload was never encoded.
        URI uri = capturedUri.get(0);
        assertThat(uri.getQuery()).isNull();
        assertThat(uri.toString()).isEqualTo("http://localhost:8080/2015-03-31/functions/my-function/invocations");

        // The known consumer pattern: extract the path suffix following "/invocations".
        String path = uri.toString();
        int idx = path.indexOf("/invocations");
        assertThat(idx).isGreaterThanOrEqualTo(0);
        assertThat(path.substring(idx + "/invocations".length())).isEmpty();
    }

    private void expectRetrievalFromMocks() {
        when(marshaller.marshall(request)).thenReturn(marshalledRequest);
        when(httpClient.prepareRequest(any())).thenReturn(httpClientCall);
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
