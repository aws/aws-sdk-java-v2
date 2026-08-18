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

package software.amazon.awssdk.retries.internal.ratelimiter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RateLimiterTokenBucketTest {
    private static final double EPSILON = 0.0001;
    private MutableClock clock = null;
    private ScheduledExecutorService scheduler = null;
    private RateLimiterTokenBucket tokenBucket = null;

    @BeforeEach
    void setup() {
        clock = new MutableClock();
        scheduler = mock(ScheduledExecutorService.class);
        tokenBucket = new RateLimiterTokenBucket(clock, scheduler);
    }

    @Test
    void acquireAsync_bucketClosed_futureCompletedExceptionally() {
        tokenBucket.close();
        CompletableFuture<Void> f = tokenBucket.acquireAsync();
        assertThatThrownBy(f::join).satisfies(t -> {
            Throwable cause = t.getCause();
            assertThat(cause).isExactlyInstanceOf(IllegalStateException.class);
            assertThat(cause).hasMessage("Rate limiter bucket is closed");
        });
    }

    @Test
    void acquireAsync_notEnabled_doesNotScheduleTask() {
        CompletableFuture<Void> f = tokenBucket.acquireAsync();
        assertThat(f).isCompleted();
        verifyNoInteractions(scheduler);
    }

    @Test
    void acquireAsync_enabled_schedulesTask() {
        tokenBucket.updateRateAfterThrottling();

        tokenBucket.acquireAsync();
        verify(scheduler).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));
    }


    @Test
    void acquireAsync_scheduleFails_completesFutureExceptionally() {
        tokenBucket.updateRateAfterThrottling();

        doThrow(new RejectedExecutionException("no")).when(scheduler).schedule(any(Runnable.class),
                                                                               anyLong(),
                                                                               any(TimeUnit.class));

        CompletableFuture<Void> f = tokenBucket.acquireAsync();
        assertThatThrownBy(f::join).satisfies(t -> {
            Throwable cause = t.getCause();
            assertThat(cause).hasMessage("Rate limiter bucket is closed");
        });
    }

    @Test
    void acquireAsync_scheduleFails_futureNotInWaitingDeque() {
        tokenBucket.updateRateAfterThrottling();

        doThrow(new RejectedExecutionException("no")).when(scheduler).schedule(any(Runnable.class),
                                                                               anyLong(),
                                                                               any(TimeUnit.class));

        CompletableFuture<Void> f = tokenBucket.acquireAsync();
        assertThat(f).isCompletedExceptionally();
        assertThat(tokenBucket.waiting()).isEmpty();
    }

    @Test
    void acquireAsync_scheduleFails_closesBucket() {
        tokenBucket.updateRateAfterThrottling();

        doThrow(new RejectedExecutionException("no")).when(scheduler).schedule(any(Runnable.class),
                                                                               anyLong(),
                                                                               any(TimeUnit.class));

        CompletableFuture<Void> f = tokenBucket.acquireAsync();
        assertThat(f).isCompletedExceptionally();
        assertThat(tokenBucket.isClosed()).isTrue();
    }

    @Test
    void close_completesAllPendingFutures() {
        // enable throttling so futures actually get queued instead of being completed immediately
        tokenBucket.updateRateAfterThrottling();

        List<CompletableFuture<Void>> futures = IntStream.range(0, 10)
                                                         .mapToObj(i -> tokenBucket.acquireAsync())
                                                         .collect(Collectors.toList());

        tokenBucket.close();

        assertThat(futures).allSatisfy(f -> {
            assertThatThrownBy(f::join).satisfies(t -> {
                Throwable cause = t.getCause();
                assertThat(cause).isExactlyInstanceOf(IllegalStateException.class);
                assertThat(cause).hasMessage("Rate limiter bucket is closed");
            });
        });

        assertThat(tokenBucket.waiting()).isEmpty();
    }

    @Test
    void close_doesShutDownExecutor() {
        tokenBucket.close();
        verifyNoInteractions(scheduler);
    }
    
    @Test
    void doNotify_scheduleRejected_failsFuture() {
        tokenBucket.updateRateAfterThrottling();

        // Empty bucket at default rate of 0.5 tokens per second should be 2seconds
        when(scheduler.schedule(any(Runnable.class), eq(2000L), eq(TimeUnit.MILLISECONDS)))
            .thenThrow(new RejectedExecutionException("no"));

        // 0L is the initial schedule from acquireAsync, capture the doNotify schedule and execute that.
        when(scheduler.schedule(any(Runnable.class), eq(0L), any(TimeUnit.class))).thenAnswer(i -> {
           Runnable r = i.getArgument(0);
           r.run();
           return null;
        });

        tokenBucket.acquireAsync();
        assertThat(tokenBucket.isClosed()).isTrue();
    }

    @Test
    void doNotify_scheduleRejected_closesBucket() {
        tokenBucket.updateRateAfterThrottling();

        // Empty bucket at default rate of 0.5 tokens per second should be 2seconds
        when(scheduler.schedule(any(Runnable.class), eq(2000L), eq(TimeUnit.MILLISECONDS)))
            .thenThrow(new RejectedExecutionException("no"));

        // 0L is the initial schedule from acquireAsync, capture the doNotify schedule and execute that.
        when(scheduler.schedule(any(Runnable.class), eq(0L), any(TimeUnit.class))).thenAnswer(i -> {
            Runnable r = i.getArgument(0);
            r.run();
            return null;
        });

        CompletableFuture<Void> future = tokenBucket.acquireAsync();
        assertThatThrownBy(future::join).hasRootCauseInstanceOf(RejectedExecutionException.class);
        assertThat(tokenBucket.isClosed()).isTrue();
    }

    @Test
    void sendingRateEndToEndTest() {
        for (TestCase sendingRateTestCase : sendingRateTestCases()) {
            assertSendingRateTestCase(sendingRateTestCase);
        }
    }

    void assertSendingRateTestCase(TestCase testCase) {
        clock.setCurrent(testCase.givenTimestamp);
        RateLimiterUpdateResponse res;

        if (testCase.throttleResponse) {
            res = tokenBucket.updateRateAfterThrottling();
        } else {
            res = tokenBucket.updateRateAfterSuccess();
        }
        double measuredTxRate = res.measuredTxRate();
        assertThat(measuredTxRate)
            .as("%s: Measured TX rate", testCase)
            .isCloseTo(testCase.expectMeasuredTxRate, within(EPSILON));
        double fillRate = res.fillRate();
        assertThat(fillRate)
            .as("%s: Fill rate", testCase)
            .isCloseTo(testCase.expectFillRate, within(EPSILON));
    }

    static Collection<TestCase> sendingRateTestCases() {
        // Note: Test cases are not independent. Each case depends on the state of the bucket being correctly updated from the
        // previous test.
        return Arrays.asList(
            new TestCase()
                .givenSuccessResponse()
                .givenTimestamp(0.2)
                .expectMeasuredTxRate(0.000000)
                .expectFillRate(0.500000)
            , new TestCase()
                .givenSuccessResponse()
                .givenTimestamp(0.4)
                .expectMeasuredTxRate(0.000000)
                .expectFillRate(0.500000)
            , new TestCase()
                .givenSuccessResponse()
                .givenTimestamp(0.6)
                .expectMeasuredTxRate(4.800000)
                .expectFillRate(0.500000)
            , new TestCase()
                .givenSuccessResponse()
                .givenTimestamp(0.8)
                .expectMeasuredTxRate(4.800000)
                .expectFillRate(0.500000)
            , new TestCase()
                .givenSuccessResponse()
                .givenTimestamp(1.0)
                .expectMeasuredTxRate(4.160000)
                .expectFillRate(0.500000)
            , new TestCase()
                .givenSuccessResponse()
                .givenTimestamp(1.2)
                .expectMeasuredTxRate(4.160000)
                .expectFillRate(0.691200)
            , new TestCase()
                .givenSuccessResponse()
                .givenTimestamp(1.4)
                .expectMeasuredTxRate(4.160000)
                .expectFillRate(1.097600)
            , new TestCase()
                .givenSuccessResponse()
                .givenTimestamp(1.6)
                .expectMeasuredTxRate(5.632000)
                .expectFillRate(1.638400)
            , new TestCase()
                .givenSuccessResponse()
                .givenTimestamp(1.8)
                .expectMeasuredTxRate(5.632000)
                .expectFillRate(2.332800)
            , new TestCase()
                .givenThrottleResponse()
                .givenTimestamp(2.0)
                .expectMeasuredTxRate(4.326400)
                .expectFillRate(3.028480)
            , new TestCase()
                .givenSuccessResponse()
                .givenTimestamp(2.2)
                .expectMeasuredTxRate(4.326400)
                .expectFillRate(3.486639)
            , new TestCase()
                .givenSuccessResponse()
                .givenTimestamp(2.4)
                .expectMeasuredTxRate(4.326400)
                .expectFillRate(3.821874)
            , new TestCase()
                .givenSuccessResponse()
                .givenTimestamp(2.6)
                .expectMeasuredTxRate(5.665280)
                .expectFillRate(4.053386)
            , new TestCase()
                .givenSuccessResponse()
                .givenTimestamp(2.8)
                .expectMeasuredTxRate(5.665280)
                .expectFillRate(4.200373)
            , new TestCase()
                .givenSuccessResponse()
                .givenTimestamp(3.0)
                .expectMeasuredTxRate(4.333056)
                .expectFillRate(4.282037)
            , new TestCase()
                .givenThrottleResponse()
                .givenTimestamp(3.2)
                .expectMeasuredTxRate(4.333056)
                .expectFillRate(2.997426)
            , new TestCase()
                .givenSuccessResponse()
                .givenTimestamp(3.4)
                .expectMeasuredTxRate(4.333056)
                .expectFillRate(3.452226)
        );
    }

    static class TestCase {
        private boolean throttleResponse;
        private double givenTimestamp;
        private double expectMeasuredTxRate;
        private double expectFillRate;

        TestCase givenSuccessResponse() {
            this.throttleResponse = false;
            return this;
        }

        TestCase givenThrottleResponse() {
            this.throttleResponse = true;
            return this;
        }

        TestCase givenTimestamp(double givenTimestamp) {
            this.givenTimestamp = givenTimestamp;
            return this;
        }

        TestCase expectMeasuredTxRate(double expectMeasuredTxRate) {
            this.expectMeasuredTxRate = expectMeasuredTxRate;
            return this;
        }

        TestCase expectFillRate(double expectFillRate) {
            this.expectFillRate = expectFillRate;
            return this;
        }

        @Override
        public String toString() {
            return "TestCase{" +
                   "throttleResponse=" + throttleResponse +
                   ", givenTimestamp=" + givenTimestamp +
                   ", expectMeasuredTxRate=" + expectMeasuredTxRate +
                   ", expectFillRate=" + expectFillRate +
                   '}';
        }
    }

    static class MutableClock implements RateLimiterClock {
        private double current;

        @Override
        public double time() {
            return current;
        }

        public void setCurrent(double current) {
            this.current = current;
        }
    }
}
