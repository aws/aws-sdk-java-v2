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

package software.amazon.awssdk.benchmark.enhanced.dynamodb;

import static software.amazon.awssdk.core.client.config.SdkClientOption.ENDPOINT;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.Map;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.benchmark.utils.MockHttpClient;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.protocols.json.AwsJsonProtocol;
import software.amazon.awssdk.protocols.json.AwsJsonProtocolFactory;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.transform.PutItemRequestMarshaller;
import software.amazon.awssdk.utils.IoUtils;

@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
@Fork(2)
@State(Scope.Benchmark)
public class EnhancedClientGetOverheadBenchmark {
    private static final AwsJsonProtocolFactory JSON_PROTOCOL_FACTORY = AwsJsonProtocolFactory
            .builder()
            .clientConfiguration(SdkClientConfiguration.builder()
                    .option(ENDPOINT, URI.create("https://dynamodb.amazonaws.com"))
                    .build())
            .defaultServiceExceptionSupplier(DynamoDbException::builder)
            .protocol(AwsJsonProtocol.AWS_JSON)
            .protocolVersion("1.0")
            .build();

    private static final PutItemRequestMarshaller PUT_ITEM_REQUEST_MARSHALLER =
            new PutItemRequestMarshaller(JSON_PROTOCOL_FACTORY);

    private static final V2ItemFactory ITEM_FACTORY = new V2ItemFactory();

    private final Key testKey = Key.builder().partitionValue("key").build();


    @Benchmark
    public Object llGet(TestState s) {
        return s.dynamoDb.getItem(GetItemRequest.builder().build());
    }

    @Benchmark
    public Object enhGet(TestState s) {
        return s.table.getItem(testKey);
    }

    @State(Scope.Benchmark)
    public static class TestState {
        private DynamoDbClient dynamoDb;

        @Param({"TINY", "SMALL", "HUGE", "HUGE_FLAT"})
        private TestItem testItem;

        private DynamoDbTable table;

        @Setup
        public void setup(Blackhole bh) {
            dynamoDb = DynamoDbClient.builder()
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create("akid", "skid")))
                    .httpClient(new MockHttpClient(testItem.responseContent, "{}"))
                    .overrideConfiguration(o -> o.addExecutionInterceptor(new ExecutionInterceptor() {
                        @Override
                        public void afterUnmarshalling(Context.AfterUnmarshalling context,
                                                       ExecutionAttributes executionAttributes) {
                            bh.consume(context);
                            bh.consume(executionAttributes);
                        }
                    }))
                    .build();

            DynamoDbEnhancedClient ddbEnh = DynamoDbEnhancedClient.builder()
                    .dynamoDbClient(dynamoDb)
                    .build();

            table = ddbEnh.table(testItem.name(), testItem.tableSchema);
        }
    }

    public enum TestItem {
        TINY(marshall(ITEM_FACTORY.tiny()), V2ItemFactory.TINY_BEAN_TABLE_SCHEMA),
        SMALL(marshall(ITEM_FACTORY.small()), V2ItemFactory.SMALL_BEAN_TABLE_SCHEMA),
        HUGE(marshall(ITEM_FACTORY.huge()), V2ItemFactory.HUGE_BEAN_TABLE_SCHEMA),
        HUGE_FLAT(marshall(ITEM_FACTORY.hugeFlat()), V2ItemFactory.HUGE_BEAN_FLAT_TABLE_SCHEMA)
        ;

        private String responseContent;
        private TableSchema tableSchema;

        TestItem(String responseContent, TableSchema tableSchema) {
            this.responseContent = responseContent;
            this.tableSchema = tableSchema;
        }
    }

    private static String marshall(Map<String, AttributeValue> item) {
        return PUT_ITEM_REQUEST_MARSHALLER.marshall(PutItemRequest.builder().item(item).build())
                .contentStreamProvider().map(cs -> {
                    try {
                        return IoUtils.toUtf8String(cs.newStream());
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }).orElse(null);
    }
}
