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

import static org.apache.logging.log4j.Level.DEBUG;
import static org.apache.logging.log4j.Level.INFO;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.core.LogEvent;
import org.junit.jupiter.api.Test;
import org.slf4j.event.Level;
import software.amazon.awssdk.metrics.LoggingMetricPublisher.Format;
import software.amazon.awssdk.metrics.internal.DefaultMetricCollection;
import software.amazon.awssdk.metrics.internal.DefaultMetricRecord;
import software.amazon.awssdk.testutils.LogCaptor;

class LoggingMetricPublisherTest {
    private static final SdkMetric<Integer> TEST_METRIC =
        SdkMetric.create("LoggingMetricPublisherTest", Integer.class, MetricLevel.INFO, MetricCategory.CORE);
    private static final SdkMetric<String> STRING_METRIC =
        SdkMetric.create("Metric\nName", String.class, MetricLevel.INFO, MetricCategory.CORE);

    @Test
    void testDefaultConfiguration() {
        MetricCollection qux = metrics("qux");
        MetricCollection baz = metrics("baz");
        MetricCollection bar = metrics("bar", baz);
        MetricCollection foo = metrics("foo", bar, qux);

        LoggingMetricPublisher publisher = LoggingMetricPublisher.create();

        try (LogCaptor logCaptor = LogCaptor.create()) {
            publisher.publish(foo);
            List<LogEvent> events = logCaptor.loggedEvents();
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

        try (LogCaptor logCaptor = LogCaptor.create()) {
            publisher.publish(foo);
            List<LogEvent> events = logCaptor.loggedEvents();
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

    @Test
    void formats_withControlCharacters_escapesNamesAndValues() {
        MetricCollection collection = stringMetrics("Collection\rName", "value\nFORGED\u2028");

        try (LogCaptor logCaptor = LogCaptor.create()) {
            LoggingMetricPublisher.create().publish(collection);
            List<LogEvent> events = logCaptor.loggedEvents();
            assertThat(events).hasSize(1);
            assertNoControlCharacters(events.get(0).getMessage().getFormattedMessage());
            assertThat(events.get(0).getMessage().getFormattedMessage())
                .contains("name=Collection\\rName")
                .contains("metric=Metric\\nName")
                .contains("value=value\\nFORGED\\u2028");
        }

        try (LogCaptor logCaptor = LogCaptor.create()) {
            LoggingMetricPublisher.create(Level.DEBUG, Format.PRETTY).publish(collection);
            List<LogEvent> events = logCaptor.loggedEvents();
            assertThat(events).hasSize(4);
            events.forEach(event -> assertNoControlCharacters(event.getMessage().getFormattedMessage()));
            assertThat(events.get(0).getMessage().getFormattedMessage()).contains("Collection\\rName");
            assertThat(events.get(2).getMessage().getFormattedMessage())
                .contains("Metric\\nName=value\\nFORGED\\u2028");
        }
    }

    private static void assertNoControlCharacters(String message) {
        assertThat(message.chars()).noneMatch(c -> Character.isISOControl(c) || c == '\u2028' || c == '\u2029');
    }

    private static MetricCollection stringMetrics(String name, String value) {
        Map<SdkMetric<?>, List<MetricRecord<?>>> recordMap = new HashMap<>();
        recordMap.put(STRING_METRIC, Arrays.asList(new DefaultMetricRecord<>(STRING_METRIC, value)));
        return new DefaultMetricCollection(name, recordMap, Collections.emptyList());
    }

    private static MetricCollection metrics(String name, MetricCollection... children) {
        Integer[] values = {1, 2, 3};
        Map<SdkMetric<?>, List<MetricRecord<?>>> recordMap = new HashMap<>();
        List<MetricRecord<?>> records =
            Stream.of(values).map(v -> new DefaultMetricRecord<>(TEST_METRIC, v)).collect(Collectors.toList());
        recordMap.put(TEST_METRIC, records);
        return new DefaultMetricCollection(name, recordMap, Arrays.asList(children));
    }

    private static void assertLogged(List<LogEvent> events, org.apache.logging.log4j.Level level, String message, Object... args) {
        assertThat(events).withFailMessage("Expecting events to not be empty").isNotEmpty();
        LogEvent event = events.remove(0);
        String msg = event.getMessage().getFormattedMessage();
        assertThat(msg).isEqualTo(String.format(message, args));
        assertThat(event.getLevel()).isEqualTo(level);
    }
}