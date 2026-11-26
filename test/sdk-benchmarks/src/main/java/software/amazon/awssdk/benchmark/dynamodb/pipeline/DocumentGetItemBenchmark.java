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

package software.amazon.awssdk.benchmark.dynamodb.pipeline;

import static software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider.defaultProvider;

import java.util.List;
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
import software.amazon.awssdk.benchmark.dynamodb.DynamoDbBenchmarkConstant;
import software.amazon.awssdk.benchmark.dynamodb.fixture.DynamoDbBenchmarkFixture;
import software.amazon.awssdk.benchmark.dynamodb.mock.DynamoDbMockClientFactory;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Tier C DOCUMENT sync GetItem: Enhanced Document mapping plus full mocked SDK pipeline.
 *
 * <p>No low-level {@code GetItemRequest} is pre-built. Response AttributeValues are mapped to
 * {@link EnhancedDocument} inside the timed {@link DynamoDbTable#getItem(Key)} call, and
 * representative attributes are read via Document accessors so lazy AttributeValue→Java conversion
 * is included in the measurement.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
@Fork(2)
public class DocumentGetItemBenchmark {

    private DynamoDbClient lowLevelClient;
    private DynamoDbTable<EnhancedDocument> table;
    private Key key;

    @Setup(Level.Trial)
    public void setup() {
        DynamoDbBenchmarkFixture fixture = DynamoDbBenchmarkFixture.create();
        lowLevelClient = DynamoDbMockClientFactory.syncGetItemClient(fixture.attributeMap());
        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                                                                      .dynamoDbClient(lowLevelClient)
                                                                      .build();
        TableSchema<EnhancedDocument> documentSchema =
            TableSchema.documentSchemaBuilder()
                       .addIndexPartitionKey(TableMetadata.primaryIndexName(),
                                             DynamoDbBenchmarkFixture.PARTITION_KEY_NAME,
                                             AttributeValueType.S)
                       .attributeConverterProviders(defaultProvider())
                       .build();
        table = enhancedClient.table(DynamoDbBenchmarkConstant.TABLE_NAME, documentSchema);
        key = Key.builder()
                 .partitionValue(DynamoDbBenchmarkFixture.PARTITION_KEY_VALUE)
                 .build();

        // One-time smoke call: verifies response→EnhancedDocument mapping + accessor conversion and
        // warms the SDK pipeline before JMH warmup iterations begin.
        EnhancedDocument smoke = table.getItem(key);
        if (smoke == null) {
            throw new IllegalStateException("Document GetItem smoke call returned null");
        }
        consumeRepresentativeAttributes(smoke, null);
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        if (lowLevelClient != null) {
            lowLevelClient.close();
        }
    }

    /**
     * Times getItem plus lazy Document accessor conversion for the shared fixture attributes.
     */
    @Benchmark
    public void getItem(Blackhole bh) {
        EnhancedDocument document = table.getItem(key);
        consumeRepresentativeAttributes(document, bh);
    }

    /**
     * Forces AttributeValue→Java conversion for every representative fixture attribute. When
     * {@code bh} is null (setup smoke), values are only validated for non-null content.
     */
    public static void consumeRepresentativeAttributes(EnhancedDocument document, Blackhole bh) {
        String pk = document.getString(DynamoDbBenchmarkFixture.PARTITION_KEY_NAME);
        String stringAttr = document.getString(DynamoDbBenchmarkFixture.STRING_ATTR);
        Object numberAttr = document.getNumber(DynamoDbBenchmarkFixture.NUMBER_ATTR);
        Boolean boolAttr = document.getBoolean(DynamoDbBenchmarkFixture.BOOL_ATTR);
        List<String> stringList = document.getList(DynamoDbBenchmarkFixture.STRING_LIST_ATTR,
                                                   EnhancedType.of(String.class));
        Map<String, String> stringMap = document.getMap(DynamoDbBenchmarkFixture.STRING_MAP_ATTR,
                                                        EnhancedType.of(String.class),
                                                        EnhancedType.of(String.class));
        Map<String, AttributeValue> nestedAv =
            document.getMapOfUnknownType(DynamoDbBenchmarkFixture.NESTED_ATTR);
        EnhancedDocument nested = EnhancedDocument.fromAttributeValueMap(nestedAv);
        String nestedString = nested.getString(DynamoDbBenchmarkFixture.NESTED_STRING_ATTR);
        Object nestedNumber = nested.getNumber(DynamoDbBenchmarkFixture.NESTED_NUMBER_ATTR);

        if (pk == null || stringAttr == null || numberAttr == null || boolAttr == null
            || stringList == null || stringList.isEmpty()
            || stringMap == null || stringMap.isEmpty()
            || nestedString == null || nestedNumber == null) {
            throw new IllegalStateException("Document GetItem did not expose usable converted attributes");
        }

        if (bh != null) {
            bh.consume(pk);
            bh.consume(stringAttr);
            bh.consume(numberAttr);
            bh.consume(boolAttr);
            bh.consume(stringList);
            bh.consume(stringMap);
            bh.consume(nestedString);
            bh.consume(nestedNumber);
        }
    }
}
