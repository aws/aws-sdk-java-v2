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
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.stability.tests.utils.StabilityTestRunner;
import software.amazon.awssdk.testutils.service.AwsTestBase;

public abstract class CloudWatchBaseStabilityTest extends AwsTestBase {
    protected static final int CONCURRENCY = 50;
    protected static final int TOTAL_RUNS = 3;



    protected abstract CloudWatchAsyncClient getTestClient();
    protected abstract String getNamespace();

    protected void putMetrics() {
        List<MetricDatum> metrics = new ArrayList<>();
        for (int i = 0; i < 20 ; i++) {
            metrics.add(MetricDatum.builder()
                    .metricName("test")
                    .values(RandomUtils.nextDouble(1d, 1000d))
                    .build());
        }

        IntFunction<CompletableFuture<?>> futureIntFunction = i ->
                getTestClient().putMetricData(b -> b.namespace(getNamespace())
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
