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

package software.amazon.awssdk.auth.credentials.internal;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.cache.CachedSupplier;

/**
 * Utility methods for credential provider invalidation logic.
 *
 * <p>This class provides shared implementations for the common pattern of comparing a rejected
 * identity's access key ID against a cached credential and conditionally invalidating the cache.</p>
 */
@SdkProtectedApi
public final class CredentialsInvalidationUtils {

    private CredentialsInvalidationUtils() {
    }

    /**
     * Invalidates the given cache if the rejected identity's access key ID matches the access key ID of the cached value.
     *
     * <p>This method encapsulates the common pattern used by caching credential providers:
     * <ol>
     *   <li>Extract the rejected access key ID from the identity</li>
     *   <li>Use the {@code accessKeyIdExtractor} to get the access key ID from the currently cached value</li>
     *   <li>If they match, mark the cache for mandatory refresh</li>
     * </ol>
     *
     * <p>For providers whose cache directly stores {@link AwsCredentialsIdentity} (or a subtype), pass
     * {@code AwsCredentialsIdentity::accessKeyId} as the extractor. For providers that cache a wrapper type,
     * provide an appropriate extraction function (e.g., {@code holder -> holder.sessionCredentials().accessKeyId()}).
     *
     * @param identity The identity that was rejected by the service.
     * @param cache The cached supplier to invalidate.
     * @param accessKeyIdExtractor A function that extracts the access key ID from the cached value.
     * @param <T> The type of value stored in the cache.
     * @return A {@link CompletableFuture} that completes when the invalidation check is done, or completes exceptionally
     *         if an error occurs during the invalidation.
     */
    public static <T> CompletableFuture<Void> invalidateCredentialsCache(
            AwsCredentialsIdentity identity,
            CachedSupplier<T> cache,
            Function<T, String> accessKeyIdExtractor) {

        try {
            String rejectedAccessKeyId = identity.accessKeyId();
            cache.invalidate(cachedValue -> rejectedAccessKeyId.equals(accessKeyIdExtractor.apply(cachedValue)));
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            return CompletableFutureUtils.failedFuture(e);
        }
    }
}
