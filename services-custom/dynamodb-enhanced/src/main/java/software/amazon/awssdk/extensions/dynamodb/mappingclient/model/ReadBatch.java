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
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.BatchableReadOperation;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.MappedTableResource;

@SdkPublicApi
public class ReadBatch<T> {
    private final MappedTableResource<T> mappedTableResource;
    private final List<BatchableReadOperation> readOperations;

    private ReadBatch(MappedTableResource<T> mappedTableResource, List<BatchableReadOperation> readOperations) {
        this.mappedTableResource = mappedTableResource;
        this.readOperations = Collections.unmodifiableList(readOperations);
    }

    public static <T> ReadBatch<T> create(MappedTableResource<T> mappedTableResource,
                                      Collection<BatchableReadOperation> readOperations) {
        return new ReadBatch<>(mappedTableResource, new ArrayList<>(readOperations));
    }

    public static <T> ReadBatch<T> create(MappedTableResource<T> mappedTableResource,
                                      BatchableReadOperation... readOperations) {
        return new ReadBatch<>(mappedTableResource, Arrays.asList(readOperations));
    }

    public MappedTableResource<T> mappedTableResource() {
        return mappedTableResource;
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

}
