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

import static java.util.Collections.emptyList;
import static software.amazon.awssdk.enhanced.dynamodb.internal.EnhancedClientUtils.createKeyFromItem;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClientExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.IndexMetadata;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.KeyAttributeMetadata;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.internal.operations.CreateTableOperation;
import software.amazon.awssdk.enhanced.dynamodb.internal.operations.DeleteItemOperation;
import software.amazon.awssdk.enhanced.dynamodb.internal.operations.DeleteTableOperation;
import software.amazon.awssdk.enhanced.dynamodb.internal.operations.DescribeTableOperation;
import software.amazon.awssdk.enhanced.dynamodb.internal.operations.GetItemOperation;
import software.amazon.awssdk.enhanced.dynamodb.internal.operations.PaginatedTableOperation;
import software.amazon.awssdk.enhanced.dynamodb.internal.operations.PutItemOperation;
import software.amazon.awssdk.enhanced.dynamodb.internal.operations.QueryOperation;
import software.amazon.awssdk.enhanced.dynamodb.internal.operations.ScanOperation;
import software.amazon.awssdk.enhanced.dynamodb.internal.operations.TableOperation;
import software.amazon.awssdk.enhanced.dynamodb.internal.operations.UpdateItemOperation;
import software.amazon.awssdk.enhanced.dynamodb.model.CreateTableEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.DeleteItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.DeleteItemEnhancedResponse;
import software.amazon.awssdk.enhanced.dynamodb.model.DescribeTableEnhancedResponse;
import software.amazon.awssdk.enhanced.dynamodb.model.EnhancedGlobalSecondaryIndex;
import software.amazon.awssdk.enhanced.dynamodb.model.EnhancedLocalSecondaryIndex;
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedResponse;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedResponse;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedResponse;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableResponse;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;

@SdkInternalApi
public class DefaultDynamoDbTable<T> implements DynamoDbTable<T> {
    private final DynamoDbClient dynamoDbClient;
    private final DynamoDbEnhancedClientExtension extension;
    private final TableSchema<T> tableSchema;
    private final String tableName;

    DefaultDynamoDbTable(DynamoDbClient dynamoDbClient,
                         DynamoDbEnhancedClientExtension extension,
                         TableSchema<T> tableSchema,
                         String tableName) {
        this.dynamoDbClient = dynamoDbClient;
        this.extension = extension;
        this.tableSchema = tableSchema;
        this.tableName = tableName;
    }

    @Override
    public DynamoDbEnhancedClientExtension mapperExtension() {
        return this.extension;
    }

    @Override
    public TableSchema<T> tableSchema() {
        return this.tableSchema;
    }

    public DynamoDbClient dynamoDbClient() {
        return dynamoDbClient;
    }

    @Override
    public String tableName() {
        return tableName;
    }

    @Override
    public DefaultDynamoDbIndex<T> index(String indexName) {
        // Force a check for the existence of the index
        tableSchema.tableMetadata().indexPartitionKey(indexName);

        return new DefaultDynamoDbIndex<>(dynamoDbClient,
                                          extension,
                                          tableSchema,
                                          tableName,
                                          indexName);
    }

    @Override
    public void createTable(CreateTableEnhancedRequest request) {
        TableOperation<T, ?, ?, Void> operation = CreateTableOperation.create(request);
        operation.executeOnPrimaryIndex(tableSchema, tableName, extension, dynamoDbClient);
    }

    @Override
    public void createTable(Consumer<CreateTableEnhancedRequest.Builder> requestConsumer) {
        CreateTableEnhancedRequest.Builder builder = CreateTableEnhancedRequest.builder();
        requestConsumer.accept(builder);
        createTable(builder.build());
    }

    @Override
    public void createTable() {
        Map<IndexType, List<IndexMetadata>> indexGroups = splitSecondaryIndicesToLocalAndGlobalOnes();
        createTable(CreateTableEnhancedRequest.builder()
                                              .localSecondaryIndices(extractLocalSecondaryIndices(indexGroups))
                                              .globalSecondaryIndices(extractGlobalSecondaryIndices(indexGroups))
                                              .build());
    }

    private Map<IndexType, List<IndexMetadata>> splitSecondaryIndicesToLocalAndGlobalOnes() {
        String primaryPartitionKeyName = tableSchema.tableMetadata().primaryPartitionKey();
        Collection<IndexMetadata> indices = tableSchema.tableMetadata().indices();
        return indices.stream()
                      .filter(index -> !TableMetadata.primaryIndexName().equals(index.name()))
                      .collect(Collectors.groupingBy(metadata -> {
                          String partitionKeyName = metadata.partitionKey().map(KeyAttributeMetadata::name).orElse(null);
                          if (partitionKeyName == null || primaryPartitionKeyName.equals(partitionKeyName)) {
                              return IndexType.LSI;
                          }
                          return IndexType.GSI;
                      }));
    }

    private List<EnhancedLocalSecondaryIndex> extractLocalSecondaryIndices(Map<IndexType, List<IndexMetadata>> indicesGroups) {
        return indicesGroups.getOrDefault(IndexType.LSI, emptyList()).stream()
                            .map(this::mapIndexMetadataToEnhancedLocalSecondaryIndex)
                            .collect(Collectors.toList());
    }

    private EnhancedLocalSecondaryIndex mapIndexMetadataToEnhancedLocalSecondaryIndex(IndexMetadata indexMetadata) {
        return EnhancedLocalSecondaryIndex.builder()
                                          .indexName(indexMetadata.name())
                                          .projection(pb -> pb.projectionType(ProjectionType.ALL))
                                          .build();
    }

    private List<EnhancedGlobalSecondaryIndex> extractGlobalSecondaryIndices(Map<IndexType, List<IndexMetadata>> indicesGroups) {
        return indicesGroups.getOrDefault(IndexType.GSI, emptyList()).stream()
                            .map(this::mapIndexMetadataToEnhancedGlobalSecondaryIndex)
                            .collect(Collectors.toList());
    }

    private EnhancedGlobalSecondaryIndex mapIndexMetadataToEnhancedGlobalSecondaryIndex(IndexMetadata indexMetadata) {
        return EnhancedGlobalSecondaryIndex.builder()
                                           .indexName(indexMetadata.name())
                                           .projection(pb -> pb.projectionType(ProjectionType.ALL))
                                           .build();
    }

    @Override
    public T deleteItem(DeleteItemEnhancedRequest request) {
        TableOperation<T, ?, ?, DeleteItemEnhancedResponse<T>> operation = DeleteItemOperation.create(request);
        return operation.executeOnPrimaryIndex(tableSchema, tableName, extension, dynamoDbClient).attributes();
    }

    @Override
    public T deleteItem(Consumer<DeleteItemEnhancedRequest.Builder> requestConsumer) {
        DeleteItemEnhancedRequest.Builder builder = DeleteItemEnhancedRequest.builder();
        requestConsumer.accept(builder);
        return deleteItem(builder.build());
    }

    @Override
    public T deleteItem(Key key) {
        return deleteItem(r -> r.key(key));
    }

    @Override
    public T deleteItem(T keyItem) {
        return deleteItem(keyFrom(keyItem));
    }

    @Override
    public DeleteItemEnhancedResponse<T> deleteItemWithResponse(DeleteItemEnhancedRequest request) {
        TableOperation<T, ?, ?, DeleteItemEnhancedResponse<T>> operation = DeleteItemOperation.create(request);
        return operation.executeOnPrimaryIndex(tableSchema, tableName, extension, dynamoDbClient);
    }

    @Override
    public DeleteItemEnhancedResponse<T> deleteItemWithResponse(Consumer<DeleteItemEnhancedRequest.Builder> requestConsumer) {
        DeleteItemEnhancedRequest.Builder builder = DeleteItemEnhancedRequest.builder();
        requestConsumer.accept(builder);
        return deleteItemWithResponse(builder.build());
    }

    @Override
    public T getItem(GetItemEnhancedRequest request) {
        TableOperation<T, ?, ?, GetItemEnhancedResponse<T>> operation = GetItemOperation.create(request);
        return operation.executeOnPrimaryIndex(tableSchema, tableName, extension, dynamoDbClient).attributes();
    }

    @Override
    public T getItem(Consumer<GetItemEnhancedRequest.Builder> requestConsumer) {
        GetItemEnhancedRequest.Builder builder = GetItemEnhancedRequest.builder();
        requestConsumer.accept(builder);
        return getItem(builder.build());
    }

    @Override
    public T getItem(Key key) {
        return getItem(r -> r.key(key));
    }

    @Override
    public T getItem(T keyItem) {
        return getItem(keyFrom(keyItem));
    }

    @Override
    public GetItemEnhancedResponse<T> getItemWithResponse(Consumer<GetItemEnhancedRequest.Builder> requestConsumer) {
        GetItemEnhancedRequest.Builder builder = GetItemEnhancedRequest.builder();
        requestConsumer.accept(builder);
        return getItemWithResponse(builder.build());
    }

    @Override
    public GetItemEnhancedResponse<T> getItemWithResponse(GetItemEnhancedRequest request) {
        TableOperation<T, ?, ?, GetItemEnhancedResponse<T>> operation = GetItemOperation.create(request);
        return operation.executeOnPrimaryIndex(tableSchema, tableName, extension, dynamoDbClient);
    }

    @Override
    public PageIterable<T> query(QueryEnhancedRequest request) {
        PaginatedTableOperation<T, ?, ?> operation = QueryOperation.create(request);
        return operation.executeOnPrimaryIndex(tableSchema, tableName, extension, dynamoDbClient);
    }

    @Override
    public PageIterable<T> query(Consumer<QueryEnhancedRequest.Builder> requestConsumer) {
        QueryEnhancedRequest.Builder builder = QueryEnhancedRequest.builder();
        requestConsumer.accept(builder);
        return query(builder.build());
    }

    @Override
    public PageIterable<T> query(QueryConditional queryConditional) {
        return query(r -> r.queryConditional(queryConditional));
    }

    @Override
    public void putItem(PutItemEnhancedRequest<T> request) {
        TableOperation<T, ?, ?, PutItemEnhancedResponse<T>> operation = PutItemOperation.create(request);
        operation.executeOnPrimaryIndex(tableSchema, tableName, extension, dynamoDbClient);
    }

    @Override
    public void putItem(Consumer<PutItemEnhancedRequest.Builder<T>> requestConsumer) {
        PutItemEnhancedRequest.Builder<T> builder =
            PutItemEnhancedRequest.builder(this.tableSchema.itemType().rawClass());
        requestConsumer.accept(builder);
        putItem(builder.build());
    }

    @Override
    public void putItem(T item) {
        putItem(r -> r.item(item));
    }

    @Override
    public PutItemEnhancedResponse<T> putItemWithResponse(PutItemEnhancedRequest<T> request) {
        TableOperation<T, ?, ?, PutItemEnhancedResponse<T>> operation = PutItemOperation.create(request);
        return operation.executeOnPrimaryIndex(tableSchema, tableName, extension, dynamoDbClient);
    }

    @Override
    public PutItemEnhancedResponse<T> putItemWithResponse(Consumer<PutItemEnhancedRequest.Builder<T>> requestConsumer) {
        PutItemEnhancedRequest.Builder<T> builder =
            PutItemEnhancedRequest.builder(this.tableSchema.itemType().rawClass());
        requestConsumer.accept(builder);
        return putItemWithResponse(builder.build());
    }

    @Override
    public PageIterable<T> scan(ScanEnhancedRequest request) {
        PaginatedTableOperation<T, ?, ?> operation = ScanOperation.create(request);
        return operation.executeOnPrimaryIndex(tableSchema, tableName, extension, dynamoDbClient);
    }

    @Override
    public PageIterable<T> scan(Consumer<ScanEnhancedRequest.Builder> requestConsumer) {
        ScanEnhancedRequest.Builder builder = ScanEnhancedRequest.builder();
        requestConsumer.accept(builder);
        return scan(builder.build());
    }

    @Override
    public PageIterable<T> scan() {
        return scan(ScanEnhancedRequest.builder().build());
    }

    @Override
    public T updateItem(UpdateItemEnhancedRequest<T> request) {
        TableOperation<T, ?, ?, UpdateItemEnhancedResponse<T>> operation = UpdateItemOperation.create(request);
        return operation.executeOnPrimaryIndex(tableSchema, tableName, extension, dynamoDbClient).attributes();
    }

    @Override
    public T updateItem(Consumer<UpdateItemEnhancedRequest.Builder<T>> requestConsumer) {
        UpdateItemEnhancedRequest.Builder<T> builder =
            UpdateItemEnhancedRequest.builder(this.tableSchema.itemType().rawClass());
        requestConsumer.accept(builder);
        return updateItem(builder.build());
    }

    @Override
    public T updateItem(T item) {
        return updateItem(r -> r.item(item));
    }

    @Override
    public UpdateItemEnhancedResponse<T> updateItemWithResponse(UpdateItemEnhancedRequest<T> request) {
        TableOperation<T, ?, ?, UpdateItemEnhancedResponse<T>> operation = UpdateItemOperation.create(request);
        return operation.executeOnPrimaryIndex(tableSchema, tableName, extension, dynamoDbClient);
    }

    @Override
    public UpdateItemEnhancedResponse<T> updateItemWithResponse(Consumer<UpdateItemEnhancedRequest.Builder<T>> requestConsumer) {
        UpdateItemEnhancedRequest.Builder<T> builder =
            UpdateItemEnhancedRequest.builder(this.tableSchema.itemType().rawClass());
        requestConsumer.accept(builder);
        return updateItemWithResponse(builder.build());
    }

    @Override
    public Key keyFrom(T item) {
        return createKeyFromItem(item, tableSchema, TableMetadata.primaryIndexName());
    }

    @Override
    public void deleteTable() {
        TableOperation<T, ?, ?, Void> operation = DeleteTableOperation.create();
        operation.executeOnPrimaryIndex(tableSchema, tableName, extension, dynamoDbClient);
    }

    @Override
    public DescribeTableEnhancedResponse describeTable() {
        TableOperation<T, DescribeTableRequest, DescribeTableResponse, DescribeTableEnhancedResponse> operation =
            DescribeTableOperation.create();
        return operation.executeOnPrimaryIndex(tableSchema, tableName, extension, dynamoDbClient);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DefaultDynamoDbTable<?> that = (DefaultDynamoDbTable<?>) o;

        if (dynamoDbClient != null ? ! dynamoDbClient.equals(that.dynamoDbClient) : that.dynamoDbClient != null) {
            return false;
        }
        if (extension != null ?
            !extension.equals(that.extension) :
            that.extension != null) {

            return false;
        }
        if (tableSchema != null ? ! tableSchema.equals(that.tableSchema) : that.tableSchema != null) {
            return false;
        }
        return tableName != null ? tableName.equals(that.tableName) : that.tableName == null;
    }

    @Override
    public int hashCode() {
        int result = dynamoDbClient != null ? dynamoDbClient.hashCode() : 0;
        result = 31 * result + (extension != null ?
            extension.hashCode() : 0);
        result = 31 * result + (tableSchema != null ? tableSchema.hashCode() : 0);
        result = 31 * result + (tableName != null ? tableName.hashCode() : 0);
        return result;
    }
}
