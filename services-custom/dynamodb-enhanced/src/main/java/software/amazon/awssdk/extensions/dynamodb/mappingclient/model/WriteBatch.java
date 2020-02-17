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

package software.amazon.awssdk.extensions.dynamodb.mappingclient.model;

import static software.amazon.awssdk.extensions.dynamodb.mappingclient.core.Utils.getItemsFromSupplier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.BatchableWriteOperation;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.MappedTableResource;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.OperationContext;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.operations.DeleteItemOperation;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.operations.PutItemOperation;
import software.amazon.awssdk.services.dynamodb.model.WriteRequest;

@SdkPublicApi
public final class WriteBatch {
    private final String tableName;
    private final List<WriteRequest> writeRequests;

    private WriteBatch(BuilderImpl<?> builder) {
        this.tableName = builder.mappedTableResource != null ? builder.mappedTableResource.tableName() : null;
        this.writeRequests = getItemsFromSupplier(builder.itemSupplierList);
    }

    public static <T> Builder<T> builder(Class<? extends T> itemClass) {
        return new BuilderImpl<>(itemClass);
    }

    public String tableName() {
        return tableName;
    }

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

    public interface Builder<T> {
        Builder<T> mappedTableResource(MappedTableResource<T> mappedTableResource);

        Builder<T> addDeleteItem(DeleteItemEnhancedRequest request);

        Builder<T> addDeleteItem(Consumer<DeleteItemEnhancedRequest.Builder> requestConsumer);

        Builder<T> addPutItem(PutItemEnhancedRequest<T> request);

        Builder<T> addPutItem(Consumer<PutItemEnhancedRequest.Builder<T>> requestConsumer);

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

        public Builder<T> addPutItem(PutItemEnhancedRequest<T> request) {
            itemSupplierList.add(() -> generateWriteRequest(() -> mappedTableResource, PutItemOperation.create(request)));
            return this;
        }

        public Builder<T> addPutItem(Consumer<PutItemEnhancedRequest.Builder<T>> requestConsumer) {
            PutItemEnhancedRequest.Builder<T> builder = PutItemEnhancedRequest.builder(this.itemClass);
            requestConsumer.accept(builder);
            return addPutItem(builder.build());
        }

        public WriteBatch build() {
            return new WriteBatch(this);
        }

        private WriteRequest generateWriteRequest(Supplier<MappedTableResource<T>> mappedTableResourceSupplier,
                                                  BatchableWriteOperation<T> operation) {
            return operation.generateWriteRequest(mappedTableResourceSupplier.get().tableSchema(),
                                                  OperationContext.create(mappedTableResourceSupplier.get().tableName()),
                                                  mappedTableResourceSupplier.get().mapperExtension());
        }
    }

}
