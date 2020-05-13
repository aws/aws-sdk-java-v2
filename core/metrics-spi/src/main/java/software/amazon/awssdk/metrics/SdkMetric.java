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

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.utils.Validate;

/**
 * A specific SDK metric
 * @param <T>
 */
public final class SdkMetric<T> {
    private static final Set<SdkMetric<?>> SDK_METRICS = new HashSet<>();

    private final String name;
    private final Class<T> clzz;
    private final Set<MetricCategory> categories;

    private SdkMetric(String name, Class<T> clzz, Set<MetricCategory> categories) {
        this.name = Validate.notBlank(name, "name must not be blank");
        this.clzz = Validate.notNull(clzz, "clzz must not be null");
        this.categories = Validate.notEmpty(EnumSet.copyOf(categories), "categories must not be empty");
    }

    /**
     * @return The name of this event.
     */
    public String name() {
        return name;
    }

    /**
     * @return The categories of this event.
     */
    public Set<MetricCategory> categories() {
        return Collections.unmodifiableSet(categories);
    }

    /**
     * @return The class of the value associated with this event.
     */
    public Class<T> valueClass() {
        return clzz;
    }

    /**
     * Cast the given object to the value class associated with this event.
     *
     * @param o The object.
     * @return The cast object.
     */
    public T convertToType(Object o) {
        return clzz.cast(o);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SdkMetric<?> sdkMetric = (SdkMetric<?>) o;

        if (!name.equals(sdkMetric.name)) return false;
        return clzz.equals(sdkMetric.clzz);
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + clzz.hashCode();
        return result;
    }

    public static <T> SdkMetric<T> of(String name, Class<T> clzz, MetricCategory c1, MetricCategory... cn) {
        return of(name, clzz, Stream.concat(Stream.of(c1), Stream.of(cn)).collect(Collectors.toSet()));
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
    public static <T> SdkMetric<T> of(String name, Class<T> clzz, Set<MetricCategory> categories) {
        Validate.noNullElements(categories, "categories must not contain null elements");
        SdkMetric<T> event = new SdkMetric<>(name, clzz, categories);
        synchronized (SDK_METRICS) {
            if (SDK_METRICS.contains(event)) {
                throw new IllegalArgumentException("Metric with name " + name + " has already been created");
            }
            SDK_METRICS.add(event);
        }
        return event;
    }

    @SdkTestInternalApi
    static void clearDeclaredMetrics() {
        SDK_METRICS.clear();
    }

    @SdkTestInternalApi
    static Set<SdkMetric<?>> declaredEvents() {
        return SDK_METRICS;
    }
}
