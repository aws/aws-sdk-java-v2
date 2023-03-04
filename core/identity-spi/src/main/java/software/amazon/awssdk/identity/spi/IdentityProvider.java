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

package software.amazon.awssdk.identity.spi;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;

/**
 * Interface for loading {@link Identity} that is used for authentication.
 */
@SdkPublicApi
@ThreadSafe
public interface IdentityProvider<IdentityT extends Identity> {
    /**
     * Retrieve the class of identity this identity provider produces.
     *
     * This is necessary for the SDK core to determine which identity provider should be used to resolve a specific type of
     * identity.
     */
    Class<IdentityT> identityType();

    /**
     * Resolve the identity from this identity provider.
     * @param request The request to resolve an Identity
     */
    CompletableFuture<? extends IdentityT> resolveIdentity(ResolveIdentityRequest request);

    /**
     * Resolve the identity from this identity provider.
     *
     * Similar to {@link #resolveIdentity(ResolveIdentityRequest)}, but takes a lambda to configure a new
     * {@link ResolveIdentityRequest.Builder}. This removes the need to call {@link ResolveIdentityRequest#builder()} and
     * {@link ResolveIdentityRequest.Builder#build()}.
     *
     * @param consumer A {@link Consumer} to which an empty {@link ResolveIdentityRequest.Builder} will be given.
     */
    default CompletableFuture<? extends IdentityT> resolveIdentity(Consumer<ResolveIdentityRequest.Builder> consumer) {
        return resolveIdentity(ResolveIdentityRequest.builder().applyMutation(consumer).build());
    }

    /**
     * Resolve the identity from this identity provider.
     */
    default CompletableFuture<? extends IdentityT> resolveIdentity() {
        return resolveIdentity(ResolveIdentityRequest.builder().build());
    }
}
