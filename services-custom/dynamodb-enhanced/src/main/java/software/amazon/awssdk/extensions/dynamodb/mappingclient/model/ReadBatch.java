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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.BatchableReadOperation;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.MappedTableResource;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.TableMetadata;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.operations.GetItem;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.KeysAndAttributes;

/**
 * Encapsulates a single write transaction that can form a list of transactions that go into a
 * {@link BatchGetItemEnhancedRequest}.
 * Example:
 *
 * {@code
 * ReadBatch.create(myTable, putItem(myItem));
 * ReadBatch.createf(myTable, deleteItem(Key.create(stringValue("id123"))));
 * }
 *
 * @param <T> The type of object this transaction applies to. Can be safely erased as it's not needed outside the
 *            class itself.
 */
@SdkPublicApi
public class ReadBatch<T> {
    private final MappedTableResource<T> mappedTableResource;
    private final List<BatchableReadOperation> readOperations;

    private ReadBatch(Builder<T> builder) {
        this.mappedTableResource = builder.mappedTableResource;
        this.readOperations = Collections.unmodifiableList(builder.readOperations);
    }

    public static <T> ReadBatch<T> create(MappedTableResource<T> mappedTableResource,
                                      Collection<BatchableReadOperation> readOperations) {
        return new Builder<T>().mappedTableResource(mappedTableResource).readOperations(readOperations).build();
    }

    public static <T> ReadBatch<T> create(MappedTableResource<T> mappedTableResource,
                                      BatchableReadOperation... readOperations) {
        return new Builder<T>().mappedTableResource(mappedTableResource).readOperations(readOperations).build();
    }

    public MappedTableResource<T> mappedTableResource() {
        return mappedTableResource;
    }

    public Collection<BatchableReadOperation> readOperations() {
        return readOperations;
    }

    /**
     * This method is used by the internal batchGetItem operation to generate the keys and attributes used in the call to
     * DynamoDb. Each {@link BatchableReadOperation}, i.e. {@link GetItem}, creates keys and attributes corresponding to
     * that operation. The method should only be called from the batchGetItem operation and should not be used for other
     * purposes.
     * </p>
     * DynamoDB requires all component GetItem requests in a BatchGetItem to have the same consistentRead setting
     * for any given table. The logic here uses the setting of the first getItem in a table batch and then checks
     * the rest are identical or throws an exception.
     *
     * @return A {@link KeysAndAttributes} object that will be used in calls to DynamoDb.
     */
    public KeysAndAttributes generateKeysAndAttributes() {
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
                          .map(key -> key.keyMap(mappedTableResource.tableSchema(),
                                                 TableMetadata.primaryIndexName()))
                          .collect(Collectors.toList());

        return KeysAndAttributes.builder()
                                .keys(keys)
                                .consistentRead(consistentRead.get())
                                .build();
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

        if (mappedTableResource != null ? !mappedTableResource.equals(readBatch.mappedTableResource) :
            readBatch.mappedTableResource != null) {

            return false;
        }
        return readOperations != null ? readOperations.equals(readBatch.readOperations) : readBatch.readOperations == null;
    }

    @Override
    public int hashCode() {
        int result = mappedTableResource != null ? mappedTableResource.hashCode() : 0;
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

    public static final class Builder<T> {
        private MappedTableResource<T> mappedTableResource;
        private List<BatchableReadOperation> readOperations;

        private Builder() {
        }

        public Builder<T> mappedTableResource(MappedTableResource<T> mappedTableResource) {
            this.mappedTableResource = mappedTableResource;
            return this;
        }

        public Builder<T> readOperations(Collection<BatchableReadOperation> readOperations) {
            this.readOperations = new ArrayList<>(readOperations);
            return this;
        }

        public Builder<T> readOperations(BatchableReadOperation ...readOperations) {
            this.readOperations = Arrays.asList(readOperations);
            return this;
        }

        public Builder addReadOperation(BatchableReadOperation readOperation) {
            if (readOperations == null) {
                readOperations = new ArrayList<>();
            }
            readOperations.add(readOperation);
            return this;
        }

        public ReadBatch<T> build() {
            return new ReadBatch<T>(this);
        }
    }
}
