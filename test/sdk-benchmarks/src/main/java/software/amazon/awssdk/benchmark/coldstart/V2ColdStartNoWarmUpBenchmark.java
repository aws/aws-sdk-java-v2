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
import java.util.HashMap;
import java.util.Map;
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
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.http.apache5.Apache5HttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

/**
 * Baseline: build a {@link DynamoDbClient} and make its first call (to a local mock server) with no warm-up.
 * Compare against {@link V2ColdStartAfterWarmUpBenchmark}. Single-shot, zero warmup, high fork count: only the first
 * invocation per JVM is cold. Do not override these JMH parameters from the CLI, or both benchmarks converge.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 0)
@Measurement(iterations = 1)
@Fork(20)
public class V2ColdStartNoWarmUpBenchmark {

    private static final String FIXTURE = "json-protocol/putitem-response.json";
    private static final String CONTENT_TYPE = "application/x-amz-json-1.0";

    private MockHttpServer server;
    private DynamoDbClient client;

    @Setup(Level.Trial)
    public void setup() throws Exception {
        server = new MockHttpServer(MockHttpServer.loadFixture(FIXTURE), CONTENT_TYPE);
        server.start();
    }

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

    private static PutItemRequest putItemRequest() {
        return PutItemRequest.builder()
                             .tableName("benchmark-table")
                             .item(itemMap())
                             .build();
    }

    private static Map<String, AttributeValue> itemMap() {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("pk", AttributeValue.fromS("benchmark-key"));
        item.put("sk", AttributeValue.fromN("100"));
        item.put("stringField", AttributeValue.fromS("test-value"));
        item.put("numberField", AttributeValue.fromN("123.456"));
        item.put("binaryField", AttributeValue.fromB(SdkBytes.fromUtf8String("hello world")));
        item.put("stringSetField", AttributeValue.builder().ss("value1", "value2", "value3").build());
        item.put("numberSetField", AttributeValue.builder().ns("1.1", "2.2", "3.3").build());
        item.put("boolField", AttributeValue.fromBool(false));
        item.put("nullField", AttributeValue.builder().nul(true).build());
        Map<String, AttributeValue> deep = new HashMap<>();
        deep.put("level2", AttributeValue.fromN("999"));
        Map<String, AttributeValue> nested = new HashMap<>();
        nested.put("nested", AttributeValue.fromS("nested-value"));
        nested.put("deepNested", AttributeValue.fromM(deep));
        item.put("mapField", AttributeValue.fromM(nested));
        item.put("listField", AttributeValue.builder().l(
            AttributeValue.fromS("item1"),
            AttributeValue.fromN("42"),
            AttributeValue.fromBool(true),
            AttributeValue.builder().nul(true).build()).build());
        return item;
    }
}
