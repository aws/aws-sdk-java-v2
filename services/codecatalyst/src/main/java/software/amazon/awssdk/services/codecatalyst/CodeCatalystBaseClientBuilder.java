/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with
 * the License. A copy of the License is located at
 * 
 * http://aws.amazon.com/apache2.0
 * 
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package software.amazon.awssdk.services.codecatalyst;

import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.auth.token.credentials.SdkTokenProvider;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.TokenIdentity;
import software.amazon.awssdk.services.codecatalyst.auth.scheme.CodeCatalystAuthSchemeProvider;
import software.amazon.awssdk.services.codecatalyst.endpoints.CodeCatalystEndpointProvider;

/**
 * This includes configuration specific to Amazon CodeCatalyst that is supported by both
 * {@link CodeCatalystClientBuilder} and {@link CodeCatalystAsyncClientBuilder}.
 */
@Generated("software.amazon.awssdk:codegen")
public interface CodeCatalystBaseClientBuilder<B extends CodeCatalystBaseClientBuilder<B, C>, C> extends AwsClientBuilder<B, C> {
    /**
     * Set the {@link CodeCatalystEndpointProvider} implementation that will be used by the client to determine the
     * endpoint for each request. This is optional; if none is provided a default implementation will be used the SDK.
     */
    default B endpointProvider(CodeCatalystEndpointProvider endpointProvider) {
        throw new UnsupportedOperationException();
    }

    /**
     * Set the {@link CodeCatalystAuthSchemeProvider} implementation that will be used by the client to resolve the auth
     * scheme for each request. This is optional; if none is provided a default implementation will be used the SDK.
     */
    default B authSchemeProvider(CodeCatalystAuthSchemeProvider authSchemeProvider) {
        throw new UnsupportedOperationException();
    }

    /**
     * Set the token provider to use for bearer token authorization. This is optional, if none is provided, the SDK will
     * use {@link software.amazon.awssdk.auth.token.credentials.aws.DefaultAwsTokenProvider}.
     * <p>
     * If the service, or any of its operations require Bearer Token Authorization, then the SDK will default to this
     * token provider to retrieve the token to use for authorization.
     * <p>
     * This provider works in conjunction with the
     * {@code software.amazon.awssdk.core.client.config.SdkAdvancedClientOption.TOKEN_SIGNER} set on the client. By
     * default it is {@link software.amazon.awssdk.auth.token.signer.aws.BearerTokenSigner}.
     */
    default B tokenProvider(SdkTokenProvider tokenProvider) {
        return tokenProvider((IdentityProvider<? extends TokenIdentity>) tokenProvider);
    }

    /**
     * Set the token provider to use for bearer token authorization. This is optional, if none is provided, the SDK will
     * use {@link software.amazon.awssdk.auth.token.credentials.aws.DefaultAwsTokenProvider}.
     * <p>
     * If the service, or any of its operations require Bearer Token Authorization, then the SDK will default to this
     * token provider to retrieve the token to use for authorization.
     * <p>
     * This provider works in conjunction with the
     * {@code software.amazon.awssdk.core.client.config.SdkAdvancedClientOption.TOKEN_SIGNER} set on the client. By
     * default it is {@link software.amazon.awssdk.auth.token.signer.aws.BearerTokenSigner}.
     */
    default B tokenProvider(IdentityProvider<? extends TokenIdentity> tokenProvider) {
        throw new UnsupportedOperationException();
    }
}
