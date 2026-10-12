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

import java.util.concurrent.Future;
import org.apache.hc.client5.http.HttpRoute;
import org.apache.hc.client5.http.io.ManagedHttpClientConnection;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.pool.ConnPoolListener;
import org.apache.hc.core5.pool.DisposalCallback;
import org.apache.hc.core5.pool.PoolEntry;
import org.apache.hc.core5.pool.PoolReusePolicy;
import org.apache.hc.core5.pool.StrictConnPool;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.apache5.internal.utils.CancelOnInterruptWrapper;

/**
 * Specialization of {@link StrictConnPool} that prevents leaking the connection when thread waiting on the future is
 * interrupted.
 */
@SdkInternalApi
public final class SafeStrictConnPool extends StrictConnPool<HttpRoute, ManagedHttpClientConnection> {
    public SafeStrictConnPool(int defaultMaxPerRoute,
                              int maxTotal,
                              TimeValue timeToLive,
                              PoolReusePolicy policy,
                              DisposalCallback<ManagedHttpClientConnection> disposalCallback,
                              ConnPoolListener<HttpRoute> connPoolListener) {
        super(defaultMaxPerRoute, maxTotal, timeToLive, policy, disposalCallback, connPoolListener);
    }

    public Future<PoolEntry<HttpRoute, ManagedHttpClientConnection>> lease(HttpRoute route,
                                                                           Object state,
                                                                           Timeout requestTimeout,
                                                                           FutureCallback<PoolEntry<HttpRoute,
                                                                               ManagedHttpClientConnection>> callback) {
        return safeLease(super.lease(route, state, requestTimeout, callback));
    }

    private Future<PoolEntry<HttpRoute, ManagedHttpClientConnection>> safeLease(
        Future<PoolEntry<HttpRoute, ManagedHttpClientConnection>> leaseFuture) {
        return new CancelOnInterruptWrapper<>(leaseFuture);
    }
}
