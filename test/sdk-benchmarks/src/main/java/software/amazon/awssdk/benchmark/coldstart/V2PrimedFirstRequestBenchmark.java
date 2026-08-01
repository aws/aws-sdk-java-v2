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

package software.amazon.awssdk.benchmark.coldstart;

import java.util.Collection;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.profile.StackProfiler;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.CommandLineOptionException;
import org.openjdk.jmh.runner.options.CommandLineOptions;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.benchmark.utils.MockHttpServer;
import software.amazon.awssdk.core.crac.SdkWarmUp;
import software.amazon.awssdk.http.apache5.Apache5HttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/**
 * Same measured work as {@link V2DefaultFirstRequestBenchmark} (the first {@code putItem} on an already-built client),
 * but {@link SdkWarmUp#prime(Class[])} runs in the untimed {@code @Setup}. The score difference between the two is the
 * first-call work that warm-up front-loads. See {@link V2SdkWarmUpExecutionTimeBenchmark} for how long prime() itself
 * takes. Single-shot, zero warmup, high fork count: only the first invocation per JVM is cold. Do not override these
 * JMH parameters from the CLI, or the two variants converge.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 0)
@Measurement(iterations = 1)
@Fork(20)
public class V2PrimedFirstRequestBenchmark implements FirstRequestBenchmark {

    private static final String FIXTURE = "json-protocol/putitem-response.json";
    private static final String CONTENT_TYPE = "application/x-amz-json-1.0";

    private MockHttpServer server;
    private DynamoDbClient client;

    @Setup(Level.Trial)
    public void setup() throws Exception {
        server = new MockHttpServer(MockHttpServer.loadFixture(FIXTURE), CONTENT_TYPE);
        server.start();

        SdkWarmUp.prime(DynamoDbClient.class);

        client = DynamoDbClient.builder()
                               .endpointOverride(server.getHttpUri())
                               .region(Region.US_EAST_1)
                               .credentialsProvider(StaticCredentialsProvider.create(
                                   AwsBasicCredentials.create("test", "test")))
                               .httpClient(Apache5HttpClient.create())
                               .endpointDiscoveryEnabled(false)
                               .build();
    }

    @Override
    @Benchmark
    public void firstRequest(Blackhole blackhole) throws Exception {
        blackhole.consume(client.putItem(FirstRequestBenchmark.v2PutItemRequest()));
    }

    @TearDown(Level.Trial)
    public void tearDown() throws Exception {
        if (client != null) {
            client.close();
        }
        if (server != null) {
            server.stop();
        }
    }

    public static void main(String... args) throws RunnerException, CommandLineOptionException {
        Options opt = new OptionsBuilder()
            .parent(new CommandLineOptions())
            .include(V2PrimedFirstRequestBenchmark.class.getSimpleName())
            .addProfiler(StackProfiler.class)
            .build();
        Collection<RunResult> run = new Runner(opt).run();
    }
}
