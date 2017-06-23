package software.amazon.awssdk.jmx;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import software.amazon.awssdk.metrics.AwsSdkMetrics;

public class MBeansTest {

    @Test
    public void test() {
        boolean registered = AwsSdkMetrics.isMetricAdminMBeanRegistered();
        // Flip-flop 10 times, registering and unregistering
        for (int i = 0; i < 10; i++) {
            if (registered) {
                assertTrue(AwsSdkMetrics.unregisterMetricAdminMBean());
            } else {
                assertTrue(AwsSdkMetrics.registerMetricAdminMBean());
            }
            assertTrue(registered != AwsSdkMetrics.isMetricAdminMBeanRegistered());
            registered = !registered;
        }
    }

}
