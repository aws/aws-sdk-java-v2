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

package software.amazon.awssdk.http.apache5;

import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * Controls whether an {@link Apache5HttpClient} rebuilds its connection pool after the underlying Apache HTTP client
 * shuts it down.
 *
 * <p>Apache deliberately shuts its connection pool down when a {@link java.lang.Error}, such as an
 * {@link OutOfMemoryError} or a {@link StackOverflowError}, escapes a request. Without recovery, every later request on
 * the client fails with {@link software.amazon.awssdk.http.ConnectionPoolShutdownException} (reason
 * {@code SHUT_DOWN_AFTER_ERROR}) until the client is replaced. With recovery, the client discards the shut-down pool and
 * its connections and builds a new pool on a later request. Requests in flight when the pool is shut down still fail,
 * usually with an {@link java.io.IOException}, which the SDK retries if it happened before a response was returned.
 *
 * <p>Closing the client is always permanent, in every mode.
 *
 * <p>Constants may be added in future releases. Code that switches on this type should include a default branch.
 *
 * @see Apache5HttpClient.Builder#connectionPoolRecoveryMode(ConnectionPoolRecoveryMode)
 */
@SdkPublicApi
public enum ConnectionPoolRecoveryMode {
    /**
     * The default. The client never rebuilds its connection pool. After an Error shuts it down, every later request
     * fails with {@link software.amazon.awssdk.http.ConnectionPoolShutdownException} (reason
     * {@code SHUT_DOWN_AFTER_ERROR}). This is the safest choice when an Error means the application should restart.
     */
    DISABLED,

    /**
     * Recommended for most services that enable recovery. The client rebuilds the pool on the next request after the
     * first Error-driven shutdown, and then at most once every 5 minutes. If an Error shuts the pool down again within
     * 5 minutes of the last rebuild, the pool stays shut down until 5 minutes after that rebuild: requests in the
     * meantime fail immediately with {@link software.amazon.awssdk.http.ConnectionPoolShutdownException} (reason
     * {@code RECOVERY_BACKOFF}), and the first request after that rebuilds the pool. The 5-minute period is fixed.
     */
    WITH_BACKOFF,

    /**
     * The client rebuilds the pool on the next request after every Error-driven shutdown, with no backoff, so it never
     * fails with reason {@code RECOVERY_BACKOFF}.
     *
     * <p><b>Warning:</b> if an Error recurs on every request, for example a {@code StackOverflowError} raised by a request
     * body stream while the HTTP client is writing it, every such request destroys the pool again. Each time, the
     * requests in flight on other threads fail and a new pool (with a new TLS context) is built. Prefer
     * {@link #WITH_BACKOFF} unless the application is known to recover from every Error that can reach the HTTP client.
     */
    ALWAYS
}
