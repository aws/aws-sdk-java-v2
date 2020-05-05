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
 * A specific event
 * @param <T>
 */
public final class MetricEvent<T> {
    private static final Set<MetricEvent<?>> METRIC_EVENTS = new HashSet<>();

    private final String name;
    private final Class<T> clzz;
    private final Set<MetricCategory> categories;

    private MetricEvent(String name, Class<T> clzz, Set<MetricCategory> categories) {
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
     * @return The class of the data associated with this event.
     */
    public Class<T> dataClass() {
        return clzz;
    }

    /**
     * Cast the given object to the data class associated with this event.
     *
     * @param o The object.
     * @return The cast object.
     */
    public T convertToType(Object o) {
        return clzz.cast(o);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Only the name of the event is considered when checking equality.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MetricEvent<?> that = (MetricEvent<?>) o;

        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    public static <T> MetricEvent<T> of(String name, Class<T> clzz, MetricCategory c1, MetricCategory... cn) {
        Stream<MetricCategory> categoryStream = Stream.of(c1);
        if (cn != null) {
            categoryStream = Stream.concat(categoryStream, Stream.of(cn));
        }
        return of(name, clzz, categoryStream.collect(Collectors.toSet()));
    }

    /**
     * Create a new metric event.
     *
     * @param name The name of this metric event.
     * @param clzz The class of the object containing the associated data for this event.
     * @param categories The categories associated with this event.
     * @param <T> The type of the object containing the associated data for this event.
     * @return The created metric event.
     *
     * @throws IllegalArgumentException If a metric of the same name has already been created.
     */
    public static <T> MetricEvent<T> of(String name, Class<T> clzz, Set<MetricCategory> categories) {
        Validate.noNullElements(categories, "categories must not contain null elements");
        MetricEvent<T> event = new MetricEvent<>(name, clzz, categories);
        synchronized (METRIC_EVENTS) {
            if (METRIC_EVENTS.contains(event)) {
                throw new IllegalArgumentException("Metric with name " + name + " has already been created");
            }
            METRIC_EVENTS.add(event);
        }
        return event;
    }

    @SdkTestInternalApi
    static void clearEvents() {
        METRIC_EVENTS.clear();
    }

    @SdkTestInternalApi
    static Set<MetricEvent<?>> declaredEvents() {
        return METRIC_EVENTS;
    }
}
