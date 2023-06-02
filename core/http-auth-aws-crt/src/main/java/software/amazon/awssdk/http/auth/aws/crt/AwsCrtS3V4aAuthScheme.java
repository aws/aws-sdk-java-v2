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

package software.amazon.awssdk.http.auth.aws.crt;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.http.auth.spi.AuthScheme;
import software.amazon.awssdk.http.auth.spi.IdentityProviderConfiguration;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;

/**
 * The <a href="https://smithy.io/2.0/aws/aws-auth.html#aws-auth-sigv4-trait">aws.auth#sigv4</a>
 * auth scheme, which uses a {@link AwsCredentialsIdentity} and {@link AwsCrtV4aHttpSigner}.
 */
@SdkPublicApi
public interface AwsCrtS3V4aAuthScheme extends AuthScheme<AwsCredentialsIdentity> {

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
     * Retrieve the {@link AwsCrtV4aHttpSigner} associated with this authentication scheme.
     */
    @Override
    default AwsCrtV4aHttpSigner signer() {
        return AwsCrtV4aHttpSigner.create();
    }

    /**
     * Get a default implementation of a {@link AwsCrtS3V4aAuthScheme}
     */
    static AwsCrtS3V4aAuthScheme create() {
        return new AwsCrtS3V4aAuthScheme() {
        };
    }
}
