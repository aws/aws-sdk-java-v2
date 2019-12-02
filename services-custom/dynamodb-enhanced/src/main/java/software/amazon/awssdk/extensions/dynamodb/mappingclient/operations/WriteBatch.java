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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.BatchableWriteOperation;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.MappedTable;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.OperationContext;
import software.amazon.awssdk.services.dynamodb.model.WriteRequest;

@SdkPublicApi
public class WriteBatch<T> {
    private final MappedTable<T> mappedTable;
    private final Collection<BatchableWriteOperation<T>> writeOperations;

    private WriteBatch(MappedTable<T> mappedTable, Collection<BatchableWriteOperation<T>> writeOperations) {
        this.mappedTable = mappedTable;
        this.writeOperations = writeOperations;
    }

    public static <T> WriteBatch<T> of(MappedTable<T> mappedTable,
                                       Collection<BatchableWriteOperation<T>> writeOperations) {
        return new WriteBatch<>(mappedTable, writeOperations);
    }

    @SafeVarargs
    public static <T> WriteBatch<T> of(MappedTable<T> mappedTable,
                                       BatchableWriteOperation<T>... writeOperations) {
        return new WriteBatch<>(mappedTable, Arrays.asList(writeOperations));
    }

    public MappedTable<T> mappedTable() {
        return mappedTable;
    }

    public Collection<BatchableWriteOperation<T>> writeOperations() {
        return writeOperations;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        WriteBatch<?> that = (WriteBatch<?>) o;

        if (mappedTable != null ? ! mappedTable.equals(that.mappedTable) : that.mappedTable != null) {
            return false;
        }
        return writeOperations != null ? writeOperations.equals(that.writeOperations) : that.writeOperations == null;
    }

    @Override
    public int hashCode() {
        int result = mappedTable != null ? mappedTable.hashCode() : 0;
        result = 31 * result + (writeOperations != null ? writeOperations.hashCode() : 0);
        return result;
    }

    // Due to raw-type erasure, it's necessary to pass a map in to be mutated rather than try and extract the write
    // requests which would be more straight forward but Collection<WriteRequest> becomes Collection when you try and
    // exfiltrate the objects from a raw-typed WriteBatch.
    void addWriteRequestsToMap(Map<String, Collection<WriteRequest>> writeRequestMap) {
        Collection<WriteRequest> writeRequestsForTable = writeRequestMap.computeIfAbsent(getTableName(),
            ignored -> new ArrayList<>());

        writeRequestsForTable.addAll(
            writeOperations.stream()
                           .map(writeOperation ->
                                    writeOperation.generateWriteRequest(mappedTable.tableSchema(),
                                                                        OperationContext.of(mappedTable.tableName()),
                                                                        mappedTable.mapperExtension()))
                           .collect(Collectors.toList()));
    }

    String getTableName() {
        return mappedTable.tableName();
    }
}
