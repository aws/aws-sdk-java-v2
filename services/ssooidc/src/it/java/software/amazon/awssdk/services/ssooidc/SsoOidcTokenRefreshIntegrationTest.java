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



import org.junit.jupiter.api.Disabled;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssooidc.internal.common.SsoOidcTokenRefreshTestBase;

@Disabled("Disabled since base registered tokens comes with expiration date and registration of token requires manual login")
public class SsoOidcTokenRefreshIntegrationTest extends SsoOidcTokenRefreshTestBase {


    // TODO : Remove this gamma specific startUrl and endpoint before GA release.
    @Override
    public void initializeClient() {
        shouldMockServiceClient = false;
        // Follow registration steps to get the base token.
        baseTokenResourceFile = "src/it/resources/baseCreateToken.json";
        testStartUrl = "http://caws-sono-testing.awsapps.com/start-beta";

        ssoOidcClient = SsoOidcClient.builder()
                                     .region(Region.of(REGION))
                                     .credentialsProvider(AnonymousCredentialsProvider.create())
                                     .build();
    }
}
