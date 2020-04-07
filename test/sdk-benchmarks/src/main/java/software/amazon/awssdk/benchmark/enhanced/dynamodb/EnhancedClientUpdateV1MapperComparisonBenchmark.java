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
import com.amazonaws.services.dynamodbv2.model.UpdateItemResult;
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
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;

@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
@Fork(2)
@State(Scope.Benchmark)
public class EnhancedClientUpdateV1MapperComparisonBenchmark {
    private static final V2ItemFactory V2_ITEM_FACTORY = new V2ItemFactory();
    private static final V1ItemFactory V1_ITEM_FACTORY = new V1ItemFactory();
    private static final DynamoDBMapperConfig MAPPER_CONFIG =
        DynamoDBMapperConfig.builder()
                            .withSaveBehavior(DynamoDBMapperConfig.SaveBehavior.UPDATE)
                            .build();

    @Benchmark
    public void v2Update(TestState s) {
        s.v2Table.updateItem(s.testItem.v2Bean);
    }

    @Benchmark
    public void v1Update(TestState s) {
        s.v1DdbMapper.save(s.testItem.v1Bean);
    }

    private static DynamoDbClient getV2Client(Blackhole bh, UpdateItemResponse updateItemResponse) {
        return new V2TestDynamoDbUpdateItemClient(bh, updateItemResponse);
    }

    private static AmazonDynamoDB getV1Client(Blackhole bh, UpdateItemResult updateItemResult) {
        return new V1TestDynamoDbUpdateItemClient(bh, updateItemResult);
    }

    @State(Scope.Benchmark)
    public static class TestState {
        @Param({"TINY", "SMALL", "HUGE", "HUGE_FLAT"})
        public TestItem testItem;

        private DynamoDbTable v2Table;
        private DynamoDBMapper v1DdbMapper;


        @Setup
        public void setup(Blackhole bh) {
            DynamoDbEnhancedClient v2DdbEnh = DynamoDbEnhancedClient.builder()
                    .dynamoDbClient(getV2Client(bh, testItem.v2UpdateItemResponse))
                    .build();

            v2Table = v2DdbEnh.table(testItem.name(), testItem.schema);

            v1DdbMapper = new DynamoDBMapper(getV1Client(bh, testItem.v1UpdateItemResult), MAPPER_CONFIG);
        }

        public enum TestItem {
            TINY(
                    V2ItemFactory.TINY_BEAN_TABLE_SCHEMA,
                    V2_ITEM_FACTORY.tinyBean(),
                    UpdateItemResponse.builder().attributes(V2_ITEM_FACTORY.tiny()).build(),

                    V1_ITEM_FACTORY.v1TinyBean(),
                    new UpdateItemResult().withAttributes(V1_ITEM_FACTORY.tiny())
            ),

            SMALL(
                    V2ItemFactory.SMALL_BEAN_TABLE_SCHEMA,
                    V2_ITEM_FACTORY.smallBean(),
                    UpdateItemResponse.builder().attributes(V2_ITEM_FACTORY.small()).build(),

                    V1_ITEM_FACTORY.v1SmallBean(),
                    new UpdateItemResult().withAttributes(V1_ITEM_FACTORY.small())
            ),

            HUGE(
                    V2ItemFactory.HUGE_BEAN_TABLE_SCHEMA,
                    V2_ITEM_FACTORY.hugeBean(),
                    UpdateItemResponse.builder().attributes(V2_ITEM_FACTORY.huge()).build(),

                    V1_ITEM_FACTORY.v1hugeBean(),
                    new UpdateItemResult().withAttributes(V1_ITEM_FACTORY.huge())
            ),

            HUGE_FLAT(
                    V2ItemFactory.HUGE_BEAN_FLAT_TABLE_SCHEMA,
                    V2_ITEM_FACTORY.hugeBeanFlat(),
                    UpdateItemResponse.builder().attributes(V2_ITEM_FACTORY.hugeFlat()).build(),

                    V1_ITEM_FACTORY.v1HugeBeanFlat(),
                    new UpdateItemResult().withAttributes(V1_ITEM_FACTORY.hugeFlat())
            ),
            ;

            // V2
            private TableSchema schema;
            private Object v2Bean;
            private UpdateItemResponse v2UpdateItemResponse;

            // V1
            private Object v1Bean;
            private UpdateItemResult v1UpdateItemResult;

            TestItem(TableSchema<?> schema,
                             Object v2Bean,
                             UpdateItemResponse v2UpdateItemResponse,

                             Object v1Bean,
                             UpdateItemResult v1UpdateItemResult) {
                this.schema = schema;
                this.v2Bean = v2Bean;
                this.v2UpdateItemResponse = v2UpdateItemResponse;

                this.v1Bean = v1Bean;
                this.v1UpdateItemResult = v1UpdateItemResult;
            }
        }
    }
}
