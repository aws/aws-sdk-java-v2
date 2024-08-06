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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.core.client.config.SdkClientOption.SCHEDULED_EXECUTOR_SERVICE;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.core.Response;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkRequestOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.exception.AbortedException;
import software.amazon.awssdk.core.exception.ApiCallAttemptTimeoutException;
import software.amazon.awssdk.core.exception.SdkInterruptedException;
import software.amazon.awssdk.core.http.NoopTestRequest;
import software.amazon.awssdk.core.internal.http.HttpClientDependencies;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.http.pipeline.RequestPipeline;
import software.amazon.awssdk.core.internal.http.timers.ApiCallTimeoutTracker;
import software.amazon.awssdk.core.internal.http.timers.ClientExecutionAndRequestTimerTestUtils;
import software.amazon.awssdk.core.internal.http.timers.NoOpTimeoutTracker;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import utils.ValidSdkObjects;

@RunWith(MockitoJUnitRunner.class)
public class ApiCallAttemptTimeoutTrackingStageTest {

    @Mock
    private RequestPipeline<SdkHttpFullRequest, Response<Void>> wrapped;

    @Mock
    private ScheduledExecutorService timeoutExecutor;

    @Mock
    private ScheduledFuture scheduledFuture;

    @Mock
    private InputStream responseStream;

    private ApiCallAttemptTimeoutTrackingStage<Void> stage;

    @Before
    public void setUp() throws Exception {
        stage = new ApiCallAttemptTimeoutTrackingStage<>(HttpClientDependencies.builder()
                                                                               .clientConfiguration(
                                                                                   SdkClientConfiguration.builder()
                                                                                                         .option(SCHEDULED_EXECUTOR_SERVICE, timeoutExecutor)
                                                                                                         .build())
                                                                               .build(), wrapped);
    }

    @Test
    public void timeoutEnabled_shouldHaveTracker() throws Exception {
        when(wrapped.execute(any(SdkHttpFullRequest.class), any(RequestExecutionContext.class)))
            .thenAnswer(invocationOnMock -> null);
        when(timeoutExecutor.schedule(any(Runnable.class), anyLong(), any(TimeUnit.class))).thenReturn(scheduledFuture);
        RequestExecutionContext context = requestContext(500);
        stage.execute(mock(SdkHttpFullRequest.class), context);
        assertThat(scheduledFuture.isDone()).isFalse();
        assertThat(context.apiCallAttemptTimeoutTracker()).isInstanceOf(ApiCallTimeoutTracker.class);
    }

    @Test
    public void timeoutDisabled_shouldNotExecuteTimer() throws Exception {
        when(wrapped.execute(any(SdkHttpFullRequest.class), any(RequestExecutionContext.class)))
            .thenAnswer(invocationOnMock -> null);

        RequestExecutionContext context = requestContext(0);
        stage.execute(mock(SdkHttpFullRequest.class), context);
        assertThat(context.apiCallAttemptTimeoutTracker()).isInstanceOf(NoOpTimeoutTracker.class);
        assertThat(context.apiCallAttemptTimeoutTracker().isEnabled()).isFalse();
        assertThat(context.apiCallAttemptTimeoutTracker().hasExecuted()).isFalse();
    }

    @Test
    public void interruptedException_streamCloseFailed_shouldNotSurface() throws Exception {
        SdkHttpFullResponse response = SdkHttpFullResponse.builder().content(AbortableInputStream.create(responseStream,
                                                                                                         () -> {})).build();
        when(timeoutExecutor.schedule(any(Runnable.class), anyLong(), any(TimeUnit.class))).thenReturn(scheduledFuture);
        when(wrapped.execute(any(), any())).thenThrow(new SdkInterruptedException(response));
        RequestExecutionContext context = requestContext(1);
        doThrow(new IOException()).when(responseStream).close();
        assertThatThrownBy(() -> stage.execute(ValidSdkObjects.sdkHttpFullRequest().build(), context))
            .isInstanceOfAny(AbortedException.class);
        verify(responseStream).close();
    }

    private RequestExecutionContext requestContext(long timeout) {
        SdkRequestOverrideConfiguration.Builder configBuilder = SdkRequestOverrideConfiguration.builder();

        if (timeout > 0) {
            configBuilder.apiCallAttemptTimeout(Duration.ofMillis(timeout));
        }

        SdkRequest originalRequest =
            NoopTestRequest.builder()
                           .overrideConfiguration(configBuilder
                                                      .build())
                           .build();
        return RequestExecutionContext.builder()
                                      .executionContext(ClientExecutionAndRequestTimerTestUtils.executionContext(null))
                                      .originalRequest(originalRequest)
                                      .build();
    }
}
