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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.stability.tests.exceptions.StabilityTestsRetryableException;
import software.amazon.awssdk.stability.tests.utils.RetryableTest;

public class SqsNettyAsyncStabilityTest extends SqsBaseStabilityTest {
    private static String queueName;
    private static String queueUrl;

    private static SqsAsyncClient sqsAsyncClient;

    @Override
    protected SqsAsyncClient getTestClient() { return sqsAsyncClient; }

    @Override
    protected String getQueueUrl() { return queueUrl; }

    @Override
    protected String getQueueName() { return queueName; }

    @BeforeAll
    public static void setup() {
        sqsAsyncClient = SqsAsyncClient.builder()
                .httpClientBuilder(NettyNioAsyncHttpClient.builder().maxConcurrency(CONCURRENCY))
                .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                .overrideConfiguration(b -> b.apiCallTimeout(Duration.ofMinutes(1)))
                .build();
        queueName = "sqsnettyasyncstabilitytests" + System.currentTimeMillis();
        queueUrl = setup(sqsAsyncClient, queueName);
    }

    @AfterAll
    public static void tearDown() {
        tearDown(sqsAsyncClient, queueUrl);
        sqsAsyncClient.close();
    }

    @RetryableTest(maxRetries = 3, retryableException = StabilityTestsRetryableException.class)
    public void sendMessage_receiveMessage() {
        sendMessage();
        receiveMessage();
    }
}
