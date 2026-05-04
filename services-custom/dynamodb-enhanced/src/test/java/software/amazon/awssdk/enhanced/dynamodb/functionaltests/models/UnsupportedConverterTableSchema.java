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

package software.amazon.awssdk.enhanced.dynamodb.functionaltests.models;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableMetadata;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Pass-through map schema that intentionally keeps the default {@link TableSchema#converterForAttribute(Object)}
 * implementation, which throws {@link UnsupportedOperationException}.
 */
@SuppressWarnings("unchecked")
public final class UnsupportedConverterTableSchema implements TableSchema<Map<String, AttributeValue>> {
    private final TableMetadata tableMetadata = StaticTableMetadata.builder()
                                                                  .addIndexPartitionKey(TableMetadata.primaryIndexName(), "pk",
                                                                                        AttributeValueType.S)
                                                                  .build();

    @Override
    public Map<String, AttributeValue> mapToItem(Map<String, AttributeValue> attributeMap) {
        return attributeMap == null ? Collections.emptyMap() : new HashMap<>(attributeMap);
    }

    @Override
    public Map<String, AttributeValue> itemToMap(Map<String, AttributeValue> item, boolean ignoreNulls) {
        if (item == null) {
            return Collections.emptyMap();
        }

        if (!ignoreNulls) {
            return new HashMap<>(item);
        }

        return item.entrySet()
                   .stream()
                   .filter(e -> e.getValue() != null && !Boolean.TRUE.equals(e.getValue().nul()))
                   .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public Map<String, AttributeValue> itemToMap(Map<String, AttributeValue> item, Collection<String> attributes) {
        if (item == null || attributes == null) {
            return Collections.emptyMap();
        }

        return attributes.stream()
                         .filter(item::containsKey)
                         .collect(Collectors.toMap(a -> a, item::get));
    }

    @Override
    public AttributeValue attributeValue(Map<String, AttributeValue> item, String attributeName) {
        return item == null ? null : item.get(attributeName);
    }

    @Override
    public TableMetadata tableMetadata() {
        return tableMetadata;
    }

    @Override
    public EnhancedType<Map<String, AttributeValue>> itemType() {
        return (EnhancedType<Map<String, AttributeValue>>) (EnhancedType<?>) EnhancedType.of(Map.class);
    }

    @Override
    public List<String> attributeNames() {
        return Collections.singletonList("pk");
    }

    @Override
    public boolean isAbstract() {
        return false;
    }
}
