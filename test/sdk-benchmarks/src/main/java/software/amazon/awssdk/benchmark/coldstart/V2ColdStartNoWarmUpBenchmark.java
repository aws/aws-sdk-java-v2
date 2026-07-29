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
import software.amazon.awssdk.http.apache5.Apache5HttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/**
 * Baseline arm: build a {@link DynamoDbClient} and complete its first operation in a JVM where
 * {@code SdkWarmUp.prime} never ran. Compare against {@link V2ColdStartAfterWarmUpBenchmark}, whose only difference is that
 * priming happened in {@code @Setup}, outside the timing window.
 *
 * <p>The operation goes to an in-process Jetty server on localhost over plain HTTP with an explicit endpoint override and
 * static credentials, so the measurement excludes DNS, TLS to AWS and service latency, and isolates SDK-side
 * initialization: class loading, builder and configuration resolution, interceptor chain assembly, endpoint rule
 * evaluation, auth scheme and signer setup, marshaller and unmarshaller construction, and HTTP client construction.
 *
 * <p><b>The JMH parameters below deliberately differ from every other benchmark in this module.</b>
 * {@code SdkWarmUp} keeps its primed state in statics with no reset hook, so priming does real work at most once per JVM
 * and only the first invocation after it is genuinely cold. {@link Mode#SingleShotTime} with zero warmup iterations and a
 * single measurement iteration yields exactly one measured invocation per fork; the high fork count supplies the samples.
 * For {@code SingleShotTime} a JMH warmup iteration is itself a full invocation, so zero warmup is required, not cosmetic.
 *
 * <p><b>Do not override these from the command line.</b> JMH CLI flags such as {@code -wi 3 -i 3 -f 1} take precedence
 * over these annotations and would silently make the measured invocations warm, which makes both arms converge.
 *
 * <p>This benchmark cannot speak to Lambda SnapStart economics. Under SnapStart the priming cost is paid once at
 * checkpoint and amortized across every restore; JMH has no checkpoint or restore, so here it is paid in the same JVM
 * that is measured.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 0)
@Measurement(iterations = 1)
@Fork(20)
public class V2ColdStartNoWarmUpBenchmark implements SdkClientColdStartBenchmark {

    private ColdStartMockServer server;
    private DynamoDbClient client;

    @Setup(Level.Trial)
    public void setup() throws Exception {
        server = new ColdStartMockServer(ColdStartMockServer.loadFixture(FIXTURE), CONTENT_TYPE);
        server.start();
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
            .include(V2ColdStartNoWarmUpBenchmark.class.getSimpleName())
            .addProfiler(StackProfiler.class)
            .build();
        Collection<RunResult> run = new Runner(opt).run();
    }
}
