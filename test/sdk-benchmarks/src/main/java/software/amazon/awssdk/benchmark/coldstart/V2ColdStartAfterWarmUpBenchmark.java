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

import static software.amazon.awssdk.benchmark.coldstart.ColdStartRequests.CONTENT_TYPE;
import static software.amazon.awssdk.benchmark.coldstart.ColdStartRequests.FIXTURE;
import static software.amazon.awssdk.benchmark.coldstart.ColdStartRequests.putItemRequest;

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
import software.amazon.awssdk.core.crac.SdkWarmUp;
import software.amazon.awssdk.http.apache5.Apache5HttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/**
 * Primed arm: identical to {@link V2ColdStartNoWarmUpBenchmark} except that {@link SdkWarmUp#prime(Class[])} runs in
 * {@code @Setup}, outside the timing window. The measured number is therefore post-prime construction plus post-prime
 * first call, and the difference from the baseline arm is the first-call work that priming front-loads.
 *
 * <p>Uses the targeted overload rather than {@code SdkWarmUp.prime()}. The no-arg form warms every
 * {@code SdkWarmUpProvider} on the classpath, which on this module's classpath includes services unrelated to this
 * measurement.
 *
 * <p>Priming also warms each sync {@code SdkHttpService} on the classpath by sending a {@code GET} to the regional STS
 * endpoint, which is intended: exercising a real request is how the SPI lookup, TLS stack and connection machinery get
 * initialized ahead of time. Those calls happen in {@code @Setup}, outside the measured window, and are best effort:
 * a failure never fails the benchmark, but it does affect the score. On a host without network access the HTTP client
 * stack is only partially warmed, the measured first call is slower, and the primed-vs-no-prime delta silently shrinks.
 * Only compare the two arms when both ran in the same network environment. The cost of priming itself is measured
 * separately by {@link V2SdkWarmUpExecutionTimeBenchmark}.
 *
 * <p>The provider primes DynamoDB with {@code ListBackups} while the measured call is {@code PutItem}. That is
 * deliberate: the measured delta is the cross-operation transfer benefit, the realistic case, since shared work
 * (signing, HTTP, base marshalling) dominates over operation-specific marshallers.
 *
 * <p>See {@link V2ColdStartNoWarmUpBenchmark} for why the JMH parameters below differ from the rest of this module and why
 * they must not be overridden from the command line.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 0)
@Measurement(iterations = 1)
@Fork(20)
public class V2ColdStartAfterWarmUpBenchmark implements SdkClientColdStartBenchmark {

    private ColdStartMockServer server;
    private DynamoDbClient client;

    @Setup(Level.Trial)
    public void setup() throws Exception {
        server = new ColdStartMockServer(ColdStartMockServer.loadFixture(FIXTURE), CONTENT_TYPE);
        server.start();

        SdkWarmUp.prime(DynamoDbClient.class);
    }

    @Override
    @Benchmark
    public void coldFirstCall(Blackhole blackhole) throws Exception {
        client = DynamoDbClient.builder()
                               .endpointOverride(server.getHttpUri())
                               .region(Region.US_EAST_1)
                               .credentialsProvider(StaticCredentialsProvider.create(
                                   AwsBasicCredentials.create("test", "test")))
                               .httpClient(Apache5HttpClient.create())
                               .endpointDiscoveryEnabled(false)
                               .build();

        blackhole.consume(client.putItem(putItemRequest()));
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
            .include(V2ColdStartAfterWarmUpBenchmark.class.getSimpleName())
            .addProfiler(StackProfiler.class)
            .build();
        Collection<RunResult> run = new Runner(opt).run();
    }
}
