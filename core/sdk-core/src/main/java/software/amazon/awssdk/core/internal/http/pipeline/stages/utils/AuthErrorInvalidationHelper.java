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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
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
 * <p>When a service returns {@code ExpiredToken}, {@code InvalidToken}, or {@code AuthFailure},
 * this helper retrieves the {@link SelectedAuthScheme} from the execution context and calls
 * {@link IdentityProvider#invalidate} so the next retry attempt resolves fresh credentials.</p>
 *
 * <p>All exceptions from the invalidation path are caught and logged at debug level.
 * Invalidation failures never disrupt the normal request/retry flow.</p>
 */
@SdkInternalApi
public final class AuthErrorInvalidationHelper {

    private static final Logger LOG = Logger.loggerFor(AuthErrorInvalidationHelper.class);

    private static final Set<String> INVALIDATION_ERROR_CODES = new HashSet<>(Arrays.asList(
        "ExpiredToken",
        "InvalidToken",
        "AuthFailure"
    ));

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
        String errorCode = extractErrorCode(serviceException);

        if (errorCode == null || !INVALIDATION_ERROR_CODES.contains(errorCode)) {
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

    /**
     * Extracts the error code from an SdkServiceException. Since sdk-core does not depend on
     * aws-core, this uses reflection to access awsErrorDetails().errorCode() which is defined
     * on AwsServiceException.
     */
    private static String extractErrorCode(SdkServiceException serviceException) {
        try {
            java.lang.reflect.Method awsErrorDetailsMethod =
                serviceException.getClass().getMethod("awsErrorDetails");
            Object errorDetails = awsErrorDetailsMethod.invoke(serviceException);
            if (errorDetails == null) {
                return null;
            }
            java.lang.reflect.Method errorCodeMethod = errorDetails.getClass().getMethod("errorCode");
            Object errorCode = errorCodeMethod.invoke(errorDetails);
            return errorCode instanceof String ? (String) errorCode : null;
        } catch (NoSuchMethodException e) {
            // Exception type does not have awsErrorDetails() — not an AWS service exception
            return null;
        } catch (Exception e) {
            LOG.debug(() -> "Failed to extract error code from exception: " + e.getMessage(), e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Identity> void doInvalidate(SelectedAuthScheme<T> selectedAuthScheme) {
        T resolvedIdentity = CompletableFutureUtils.joinLikeSync(selectedAuthScheme.identity());
        IdentityProvider<T> provider = selectedAuthScheme.identityProvider();
        provider.invalidate(resolvedIdentity);
    }
}
