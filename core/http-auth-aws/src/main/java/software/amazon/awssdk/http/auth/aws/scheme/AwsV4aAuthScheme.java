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

package software.amazon.awssdk.http.auth.aws.scheme;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.http.auth.aws.internal.scheme.DefaultAwsV4aAuthScheme;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4aHttpSigner;
import software.amazon.awssdk.http.auth.spi.scheme.AuthScheme;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.IdentityProviders;

/**
 * The <a href="https://smithy.io/2.0/aws/aws-auth.html#aws-auth-sigv4a-trait">aws.auth#sigv4a</a> auth scheme, which uses a
 * {@link AwsCredentialsIdentity} and AwsV4aHttpSigner.
 */
@SdkPublicApi
public interface AwsV4aAuthScheme extends AuthScheme<AwsCredentialsIdentity> {

    /**
     * The scheme ID for this interface.
     */
    String SCHEME_ID = "aws.auth#sigv4a";

    /**
     * Get a default implementation of a {@link AwsV4aAuthScheme}
     */
    static AwsV4aAuthScheme create() {
        return DefaultAwsV4aAuthScheme.create();
    }

    /**
     * Retrieve the {@link AwsCredentialsIdentity} based {@link IdentityProvider} associated with this authentication scheme.
     */
    @Override
    IdentityProvider<AwsCredentialsIdentity> identityProvider(IdentityProviders providers);

    /**
     * Retrieve the {@link AwsV4aHttpSigner} associated with this authentication scheme.
     */
    @Override
    AwsV4aHttpSigner signer();
}
