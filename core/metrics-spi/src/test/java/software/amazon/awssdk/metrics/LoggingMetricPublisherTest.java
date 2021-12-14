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

import static org.apache.log4j.Level.ALL;
import static org.apache.log4j.Level.DEBUG;
import static org.apache.log4j.Level.INFO;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.jupiter.api.Test;
import org.slf4j.event.Level;
import software.amazon.awssdk.metrics.LoggingMetricPublisher.Format;
import software.amazon.awssdk.metrics.internal.DefaultMetricCollection;
import software.amazon.awssdk.metrics.internal.DefaultMetricRecord;
import software.amazon.awssdk.testutils.LogCaptor;

class LoggingMetricPublisherTest {
    private static final SdkMetric<Integer> TEST_METRIC =
        SdkMetric.create("LoggingMetricPublisherTest", Integer.class, MetricLevel.INFO, MetricCategory.CORE);

    @Test
    void testDefaultConfiguration() {
        MetricCollection qux = metrics("qux");
        MetricCollection baz = metrics("baz");
        MetricCollection bar = metrics("bar", baz);
        MetricCollection foo = metrics("foo", bar, qux);

        LoggingMetricPublisher publisher = LoggingMetricPublisher.create();
        publisher.publish(foo);

        try (LogCaptor logCaptor = new LogCaptor.DefaultLogCaptor(ALL)) {
            publisher.publish(foo);
            List<LoggingEvent> events = logCaptor.loggedEvents();
            assertLogged(events, INFO, "Metrics published: %s", foo);
            assertThat(events).isEmpty();
        }
    }

    @Test
    void testPrettyFormat() {
        MetricCollection qux = metrics("qux");
        MetricCollection baz = metrics("baz");
        MetricCollection bar = metrics("bar", baz);
        MetricCollection foo = metrics("foo", bar, qux);
        String guid = Integer.toHexString(foo.hashCode());

        LoggingMetricPublisher publisher = LoggingMetricPublisher.create(Level.DEBUG, Format.PRETTY);
        publisher.publish(foo);

        try (LogCaptor logCaptor = new LogCaptor.DefaultLogCaptor(ALL)) {
            publisher.publish(foo);
            List<LoggingEvent> events = logCaptor.loggedEvents();
            assertLogged(events, DEBUG, "[%s] foo", guid);
            assertLogged(events, DEBUG, "[%s] ┌──────────────────────────────┐", guid);
            assertLogged(events, DEBUG, "[%s] │ LoggingMetricPublisherTest=1 │", guid);
            assertLogged(events, DEBUG, "[%s] │ LoggingMetricPublisherTest=2 │", guid);
            assertLogged(events, DEBUG, "[%s] │ LoggingMetricPublisherTest=3 │", guid);
            assertLogged(events, DEBUG, "[%s] └──────────────────────────────┘", guid);
            assertLogged(events, DEBUG, "[%s]     bar", guid);
            assertLogged(events, DEBUG, "[%s]     ┌──────────────────────────────┐", guid);
            assertLogged(events, DEBUG, "[%s]     │ LoggingMetricPublisherTest=1 │", guid);
            assertLogged(events, DEBUG, "[%s]     │ LoggingMetricPublisherTest=2 │", guid);
            assertLogged(events, DEBUG, "[%s]     │ LoggingMetricPublisherTest=3 │", guid);
            assertLogged(events, DEBUG, "[%s]     └──────────────────────────────┘", guid);
            assertLogged(events, DEBUG, "[%s]         baz", guid);
            assertLogged(events, DEBUG, "[%s]         ┌──────────────────────────────┐", guid);
            assertLogged(events, DEBUG, "[%s]         │ LoggingMetricPublisherTest=1 │", guid);
            assertLogged(events, DEBUG, "[%s]         │ LoggingMetricPublisherTest=2 │", guid);
            assertLogged(events, DEBUG, "[%s]         │ LoggingMetricPublisherTest=3 │", guid);
            assertLogged(events, DEBUG, "[%s]         └──────────────────────────────┘", guid);
            assertLogged(events, DEBUG, "[%s]     qux", guid);
            assertLogged(events, DEBUG, "[%s]     ┌──────────────────────────────┐", guid);
            assertLogged(events, DEBUG, "[%s]     │ LoggingMetricPublisherTest=1 │", guid);
            assertLogged(events, DEBUG, "[%s]     │ LoggingMetricPublisherTest=2 │", guid);
            assertLogged(events, DEBUG, "[%s]     │ LoggingMetricPublisherTest=3 │", guid);
            assertLogged(events, DEBUG, "[%s]     └──────────────────────────────┘", guid);
            assertThat(events).isEmpty();
        }
    }

    private static MetricCollection metrics(String name, MetricCollection... children) {
        Integer[] values = {1, 2, 3};
        Map<SdkMetric<?>, List<MetricRecord<?>>> recordMap = new HashMap<>();
        List<MetricRecord<?>> records =
            Stream.of(values).map(v -> new DefaultMetricRecord<>(TEST_METRIC, v)).collect(Collectors.toList());
        recordMap.put(TEST_METRIC, records);
        return new DefaultMetricCollection(name, recordMap, Arrays.asList(children));
    }

    private static void assertLogged(List<LoggingEvent> events, org.apache.log4j.Level level, String message, Object... args) {
        LoggingEvent event = events.remove(0);
        assertThat(event.getLevel()).isEqualTo(level);
        assertThat(event.getMessage()).isEqualTo(String.format(message, args));
    }
}