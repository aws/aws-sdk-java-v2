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

package software.amazon.awssdk.auth.token.credentials;

import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;
import software.amazon.awssdk.identity.spi.TokenIdentity;

/**
 * Interface for loading {@link SdkToken} that are used for authentication.
 *
 */
@FunctionalInterface
@SdkPublicApi
public interface SdkTokenProvider extends IdentityProvider<TokenIdentity> {
    /**
     * Returns an {@link SdkToken} that can be used to authorize a request. Each implementation of SdkTokenProvider
     * can choose its own strategy for loading token. For example, an implementation might load token from an existing
     * key management system, or load new token when token is refreshed.
     *
     *
     * @return AwsToken which the caller can use to authorize an AWS request using token authorization for a request.
     */
    SdkToken resolveToken();

    @Override
    default Class<TokenIdentity> identityType() {
        return TokenIdentity.class;
    }

    @Override
    default CompletableFuture<TokenIdentity> resolveIdentity(ResolveIdentityRequest request) {
        return CompletableFuture.completedFuture(resolveToken());
    }
}
