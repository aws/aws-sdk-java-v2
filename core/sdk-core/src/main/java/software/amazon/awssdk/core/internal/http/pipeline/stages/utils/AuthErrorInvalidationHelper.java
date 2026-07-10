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

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.Logger;

/**
 * Utility that detects authentication error responses and triggers credential invalidation
 * on the identity provider that produced the rejected credentials.
 *
 * <p>When a service returns an authentication error (as determined by
 * {@link SdkServiceException#isAuthenticationError()}), this helper retrieves the
 * {@link SelectedAuthScheme} from the execution context and calls
 * {@link IdentityProvider#invalidate} so the next retry attempt resolves fresh credentials.
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
     * @param exception The exception from the failed request attempt
     * @param context   The request execution context containing auth scheme info
     */
    public static void invalidateIfAuthError(Throwable exception, RequestExecutionContext context) {
        if (!(exception instanceof SdkServiceException)) {
            return;
        }

        SdkServiceException serviceException = (SdkServiceException) exception;
        if (!serviceException.isAuthenticationError()) {
            return;
        }

        SelectedAuthScheme<?> selectedAuthScheme =
            context.executionAttributes().getAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME);

        if (selectedAuthScheme == null || selectedAuthScheme.identityProvider() == null) {
            return;
        }

        try {
            doInvalidate(selectedAuthScheme);
        } catch (Exception e) {
            LOG.debug(() -> "Failed to invalidate identity provider after auth error: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Identity> void doInvalidate(SelectedAuthScheme<T> selectedAuthScheme) {
        T resolvedIdentity = CompletableFutureUtils.joinLikeSync(selectedAuthScheme.identity());
        IdentityProvider<T> provider = selectedAuthScheme.identityProvider();
        try {
            CompletableFutureUtils.joinLikeSync(provider.invalidate(resolvedIdentity));
        } catch (Exception e) {
            LOG.debug(() -> "Failed to invalidate identity provider: " + e.getMessage(), e);
        }
    }
}
