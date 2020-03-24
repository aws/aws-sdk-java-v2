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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.core.client.config.SdkClientOption.API_CALL_ATTEMPT_TIMEOUT;
import static software.amazon.awssdk.core.client.config.SdkClientOption.ASYNC_HTTP_CLIENT;
import static software.amazon.awssdk.core.client.config.SdkClientOption.SCHEDULED_EXECUTOR_SERVICE;
import static software.amazon.awssdk.core.internal.util.AsyncResponseHandlerTestUtils.combinedAsyncResponseHandler;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.core.client.config.SdkAdvancedAsyncClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.http.NoopTestRequest;
import software.amazon.awssdk.core.internal.http.HttpClientDependencies;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.http.timers.ClientExecutionAndRequestTimerTestUtils;
import software.amazon.awssdk.core.internal.util.AsyncResponseHandlerTestUtils;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import utils.ValidSdkObjects;

@RunWith(MockitoJUnitRunner.class)
public class MakeAsyncHttpRequestStageTest {

    @Mock
    private SdkAsyncHttpClient sdkAsyncHttpClient;

    @Mock
    private ScheduledExecutorService timeoutExecutor;

    private CompletableFuture<Void> clientExecuteFuture = CompletableFuture.completedFuture(null);

    @Mock
    private ScheduledFuture future;

    private MakeAsyncHttpRequestStage stage;

    @Before
    public void setup() {
        when(sdkAsyncHttpClient.execute(any())).thenReturn(clientExecuteFuture);
        when(timeoutExecutor.schedule(any(Runnable.class), anyLong(), any(TimeUnit.class))).thenReturn(future);
    }

    @Test
    public void apiCallAttemptTimeoutEnabled_shouldInvokeExecutor() throws Exception {
        stage = new MakeAsyncHttpRequestStage<>(
            combinedAsyncResponseHandler(AsyncResponseHandlerTestUtils.noOpResponseHandler(),
                                         AsyncResponseHandlerTestUtils.noOpResponseHandler()),
            clientDependencies(Duration.ofMillis(1000)));
        stage.execute(ValidSdkObjects.sdkHttpFullRequest().build(), requestContext());

        verify(timeoutExecutor, times(1)).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));
    }

    @Test
    public void apiCallAttemptTimeoutNotEnabled_shouldNotInvokeExecutor() throws Exception {
        stage = new MakeAsyncHttpRequestStage<>(
            combinedAsyncResponseHandler(AsyncResponseHandlerTestUtils.noOpResponseHandler(),
                                         AsyncResponseHandlerTestUtils.noOpResponseHandler()),
            clientDependencies(null));
        stage.execute(ValidSdkObjects.sdkHttpFullRequest().build(), requestContext());

        verify(timeoutExecutor, never()).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));
    }

    private HttpClientDependencies clientDependencies(Duration timeout) {
        SdkClientConfiguration configuration = SdkClientConfiguration.builder()
                                                                     .option(SdkAdvancedAsyncClientOption.FUTURE_COMPLETION_EXECUTOR, Runnable::run)
                                                                     .option(ASYNC_HTTP_CLIENT, sdkAsyncHttpClient)
                                                                     .option(SCHEDULED_EXECUTOR_SERVICE, timeoutExecutor)
                                                                     .option(API_CALL_ATTEMPT_TIMEOUT, timeout)
                                                                     .build();


        return HttpClientDependencies.builder()
                                     .clientConfiguration(configuration)
                                     .build();
    }

    private RequestExecutionContext requestContext() {
        ExecutionContext executionContext = ClientExecutionAndRequestTimerTestUtils.executionContext(ValidSdkObjects.sdkHttpFullRequest().build());
        return RequestExecutionContext.builder()
                                      .executionContext(executionContext)
                                      .originalRequest(NoopTestRequest.builder().build())
                                      .build();
    }
}
