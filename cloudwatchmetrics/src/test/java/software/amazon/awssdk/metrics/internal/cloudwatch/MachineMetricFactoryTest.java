package software.amazon.awssdk.metrics.internal.cloudwatch;

import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;

public class MachineMetricFactoryTest {
    @Test
    public void test() throws Exception {
        List<MetricDatum> list = new MachineMetricFactory().generateMetrics();
        for (MetricDatum d: list)
            p(d);
        int expectedCount = 0;
        for (MachineMetric m: MachineMetric.values()) {
            if (m.includeZeroValue())
                expectedCount++;
        }
        Assert.assertTrue(
            // zero deamon threads
            list.size() == expectedCount   
            // non-zero deamon threads
            || list.size() == expectedCount+1);
    }
    public static void p(Object o) {
        System.out.println(String.valueOf(o));
    }
}
