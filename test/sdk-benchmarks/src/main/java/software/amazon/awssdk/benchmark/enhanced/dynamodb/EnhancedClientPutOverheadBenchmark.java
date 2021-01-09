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
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
@Fork(2)
@State(Scope.Benchmark)
public class EnhancedClientPutOverheadBenchmark {
    @Benchmark
    public void lowLevelPut(TestState s) {
        s.ddb.putItem(r -> r.item(s.testItem.av));
    }

    @Benchmark
    public void enhancedPut(TestState s) {
        s.enhTable.putItem(s.testItem.bean);
    }

    @State(Scope.Benchmark)
    public static class TestState {
        @Param({"TINY", "SMALL", "HUGE", "HUGE_FLAT"})
        private TestItem testItem;
        private DynamoDbClient ddb;

        private DynamoDbTable enhTable;

        @Setup
        public void setup(Blackhole bh) {
            ddb = DynamoDbClient.builder()
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create("akid", "skid")))
                    .httpClient(new MockHttpClient("{}", "{}"))
                    .overrideConfiguration(c -> c.addExecutionInterceptor(new ExecutionInterceptor() {
                        @Override
                        public void afterUnmarshalling(Context.AfterUnmarshalling context,
                                                       ExecutionAttributes executionAttributes) {
                            bh.consume(context);
                            bh.consume(executionAttributes);
                        }
                    }))
                    .build();

            DynamoDbEnhancedClient ddbEnh = DynamoDbEnhancedClient.builder()
                    .dynamoDbClient(ddb)
                    .build();

            enhTable = ddbEnh.table(testItem.name(), testItem.tableSchema);
        }
    }

    public enum TestItem {
        TINY,
        SMALL,
        HUGE,
        HUGE_FLAT
        ;

        private static final V2ItemFactory FACTORY = new V2ItemFactory();

        private Map<String, AttributeValue> av;

        private TableSchema tableSchema;
        private Object bean;

        static {
            TINY.av = FACTORY.tiny();
            TINY.tableSchema = V2ItemFactory.TINY_BEAN_TABLE_SCHEMA;
            TINY.bean = FACTORY.tinyBean();

            SMALL.av = FACTORY.small();
            SMALL.tableSchema = V2ItemFactory.SMALL_BEAN_TABLE_SCHEMA;
            SMALL.bean = FACTORY.smallBean();

            HUGE.av = FACTORY.huge();
            HUGE.tableSchema = V2ItemFactory.HUGE_BEAN_TABLE_SCHEMA;
            HUGE.bean = FACTORY.hugeBean();

            HUGE_FLAT.av = FACTORY.hugeFlat();
            HUGE_FLAT.tableSchema = V2ItemFactory.HUGE_BEAN_FLAT_TABLE_SCHEMA;
            HUGE_FLAT.bean = FACTORY.hugeBeanFlat();
        }
    }
}
