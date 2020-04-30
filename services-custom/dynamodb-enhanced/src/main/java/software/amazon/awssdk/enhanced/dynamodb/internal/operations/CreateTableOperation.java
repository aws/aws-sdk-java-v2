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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClientExtension;
import software.amazon.awssdk.enhanced.dynamodb.OperationContext;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.CreateTableEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.CreateTableResponse;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;

@SdkInternalApi
public class CreateTableOperation<T> implements TableOperation<T, CreateTableRequest, CreateTableResponse, Void> {

    private final CreateTableEnhancedRequest request;

    private CreateTableOperation(CreateTableEnhancedRequest request) {
        this.request = request;
    }

    public static <T> CreateTableOperation<T> create(CreateTableEnhancedRequest request) {
        return new CreateTableOperation<>(request);
    }

    @Override
    public CreateTableRequest generateRequest(TableSchema<T> tableSchema,
                                              OperationContext operationContext,
                                              DynamoDbEnhancedClientExtension extension) {
        if (!TableMetadata.primaryIndexName().equals(operationContext.indexName())) {
            throw new IllegalArgumentException("PutItem cannot be executed against a secondary index.");
        }

        String primaryPartitionKey = tableSchema.tableMetadata().primaryPartitionKey();
        Optional<String> primarySortKey = tableSchema.tableMetadata().primarySortKey();
        Set<String> dedupedIndexKeys = new HashSet<>();
        dedupedIndexKeys.add(primaryPartitionKey);
        primarySortKey.ifPresent(dedupedIndexKeys::add);
        List<software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndex> sdkGlobalSecondaryIndices = null;
        List<software.amazon.awssdk.services.dynamodb.model.LocalSecondaryIndex> sdkLocalSecondaryIndices = null;

        if (this.request.globalSecondaryIndices() != null) {
            sdkGlobalSecondaryIndices =
                this.request.globalSecondaryIndices().stream().map(gsi -> {
                    String indexPartitionKey = tableSchema.tableMetadata().indexPartitionKey(gsi.indexName());
                    Optional<String> indexSortKey = tableSchema.tableMetadata().indexSortKey(gsi.indexName());
                    dedupedIndexKeys.add(indexPartitionKey);
                    indexSortKey.ifPresent(dedupedIndexKeys::add);

                    return software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndex
                        .builder()
                        .indexName(gsi.indexName())
                        .keySchema(generateKeySchema(indexPartitionKey, indexSortKey.orElse(null)))
                        .projection(gsi.projection())
                        .provisionedThroughput(gsi.provisionedThroughput())
                        .build();
                }).collect(Collectors.toList());
        }

        if (this.request.localSecondaryIndices() != null) {
            sdkLocalSecondaryIndices =
                this.request.localSecondaryIndices().stream().map(lsi -> {
                    Optional<String> indexSortKey = tableSchema.tableMetadata().indexSortKey(lsi.indexName());
                    indexSortKey.ifPresent(dedupedIndexKeys::add);

                    if (!primaryPartitionKey.equals(
                        tableSchema.tableMetadata().indexPartitionKey(lsi.indexName()))) {
                        throw new IllegalArgumentException("Attempt to create a local secondary index with a partition "
                                                           + "key that is not the primary partition key. Index name: "
                                                           + lsi.indexName());
                    }

                    return software.amazon.awssdk.services.dynamodb.model.LocalSecondaryIndex
                        .builder()
                        .indexName(lsi.indexName())
                        .keySchema(generateKeySchema(primaryPartitionKey, indexSortKey.orElse(null)))
                        .projection(lsi.projection())
                        .build();
                }).collect(Collectors.toList());
        }

        List<AttributeDefinition> attributeDefinitions =
            dedupedIndexKeys.stream()
                            .map(attribute ->
                                     AttributeDefinition.builder()
                                                        .attributeName(attribute)
                                                        .attributeType(tableSchema.tableMetadata()
                                                                                  .scalarAttributeType(attribute)
                                                                                  .orElseThrow(() ->
                                        new IllegalArgumentException("Could not map the key attribute '" + attribute +
                                                                     "' to a valid scalar type.")))
                                                        .build())
                            .collect(Collectors.toList());

        BillingMode billingMode = this.request.provisionedThroughput() == null ?
                                  BillingMode.PAY_PER_REQUEST :
                                  BillingMode.PROVISIONED;

        return CreateTableRequest.builder()
                                 .tableName(operationContext.tableName())
                                 .keySchema(generateKeySchema(primaryPartitionKey, primarySortKey.orElse(null)))
                                 .globalSecondaryIndexes(sdkGlobalSecondaryIndices)
                                 .localSecondaryIndexes(sdkLocalSecondaryIndices)
                                 .attributeDefinitions(attributeDefinitions)
                                 .billingMode(billingMode)
                                 .provisionedThroughput(this.request.provisionedThroughput())
                                 .build();
    }

    @Override
    public Function<CreateTableRequest, CreateTableResponse> serviceCall(DynamoDbClient dynamoDbClient) {
        return dynamoDbClient::createTable;
    }

    @Override
    public Function<CreateTableRequest, CompletableFuture<CreateTableResponse>> asyncServiceCall(
        DynamoDbAsyncClient dynamoDbAsyncClient) {

        return dynamoDbAsyncClient::createTable;
    }

    @Override
    public Void transformResponse(CreateTableResponse response,
                                  TableSchema<T> tableSchema,
                                  OperationContext operationContext,
                                  DynamoDbEnhancedClientExtension extension) {
        // This operation does not return results
        return null;
    }

    private static Collection<KeySchemaElement> generateKeySchema(String partitionKey, String sortKey) {
        if (sortKey == null) {
            return generateKeySchema(partitionKey);
        }

        return Collections.unmodifiableList(Arrays.asList(KeySchemaElement.builder()
                                                                          .attributeName(partitionKey)
                                                                          .keyType(KeyType.HASH)
                                                                          .build(),
                                                          KeySchemaElement.builder()
                                                                          .attributeName(sortKey)
                                                                          .keyType(KeyType.RANGE)
                                                                          .build()));
    }

    private static Collection<KeySchemaElement> generateKeySchema(String partitionKey) {
        return Collections.singletonList(KeySchemaElement.builder()
                                                         .attributeName(partitionKey)
                                                         .keyType(KeyType.HASH)
                                                         .build());
    }

}
