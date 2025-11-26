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

package software.amazon.awssdk.services.signin.auth.internal;

import java.time.Instant;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.services.signin.internal.LoginAccessToken;

public class LoginAccessTokenTest {
    @Test
    public void toStringDoesNotContainSensitiveFields() {
        LoginAccessToken token = LoginAccessToken
            .builder()
            .accessToken(AwsSessionCredentials
                             .builder()
                             .accessKeyId("accessKeyId")
                             .secretAccessKey("SECRET")
                             .sessionToken("SECRET")
                             .expirationTime(Instant.now())
                             .build())
            .clientId("clientId")
            .identityToken("identityToken")
            .refreshToken("SECRET")
            .identityToken("identityToken")
            .tokenType("tokenType")
            .dpopKey("SECRET")
            .build();
        Assertions.assertThat(token.toString()).doesNotContain("SECRET");
    }

}
