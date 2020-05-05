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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.metrics.MetricEvent;
import software.amazon.awssdk.metrics.MetricEventRecord;
import software.amazon.awssdk.metrics.MetricEvents;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
public final class DefaultMetricEvents implements MetricEvents {
    private final Map<MetricEvent<?>, MetricEventRecord<?>> events;

    private DefaultMetricEvents(Map<MetricEvent<?>,  MetricEventRecord<?>> events) {
        this.events = Collections.unmodifiableMap(new LinkedHashMap<>(events));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getMetricEventData(MetricEvent<T> event) {
        MetricEventRecord<?> record = events.get(event);
        if (record != null) {
            return (T) record.getData();
        }
        return null;
    }

    @Override
    public Iterator<MetricEventRecord<?>> iterator() {
        return events.values().iterator();
    }

    public static class Builder implements MetricEvents.Builder {
        private Map<MetricEvent<?>, MetricEventRecord<?>> events = new LinkedHashMap<>();

        @Override
        public <T> Builder putMetricEvent(MetricEvent<T> event, T eventData) {
            Validate.notNull(event, "event must not be null");
            Validate.notNull(eventData, "eventData must not be null");
            events.put(event, new DefaultMetricEventRecord<>(event, eventData));
            return this;
        }

        @Override
        public MetricEvents build() {
            return new DefaultMetricEvents(events);
        }
    }
}
