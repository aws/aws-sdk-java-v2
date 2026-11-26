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

import static software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider.defaultProvider;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
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

    public static final String STRING_ATTR = "stringAttr";
    public static final String NUMBER_ATTR = "numberAttr";
    public static final String BOOL_ATTR = "boolAttr";
    public static final String STRING_LIST_ATTR = "stringList";
    public static final String STRING_MAP_ATTR = "stringMap";
    public static final String NESTED_ATTR = "nested";
    public static final String NESTED_STRING_ATTR = "nestedString";
    public static final String NESTED_NUMBER_ATTR = "nestedNumber";

    private static final TableSchema<BenchmarkItem> TABLE_SCHEMA = TableSchema.fromBean(BenchmarkItem.class);

    /**
     * Nested-document schema used so {@code put(nested, EnhancedDocument)} keeps Java values until
     * {@code toMap()} / {@code itemToMap()}, matching typical Document Put conversion work.
     */
    private static final TableSchema<EnhancedDocument> NESTED_DOCUMENT_SCHEMA =
        TableSchema.documentSchemaBuilder()
                   .attributeConverterProviders(defaultProvider())
                   .build();

    private static final EnhancedType<EnhancedDocument> NESTED_DOCUMENT_TYPE =
        EnhancedType.documentOf(EnhancedDocument.class, NESTED_DOCUMENT_SCHEMA);

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
        // Build via Document put* APIs (Java values), not fromAttributeValueMap, so DOCUMENT Put
        // measures real Java→AttributeValue conversion inside toMap()/itemToMap().
        EnhancedDocument document = newDocumentFromJavaValues(item);
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

    /**
     * Document built from Java values so timed Put→{@code toMap()} performs converter work rather
     * than identity-copying pre-seeded {@link AttributeValue}s.
     */
    private static EnhancedDocument newDocumentFromJavaValues(BenchmarkItem item) {
        EnhancedDocument nested = EnhancedDocument.builder()
                                                  .attributeConverterProviders(defaultProvider())
                                                  .putString(NESTED_STRING_ATTR, item.getNested().getNestedString())
                                                  .putNumber(NESTED_NUMBER_ATTR, item.getNested().getNestedNumber())
                                                  .build();

        List<String> stringList = item.getStringList();
        Map<String, String> stringMap = item.getStringMap();

        return EnhancedDocument.builder()
                               .attributeConverterProviders(defaultProvider())
                               .putString(PARTITION_KEY_NAME, item.getPk())
                               .putString(STRING_ATTR, item.getStringAttr())
                               .putNumber(NUMBER_ATTR, item.getNumberAttr())
                               .putBoolean(BOOL_ATTR, item.getBoolAttr())
                               .putList(STRING_LIST_ATTR, stringList, EnhancedType.of(String.class))
                               .putMap(STRING_MAP_ATTR, stringMap,
                                       EnhancedType.of(String.class), EnhancedType.of(String.class))
                               .put(NESTED_ATTR, nested, NESTED_DOCUMENT_TYPE)
                               .build();
    }
}
