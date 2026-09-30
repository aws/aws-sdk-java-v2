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

package software.amazon.awssdk.bridge.smithyjava.client;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.core.exception.ApiCallAttemptTimeoutException;
import software.amazon.awssdk.core.exception.ApiCallTimeoutException;
import software.amazon.smithy.java.context.Context;

/**
 * v2's {@code apiCallTimeout} and {@code apiCallAttemptTimeout}, client- and request-level (ledger 8.1, 8.2).
 *
 * <p>smithy-java 1.6.1 has no timeout concept at all, so these are bridge-owned. They are built the way
 * v2's {@code ApiCallTimeoutTrackingStage} and {@code ApiCallAttemptTimeoutTrackingStage} are: a timer that,
 * on expiry, <em>aborts the in-flight HTTP request</em> and interrupts the thread waiting on it, and a check
 * afterwards that turns whatever the abort produced into v2's {@link ApiCallTimeoutException} or
 * {@link ApiCallAttemptTimeoutException}. The abort is what makes a timeout real rather than advisory: the
 * transport bridges register one per attempt ({@link #registerAbort}) — {@code abort()} on a sync request,
 * cancellation of the async exchange — so a stalled connection is released, not just abandoned.
 *
 * <p>An async client runs its whole call on the envelope's virtual thread, so the same mechanism serves both
 * client types: the "caller" whose wait is interrupted is the envelope.
 *
 * <p>One limit is structural. An attempt timeout surfaces from the transport, and smithy-java never retries a
 * transport failure (ledger 3.6), so where v2 would retry after an attempt timeout the bridge fails the call
 * with it.
 */
@SdkProtectedApi
public final class V2Timeouts {

    /** The client-level attempt timeout, in the client config context, for the transports to read. */
    public static final Context.Key<Duration> CLIENT_ATTEMPT_TIMEOUT = Context.key("v2 apiCallAttemptTimeout");

    private static final ScheduledExecutorService TIMER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "sdk-smithy-bridge-timeout");
        t.setDaemon(true);
        return t;
    });

    /** The attempt currently in flight on this thread; set around each call, filled in by the transport. */
    private static final ThreadLocal<InFlight> CURRENT = new ThreadLocal<>();

    private V2Timeouts() {
    }

    /**
     * Called by a transport as it starts an attempt, with the action that aborts it.
     *
     * @param abort releases the attempt's connection; must be safe to call from another thread.
     */
    public static void registerAbort(Runnable abort) {
        // Every active timer -- a call's, and the attempt's inside it -- has to be able to abort the attempt.
        for (InFlight inFlight = CURRENT.get(); inFlight != null; inFlight = inFlight.parent) {
            inFlight.abort = abort;
        }
    }

    /** The attempt timeout for a call: the request's own, else the client's. */
    public static Duration attemptTimeout(Context context) {
        RequestOverrideConfiguration overrides = context.get(V2RequestOverrides.KEY);
        if (overrides != null && overrides.apiCallAttemptTimeout().isPresent()) {
            return overrides.apiCallAttemptTimeout().get();
        }
        return context.get(CLIENT_ATTEMPT_TIMEOUT);
    }

    /**
     * Runs one transport attempt under {@code timeout}.
     *
     * @param timeout the attempt timeout, or null for none.
     * @param attempt the send; it registers its abort action through {@link #registerAbort}.
     * @param <T>     the response type.
     * @return the attempt's response.
     */
    public static <T> T attempt(Duration timeout, Supplier<T> attempt) {
        return run(timeout, attempt, false);
    }

    static <T> T apiCall(Duration timeout, Supplier<T> call) {
        return run(timeout, call, true);
    }

    private static <T> T run(Duration timeout, Supplier<T> body, boolean wholeCall) {
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            return body.get();
        }
        InFlight previous = CURRENT.get();
        InFlight inFlight = new InFlight(Thread.currentThread(), previous);
        CURRENT.set(inFlight);
        ScheduledFuture<?> timer = TIMER.schedule(inFlight::fire, timeout.toMillis(), TimeUnit.MILLISECONDS);
        try {
            T result = body.get();
            timer.cancel(false);
            if (inFlight.fired) {
                Thread.interrupted();
            }
            return result;
        } catch (RuntimeException e) {
            timer.cancel(false);
            if (!inFlight.fired) {
                throw e;
            }
            // The timer's interrupt must not leak into whatever the thread does next.
            Thread.interrupted();
            if (wholeCall) {
                throw ApiCallTimeoutException.builder()
                                             .message(ApiCallTimeoutException.create(timeout.toMillis()).getMessage())
                                             .cause(e).build();
            }
            throw ApiCallAttemptTimeoutException.builder()
                                                .message(ApiCallAttemptTimeoutException.create(timeout.toMillis()).getMessage())
                                                .cause(e).build();
        } finally {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }

    private static final class InFlight {
        private final Thread waiter;
        private final InFlight parent;
        private volatile Runnable abort;
        private volatile boolean fired;

        private InFlight(Thread waiter, InFlight parent) {
            this.waiter = waiter;
            this.parent = parent;
        }

        private void fire() {
            fired = true;
            Runnable action = abort;
            if (action != null) {
                try {
                    action.run();
                } catch (RuntimeException ignored) {
                    // Aborting is best effort; the interrupt below still ends the wait.
                }
            }
            waiter.interrupt();
        }
    }
}
