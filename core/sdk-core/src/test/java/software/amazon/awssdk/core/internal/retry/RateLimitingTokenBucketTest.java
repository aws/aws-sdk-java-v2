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

package software.amazon.awssdk.core.internal.retry;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.mockito.Mockito;

public class RateLimitingTokenBucketTest {

    @Test
    public void acquire_notEnabled_returnsTrue() {
        RateLimitingTokenBucket tb = new RateLimitingTokenBucket();
        assertThat(tb.acquire(0.0)).isTrue();
    }

    @Test
    public void acquire_capacitySufficient_returnsImmediately() {
        RateLimitingTokenBucket tb = Mockito.spy(new RateLimitingTokenBucket());

        // stub out refill() so we have control over the capacity
        Mockito.doAnswer(invocationOnMock -> null).when(tb).refill();

        tb.setFillRate(0.5);
        tb.setCurrentCapacity(1000.0);
        tb.enable();

        long a = System.nanoTime();
        boolean acquired = tb.acquire(1000.0);
        long elapsed = System.nanoTime() - a;

        assertThat(acquired).isTrue();
        assertThat(TimeUnit.NANOSECONDS.toMillis(elapsed)).isLessThan(3L);
    }

    @Test
    public void acquire_capacityInsufficient_sleepsForRequiredTime() {
        RateLimitingTokenBucket tb = Mockito.spy(new RateLimitingTokenBucket());

        // stub out refill() so we have control over the capacity
        Mockito.doAnswer(invocationOnMock -> null).when(tb).refill();

        tb.setFillRate(1.0);
        tb.setCurrentCapacity(0.0);
        tb.enable();

        // 1 token to wait for at a rate of 1 per second should sleep for approx 1s
        long a = System.nanoTime();
        boolean acquired = tb.acquire(1);
        long elapsed = System.nanoTime() - a;

        assertThat(acquired).isTrue();
        assertThat(Duration.ofNanos(elapsed).getSeconds()).isEqualTo(1);
    }

    @Test
    public void acquire_capacityInsufficient_fastFailEnabled_doesNotSleep() {
        RateLimitingTokenBucket tb = Mockito.spy(new RateLimitingTokenBucket());

        // stub out refill() so we have control over the capacity
        Mockito.doAnswer(invocationOnMock -> null).when(tb).refill();

        tb.setFillRate(1.0);
        tb.setCurrentCapacity(4.0);
        tb.enable();

        long a = System.nanoTime();
        boolean acquired = tb.acquire(5, true);
        long elapsed = System.nanoTime() - a;

        assertThat(acquired).isFalse();
        assertThat(tb.getCurrentCapacity()).isEqualTo(4.0);
        // The method call should be nowhere near a millisecond
        assertThat(Duration.ofNanos(elapsed).getSeconds()).isZero();
    }

    @Test
    public void tryAcquireCapacity_capacitySufficient_returns0() {
        RateLimitingTokenBucket tb = new RateLimitingTokenBucket();
        tb.setCurrentCapacity(5.0);

        assertThat(tb.tryAcquireCapacity(5.0)).isZero();
    }

    @Test
    public void tryAcquireCapacity_capacityInsufficient_returnsDifference() {
        RateLimitingTokenBucket tb = new RateLimitingTokenBucket();
        tb.setCurrentCapacity(3.0);

        assertThat(tb.tryAcquireCapacity(5.0)).isEqualTo(2.0);
    }
}