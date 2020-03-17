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

package software.amazon.awssdk.services.sns;

import org.junit.Test;
import software.amazon.awssdk.testutils.service.AwsTestBase;

/**
 * Simple smoke test for session management.
 */
public class SessionBasedAuthenticationIntegrationTest extends AwsTestBase {

    /**
     * TODO: reenable
     */
    @Test
    public void testSessions() throws Exception {
        setUpCredentials();

        // RenewableAWSSessionCredentials sessionCredentials = new
        // STSSessionCredentials(credentials);
        // AmazonSnsClient sns = new AmazonSnsClient(sessionCredentials);
        //
        // sns.createTopic(new CreateTopicRequest().withName("java" +
        // this.getClass().getSimpleName()
        // + System.currentTimeMillis()));
    }
}
