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

package software.amazon.awssdk.extensions.dynamodb.mappingclient.operations;

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

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.MapperExtension;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.OperationContext;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.TableMetadata;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.TableOperation;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.CreateTableResponse;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;

@SdkPublicApi
public class CreateTable<T> implements TableOperation<T, CreateTableRequest, CreateTableResponse, Void> {
    private final ProvisionedThroughput provisionedThroughput;
    private final Collection<LocalSecondaryIndex> localSecondaryIndices;
    private final Collection<GlobalSecondaryIndex> globalSecondaryIndices;

    private CreateTable(ProvisionedThroughput provisionedThroughput,
                        Collection<LocalSecondaryIndex> localSecondaryIndices,
                        Collection<GlobalSecondaryIndex> globalSecondaryIndices) {
        this.provisionedThroughput = provisionedThroughput;
        this.localSecondaryIndices = localSecondaryIndices;
        this.globalSecondaryIndices = globalSecondaryIndices;
    }

    public static <T> CreateTable<T> of(ProvisionedThroughput provisionedThroughput) {
        return new CreateTable<>(provisionedThroughput, null, null);
    }

    public static <T> CreateTable<T> create() {
        return new CreateTable<>(null, null, null);
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder().provisionedThroughput(provisionedThroughput)
                            .localSecondaryIndices(localSecondaryIndices)
                            .globalSecondaryIndices(globalSecondaryIndices);
    }

    @Override
    public CreateTableRequest generateRequest(TableSchema<T> tableSchema,
                                              OperationContext operationContext,
                                              MapperExtension mapperExtension) {
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

        if (globalSecondaryIndices != null) {
            sdkGlobalSecondaryIndices =
                this.globalSecondaryIndices.stream().map(gsi -> {
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

        if (localSecondaryIndices != null) {
            sdkLocalSecondaryIndices =
                this.localSecondaryIndices.stream().map(lsi -> {
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

        BillingMode billingMode = provisionedThroughput == null ? BillingMode.PAY_PER_REQUEST : BillingMode.PROVISIONED;

        return CreateTableRequest.builder()
                                 .tableName(operationContext.tableName())
                                 .keySchema(generateKeySchema(primaryPartitionKey, primarySortKey.orElse(null)))
                                 .globalSecondaryIndexes(sdkGlobalSecondaryIndices)
                                 .localSecondaryIndexes(sdkLocalSecondaryIndices)
                                 .attributeDefinitions(attributeDefinitions)
                                 .billingMode(billingMode)
                                 .provisionedThroughput(provisionedThroughput)
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
                                  MapperExtension mapperExtension) {
        // This operation does not return results
        return null;
    }

    public ProvisionedThroughput provisionedThroughput() {
        return provisionedThroughput;
    }

    public Collection<LocalSecondaryIndex> localSecondaryIndices() {
        return localSecondaryIndices;
    }

    public Collection<GlobalSecondaryIndex> globalSecondaryIndices() {
        return globalSecondaryIndices;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CreateTable<?> that = (CreateTable<?>) o;

        if (provisionedThroughput != null ? ! provisionedThroughput.equals(that.provisionedThroughput) :
            that.provisionedThroughput != null) {
            return false;
        }
        if (localSecondaryIndices != null ? ! localSecondaryIndices.equals(that.localSecondaryIndices) :
            that.localSecondaryIndices != null) {
            return false;
        }
        return globalSecondaryIndices != null ? globalSecondaryIndices.equals(that.globalSecondaryIndices) :
            that.globalSecondaryIndices == null;
    }

    @Override
    public int hashCode() {
        int result = provisionedThroughput != null ? provisionedThroughput.hashCode() : 0;
        result = 31 * result + (localSecondaryIndices != null ? localSecondaryIndices.hashCode() : 0);
        result = 31 * result + (globalSecondaryIndices != null ? globalSecondaryIndices.hashCode() : 0);
        return result;
    }

    public static final class Builder {
        private ProvisionedThroughput provisionedThroughput;
        private Collection<LocalSecondaryIndex> localSecondaryIndices;
        private Collection<GlobalSecondaryIndex> globalSecondaryIndices;

        private Builder() {
        }

        public Builder provisionedThroughput(ProvisionedThroughput provisionedThroughput) {
            this.provisionedThroughput = provisionedThroughput;
            return this;
        }

        public Builder localSecondaryIndices(Collection<LocalSecondaryIndex> localSecondaryIndices) {
            this.localSecondaryIndices = localSecondaryIndices;
            return this;
        }

        public Builder localSecondaryIndices(LocalSecondaryIndex... localSecondaryIndices) {
            this.localSecondaryIndices = Arrays.asList(localSecondaryIndices);
            return this;
        }

        public Builder globalSecondaryIndices(Collection<GlobalSecondaryIndex> globalSecondaryIndices) {
            this.globalSecondaryIndices = globalSecondaryIndices;
            return this;
        }

        public Builder globalSecondaryIndices(GlobalSecondaryIndex... globalSecondaryIndices) {
            this.globalSecondaryIndices = Arrays.asList(globalSecondaryIndices);
            return this;
        }

        public <T> CreateTable<T> build() {
            return new CreateTable<>(provisionedThroughput, localSecondaryIndices, globalSecondaryIndices);
        }
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
