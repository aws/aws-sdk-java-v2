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

import com.amazonaws.services.ec2.AbstractAmazonEC2;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
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

@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 15)
@Measurement(iterations = 3, time = 15)
@Fork(2)
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class V1Ec2ProtocolDeserBenchmark {

    private AbstractAmazonEC2 ec2;
    private RunInstancesRequest runInstancesRequest;
    private DescribeInstancesRequest describeInstancesRequest;

    @Setup
    public void setup() {
        Instance instance = new Instance()
                .withInstanceId("i-1234567890abcdef0")
                .withInstanceType("t2.micro");
        
        Reservation reservation = new Reservation()
                .withInstances(Collections.singletonList(instance));
        
        RunInstancesResult runInstancesResult = new RunInstancesResult()
                .withReservation(reservation);

        DescribeInstancesResult describeInstancesResult = new DescribeInstancesResult()
                .withReservations(Collections.singletonList(reservation));

        ec2 = new AbstractAmazonEC2() {
            @Override
            public RunInstancesResult runInstances(RunInstancesRequest request) {
                return runInstancesResult;
            }

            @Override
            public DescribeInstancesResult describeInstances(DescribeInstancesRequest request) {
                return describeInstancesResult;
            }
        };

        runInstancesRequest = new RunInstancesRequest()
                .withImageId("ami-12345678")
                .withMinCount(1)
                .withMaxCount(1);

        describeInstancesRequest = new DescribeInstancesRequest();
    }

    @Benchmark
    public RunInstancesResult runInstancesSerialization(Blackhole bh) {
        RunInstancesResult result = ec2.runInstances(runInstancesRequest);
        bh.consume(result.getReservation());
        return result;
    }

    @Benchmark
    public DescribeInstancesResult describeInstancesDeserialization(Blackhole bh) {
        DescribeInstancesResult result = ec2.describeInstances(describeInstancesRequest);
        bh.consume(result.getReservations());
        return result;
    }
}
