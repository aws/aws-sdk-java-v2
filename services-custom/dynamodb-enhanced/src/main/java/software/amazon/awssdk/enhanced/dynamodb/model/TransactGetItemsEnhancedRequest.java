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
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.MappedTableResource;
import software.amazon.awssdk.enhanced.dynamodb.internal.operations.DefaultOperationContext;
import software.amazon.awssdk.enhanced.dynamodb.internal.operations.GetItemOperation;
import software.amazon.awssdk.enhanced.dynamodb.internal.operations.TransactableReadOperation;
import software.amazon.awssdk.services.dynamodb.model.TransactGetItem;
import software.amazon.awssdk.services.dynamodb.model.TransactGetItemsRequest;

/**
 * Defines parameters used for the transaction operation transactGetItems() (such as
 * {@link DynamoDbEnhancedClient#transactGetItems(TransactGetItemsEnhancedRequest)}).
 * <p>
 * A request contains references to the primary keys for the items this operation will search for.
 * It's populated with one or more {@link GetItemEnhancedRequest}, each associated with with the table where the item is located.
 * On initialization, these requests are transformed into {@link TransactGetItem} and stored in the request.
 * .
 */
@SdkPublicApi
@ThreadSafe
public final class TransactGetItemsEnhancedRequest {

    private final List<TransactGetItem> transactGetItems;
    private final AwsRequestOverrideConfiguration overrideConfiguration;

    private TransactGetItemsEnhancedRequest(Builder builder) {
        this.transactGetItems = getItemsFromSupplier(builder.itemSupplierList);
        this.overrideConfiguration = builder.overrideConfiguration;
    }

    /**
     * Creates a newly initialized builder for a request object.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the list of {@link TransactGetItem} that represents all lookup keys in the request.
     */
    public List<TransactGetItem> transactGetItems() {
        return transactGetItems;
    }

    /**
     * Returns the override configuration to apply to the low-level {@link TransactGetItemsRequest}.
     * <p>
     * This can be used to customize the request, such as adding custom headers, MetricPublisher or AwsCredentialsProvider.
     * </p>
     *
     * @return the {@link AwsRequestOverrideConfiguration} to apply to the underlying service call.
     */
    public AwsRequestOverrideConfiguration overrideConfiguration() {
        return overrideConfiguration;
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

        if (overrideConfiguration != null ? !overrideConfiguration.equals(that.overrideConfiguration) :
            that.overrideConfiguration != null) {
            return false;
        }
        return transactGetItems != null ? transactGetItems.equals(that.transactGetItems) : that.transactGetItems == null;
    }

    @Override
    public int hashCode() {
        int result = transactGetItems != null ? transactGetItems.hashCode() : 0;
        result = 31 * result + (overrideConfiguration != null ? overrideConfiguration.hashCode() : 0);
        return result;
    }

    /**
     * A builder that is used to create a transaction object with the desired parameters.
     * <p>
     * A valid builder should contain at least one {@link GetItemEnhancedRequest} added through addGetItem().
     */
    @NotThreadSafe
    public static final class Builder {
        private List<Supplier<TransactGetItem>> itemSupplierList = new ArrayList<>();
        private AwsRequestOverrideConfiguration overrideConfiguration;

        private Builder() {
        }

        /**
         * Adds a primary lookup key and it's associated table to the transaction.
         *
         * @param mappedTableResource the table where the key is located
         * @param request A {@link GetItemEnhancedRequest}
         * @return a builder of this type
         */
        public Builder addGetItem(MappedTableResource<?> mappedTableResource, GetItemEnhancedRequest request) {
            itemSupplierList.add(() -> generateTransactWriteItem(mappedTableResource, GetItemOperation.create(request)));
            return this;
        }

        /**
         * Adds a primary lookup key and it's associated table to the transaction.
         *
         * @param mappedTableResource the table where the key is located
         * @param key the primary key of an item to retrieve as part of the transaction
         * @return a builder of this type
         */
        public Builder addGetItem(MappedTableResource<?> mappedTableResource, Key key) {
            return addGetItem(mappedTableResource, GetItemEnhancedRequest.builder().key(key).build());
        }

        /**
         * Adds a primary lookup key and it's associated table to the transaction.
         *
         * @param mappedTableResource the table where the key is located
         * @param keyItem an item that will have its key fields used to match a record to retrieve from the database
         * @param <T> the type of modelled objects in the table
         * @return a builder of this type
         */
        public <T> Builder addGetItem(MappedTableResource<T> mappedTableResource,
                                      T keyItem) {
            return addGetItem(mappedTableResource, mappedTableResource.keyFrom(keyItem));
        }

        /**
         * Sets the override configuration to apply to the low-level {@link TransactGetItemsRequest}.
         *
         * @see TransactGetItemsRequest.Builder#overrideConfiguration(AwsRequestOverrideConfiguration)
         * @return a builder of this type
         */
        public Builder overrideConfiguration(AwsRequestOverrideConfiguration overrideConfiguration) {
            this.overrideConfiguration = overrideConfiguration;
            return this;
        }

        /**
         * Sets the override configuration to apply to the low-level {@link TransactGetItemsRequest}.
         *
         * @see TransactGetItemsRequest.Builder#overrideConfiguration(Consumer)
         * @return a builder of this type
         */
        public Builder overrideConfiguration(Consumer<AwsRequestOverrideConfiguration.Builder> overrideConfigurationBuilder) {
            AwsRequestOverrideConfiguration.Builder builder = AwsRequestOverrideConfiguration.builder();
            overrideConfigurationBuilder.accept(builder);
            this.overrideConfiguration = builder.build();
            return this;
        }

        /**
         * Builds a {@link TransactGetItemsEnhancedRequest} from the values stored in this builder.
         */
        public TransactGetItemsEnhancedRequest build() {
            return new TransactGetItemsEnhancedRequest(this);
        }

        private <T> TransactGetItem generateTransactWriteItem(MappedTableResource<T> mappedTableResource,
                                                              TransactableReadOperation<T> generator) {
            return generator.generateTransactGetItem(mappedTableResource.tableSchema(),
                                                     DefaultOperationContext.create(mappedTableResource.tableName()),
                                                     mappedTableResource.mapperExtension());
        }
    }
}
