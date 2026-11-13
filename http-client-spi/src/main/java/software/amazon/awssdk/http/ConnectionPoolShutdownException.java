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

package software.amazon.awssdk.http;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.Validate;

/**
 * Thrown by an SDK HTTP client when a request cannot be executed because the client's connection pool is shut down, so
 * the client cannot serve requests right now. {@link #reason()} reports why, and whether the condition is permanent:
 * <ul>
 *   <li>{@link Reason#CLIENT_CLOSED} (permanent): the HTTP client was closed, for example by closing the SDK service
 *       client that manages it. This usually means the application used a client after closing it.</li>
 *   <li>{@link Reason#SHUT_DOWN_AFTER_ERROR} (permanent): a {@link java.lang.Error}, such as an
 *       {@link OutOfMemoryError} or a {@link StackOverflowError}, escaped a request and the underlying HTTP library
 *       shut its connection pool down in response. Connection pool recovery is not enabled, or is not supported by this
 *       HTTP client, so this client never successfully executes another request. HTTP clients that support recovery
 *       expose it on their builder, for example {@code Apache5HttpClient.Builder#connectionPoolRecoveryMode}.</li>
 *   <li>{@link Reason#RECOVERY_BACKOFF} (temporary): the HTTP client rebuilds its pool after an Error, but with a
 *       backoff, for example the Apache 5 client's {@code WITH_BACKOFF} connection pool recovery mode. An Error shut the
 *       pool down again soon after the client last rebuilt it, so requests fail with this reason until the backoff
 *       period (5 minutes for the Apache 5 client) has passed since that rebuild. The first request after that rebuilds
 *       the pool.</li>
 * </ul>
 *
 * <p>Applications can use this exception to detect a broken client and react, for example by replacing the HTTP client,
 * or by failing health checks and restarting when the reason is permanent. A {@link Reason#RECOVERY_BACKOFF} means
 * Errors are recurring in the application, so some applications may also choose to restart on it.
 *
 * <p>For compatibility with code written before this type existed, this is an {@link IllegalStateException} whose
 * message is exactly {@code "Connection pool shut down"}. The exception raised by the underlying HTTP library is
 * available as the {@link #getCause() cause}.
 *
 * <p>Synchronous SDK service clients do not retry this exception, so requests fail fast while it applies, and they do not
 * wrap it in an {@code software.amazon.awssdk.core.exception.SdkClientException}. An execution interceptor or
 * application code may wrap it, so to detect it reliably, walk the cause chain:
 *
 * <pre class="brush: java">
 * static boolean isPermanentlyBroken(Throwable t) {
 *     for (Throwable c = t; c != null; c = c.getCause()) {
 *         if (c instanceof ConnectionPoolShutdownException) {
 *             ConnectionPoolShutdownException.Reason reason = ((ConnectionPoolShutdownException) c).reason();
 *             return reason == ConnectionPoolShutdownException.Reason.CLIENT_CLOSED
 *                    || reason == ConnectionPoolShutdownException.Reason.SHUT_DOWN_AFTER_ERROR;
 *         }
 *     }
 *     return false;
 * }
 * </pre>
 */
@SdkPublicApi
public final class ConnectionPoolShutdownException extends IllegalStateException {
    private static final long serialVersionUID = 1L;

    private static final String MESSAGE = "Connection pool shut down";

    private final Reason reason;

    private ConnectionPoolShutdownException(Reason reason, Throwable cause) {
        super(MESSAGE, cause);
        this.reason = Validate.paramNotNull(reason, "reason");
    }

    /**
     * Creates an exception reporting that a connection pool is shut down for the given reason.
     *
     * @param reason why the pool is shut down. Must not be null.
     * @param cause the exception raised by the underlying HTTP library, or null if there is none.
     * @return the new exception
     */
    public static ConnectionPoolShutdownException create(Reason reason, Throwable cause) {
        return new ConnectionPoolShutdownException(reason, cause);
    }

    /**
     * Returns why the connection pool is shut down, which also tells whether the condition is permanent.
     *
     * @return the reason, never null
     */
    public Reason reason() {
        return reason;
    }

    /**
     * Why a connection pool is shut down. Constants may be added in future releases; code that switches on this type
     * should include a default branch.
     */
    public enum Reason {
        /** Permanent. The HTTP client was closed. */
        CLIENT_CLOSED,
        /**
         * Permanent. A {@link java.lang.Error} escaped a request, the HTTP library shut the pool down, and connection pool
         * recovery is not enabled or not supported by this HTTP client.
         */
        SHUT_DOWN_AFTER_ERROR,
        /**
         * Temporary. The HTTP client rebuilds its pool with a backoff (for example the Apache 5 client's
         * {@code WITH_BACKOFF} mode), and an Error shut the pool down again within the backoff period. The client
         * rebuilds the pool on the first request after that period ends.
         */
        RECOVERY_BACKOFF
    }
}
