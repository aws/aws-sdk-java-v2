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

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.SdkAutoCloseable;

/**
 * The {@link RateLimiterTokenBucket} keeps track of past throttling responses and adapts to slow down the send rate to adapt to
 * the service. It does this by delaying the completion of the future returned by {@link #acquireAsync()} until the requested
 * capacity is available. Callers must update its internal state by calling {@link #updateRateAfterThrottling()} when getting a
 * throttling response or {@link #updateRateAfterSuccess()} when getting successful response.
 *
 * <p>This class is thread-safe.
 *
 * <p>The algorithm used is adapted from the network congestion avoidance algorithm
 * <a href="https://en.wikipedia.org/wiki/CUBIC_TCP">CUBIC</a>.
 */
@SdkInternalApi
@ThreadSafe
public class RateLimiterTokenBucket implements SdkAutoCloseable {
    // Thread used for capacity waiting and notifying.
    private final ScheduledExecutorService scheduler;

    // Protect access to other members below
    private final Object lock = new Object();

    private final RateLimiterClock clock;
    // the collection of futures returned to threads currently waiting for capacity.
    // Futures are completed in FIFO order.
    // The size of this equal to the number of threads concurrently accessing this bucket.
    private final Deque<CompletableFuture<Void>> waiting = new ArrayDeque<>();
    private PersistentState state;
    private boolean open = true;
    private boolean notifierRunning = false;

    RateLimiterTokenBucket(RateLimiterClock clock, ScheduledExecutorService scheduler) {
        this.clock = clock;
        this.scheduler = scheduler;
        this.state = new PersistentState();
    }

    @Override
    public void close() {
        doClose(null);
    }

    private void doClose(Throwable cause) {
        synchronized (lock) {
            open = false;
            IllegalStateException closedException = new IllegalStateException("Rate limiter bucket is closed", cause);
            while (true) {
                CompletableFuture<Void> w = waiting.poll();
                if (w == null) {
                    break;
                }
                w.completeExceptionally(closedException);
            }
        }
    }

    /**
     * Acquire a token from the bucket.
     *
     * @return A future that is completed when the requested amount is acquired from this bucket.
     */
    public CompletableFuture<Void> acquireAsync() {
        synchronized (lock) {
            if (!open) {
                return CompletableFutureUtils.failedFuture(new IllegalStateException("Rate limiter bucket is closed"));
            }

            // fast path and avoid scheduling in the executor if throttling isn't enabled.
            if (!state.enabled) {
                return CompletableFuture.completedFuture(null);
            }

            CompletableFuture<Void> future = new CompletableFuture<>();
            waiting.add(future);
            if (!notifierRunning) {
                notifierRunning = scheduleOrClose(this::doNotify, Duration.ZERO);
            }
            return future;
        }
    }


    @SdkTestInternalApi
    Deque<CompletableFuture<Void>> waiting() {
        return waiting;
    }

    @SdkTestInternalApi
    boolean isClosed() {
        return !open;
    }

    private void doNotify() {
        while (true) {
            CompletableFuture<Void> w;
            synchronized (lock) {
                w = waiting.poll();
                if (w == null) {
                    notifierRunning = false;
                    return;
                }

                TransientState.AcquireResult acquireResult = updateState(t -> t.tokenBucketAcquire(clock, 1.0)).result;

                // Not enough capacity. Try again later when enough time has
                // passed to refill the bucket at the current rate.
                if (!acquireResult.isSuccessful()) {
                    waiting.push(w);
                    notifierRunning = scheduleOrClose(this::doNotify, acquireResult.refillWait());
                    return;
                }

            }
            // Acquire was successful, signal the waiting thread.
            w.complete(null);
        }
    }

    private void schedule(Runnable command, Duration d) {
        scheduler.schedule(command, d.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * @return true if schedule was successful, false otherwise. If the schedule failed, this bucket will be closed.
     */
    private boolean scheduleOrClose(Runnable command, Duration d) {
        try {
            schedule(command, d);
            return true;
        } catch (Throwable t) {
            doClose(t);
        }
        return false;
    }

    /**
     * Updates the estimated send rate after a throttling response.
     */
    public RateLimiterUpdateResponse updateRateAfterThrottling() {
        StateUpdate<Void> update = consumeState(ts -> ts.updateClientSendingRate(clock, true));
        return RateLimiterUpdateResponse.builder()
                                        .measuredTxRate(update.newState.measuredTxRate())
                                        .fillRate(update.newState.fillRate())
                                        .build();
    }

    /**
     * Updates the estimated send rate after a successful response.
     */
    public RateLimiterUpdateResponse updateRateAfterSuccess() {
        StateUpdate<Void> update = consumeState(ts -> ts.updateClientSendingRate(clock, false));
        return RateLimiterUpdateResponse.builder()
                                        .measuredTxRate(update.newState.measuredTxRate())
                                        .fillRate(update.newState.fillRate())
                                        .build();
    }

    /**
     * Similar to {@link #updateState} but used when the caller only cares about the side effects of the {@link Consumer} but not
     * for the value returned.
     */
    private StateUpdate<Void> consumeState(Consumer<TransientState> mutator) {
        return updateState(ts -> {
            mutator.accept(ts);
            return null;
        });
    }

    /**
     * Converts the stored persistent state into a transient one and transforms it using the provided function.
     */
    private <T> StateUpdate<T> updateState(Function<TransientState, T> mutator) {
        synchronized (lock) {
            TransientState transientState = state.toTransient();
            T result = mutator.apply(transientState);
            state = transientState.toPersistent();
            return new StateUpdate<>(state, result);
        }
    }

    static class StateUpdate<T> {
        private final PersistentState newState;
        private final T result;

        StateUpdate(PersistentState newState, T result) {
            this.newState = newState;
            this.result = result;
        }
    }

    static final class TransientState {
        private static final double MIN_FILL_RATE = 0.5;
        private static final double MIN_CAPACITY = 1.0;
        private static final double SMOOTH = 0.8;
        private static final double BETA = 0.7;
        private static final double SCALE_CONSTANT = 0.4;
        private double fillRate;
        private double maxCapacity;
        private double currentCapacity;
        private boolean lastTimestampIsSet;
        private double lastTimestamp;
        private boolean enabled;
        private double measuredTxRate;
        private double lastTxRateBucket;
        private long requestCount;
        private double lastMaxRate;
        private double lastThrottleTime;
        private double timeWindow;
        private double newTokenBucketRate;

        private TransientState(PersistentState state) {
            this.fillRate = state.fillRate;
            this.maxCapacity = state.maxCapacity;
            this.currentCapacity = state.currentCapacity;
            this.lastTimestampIsSet = state.lastTimestampIsSet;
            this.lastTimestamp = state.lastTimestamp;
            this.enabled = state.enabled;
            this.measuredTxRate = state.measuredTxRate;
            this.lastTxRateBucket = state.lastTxRateBucket;
            this.requestCount = state.requestCount;
            this.lastMaxRate = state.lastMaxRate;
            this.lastThrottleTime = state.lastThrottleTime;
            this.timeWindow = state.timeWindow;
            this.newTokenBucketRate = state.newTokenBucketRate;
        }

        PersistentState toPersistent() {
            return new PersistentState(this);
        }

        /**
         * Acquire tokens from the bucket. If the bucket contains enough capacity to satisfy the request, this method will return
         * a {@link Duration#ZERO} value, otherwise it will return the amount of time the callers need to wait until enough tokens
         * are refilled.
         */
        AcquireResult tokenBucketAcquire(RateLimiterClock clock, double amount) {
            if (!this.enabled) {
                return new AcquireResult(true, Duration.ZERO);
            }
            refill(clock);
            if (this.currentCapacity < amount) {
                double diff = amount - currentCapacity;
                double waitTime = diff / this.fillRate;
                double waitTimeMs = waitTime * 1_000.0;
                Duration duration = Duration.ofMillis((long) Math.ceil(waitTimeMs));
                return new AcquireResult(false, duration);
            }
            this.currentCapacity -= amount;
            return new AcquireResult(true, Duration.ZERO);
        }

        private static class AcquireResult {
            final boolean successful;
            final Duration refillWait;

            AcquireResult(boolean successful, Duration refillWait) {
                this.successful = successful;
                this.refillWait = refillWait;
            }

            boolean isSuccessful() {
                return successful;
            }

            Duration refillWait() {
                return refillWait;
            }
        }

        /**
         * Updates the sending rate depending on whether the response was successful or
         * we got a throttling response.
         */
        void updateClientSendingRate(RateLimiterClock clock, boolean throttlingResponse) {
            updateMeasuredRate(clock);
            double calculatedRate;
            if (throttlingResponse) {
                double rateToUse;
                if (!this.enabled) {
                    rateToUse = this.measuredTxRate;
                } else {
                    rateToUse = Math.min(this.measuredTxRate, this.fillRate);
                }

                this.lastMaxRate = rateToUse;
                calculateTimeWindow();
                this.lastThrottleTime = clock.time();
                calculatedRate = cubicThrottle(rateToUse);
                this.enabled = true;
            } else {
                calculateTimeWindow();
                calculatedRate = cubicSuccess(clock.time());
            }

            double newRate = Math.min(calculatedRate, 2 * this.measuredTxRate);
            updateRate(clock, newRate);
        }

        void refill(RateLimiterClock clock) {
            double timestamp = clock.time();
            if (this.lastTimestampIsSet) {
                double fillAmount = (timestamp - this.lastTimestamp) * this.fillRate;
                this.currentCapacity = Math.min(this.maxCapacity, this.currentCapacity + fillAmount);
            }
            this.lastTimestamp = timestamp;
            this.lastTimestampIsSet = true;
        }

        void updateRate(RateLimiterClock clock, double newRps) {
            refill(clock);
            this.fillRate = Math.max(newRps, MIN_FILL_RATE);
            this.maxCapacity = Math.max(newRps, MIN_CAPACITY);
            this.currentCapacity = Math.min(this.currentCapacity, this.maxCapacity);
            this.newTokenBucketRate = newRps;
        }

        void updateMeasuredRate(RateLimiterClock clock) {
            double time = clock.time();
            this.requestCount += 1;
            double timeBucket = Math.floor(time * 2) / 2;
            if (timeBucket > this.lastTxRateBucket) {
                double currentRate = this.requestCount / (timeBucket - this.lastTxRateBucket);
                this.measuredTxRate = (currentRate * SMOOTH) + (this.measuredTxRate * (1 - SMOOTH));
                this.requestCount = 0;
                this.lastTxRateBucket = timeBucket;
            }
        }

        void calculateTimeWindow() {
            this.timeWindow = Math.pow((this.lastMaxRate * (1 - BETA)) / SCALE_CONSTANT, 1.0 / 3);
        }

        double cubicSuccess(double timestamp) {
            double delta = timestamp - this.lastThrottleTime;
            return (SCALE_CONSTANT * Math.pow(delta - this.timeWindow, 3)) + this.lastMaxRate;
        }

        double cubicThrottle(double rateToUse) {
            return rateToUse * BETA;
        }
    }

    static final class PersistentState {
        private final double fillRate;
        private final double maxCapacity;
        private final double currentCapacity;
        private final boolean lastTimestampIsSet;
        private final double lastTimestamp;
        private final boolean enabled;
        private final double measuredTxRate;
        private final double lastTxRateBucket;
        private final long requestCount;
        private final double lastMaxRate;
        private final double lastThrottleTime;
        private final double timeWindow;
        private final double newTokenBucketRate;

        private PersistentState() {
            this.fillRate = 0;
            this.maxCapacity = 0;
            this.currentCapacity = 0;
            this.lastTimestampIsSet = false;
            this.lastTimestamp = 0;
            this.enabled = false;
            this.measuredTxRate = 0;
            this.lastTxRateBucket = 0;
            this.requestCount = 0;
            this.lastMaxRate = 0;
            this.lastThrottleTime = 0;
            this.timeWindow = 0;
            this.newTokenBucketRate = 0;
        }

        PersistentState(TransientState state) {
            this.fillRate = state.fillRate;
            this.maxCapacity = state.maxCapacity;
            this.currentCapacity = state.currentCapacity;
            this.lastTimestampIsSet = state.lastTimestampIsSet;
            this.lastTimestamp = state.lastTimestamp;
            this.enabled = state.enabled;
            this.measuredTxRate = state.measuredTxRate;
            this.lastTxRateBucket = state.lastTxRateBucket;
            this.requestCount = state.requestCount;
            this.lastMaxRate = state.lastMaxRate;
            this.lastThrottleTime = state.lastThrottleTime;
            this.timeWindow = state.timeWindow;
            this.newTokenBucketRate = state.newTokenBucketRate;
        }

        TransientState toTransient() {
            return new TransientState(this);
        }

        public double fillRate() {
            return fillRate;
        }

        public double measuredTxRate() {
            return measuredTxRate;
        }
    }
}
