package software.amazon.awssdk.metrics.internal.cloudwatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import org.junit.Test;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;
import software.amazon.awssdk.services.cloudwatch.model.StatisticSet;

public class BlockingRequestBuilderTest {

    @Test
    public void cloneMetricDatum() {
        BlockingRequestBuilder b = new BlockingRequestBuilder(new CloudWatchMetricConfig(), null);
        Collection<Dimension> dimensions = Collections.emptyList();
        MetricDatum md = MetricDatum.builder().dimensions(dimensions).build();
        assertNotSame("Expect a new collection to be created", md.dimensions(), dimensions);
        assertTrue(0 == md.dimensions().size());
        md = md.toBuilder().dimensions(Dimension.builder().name("Name1").value("Value1").build()).build();
        assertTrue(1 == md.dimensions().size());
        md = md.toBuilder().dimensions(Dimension.builder().name("Name2").value("Value2").build()).build();
        assertTrue(2 == md.dimensions().size());
        md = md.toBuilder()
                .metricName("MetricName")
                .statisticValues(StatisticSet.builder().maximum(100.0)
                .minimum(10.0).sampleCount(12.34).sum(99.9).build())
                .timestamp(new Date())
                .unit(StandardUnit.Milliseconds)
                .value(56.78)
                .build();

        MetricDatum md2 = b.cloneMetricDatum(md);

        assertNotSame(md.dimensions(), md2.dimensions());
        assertTrue(Arrays.equals(md.dimensions().toArray(), md2.dimensions().toArray()));
        assertEquals(md.metricName(), md2.metricName());
        assertEquals(md.statisticValues(), md2.statisticValues());
        assertEquals(md.timestamp(), md2.timestamp());
        assertEquals(md.unit(), md2.unit());
        assertEquals(md.value(), md2.value());
    }

}
