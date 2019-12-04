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

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.MappedTable;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.OperationContext;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.TransactableReadOperation;
import software.amazon.awssdk.services.dynamodb.model.TransactGetItem;

@SdkPublicApi
public class ReadTransaction<T> {
    private final MappedTable<T> mappedTable;
    private final TransactableReadOperation<T> readOperation;

    private ReadTransaction(MappedTable<T> mappedTable, TransactableReadOperation<T> readOperation) {
        this.mappedTable = mappedTable;
        this.readOperation = readOperation;
    }

    public static <T> ReadTransaction<T> of(MappedTable<T> mappedTable, TransactableReadOperation<T> readOperation) {
        return new ReadTransaction<>(mappedTable, readOperation);
    }

    public MappedTable<T> mappedTable() {
        return mappedTable;
    }

    public TransactableReadOperation<T> readOperation() {
        return readOperation;
    }

    TransactGetItem generateTransactGetItem() {
        return readOperation.generateTransactGetItem(mappedTable.tableSchema(),
                                                     OperationContext.of(mappedTable.tableName()),
                                                     mappedTable.mapperExtension());
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

        if (mappedTable != null ? ! mappedTable.equals(that.mappedTable) : that.mappedTable != null) {
            return false;
        }
        return readOperation != null ? readOperation.equals(that.readOperation) : that.readOperation == null;
    }

    @Override
    public int hashCode() {
        int result = mappedTable != null ? mappedTable.hashCode() : 0;
        result = 31 * result + (readOperation != null ? readOperation.hashCode() : 0);
        return result;
    }
}
