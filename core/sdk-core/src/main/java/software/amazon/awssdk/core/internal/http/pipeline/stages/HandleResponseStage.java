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

import static software.amazon.awssdk.core.internal.util.ProgressListenerUtils.updateProgressListenersWithResponseStatus;
import static software.amazon.awssdk.core.internal.util.ProgressListenerUtils.wrapWithBytesReadTrackingStream;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.Response;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.http.pipeline.RequestPipeline;
import software.amazon.awssdk.core.internal.metrics.BytesReadTrackingInputStream;
import software.amazon.awssdk.core.internal.util.MetricUtils;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.metrics.MetricCollector;

/**
 * Pipeline stage that executes an {@link HttpResponseHandler} to transform the response into a {@link Response}
 * object that contains a flag indicating success of failure and an unmarshalled response object or exception as
 * appropriate.
 */
@SdkInternalApi
public class HandleResponseStage<OutputT> implements RequestPipeline<SdkHttpFullResponse, Response<OutputT>> {
    private final HttpResponseHandler<Response<OutputT>> responseHandler;

    public HandleResponseStage(HttpResponseHandler<Response<OutputT>> responseHandler) {
        this.responseHandler = responseHandler;
    }

    @Override
    public Response<OutputT> execute(SdkHttpFullResponse httpResponse, RequestExecutionContext context) throws Exception {

        updateProgressListenersWithResponseStatus(context.progressUpdater(), httpResponse);
        SdkHttpFullResponse bytesReadTracking = trackBytesRead(httpResponse, context);

        Response<OutputT> response = responseHandler.handle(bytesReadTracking, context.executionAttributes());
        collectMetrics(context);

        return response;
    }

    private void collectMetrics(RequestExecutionContext context) {
        MetricCollector attemptMetricCollector = context.attemptMetricCollector();

        long attemptStartTime = context.executionAttributes()
                                       .getAttribute(SdkInternalExecutionAttribute.API_CALL_ATTEMPT_START_NANO_TIME);

        long now = System.nanoTime();
        long ttlb = now - attemptStartTime;
        attemptMetricCollector.reportMetric(CoreMetric.TIME_TO_LAST_BYTE, Duration.ofNanos(ttlb));

        long responseBytesRead = MetricUtils.apiCallAttemptResponseBytesRead(context).getAsLong();
        long responseReadStart = MetricUtils.responseHeadersReadEndNanoTime(context).getAsLong();
        double throughput = MetricUtils.bytesPerSec(responseBytesRead, responseReadStart, now);

        attemptMetricCollector.reportMetric(CoreMetric.READ_THROUGHPUT, throughput);
    }

    private SdkHttpFullResponse trackBytesRead(SdkHttpFullResponse httpFullResponse, RequestExecutionContext context) {
        if (!httpFullResponse.content().isPresent()) {
            return httpFullResponse;
        }

        AbortableInputStream content = httpFullResponse.content().get();

        return httpFullResponse.toBuilder()
                               .content(trackBytesRead(content, context))
                               .build();
    }

    private AbortableInputStream trackBytesRead(AbortableInputStream content, RequestExecutionContext context) {
        AtomicLong bytesRead = context.executionAttributes().getAttribute(SdkInternalExecutionAttribute.RESPONSE_BYTES_READ);

        BytesReadTrackingInputStream bytesReadTrackedStream =
            wrapWithBytesReadTrackingStream(
                content, bytesRead, context.progressUpdater());

        return AbortableInputStream.create(bytesReadTrackedStream);
    }
}
