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
 * Asynchronous interface for running commands against an object that is linked to a specific DynamoDb table resource
 * and therefore knows how to map records from that table into a modelled object.
 *
 * @param <T> The type of the modelled object.
 */
@SdkPublicApi
public interface AsyncMappedTable<T> extends MappedTableResource<T> {
    /**
     * Returns a mapped index that can be used to execute commands against a secondary index belonging to the table
     * being mapped by this object. Note that only a subset of the commands that work against a table will work
     * against a secondary index.
     *
     * @param indexName The name of the secondary index to build the command interface for.
     * @return An {@link AsyncMappedIndex} object that can be used to execute database commands against.
     */
    AsyncMappedIndex<T> index(String indexName);

    /**
     * Executes a command that is expected to return a single data item against the database with the context of the
     * primary index of the specific table this object is linked to.
     **
     * @param operationToPerform The operation to be performed in the context of the primary index of the table.
     * @param <R> The expected return type from the operation. This is typically inferred by the compiler.
     *
     * @return A {@link CompletableFuture} that will return the result of the operation being executed. The
     * documentation on the operation itself should have more information.
     */
    <R> CompletableFuture<R> execute(TableOperation<T, ?, ?, R> operationToPerform);

    /**
     * Executes a command that is expected to return a paginated list of data items against the database with the
     * context of the primary index of the specific table this object is linked to.
     **
     * @param operationToPerform The operation to be performed in the context of the primary index of the table.
     * @param <R> The expected return type from the operation. This is typically inferred by the compiler.
     *
     * @return An {@link SdkPublisher} that will publish successive pages of result data items to any subscriber with
     * demand for them.
     */
    <R> SdkPublisher<R> execute(PaginatedTableOperation<T, ?, ?, R> operationToPerform);
}
