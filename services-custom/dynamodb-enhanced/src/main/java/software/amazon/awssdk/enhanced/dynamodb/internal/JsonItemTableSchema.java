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

package software.amazon.awssdk.enhanced.dynamodb.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.JsonItem;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.utils.Validate;

/**
 * Implementation of {@link TableSchema} that builds a schema that is used to while using enhanced ddb client with Json input and
 * outputs {@link TableSchema}.
 * <p>
 */
@SdkInternalApi
public final class JsonItemTableSchema implements TableSchema<JsonItem> {

    private final TableMetadata tableMetadata;

    public JsonItemTableSchema(Builder builder) {
        Validate.paramNotNull(builder.tableMetadata, "tableMetadata");
        this.tableMetadata = builder.tableMetadata;

    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public TableMetadata tableMetadata() {
        return tableMetadata;
    }

    @Override
    public EnhancedType<JsonItem> itemType() {
        return EnhancedType.of(JsonItem.class);
    }

    @Override
    public List<String> attributeNames() {
        throw new UnsupportedOperationException("attributeNames is not supported for JsonImmutableTableSchema");
    }

    @Override
    public boolean isAbstract() {
        return false;
    }

    @Override
    public JsonItem mapToItem(Map<String, AttributeValue> attributeMap) {
        if (attributeMap == null) {
            return null;
        }
        return JsonItem.fromAttributeValueMap(attributeMap);
    }

    @Override
    public Map<String, AttributeValue> itemToMap(JsonItem item, boolean ignoreNulls) {
        if (item == null) {
            return Collections.emptyMap();
        }
        return item.itemToMap();
    }

    @Override
    public Map<String, AttributeValue> itemToMap(JsonItem item, Collection<String> attributes) {

        Map<String, AttributeValue> attributeValueMap = item.itemToMap();
        Map<String, AttributeValue> newMap = new LinkedHashMap<>();
        attributes.forEach(key -> Optional.ofNullable(attributeValueMap.get(key))
                                          .ifPresent(value -> newMap.put(key, value)));
        return newMap;
    }

    @Override
    public AttributeValue attributeValue(JsonItem item, String attributeName) {
        if (item != null) {
            return item.itemToMap().get(attributeName);
        }
        return null;
    }

    public static class Builder {
        private TableMetadata tableMetadata;

        public Builder tableMetadata(TableMetadata tableMetadata) {
            this.tableMetadata = tableMetadata;
            return this;
        }

        public JsonItemTableSchema build() {
            return new JsonItemTableSchema(this);
        }
    }
}
