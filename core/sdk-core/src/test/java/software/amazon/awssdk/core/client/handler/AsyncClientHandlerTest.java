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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.async.EmptyPublisher;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.core.protocol.VoidSdkResponse;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.runtime.transform.Marshaller;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.retries.DefaultRetryStrategy;
import utils.HttpTestUtils;
import utils.ValidSdkObjects;

@RunWith(MockitoJUnitRunner.class)
public class AsyncClientHandlerTest {
    private SdkAsyncClientHandler asyncClientHandler;

    @Mock
    private SdkRequest request;

    @Mock
    private Marshaller<SdkRequest> marshaller;

    private SdkHttpFullRequest marshalledRequest = ValidSdkObjects.sdkHttpFullRequest().build();

    @Mock
    private SdkAsyncHttpClient httpClient;

    private CompletableFuture<Void> httpClientFuture = CompletableFuture.completedFuture(null);

    @Mock
    private HttpResponseHandler<SdkResponse> responseHandler;

    @Mock
    private HttpResponseHandler<SdkServiceException> errorResponseHandler;

    @Before
    public void setup() {
        this.asyncClientHandler = new SdkAsyncClientHandler(clientConfiguration());
        when(request.overrideConfiguration()).thenReturn(Optional.empty());
    }

    @Test
    public void successfulExecutionCallsResponseHandler() throws Exception {
        // Given
        SdkResponse expected = VoidSdkResponse.builder().build();
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("foo", Arrays.asList("bar"));
        ArgumentCaptor<AsyncExecuteRequest> executeRequest = ArgumentCaptor.forClass(AsyncExecuteRequest.class);

        expectRetrievalFromMocks();
        when(httpClient.execute(executeRequest.capture())).thenReturn(httpClientFuture);
        when(responseHandler.handle(any(), any())).thenReturn(expected); // Response handler call

        // When
        CompletableFuture<SdkResponse> responseFuture = asyncClientHandler.execute(clientExecutionParams());
        SdkAsyncHttpResponseHandler capturedHandler = executeRequest.getValue().responseHandler();
        capturedHandler.onHeaders(SdkHttpFullResponse.builder().statusCode(200)
                                                                             .headers(headers).build());
        capturedHandler.onStream(new EmptyPublisher<>());
        SdkResponse actualResponse = responseFuture.get(1, TimeUnit.SECONDS);

        // Then
        verifyNoMoreInteractions(errorResponseHandler); // No error handler calls
        assertThat(actualResponse.sdkHttpResponse().statusCode()).isEqualTo(200);
        assertThat(actualResponse.sdkHttpResponse().headers()).isEqualTo(headers);
    }

    /**
     * The handler reports {@code API_CALL_DURATION} and {@code API_CALL_SUCCESSFUL} from a {@code whenComplete} action on
     * the future it returns, so a collector that throws must fail that future rather than be swallowed.
     *
     * <p>Only assertable on the success path. When the source future has already failed and the {@code whenComplete}
     * action throws, {@link CompletableFuture} keeps the source exception and discards the action's. The pipeline stage
     * this reporting replaced behaved the same way, so that is a pre-existing limitation rather than a new one.
     */
    @Test
    public void metricCollectorThrowsOnSuccessfulExecution_exceptionReportedInFuture() throws Exception {
        MetricCollector metricCollector = mock(MetricCollector.class);
        when(metricCollector.createChild(any())).thenReturn(metricCollector);
        RuntimeException exception = new RuntimeException("metric collector boom");
        doThrow(exception).when(metricCollector).reportMetric(eq(CoreMetric.API_CALL_DURATION), any(Duration.class));

        ArgumentCaptor<AsyncExecuteRequest> executeRequest = ArgumentCaptor.forClass(AsyncExecuteRequest.class);

        expectRetrievalFromMocks();
        when(httpClient.execute(executeRequest.capture())).thenReturn(httpClientFuture);
        when(responseHandler.handle(any(), any())).thenReturn(VoidSdkResponse.builder().build());

        CompletableFuture<SdkResponse> responseFuture =
            asyncClientHandler.execute(clientExecutionParams().withMetricCollector(metricCollector));
        SdkAsyncHttpResponseHandler capturedHandler = executeRequest.getValue().responseHandler();
        capturedHandler.onHeaders(SdkHttpFullResponse.builder().statusCode(200).build());
        capturedHandler.onStream(new EmptyPublisher<>());

        assertThatThrownBy(() -> responseFuture.get(1, TimeUnit.SECONDS)).hasRootCause(exception);
    }

    @Test
    public void failedExecutionCallsErrorResponseHandler() throws Exception {
        SdkServiceException exception = SdkServiceException.builder().message("Uh oh!").statusCode(500).build();

        // Given
        ArgumentCaptor<AsyncExecuteRequest> executeRequest = ArgumentCaptor.forClass(AsyncExecuteRequest.class);

        expectRetrievalFromMocks();
        when(httpClient.execute(executeRequest.capture())).thenReturn(httpClientFuture);
        when(errorResponseHandler.handle(any(), any())).thenReturn(exception); // Error response handler call

        // When
        CompletableFuture<SdkResponse> responseFuture = asyncClientHandler.execute(clientExecutionParams());
        SdkAsyncHttpResponseHandler capturedHandler = executeRequest.getValue().responseHandler();
        capturedHandler.onHeaders(SdkHttpFullResponse.builder().statusCode(500).build());
        capturedHandler.onStream(new EmptyPublisher<>());
        assertThatThrownBy(() -> responseFuture.get(1, TimeUnit.SECONDS)).hasCause(exception);

        // Then
        verifyNoMoreInteractions(responseHandler); // Response handler is not called
    }

    private void expectRetrievalFromMocks() {
        when(marshaller.marshall(request)).thenReturn(marshalledRequest);
    }

    private ClientExecutionParams<SdkRequest, SdkResponse> clientExecutionParams() {
        return new ClientExecutionParams<SdkRequest, SdkResponse>()
                .withInput(request)
                .withMarshaller(marshaller)
                .withResponseHandler(responseHandler)
                .withErrorResponseHandler(errorResponseHandler);
    }

    public SdkClientConfiguration clientConfiguration() {
        ScheduledExecutorService exec = mock(ScheduledExecutorService.class);
        return HttpTestUtils.testClientConfiguration().toBuilder()
                            .option(SdkClientOption.ASYNC_HTTP_CLIENT, httpClient)
                            .option(SdkClientOption.RETRY_POLICY, RetryPolicy.none())
                            .option(SdkClientOption.RETRY_STRATEGY, DefaultRetryStrategy.doNotRetry())
            .option(SdkClientOption.SCHEDULED_EXECUTOR_SERVICE, exec)
                            .build();
    }
}
