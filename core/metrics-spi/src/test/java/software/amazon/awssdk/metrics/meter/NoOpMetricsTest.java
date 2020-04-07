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

package software.amazon.awssdk.metrics.meter;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;

public class NoOpMetricsTest {

    @Test
    public void testCounter() {
        Counter counter = NoOpCounter.instance();

        counter.increment();
        counter.increment(5);
        counter.decrement(1);

        assertThat(counter.count()).isEqualTo(0);
    }

    @Test
    public void noopGauge_always_return_sameObjectInstance() {
        assertThat(NoOpGauge.instance().value())
            .isEqualTo(NoOpGauge.instance().value());
    }

    @Test
    public void noopTimer_runnable_returnsZeroDuration() {
        AtomicBoolean flag = new AtomicBoolean(false);
        Timer timer = NoOpTimer.instance();
        timer.record(() -> flag.set(true));

        assertThat(flag.get()).isTrue();
        assertThat(timer.totalTime()).isEqualTo(Duration.ZERO);
    }

    @Test
    public void noopTimer_startAndEndMethods() throws Exception {
        Timer timer = NoOpTimer.instance();
        timer.start();
        Thread.sleep(Duration.ofSeconds(1).toMillis());
        timer.end();

        assertThat(timer.totalTime()).isEqualTo(Duration.ZERO);
    }
}
