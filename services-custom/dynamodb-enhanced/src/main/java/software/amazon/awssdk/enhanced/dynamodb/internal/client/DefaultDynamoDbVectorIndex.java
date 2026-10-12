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

package software.amazon.awssdk.enhanced.dynamodb.internal.client;

import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClientExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbVectorIndex;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.internal.operations.SearchVectorsOperation;
import software.amazon.awssdk.enhanced.dynamodb.model.SearchVectorsEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.SearchVectorsEnhancedResponse;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@SdkInternalApi
public final class DefaultDynamoDbVectorIndex<T> implements DynamoDbVectorIndex<T> {
    private final DynamoDbClient dynamoDbClient;
    private final DynamoDbEnhancedClientExtension extension;
    private final TableSchema<T> tableSchema;
    private final String tableName;
    private final String indexName;

    DefaultDynamoDbVectorIndex(DynamoDbClient dynamoDbClient,
                               DynamoDbEnhancedClientExtension extension,
                               TableSchema<T> tableSchema,
                               String tableName,
                               String indexName) {
        this.dynamoDbClient = dynamoDbClient;
        this.extension = extension;
        this.tableSchema = tableSchema;
        this.tableName = tableName;
        this.indexName = indexName;
    }

    @Override
    public SearchVectorsEnhancedResponse<T> searchVectorsWithResponse(SearchVectorsEnhancedRequest request) {
        SearchVectorsOperation<T> operation = SearchVectorsOperation.create(request);
        return operation.executeOnVectorIndex(tableSchema, tableName, indexName, extension, dynamoDbClient);
    }

    @Override
    public SearchVectorsEnhancedResponse<T> searchVectorsWithResponse(
        Consumer<SearchVectorsEnhancedRequest.Builder> requestConsumer) {
        SearchVectorsEnhancedRequest.Builder builder = SearchVectorsEnhancedRequest.builder();
        requestConsumer.accept(builder);
        return searchVectorsWithResponse(builder.build());
    }

    @Override
    public DynamoDbEnhancedClientExtension mapperExtension() {
        return extension;
    }

    @Override
    public TableSchema<T> tableSchema() {
        return tableSchema;
    }

    @Override
    public String tableName() {
        return tableName;
    }

    @Override
    public String indexName() {
        return indexName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DefaultDynamoDbVectorIndex<?> that = (DefaultDynamoDbVectorIndex<?>) o;

        if (dynamoDbClient != null ? !dynamoDbClient.equals(that.dynamoDbClient) : that.dynamoDbClient != null) {
            return false;
        }
        if (extension != null ? !extension.equals(that.extension) : that.extension != null) {
            return false;
        }
        if (tableSchema != null ? !tableSchema.equals(that.tableSchema) : that.tableSchema != null) {
            return false;
        }
        if (tableName != null ? !tableName.equals(that.tableName) : that.tableName != null) {
            return false;
        }
        return indexName != null ? indexName.equals(that.indexName) : that.indexName == null;
    }

    @Override
    public int hashCode() {
        int result = dynamoDbClient != null ? dynamoDbClient.hashCode() : 0;
        result = 31 * result + (extension != null ? extension.hashCode() : 0);
        result = 31 * result + (tableSchema != null ? tableSchema.hashCode() : 0);
        result = 31 * result + (tableName != null ? tableName.hashCode() : 0);
        result = 31 * result + (indexName != null ? indexName.hashCode() : 0);
        return result;
    }
}
