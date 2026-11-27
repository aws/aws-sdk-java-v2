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

package software.amazon.awssdk.http.apache5.internal.conn;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import org.apache.hc.client5.http.HttpRoute;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.io.ConnectionEndpoint;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.io.LeaseRequest;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.core5.pool.PoolStats;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

/**
 * A {@link HttpClientConnectionManager} that replaces the connection pool it delegates to if Apache closes that pool
 * behind our back, instead of leaving this client permanently unusable. The SDK installs it only when the client's
 * {@code ConnectionPoolRecoveryMode} is {@code WITH_BACKOFF} or {@code ALWAYS}; otherwise Apache's own pool is used
 * directly.
 *
 * <p>Apache deliberately destroys the connection manager when a {@link Error} escapes a request
 * ({@code MainClientExec.execute} and {@code MinimalHttpClient.doExecute}, the 5.x form of
 * <a href="https://issues.apache.org/jira/browse/HTTPCLIENT-1924">HTTPCLIENT-1924</a>):
 *
 * <pre>
 * } catch (final Error error) {
 *     connectionManager.close(CloseMode.IMMEDIATE);
 *     throw error;
 * }
 * </pre>
 *
 * <p>The Error is usually an {@code OutOfMemoryError} raised by unrelated application code that happened to exhaust the
 * heap while a request thread was inside the HTTP client, or a {@code StackOverflowError}. Applications routinely survive
 * that: the allocation that failed becomes garbage and the heap recovers. Without this class the HTTP client did not, and
 * every subsequent request failed with {@code IllegalStateException: Connection pool shut down}.
 *
 * <p>Apache's reasoning for discarding the pool is sound - after an Error, connection state may be inconsistent - so
 * this class honours it. The closed pool is not resurrected; it is thrown away, along with any connections it had
 * leased, and a brand new pool is built on the next request. That also disposes of the leases Apache's Error path never
 * released, so the replacement pool starts with its full capacity available.
 *
 * <h2>Recovery backoff</h2>
 * The backoff window is given at construction: 5 minutes for {@code WITH_BACKOFF}, zero for {@code ALWAYS}. The first
 * Error-driven close is rebuilt on the next request. After that, at most one rebuild happens per window, measured from
 * the last successful rebuild: a close inside that window leaves the pool down ({@link #inRecoveryBackoff()}), so
 * requests fail fast, and the first request after the window rebuilds it. Errors during the window neither restart nor
 * extend it. This keeps an Error that recurs on every request from repeatedly destroying the pool and failing requests in
 * flight on other threads. A zero window means every Error-driven close is rebuilt on the next request; the recovery
 * WARN is then rate-limited, and the other rebuilds are logged at DEBUG.
 *
 * <h2>Attributing a close to the pool the failing request used</h2>
 * Apache's close carries no information about which pool the failing request used. Each thread therefore records the
 * pool it is leasing from, before it leases, and a close requested by a thread whose recorded pool has already been
 * replaced is ignored: that pool is already gone, and destroying the healthy replacement would start a backoff outage for
 * nothing. A close from a thread with no record (one that never leased through this manager) closes the current pool.
 * The client clears the record at the end of every request with {@link #clearCurrentThreadLease()}.
 *
 * <h2>Distinguishing the two callers of close()</h2>
 * {@link #close()} and {@link #close(CloseMode)} mean "Apache is destroying the pool, rebuild it later". An intentional
 * close by the SDK goes through {@link #closePermanently()} instead, which does not rebuild. In 5.x the SDK and Apache
 * would otherwise call the identical method, so this distinction is essential.
 *
 * <h2>Endpoints leased from a pool that has since been replaced</h2>
 * A request that was in flight when the pool was replaced will still call {@link #release}, {@link #connect} or
 * {@link #upgrade} afterwards, and those calls reach the replacement pool rather than the one the endpoint came from.
 * {@code connect} and {@code upgrade} are fine: they act on the endpoint's own connection, not on pool bookkeeping.
 *
 * <p>{@code release} is not. Apache 5.x rejects an entry the pool never leased - {@code StrictConnPool.release} ends in
 * {@code else { throw new IllegalStateException("Pool entry is not present in the set of leased entries"); }}, and
 * {@code LaxConnPool} throws the same from {@code removeLeased}, so this does not depend on which
 * {@code PoolConcurrencyPolicy} is in effect. Apache 4.x differs here: its {@code AbstractConnPool.release} performs the
 * same work inside {@code if (leased.remove(entry))} with no else branch, so a foreign entry is silently ignored and the
 * 4.x wrapper needs no equivalent handling.
 *
 * <p>{@link #release} therefore swallows exactly that exception, and only once a pool has actually been replaced. There
 * is genuinely nothing to return: the pool the endpoint came from was closed, and closing it closed the connection.
 * Without this, a request that merely overlapped someone else's {@link Error} would fail with an exception the SDK's
 * retry policy does not retry - reintroducing the symptom this class exists to remove, on a different code path.
 *
 * <p>The in-flight request itself still fails, because its connection was closed when the old pool was closed. It fails
 * with an {@link java.io.IOException} such as {@code SocketException: Socket closed}, which the SDK does retry. Only
 * requests that start after the replacement see a working pool.
 */
@SdkInternalApi
public final class RecreatingHttpClientConnectionManager implements HttpClientConnectionManager, IdleConnectionCloser {

    /** The recovery backoff window of the {@code WITH_BACKOFF} mode. */
    public static final Duration DEFAULT_RECOVERY_BACKOFF = Duration.ofMinutes(5);

    private static final Logger log = Logger.loggerFor(RecreatingHttpClientConnectionManager.class);

    /** With a zero backoff window, the minimum time between two recovery WARNs; the rebuilds in between log at DEBUG. */
    private static final long RECOVERY_WARNING_INTERVAL_NANOS = TimeUnit.MINUTES.toNanos(5);

    private final Supplier<HttpClientConnectionManager> connectionManagerFactory;
    private final LongSupplier nanoClock;
    private final long recoveryBackoffNanos;
    /** The window in whole minutes, only for log messages, which are logged only with a non-zero window. */
    private final long recoveryBackoffMinutes;
    private final Object lifecycleLock = new Object();
    private final AtomicInteger recreationCount = new AtomicInteger();

    /** The pool the request in progress on each thread leased from, or unset if there is none. */
    private final ThreadLocal<HttpClientConnectionManager> currentThreadLease = new ThreadLocal<>();

    private volatile HttpClientConnectionManager delegate;
    private volatile Lifecycle lifecycle = Lifecycle.ACTIVE;

    /** When the last successful rebuild happened, from {@link #nanoClock}. Meaningful only once one has happened. */
    private volatile long lastRecoveryNanos;

    /** A pool whose close failed part way through, still holding sockets that need closing. */
    private volatile HttpClientConnectionManager abandonedPool;

    /** Zero window only: when the last recovery WARN was logged. Guarded by {@link #lifecycleLock}. */
    private long lastRecoveryWarningNanos;

    /** Zero window only: rebuilds since the last recovery WARN. Guarded by {@link #lifecycleLock}. */
    private int rebuildsSincePreviousWarning;

    private RecreatingHttpClientConnectionManager(Supplier<HttpClientConnectionManager> connectionManagerFactory,
                                                  LongSupplier nanoClock,
                                                  Duration recoveryBackoff) {
        // Validated before the factory call, so an invalid argument never builds a pool.
        this.nanoClock = Validate.paramNotNull(nanoClock, "nanoClock");
        Validate.isNotNegative(recoveryBackoff, "recoveryBackoff");
        this.recoveryBackoffNanos = recoveryBackoff.toNanos();
        this.recoveryBackoffMinutes = recoveryBackoff.toMinutes();
        this.connectionManagerFactory = connectionManagerFactory;
        this.delegate = connectionManagerFactory.get();
    }

    /**
     * Equivalent to {@link #create(Supplier, LongSupplier, Duration)} with the system clock and the {@code WITH_BACKOFF}
     * window, {@link #DEFAULT_RECOVERY_BACKOFF}.
     *
     * @param connectionManagerFactory creates a new connection pool. Called once up front, and again each time Apache
     *                                 destroys the current pool. Must return a new instance on every call.
     */
    public static RecreatingHttpClientConnectionManager create(
        Supplier<HttpClientConnectionManager> connectionManagerFactory) {
        return create(connectionManagerFactory, System::nanoTime);
    }

    /**
     * Equivalent to {@link #create(Supplier, LongSupplier, Duration)} with the {@code WITH_BACKOFF} window,
     * {@link #DEFAULT_RECOVERY_BACKOFF}.
     *
     * @param connectionManagerFactory creates a new connection pool. Called once up front, and again each time Apache
     *                                 destroys the current pool. Must return a new instance on every call.
     * @param nanoClock the time source for the recovery backoff, in the units of {@link System#nanoTime()}
     */
    public static RecreatingHttpClientConnectionManager create(
        Supplier<HttpClientConnectionManager> connectionManagerFactory, LongSupplier nanoClock) {
        return create(connectionManagerFactory, nanoClock, DEFAULT_RECOVERY_BACKOFF);
    }

    /**
     * @param connectionManagerFactory creates a new connection pool. Called once up front, and again each time Apache
     *                                 destroys the current pool. Must return a new instance on every call.
     * @param nanoClock the time source for the recovery backoff, in the units of {@link System#nanoTime()}
     * @param recoveryBackoff the minimum time between rebuilds; zero rebuilds after every Error-driven close. Must not be
     *                        null or negative.
     */
    public static RecreatingHttpClientConnectionManager create(
        Supplier<HttpClientConnectionManager> connectionManagerFactory, LongSupplier nanoClock, Duration recoveryBackoff) {
        return new RecreatingHttpClientConnectionManager(connectionManagerFactory, nanoClock, recoveryBackoff);
    }

    @Override
    public LeaseRequest lease(String id, HttpRoute route, Timeout requestTimeout, Object state) {
        HttpClientConnectionManager current = activeDelegate();
        try {
            // Before the delegate call, which can lease an entry at once, so that allocating the record can never fail
            // while a connection is held.
            currentThreadLease.set(current);
            return current.lease(id, route, requestTimeout, state);
        } catch (IllegalStateException e) {
            // We may have lost a race: Apache's Error handler destroyed this pool after we read it but before we leased
            // from it, which Apache reports as IllegalStateException("Connection pool shut down"). The SDK does not
            // retry that exception, so rebuild and lease once more rather than failing the caller for someone else's
            // Error. Rethrown if the pool was not concurrently destroyed, so a genuinely closed client keeps failing.
            if (!wasDestroyedConcurrently(current)) {
                throw e;
            }
            HttpClientConnectionManager retry = activeDelegate();
            // Replaces the value of the existing record, so it does not allocate.
            currentThreadLease.set(retry);
            return retry.lease(id, route, requestTimeout, state);
        }
    }

    /**
     * @param used the pool a caller just tried and failed to lease from
     * @return true if that pool is no longer the one this manager hands out, meaning it was destroyed underneath the
     *         caller. False once {@link #closePermanently()} has run, so that a closed client keeps rejecting requests,
     *         and false in {@link Lifecycle#RECOVERY_BACKOFF}, where the dead pool is deliberately still handed out.
     */
    private boolean wasDestroyedConcurrently(HttpClientConnectionManager used) {
        return lifecycle == Lifecycle.NEEDS_RECREATE || used != delegate;
    }

    @Override
    public void release(ConnectionEndpoint endpoint, Object newState, TimeValue validDuration) {
        try {
            delegate.release(endpoint, newState, validDuration);
        } catch (IllegalStateException e) {
            if (!isForeignEntryRelease(e)) {
                throw e;
            }
            // The endpoint was leased from a pool that has since been replaced, so the pool it is being returned to has
            // never heard of it. Unlike Apache 4.x, whose AbstractConnPool.release silently ignores a foreign entry,
            // 5.x StrictConnPool.release throws. There is genuinely nothing to return: the pool this endpoint came from
            // was closed, and closing it closed this connection. Swallowing keeps a request that merely overlapped
            // someone else's Error from failing with an exception the SDK cannot retry.
            log.debug(() -> "Ignoring release of a connection that was leased from a connection pool which has since "
                            + "been replaced. The connection was already closed when that pool was closed.");
        }
    }

    /**
     * @return true if this exception is 5.x rejecting an endpoint that belongs to a pool we have already discarded.
     *         Gated on a replacement actually having happened, so that before any Error-triggered rebuild this method
     *         changes nothing and a genuine double-release still surfaces.
     */
    private boolean isForeignEntryRelease(IllegalStateException e) {
        return recreationCount.get() > 0
               && e.getMessage() != null
               && e.getMessage().contains("not present in the set of leased entries");
    }

    @Override
    public void connect(ConnectionEndpoint endpoint, TimeValue connectTimeout, HttpContext context) throws IOException {
        delegate.connect(endpoint, connectTimeout, context);
    }

    @Override
    public void upgrade(ConnectionEndpoint endpoint, HttpContext context) throws IOException {
        delegate.upgrade(endpoint, context);
    }

    /**
     * Invoked by Apache when it destroys the connection manager because an {@link Error} escaped a request. The current
     * pool is closed and discarded. The next request builds a replacement, unless the last rebuild was within the
     * recovery backoff window given at construction, in which case the pool stays down until the window has passed. With
     * a zero window the next request always builds a replacement.
     *
     * <p>Ignored if the request on this thread leased from a pool that has already been replaced, and a no-op if the
     * current pool is already closed. An intentional shutdown by the SDK must call {@link #closePermanently()} instead.
     */
    @Override
    public void close(CloseMode closeMode) {
        closeAfterError(closeMode);
    }

    /**
     * @see #close(CloseMode)
     */
    @Override
    public void close() {
        closeAfterError(null);
    }

    /**
     * The Error path shared by both close methods.
     *
     * @param closeMode the mode Apache asked for, or null when it called close() without one
     */
    private void closeAfterError(CloseMode closeMode) {
        // The pool the request on this thread leased from, or null if no request on this thread is in progress.
        HttpClientConnectionManager requestPool = currentThreadLease.get();
        HttpClientConnectionManager toClose;
        long backoffRemaining;
        synchronized (lifecycleLock) {
            if (lifecycle == Lifecycle.CLOSED) {
                return;
            }
            if (requestPool != null && requestPool != delegate) {
                // The failing request used a pool that has already been replaced. Apache asked us to destroy that pool,
                // and it is already gone; destroying the current one would fail healthy requests for nothing.
                log.debug(() -> "Ignoring a connection pool close requested by a request that used a connection pool "
                                + "which has already been replaced.");
                return;
            }
            if (lifecycle != Lifecycle.ACTIVE) {
                // NEEDS_RECREATE or RECOVERY_BACKOFF: the current pool is already closed or being closed.
                return;
            }
            backoffRemaining = backoffRemainingNanos();
            // Set before closing the pool so a concurrent lease never picks up the pool being destroyed.
            lifecycle = backoffRemaining > 0 ? Lifecycle.RECOVERY_BACKOFF : Lifecycle.NEEDS_RECREATE;
            toClose = delegate;
        }
        // Nothing may run between the transition above and this try (no call, no allocation): this path runs right after
        // an Error, and anything that throws here would leave the pool neither closed nor recorded for cleanup.
        try {
            if (closeMode == null) {
                toClose.close();
            } else {
                toClose.close(closeMode);
            }
        } catch (Throwable t) {
            // Deliberately not rethrown. Apache calls us from `catch (Error error) {
            // connectionManager.close(CloseMode.IMMEDIATE); throw error; }`, so throwing here would replace the Error the
            // application actually needs to see with a second one raised while cleaning up. Swallowing lets Apache
            // rethrow the original.
            //
            // A close that dies part way through can leave sockets open, and one that dies before it reaches the pool
            // leaves the pool serving leases. Record the pool first, because the WARN below allocates and may itself
            // fail; the cleanup path finishes the close and reclaims what is left on the next lease, when the heap has
            // likely recovered.
            abandonedPool = toClose;
            log.warn(() -> "Closing the Apache HTTP connection pool did not complete, so some of its connections may "
                          + "still be open. A best effort attempt to close them will be made on the next request.", t);
        }
        // Logged only after the close, for the same reason.
        if (backoffRemaining > 0) {
            log.warn(() -> "The Apache HTTP client closed its connection pool again because a java.lang.Error escaped a "
                           + "request, less than " + recoveryBackoffMinutes + " minutes after the pool was last "
                           + "rebuilt. To avoid rebuilding the pool repeatedly, it will not be rebuilt until "
                           + recoveryBackoffMinutes + " minutes after that rebuild [recoveryNumber="
                           + recreationCount.get() + ", secondsUntilRecovery=" + toSecondsRoundedUp(backoffRemaining)
                           + "]. Until then, requests on this client fail with ConnectionPoolShutdownException (reason "
                           + "RECOVERY_BACKOFF). Check application logs for the underlying Error.");
        } else {
            log.debug(() -> "The Apache HTTP client closed its connection pool after a java.lang.Error escaped a "
                            + "request. The pool will be rebuilt on the next request.");
        }
    }

    /**
     * Finishes off a pool whose close failed, and closes the connections it left open.
     *
     * <p>Apache's manager-level methods cannot do this: {@code PoolingHttpClientConnectionManager.close} marks the manager
     * closed before it closes the pool, after which its {@code closeIdle} returns early. The SDK's
     * {@link SafePoolingHttpClientConnectionManager#reclaimAfterFailedClose()} works on the pool directly instead. It
     * first performs the close Apache asked for, which matters when the failure happened before the pool started closing
     * (the pool would otherwise keep serving leases), and then closes the idle connections an interrupted close left
     * behind. Connections that were leased on a route whose close was interrupted are not reachable this way.
     *
     * <p>Best effort by nature. It runs before the replacement pool is built, on the first lease in
     * {@link Lifecycle#RECOVERY_BACKOFF} and in {@link #closePermanently()}, and a failure here cannot stop the client
     * from recovering. It runs once per abandoned pool and is not retried if it fails again, because a cleanup that keeps
     * failing would log a WARN on every lease.
     */
    private void cleanUpAbandonedPool() {
        HttpClientConnectionManager abandoned = abandonedPool;
        if (abandoned == null) {
            return;
        }
        abandonedPool = null;

        try {
            // No other branch. The SDK's factory always returns SafePoolingHttpClientConnectionManager, and for a plain
            // PoolingHttpClientConnectionManager the manager-level closeIdle is provably a no-op after close.
            if (abandoned instanceof SafePoolingHttpClientConnectionManager) {
                ((SafePoolingHttpClientConnectionManager) abandoned).reclaimAfterFailedClose();
                log.debug(() -> "Closed the connections left open by a connection pool close that did not complete.");
            }
        } catch (Throwable t) {
            log.warn(() -> "Could not close connections left open by a connection pool close that did not complete. "
                          + "Some sockets may remain open until the process exits.", t);
        }
    }

    /**
     * Closes the current pool for good. Unlike {@link #close(CloseMode)} no replacement is built, so subsequent requests
     * fail - which is the documented behaviour of using an SDK HTTP client after closing it. Works from every state.
     */
    public void closePermanently() {
        HttpClientConnectionManager toClose;
        synchronized (lifecycleLock) {
            if (lifecycle == Lifecycle.CLOSED) {
                return;
            }
            lifecycle = Lifecycle.CLOSED;
            toClose = delegate;
            cleanUpAbandonedPool();
        }
        toClose.close(CloseMode.IMMEDIATE);
    }

    /**
     * @return true if Apache closed the pool again within the recovery backoff window, so the pool stays down and
     *         requests fail until the window has passed.
     */
    public boolean inRecoveryBackoff() {
        return lifecycle == Lifecycle.RECOVERY_BACKOFF;
    }

    /**
     * Forgets which pool the request on the calling thread leased from. Called by the client at the end of every
     * request, so pooled threads retain nothing between requests.
     */
    public void clearCurrentThreadLease() {
        currentThreadLease.remove();
    }

    /**
     * Reaps idle connections from the current pool, without building a replacement if there is none: there are no idle
     * connections to close in a pool that does not exist yet.
     */
    @Override
    public void closeIdle(TimeValue idleTime) {
        HttpClientConnectionManager current = delegate;
        if (current instanceof PoolingHttpClientConnectionManager) {
            ((PoolingHttpClientConnectionManager) current).closeIdle(idleTime);
        }
    }

    /**
     * Reports statistics for the pool currently held, deliberately without building one. Reaping and metrics must never
     * resurrect a client that is simply sitting idle, so between an Error destroying a pool and the next request
     * rebuilding it this returns the destroyed pool's final stats - which can show leases that no longer exist. That
     * window is transient and corrects itself on the next request.
     *
     * @return pool statistics for the current pool, or null if it does not expose any.
     */
    public PoolStats poolStats() {
        HttpClientConnectionManager current = delegate;
        if (current instanceof PoolingHttpClientConnectionManager) {
            return ((PoolingHttpClientConnectionManager) current).getTotalStats();
        }
        return null;
    }

    /**
     * @return the time left in the recovery backoff window, or 0 when the window is zero, no recovery has happened yet,
     *         or the window has ended. Reads volatile fields, so it is safe without the lock.
     */
    private long backoffRemainingNanos() {
        // A zero window never backs off, even if the clock reads earlier than the last rebuild.
        if (recoveryBackoffNanos == 0 || recreationCount.get() == 0) {
            return 0;
        }
        long elapsed = nanoClock.getAsLong() - lastRecoveryNanos;
        return elapsed >= recoveryBackoffNanos ? 0 : recoveryBackoffNanos - elapsed;
    }

    private static long toSecondsRoundedUp(long nanos) {
        return (nanos + TimeUnit.SECONDS.toNanos(1) - 1) / TimeUnit.SECONDS.toNanos(1);
    }

    /**
     * Returns the pool to use for a new request, building a replacement first if Apache destroyed the previous one and
     * the recovery backoff allows it. Inside the backoff window the dead pool is returned, so the lease fails fast.
     */
    private HttpClientConnectionManager activeDelegate() {
        Lifecycle state = lifecycle;
        if (state == Lifecycle.ACTIVE || state == Lifecycle.CLOSED) {
            return delegate;
        }
        if (state == Lifecycle.RECOVERY_BACKOFF && abandonedPool == null && backoffRemainingNanos() > 0) {
            // Fail fast without the lock: the dead pool rejects the lease.
            return delegate;
        }

        HttpClientConnectionManager rebuilt;
        int recoveryNumber;
        // Zero window only: whether this rebuild logs the rate-limited WARN, and the rebuilds since the previous one,
        // including this one.
        boolean warnAlways;
        int rebuildsForWarning;
        synchronized (lifecycleLock) {
            if (lifecycle == Lifecycle.RECOVERY_BACKOFF) {
                // Reclaim sockets promptly, not when the window ends.
                cleanUpAbandonedPool();
                if (backoffRemainingNanos() > 0) {
                    return delegate;
                }
            } else if (lifecycle != Lifecycle.NEEDS_RECREATE) {
                // ACTIVE (another lease rebuilt) or CLOSED.
                return delegate;
            }
            cleanUpAbandonedPool();
            // May throw: the state and the window stay unchanged, and the next lease tries again.
            rebuilt = connectionManagerFactory.get();
            // Incremented before delegate is published, so that a release reaching the new pool always sees a
            // replacement has happened (isForeignEntryRelease).
            recoveryNumber = recreationCount.incrementAndGet();
            delegate = rebuilt;
            long rebuiltAtNanos = nanoClock.getAsLong();
            // Written after delegate, so the lock-free precheck never pairs a new window with the old dead pool.
            lastRecoveryNanos = rebuiltAtNanos;
            if (recoveryBackoffNanos == 0) {
                // Every Error-driven close is rebuilt, so limit the WARN to one per interval.
                rebuildsSincePreviousWarning++;
                warnAlways = recoveryNumber == 1
                             || rebuiltAtNanos - lastRecoveryWarningNanos >= RECOVERY_WARNING_INTERVAL_NANOS;
                rebuildsForWarning = rebuildsSincePreviousWarning;
                if (warnAlways) {
                    rebuildsSincePreviousWarning = 0;
                    lastRecoveryWarningNanos = rebuiltAtNanos;
                }
            } else {
                warnAlways = false;
                rebuildsForWarning = 0;
            }
            // Last write: publishes everything above.
            lifecycle = Lifecycle.ACTIVE;
        }
        if (recoveryBackoffNanos != 0) {
            log.warn(() -> "Rebuilt the Apache HTTP client's connection pool, which the client closed because a "
                           + "java.lang.Error (commonly an OutOfMemoryError or StackOverflowError) escaped a request "
                           + "[recoveryNumber=" + recoveryNumber + "]. Requests in flight when the pool was closed have "
                           + "failed; check application logs for the underlying Error. If an Error closes the pool again "
                           + "within " + recoveryBackoffMinutes + " minutes of this rebuild, the pool is not rebuilt "
                           + "until those " + recoveryBackoffMinutes + " minutes have passed.");
        } else if (warnAlways) {
            log.warn(() -> "Rebuilt the Apache HTTP client's connection pool, which the client closed because a "
                           + "java.lang.Error (commonly an OutOfMemoryError or StackOverflowError) escaped a request "
                           + "[recoveryNumber=" + recoveryNumber + ", rebuildsSincePreviousWarning=" + rebuildsForWarning
                           + "]. Requests in flight when the pool was closed have failed; check application logs for the "
                           + "underlying Error. The connection pool recovery mode is ALWAYS, so the pool is rebuilt after "
                           + "every such Error. This warning is logged at most once every 5 minutes; other rebuilds are "
                           + "logged at DEBUG level.");
        } else {
            log.debug(() -> "Rebuilt the Apache HTTP client's connection pool after a java.lang.Error escaped a request "
                            + "[recoveryNumber=" + recoveryNumber + "]. A warning for this is logged at most once every "
                            + "5 minutes.");
        }
        return rebuilt;
    }

    @SdkTestInternalApi
    int recreationCount() {
        return recreationCount.get();
    }

    @SdkTestInternalApi
    HttpClientConnectionManager currentDelegate() {
        return delegate;
    }

    private enum Lifecycle {
        /** Serving requests normally. */
        ACTIVE,
        /** Apache destroyed the pool; the next request builds a replacement. */
        NEEDS_RECREATE,
        /**
         * Apache destroyed the pool within the backoff window of the last rebuild. Leases get the dead pool and fail
         * until the window has passed; the first lease after it builds a replacement.
         */
        RECOVERY_BACKOFF,
        /** The SDK closed this client. Nothing is rebuilt. */
        CLOSED
    }
}
