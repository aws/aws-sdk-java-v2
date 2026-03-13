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


import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
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
import software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.LocalSecondaryIndex;

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
    public OperationName operationName() {
        return OperationName.CREATE_TABLE;
    }

    @Override
    public CreateTableRequest generateRequest(TableSchema<T> tableSchema,
                                              OperationContext operationContext,
                                              DynamoDbEnhancedClientExtension extension) {
        if (!TableMetadata.primaryIndexName().equals(operationContext.indexName())) {
            throw new IllegalArgumentException("CreateTable cannot be executed against a secondary index.");
        }

        List<String> primaryPartitionKeys = tableSchema.tableMetadata().indexPartitionKeys(TableMetadata.primaryIndexName());
        List<String> primarySortKeys = tableSchema.tableMetadata().indexSortKeys(TableMetadata.primaryIndexName());

        validatePrimaryKeys(primaryPartitionKeys, primarySortKeys);

        Set<String> dedupedIndexKeys = new HashSet<>();
        dedupedIndexKeys.addAll(primaryPartitionKeys);
        dedupedIndexKeys.addAll(primarySortKeys);

        List<GlobalSecondaryIndex> sdkGlobalSecondaryIndices = buildGlobalSecondaryIndices(tableSchema, dedupedIndexKeys);
        List<LocalSecondaryIndex> sdkLocalSecondaryIndices = buildLocalSecondaryIndices(tableSchema, dedupedIndexKeys,
                                                                                        primaryPartitionKeys);

        List<AttributeDefinition> attributeDefinitions = buildAttributeDefinitions(dedupedIndexKeys, tableSchema);
        
        BillingMode billingMode = this.request.provisionedThroughput() == null ?
                                  BillingMode.PAY_PER_REQUEST :
                                  BillingMode.PROVISIONED;

        return CreateTableRequest.builder()
                                 .tableName(operationContext.tableName())
                                 .keySchema(generateKeySchema(primaryPartitionKeys, primarySortKeys))
                                 .globalSecondaryIndexes(sdkGlobalSecondaryIndices)
                                 .localSecondaryIndexes(sdkLocalSecondaryIndices)
                                 .attributeDefinitions(attributeDefinitions)
                                 .billingMode(billingMode)
                                 .provisionedThroughput(this.request.provisionedThroughput())
                                 .streamSpecification(this.request.streamSpecification())
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

    private void validatePrimaryKeys(List<String> primaryPartitionKeys, List<String> primarySortKeys) {
        if (primaryPartitionKeys.isEmpty()) {
            throw new IllegalArgumentException("Primary partition key is required for table creation");
        }
        if (primaryPartitionKeys.size() > 1) {
            throw new IllegalArgumentException("Primary table does not support composite partition keys");
        }
        if (primarySortKeys.size() > 1) {
            throw new IllegalArgumentException("Primary table does not support composite sort keys");
        }
    }

    private List<GlobalSecondaryIndex> buildGlobalSecondaryIndices(TableSchema<T> tableSchema, Set<String> dedupedIndexKeys) {
        if (!hasIndices(this.request.globalSecondaryIndices())) {
            return null;
        }

        return this.request.globalSecondaryIndices().stream().map(gsi -> {
            List<String> indexPartitionKeys = tableSchema.tableMetadata().indexPartitionKeys(gsi.indexName());
            List<String> indexSortKeys = tableSchema.tableMetadata().indexSortKeys(gsi.indexName());
            dedupedIndexKeys.addAll(indexPartitionKeys);
            dedupedIndexKeys.addAll(indexSortKeys);

            return GlobalSecondaryIndex.builder()
                .indexName(gsi.indexName())
                .keySchema(generateKeySchema(indexPartitionKeys, indexSortKeys))
                .projection(gsi.projection())
                .provisionedThroughput(gsi.provisionedThroughput())
                .build();
        }).collect(Collectors.toList());
    }

    private List<LocalSecondaryIndex> buildLocalSecondaryIndices(
            TableSchema<T> tableSchema, Set<String> dedupedIndexKeys, List<String> primaryPartitionKeys) {
        if (!hasIndices(this.request.localSecondaryIndices())) {
            return null;
        }

        return this.request.localSecondaryIndices().stream().map(lsi -> {
            List<String> lsiPartitionKeys = tableSchema.tableMetadata().indexPartitionKeys(lsi.indexName());
            List<String> lsiSortKeys = tableSchema.tableMetadata().indexSortKeys(lsi.indexName());

            validateLsiConstraints(primaryPartitionKeys, lsiPartitionKeys, lsiSortKeys, lsi.indexName());

            dedupedIndexKeys.addAll(lsiPartitionKeys);
            dedupedIndexKeys.addAll(lsiSortKeys);

            return LocalSecondaryIndex.builder()
                .indexName(lsi.indexName())
                .keySchema(generateKeySchema(lsiPartitionKeys, lsiSortKeys))
                .projection(lsi.projection())
                .build();
        }).collect(Collectors.toList());
    }

    private void validateLsiConstraints(List<String> primaryPartitionKeys, List<String> lsiPartitionKeys,
                                       List<String> lsiSortKeys, String indexName) {
        if (lsiPartitionKeys.size() != 1) {
            throw new IllegalArgumentException("LSI must have exactly one partition key. Index: " + indexName);
        }

        if (!primaryPartitionKeys.get(0).equals(lsiPartitionKeys.get(0))) {
            throw new IllegalArgumentException("LSI partition key must match primary partition key. Index: " + indexName);
        }

        if (lsiSortKeys.size() > 1) {
            throw new IllegalArgumentException("LSI does not support composite sort keys. Index: " + indexName);
        }
    }

    private List<AttributeDefinition> buildAttributeDefinitions(Set<String> dedupedIndexKeys,
                                                               TableSchema<T> tableSchema) {
        return dedupedIndexKeys.stream()
                              .map(attribute -> AttributeDefinition.builder()
                                           .attributeName(attribute)
                                           .attributeType(tableSchema.tableMetadata()
                                                         .scalarAttributeType(attribute)
                                                         .orElseThrow(() ->
                                                             new IllegalArgumentException(
                                                                 String.format(
                                                                     "Could not map key attribute '%s' to a valid scalar type",
                                                                     attribute))))
                                           .build())
                              .collect(Collectors.toList());
    }

    private boolean hasIndices(Collection<?> indices) {
        return indices != null && !indices.isEmpty();
    }

    private static Collection<KeySchemaElement> generateKeySchema(Collection<String> partitionKeys,
                                                                 Collection<String> sortKeys) {
        List<KeySchemaElement> keySchema = partitionKeys.stream()
                                                        .map(partitionKey -> KeySchemaElement.builder()
                                                                                          .attributeName(partitionKey)
                                                                                          .keyType(KeyType.HASH)
                                                                                          .build())
                                                        .collect(Collectors.toList());

        sortKeys.stream().map(sortKey -> KeySchemaElement.builder()
                                                         .attributeName(sortKey)
                                                         .keyType(KeyType.RANGE)
                                                         .build()).forEach(keySchema::add);

        return Collections.unmodifiableList(keySchema);
    }

}
