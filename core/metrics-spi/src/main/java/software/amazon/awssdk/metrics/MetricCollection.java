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

package software.amazon.awssdk.metrics;

import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * An immutable collection of metrics.
 */
@SdkPublicApi
public interface MetricCollection extends Iterable<MetricRecord<?>> {
    /**
     * @return The name of this metric collection.
     */
    String name();

    /**
     * Return a stream of records in this collection.
     */
    default Stream<MetricRecord<?>> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    /**
     * Return all the values of the given metric.
     *
     * @param metric The metric.
     * @param <T> The type of the value.
     * @return All of the values of this metric.
     */
    <T> List<T> metricValues(SdkMetric<T> metric);

    /**
     * @return The child metric collections.
     */
    List<MetricCollection> children();

    /**
     * Return all of the {@link #children()} with a specific name.
     *
     * @param name The name by which we will filter {@link #children()}.
     * @return The child metric collections that have the provided name.
     */
    default Stream<MetricCollection> childrenWithName(String name) {
        return children().stream().filter(c -> c.name().equals(name));
    }

    /**
     * @return The time at which this collection was created.
     */
    Instant creationTime();
}
