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

import java.util.Set;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.metrics.internal.DefaultSdkMetric;

/**
 * A specific SDK metric.
 *
 * @param <T> The type for values of this metric.
 */
@SdkPublicApi
public interface SdkMetric<T> {

    /**
     * @return The name of this metric.
     */
    String name();

    /**
     * @return The categories of this metric.
     */
    Set<MetricCategory> categories();

    /**
     * @return The class of the value associated with this metric.
     */
    Class<T> valueClass();

    /**
     * Cast the given object to the value class associated with this event.
     *
     * @param o The object.
     * @return The cast object.
     * @throws ClassCastException If {@code o} is not an instance of type {@code
     * T}.
     */
    T convertValue(Object o);

    /**
     * Create a new metric under the {@link MetricCategory#DEFAULT} category.
     *
     * @param name The name of this metric.
     * @param clzz The class of the object containing the associated value for this metric.
     * @param <T> The type of the object containing the associated value for this metric.
     * @return The created metric.
     *
     * @throws IllegalArgumentException If a metric of the same name has already been created.
     */
    static <T> SdkMetric<T> create(String name, Class<T> clzz) {
        return DefaultSdkMetric.create(name, clzz, MetricCategory.DEFAULT);
    }

    /**
     * Create a new metric.
     *
     * @param name The name of this metric.
     * @param clzz The class of the object containing the associated value for this metric.
     * @param c1 A category associated with this metric.
     * @param cn Additional categories associated with this metric.
     * @param <T> The type of the object containing the associated value for this metric.
     * @return The created metric.
     *
     * @throws IllegalArgumentException If a metric of the same name has already been created.
     */
    static <T> SdkMetric<T> create(String name, Class<T> clzz, MetricCategory c1, MetricCategory... cn) {
        return DefaultSdkMetric.create(name, clzz, c1, cn);
    }

    /**
     * Create a new metric.
     *
     * @param name The name of this metric.
     * @param clzz The class of the object containing the associated value for this metric.
     * @param categories The categories associated with this metric.
     * @param <T> The type of the object containing the associated value for this metric.
     * @return The created metric.
     *
     * @throws IllegalArgumentException If a metric of the same name has already been created.
     */
    static <T> SdkMetric<T> create(String name, Class<T> clzz, Set<MetricCategory> categories) {
        return DefaultSdkMetric.create(name, clzz, categories);
    }
}
