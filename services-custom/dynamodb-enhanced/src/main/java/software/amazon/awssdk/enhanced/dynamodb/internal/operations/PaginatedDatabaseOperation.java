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
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClientExtension;
import software.amazon.awssdk.enhanced.dynamodb.internal.TransformIterable;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/**
 * Interface for an operation that can be executed against a mapped database and is expected to return a paginated
 * list of results. These operations do not operate on a specific table or index, and may reference multiple tables
 * and indexes (eg: batch operations). Typically, each page of results that is served will automatically perform an
 * additional service call to DynamoDb to retrieve the next set of results.
 *
 * @param <RequestT>  The type of the request object for the DynamoDb call in the low level {@link DynamoDbClient}.
 * @param <ResponseT> The type of the response object for the DynamoDb call in the low level {@link DynamoDbClient}.
 * @param <ResultT> The type of the mapped result object that will be returned by the execution of this operation.
 */
@SdkInternalApi
public interface PaginatedDatabaseOperation<RequestT, ResponseT, ResultT> {
    /**
     * This method generates the request that needs to be sent to a low level {@link DynamoDbClient}.
     * @param extension A {@link DynamoDbEnhancedClientExtension} that may modify the request of this operation. A null value
     *                        here will result in no modifications.
     * @return A request that can be used as an argument to a {@link DynamoDbClient} call to perform the operation.
     */
    RequestT generateRequest(DynamoDbEnhancedClientExtension extension);

    /**
     * Provides a function for making the low level synchronous paginated SDK call to DynamoDb.
     * @param dynamoDbClient A low level {@link DynamoDbClient} to make the call against.
     * @return A function that calls DynamoDb with a provided request object and returns the response object.
     */
    Function<RequestT, SdkIterable<ResponseT>> serviceCall(DynamoDbClient dynamoDbClient);

    /**
     * Provides a function for making the low level non-blocking asynchronous paginated SDK call to DynamoDb.
     * @param dynamoDbAsyncClient A low level {@link DynamoDbAsyncClient} to make the call against.
     * @return A function that calls DynamoDb with a provided request object and returns the response object.
     */
    Function<RequestT, SdkPublisher<ResponseT>> asyncServiceCall(DynamoDbAsyncClient dynamoDbAsyncClient);

    /**
     * Takes the response object returned by the actual DynamoDb call and maps it into a higher level abstracted
     * result object.
     * @param response The response object returned by the DynamoDb call for this operation.
     * @param extension A {@link DynamoDbEnhancedClientExtension} that may modify the result of this operation. A null value
     *                        here will result in no modifications.
     * @return A high level result object as specified by the implementation of this operation.
     */
    ResultT transformResponse(ResponseT response, DynamoDbEnhancedClientExtension extension);

    /**
     * Default implementation of a complete synchronous execution of this operation against a database.
     * It performs three steps:
     * 1) Call generateRequest() to get the request object.
     * 2) Call getServiceCall() and call it using the request object generated in the previous step.
     * 3) Wraps the {@link SdkIterable} that was returned by the previous step with a transformation that turns each
     * object returned to a high level result.
     *
     * @param dynamoDbClient A {@link DynamoDbClient} to make the call against.
     * @param extension A {@link DynamoDbEnhancedClientExtension} that may modify the request or result of this operation. A
     *                        null value here will result in no modifications.
     * @return An {@link SdkIterable} that will iteratively return pages of high level result objects as specified by
     * the implementation of this operation.
     */
    default SdkIterable<ResultT> execute(DynamoDbClient dynamoDbClient, DynamoDbEnhancedClientExtension extension) {
        RequestT request = generateRequest(extension);
        SdkIterable<ResponseT> response = serviceCall(dynamoDbClient).apply(request);
        return TransformIterable.of(response, r -> transformResponse(r, extension));
    }

    /**
     * Default implementation of a complete non-blocking asynchronous execution of this operation against a database.
     * It performs three steps:
     * 1) Call generateRequest() to get the request object.
     * 2) Call getServiceCall() and call it using the request object generated in the previous step.
     * 3) Wraps the {@link CompletableFuture} returned by the SDK in a new one that calls transformResponse() to
     * convert the response object returned in the previous step to a high level result.
     *
     * @param dynamoDbAsyncClient A {@link DynamoDbAsyncClient} to make the call against.
     * @param extension A {@link DynamoDbEnhancedClientExtension} that may modify the request or result of this operation. A
     *                        null value here will result in no modifications.
     * @return An {@link SdkPublisher} that will publish pages of the high level result object as specified by the
     * implementation of this operation.
     */
    default SdkPublisher<ResultT> executeAsync(DynamoDbAsyncClient dynamoDbAsyncClient,
                                                    DynamoDbEnhancedClientExtension extension) {

        RequestT request = generateRequest(extension);
        SdkPublisher<ResponseT> response = asyncServiceCall(dynamoDbAsyncClient).apply(request);
        return response.map(r -> transformResponse(r, extension));
    }
}
