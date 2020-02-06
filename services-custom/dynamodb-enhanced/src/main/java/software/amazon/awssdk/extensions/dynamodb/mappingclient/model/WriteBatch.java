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

package software.amazon.awssdk.extensions.dynamodb.mappingclient.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.BatchableWriteOperation;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.MappedTableResource;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.OperationContext;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.operations.PutItem;
import software.amazon.awssdk.services.dynamodb.model.WriteRequest;

/**
 * Encapsulates a single write batch that can form a list of write batches that go into a {@link BatchWriteItemEnhancedRequest}.
 * Example:
 *
 * {@code
 * WriteBatch.create(myTable, putItem.create(myItem));
 * WriteBatch.create(myTable, deleteItem(Key.of(stringValue("id123"))));
 * }
 *
 * @param <T> The type of object this batch applies to. Can be safely erased as it's not needed outside the
 *            class itself.
 */
@SdkPublicApi
public class WriteBatch<T> {
    private final MappedTableResource<T> mappedTableResource;
    private final List<BatchableWriteOperation<T>> writeOperations;

    private WriteBatch(Builder<T> builder) {
        this.mappedTableResource = builder.mappedTableResource;
        this.writeOperations = Collections.unmodifiableList(builder.writeOperations);
    }

    public static <T> WriteBatch<T> create(MappedTableResource<T> mappedTableResource,
                                       Collection<BatchableWriteOperation<T>> writeOperations) {
        return new Builder<T>().mappedTableResource(mappedTableResource).writeOperations(writeOperations).build();
    }

    @SafeVarargs
    public static <T> WriteBatch<T> create(MappedTableResource<T> mappedTableResource,
                                       BatchableWriteOperation<T>... writeOperations) {
        return new Builder<T>().mappedTableResource(mappedTableResource).writeOperations(writeOperations).build();
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public Builder<T> toBuilder() {
        return new Builder<T>().mappedTableResource(mappedTableResource).writeOperations(writeOperations);
    }

    public MappedTableResource<T> mappedTableResource() {
        return mappedTableResource;
    }

    public Collection<BatchableWriteOperation<T>> writeOperations() {
        return writeOperations;
    }

    /**
     * This method is used by the internal batchWriteItem operation to generate list of batch write requests used in the call to
     * DynamoDb. Each {@link BatchableWriteOperation}, such as {@link PutItem}, creates a batch write request corresponding to
     * that operation. The method should only be called from the batchWriteItem operation and should not be used for other
     * purposes.
     *
     * @param writeRequestMap An empty map to store the batch write requests in. Due to raw-type erasure, it's necessary to
     * pass a map in to be mutated rather than try and extract the write requests which would be more straight forward,
     * but Collection<WriteRequest> becomes Collection when you try and exfiltrate the objects from a raw-typed WriteBatch.
     */
    public void addWriteRequestsToMap(Map<String, Collection<WriteRequest>> writeRequestMap) {
        Collection<WriteRequest> writeRequestsForTable = writeRequestMap
            .computeIfAbsent(mappedTableResource.tableName(), ignored -> new ArrayList<>());

        writeRequestsForTable.addAll(writeOperations.stream()
                                                    .map(operation -> operation.generateWriteRequest(
                                                        mappedTableResource.tableSchema(),
                                                        OperationContext.create(mappedTableResource.tableName()),
                                                        mappedTableResource.mapperExtension()))
                                                    .collect(Collectors.toList()));
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

        if (mappedTableResource != null ? !mappedTableResource.equals(that.mappedTableResource)
            : that.mappedTableResource != null) {

            return false;
        }
        return writeOperations != null ? writeOperations.equals(that.writeOperations) : that.writeOperations == null;
    }

    @Override
    public int hashCode() {
        int result = mappedTableResource != null ? mappedTableResource.hashCode() : 0;
        result = 31 * result + (writeOperations != null ? writeOperations.hashCode() : 0);
        return result;
    }

    public static final class Builder<T> {
        private MappedTableResource<T> mappedTableResource;
        private List<BatchableWriteOperation<T>> writeOperations;

        private Builder() {
        }

        public Builder<T> mappedTableResource(MappedTableResource<T> mappedTableResource) {
            this.mappedTableResource = mappedTableResource;
            return this;
        }

        public Builder<T> writeOperations(Collection<BatchableWriteOperation<T>> writeOperations) {
            this.writeOperations = new ArrayList<>(writeOperations);
            return this;
        }

        public Builder<T> writeOperations(BatchableWriteOperation<T> ...writeOperations) {
            this.writeOperations = Arrays.asList(writeOperations);
            return this;
        }

        public Builder addWriteOperation(BatchableWriteOperation<T> writeOperation) {
            if (writeOperations == null) {
                writeOperations = new ArrayList<>();
            }
            writeOperations.add(writeOperation);
            return this;
        }

        public WriteBatch<T> build() {
            return new WriteBatch<>(this);
        }
    }
}
