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
import java.util.concurrent.atomic.AtomicInteger;
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

/**
 * A {@link HttpClientConnectionManager} that replaces the connection pool it delegates to if Apache closes that pool
 * behind our back, instead of leaving this client permanently unusable.
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
 * heap while a request thread was inside the HTTP client. Applications routinely survive that: the allocation that
 * failed becomes garbage and the heap recovers. The HTTP client did not. Every subsequent request failed with
 * {@code IllegalStateException: Connection pool shut down} until the process was restarted, which is what this class
 * fixes.
 *
 * <p>Apache's reasoning for discarding the pool is sound - after an Error, connection state may be inconsistent - so
 * this class honours it. The closed pool is not resurrected; it is thrown away, along with any connections it had
 * leased, and a brand new pool is built on the next request. That also disposes of the leases Apache's Error path never
 * released, so the replacement pool starts with its full capacity available.
 *
 * <h2>Distinguishing the two callers of close()</h2>
 * {@link #close()} and {@link #close(CloseMode)} mean "Apache is destroying the pool, rebuild it later". An intentional
 * close by the SDK goes through {@link #closePermanently()} instead, which does not rebuild. In 5.x the SDK and Apache
 * would otherwise call the identical method, so this distinction is essential.
 *
 * <h2>Why endpoints leased from a replaced pool are safe to release into this wrapper</h2>
 * A request that was in flight when the pool was replaced will still call {@link #release}, {@link #connect} or
 * {@link #upgrade} afterwards, and those calls reach the replacement pool rather than the one the endpoint came from.
 * That is harmless with Apache's pooling manager:
 * <ul>
 *   <li>{@code connect} and {@code upgrade} act on the endpoint's own connection, not on pool bookkeeping.</li>
 *   <li>{@code release} detaches the entry from the endpoint and then calls {@code StrictConnPool.release}, which is
 *       guarded by {@code if (this.leased.remove(entry))} and so ignores an entry it never leased.</li>
 * </ul>
 * The in-flight request itself still fails, because its connection was closed when the old pool was closed. Only
 * requests that start after the replacement see a working pool.
 */
@SdkInternalApi
public final class RecreatingHttpClientConnectionManager implements HttpClientConnectionManager, IdleConnectionCloser {

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
    public LeaseRequest lease(String id, HttpRoute route, Timeout requestTimeout, Object state) {
        HttpClientConnectionManager current = activeDelegate();
        try {
            return current.lease(id, route, requestTimeout, state);
        } catch (IllegalStateException e) {
            // We may have lost a race: Apache's Error handler destroyed this pool after we read it but before we leased
            // from it, which Apache reports as IllegalStateException("Connection pool shut down"). The SDK does not
            // retry that exception, so rebuild and lease once more rather than failing the caller for someone else's
            // Error. Rethrown if the pool was not concurrently destroyed, so a genuinely closed client keeps failing.
            if (!wasDestroyedConcurrently(current)) {
                throw e;
            }
            return activeDelegate().lease(id, route, requestTimeout, state);
        }
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
     * pool is closed and discarded; the next request builds a replacement.
     *
     * <p>An intentional shutdown by the SDK must call {@link #closePermanently()} instead.
     */
    @Override
    public void close(CloseMode closeMode) {
        HttpClientConnectionManager toClose = markForRecreate();
        if (toClose != null) {
            toClose.close(closeMode);
        }
    }

    /**
     * @see #close(CloseMode)
     */
    @Override
    public void close() throws IOException {
        HttpClientConnectionManager toClose = markForRecreate();
        if (toClose != null) {
            toClose.close();
        }
    }

    /**
     * Closes the current pool for good. Unlike {@link #close(CloseMode)} no replacement is built, so subsequent requests
     * fail - which is the documented behaviour of using an SDK HTTP client after closing it.
     */
    public void closePermanently() {
        HttpClientConnectionManager toClose;
        synchronized (lifecycleLock) {
            if (lifecycle == Lifecycle.CLOSED) {
                return;
            }
            lifecycle = Lifecycle.CLOSED;
            toClose = delegate;
        }
        toClose.close(CloseMode.IMMEDIATE);
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
     * @return the pool that must be closed by the caller, or null if this manager is already closed for good.
     */
    private HttpClientConnectionManager markForRecreate() {
        HttpClientConnectionManager toClose;
        synchronized (lifecycleLock) {
            if (lifecycle == Lifecycle.CLOSED) {
                return null;
            }
            // Set this before closing the pool so that a concurrent request builds a replacement rather than picking up
            // the pool that is being destroyed.
            lifecycle = Lifecycle.NEEDS_RECREATE;
            toClose = delegate;
        }

        log.warn(() -> "The Apache HTTP client closed its connection pool, which it does when a java.lang.Error "
                       + "(commonly an OutOfMemoryError raised by application code) escapes a request. The pool will be "
                       + "rebuilt on the next request, so this client remains usable. Check application logs for the "
                       + "underlying Error - requests in flight at the time have failed.");

        return toClose;
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
                log.debug(() -> "Rebuilt the Apache HTTP connection pool after it was closed [recreationCount="
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

    private enum Lifecycle {
        /** Serving requests normally. */
        ACTIVE,
        /** Apache destroyed the pool; the next request builds a replacement. */
        NEEDS_RECREATE,
        /** The SDK closed this client. Nothing is rebuilt. */
        CLOSED
    }
}
