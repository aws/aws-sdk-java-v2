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
import software.amazon.awssdk.extensions.dynamodb.mappingclient.TransactableWriteOperation;

/**
 * Encapsulates a single write transaction that can form a list of transactions that go into a TransactWriteItems
 * operation. Example:
 *
 * {@code
 * WriteTransaction.of(myTable, putItem(myItem));
 * WriteTransaction.of(myTable, deleteItem(Key.of(stringValue("id123"))));
 * }
 *
 * @param <T> The type of object this transaction applies to. Can be safely erased as it's not needed outside the
 *            class itself.
 */
@SdkPublicApi
public class WriteTransaction<T> {
    private final MappedTableResource<T> mappedTableResource;
    private final TransactableWriteOperation<T> writeOperation;

    private WriteTransaction(MappedTableResource<T> mappedTableResource, TransactableWriteOperation<T> writeOperation) {
        this.mappedTableResource = mappedTableResource;
        this.writeOperation = writeOperation;
    }

    public static <T> WriteTransaction<T> create(MappedTableResource<T> mappedTableResource,
                                             TransactableWriteOperation<T> writeOperation) {
        return new WriteTransaction<>(mappedTableResource, writeOperation);
    }

    public MappedTableResource<T> mappedTableResource() {
        return mappedTableResource;
    }

    public TransactableWriteOperation<T> writeOperation() {
        return writeOperation;
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

}
