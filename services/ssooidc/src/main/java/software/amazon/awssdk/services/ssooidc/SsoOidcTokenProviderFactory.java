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

package software.amazon.awssdk.services.ssooidc;

import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.auth.token.AwsTokenProvider;
import software.amazon.awssdk.auth.token.SsoOidcTokenProviderFactoryProperties;
import software.amazon.awssdk.auth.token.SsoTokenProviderFactory;
import software.amazon.awssdk.services.ssooidc.internal.SsoTokenProvider;

/**
 * Factory for creating {@link SsoTokenProvider}.
 */
@SdkProtectedApi
public final class SsoOidcTokenProviderFactory implements SsoTokenProviderFactory {
    @Override
    public AwsTokenProvider create(SsoOidcTokenProviderFactoryProperties properties) {
        return SsoTokenProvider.builder()
                               .startUrl(properties.startUrl())
                               .region(properties.region())
                               .build();
    }
}
