/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.metrics.internal.cloudwatch;

import static software.amazon.awssdk.metrics.internal.cloudwatch.spi.MetricData.newMetricDatum;
import static software.amazon.awssdk.metrics.internal.cloudwatch.spi.RequestMetricTransformer.Utils.endTimestamp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import software.amazon.awssdk.AmazonWebServiceRequest;
import software.amazon.awssdk.Request;
import software.amazon.awssdk.annotation.ThreadSafe;
import software.amazon.awssdk.metrics.RequestMetricCollector;
import software.amazon.awssdk.metrics.internal.cloudwatch.spi.AwsMetricTransformerFactory;
import software.amazon.awssdk.metrics.internal.cloudwatch.spi.Dimensions;
import software.amazon.awssdk.metrics.spi.AwsRequestMetrics;
import software.amazon.awssdk.metrics.spi.AwsRequestMetrics.Field;
import software.amazon.awssdk.metrics.spi.MetricType;
import software.amazon.awssdk.metrics.spi.TimingInfo;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;

/**
 * Used to transform the predefined metrics of the AWS SDK into instances of
 * {@link MetricDatum}.
 *
 * See <a href=
 * "http://docs.aws.amazon.com/AmazonCloudWatch/latest/DeveloperGuide/publishingMetrics.html"
 * >http://docs.aws.amazon.com/AmazonCloudWatch/latest/DeveloperGuide/
 * publishingMetrics.html</a>
 *
 * @see AwsRequestMetrics
 * @see RequestMetricCollector
 */
@ThreadSafe
public class PredefinedMetricTransformer {
    static final boolean INCLUDE_REQUEST_TYPE = true;
    static final boolean EXCLUDE_REQUEST_TYPE = !INCLUDE_REQUEST_TYPE;
    private static final Log log = LogFactory.getLog(PredefinedMetricTransformer.class);

    /**
     * Returns a non-null list of metric datum for the metrics collected for the
     * given request/response.
     *
     * @param metricType the request metric type
     */
    public List<MetricDatum> toMetricData(MetricType metricType, Request<?> request, Object response) {
        if (metricType instanceof Field) {
            // Predefined metrics across all AWS http clients
            Field predefined = (Field) metricType;
            switch (predefined) {
                case HttpClientRetryCount:
                case HttpClientPoolAvailableCount:
                case HttpClientPoolLeasedCount:
                case HttpClientPoolPendingCount:
                    return metricOfCount(predefined, request);
                case RequestCount:  // intentionally fall through to reuse the same routine as RetryCount
                case RetryCount:
                    return metricOfRequestOrRetryCount(predefined, request);
                case ThrottledRetryCount: // drop through
                case RetryCapacityConsumed:
                    return counterMetricOf(predefined, request, EXCLUDE_REQUEST_TYPE);
                case ResponseProcessingTime: // drop through
                case RequestSigningTime: // drop through
                    return latencyMetricOf(predefined, request, EXCLUDE_REQUEST_TYPE);
                case ClientExecuteTime:
                    return latencyOfClientExecuteTime(request);
                case HttpClientSendRequestTime:
                case HttpClientReceiveResponseTime:
                case HttpRequestTime:
                case HttpSocketReadTime:
                    return latencyMetricOf(predefined, request, INCLUDE_REQUEST_TYPE);
                case Exception:
                case ThrottleException:
                    return counterMetricOf(predefined, request, INCLUDE_REQUEST_TYPE);
                default:
                    break;
            }
        }
        // Predefined metrics for specific service clients
        for (AwsMetricTransformerFactory aws : AwsMetricTransformerFactory.values()) {
            if (metricType.name().startsWith(aws.name())) {
                List<MetricDatum> metricData = aws.getRequestMetricTransformer().toMetricData(metricType, request, response);
                if (metricData != null) {
                    return metricData;
                }
                break;
            }
        }
        if (log.isDebugEnabled()) {
            AmazonWebServiceRequest origReq = request == null ? null : request
                    .getOriginalRequest();
            String reqClassName = origReq == null ? null : origReq.getClass().getName();
            log.debug("No request metric transformer can be found for metric type "
                      + metricType.name() + " for " + reqClassName);
        }
        return Collections.emptyList();
    }

    /**
     * Returns a list with a single metric datum for the specified retry or
     * request count predefined metric; or an empty list if there is none.
     *
     * @param metricType
     *            must be either {@link Field#RequestCount} or
     *            {@link Field#RetryCount}; or else GIGO.
     */
    protected List<MetricDatum> metricOfRequestOrRetryCount(Field metricType, Request<?> req) {
        AwsRequestMetrics m = req.getAwsRequestMetrics();
        TimingInfo ti = m.getTimingInfo();
        // Always retrieve the request count even for retry which is equivalent
        // to the number of requests minus one.
        Number counter = ti.getCounter(Field.RequestCount.name());
        if (counter == null) {
            // this is possible if one of the request handlers screwed up
            return Collections.emptyList();
        }
        int requestCount = counter.intValue();
        if (requestCount < 1) {
            LogFactory.getLog(getClass()).debug(
                    "request count must be at least one");
            return Collections.emptyList();
        }
        final double count = metricType == Field.RequestCount
                             ? requestCount
                             : requestCount - 1 // retryCount = requestCount - 1
                ;
        if (count < 1) {
            return Collections.emptyList();
        } else {
            return Collections.singletonList(MetricDatum.builder()
                    .metricName(req.getServiceName())
                    .dimensions(Dimension.builder()
                            .name(Dimensions.MetricType.name())
                            .value(metricType.name())
                            .build())
                    .unit(StandardUnit.Count)
                    .value(count)
                    .timestamp(endTimestamp(ti)).build());
        }
    }

    protected List<MetricDatum> metricOfCount(Field metricType, Request<?> req) {
        AwsRequestMetrics m = req.getAwsRequestMetrics();
        TimingInfo ti = m.getTimingInfo();
        Number counter = ti.getCounter(metricType.name());
        if (counter == null) {
            return Collections.emptyList();
        }
        final double count = counter.doubleValue();
        if (count < 1) {
            return Collections.emptyList();
        } else {
            return Collections.singletonList(MetricDatum.builder()
                    .metricName(req.getServiceName())
                    .dimensions(Dimension.builder()
                            .name(Dimensions.MetricType.name())
                            .value(metricType.name())
                            .build())
                    .unit(StandardUnit.Count)
                    .value(count)
                    .timestamp(endTimestamp(ti))
                    .build());
        }
    }

    /**
     * Returns all the latency metric data recorded for the specified metric
     * event type; or an empty list if there is none. The number of metric datum
     * in the returned list should be exactly one when there is no retries, or
     * more than one when there are retries.
     *
     * @param includesRequestType
     *            true iff the "request" dimension is to be included;
     */
    protected List<MetricDatum> latencyMetricOf(MetricType metricType, Request<?> req, boolean includesRequestType) {
        AwsRequestMetrics m = req.getAwsRequestMetrics();
        TimingInfo root = m.getTimingInfo();
        final String metricName = metricType.name();
        List<TimingInfo> subMeasures =
                root.getAllSubMeasurements(metricName);
        if (subMeasures != null) {
            List<MetricDatum> result =
                    new ArrayList<>(subMeasures.size());
            for (TimingInfo sub : subMeasures) {
                if (sub.isEndTimeKnown()) { // being defensive
                    List<Dimension> dims = new ArrayList<>();
                    dims.add(Dimension.builder()
                            .name(Dimensions.MetricType.name())
                            .value(metricName)
                            .build());
                    // Either a non request type specific datum is created per
                    // sub-measurement, or a request type specific one is
                    // created but not both
                    if (includesRequestType) {
                        dims.add(Dimension.builder()
                                .name(Dimensions.RequestType.name())
                                .value(requestType(req))
                                .build());
                    }
                    MetricDatum datum = MetricDatum.builder()
                            .metricName(req.getServiceName())
                            .dimensions(dims)
                            .unit(StandardUnit.Milliseconds)
                            .value(sub.getTimeTakenMillisIfKnown())
                            .build();
                    result.add(datum);
                }
            }
            return result;
        }
        return Collections.emptyList();
    }

    /**
     * Returns a request type specific metrics for
     * {@link Field#ClientExecuteTime} which is special in the sense that it
     * makes a more accurate measurement by taking the {@link TimingInfo} at the
     * root into account.
     */
    protected List<MetricDatum> latencyOfClientExecuteTime(Request<?> req) {
        AwsRequestMetrics m = req.getAwsRequestMetrics();
        TimingInfo root = m.getTimingInfo();
        final String metricName = Field.ClientExecuteTime.name();
        if (root.isEndTimeKnown()) { // being defensive
            List<Dimension> dims = new ArrayList<>();
            dims.add(Dimension.builder()
                             .name(Dimensions.MetricType.name())
                             .value(metricName)
                    .build());
            // request type specific
            dims.add(Dimension.builder()
                             .name(Dimensions.RequestType.name())
                             .value(requestType(req))
                    .build());
            MetricDatum datum = MetricDatum.builder()
                    .metricName(req.getServiceName())
                    .dimensions(dims)
                    .unit(StandardUnit.Milliseconds)
                    .value(root.getTimeTakenMillisIfKnown())
                    .build();
            return Collections.singletonList(datum);
        }
        return Collections.emptyList();
    }

    /**
     * Returns the name of the type of request.
     */
    private String requestType(Request<?> req) {
        return req.getOriginalRequest().getClass().getSimpleName();
    }

    /**
     * Returns a list of metric datum recorded for the specified counter metric
     * type; or an empty list if there is none.
     *
     * @param includesRequestType
     *            true iff an additional metric datum is to be created that
     *            includes the "request" dimension
     */
    protected List<MetricDatum> counterMetricOf(MetricType type, Request<?> req, boolean includesRequestType) {
        AwsRequestMetrics m = req.getAwsRequestMetrics();
        TimingInfo ti = m.getTimingInfo();
        final String metricName = type.name();
        Number counter = ti.getCounter(metricName);
        if (counter == null) {
            return Collections.emptyList();
        }
        int count = counter.intValue();
        if (count < 1) {
            LogFactory.getLog(getClass()).debug("Count must be at least one");
            return Collections.emptyList();
        }
        final List<MetricDatum> result = new ArrayList<MetricDatum>();
        final Dimension metricDimension = Dimension.builder()
                .name(Dimensions.MetricType.name())
                .value(metricName)
                .build();
        // non-request type specific metric datum
        final MetricDatum first = MetricDatum.builder()
                .metricName(req.getServiceName())
                .dimensions(metricDimension)
                .unit(StandardUnit.Count)
                .value((double) count)
                .timestamp(endTimestamp(ti))
                .build();
        result.add(first);
        if (includesRequestType) {
            // additional request type specific metric datum
            Dimension requestDimension = Dimension.builder()
                    .name(Dimensions.RequestType.name())
                    .value(requestType(req))
                    .build();
            final MetricDatum second =
                    newMetricDatum(first, metricDimension, requestDimension);
            result.add(second);
        }
        return result;
    }
}
