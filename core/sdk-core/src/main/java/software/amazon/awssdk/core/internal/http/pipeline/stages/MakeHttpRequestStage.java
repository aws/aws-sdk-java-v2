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

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.internal.http.HttpClientDependencies;
import software.amazon.awssdk.core.internal.http.InterruptMonitor;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.http.pipeline.RequestPipeline;
import software.amazon.awssdk.core.internal.metrics.BytesReadTrackingInputStream;
import software.amazon.awssdk.core.internal.util.MetricUtils;
import software.amazon.awssdk.core.internal.util.ProgressListenerUtils;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.utils.Pair;

/**
 * Delegate to the HTTP implementation to make an HTTP request and receive the response.
 */
@SdkInternalApi
public class MakeHttpRequestStage
    implements RequestPipeline<SdkHttpFullRequest, Pair<SdkHttpFullRequest, SdkHttpFullResponse>> {

    private final SdkHttpClient sdkHttpClient;

    public MakeHttpRequestStage(HttpClientDependencies dependencies) {
        this.sdkHttpClient = dependencies.clientConfiguration().option(SdkClientOption.SYNC_HTTP_CLIENT);
    }

    /**
     * Returns the response from executing one httpClientSettings request; or null for retry.
     */
    @Override
    public Pair<SdkHttpFullRequest, SdkHttpFullResponse> execute(SdkHttpFullRequest request,
                                                                 RequestExecutionContext context) throws Exception {
        InterruptMonitor.checkInterrupted();
        HttpExecuteResponse executeResponse = executeHttpRequest(request, context);
        // TODO: Plumb through ExecuteResponse instead
        SdkHttpFullResponse httpResponse = (SdkHttpFullResponse) executeResponse.httpResponse();
        return Pair.of(request, httpResponse.toBuilder().content(executeResponse.responseBody().orElse(null)).build());
    }

    private HttpExecuteResponse executeHttpRequest(SdkHttpFullRequest request, RequestExecutionContext context) throws Exception {

        MetricCollector attemptMetricCollector = context.attemptMetricCollector();

        MetricCollector httpMetricCollector = MetricUtils.createHttpMetricsCollector(context);

        ContentStreamProvider contentStreamProvider = null;
        if (request.contentStreamProvider().isPresent()) {
            AtomicLong bytesRead = context.executionAttributes()
                                          .getAttribute(SdkInternalExecutionAttribute.RESPONSE_BYTES_READ);

            BytesReadTrackingInputStream wrappedByteTracking = ProgressListenerUtils.wrapWithBytesReadTrackingStream(
                AbortableInputStream.create(request.contentStreamProvider().get().newStream()),
                bytesRead,
                context.progressUpdater());

            contentStreamProvider = ContentStreamProvider.fromInputStream(wrappedByteTracking);
        }

        ExecutableHttpRequest requestCallable = sdkHttpClient
            .prepareRequest(HttpExecuteRequest.builder()
                                              .request(request)
                                              .metricCollector(httpMetricCollector)
                                              .contentStreamProvider(contentStreamProvider)
                                              .build());

        context.apiCallTimeoutTracker().abortable(requestCallable);
        context.apiCallAttemptTimeoutTracker().abortable(requestCallable);

        long start = updateMetricCollectionAttributes(context);
        Pair<HttpExecuteResponse, Duration> measuredExecute = MetricUtils.measureDurationUnsafe(requestCallable, start);
        Duration executeDuration = measuredExecute.right();
        attemptMetricCollector.reportMetric(CoreMetric.SERVICE_CALL_DURATION, executeDuration);

        attemptMetricCollector.reportMetric(CoreMetric.TIME_TO_FIRST_BYTE, executeDuration);

        context.executionAttributes().putAttribute(SdkInternalExecutionAttribute.HEADERS_READ_END_NANO_TIME,
                                                   start + executeDuration.toNanos());

        return measuredExecute.left();
    }

    private static long updateMetricCollectionAttributes(RequestExecutionContext context) {
        long now = System.nanoTime();
        context.executionAttributes().putAttribute(SdkInternalExecutionAttribute.API_CALL_ATTEMPT_START_NANO_TIME,
                                                   now);
        return now;
    }
}
