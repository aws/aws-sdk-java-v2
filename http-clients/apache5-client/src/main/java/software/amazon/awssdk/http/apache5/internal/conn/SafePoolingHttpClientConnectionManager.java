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
import org.apache.hc.core5.pool.DefaultDisposalCallback;
import org.apache.hc.core5.pool.PoolReusePolicy;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Specialization of {@link PoolingHttpClientConnectionManager} to enable use of {@link SafeStrictConnPool} to prevent leaking
 * connections when the thread waiting on the future is interrupted.
 */
@SdkInternalApi
public final class SafePoolingHttpClientConnectionManager extends PoolingHttpClientConnectionManager {
    public SafePoolingHttpClientConnectionManager(HttpClientConnectionOperator connectionOperator) {
        super(connectionOperator,
            new SafeStrictConnPool(
                DEFAULT_MAX_CONNECTIONS_PER_ROUTE,
                DEFAULT_MAX_TOTAL_CONNECTIONS,
                null,
                PoolReusePolicy.LIFO,
                new DefaultDisposalCallback<>(),
                null
            ),
            ManagedHttpClientConnectionFactory.INSTANCE
        );
    }
}
