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

import org.apache.hc.core5.util.TimeValue;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * What {@link IdleConnectionReaper} needs from the thing it reaps.
 *
 * <p>Apache 5.x declares {@code closeIdle} on the concrete {@code PoolingHttpClientConnectionManager} rather than on the
 * {@code HttpClientConnectionManager} interface. Reaping through this interface instead lets the reaper hold a stable
 * registration for the lifetime of an SDK HTTP client, even when the connection pool underneath is replaced - see
 * {@link RecreatingHttpClientConnectionManager}. Registering the pool itself would break deregistration in
 * {@code close()} once the pool had been swapped.
 *
 * <p>Apache 4.x needs no equivalent, because there {@code closeIdleConnections} is part of the connection manager
 * interface.
 */
@SdkInternalApi
@FunctionalInterface
public interface IdleConnectionCloser {

    /**
     * Closes connections that have been idle for longer than the given time.
     */
    void closeIdle(TimeValue idleTime);
}
