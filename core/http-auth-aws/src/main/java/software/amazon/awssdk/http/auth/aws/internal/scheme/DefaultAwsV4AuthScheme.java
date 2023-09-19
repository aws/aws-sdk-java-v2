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
import software.amazon.awssdk.http.auth.aws.scheme.AwsV4AuthScheme;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.IdentityProviders;

/**
 * A default implementation of {@link AwsV4AuthScheme}.
 */
@SdkInternalApi
public final class DefaultAwsV4AuthScheme implements AwsV4AuthScheme {
    private static final DefaultAwsV4AuthScheme DEFAULT = new DefaultAwsV4AuthScheme();
    private static final AwsV4HttpSigner DEFAULT_SIGNER = AwsV4HttpSigner.create();

    /**
     * Returns an instance of the {@link DefaultAwsV4AuthScheme}.
     */
    public static DefaultAwsV4AuthScheme create() {
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

    @Override
    public AwsV4HttpSigner signer() {
        return DEFAULT_SIGNER;
    }
}
