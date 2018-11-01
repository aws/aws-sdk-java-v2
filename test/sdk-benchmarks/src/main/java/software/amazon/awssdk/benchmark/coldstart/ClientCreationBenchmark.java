/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.benchmark.coldstart;

import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/**
 * Benchmark for creating the clients
 */
@BenchmarkMode({Mode.SingleShotTime})
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 5, timeUnit = TimeUnit.SECONDS)
@Fork(5)
public class ClientCreationBenchmark {

    @Benchmark
    public void defaultClient(Blackhole blackhole) {
        DynamoDbClient dynamoDbClient = DynamoDbClient.create();
        blackhole.consume(dynamoDbClient);
    }

    @Benchmark
    public void createClient(Blackhole blackhole) {
        DynamoDbClient dynamoDbClient = DynamoDbClient.builder()
                                                      .region(Region.US_WEST_2)
                                                      .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("dgs", "sdfs")))
                                                      .httpClient(ApacheHttpClient.builder().build())
                                                      .build();
        blackhole.consume(dynamoDbClient);
    }
    public static void main(String... args) throws RunnerException {
        Options opt = new OptionsBuilder()
            .include(ClientCreationBenchmark.class.getSimpleName())
            //.addProfiler(StackProfiler.class)
            .build();
        new Runner(opt).run();
    }
}
