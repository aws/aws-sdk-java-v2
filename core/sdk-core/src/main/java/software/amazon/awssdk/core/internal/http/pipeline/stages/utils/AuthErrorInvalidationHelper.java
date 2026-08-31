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

package software.amazon.awssdk.core.internal.http.pipeline.stages.utils;

import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.utils.Logger;

/**
 * Utility that detects authentication error responses and triggers credential invalidation
 * on the identity provider that produced the rejected credentials.
 *
 * <p>When a service returns an authentication error (as determined by
 * {@link SdkServiceException#isAuthenticationError()}), this helper retrieves the
 * {@link SelectedAuthScheme} from the execution context and calls
 * {@link IdentityProvider#invalidate} so that the provider refreshes before it vends credentials again.
 *
 * <p>Identity is resolved once per API call, by a stage that sits outside the retry loop, and every attempt of that
 * call reuses it. Invalidation therefore does not affect the attempt currently being retried; it takes effect on the
 * next API call that resolves credentials.
 *
 * <p>Both the synchronous and asynchronous request paths must call
 * {@link #invalidateIfAuthError(Throwable, RequestExecutionContext)} for the behavior to apply to both client types.
 *
 * <p>All exceptions from the invalidation path are caught and logged at debug level.
 * Invalidation failures never disrupt the normal request/retry flow.
 */
@SdkInternalApi
public final class AuthErrorInvalidationHelper {

    private static final Logger LOG = Logger.loggerFor(AuthErrorInvalidationHelper.class);

    private AuthErrorInvalidationHelper() {
    }

    /**
     * Checks whether the given exception is an auth error that should trigger
     * credential invalidation. If so, retrieves the identity provider from the
     * {@link SelectedAuthScheme} and calls invalidate() on it.
     *
     * <p>This never blocks the calling thread: it composes on the resolved identity future rather than joining it, so
     * it is safe to call from the async request path, which runs on I/O threads. In practice the identity is already
     * resolved by the time a response has been received, so the invalidation completes inline.
     *
     * <p>The returned future never completes exceptionally. Invalidation is best-effort, and any failure is logged at
     * debug level instead of being propagated. Callers are not required to await it: identity is resolved outside the
     * retry loop, so a pending invalidation cannot affect the attempt currently being retried.
     *
     * @param exception The exception from the failed request attempt
     * @param context   The request execution context containing auth scheme info
     * @return A future completing when the invalidation attempt has finished. Never completes exceptionally.
     */
    public static CompletableFuture<Void> invalidateIfAuthError(Throwable exception, RequestExecutionContext context) {
        SelectedAuthScheme<?> selectedAuthScheme = authSchemeToInvalidate(exception, context);
        if (selectedAuthScheme == null) {
            return CompletableFuture.completedFuture(null);
        }

        try {
            return doInvalidate(selectedAuthScheme);
        } catch (Exception e) {
            LOG.debug(() -> "Failed to invalidate identity provider after auth error: " + e.getMessage(), e);
            return CompletableFuture.completedFuture(null);
        }
    }

    /**
     * Returns the {@link SelectedAuthScheme} whose identity provider should be invalidated in response to the given
     * exception, or null if the exception is not an authentication failure or there is no provider to invalidate.
     */
    private static SelectedAuthScheme<?> authSchemeToInvalidate(Throwable exception, RequestExecutionContext context) {
        if (!(exception instanceof SdkServiceException)) {
            return null;
        }

        SdkServiceException serviceException = (SdkServiceException) exception;
        if (!serviceException.isAuthenticationError()) {
            return null;
        }

        SelectedAuthScheme<?> selectedAuthScheme =
            context.executionAttributes().getAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME);

        if (selectedAuthScheme == null || selectedAuthScheme.identityProvider() == null) {
            return null;
        }

        return selectedAuthScheme;
    }

    private static <T extends Identity> CompletableFuture<Void> doInvalidate(SelectedAuthScheme<T> selectedAuthScheme) {
        IdentityProvider<T> provider = selectedAuthScheme.identityProvider();
        // thenCompose rather than a join on the identity future: by the time a response has been received the identity
        // is already resolved, so this completes inline, but it stays non-blocking if it ever is not.
        // A synchronous throw from invalidate(), and a failed identity future, both complete the composed future
        // exceptionally, so exceptionally() covers every failure mode.
        return selectedAuthScheme.identity()
                                 .thenCompose(provider::invalidate)
                                 .exceptionally(e -> {
                                     LOG.debug(() -> "Failed to invalidate identity provider: " + e.getMessage(), e);
                                     return null;
                                 });
    }
}
