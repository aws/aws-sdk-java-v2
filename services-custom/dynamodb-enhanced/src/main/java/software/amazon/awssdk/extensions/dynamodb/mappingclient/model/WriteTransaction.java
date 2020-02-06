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
import software.amazon.awssdk.extensions.dynamodb.mappingclient.TransactableWriteOperation;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.operations.PutItem;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;

/**
 * Encapsulates a single write transaction that can form a list of transactions that go into a
 * {@link TransactWriteItemsEnhancedRequest}.
 * Example:
 *
 * {@code
 * WriteTransaction.create(myTable, putItem.create(myItem));
 * WriteTransaction.create(myTable, deleteItem.create(Key.create(stringValue("id123"))));
 * }
 *
 * @param <T> The type of object this transaction applies to. Can be safely erased as it's not needed outside the
 *            class itself.
 */
@SdkPublicApi
public class WriteTransaction<T> {
    private final MappedTableResource<T> mappedTableResource;
    private final TransactableWriteOperation<T> writeOperation;

    private WriteTransaction(Builder<T> builder) {
        this.mappedTableResource = builder.mappedTableResource;
        this.writeOperation = builder.writeOperation;
    }

    public static <T> WriteTransaction<T> create(MappedTableResource<T> mappedTableResource,
                                             TransactableWriteOperation<T> writeOperation) {
        return new Builder<T>().mappedTableResource(mappedTableResource).writeOperation(writeOperation).build();
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public Builder<T> toBuilder() {
        return new Builder<T>().mappedTableResource(mappedTableResource).writeOperation(writeOperation);
    }

    public MappedTableResource<T> mappedTableResource() {
        return mappedTableResource;
    }

    public TransactableWriteOperation<T> writeOperation() {
        return writeOperation;
    }

    /**
     * This method is used by the internal transactWriteItems operation to generate a transact write item used in the call to
     * DynamoDb. Each {@link TransactableWriteOperation}, such as {@link PutItem}, creates a transact write item corresponding to
     * that operation. The method should only be called from the transactWriteItems operation and should not be used for other
     * purposes.
     *
     * @return A {@link TransactWriteItem} that will be used in calls to DynamoDb.
     */
    public TransactWriteItem generateTransactWriteItem() {
        return writeOperation.generateTransactWriteItem(mappedTableResource.tableSchema(),
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

        WriteTransaction<?> that = (WriteTransaction<?>) o;

        if (mappedTableResource != null ? !mappedTableResource.equals(that.mappedTableResource) :
            that.mappedTableResource != null) {

            return false;
        }
        return writeOperation != null ? writeOperation.equals(that.writeOperation) : that.writeOperation == null;
    }

    @Override
    public int hashCode() {
        int result = mappedTableResource != null ? mappedTableResource.hashCode() : 0;
        result = 31 * result + (writeOperation != null ? writeOperation.hashCode() : 0);
        return result;
    }

    public static final class Builder<T> {
        private MappedTableResource<T> mappedTableResource;
        private TransactableWriteOperation<T> writeOperation;

        private Builder() {
        }

        public Builder<T> mappedTableResource(MappedTableResource<T> mappedTableResource) {
            this.mappedTableResource = mappedTableResource;
            return this;
        }

        public Builder<T> writeOperation(TransactableWriteOperation<T> writeOperation) {
            this.writeOperation = writeOperation;
            return this;
        }

        public WriteTransaction<T> build() {
            return new WriteTransaction<>(this);
        }
    }
}
