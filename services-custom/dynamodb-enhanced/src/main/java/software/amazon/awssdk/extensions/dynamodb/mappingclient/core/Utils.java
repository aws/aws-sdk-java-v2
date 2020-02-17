/*
 * Copyright 2010-2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.extensions.dynamodb.mappingclient.core;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.Key;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.MapperExtension;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.OperationContext;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.Page;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.TableSchema;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.extensions.ReadModification;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@SdkInternalApi
public final class Utils {
    private Utils() {

    }

    /** There is a divergence in what constitutes an acceptable attribute name versus a token used in expression
     * names or values. Since the mapper translates one to the other, it is necessary to scrub out all these
     * 'illegal' characters before adding them to expression values or expression names.
     *
     * @param key A key that may contain non alpha-numeric characters acceptable to a DynamoDb attribute name.
     * @return A key that has all these characters scrubbed and overwritten with an underscore.
     */
    public static String cleanAttributeName(String key) {
        boolean somethingChanged = false;
        char[] chars = key.toCharArray();

        for (int i = 0; i < chars.length; ++i) {
            if (chars[i] == '*' || chars[i] == '.' || chars[i] == '-') {
                chars[i] = '_';
                somethingChanged = true;
            }
        }

        return somethingChanged ? new String(chars) : key;
    }

    public static <T> T readAndTransformSingleItem(Map<String, AttributeValue> itemMap,
                                            TableSchema<T> tableSchema,
                                            OperationContext operationContext,
                                            MapperExtension mapperExtension) {
        if (itemMap == null || itemMap.isEmpty()) {
            return null;
        }

        if (mapperExtension != null) {
            ReadModification readModification = mapperExtension.afterRead(itemMap,
                                                                          operationContext,
                                                                          tableSchema.tableMetadata());

            if (readModification != null && readModification.transformedItem() != null) {
                return tableSchema.mapToItem(readModification.transformedItem());
            }
        }

        return tableSchema.mapToItem(itemMap);
    }

    public static <ResponseT, ItemT> Page<ItemT> readAndTransformPaginatedItems(
        ResponseT response,
        TableSchema<ItemT> tableSchema,
        OperationContext operationContext,
        MapperExtension mapperExtension,
        Function<ResponseT, List<Map<String, AttributeValue>>> getItems,
        Function<ResponseT, Map<String, AttributeValue>> getLastEvaluatedKey) {

        if (getLastEvaluatedKey.apply(response) == null || getLastEvaluatedKey.apply(response).isEmpty()) {
            // Last page
            return Page.create(getItems.apply(response)
                                   .stream()
                                   .map(itemMap -> readAndTransformSingleItem(itemMap,
                                                                              tableSchema,
                                                                              operationContext,
                                                                              mapperExtension))
                                   .collect(Collectors.toList()));
        } else {
            // More pages to come; add the lastEvaluatedKey
            return Page.create(getItems.apply(response)
                                   .stream()
                                   .map(itemMap -> readAndTransformSingleItem(itemMap,
                                                                              tableSchema,
                                                                              operationContext,
                                                                              mapperExtension))
                                   .collect(Collectors.toList()),
                           getLastEvaluatedKey.apply(response));
        }
    }

    public static <T> Key createKeyFromItem(T item, TableSchema<T> tableSchema, String indexName) {
        String partitionKeyName = tableSchema.tableMetadata().indexPartitionKey(indexName);
        Optional<String> sortKeyName = tableSchema.tableMetadata().indexSortKey(indexName);
        AttributeValue partitionKeyValue = tableSchema.attributeValue(item, partitionKeyName);
        Optional<AttributeValue> sortKeyValue = sortKeyName.map(key -> tableSchema.attributeValue(item, key));

        return sortKeyValue.map(attributeValue -> Key.create(partitionKeyValue, attributeValue))
                           .orElseGet(() -> Key.create(partitionKeyValue));
    }

    public static <T> List<T> getItemsFromSupplier(List<Supplier<T>> itemSupplierList) {
        if (itemSupplierList == null || itemSupplierList.isEmpty()) {
            return null;
        }
        return Collections.unmodifiableList(itemSupplierList.stream()
                                                            .map(Supplier::get)
                                                            .collect(Collectors.toList()));
    }
}
