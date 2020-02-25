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
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
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
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.http.NoopTestRequest;
import software.amazon.awssdk.core.internal.http.HttpClientDependencies;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.http.pipeline.RequestPipeline;
import software.amazon.awssdk.core.internal.http.timers.ClientExecutionAndRequestTimerTestUtils;
import software.amazon.awssdk.core.internal.util.CapacityManager;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import utils.ValidSdkObjects;

/**
 * Unit tests for {@link AsyncApiCallTimeoutTrackingStage}.
 */
@RunWith(MockitoJUnitRunner.class)
public class AsyncApiCallTimeoutTrackingStageTest {

    private final long TIMEOUT_MILLIS = 1234;

    @Mock
    private RequestPipeline<SdkHttpFullRequest, CompletableFuture> requestPipeline;

    @Mock
    private ScheduledExecutorService executorService;

    private SdkClientConfiguration configuration;

    private HttpClientDependencies dependencies;

    @Mock
    private CapacityManager capacityManager;

    private SdkHttpFullRequest httpRequest;

    private RequestExecutionContext requestExecutionContext;

    private SdkRequest sdkRequest = NoopTestRequest.builder().build();

    @Before
    public void methodSetup() throws Exception {
        configuration = SdkClientConfiguration.builder()
                .option(SdkClientOption.SCHEDULED_EXECUTOR_SERVICE, executorService)
                .option(SdkClientOption.API_CALL_TIMEOUT, Duration.ofMillis(TIMEOUT_MILLIS))
                .build();

        dependencies = HttpClientDependencies.builder()
                .clientConfiguration(configuration)
                .build();

        httpRequest = SdkHttpFullRequest.builder()
                                        .uri(URI.create("https://localhost"))
                                        .method(SdkHttpMethod.GET)
                                        .build();

        requestExecutionContext = RequestExecutionContext.builder()
                .originalRequest(sdkRequest)
                .executionContext(ClientExecutionAndRequestTimerTestUtils
                        .executionContext(ValidSdkObjects.sdkHttpFullRequest().build()))
                .build();

        when(requestPipeline.execute(any(SdkHttpFullRequest.class), any(RequestExecutionContext.class)))
                .thenReturn(new CompletableFuture());

        when(executorService.schedule(any(Runnable.class), anyLong(), any(TimeUnit.class))).thenReturn(mock(ScheduledFuture.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSchedulesTheTimeoutUsingSuppliedExecutorService() throws Exception {
        AsyncApiCallTimeoutTrackingStage apiCallTimeoutTrackingStage = new AsyncApiCallTimeoutTrackingStage(dependencies,
                requestPipeline);
        apiCallTimeoutTrackingStage.execute(httpRequest, requestExecutionContext);
        verify(executorService)
                .schedule(any(Runnable.class), eq(TIMEOUT_MILLIS), eq(TimeUnit.MILLISECONDS));
    }
}
