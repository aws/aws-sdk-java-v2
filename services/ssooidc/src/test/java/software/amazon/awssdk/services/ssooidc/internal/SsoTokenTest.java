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

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

public class SsoTokenTest {
    @Test
    public void equalsAndHashCode_workCorrectly() {
        EqualsVerifier.forClass(SsoToken.class)
                      .usingGetClass()
                      .verify();
    }

    @Test
    public void builder_maximal() {
        String accessToken = "accesstoken";
        Instant expiresAt = Instant.now();
        String refreshToken = "refreshtoken";
        String clientId = "clientid";
        String clientSecret = "clientsecret";
        Instant registrationExpiresAt = expiresAt.plus(Duration.ofHours(1));
        String region = "region";
        String startUrl = "starturl";

        SsoToken ssoToken = SsoToken.builder()
                                    .accessToken(accessToken)
                                    .expiresAt(expiresAt)
                                    .refreshToken(refreshToken)
                                    .clientId(clientId)
                                    .clientSecret(clientSecret)
                                    .registrationExpiresAt(registrationExpiresAt)
                                    .region(region)
                                    .startUrl(startUrl)
                                    .build();

        assertThat(ssoToken.token()).isEqualTo(accessToken);
        assertThat(ssoToken.expirationTime()).isEqualTo(expiresAt);
        assertThat(ssoToken.refreshToken()).isEqualTo(refreshToken);
        assertThat(ssoToken.clientId()).isEqualTo(clientId);
        assertThat(ssoToken.clientSecret()).isEqualTo(clientSecret);
        assertThat(ssoToken.registrationExpiresAt()).isEqualTo(registrationExpiresAt);
        assertThat(ssoToken.region()).isEqualTo(region);
        assertThat(ssoToken.startUrl()).isEqualTo(startUrl);
    }
}
