package software.amazon.awssdk.metrics.internal.cloudwatch;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ServiceMetricCollectorSupportTest {

    @Test
    public void test() {
        ServiceMetricCollectorSupport s = new ServiceMetricCollectorSupport(null);
        double rate = s.bytesPerSecond(1, 1);
        assertTrue(ServiceMetricCollectorSupport.NANO_PER_SEC == rate);
        rate = s.bytesPerSecond(1, ServiceMetricCollectorSupport.NANO_PER_SEC);
        assertTrue(1.0 == rate);
        rate = s.bytesPerSecond(100, ServiceMetricCollectorSupport.NANO_PER_SEC);
        assertTrue(100.0 == rate);
    }

    @Test
    public void max() {
        ServiceMetricCollectorSupport s = new ServiceMetricCollectorSupport(null);
        final double max = Double.valueOf(Long.MAX_VALUE) * ServiceMetricCollectorSupport.NANO_PER_SEC;
        double rate = s.bytesPerSecond(Long.MAX_VALUE, 0);
        assertTrue(String.valueOf(rate), max == rate);
    }

    @Test
    public void zeros() {
        ServiceMetricCollectorSupport s = new ServiceMetricCollectorSupport(null);
        double rate = s.bytesPerSecond(0, 0);
        assertTrue(0.0 == rate);
        rate = s.bytesPerSecond(0, 100);
        assertTrue(0.0 == rate);
        rate = s.bytesPerSecond(1, 0);
        assertTrue(ServiceMetricCollectorSupport.NANO_PER_SEC == rate);
    }

    @Test(expected=IllegalArgumentException.class)
    public void negativeByteCount() {
        ServiceMetricCollectorSupport s = new ServiceMetricCollectorSupport(null);
        s.bytesPerSecond(-1, 100);
    }

    @Test(expected=IllegalArgumentException.class)
    public void negativeDuration() {
        ServiceMetricCollectorSupport s = new ServiceMetricCollectorSupport(null);
        s.bytesPerSecond(100, -1);
    }
}
