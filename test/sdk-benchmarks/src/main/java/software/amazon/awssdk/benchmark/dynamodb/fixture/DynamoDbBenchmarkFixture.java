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

package software.amazon.awssdk.benchmark.dynamodb.fixture;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Deterministic DynamoDB item available as typed bean, {@link EnhancedDocument}, and low-level
 * {@link AttributeValue} map. Does not construct SDK clients or JMH state.
 */
public final class DynamoDbBenchmarkFixture {

    public static final String PARTITION_KEY_NAME = "pk";
    public static final String PARTITION_KEY_VALUE = "benchmark-item-1";

    private static final TableSchema<BenchmarkItem> TABLE_SCHEMA = TableSchema.fromBean(BenchmarkItem.class);

    private final BenchmarkItem item;
    private final Map<String, AttributeValue> attributeMap;
    private final EnhancedDocument document;

    private DynamoDbBenchmarkFixture(BenchmarkItem item,
                                     Map<String, AttributeValue> attributeMap,
                                     EnhancedDocument document) {
        this.item = item;
        this.attributeMap = attributeMap;
        this.document = document;
    }

    /**
     * Creates the shared representative fixture used across LOW / DOCUMENT / TYPED benchmarks.
     */
    public static DynamoDbBenchmarkFixture create() {
        BenchmarkItem item = newItem();
        Map<String, AttributeValue> attributeMap =
            Collections.unmodifiableMap(new LinkedHashMap<>(TABLE_SCHEMA.itemToMap(item, true)));
        EnhancedDocument document = EnhancedDocument.fromAttributeValueMap(attributeMap);
        return new DynamoDbBenchmarkFixture(item, attributeMap, document);
    }

    public BenchmarkItem item() {
        return item;
    }

    public Map<String, AttributeValue> attributeMap() {
        return attributeMap;
    }

    public EnhancedDocument document() {
        return document;
    }

    public TableSchema<BenchmarkItem> tableSchema() {
        return TABLE_SCHEMA;
    }

    public AttributeValue partitionKeyAttribute() {
        return AttributeValue.fromS(PARTITION_KEY_VALUE);
    }

    private static BenchmarkItem newItem() {
        BenchmarkItem.NestedAttrs nested = new BenchmarkItem.NestedAttrs();
        nested.setNestedString("nested-value");
        nested.setNestedNumber(7);

        Map<String, String> stringMap = new LinkedHashMap<>();
        stringMap.put("mapKeyA", "mapValueA");
        stringMap.put("mapKeyB", "mapValueB");

        BenchmarkItem item = new BenchmarkItem();
        item.setPk(PARTITION_KEY_VALUE);
        item.setStringAttr("benchmark-string");
        item.setNumberAttr(42);
        item.setBoolAttr(true);
        item.setStringList(Arrays.asList("list-one", "list-two", "list-three"));
        item.setStringMap(stringMap);
        item.setNested(nested);
        return item;
    }
}
