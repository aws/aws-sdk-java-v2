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
import software.amazon.awssdk.extensions.dynamodb.mappingclient.core.DefaultDynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

/**
 * Asynchronous interface for running commands against a DynamoDb database.
 */
@SdkPublicApi
public interface DynamoDbEnhancedAsyncClient {
    /**
     * Executes a command against the database.
     *
     * @param operation The operation to be performed in the context of the database.
     * @param <T> The expected return type from the operation. This is typically inferred by the compiler.
     *
     * @return A {@link CompletableFuture} of the result of the operation being executed. The documentation on the
     * operation itself should have more information.
     */
    <T> CompletableFuture<T> execute(DatabaseOperation<?, ?, T> operation);

    /**
     * Returns a mapped table that can be used to execute commands that work with mapped items against that table.
     *
     * @param tableName The name of the physical table persisted by DynamoDb.
     * @param tableSchema A {@link TableSchema} that maps the table to a modelled object.
     * @return A {@link AsyncMappedTable} object that can be used to execute table operations against.
     * @param <T> THe modelled object type being mapped to this table.
     */
    <T> AsyncMappedTable<T> table(String tableName, TableSchema<T> tableSchema);

    /**
     * Creates a default builder for {@link DynamoDbEnhancedAsyncClient}.
     */
    static DynamoDbEnhancedAsyncClient.Builder builder() {
        return DefaultDynamoDbEnhancedAsyncClient.builder();
    }

    /**
     * The builder definition for a {@link DynamoDbEnhancedAsyncClient}.
     */
    interface Builder {
        Builder dynamoDbClient(DynamoDbAsyncClient dynamoDbAsyncClient);

        Builder extendWith(MapperExtension mapperExtension);

        DynamoDbEnhancedAsyncClient build();
    }
}
