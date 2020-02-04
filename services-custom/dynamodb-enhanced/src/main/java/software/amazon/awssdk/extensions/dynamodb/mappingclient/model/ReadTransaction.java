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

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.MappedTableResource;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.OperationContext;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.TransactableReadOperation;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.operations.GetItem;
import software.amazon.awssdk.services.dynamodb.model.TransactGetItem;

/**
 * Encapsulates a single read transaction that can form a list of transactions that go into a
 * {@link TransactGetItemsEnhancedRequest}.
 * Example:
 *
 * {@code
 * ReadTransaction.create(table1, GetItem.create(Key.create(stringValue("id123"))));
 * ReadTransaction.create(table2, GetItem.create(Key.create(stringValue("id456"))));
 * }
 *
 * @param <T> The type of object this transaction applies to. Can be safely erased as it's not needed outside the
 *            class itself.
 */
@SdkPublicApi
public class ReadTransaction<T> {
    private final MappedTableResource<T> mappedTableResource;
    private final TransactableReadOperation<T> readOperation;

    private ReadTransaction(Builder<T> builder) {
        this.mappedTableResource = builder.mappedTableResource;
        this.readOperation = builder.readOperation;
    }

    public static <T> ReadTransaction<T> create(MappedTableResource<T> mappedTableResource,
                                            TransactableReadOperation<T> readOperation) {
        return new Builder<T>().mappedTableResource(mappedTableResource).readOperation(readOperation).build();
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public Builder<T> toBuilder() {
        return new Builder<T>().mappedTableResource(mappedTableResource).readOperation(readOperation);
    }

    public MappedTableResource<T> mappedTableResource() {
        return mappedTableResource;
    }

    public TransactableReadOperation<T> readOperation() {
        return readOperation;
    }

    /**
     * This method is used by the internal transactGetItems operation to generate a transact read item used in the call to
     * DynamoDb. Each {@link TransactableReadOperation}, such as {@link GetItem}, creates a transact get item corresponding to
     * that operation. The method should only be called from the transactGetItems operation and should not be used for other
     * purposes.
     *
     * @return A {@link TransactGetItem} that will be used in calls to DynamoDb.
     */
    public TransactGetItem generateTransactGetItem() {
        return readOperation.generateTransactGetItem(mappedTableResource.tableSchema(),
                                                     OperationContext.create(mappedTableResource.tableName()),
                                                     mappedTableResource.mapperExtension());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ReadTransaction<?> that = (ReadTransaction<?>) o;

        if (mappedTableResource != null ? !mappedTableResource.equals(that.mappedTableResource)
            : that.mappedTableResource != null) {

            return false;
        }
        return readOperation != null ? readOperation.equals(that.readOperation) : that.readOperation == null;
    }

    @Override
    public int hashCode() {
        int result = mappedTableResource != null ? mappedTableResource.hashCode() : 0;
        result = 31 * result + (readOperation != null ? readOperation.hashCode() : 0);
        return result;
    }

    public static final class Builder<T> {
        private MappedTableResource<T> mappedTableResource;
        private TransactableReadOperation<T> readOperation;

        private Builder() {
        }

        public Builder<T> mappedTableResource(MappedTableResource<T> mappedTableResource) {
            this.mappedTableResource = mappedTableResource;
            return this;
        }

        public Builder<T> readOperation(TransactableReadOperation<T> readOperation) {
            this.readOperation = readOperation;
            return this;
        }

        public ReadTransaction<T> build() {
            return new ReadTransaction<>(this);
        }
    }
}
