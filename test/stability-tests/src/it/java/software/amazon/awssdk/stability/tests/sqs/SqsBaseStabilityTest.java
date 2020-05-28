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

package software.amazon.awssdk.stability.tests.sqs;

import java.time.Duration;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.testutils.service.AwsTestBase;


public abstract class SqsBaseStabilityTest extends AwsTestBase {
    protected static final int CONCURRENCY = 100;
    protected static final int TOTAL_REQUEST_NUMBER = 5000;
    protected static final int TOTAL_RUNS = 50;

    protected static SqsAsyncClient sqsAsyncClient = SqsAsyncClient.builder()
                                                                   .httpClientBuilder(NettyNioAsyncHttpClient.builder().maxConcurrency(CONCURRENCY))
                                                                   .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                                                                   .overrideConfiguration(b -> b.apiCallTimeout(Duration.ofMinutes(1)))
                                                                   .build();

    protected static SqsClient sqsClient = SqsClient.builder()
                                                    .httpClientBuilder(ApacheHttpClient.builder().maxConnections(CONCURRENCY))
                                                    .overrideConfiguration(b -> b.apiCallTimeout(Duration.ofMinutes(1)))
                                                    .build();


    public abstract void sendMessage_receiveMessage();
}
