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

package software.amazon.awssdk.benchmark.metricpublisher.emf;

import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import software.amazon.awssdk.benchmark.apicall.MetricsEnabledBenchmark;
import software.amazon.awssdk.core.client.builder.SdkClientBuilder;
import software.amazon.awssdk.metrics.publishers.emf.EmfMetricLoggingPublisher;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
public class EmfMetricPublisherBenchmark extends MetricsEnabledBenchmark {
    private EmfMetricLoggingPublisher emfMetricLoggingPublisher;

    @Override
    @Setup(Level.Trial)
    public void setup() throws Exception {
        emfMetricLoggingPublisher = EmfMetricLoggingPublisher.builder()
                                                             .namespace("EmfMetricPublisherBenchmark")
                                                             .logGroupName("LogGroupName")
                                                             .build();
        super.setup();
    }

    @Override
    protected <T extends SdkClientBuilder<T, ?>> T enableMetrics(T clientBuilder) {
        return clientBuilder.overrideConfiguration(c -> c.addMetricPublisher(emfMetricLoggingPublisher));
    }

    @Override
    @TearDown(Level.Trial)
    public void tearDown() throws Exception {
        super.tearDown();
        emfMetricLoggingPublisher.close();
    }
}
