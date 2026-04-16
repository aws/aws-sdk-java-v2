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

package software.amazon.awssdk.stability.tests.kinesis;

import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.awssdk.stability.tests.exceptions.StabilityTestsRetryableException;
import software.amazon.awssdk.testutils.retry.RetryableTest;

/**
 * Stability tests for Kinesis using Netty HTTP client.
 */
public class KinesisStabilityTest extends KinesisBaseStabilityTest {

    @Override
    protected KinesisAsyncClient createClient() {
        return KinesisAsyncClient.builder()
                                 .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                                 .httpClientBuilder(NettyNioAsyncHttpClient.builder().maxConcurrency(MAX_CONCURRENCY))
                                 .build();
    }

    @Override
    protected String getTestNamePrefix() {
        return "KinesisStabilityTest";
    }

    @Override
    protected String getConsumerPrefix() {
        return "kinesisstabilitytestconsumer_";
    }

    @RetryableTest(maxRetries = 3, retryableException = StabilityTestsRetryableException.class)
    public void putRecords_subscribeToShard() throws InterruptedException {
        runPutRecordsAndSubscribeToShard();
    }
}
