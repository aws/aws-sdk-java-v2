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

package software.amazon.awssdk.core.http;

import java.time.Duration;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.internal.util.MetricUtils;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.metrics.SdkMetric;
import software.amazon.awssdk.utils.Pair;

/**
 * An implementation of {@link HttpResponseHandler} that publishes the time it took to execute {
 * @link #handle(SdkHttpFullResponse, ExecutionAttributes)} as the provieded duration metric to the
 * {@link SdkExecutionAttribute#API_CALL_ATTEMPT_METRIC_COLLECTOR}.
 */
@SdkProtectedApi
public final class MetricCollectingHttpResponseHandler<T> implements HttpResponseHandler<T> {
    public final SdkMetric<? super Duration> metric;
    public final HttpResponseHandler<T> delegateToTime;

    private MetricCollectingHttpResponseHandler(SdkMetric<? super Duration> durationMetric,
                                                HttpResponseHandler<T> delegateToTime) {
        this.metric = durationMetric;
        this.delegateToTime = delegateToTime;
    }

    public static <T> MetricCollectingHttpResponseHandler<T> create(SdkMetric<? super Duration> durationMetric,
                                                                    HttpResponseHandler<T> delegateToTime) {
        return new MetricCollectingHttpResponseHandler<>(durationMetric, delegateToTime);
    }

    @Override
    public T handle(SdkHttpFullResponse response, ExecutionAttributes executionAttributes) throws Exception {
        Pair<T, Duration> result = MetricUtils.measureDurationUnsafe(() -> delegateToTime.handle(response, executionAttributes));

        collector(executionAttributes).ifPresent(c -> c.reportMetric(metric, result.right()));

        return result.left();
    }

    private Optional<MetricCollector> collector(ExecutionAttributes attributes) {
        if (attributes == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(attributes.getAttribute(SdkExecutionAttribute.API_CALL_ATTEMPT_METRIC_COLLECTOR));
    }

    @Override
    public boolean needsConnectionLeftOpen() {
        return delegateToTime.needsConnectionLeftOpen();
    }
}
