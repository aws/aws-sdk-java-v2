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
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;
import software.amazon.awssdk.identity.spi.TokenIdentity;


/**
 * Interface for loading a {@link TokenIdentity} that can be used for authentication. This interface has been
 * superseded by {@link IdentityProvider}{@code <}{@link TokenIdentity}{@code >}.
 *
 * <p>
 * To avoid unnecessary churn this class has not been marked as deprecated, but it's recommended to use
 * {@link IdentityProvider}{@code <}{@link TokenIdentity}{@code >} when defining generic credential providers because it
 * provides the same functionality with considerably fewer dependencies.
 */
@FunctionalInterface
@SdkPublicApi
public interface SdkTokenProvider extends IdentityProvider<TokenIdentity> {
    /**
     * Returns {@link AwsCredentials} that can be used to authorize a bearer token request.
     *
     * <p>
     * If an error occurs during the loading of credentials or credentials could not be found, a runtime exception will be
     * raised.
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
