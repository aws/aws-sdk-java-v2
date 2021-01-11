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
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import java.util.Arrays;
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
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
@Fork(2)
@State(Scope.Benchmark)
public class EnhancedClientScanV1MapperComparisonBenchmark {
    private static final V2ItemFactory V2_ITEM_FACTORY = new V2ItemFactory();
    private static final V1ItemFactory V1_ITEM_FACTORY = new V1ItemFactory();
    private static final DynamoDBScanExpression V1_SCAN_EXPRESSION = new DynamoDBScanExpression();

    @Benchmark
    public Object v2Scan(TestState s) {
        return s.v2Table.scan().iterator().next();
    }

    @Benchmark
    public Object v1Scan(TestState s) {
        return s.v1DdbMapper.scan(s.testItem.getV1BeanClass(), V1_SCAN_EXPRESSION).iterator().next();
    }

    private static DynamoDbClient getV2Client(Blackhole bh, ScanResponse scanResponse) {
        return new V2TestDynamoDbScanClient(bh, scanResponse);
    }

    private static AmazonDynamoDB getV1Client(Blackhole bh, ScanResult scanResult) {
        return new V1TestDynamoDbScanClient(bh, scanResult);
    }

    @State(Scope.Benchmark)
    public static class TestState {
        @Param({"TINY", "SMALL", "HUGE", "HUGE_FLAT"})
        public TestItem testItem;

        private DynamoDbTable<?> v2Table;
        private DynamoDBMapper v1DdbMapper;


        @Setup
        public void setup(Blackhole bh) {
            DynamoDbEnhancedClient v2DdbEnh = DynamoDbEnhancedClient.builder()
                    .dynamoDbClient(getV2Client(bh, testItem.v2Response))
                    .build();

            v2Table = v2DdbEnh.table(testItem.name(), testItem.schema);

            v1DdbMapper = new DynamoDBMapper(getV1Client(bh, testItem.v1Response));
        }

        public enum TestItem {
            TINY(
                    V2ItemFactory.TINY_BEAN_TABLE_SCHEMA,
                    ScanResponse.builder()
                                .items(Arrays.asList(V2_ITEM_FACTORY.tiny(),
                                                     V2_ITEM_FACTORY.tiny(),
                                                     V2_ITEM_FACTORY.tiny()))
                                .build(),

                    V1ItemFactory.V1TinyBean.class,
                    new ScanResult().withItems(
                        Arrays.asList(V1_ITEM_FACTORY.tiny(), V1_ITEM_FACTORY.tiny(), V1_ITEM_FACTORY.tiny()))
            ),
            SMALL(
                    V2ItemFactory.SMALL_BEAN_TABLE_SCHEMA,
                    ScanResponse.builder()
                                .items(Arrays.asList(V2_ITEM_FACTORY.small(),
                                                     V2_ITEM_FACTORY.small(),
                                                     V2_ITEM_FACTORY.small()))
                                .build(),

                    V1ItemFactory.V1SmallBean.class,
                    new ScanResult().withItems(
                        Arrays.asList(V1_ITEM_FACTORY.small(), V1_ITEM_FACTORY.small(), V1_ITEM_FACTORY.small()))
            ),

            HUGE(
                    V2ItemFactory.HUGE_BEAN_TABLE_SCHEMA,
                    ScanResponse.builder()
                                .items(Arrays.asList(V2_ITEM_FACTORY.huge(),
                                                     V2_ITEM_FACTORY.huge(),
                                                     V2_ITEM_FACTORY.huge()))
                                .build(),

                    V1ItemFactory.V1HugeBean.class,
                    new ScanResult().withItems(
                        Arrays.asList(V1_ITEM_FACTORY.huge(), V1_ITEM_FACTORY.huge(), V1_ITEM_FACTORY.huge()))
            ),

            HUGE_FLAT(
                    V2ItemFactory.HUGE_BEAN_FLAT_TABLE_SCHEMA,
                    ScanResponse.builder()
                                .items(Arrays.asList(V2_ITEM_FACTORY.hugeFlat(),
                                                     V2_ITEM_FACTORY.hugeFlat(),
                                                     V2_ITEM_FACTORY.hugeFlat()))
                                .build(),

                    V1ItemFactory.V1HugeBeanFlat.class,
                    new ScanResult().withItems(
                        Arrays.asList(V1_ITEM_FACTORY.hugeFlat(), V1_ITEM_FACTORY.hugeFlat(), V1_ITEM_FACTORY.hugeFlat()))
            ),
            ;

            // V2
            private TableSchema<?> schema;
            private ScanResponse v2Response;

            // V1
            private Class<?> v1BeanClass;
            private ScanResult v1Response;

            TestItem(TableSchema<?> schema,
                     ScanResponse v2Response,

                     Class<?> v1BeanClass,
                     ScanResult v1Response) {
                this.schema = schema;
                this.v2Response = v2Response;

                this.v1BeanClass = v1BeanClass;
                this.v1Response = v1Response;
            }

            public Class<?> getV1BeanClass() {
                return v1BeanClass;
            }
        }
    }
}
