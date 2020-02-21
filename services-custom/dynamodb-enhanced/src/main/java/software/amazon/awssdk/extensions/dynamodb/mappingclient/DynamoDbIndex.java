/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.extensions.dynamodb.mappingclient;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.model.QueryEnhancedRequest;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.model.ScanEnhancedRequest;

/**
 * Synchronous interface for running commands against an object that is linked to a specific DynamoDb secondary index
 * and knows how to map records from the table that index is linked to into a modelled object.
 *
 * @param <T> The type of the modelled object.
 */
@SdkPublicApi
public interface DynamoDbIndex<T> {

    default SdkIterable<Page<T>> query(QueryEnhancedRequest request) {
        throw new UnsupportedOperationException();
    }

    default SdkIterable<Page<T>> scan(ScanEnhancedRequest request) {
        throw new UnsupportedOperationException();
    }

    /**
     * Gets the {@link MapperExtension} associated with this mapped resource.
     * @return The {@link MapperExtension} associated with this mapped resource.
     */
    MapperExtension mapperExtension();

    /**
     * Gets the {@link TableSchema} object that this mapped table was built with.
     * @return The {@link TableSchema} object for this mapped table.
     */
    TableSchema<T> tableSchema();

    /**
     * Gets the physical table name that operations performed by this object will be executed against.
     * @return The physical table name.
     */
    String tableName();

    /**
     * Gets the physical secondary index name that operations performed by this object will be executed against.
     * @return The physical secondary index name.
     */
    String indexName();

    /**
     * Creates a {@link Key} object from a modelled item. This key can be used in query conditionals and get
     * operations to locate a specific record.
     * @param item The item to extract the key fields from.
     * @return A key that has been initialized with the index values extracted from the modelled object.
     */
    Key keyFrom(T item);
}
