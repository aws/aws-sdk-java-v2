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

package software.amazon.awssdk.identity.spi.internal;

import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;

@SdkInternalApi
public class StaticIdentityProvider<T extends Identity> implements IdentityProvider<T> {
    private final Class<T> identityType;
    private final CompletableFuture<T> identity;

    public StaticIdentityProvider(Class<T> identityType, T identity) {
        this.identityType = identityType;
        this.identity = CompletableFuture.completedFuture(identity);
    }

    @Override
    public Class<T> identityType() {
        return identityType;
    }

    @Override
    public CompletableFuture<? extends T> resolveIdentity(ResolveIdentityRequest request) {
        return identity;
    }
}
