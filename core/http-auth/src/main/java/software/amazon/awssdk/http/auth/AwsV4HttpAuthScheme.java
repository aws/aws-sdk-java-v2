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

package software.amazon.awssdk.http.auth;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.http.auth.spi.HttpAuthScheme;
import software.amazon.awssdk.http.auth.spi.IdentityProviderConfiguration;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;

/**
 * The <a href="https://smithy.io/2.0/aws/aws-auth.html#aws-auth-sigv4-trait">aws.auth#sigv4</a>
 * auth scheme, which uses a {@link AwsCredentialsIdentity} and {@link AwsV4HttpSigner}.
 */
@SdkPublicApi
public interface AwsV4HttpAuthScheme extends HttpAuthScheme<AwsCredentialsIdentity> {

    /**
     * Retrieve the scheme ID.
     */
    @Override
    default String schemeId() {
        return "aws.auth#sigv4";
    }

    /**
     * Retrieve the {@link AwsCredentialsIdentity} based {@link IdentityProvider} associated with this authentication scheme.
     */
    @Override
    default IdentityProvider<AwsCredentialsIdentity> identityProvider(IdentityProviderConfiguration providers) {
        return providers.identityProvider(AwsCredentialsIdentity.class);
    }

    /**
     * Retrieve the {@link AwsV4HttpSigner} associated with this authentication scheme.
     */
    @Override
    default AwsV4HttpSigner signer() {
        return AwsV4HttpSigner.create();
    }

    /**
     * Get a default implementation of a {@link AwsV4HttpAuthScheme}
     */
    static AwsV4HttpAuthScheme create() {
        return new AwsV4HttpAuthScheme() {
        };
    }
}
