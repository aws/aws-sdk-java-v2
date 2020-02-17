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
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.MappedTableResource;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.OperationContext;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.TransactableWriteOperation;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.operations.DeleteItemOperation;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.operations.PutItemOperation;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.operations.UpdateItemOperation;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;

@SdkPublicApi
public final class TransactWriteItemsEnhancedRequest {

    private final List<TransactWriteItem> transactWriteItems;

    private TransactWriteItemsEnhancedRequest(Builder builder) {
        this.transactWriteItems = getItemsFromSupplier(builder.itemSupplierList);
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<TransactWriteItem> transactWriteItems() {
        return transactWriteItems;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TransactWriteItemsEnhancedRequest that = (TransactWriteItemsEnhancedRequest) o;

        return transactWriteItems != null ? transactWriteItems.equals(that.transactWriteItems) : that.transactWriteItems == null;
    }

    @Override
    public int hashCode() {
        return transactWriteItems != null ? transactWriteItems.hashCode() : 0;
    }

    public static final class Builder {
        private List<Supplier<TransactWriteItem>> itemSupplierList = new ArrayList<>();

        private Builder() {
        }

        public <T> Builder addConditionCheck(MappedTableResource<T> mappedTableResource, ConditionCheck<T> request) {
            itemSupplierList.add(() -> generateTransactWriteItem(mappedTableResource, request));
            return this;
        }

        public <T> Builder addConditionCheck(MappedTableResource<T> mappedTableResource,
                                             Consumer<ConditionCheck.Builder> requestConsumer) {
            ConditionCheck.Builder builder = ConditionCheck.builder();
            requestConsumer.accept(builder);
            return addConditionCheck(mappedTableResource, builder.build());
        }

        public <T> Builder addDeleteItem(MappedTableResource<T> mappedTableResource, DeleteItemEnhancedRequest request) {
            itemSupplierList.add(() -> generateTransactWriteItem(mappedTableResource, DeleteItemOperation.create(request)));
            return this;
        }

        public <T> Builder addDeleteItem(MappedTableResource<T> mappedTableResource,
                                      Consumer<DeleteItemEnhancedRequest.Builder> requestConsumer) {
            DeleteItemEnhancedRequest.Builder builder = DeleteItemEnhancedRequest.builder();
            requestConsumer.accept(builder);
            return addDeleteItem(mappedTableResource, builder.build());
        }

        public <T> Builder addPutItem(MappedTableResource<T> mappedTableResource, PutItemEnhancedRequest<T> request) {
            itemSupplierList.add(() -> generateTransactWriteItem(mappedTableResource, PutItemOperation.create(request)));
            return this;
        }

        public <T> Builder addPutItem(MappedTableResource<T> mappedTableResource, Class<? extends T> itemClass,
                                      Consumer<PutItemEnhancedRequest.Builder<T>> requestConsumer) {
            PutItemEnhancedRequest.Builder<T> builder = PutItemEnhancedRequest.builder(itemClass);
            requestConsumer.accept(builder);
            return addPutItem(mappedTableResource, builder.build());
        }

        public <T> Builder addUpdateItem(MappedTableResource<T> mappedTableResource, UpdateItemEnhancedRequest<T> request) {
            itemSupplierList.add(() -> generateTransactWriteItem(mappedTableResource, UpdateItemOperation.create(request)));
            return this;
        }

        public <T> Builder addUpdateItem(MappedTableResource<T> mappedTableResource, Class<? extends T> itemClass,
                                         Consumer<UpdateItemEnhancedRequest.Builder<T>> requestConsumer) {
            UpdateItemEnhancedRequest.Builder<T> builder = UpdateItemEnhancedRequest.builder(itemClass);
            requestConsumer.accept(builder);
            return addUpdateItem(mappedTableResource, builder.build());
        }

        public TransactWriteItemsEnhancedRequest build() {
            return new TransactWriteItemsEnhancedRequest(this);
        }

        private <T> TransactWriteItem generateTransactWriteItem(MappedTableResource<T> mappedTableResource,
                                                                TransactableWriteOperation<T> generator) {
            return generator.generateTransactWriteItem(mappedTableResource.tableSchema(),
                                                       OperationContext.create(mappedTableResource.tableName()),
                                                       mappedTableResource.mapperExtension());
        }
    }
}
