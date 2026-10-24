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

import org.apache.hc.client5.http.impl.io.ManagedHttpClientConnectionFactory;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.io.HttpClientConnectionOperator;
import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.core5.pool.DefaultDisposalCallback;
import org.apache.hc.core5.pool.PoolReusePolicy;
import org.apache.hc.core5.util.TimeValue;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;

/**
 * Specialization of {@link PoolingHttpClientConnectionManager} to enable use of {@link SafeStrictConnPool} to prevent leaking
 * connections when the thread waiting on the future is interrupted.
 *
 * <p>It is an {@link IdleConnectionCloser} through the inherited {@code closeIdle(TimeValue)}, so that it can be registered
 * with {@link IdleConnectionReaper} directly when connection pool recovery is disabled.
 */
@SdkInternalApi
public final class SafePoolingHttpClientConnectionManager extends PoolingHttpClientConnectionManager
        implements IdleConnectionCloser {

    private final SafeStrictConnPool strictPool;

    public SafePoolingHttpClientConnectionManager(HttpClientConnectionOperator connectionOperator) {
        this(connectionOperator, newPool());
    }

    private SafePoolingHttpClientConnectionManager(HttpClientConnectionOperator connectionOperator,
                                                   SafeStrictConnPool strictPool) {
        super(connectionOperator, strictPool, ManagedHttpClientConnectionFactory.INSTANCE);
        this.strictPool = strictPool;
    }

    @SdkTestInternalApi
    static SafePoolingHttpClientConnectionManager create(HttpClientConnectionOperator connectionOperator,
                                                         SafeStrictConnPool strictPool) {
        return new SafePoolingHttpClientConnectionManager(connectionOperator, strictPool);
    }

    private static SafeStrictConnPool newPool() {
        return new SafeStrictConnPool(DEFAULT_MAX_CONNECTIONS_PER_ROUTE,
                                      DEFAULT_MAX_TOTAL_CONNECTIONS,
                                      null,
                                      PoolReusePolicy.LIFO,
                                      new DefaultDisposalCallback<>(),
                                      null);
    }

    /**
     * Finishes a close that failed part way and closes the idle connections it left open. Called only on a manager
     * whose close() threw.
     *
     * <p>PoolingHttpClientConnectionManager.close marks the manager closed before it closes the pool. If it failed before
     * StrictConnPool.close began, the pool would otherwise keep serving leases (the manager's lease does not check
     * isClosed) while the manager's release silently drops every connection returned to it. StrictConnPool.close is
     * guarded by a compare-and-set, so the first call below is a no-op whenever the interrupted close had already
     * reached the pool.
     *
     * <p>PoolingHttpClientConnectionManager.closeIdle returns early once the manager is closed, so it can never reach
     * the idle connections an interrupted StrictConnPool.close leaves behind. StrictConnPool.closeIdle does not check
     * the pool's shut-down flag, and an interrupted StrictConnPool.close leaves every idle entry in the list it walks.
     * It runs even if the close fails again, so that a second failure still reclaims what it left idle.
     */
    void reclaimAfterFailedClose() {
        try {
            // A no-op unless the pool never started closing.
            strictPool.close(CloseMode.IMMEDIATE);
        } finally {
            strictPool.closeIdle(TimeValue.ZERO_MILLISECONDS);
        }
    }
}
