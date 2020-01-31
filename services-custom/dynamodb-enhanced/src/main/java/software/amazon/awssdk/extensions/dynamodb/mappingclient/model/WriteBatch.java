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

import java.util.Arrays;
import java.util.Collection;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.BatchableWriteOperation;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.MappedTableResource;

@SdkPublicApi
public class WriteBatch<T> {
    private final MappedTableResource<T> mappedTableResource;
    private final Collection<BatchableWriteOperation<T>> writeOperations;

    private WriteBatch(MappedTableResource<T> mappedTableResource,
                       Collection<BatchableWriteOperation<T>> writeOperations) {
        this.mappedTableResource = mappedTableResource;
        this.writeOperations = writeOperations;
    }

    public static <T> WriteBatch<T> create(MappedTableResource<T> mappedTableResource,
                                       Collection<BatchableWriteOperation<T>> writeOperations) {
        return new WriteBatch<>(mappedTableResource, writeOperations);
    }

    @SafeVarargs
    public static <T> WriteBatch<T> create(MappedTableResource<T> mappedTableResource,
                                       BatchableWriteOperation<T>... writeOperations) {
        return new WriteBatch<>(mappedTableResource, Arrays.asList(writeOperations));
    }

    public MappedTableResource<T> mappedTableResource() {
        return mappedTableResource;
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

}
