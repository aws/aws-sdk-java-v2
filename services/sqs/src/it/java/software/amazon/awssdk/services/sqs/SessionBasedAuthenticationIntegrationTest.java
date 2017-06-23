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

package software.amazon.awssdk.services.sqs;

import org.junit.Test;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.model.DeleteQueueRequest;
import software.amazon.awssdk.services.sts.STSClient;
import software.amazon.awssdk.services.sts.auth.StsGetSessionTokenCredentialsProvider;
import software.amazon.awssdk.services.sts.model.GetSessionTokenRequest;

/**
 * Smoke test of using session-based auth to connect to SQS
 */
public class SessionBasedAuthenticationIntegrationTest extends IntegrationTestBase {

    @Test
    public void clientWithStsSessionCredentials_CanMakeCallsToSqs() throws Exception {
        STSClient stsClient = STSClient.builder()
                                       .region(Region.US_EAST_1)
                                       .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                                       .build();
        StsGetSessionTokenCredentialsProvider sessionCredentials =
                StsGetSessionTokenCredentialsProvider.builder()
                                                     .stsClient(stsClient)
                                                     .refreshRequest(GetSessionTokenRequest.builder().build())
                                                     .build();
        SQSAsyncClient sqsClient = SQSAsyncClient.builder().credentialsProvider(sessionCredentials).build();
        String queueUrl = createQueue(sqsClient);
        sqsClient.deleteQueue(DeleteQueueRequest.builder().queueUrl(queueUrl).build());
    }
}
