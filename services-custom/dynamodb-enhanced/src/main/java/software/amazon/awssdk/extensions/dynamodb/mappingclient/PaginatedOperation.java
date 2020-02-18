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

import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.core.TransformIterable;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.core.TransformPublisher;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/**
 * Common interface for an operation that can be executed in a synchronous or non-blocking asynchronous fashion
 * against a mapped database table and is expected to return a paginated list of results. These operations can be made
 * against either the primary index of a table or a secondary index, although some implementations of this interface
 * do not support secondary indices and will throw an exception when executed against one. Typically, each page of
 * results that is served will automatically perform an additional service call to DynamoDb to retrieve the next set
 * of results.
 * <p>
 * This interface is extended by {@link PaginatedTableOperation} and {@link PaginatedIndexOperation} which contain
 * implementations of the behavior to actually execute the operation in the context of a table or secondary index and
 * are used by {@link DynamoDbTable} or {@link DynamoDbAsyncTable} and {@link DynamoDbIndex} or {@link DynamoDbAsyncIndex}
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
@SdkPublicApi
public interface PaginatedOperation<ItemT, RequestT, ResponseT, ResultT> {
    /**
     * This method generates the request that needs to be sent to a low level {@link DynamoDbClient}.
     * @param tableSchema A {@link TableSchema} that maps the table to a modelled object.
     * @param context An object containing the context, or target, of the command execution.
     * @param mapperExtension A {@link MapperExtension} that may modify the request of this operation. A null value
     *                        here will result in no modifications.
     * @return A request that can be used as an argument to a {@link DynamoDbClient} call to perform the operation.
     */
    RequestT generateRequest(TableSchema<ItemT> tableSchema, OperationContext context, MapperExtension mapperExtension);

    /**
     * Provides a function for making the low level synchronous SDK call to DynamoDb.
     * @param dynamoDbClient A low level {@link DynamoDbClient} to make the call against.
     * @return A function that calls a paginated DynamoDb operation with a provided request object and returns the
     * response object.
     */
    Function<RequestT, SdkIterable<ResponseT>> serviceCall(DynamoDbClient dynamoDbClient);

    /**
     * Provides a function for making the low level non-blocking asynchronous SDK call to DynamoDb.
     * @param dynamoDbAsyncClient A low level {@link DynamoDbAsyncClient} to make the call against.
     * @return A function that calls a paginated DynamoDb operation with a provided request object and returns the
     * response object.
     */
    Function<RequestT, SdkPublisher<ResponseT>> asyncServiceCall(DynamoDbAsyncClient dynamoDbAsyncClient);

    /**
     * Takes the response object returned by the actual DynamoDb call and maps it into a higher level abstracted
     * result object.
     * @param response The response object returned by the DynamoDb call for this operation.
     * @param tableSchema A {@link TableSchema} that maps the table to a modelled object.
     * @param context An object containing the context, or target, of the command execution.
     * @param mapperExtension A {@link MapperExtension} that may modify the result of this operation. A null value
     *                        here will result in no modifications.
     * @return A high level result object as specified by the implementation of this operation.
     */
    ResultT transformResponse(ResponseT response,
                              TableSchema<ItemT> tableSchema,
                              OperationContext context,
                              MapperExtension mapperExtension);

    /**
     * Default implementation of a complete synchronous execution of this operation against either the primary or a
     * secondary index.
     * It performs three steps:
     * 1) Call generateRequest() to get the request object.
     * 2) Call getServiceCall() and call it using the request object generated in the previous step.
     * 3) Wraps the {@link SdkIterable} that was returned by the previous step with a transformation that turns each
     * object returned to a high level result.
     *
     * @param tableSchema A {@link TableSchema} that maps the table to a modelled object.
     * @param context An object containing the context, or target, of the command execution.
     * @param dynamoDbClient A {@link DynamoDbClient} to make the call against.
     * @param mapperExtension A {@link MapperExtension} that may modify the request or result of this operation. A
     *                        null value here will result in no modifications.
     * @return A high level result object as specified by the implementation of this operation.
     */
    default SdkIterable<ResultT> execute(TableSchema<ItemT> tableSchema,
                                         OperationContext context,
                                         MapperExtension mapperExtension,
                                         DynamoDbClient dynamoDbClient) {
        RequestT request = generateRequest(tableSchema, context, mapperExtension);
        SdkIterable<ResponseT> response = serviceCall(dynamoDbClient).apply(request);
        return TransformIterable.of(response, r -> transformResponse(r, tableSchema, context, mapperExtension));
    }

    /**
     * Default implementation of a complete non-blocking asynchronous execution of this operation against either the
     * primary or a secondary index.
     * It performs three steps:
     * 1) Call generateRequest() to get the request object.
     * 2) Call getServiceCall() and call it using the request object generated in the previous step.
     * 3) Wraps the {@link SdkPublisher} returned by the SDK in a new one that calls transformResponse() to
     * convert the response objects published to a high level result.
     *
     * @param tableSchema A {@link TableSchema} that maps the table to a modelled object.
     * @param context An object containing the context, or target, of the command execution.
     * @param dynamoDbAsyncClient A {@link DynamoDbAsyncClient} to make the call against.
     * @param mapperExtension A {@link MapperExtension} that may modify the request or result of this operation. A
     *                        null value here will result in no modifications.
     * @return An {@link SdkPublisher} that will publish pages of the high level result object as specified by the
     * implementation of this operation.
     */
    default SdkPublisher<ResultT> executeAsync(TableSchema<ItemT> tableSchema,
                                               OperationContext context,
                                               MapperExtension mapperExtension,
                                               DynamoDbAsyncClient dynamoDbAsyncClient) {
        RequestT request = generateRequest(tableSchema, context, mapperExtension);
        SdkPublisher<ResponseT> response = asyncServiceCall(dynamoDbAsyncClient).apply(request);

        return TransformPublisher.of(response, r -> transformResponse(r, tableSchema, context, mapperExtension));
    }
}
