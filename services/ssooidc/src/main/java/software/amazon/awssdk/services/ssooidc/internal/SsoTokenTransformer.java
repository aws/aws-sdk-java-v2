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

package software.amazon.awssdk.services.ssooidc.internal;

import java.time.Instant;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.internal.token.TokenTransformer;
import software.amazon.awssdk.services.ssooidc.model.CreateTokenResponse;
import software.amazon.awssdk.utils.Validate;

/**
 * Transformer to tranform CreateTokenResponse to SsoToken.
 */
@SdkInternalApi
public final class SsoTokenTransformer implements TokenTransformer<SsoToken, CreateTokenResponse> {

    private final SsoToken baseToken;

    private SsoTokenTransformer(SsoToken baseToken) {
        Validate.notNull(baseToken.startUrl(), "startUrl is null ");
        Validate.notNull(baseToken.clientId(), "clientId is null ");
        Validate.notNull(baseToken.clientSecret(), "clientSecret is null ");
        this.baseToken = baseToken;
    }

    public static SsoTokenTransformer create(SsoToken baseToken) {
        Validate.paramNotNull(baseToken, "baseToken");
        return new SsoTokenTransformer(baseToken);
    }

    @Override
    public SsoToken transform(CreateTokenResponse awsResponse) {

        return SsoToken.builder()
                       .accessToken(awsResponse.accessToken())
                       .refreshToken(awsResponse.refreshToken())
                       .expiresAt(awsResponse.expiresIn() != null ? Instant.ofEpochSecond(awsResponse.expiresIn()) : null)
                       .startUrl(baseToken.startUrl())
                       .registrationExpiresAt(baseToken.registrationExpiresAt())
                       .region(baseToken.region())
                       .clientSecret(baseToken.clientSecret())
                       .clientId(baseToken.clientId())
                       .build();
    }
}
