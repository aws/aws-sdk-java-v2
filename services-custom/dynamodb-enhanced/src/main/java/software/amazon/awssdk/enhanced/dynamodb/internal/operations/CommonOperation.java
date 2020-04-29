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
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClientExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.OperationContext;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;


/**
 * Common interface for a single operation that can be executed in a synchronous or non-blocking asynchronous fashion
 * against a mapped database table. These operations can be made against either the primary index of a table or a
 * secondary index, although some implementations of this interface do not support secondary indices and will throw
 * an exception when executed against one. Conceptually an operation maps 1:1 with an actual DynamoDb call.
 * <p>
 * This interface is extended by {@link TableOperation} and {@link IndexOperation} which contain implementations of
 * the behavior to actually execute the operation in the context of a table or secondary index and are used by
 * {@link DynamoDbTable} or {@link DynamoDbAsyncTable} and {@link DynamoDbIndex} or {@link DynamoDbAsyncIndex}
 * respectively. By sharing this common interface operations are able to re-use code regardless of whether they are
 * executed in the context of a primary or secondary index or whether they are being executed in a synchronous or
 * non-blocking asynchronous fashion.
 *
 * @param <ItemT> The modelled object that this table maps records to.
 * @param <RequestT>  The type of the request object for the DynamoDb call in the low level {@link DynamoDbClient} or
 *                  {@link DynamoDbAsyncClient}.
 * @param <ResponseT> The type of the response object for the DynamoDb call in the low level {@link DynamoDbClient}
 *                  or {@link DynamoDbAsyncClient}.
 * @param <ResultT> The type of the mapped result object that will be returned by the execution of this operation.
 */
@SdkInternalApi
public interface CommonOperation<ItemT, RequestT, ResponseT, ResultT> {
    /**
     * This method generates the request that needs to be sent to a low level {@link DynamoDbClient}.
     * @param tableSchema A {@link TableSchema} that maps the table to a modelled object.
     * @param context An object containing the context, or target, of the command execution.
     * @param extension A {@link DynamoDbEnhancedClientExtension} that may modify the request of this operation. A null
     *                  value here will result in no modifications.
     * @return A request that can be used as an argument to a {@link DynamoDbClient} call to perform the operation.
     */
    RequestT generateRequest(TableSchema<ItemT> tableSchema, OperationContext context,
                             DynamoDbEnhancedClientExtension extension);

    /**
     * Provides a function for making the low level synchronous SDK call to DynamoDb.
     * @param dynamoDbClient A low level {@link DynamoDbClient} to make the call against.
     * @return A function that calls DynamoDb with a provided request object and returns the response object.
     */
    Function<RequestT, ResponseT> serviceCall(DynamoDbClient dynamoDbClient);

    /**
     * Provides a function for making the low level non-blocking asynchronous SDK call to DynamoDb.
     * @param dynamoDbAsyncClient A low level {@link DynamoDbAsyncClient} to make the call against.
     * @return A function that calls DynamoDb with a provided request object and returns the response object.
     */
    Function<RequestT, CompletableFuture<ResponseT>> asyncServiceCall(DynamoDbAsyncClient dynamoDbAsyncClient);

    /**
     * Takes the response object returned by the actual DynamoDb call and maps it into a higher level abstracted
     * result object.
     * @param response The response object returned by the DynamoDb call for this operation.
     * @param tableSchema A {@link TableSchema} that maps the table to a modelled object.
     * @param context An object containing the context, or target, of the command execution.
     * @param extension A {@link DynamoDbEnhancedClientExtension} that may modify the result of this operation. A null
     *                  value here will result in no modifications.
     * @return A high level result object as specified by the implementation of this operation.
     */
    ResultT transformResponse(ResponseT response,
                              TableSchema<ItemT> tableSchema,
                              OperationContext context,
                              DynamoDbEnhancedClientExtension extension);

    /**
     * Default implementation of a complete synchronous execution of this operation against either the primary or a
     * secondary index.
     * It performs three steps:
     * 1) Call generateRequest() to get the request object.
     * 2) Call getServiceCall() and call it using the request object generated in the previous step.
     * 3) Call transformResponse() to convert the response object returned in the previous step to a high level result.
     *
     * @param tableSchema A {@link TableSchema} that maps the table to a modelled object.
     * @param context An object containing the context, or target, of the command execution.
     * @param dynamoDbClient A {@link DynamoDbClient} to make the call against.
     * @param extension A {@link DynamoDbEnhancedClientExtension} that may modify the request or result of this
     *                  operation. A null value here will result in no modifications.
     * @return A high level result object as specified by the implementation of this operation.
     */
    default ResultT execute(TableSchema<ItemT> tableSchema,
                            OperationContext context,
                            DynamoDbEnhancedClientExtension extension,
                            DynamoDbClient dynamoDbClient) {
        RequestT request = generateRequest(tableSchema, context, extension);
        ResponseT response = serviceCall(dynamoDbClient).apply(request);
        return transformResponse(response, tableSchema, context, extension);
    }

    /**
     * Default implementation of a complete non-blocking asynchronous execution of this operation against either the
     * primary or a secondary index.
     * It performs three steps:
     * 1) Call generateRequest() to get the request object.
     * 2) Call getServiceCall() and call it using the request object generated in the previous step.
     * 3) Wraps the {@link CompletableFuture} returned by the SDK in a new one that calls transformResponse() to
     * convert the response object returned in the previous step to a high level result.
     *
     * @param tableSchema A {@link TableSchema} that maps the table to a modelled object.
     * @param context An object containing the context, or target, of the command execution.
     * @param dynamoDbAsyncClient A {@link DynamoDbAsyncClient} to make the call against.
     * @param extension A {@link DynamoDbEnhancedClientExtension} that may modify the request or result of this
     *                  operation. A null value here will result in no modifications.
     * @return A {@link CompletableFuture} of the high level result object as specified by the implementation of this
     * operation.
     */
    default CompletableFuture<ResultT> executeAsync(TableSchema<ItemT> tableSchema,
                                                    OperationContext context,
                                                    DynamoDbEnhancedClientExtension extension,
                                                    DynamoDbAsyncClient dynamoDbAsyncClient) {
        RequestT request = generateRequest(tableSchema, context, extension);
        CompletableFuture<ResponseT> response = asyncServiceCall(dynamoDbAsyncClient).apply(request);
        return response.thenApply(r -> transformResponse(r, tableSchema, context, extension));
    }
}
