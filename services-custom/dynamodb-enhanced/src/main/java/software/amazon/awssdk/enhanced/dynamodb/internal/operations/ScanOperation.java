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

import java.util.Map;
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClientExtension;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.OperationContext;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.internal.EnhancedClientUtils;
import software.amazon.awssdk.enhanced.dynamodb.internal.ProjectionExpressionConvertor;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

@SdkInternalApi
public class ScanOperation<T> implements PaginatedTableOperation<T, ScanRequest, ScanResponse>,
        PaginatedIndexOperation<T, ScanRequest, ScanResponse> {


    private final ScanEnhancedRequest request;

    private ScanOperation(ScanEnhancedRequest request) {
        this.request = request;
    }

    public static <T> ScanOperation<T> create(ScanEnhancedRequest request) {
        return new ScanOperation<>(request);
    }

    @Override
    public ScanRequest generateRequest(TableSchema<T> tableSchema,
                                       OperationContext operationContext,
                                       DynamoDbEnhancedClientExtension extension) {
        Map<String, AttributeValue> expressionValues = null;
        Map<String, String> expressionNames = null;

        if (this.request.filterExpression() != null) {
            expressionValues = this.request.filterExpression().expressionValues();
            expressionNames = this.request.filterExpression().expressionNames();
        }

        ProjectionExpressionConvertor attributeToProject =
                ProjectionExpressionConvertor.create(this.request.nestedAttributesToProject());
        Map<String, String> projectionNameMap = attributeToProject.convertToExpressionMap();
        if (!projectionNameMap.isEmpty()) {
            expressionNames = Expression.joinNames(expressionNames, projectionNameMap);
        }
        String projectionExpression = attributeToProject.convertToProjectionExpression().orElse(null);

        ScanRequest.Builder scanRequest = ScanRequest.builder()
            .tableName(operationContext.tableName())
            .limit(this.request.limit())
            .exclusiveStartKey(this.request.exclusiveStartKey())
            .consistentRead(this.request.consistentRead())
            .expressionAttributeValues(expressionValues)
            .expressionAttributeNames(expressionNames)
            .projectionExpression(projectionExpression);

        if (!TableMetadata.primaryIndexName().equals(operationContext.indexName())) {
            scanRequest = scanRequest.indexName(operationContext.indexName());
        }

        if (this.request.filterExpression() != null) {
            scanRequest = scanRequest.filterExpression(this.request.filterExpression().expression());
        }

        return scanRequest.build();
    }

    @Override
    public Page<T> transformResponse(ScanResponse response,
                                     TableSchema<T> tableSchema,
                                     OperationContext context,
                                     DynamoDbEnhancedClientExtension dynamoDbEnhancedClientExtension) {

        return EnhancedClientUtils.readAndTransformPaginatedItems(response,
                                                                  tableSchema,
                                                                  context,
                                                                  dynamoDbEnhancedClientExtension,
                                                                  ScanResponse::items,
                                                                  ScanResponse::lastEvaluatedKey);
    }

    @Override
    public Function<ScanRequest, SdkIterable<ScanResponse>> serviceCall(DynamoDbClient dynamoDbClient) {
        return dynamoDbClient::scanPaginator;
    }

    @Override
    public Function<ScanRequest, SdkPublisher<ScanResponse>> asyncServiceCall(DynamoDbAsyncClient dynamoDbAsyncClient) {
        return dynamoDbAsyncClient::scanPaginator;
    }

}
