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
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.MappedTableResource;
import software.amazon.awssdk.enhanced.dynamodb.internal.operations.DefaultOperationContext;
import software.amazon.awssdk.enhanced.dynamodb.internal.operations.DeleteItemOperation;
import software.amazon.awssdk.enhanced.dynamodb.internal.operations.PutItemOperation;
import software.amazon.awssdk.enhanced.dynamodb.internal.operations.TransactableWriteOperation;
import software.amazon.awssdk.enhanced.dynamodb.internal.operations.UpdateItemOperation;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;

/**
 * Defines parameters used for the transaction operation transactWriteItems() (such as
 * {@link DynamoDbEnhancedClient#transactWriteItems(TransactWriteItemsEnhancedRequest)}).
 * <p>
 * A request contains parameters for the different actions available in the operation:
 * <ul>
 *     <li>Write/Update items through put and update actions</li>
 *     <li>Delete items</li>
 *     <li>Use a condition check</li>
 * </ul>
 * It's populated with one or more low-level requests, such as {@link PutItemEnhancedRequest} and each low-level action request
 * is associated with with the table where the action should be applied.
 * On initialization, these requests are transformed into {@link TransactWriteItem} and stored in the request.
 */
@SdkPublicApi
public final class TransactWriteItemsEnhancedRequest {

    private final List<TransactWriteItem> transactWriteItems;

    private final String clientRequestToken;

    private TransactWriteItemsEnhancedRequest(Builder builder) {
        this.transactWriteItems = getItemsFromSupplier(builder.itemSupplierList);
        this.clientRequestToken = builder.clientRequestToken;
    }

    /**
     * Creates a newly initialized builder for a request object.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * <p>
     * Providing a <code>ClientRequestToken</code> makes the call to <code>TransactWriteItems</code> idempotent, meaning
     * that multiple identical calls have the same effect as one single call.
     * </p>
     * <p>
     * A client request token is valid for 10 minutes after the first request that uses it is completed. After 10
     * minutes, any request with the same client token is treated as a new request. Do not resubmit the same request
     * with the same client token for more than 10 minutes, or the result might not be idempotent.
     * </p>
     * <p>
     * If you submit a request with the same client token but a change in other parameters within the 10-minute
     * idempotency window, DynamoDB returns an <code>IdempotentParameterMismatch</code> exception.
     * </p>
     */

    public String clientRequestToken() {
        return clientRequestToken;
    }

    /**
     * Returns the list of {@link TransactWriteItem} that represents all actions in the request.
     */
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

    /**
     * A builder that is used to create a transaction object with the desired parameters.
     * <p>
     * A valid builder should contain at least one low-level request such as {@link DeleteItemEnhancedRequest}.
     */
    public static final class Builder {
        private List<Supplier<TransactWriteItem>> itemSupplierList = new ArrayList<>();

        private String clientRequestToken;

        private Builder() {
        }

        /**
         * Adds a condition check for a primary key in the associated table to the transaction.
         * <p>
         * <b>Note:</b> The condition check should be applied to an item that is not modified by another action in the
         * same transaction. See {@link ConditionCheck} for more information on how to build a condition check, and the
         * DynamoDb TransactWriteItems documentation for more information on how a condition check affects the transaction.
         *
         * @param mappedTableResource the table on which to apply the condition check
         * @param request A {@link ConditionCheck} definition
         * @param <T> the type of modelled objects in the table
         * @return a builder of this type
         */
        public <T> Builder addConditionCheck(MappedTableResource<T> mappedTableResource, ConditionCheck<T> request) {
            itemSupplierList.add(() -> generateTransactWriteItem(mappedTableResource, request));
            return this;
        }

        /**
         * Adds a condition check for a primary key in the associated table to the transaction by accepting a consumer
         * of {@link ConditionCheck.Builder}.
         * <p>
         * <b>Note:</b> The condition check should be applied to an item that is not modified by another action in the
         * same transaction. See {@link ConditionCheck} for more information on how to build a condition check, and the
         * DynamoDb TransactWriteItems documentation for more information on how a condition check affects the transaction.
         *
         * @param mappedTableResource the table on which to apply the condition check
         * @param requestConsumer a {@link Consumer} of {@link DeleteItemEnhancedRequest}
         * @param <T> the type of modelled objects in the table
         * @return a builder of this type
         */
        public <T> Builder addConditionCheck(MappedTableResource<T> mappedTableResource,
                                             Consumer<ConditionCheck.Builder> requestConsumer) {
            ConditionCheck.Builder builder = ConditionCheck.builder();
            requestConsumer.accept(builder);
            return addConditionCheck(mappedTableResource, builder.build());
        }

        /**
         * Adds a primary lookup key for the item to delete, and it's associated table, to the transaction. For more information
         * on the delete action, see the low-level operation description in for instance
         * {@link DynamoDbTable#deleteItem(DeleteItemEnhancedRequest)} and how to construct the low-level request in
         * {@link DeleteItemEnhancedRequest}.
         *
         * @param mappedTableResource the table where the key is located
         * @param request A {@link DeleteItemEnhancedRequest}
         * @param <T> the type of modelled objects in the table
         * @return a builder of this type
         */
        public <T> Builder addDeleteItem(MappedTableResource<T> mappedTableResource, DeleteItemEnhancedRequest request) {
            itemSupplierList.add(() -> generateTransactWriteItem(mappedTableResource, DeleteItemOperation.create(request)));
            return this;
        }

        /**
         * Adds a primary lookup key for the item to delete, and it's associated table, to the transaction. For more
         * information on the delete action, see the low-level operation description in for instance
         * {@link DynamoDbTable#deleteItem(DeleteItemEnhancedRequest)}.
         *
         * @param mappedTableResource the table where the key is located
         * @param key a {@link Key} that identifies the record to be deleted as part of the transaction.
         * @param <T> the type of modelled objects in the table
         * @return a builder of this type
         */
        public <T> Builder addDeleteItem(MappedTableResource<T> mappedTableResource, Key key) {
            return addDeleteItem(mappedTableResource, DeleteItemEnhancedRequest.builder().key(key).build());
        }

        /**
         * Adds a primary lookup key for the item to delete, and it's associated table, to the transaction. For more
         * information on the delete action, see the low-level operation description in for instance
         * {@link DynamoDbTable#deleteItem(DeleteItemEnhancedRequest)}.
         *
         * @param mappedTableResource the table where the key is located
         * @param keyItem an item that will have its key fields used to match a record to retrieve from the database
         * @param <T> the type of modelled objects in the table
         * @return a builder of this type
         */
        public <T> Builder addDeleteItem(MappedTableResource<T> mappedTableResource, T keyItem) {
            return addDeleteItem(mappedTableResource, mappedTableResource.keyFrom(keyItem));
        }

        /**
         * Adds an item to be written, and it's associated table, to the transaction. For more information on the put action,
         * see the low-level operation description in for instance {@link DynamoDbTable#putItem(PutItemEnhancedRequest)}
         * and how to construct the low-level request in {@link PutItemEnhancedRequest}.
         *
         * @param mappedTableResource the table to write the item to
         * @param request A {@link PutItemEnhancedRequest}
         * @param <T> the type of modelled objects in the table
         * @return a builder of this type
         */
        public <T> Builder addPutItem(MappedTableResource<T> mappedTableResource, PutItemEnhancedRequest<T> request) {
            itemSupplierList.add(() -> generateTransactWriteItem(mappedTableResource, PutItemOperation.create(request)));
            return this;
        }

        /**
         * Adds an item to be written, and it's associated table, to the transaction. For more information on the put
         * action, see the low-level operation description in for instance
         * {@link DynamoDbTable#putItem(PutItemEnhancedRequest)}.
         *
         * @param mappedTableResource the table to write the item to
         * @param item the item to be inserted or overwritten in the database
         * @param <T> the type of modelled objects in the table
         * @return a builder of this type
         */
        public <T> Builder addPutItem(MappedTableResource<T> mappedTableResource, T item) {
            return addPutItem(
                mappedTableResource,
                PutItemEnhancedRequest.builder(mappedTableResource.tableSchema().itemType().rawClass())
                                      .item(item)
                                      .build());
        }

        /**
         * Adds an item to be updated, and it's associated table, to the transaction. For more information on the update
         * action, see the low-level operation description in for instance
         * {@link DynamoDbTable#updateItem(UpdateItemEnhancedRequest)} and how to construct the low-level request in
         * {@link UpdateItemEnhancedRequest}.
         *
         * @param mappedTableResource the table to write the item to
         * @param request A {@link UpdateItemEnhancedRequest}
         * @param <T> the type of modelled objects in the table
         * @return a builder of this type
         */
        public <T> Builder addUpdateItem(MappedTableResource<T> mappedTableResource,
                                         UpdateItemEnhancedRequest<T> request) {
            itemSupplierList.add(() -> generateTransactWriteItem(mappedTableResource,
                                                                 UpdateItemOperation.create(request)));
            return this;
        }

        /**
         * Adds an item to be updated, and it's associated table, to the transaction. For more information on the update
         * action, see the low-level operation description in for instance
         * {@link DynamoDbTable#updateItem(UpdateItemEnhancedRequest)}.
         *
         * @param mappedTableResource the table to write the item to
         * @param item an item to update or insert into the database as part of this transaction
         * @param <T> the type of modelled objects in the table
         * @return a builder of this type
         */
        public <T> Builder addUpdateItem(MappedTableResource<T> mappedTableResource, T item) {
            return addUpdateItem(
                mappedTableResource,
                UpdateItemEnhancedRequest.builder(mappedTableResource.tableSchema().itemType().rawClass())
                                         .item(item)
                                         .build());
        }

        /**
         * Sets the clientRequestToken in this builder.
         *
         * @param clientRequestToken the clientRequestToken going to be used for build
         * @return a builder of this type
         */
        public Builder clientRequestToken(String clientRequestToken) {
            this.clientRequestToken = clientRequestToken;
            return this;
        }

        /**
         * Builds a {@link TransactWriteItemsEnhancedRequest} from the values stored in this builder.
         */
        public TransactWriteItemsEnhancedRequest build() {
            return new TransactWriteItemsEnhancedRequest(this);
        }

        private <T> TransactWriteItem generateTransactWriteItem(MappedTableResource<T> mappedTableResource,
                                                                TransactableWriteOperation<T> generator) {
            return generator.generateTransactWriteItem(mappedTableResource.tableSchema(),
                                                       DefaultOperationContext.create(mappedTableResource.tableName()),
                                                       mappedTableResource.mapperExtension());
        }
    }
}
