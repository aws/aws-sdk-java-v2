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

package software.amazon.awssdk.stability.tests.cloudwatch;


import java.time.Duration;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.testutils.service.AwsTestBase;

public abstract class CloudWatchBaseStabilityTest extends AwsTestBase {
    protected static final int CONCURRENCY = 50;
    protected static final int TOTAL_RUNS = 3;

    protected static CloudWatchAsyncClient cloudWatchAsyncClient =
        CloudWatchAsyncClient.builder()
                             .httpClientBuilder(NettyNioAsyncHttpClient.builder().maxConcurrency(CONCURRENCY))
                             .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                             .overrideConfiguration(b -> b
                                 // Retry at test level
                                 .retryPolicy(RetryPolicy.none())
                                 .apiCallTimeout(Duration.ofMinutes(1)))
                             .build();


}
