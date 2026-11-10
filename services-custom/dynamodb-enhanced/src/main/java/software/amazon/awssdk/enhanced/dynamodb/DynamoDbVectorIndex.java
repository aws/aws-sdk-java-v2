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

package software.amazon.awssdk.enhanced.dynamodb;

import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.model.SearchVectorsEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.SearchVectorsEnhancedResponse;

/**
 * Synchronous interface for running vector search commands against a specific DynamoDb vector index linked to a mapped table.
 * <p>
 * By default, all command methods throw an {@link UnsupportedOperationException} to prevent interface extensions from breaking
 * implementing classes.
 *
 * @param <T> The type of the modelled object.
 */
@SdkPublicApi
@ThreadSafe
public interface DynamoDbVectorIndex<T> {

    /**
     * Executes a vector similarity search against this vector index and returns the full enhanced response.
     *
     * @param request A {@link SearchVectorsEnhancedRequest} defining the search parameters.
     * @return A {@link SearchVectorsEnhancedResponse} containing results and vector capacity metadata.
     */
    default SearchVectorsEnhancedResponse<T> searchVectorsWithResponse(SearchVectorsEnhancedRequest request) {
        throw new UnsupportedOperationException();
    }

    /**
     * Executes a vector similarity search against this vector index and returns the full enhanced response.
     *
     * @param requestConsumer A {@link Consumer} of {@link SearchVectorsEnhancedRequest.Builder}.
     * @return A {@link SearchVectorsEnhancedResponse} containing results and vector capacity metadata.
     */
    default SearchVectorsEnhancedResponse<T> searchVectorsWithResponse(
        Consumer<SearchVectorsEnhancedRequest.Builder> requestConsumer) {
        throw new UnsupportedOperationException();
    }

    /**
     * Gets the {@link DynamoDbEnhancedClientExtension} associated with this mapped resource.
     *
     * @return The {@link DynamoDbEnhancedClientExtension} associated with this mapped resource.
     */
    DynamoDbEnhancedClientExtension mapperExtension();

    /**
     * Gets the {@link TableSchema} object that this mapped vector index was built with.
     *
     * @return The {@link TableSchema} object for this mapped vector index.
     */
    TableSchema<T> tableSchema();

    /**
     * Gets the physical table name that operations performed by this object will be executed against.
     *
     * @return The physical table name.
     */
    String tableName();

    /**
     * Gets the physical vector index name that operations performed by this object will be executed against.
     *
     * @return The physical vector index name.
     */
    String indexName();
}
