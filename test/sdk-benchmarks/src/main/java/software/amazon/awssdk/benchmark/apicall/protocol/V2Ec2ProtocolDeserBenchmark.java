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

package software.amazon.awssdk.benchmark.apicall.protocol;

import java.util.Collections;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceType;
import software.amazon.awssdk.services.ec2.model.Reservation;
import software.amazon.awssdk.services.ec2.model.RunInstancesRequest;
import software.amazon.awssdk.services.ec2.model.RunInstancesResponse;

@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 15)
@Measurement(iterations = 3, time = 15)
@Fork(2)
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class V2Ec2ProtocolDeserBenchmark {

    private Ec2Client ec2;
    private RunInstancesRequest runInstancesRequest;
    private DescribeInstancesRequest describeInstancesRequest;

    @Setup
    public void setup() {
        Instance instance = Instance.builder()
                .instanceId("i-1234567890abcdef0")
                .instanceType(InstanceType.T2_MICRO)
                .build();
        
        Reservation reservation = Reservation.builder()
                .instances(Collections.singletonList(instance))
                .build();
        
        RunInstancesResponse runInstancesResponse = RunInstancesResponse.builder()
                .instances(Collections.singletonList(instance))
                .build();

        DescribeInstancesResponse describeInstancesResponse = DescribeInstancesResponse.builder()
                .reservations(Collections.singletonList(reservation))
                .build();

        ec2 = new Ec2Client() {
            @Override
            public String serviceName() {
                return "EC2";
            }

            @Override
            public void close() {
            }

            @Override
            public RunInstancesResponse runInstances(RunInstancesRequest request) {
                return runInstancesResponse;
            }

            @Override
            public DescribeInstancesResponse describeInstances(DescribeInstancesRequest request) {
                return describeInstancesResponse;
            }
        };

        runInstancesRequest = RunInstancesRequest.builder()
                .imageId("ami-12345678")
                .minCount(1)
                .maxCount(1)
                .build();

        describeInstancesRequest = DescribeInstancesRequest.builder().build();
    }

    @Benchmark
    public RunInstancesResponse runInstancesSerialization(Blackhole bh) {
        RunInstancesResponse result = ec2.runInstances(runInstancesRequest);
        bh.consume(result.instances());
        return result;
    }

    @Benchmark
    public DescribeInstancesResponse describeInstancesDeserialization(Blackhole bh) {
        DescribeInstancesResponse result = ec2.describeInstances(describeInstancesRequest);
        bh.consume(result.reservations());
        return result;
    }
}
