/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.sts;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import software.amazon.awssdk.core.SdkGlobalTime;
import software.amazon.awssdk.services.sts.model.GetFederationTokenRequest;
import software.amazon.awssdk.services.sts.model.GetFederationTokenResponse;
import software.amazon.awssdk.services.sts.model.GetSessionTokenRequest;
import software.amazon.awssdk.services.sts.model.GetSessionTokenResponse;


public class SecurityTokenServiceIntegrationTest extends IntegrationTestBase {

    private static final int SESSION_DURATION = 60 * 60;

    /** Tests that we can call GetSession to start a session. */
    @Test
    public void testGetSessionToken() throws Exception {
        GetSessionTokenRequest request = GetSessionTokenRequest.builder().durationSeconds(SESSION_DURATION).build();
        GetSessionTokenResponse result = sts.getSessionToken(request);

        assertNotNull(result.credentials().accessKeyId());
        assertNotNull(result.credentials().expiration());
        assertNotNull(result.credentials().secretAccessKey());
        assertNotNull(result.credentials().sessionToken());
    }

    /** Tests that we can call GetFederatedSession to start a federated session. */
    @Test
    public void testGetFederatedSessionToken() throws Exception {
        GetFederationTokenRequest request = GetFederationTokenRequest.builder()
                                                                     .durationSeconds(SESSION_DURATION)
                                                                     .name("Name").build();
        GetFederationTokenResponse result = sts.getFederationToken(request);

        assertNotNull(result.credentials().accessKeyId());
        assertNotNull(result.credentials().expiration());
        assertNotNull(result.credentials().secretAccessKey());
        assertNotNull(result.credentials().sessionToken());

        assertNotNull(result.federatedUser().arn());
        assertNotNull(result.federatedUser().federatedUserId());


    }

    /**
     * In the following test, we purposely setting the time offset to trigger a clock skew error.
     * The time offset must be fixed and then we validate the global value for time offset has been
     * update.
     */
    @Test
    public void testClockSkew() {
        SdkGlobalTime.setGlobalTimeOffset(3600);
        assertTrue(SdkGlobalTime.getGlobalTimeOffset() == 3600);
        sts = STSClient.builder().credentialsProvider(CREDENTIALS_PROVIDER_CHAIN).build();
        sts.getSessionToken(GetSessionTokenRequest.builder().build());
        assertTrue("Clockskew is fixed!", SdkGlobalTime.getGlobalTimeOffset() < 3600);
        // subsequent changes to the global time offset won't affect existing client
        SdkGlobalTime.setGlobalTimeOffset(3600);
        sts.getSessionToken(GetSessionTokenRequest.builder().build());
        assertTrue(SdkGlobalTime.getGlobalTimeOffset() == 3600);
    }
}
