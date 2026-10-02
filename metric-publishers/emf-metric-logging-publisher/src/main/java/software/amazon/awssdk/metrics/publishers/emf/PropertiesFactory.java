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

package software.amazon.awssdk.metrics.publishers.emf;

import java.util.Map;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.metrics.MetricCollection;

/**
 * A factory for producing custom properties to include in each EMF record.
 *
 * <p>Implementations receive the {@link MetricCollection} being published, allowing properties
 * to be derived from the SDK metrics (e.g. service endpoint, request ID) or from ambient
 * context (e.g. Lambda request ID, trace ID).
 *
 * <p>The returned map entries are written as top-level key-value pairs in the EMF JSON output,
 * making them searchable in CloudWatch Logs Insights. Keys that collide with reserved EMF
 * fields ({@code _aws}), dimension names, or metric names are silently skipped.
 *
 * <p>If the factory returns {@code null} or throws an exception, no custom properties are added
 * and a warning is logged.
 *
 * <p>Example using ambient context:
 * <pre>{@code
 * EmfMetricLoggingPublisher.builder()
 *     .propertiesFactory(metrics -> Collections.singletonMap("RequestId", requestId))
 *     .build();
 * }</pre>
 *
 * <p>Example using metric collection values:
 * <pre>{@code
 * EmfMetricLoggingPublisher.builder()
 *     .propertiesFactory(metrics -> {
 *         Map<String, String> props = new HashMap<>();
 *         metrics.metricValues(CoreMetric.SERVICE_ENDPOINT)
 *                .stream().findFirst()
 *                .ifPresent(uri -> props.put("ServiceEndpoint", uri.toString()));
 *         return props;
 *     })
 *     .build();
 * }</pre>
 *
 * @see EmfMetricLoggingPublisher.Builder#propertiesFactory(PropertiesFactory)
 */
@FunctionalInterface
@SdkPublicApi
public interface PropertiesFactory {

    /**
     * Create a map of custom properties to include in the EMF record for the given metric collection.
     *
     * @param metricCollection the SDK metric collection being published
     * @return a map of property names to string values, or {@code null} for no custom properties
     */
    Map<String, String> create(MetricCollection metricCollection);
}
