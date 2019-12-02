/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.extensions.dynamodb.mappingclient.operations;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.BatchableReadOperation;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.MappedTable;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.TableMetadata;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.KeysAndAttributes;

@SdkPublicApi
public class ReadBatch<T> {
    private final MappedTable<T> mappedTable;
    private final Collection<BatchableReadOperation> readOperations;

    private ReadBatch(MappedTable<T> mappedTable, Collection<BatchableReadOperation> readOperations) {
        this.mappedTable = mappedTable;
        this.readOperations = readOperations;
    }

    public static <T> ReadBatch<T> of(MappedTable<T> mappedTable,
                                      Collection<BatchableReadOperation> readOperations) {
        return new ReadBatch<>(mappedTable, readOperations);
    }

    public static <T> ReadBatch<T> of(MappedTable<T> mappedTable,
                                      BatchableReadOperation... readOperations) {
        return new ReadBatch<>(mappedTable, Arrays.asList(readOperations));
    }

    void addReadRequestsToMap(Map<String, KeysAndAttributes> readRequestMap) {
        KeysAndAttributes newKeysAndAttributes = generateKeysAndAttributes();
        KeysAndAttributes existingKeysAndAttributes = readRequestMap.get(tableName());

        if (existingKeysAndAttributes == null) {
            readRequestMap.put(tableName(), newKeysAndAttributes);
            return;
        }

        KeysAndAttributes mergedKeysAndAttributes = mergeKeysAndAttributes(existingKeysAndAttributes,
                                                                           newKeysAndAttributes);

        readRequestMap.put(tableName(), mergedKeysAndAttributes);
    }

    String tableName() {
        return mappedTable.tableName();
    }

    private KeysAndAttributes generateKeysAndAttributes() {
        // DynamoDB requires all component GetItem requests in a BatchGetItem to have the same consistentRead setting
        // for any given table. The logic here uses the setting of the first getItem in a table batch and then checks
        // the rest are identical or throws an exception.
        AtomicReference<Boolean> consistentRead = new AtomicReference<>();
        AtomicBoolean firstRecord = new AtomicBoolean(true);

        List<Map<String, AttributeValue>> keys =
            readOperations.stream()
                          .peek(operation -> {
                              if (firstRecord.getAndSet(false)) {
                                  consistentRead.set(operation.consistentRead());
                              } else {
                                  if (!compareNullableBooleans(consistentRead.get(), operation.consistentRead())) {
                                      throw new IllegalArgumentException("All batchable read requests for the same "
                                                                         + "table must have the same 'consistentRead' "
                                                                         + "setting.");
                                  }
                              }
                          })
                          .map(BatchableReadOperation::key)
                          .map(key -> key.keyMap(mappedTable.tableSchema(),
                                                 TableMetadata.primaryIndexName()))
                          .collect(Collectors.toList());

        return KeysAndAttributes.builder()
                                .keys(keys)
                                .consistentRead(consistentRead.get())
                                .build();
    }

    public MappedTable<T> mappedTable() {
        return mappedTable;
    }

    public Collection<BatchableReadOperation> readOperations() {
        return readOperations;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ReadBatch<?> readBatch = (ReadBatch<?>) o;

        if (mappedTable != null ? ! mappedTable.equals(readBatch.mappedTable) : readBatch.mappedTable != null) {
            return false;
        }
        return readOperations != null ? readOperations.equals(readBatch.readOperations) : readBatch.readOperations == null;
    }

    @Override
    public int hashCode() {
        int result = mappedTable != null ? mappedTable.hashCode() : 0;
        result = 31 * result + (readOperations != null ? readOperations.hashCode() : 0);
        return result;
    }

    private static boolean compareNullableBooleans(Boolean one, Boolean two) {
        if (one == null && two == null) {
            return true;
        }

        if (one != null) {
            return one.equals(two);
        } else {
            return false;
        }
    }

    private static KeysAndAttributes mergeKeysAndAttributes(KeysAndAttributes first, KeysAndAttributes second) {
        if (!compareNullableBooleans(first.consistentRead(), second.consistentRead())) {
            throw new IllegalArgumentException("All batchable read requests for the same table must have the "
                                               + "same 'consistentRead' setting.");
        }

        Boolean consistentRead = first.consistentRead() == null ? second.consistentRead() : first.consistentRead();
        List<Map<String, AttributeValue>> keys =
            Stream.concat(first.keys().stream(), second.keys().stream()).collect(Collectors.toList());

        return KeysAndAttributes.builder()
                                .keys(keys)
                                .consistentRead(consistentRead)
                                .build();
    }
}
