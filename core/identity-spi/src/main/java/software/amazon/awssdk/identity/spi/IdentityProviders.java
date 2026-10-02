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

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.identity.spi.internal.DefaultIdentityProviders;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * An interface to allow retrieving an {@link IdentityProvider} based on the identity type.
 *
 * <p>
 * {@code IdentityProviders} is a registry that maps identity types to their corresponding identity providers.
 * It is used by {@link software.amazon.awssdk.http.auth.spi.scheme.AuthScheme}s to retrieve the appropriate
 * identity provider for resolving authentication identities.
 *
 * <p>
 * <b>How It Works</b>
 * <p>
 * When an auth scheme needs to resolve an identity, it calls {@link #identityProvider(Class)} with the identity
 * type it requires (e.g., {@link AwsCredentialsIdentity}.class). The registry returns the corresponding provider
 * that was configured on the client.
 *
 * <p>
 * <b>Default Configuration</b>
 * <p>
 * The SDK automatically configures identity providers based on the client configuration:
 * <ul>
 *     <li>When you set {@code credentialsProvider()} on the client builder, it registers an
 *         {@link AwsCredentialsIdentity} provider</li>
 *     <li>When you set {@code tokenProvider()} on the client builder, it registers a {@link TokenIdentity} provider</li>
 * </ul>
 *
 * <p>
 * <b>Usage in Auth Schemes</b>
 * <p>
 * Auth schemes use {@code IdentityProviders} to retrieve the appropriate identity provider for their
 * identity type.
 *
 * <p>
 * Example - Auth scheme using IdentityProviders:
 *
 * {@snippet :
 * public class CustomAuthScheme implements AwsV4AuthScheme {
 *     @Override
 *     public String schemeId() {
 *         return AwsV4AuthScheme.SCHEME_ID;
 *     }
 *
 *     @Override
 *     public IdentityProvider<AwsCredentialsIdentity> identityProvider(IdentityProviders providers) {
 *         // Retrieve the AWS credentials provider from the registry
 *         return providers.identityProvider(AwsCredentialsIdentity.class);
 *     }
 *
 *     @Override
 *     public AwsV4HttpSigner signer() {
 *         return new CustomSigV4Signer();
 *     }
 * }
 * }
 *
 * <p>
 * <b>Standard Identity Types</b>
 * <p>
 * The SDK provides standard identity types:
 * <ul>
 *     <li>{@link AwsCredentialsIdentity} - AWS access key credentials</li>
 *     <li>{@link TokenIdentity} - Bearer tokens</li>
 *     <li>{@link AwsSessionCredentialsIdentity} - AWS credentials with session token</li>
 * </ul>
 *
 * @see IdentityProvider
 * @see Identity
 * @see software.amazon.awssdk.http.auth.spi.scheme.AuthScheme
 */
@SdkPublicApi
public interface IdentityProviders extends ToCopyableBuilder<IdentityProviders.Builder, IdentityProviders> {

    /**
     * Retrieve an identity provider for the provided identity type.
     */
    <T extends Identity> IdentityProvider<T> identityProvider(Class<T> identityType);

    /**
     * Get a new builder for creating a {@link IdentityProviders}.
     */
    static Builder builder() {
        return DefaultIdentityProviders.builder();
    }

    /**
     * A builder for a {@link IdentityProviders}.
     */
    interface Builder extends CopyableBuilder<Builder, IdentityProviders> {

        /**
         * Add the {@link IdentityProvider} for a given type. If a provider of that type, as determined by {@link
         * IdentityProvider#identityType()} is already added, it will be replaced.
         */
        <T extends Identity> Builder putIdentityProvider(IdentityProvider<T> identityProvider);
    }
}
