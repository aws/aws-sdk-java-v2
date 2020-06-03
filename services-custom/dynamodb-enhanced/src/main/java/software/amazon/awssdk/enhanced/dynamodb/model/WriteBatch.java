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

package software.amazon.awssdk.enhanced.dynamodb.model;

import static software.amazon.awssdk.enhanced.dynamodb.internal.EnhancedClientUtils.getItemsFromSupplier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.MappedTableResource;
import software.amazon.awssdk.enhanced.dynamodb.internal.operations.BatchableWriteOperation;
import software.amazon.awssdk.enhanced.dynamodb.internal.operations.DefaultOperationContext;
import software.amazon.awssdk.enhanced.dynamodb.internal.operations.DeleteItemOperation;
import software.amazon.awssdk.enhanced.dynamodb.internal.operations.PutItemOperation;
import software.amazon.awssdk.services.dynamodb.model.WriteRequest;

/**
 * Defines a collection of references to keys for delete actions and items for put actions
 * for one specific table. A WriteBatch is part of a {@link BatchWriteItemEnhancedRequest}
 * and used in a batchWriteItem() operation (such as
 * {@link DynamoDbEnhancedClient#batchWriteItem(BatchWriteItemEnhancedRequest)}).
 * <p>
 * A valid write batch should contain one or more delete or put action reference.
 */
@SdkPublicApi
public final class WriteBatch {
    private final String tableName;
    private final List<WriteRequest> writeRequests;

    private WriteBatch(BuilderImpl<?> builder) {
        this.tableName = builder.mappedTableResource != null ? builder.mappedTableResource.tableName() : null;
        this.writeRequests = getItemsFromSupplier(builder.itemSupplierList);
    }

    /**
     * Creates a newly initialized builder for a write batch.
     *
     * @param itemClass the class that items in this table map to
     * @param <T> The type of the modelled object, corresponding to itemClass
     * @return a WriteBatch builder
     */
    public static <T> Builder<T> builder(Class<? extends T> itemClass) {
        return new BuilderImpl<>(itemClass);
    }

    /**
     * Returns the table name associated with this batch.
     */
    public String tableName() {
        return tableName;
    }

    /**
     * Returns the collection of write requests in this writek batch.
     */
    public Collection<WriteRequest> writeRequests() {
        return writeRequests;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        WriteBatch that = (WriteBatch) o;

        if (tableName != null ? !tableName.equals(that.tableName)
                              : that.tableName != null) {

            return false;
        }
        return writeRequests != null ? writeRequests.equals(that.writeRequests) : that.writeRequests == null;
    }

    @Override
    public int hashCode() {
        int result = tableName != null ? tableName.hashCode() : 0;
        result = 31 * result + (writeRequests != null ? writeRequests.hashCode() : 0);
        return result;
    }

    /**
     * A builder that is used to create a request with the desired parameters.
     * <p>
     * A valid builder must define a {@link MappedTableResource} and add at least one
     * {@link DeleteItemEnhancedRequest} or {@link PutItemEnhancedRequest}.
     *
     * @param <T> the type that items in this table map to
     */
    public interface Builder<T> {

        /**
         * Sets the mapped table resource (table) that the items in this write batch should come from.
         *
         * @param mappedTableResource the table reference
         * @return a builder of this type
         */
        Builder<T> mappedTableResource(MappedTableResource<T> mappedTableResource);

        /**
         * Adds a {@link DeleteItemEnhancedRequest} to the builder, this request should contain
         * the primary {@link Key} to an item to be deleted.
         *
         * @param request A {@link DeleteItemEnhancedRequest}
         * @return a builder of this type
         */
        Builder<T> addDeleteItem(DeleteItemEnhancedRequest request);

        /**
         * Adds a {@link DeleteItemEnhancedRequest} to the builder, this request should contain
         * the primary {@link Key} to an item to be deleted.
         *
         * @param requestConsumer a {@link Consumer} of {@link DeleteItemEnhancedRequest}
         * @return a builder of this type
         */
        Builder<T> addDeleteItem(Consumer<DeleteItemEnhancedRequest.Builder> requestConsumer);

        /**
         * Adds a DeleteItem request to the builder.
         *
         * @param key a {@link Key} to match the item to be deleted from the database.
         * @return a builder of this type
         */
        Builder<T> addDeleteItem(Key key);

        /**
         * Adds a DeleteItem request to the builder.
         *
         * @param keyItem an item that will have its key fields used to match a record to delete from the database.
         * @return a builder of this type
         */
        Builder<T> addDeleteItem(T keyItem);

        /**
         * Adds a {@link PutItemEnhancedRequest} to the builder, this request should contain the item
         * to be written.
         *
         * @param request A {@link PutItemEnhancedRequest}
         * @return a builder of this type
         */
        Builder<T> addPutItem(PutItemEnhancedRequest<T> request);

        /**
         * Adds a {@link PutItemEnhancedRequest} to the builder, this request should contain the item
         * to be written.
         *
         * @param requestConsumer a {@link Consumer} of {@link PutItemEnhancedRequest}
         * @return a builder of this type
         */
        Builder<T> addPutItem(Consumer<PutItemEnhancedRequest.Builder<T>> requestConsumer);

        /**
         * Adds a PutItem request to the builder.
         *
         * @param item the item to insert or overwrite in the database.
         * @return a builder of this type
         */
        Builder<T> addPutItem(T item);

        WriteBatch build();
    }

    private static final class BuilderImpl<T> implements Builder<T> {

        private Class<? extends T> itemClass;
        private List<Supplier<WriteRequest>> itemSupplierList = new ArrayList<>();
        private MappedTableResource<T> mappedTableResource;

        private BuilderImpl(Class<? extends T> itemClass) {
            this.itemClass = itemClass;
        }

        public Builder<T> mappedTableResource(MappedTableResource<T> mappedTableResource) {
            this.mappedTableResource = mappedTableResource;
            return this;
        }

        public Builder<T> addDeleteItem(DeleteItemEnhancedRequest request) {
            itemSupplierList.add(() -> generateWriteRequest(() -> mappedTableResource, DeleteItemOperation.create(request)));
            return this;
        }

        public Builder<T> addDeleteItem(Consumer<DeleteItemEnhancedRequest.Builder> requestConsumer) {
            DeleteItemEnhancedRequest.Builder builder = DeleteItemEnhancedRequest.builder();
            requestConsumer.accept(builder);
            return addDeleteItem(builder.build());
        }

        @Override
        public Builder<T> addDeleteItem(Key key) {
            return addDeleteItem(r -> r.key(key));
        }

        @Override
        public Builder<T> addDeleteItem(T keyItem) {
            return addDeleteItem(this.mappedTableResource.keyFrom(keyItem));
        }

        public Builder<T> addPutItem(PutItemEnhancedRequest<T> request) {
            itemSupplierList.add(() -> generateWriteRequest(() -> mappedTableResource, PutItemOperation.create(request)));
            return this;
        }

        public Builder<T> addPutItem(Consumer<PutItemEnhancedRequest.Builder<T>> requestConsumer) {
            PutItemEnhancedRequest.Builder<T> builder = PutItemEnhancedRequest.builder(this.itemClass);
            requestConsumer.accept(builder);
            return addPutItem(builder.build());
        }

        @Override
        public Builder<T> addPutItem(T item) {
            return addPutItem(r -> r.item(item));
        }

        public WriteBatch build() {
            return new WriteBatch(this);
        }

        private WriteRequest generateWriteRequest(Supplier<MappedTableResource<T>> mappedTableResourceSupplier,
                                                  BatchableWriteOperation<T> operation) {
            return operation.generateWriteRequest(mappedTableResourceSupplier.get().tableSchema(),
                                                  DefaultOperationContext.create(mappedTableResourceSupplier.get().tableName()),
                                                  mappedTableResourceSupplier.get().mapperExtension());
        }
    }

}
