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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.crt.io.EventLoopGroup;
import software.amazon.awssdk.crt.io.HostResolver;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.crt.AwsCrtAsyncHttpClient;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.stability.tests.exceptions.StabilityTestsRetryableException;
import software.amazon.awssdk.stability.tests.utils.RetryableTest;

public class CloudWatchCrtAsyncStabilityTest extends CloudWatchBaseStabilityTest {
    private static String namespace;
    private static CloudWatchAsyncClient cloudWatchAsyncClient;

    @Override
    protected CloudWatchAsyncClient getTestClient() { return cloudWatchAsyncClient; }

    @Override
    protected String getNamespace() { return namespace; }

    @BeforeAll
    public static void setup() {
        namespace = "CloudWatchCrtAsyncStabilityTest" + System.currentTimeMillis();
        SdkAsyncHttpClient.Builder crtClientBuilder = AwsCrtAsyncHttpClient.builder()
                                                                           .connectionMaxIdleTime(Duration.ofSeconds(5));

        cloudWatchAsyncClient = CloudWatchAsyncClient.builder()
                                                     .httpClientBuilder(crtClientBuilder)
                                                     .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                                                     .overrideConfiguration(b -> b.apiCallTimeout(Duration.ofMinutes(10))
                                                                                  // Retry at test level
                                                                                  .retryPolicy(RetryPolicy.none()))
                                                     .build();
    }

    @AfterAll
    public static void tearDown() {
        cloudWatchAsyncClient.close();
    }

    @RetryableTest(maxRetries = 3, retryableException = StabilityTestsRetryableException.class)
    public void putMetrics_lowTpsLongInterval() {
        putMetrics();
    }
}
