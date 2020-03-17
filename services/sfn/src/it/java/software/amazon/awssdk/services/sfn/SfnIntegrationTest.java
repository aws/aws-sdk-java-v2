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

package software.amazon.awssdk.services.sfn;

import static org.junit.Assert.assertNotNull;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.testutils.service.AwsIntegrationTestBase;

public class SfnIntegrationTest extends AwsIntegrationTestBase {

    private static SfnClient sfnClient;
    private static String activityArn;

    @BeforeClass
    public static void setUpClient() {
        sfnClient = SfnClient.builder().credentialsProvider(CREDENTIALS_PROVIDER_CHAIN).build();
        activityArn = sfnClient.createActivity(b -> b.name("test")).activityArn();
    }

    @AfterClass
    public static void cleanUp() {
        if (activityArn != null) {
            sfnClient.deleteActivity(b -> b.activityArn(activityArn));
        }
    }

    @Test
    public void getActivityTask_shouldWorkByDefault(){
        assertNotNull(sfnClient.getActivityTask(b -> b.activityArn(activityArn)));
    }
}
