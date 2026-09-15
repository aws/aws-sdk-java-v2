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

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
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
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
@Fork(2)
@State(Scope.Benchmark)
public class EnhancedClientPutV1MapperComparisonBenchmark {
    private static final V2ItemFactory V2_ITEM_FACTORY = new V2ItemFactory();
    private static final V2MapperItemFactory V2_MAPPER_ITEM_FACTORY = new V2MapperItemFactory();
    private static final V2MapperSdkBytesItemFactory V2_MAPPER_SDK_BYTES_ITEM_FACTORY =
            new V2MapperSdkBytesItemFactory();
    private static final V1ItemFactory V1_ITEM_FACTORY = new V1ItemFactory();
    private static final DynamoDBMapperConfig MAPPER_CONFIG =
        DynamoDBMapperConfig.builder()
                            .withSaveBehavior(DynamoDBMapperConfig.SaveBehavior.PUT)
                            .build();
    private static final software.amazon.awssdk.mapper.dynamodb.DynamoDBMapperConfig V2_MAPPER_CONFIG =
        software.amazon.awssdk.mapper.dynamodb.DynamoDBMapperConfig.builder()
            .withSaveBehavior(software.amazon.awssdk.mapper.dynamodb.DynamoDBMapperConfig.SaveBehavior.PUT)
            .build();

    @Benchmark
    public void v2Put(TestState s) {
        s.v2Table.putItem(s.testItem.v2Bean);
    }

    @Benchmark
    public void v1Put(TestState s) {
        s.v1DdbMapper.save(s.testItem.v1Bean);
    }

    @Benchmark
    public void v2MapperPut(TestState s) {
        s.v2DdbMapper.save(s.testItem.v2MapperBean);
    }

    @Benchmark
    public void v2MapperPutSdkBytes(TestState s) {
        s.v2DdbMapper.save(s.testItem.v2MapperSdkBytesBean);
    }

    private static DynamoDbClient getV2Client(Blackhole bh) {
        return new V2TestDynamoDbPutItemClient(bh);
    }

    private static AmazonDynamoDB getV1Client(Blackhole bh) {
        return new V1TestDynamoDbPutItemClient(bh);
    }

    @State(Scope.Benchmark)
    public static class TestState {
        @Param({"TINY", "SMALL", "HUGE", "HUGE_FLAT"})
        public TestItem testItem;

        private DynamoDbTable v2Table;
        private DynamoDBMapper v1DdbMapper;
        private software.amazon.awssdk.mapper.dynamodb.DynamoDBMapper v2DdbMapper;

        @Setup
        public void setup(Blackhole bh) {
            DynamoDbClient v2Client = getV2Client(bh);
            DynamoDbEnhancedClient v2DdbEnh = DynamoDbEnhancedClient.builder()
                    .dynamoDbClient(v2Client)
                    .build();

            v2Table = v2DdbEnh.table(testItem.name(), testItem.schema);
            v2DdbMapper = new software.amazon.awssdk.mapper.dynamodb.DynamoDBMapper(v2Client, V2_MAPPER_CONFIG);

            v1DdbMapper = new DynamoDBMapper(getV1Client(bh), MAPPER_CONFIG);
        }

        public enum TestItem {
            TINY(
                    V2ItemFactory.TINY_BEAN_TABLE_SCHEMA,
                    V2_ITEM_FACTORY.tinyBean(),
                    V2_MAPPER_ITEM_FACTORY.v2MapperTinyBean(),
                    V2_MAPPER_SDK_BYTES_ITEM_FACTORY.v2MapperTinyBean(),

                    V1_ITEM_FACTORY.v1TinyBean()
            ),

            SMALL(
                    V2ItemFactory.SMALL_BEAN_TABLE_SCHEMA,
                    V2_ITEM_FACTORY.smallBean(),
                    V2_MAPPER_ITEM_FACTORY.v2MapperSmallBean(),
                    V2_MAPPER_SDK_BYTES_ITEM_FACTORY.v2MapperSmallBean(),

                    V1_ITEM_FACTORY.v1SmallBean()
            ),

            HUGE(
                    V2ItemFactory.HUGE_BEAN_TABLE_SCHEMA,
                    V2_ITEM_FACTORY.hugeBean(),
                    V2_MAPPER_ITEM_FACTORY.v2MapperHugeBean(),
                    V2_MAPPER_SDK_BYTES_ITEM_FACTORY.v2MapperHugeBean(),

                    V1_ITEM_FACTORY.v1hugeBean()
            ),

            HUGE_FLAT(
                    V2ItemFactory.HUGE_BEAN_FLAT_TABLE_SCHEMA,
                    V2_ITEM_FACTORY.hugeBeanFlat(),
                    V2_MAPPER_ITEM_FACTORY.v2MapperHugeBeanFlat(),
                    V2_MAPPER_SDK_BYTES_ITEM_FACTORY.v2MapperHugeBeanFlat(),

                    V1_ITEM_FACTORY.v1HugeBeanFlat()
            ),
            ;

            // V2
            private TableSchema<?> schema;
            private Object v2Bean;
            private Object v2MapperBean;
            private Object v2MapperSdkBytesBean;

            // V1
            private Object v1Bean;

            TestItem(TableSchema<?> schema,
                     Object v2Bean,
                     Object v2MapperBean,
                     Object v2MapperSdkBytesBean,

                     Object v1Bean) {
                this.schema = schema;
                this.v2Bean = v2Bean;
                this.v2MapperBean = v2MapperBean;
                this.v2MapperSdkBytesBean = v2MapperSdkBytesBean;

                this.v1Bean = v1Bean;
            }
        }
    }
}
