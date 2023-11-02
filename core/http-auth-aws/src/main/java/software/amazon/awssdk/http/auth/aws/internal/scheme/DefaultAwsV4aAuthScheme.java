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

package software.amazon.awssdk.http.auth.aws.internal.scheme;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.auth.aws.scheme.AwsV4aAuthScheme;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4aHttpSigner;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.IdentityProviders;

/**
 * A default implementation of {@link AwsV4aAuthScheme}.
 */
@SdkInternalApi
public final class DefaultAwsV4aAuthScheme implements AwsV4aAuthScheme {
    private static final DefaultAwsV4aAuthScheme DEFAULT = new DefaultAwsV4aAuthScheme();

    /**
     * Returns an instance of the {@link DefaultAwsV4aAuthScheme}.
     */
    public static DefaultAwsV4aAuthScheme create() {
        return DEFAULT;
    }

    @Override
    public String schemeId() {
        return SCHEME_ID;
    }

    @Override
    public IdentityProvider<AwsCredentialsIdentity> identityProvider(IdentityProviders providers) {
        return providers.identityProvider(AwsCredentialsIdentity.class);
    }

    /**
     * AwsV4aHttpSigner.create() returns the CRT implementation and requires the optional dependency http-auth-aws-crt to be
     * added. So lazily creating the instance only when this method is called.
     */
    @Override
    public AwsV4aHttpSigner signer() {
        return SignerSingletonHolder.INSTANCE;
    }

    private static class SignerSingletonHolder {
        private static final AwsV4aHttpSigner INSTANCE = AwsV4aHttpSigner.create();
    }
}
