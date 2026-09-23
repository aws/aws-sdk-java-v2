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

package software.amazon.awssdk.http.apache.internal.conn;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.apache.http.HttpClientConnection;
import org.apache.http.conn.ConnectionPoolTimeoutException;
import org.apache.http.conn.ConnectionRequest;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.pool.PoolStats;
import org.apache.http.protocol.HttpContext;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.utils.Logger;

/**
 * A {@link HttpClientConnectionManager} that replaces the connection pool it delegates to if Apache shuts that pool down
 * behind our back, instead of leaving this client permanently unusable.
 *
 * <p>Apache deliberately destroys the connection manager when a {@link Error} escapes a request
 * ({@code MainClientExec.execute}, added in
 * <a href="https://issues.apache.org/jira/browse/HTTPCLIENT-1924">HTTPCLIENT-1924</a>):
 *
 * <pre>
 * } catch (final Error error) {
 *     connManager.shutdown();
 *     throw error;
 * }
 * </pre>
 *
 * <p>The Error is usually an {@code OutOfMemoryError} raised by unrelated application code that happened to exhaust the
 * heap while a request thread was inside the HTTP client. Applications routinely survive that: the allocation that
 * failed becomes garbage and the heap recovers. The HTTP client did not. Every subsequent request failed with
 * {@code IllegalStateException: Connection pool shut down} until the process was restarted, which is what this class
 * fixes.
 *
 * <p>Apache's reasoning for discarding the pool is sound - after an Error, connection state may be inconsistent - so
 * this class honours it. The shut-down pool is not resurrected; it is thrown away, along with any connections it had
 * leased, and a brand new pool is built on the next request. That also disposes of the leases Apache's Error path never
 * released, so the replacement pool starts with its full capacity available.
 *
 * <h2>Distinguishing the two callers of shutdown()</h2>
 * {@link #shutdown()} means "Apache is destroying the pool, rebuild it later". An intentional close by the SDK goes
 * through {@link #closePermanently()} instead, which does not rebuild. This distinction is the whole reason the SDK
 * needs its own wrapper here.
 *
 * <h2>Why connections leased from a replaced pool are safe to release into this wrapper</h2>
 * A request that was in flight when the pool was replaced will still call {@link #releaseConnection}, {@link #connect},
 * {@link #upgrade} or {@link #routeComplete} afterwards, and those calls reach the replacement pool rather than the one
 * the connection came from. That is harmless with Apache's pooling manager:
 * <ul>
 *   <li>{@code connect} and {@code routeComplete} act on the connection and its own pool entry, not on pool
 *       bookkeeping.</li>
 *   <li>{@code releaseConnection} detaches the entry from the connection and then calls {@code AbstractConnPool.release},
 *       which is guarded by {@code if (this.leased.remove(entry))} and so ignores an entry it never leased.</li>
 * </ul>
 * The in-flight request itself still fails, because its connection was closed when the old pool was shut down. Only
 * requests that start after the replacement see a working pool.
 */
@SdkInternalApi
public final class RecreatingHttpClientConnectionManager implements HttpClientConnectionManager {

    private static final Logger log = Logger.loggerFor(RecreatingHttpClientConnectionManager.class);

    private final Supplier<HttpClientConnectionManager> connectionManagerFactory;
    private final Object lifecycleLock = new Object();
    private final AtomicInteger recreationCount = new AtomicInteger();

    private volatile HttpClientConnectionManager delegate;
    private volatile Lifecycle lifecycle = Lifecycle.ACTIVE;

    private RecreatingHttpClientConnectionManager(Supplier<HttpClientConnectionManager> connectionManagerFactory) {
        this.connectionManagerFactory = connectionManagerFactory;
        this.delegate = connectionManagerFactory.get();
    }

    /**
     * @param connectionManagerFactory creates a new connection pool. Called once up front, and again each time Apache
     *                                 destroys the current pool. Must return a new instance on every call.
     */
    public static RecreatingHttpClientConnectionManager create(
        Supplier<HttpClientConnectionManager> connectionManagerFactory) {
        return new RecreatingHttpClientConnectionManager(connectionManagerFactory);
    }

    @Override
    public ConnectionRequest requestConnection(HttpRoute route, Object state) {
        return new RebuildingConnectionRequest(route, state);
    }

    /**
     * @param used the pool a caller just tried and failed to lease from
     * @return true if that pool is no longer the one this manager hands out, meaning it was destroyed underneath the
     *         caller. False once {@link #closePermanently()} has run, so that a closed client keeps rejecting requests.
     */
    private boolean wasDestroyedConcurrently(HttpClientConnectionManager used) {
        return lifecycle == Lifecycle.NEEDS_RECREATE || used != delegate;
    }

    @Override
    public void releaseConnection(HttpClientConnection conn, Object newState, long validDuration, TimeUnit timeUnit) {
        delegate.releaseConnection(conn, newState, validDuration, timeUnit);
    }

    @Override
    public void connect(HttpClientConnection conn, HttpRoute route, int connectTimeout, HttpContext context)
            throws IOException {
        delegate.connect(conn, route, connectTimeout, context);
    }

    @Override
    public void upgrade(HttpClientConnection conn, HttpRoute route, HttpContext context) throws IOException {
        delegate.upgrade(conn, route, context);
    }

    @Override
    public void routeComplete(HttpClientConnection conn, HttpRoute route, HttpContext context) throws IOException {
        delegate.routeComplete(conn, route, context);
    }

    @Override
    public void closeIdleConnections(long idletime, TimeUnit timeUnit) {
        delegate.closeIdleConnections(idletime, timeUnit);
    }

    @Override
    public void closeExpiredConnections() {
        delegate.closeExpiredConnections();
    }

    /**
     * Invoked by Apache when it destroys the connection manager because an {@link Error} escaped a request. The current
     * pool is shut down and discarded; the next request builds a replacement.
     *
     * <p>An intentional shutdown by the SDK must call {@link #closePermanently()} instead.
     */
    @Override
    public void shutdown() {
        HttpClientConnectionManager toShutDown;
        synchronized (lifecycleLock) {
            if (lifecycle == Lifecycle.CLOSED) {
                return;
            }
            // Set this before shutting the pool down so that a concurrent request builds a replacement rather than
            // picking up the pool that is being destroyed.
            lifecycle = Lifecycle.NEEDS_RECREATE;
            toShutDown = delegate;
        }

        log.warn(() -> "The Apache HTTP client shut its connection pool down, which it does when a java.lang.Error "
                       + "(commonly an OutOfMemoryError raised by application code) escapes a request. The pool will be "
                       + "rebuilt on the next request, so this client remains usable. Check application logs for the "
                       + "underlying Error - requests in flight at the time have failed.");

        toShutDown.shutdown();
    }

    /**
     * Shuts the current pool down for good. Unlike {@link #shutdown()} no replacement is built, so subsequent requests
     * fail - which is the documented behaviour of using an SDK HTTP client after closing it.
     */
    public void closePermanently() {
        HttpClientConnectionManager toShutDown;
        synchronized (lifecycleLock) {
            if (lifecycle == Lifecycle.CLOSED) {
                return;
            }
            lifecycle = Lifecycle.CLOSED;
            toShutDown = delegate;
        }
        toShutDown.shutdown();
    }

    /**
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
     * Returns the pool to use for a new request, building a replacement first if Apache destroyed the previous one.
     */
    private HttpClientConnectionManager activeDelegate() {
        if (lifecycle != Lifecycle.NEEDS_RECREATE) {
            return delegate;
        }

        synchronized (lifecycleLock) {
            if (lifecycle == Lifecycle.NEEDS_RECREATE) {
                delegate = connectionManagerFactory.get();
                lifecycle = Lifecycle.ACTIVE;
                int count = recreationCount.incrementAndGet();
                log.debug(() -> "Rebuilt the Apache HTTP connection pool after it was shut down [recreationCount="
                                + count + "]");
            }
            return delegate;
        }
    }

    @SdkTestInternalApi
    int recreationCount() {
        return recreationCount.get();
    }

    @SdkTestInternalApi
    HttpClientConnectionManager currentDelegate() {
        return delegate;
    }

    /**
     * A connection request that survives having its pool destroyed underneath it.
     *
     * <p>Acquiring a connection spans two calls, and Apache raises
     * {@code IllegalStateException("Connection pool shut down")} from both: {@code requestConnection}, and later
     * {@code ConnectionRequest#get} when the caller blocks for a connection
     * ({@code AbstractConnPool.getPoolEntryBlocking}). Either can hit a request that read a live pool moments before
     * Apache's Error handler destroyed it.
     *
     * <p>That exception is not retried by the SDK's retry policy, so without this a concurrent request would be failed
     * outright by an Error raised somewhere else - the exact symptom this class exists to remove. Instead, acquire again
     * from the replacement pool. Bounded to one extra attempt per call so a pool that keeps dying cannot loop.
     */
    private final class RebuildingConnectionRequest implements ConnectionRequest {

        private final HttpRoute route;
        private final Object state;
        private volatile HttpClientConnectionManager leasedFrom;
        private volatile ConnectionRequest delegateRequest;
        private volatile boolean cancelled;

        private RebuildingConnectionRequest(HttpRoute route, Object state) {
            this.route = route;
            this.state = state;
            acquireWithRetry();
        }

        @Override
        public HttpClientConnection get(long timeout, TimeUnit timeUnit)
                throws InterruptedException, ExecutionException, ConnectionPoolTimeoutException {
            try {
                return delegateRequest.get(timeout, timeUnit);
            } catch (IllegalStateException e) {
                if (!shouldAcquireAgain()) {
                    throw e;
                }
                acquireWithRetry();
                return delegateRequest.get(timeout, timeUnit);
            }
        }

        @Override
        public boolean cancel() {
            cancelled = true;
            return delegateRequest.cancel();
        }

        private void acquireWithRetry() {
            try {
                acquire();
            } catch (IllegalStateException e) {
                if (!shouldAcquireAgain()) {
                    throw e;
                }
                acquire();
            }
        }

        private void acquire() {
            // Cleared first so that a failure to build a pool at all is never mistaken for a pool that was destroyed
            // underneath us. The former must surface; retrying it would just hide the real cause.
            leasedFrom = null;
            HttpClientConnectionManager pool = activeDelegate();
            leasedFrom = pool;
            delegateRequest = pool.requestConnection(route, state);
        }

        /**
         * @return true only when this request had a pool and that pool is no longer the one handed out, meaning it was
         *         destroyed underneath us. False for a cancelled request, for a genuinely closed client, for a failure
         *         to build a replacement pool, and for any other {@link IllegalStateException} - all of which must
         *         surface to the caller.
         */
        private boolean shouldAcquireAgain() {
            HttpClientConnectionManager used = leasedFrom;
            return !cancelled && used != null && wasDestroyedConcurrently(used);
        }
    }

    private enum Lifecycle {
        /** Serving requests normally. */
        ACTIVE,
        /** Apache destroyed the pool; the next request builds a replacement. */
        NEEDS_RECREATE,
        /** The SDK closed this client. Nothing is rebuilt. */
        CLOSED
    }
}
