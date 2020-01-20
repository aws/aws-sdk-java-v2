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

package software.amazon.awssdk.extensions.dynamodb.mappingclient;

import java.util.concurrent.CompletableFuture;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.async.SdkPublisher;

/**
 * Asynchronous interface for running commands against an object that is linked to a specific DynamoDb secondary index
 * and knows how to map records from the table that index is linked to into a modelled object.
 *
 * @param <T> The type of the modelled object.
 */
@SdkPublicApi
public interface AsyncMappedIndex<T> {
    /**
     * Executes a command that is expected to return a single data item against the database with the context of the
     * specific table and secondary index this object is linked to.
     *
     * @param operationToPerform The operation to be performed in the context of the secondary index.
     * @param <R> The expected return type from the operation. This is typically inferred by the compiler.
     * @return A {@link CompletableFuture} of the result of the operation being executed. The documentation on the
     * operation itself should have more information.
     */
    <R> CompletableFuture<R> execute(IndexOperation<T, ?, ?, R> operationToPerform);

    /**
     * Executes a command that is expected to return a paginated list of data items against the database with the
     * context of the specific table and secondary index this object is linked to.
     *
     * @param operationToPerform The operation to be performed in the context of the secondary index.
     * @param <R> The expected return type from the operation. This is typically inferred by the compiler.
     * @return An {@link SdkPublisher} that will publish successive pages of result data items to any subscriber with
     * demand for them.
     */
    <R> SdkPublisher<R> execute(PaginatedIndexOperation<T, ?, ?, R> operationToPerform);

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
