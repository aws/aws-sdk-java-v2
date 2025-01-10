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

package software.amazon.awssdk.benchmark.metricpublisher.cloudwatch;

import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import software.amazon.awssdk.benchmark.apicall.MetricsEnabledBenchmark;
import software.amazon.awssdk.benchmark.utils.NoOpCloudWatchAsyncClient;
import software.amazon.awssdk.core.client.builder.SdkClientBuilder;
import software.amazon.awssdk.metrics.publishers.cloudwatch.CloudWatchMetricPublisher;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
public class CloudWatchMetricPublisherBenchmark extends MetricsEnabledBenchmark {

    @Override
    protected <T extends SdkClientBuilder<T, ?>> T enableMetrics(T clientBuilder) {

        CloudWatchAsyncClient noOpClient = new NoOpCloudWatchAsyncClient();

        CloudWatchMetricPublisher cwPublisher = CloudWatchMetricPublisher.builder()
                                                                         .cloudWatchClient(noOpClient)
                                                                         .namespace("CloudWatchMetricPublisherBenchmark")
                                                                         .build();

        return clientBuilder.overrideConfiguration(c -> c.addMetricPublisher(cwPublisher));
    }

}
