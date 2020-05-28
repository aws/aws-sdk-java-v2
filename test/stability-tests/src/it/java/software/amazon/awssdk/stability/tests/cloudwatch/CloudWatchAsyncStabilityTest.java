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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.IntFunction;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.stability.tests.exceptions.StabilityTestsRetryableException;
import software.amazon.awssdk.stability.tests.utils.RetryableTest;
import software.amazon.awssdk.stability.tests.utils.StabilityTestRunner;

public class CloudWatchAsyncStabilityTest extends CloudWatchBaseStabilityTest {
    private static String namespace;

    @BeforeAll
    public static void setup() {
        namespace = "CloudWatchAsyncStabilityTest" + System.currentTimeMillis();
    }

    @AfterAll
    public static void tearDown() {
        cloudWatchAsyncClient.close();
    }

    @RetryableTest(maxRetries = 3, retryableException = StabilityTestsRetryableException.class)
    public void putMetrics_lowTpsLongInterval() {
        List<MetricDatum> metrics = new ArrayList<>();
        for (int i = 0; i < 20 ; i++) {
            metrics.add(MetricDatum.builder()
                                   .metricName("test")
                                   .values(RandomUtils.nextDouble(1d, 1000d))
                                   .build());
        }

        IntFunction<CompletableFuture<?>> futureIntFunction = i ->
            cloudWatchAsyncClient.putMetricData(b -> b.namespace(namespace)
                                                      .metricData(metrics));

        runCloudWatchTest("putMetrics_lowTpsLongInterval", futureIntFunction);
    }


    private void runCloudWatchTest(String testName, IntFunction<CompletableFuture<?>> futureIntFunction) {
        StabilityTestRunner.newRunner()
                           .testName("CloudWatchAsyncStabilityTest." + testName)
                           .futureFactory(futureIntFunction)
                           .totalRuns(TOTAL_RUNS)
                           .requestCountPerRun(CONCURRENCY)
                           .delaysBetweenEachRun(Duration.ofSeconds(6))
                           .run();
    }
}
