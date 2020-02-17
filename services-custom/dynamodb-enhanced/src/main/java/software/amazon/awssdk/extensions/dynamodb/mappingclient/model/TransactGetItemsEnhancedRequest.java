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
import software.amazon.awssdk.extensions.dynamodb.mappingclient.TransactableReadOperation;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.operations.GetItemOperation;
import software.amazon.awssdk.services.dynamodb.model.TransactGetItem;

@SdkPublicApi
public final class TransactGetItemsEnhancedRequest {

    private final List<TransactGetItem> transactGetItems;

    private TransactGetItemsEnhancedRequest(Builder builder) {
        this.transactGetItems = getItemsFromSupplier(builder.itemSupplierList);
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<TransactGetItem> transactGetItems() {
        return transactGetItems;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TransactGetItemsEnhancedRequest that = (TransactGetItemsEnhancedRequest) o;

        return transactGetItems != null ? transactGetItems.equals(that.transactGetItems) : that.transactGetItems == null;
    }

    @Override
    public int hashCode() {
        return transactGetItems != null ? transactGetItems.hashCode() : 0;
    }

    public static final class Builder {
        private List<Supplier<TransactGetItem>> itemSupplierList = new ArrayList<>();

        private Builder() {
        }

        public <T> Builder addGetItem(MappedTableResource<T> mappedTableResource, GetItemEnhancedRequest request) {
            itemSupplierList.add(() -> generateTransactWriteItem(mappedTableResource, GetItemOperation.create(request)));
            return this;
        }

        public <T> Builder addGetItem(MappedTableResource<T> mappedTableResource,
                                      Consumer<GetItemEnhancedRequest.Builder> requestConsumer) {
            GetItemEnhancedRequest.Builder builder = GetItemEnhancedRequest.builder();
            requestConsumer.accept(builder);
            return addGetItem(mappedTableResource, builder.build());
        }

        public TransactGetItemsEnhancedRequest build() {
            return new TransactGetItemsEnhancedRequest(this);
        }

        private <T> TransactGetItem generateTransactWriteItem(MappedTableResource<T> mappedTableResource,
                                                              TransactableReadOperation<T> generator) {
            return generator.generateTransactGetItem(mappedTableResource.tableSchema(),
                                                     OperationContext.create(mappedTableResource.tableName()),
                                                     mappedTableResource.mapperExtension());
        }
    }
}
