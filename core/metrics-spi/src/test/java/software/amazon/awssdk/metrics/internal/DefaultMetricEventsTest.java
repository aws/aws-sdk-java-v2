package software.amazon.awssdk.metrics.internal;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.HashMap;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import software.amazon.awssdk.metrics.MetricCategory;
import software.amazon.awssdk.metrics.MetricEvent;
import software.amazon.awssdk.metrics.MetricEvents;

/**
 * Tests for {@link DefaultMetricEvents}.
 */
public class DefaultMetricEventsTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testBuilder_putMetricData_nullEvent_throws() {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage("event must not be null");
        MetricEvents.builder().putMetricEvent(null, 3);
    }

    @Test
    public void testBuilder_putMetricData_nullData_throws() {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage("eventData must not be null");
        MetricEvent<Integer> event = MetricEvent.of("foo", Integer.class, MetricCategory.DEFAULT);
        MetricEvents.builder().putMetricEvent(event, null);
    }

    @Test
    public void testBuilder_build_builtCorrectly() {
        MetricEvent<Integer> event1 = MetricEvent.of("event1", Integer.class, MetricCategory.DEFAULT);

        MetricEvents metricEvents = MetricEvents.builder()
                .putMetricEvent(event1, 1)
                .build();

        assertThat(metricEvents).isNotNull();
        assertThat(metricEvents.iterator().hasNext()).isTrue();
    }

    @Test
    public void test_iterator_noEvents_hasNextIsFalse() {
        MetricEvents metricEvents = MetricEvents.builder().build();
        assertThat(metricEvents.iterator().hasNext()).isFalse();
    }

        @Test
    public void test_iterator_iteratesAllElements() {
        MetricEvent<Integer> event1 = MetricEvent.of("event1", Integer.class, MetricCategory.DEFAULT);
        MetricEvent<Long> event2 = MetricEvent.of("event2", Long.class, MetricCategory.DEFAULT);
        MetricEvent<String> event3 = MetricEvent.of("event3", String.class, MetricCategory.DEFAULT);

        MetricEvents metricEvents = MetricEvents.builder()
                .putMetricEvent(event1, 1)
                .putMetricEvent(event2, 2L)
                .putMetricEvent(event3, "data")
                .build();

        Map<MetricEvent<?>, Object> eventToData = new HashMap<>();
        eventToData.put(event1, 1);
        eventToData.put(event2, 2L);
        eventToData.put(event3, "data");

        metricEvents.iterator()
                .forEachRemaining(e -> {
                    Object data = eventToData.remove(e.getEvent());
                    assertThat(e.getData()).isEqualTo(data);
                });

        assertThat(eventToData).isEmpty();
    }

    @Test
    public void test_getMetricData_eventExists_returnsData() {
        MetricEvent<Integer> event1 = MetricEvent.of("event1", Integer.class, MetricCategory.DEFAULT);

        MetricEvents metricEvents = MetricEvents.builder()
                .putMetricEvent(event1, 1)
                .build();

        Integer event1Data = metricEvents.getMetricEventData(event1);
        assertThat(event1Data).isEqualTo(1);
    }

    @Test
    public void test_getMetricData_eventNotExists_returnsNull() {
        MetricEvent<Integer> event1 = MetricEvent.of("event1", Integer.class, MetricCategory.DEFAULT);
        MetricEvents metricEvents = MetricEvents.builder().build();
        assertThat(metricEvents.getMetricEventData(event1)).isNull();;
    }
}
