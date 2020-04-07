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
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.metrics.MetricCategory;

@RunWith(MockitoJUnitRunner.class)
public class DefaultTimerTest {

    private static final long DURATION_MILLIS = 15L;

    private static final int CALLABLE_RESULT = 1234;

    @Mock
    private Clock mockClock;

    private static Timer timer;

    @Before
    public void setupCase() {
        timer = DefaultTimer.builder()
                            .clock(mockClock)
                            .addCategory(MetricCategory.DEFAULT)
                            .build();

        when(mockClock.instant()).thenReturn(Instant.ofEpochMilli(0), Instant.ofEpochMilli(DURATION_MILLIS));
    }

    @Test
    public void testRunnable() {
        AtomicBoolean flag = new AtomicBoolean(false);

        timer.record(() -> flag.set(true));

        // validates runnable is run
        assertThat(flag.get()).isTrue();

        assertThat(timer.totalTime()).isEqualTo(Duration.ofMillis(DURATION_MILLIS));
    }

    @Test
    public void testCallable() throws Exception {
        int callResult = timer.record(new MyCallable());

        assertThat(callResult).isEqualTo(CALLABLE_RESULT);

        assertThat(timer.totalTime()).isEqualTo(Duration.ofMillis(DURATION_MILLIS));
    }

    private static class MyCallable implements Callable<Integer> {
        @Override
        public Integer call() throws Exception {
            return CALLABLE_RESULT;
        }
    }
}
