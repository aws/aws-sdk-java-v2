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

package software.amazon.awssdk.enhanced.dynamodb.internal.operations;

import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClientExtension;
import software.amazon.awssdk.enhanced.dynamodb.OperationContext;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/**
 * Interface for a single operation that can be executed against a vector index of a mapped database table.
 */
@SdkInternalApi
public interface VectorIndexOperation<ItemT, RequestT, ResponseT, ResultT>
    extends CommonOperation<ItemT, RequestT, ResponseT, ResultT> {

    /**
     * Executes this operation synchronously against a vector index.
     */
    default ResultT executeOnVectorIndex(TableSchema<ItemT> tableSchema,
                                         String tableName,
                                         String vectorIndexName,
                                         DynamoDbEnhancedClientExtension extension,
                                         DynamoDbClient dynamoDbClient) {
        OperationContext context = DefaultOperationContext.create(tableName, vectorIndexName);
        return execute(tableSchema, context, extension, dynamoDbClient);
    }

    /**
     * Executes this operation asynchronously against a vector index.
     */
    default CompletableFuture<ResultT> executeOnVectorIndexAsync(TableSchema<ItemT> tableSchema,
                                                                 String tableName,
                                                                 String vectorIndexName,
                                                                 DynamoDbEnhancedClientExtension extension,
                                                                 DynamoDbAsyncClient dynamoDbAsyncClient) {
        OperationContext context = DefaultOperationContext.create(tableName, vectorIndexName);
        return executeAsync(tableSchema, context, extension, dynamoDbAsyncClient);
    }
}
