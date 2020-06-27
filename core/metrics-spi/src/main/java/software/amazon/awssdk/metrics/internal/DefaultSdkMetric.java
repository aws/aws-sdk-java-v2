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

package software.amazon.awssdk.metrics.internal;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.metrics.MetricCategory;
import software.amazon.awssdk.metrics.MetricLevel;
import software.amazon.awssdk.metrics.SdkMetric;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
public final class DefaultSdkMetric<T> extends AttributeMap.Key<T> implements SdkMetric<T> {
    private static final ConcurrentHashMap<SdkMetric<?>, Boolean> SDK_METRICS = new ConcurrentHashMap<>();

    private final String name;
    private final Class<T> clzz;
    private final Set<MetricCategory> categories;
    private final MetricLevel level;

    private DefaultSdkMetric(String name, Class<T> clzz, MetricLevel level, Set<MetricCategory> categories) {
        super(clzz);
        this.name = Validate.notBlank(name, "name must not be blank");
        this.clzz = Validate.notNull(clzz, "clzz must not be null");
        this.level = Validate.notNull(level, "level must not be null");
        Validate.notEmpty(categories, "categories must not be empty");
        this.categories = EnumSet.copyOf(categories);
    }

    /**
     * @return The name of this event.
     */
    @Override
    public String name() {
        return name;
    }

    /**
     * @return The categories of this event.
     */
    @Override
    public Set<MetricCategory> categories() {
        return Collections.unmodifiableSet(categories);
    }

    @Override
    public MetricLevel level() {
        return level;
    }

    /**
     * @return The class of the value associated with this event.
     */
    @Override
    public Class<T> valueClass() {
        return clzz;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DefaultSdkMetric<?> that = (DefaultSdkMetric<?>) o;

        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return ToString.builder("DefaultMetric")
                       .add("name", name)
                       .add("categories", categories())
                       .build();
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
    public static <T> SdkMetric<T> create(String name, Class<T> clzz, MetricLevel level,
                                          MetricCategory c1, MetricCategory... cn) {
        Stream<MetricCategory> categoryStream = Stream.of(c1);
        if (cn != null) {
            categoryStream = Stream.concat(categoryStream, Stream.of(cn));
        }
        Set<MetricCategory> categories = categoryStream.collect(Collectors.toSet());
        return create(name, clzz, level, categories);
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
    public static <T> SdkMetric<T> create(String name, Class<T> clzz, MetricLevel level, Set<MetricCategory> categories) {
        Validate.noNullElements(categories, "categories must not contain null elements");
        SdkMetric<T> event = new DefaultSdkMetric<>(name, clzz, level, categories);
        if (SDK_METRICS.putIfAbsent(event, Boolean.TRUE) != null) {
            throw new IllegalArgumentException("Metric with name " + name + " has already been created");
        }
        return event;
    }

    @SdkTestInternalApi
    static void clearDeclaredMetrics() {
        SDK_METRICS.clear();
    }

    @SdkTestInternalApi
    static Set<SdkMetric<?>> declaredEvents() {
        return SDK_METRICS.keySet();
    }
}
