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

package software.amazon.awssdk.extensions.dynamodb.mappingclient;

import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.model.CreateTableEnhancedRequest;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.model.DeleteItemEnhancedRequest;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.model.GetItemEnhancedRequest;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.model.PutItemEnhancedRequest;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.model.QueryEnhancedRequest;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.model.ScanEnhancedRequest;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.model.UpdateItemEnhancedRequest;

/**
 * Synchronous interface for running commands against an object that is linked to a specific DynamoDb table resource
 * and therefore knows how to map records from that table into a modelled object.
 *
 * @param <T> The type of the modelled object.
 */
@SdkPublicApi
public interface DynamoDbTable<T> extends MappedTableResource<T> {
    /**
     * Returns a mapped index that can be used to execute commands against a secondary index belonging to the table
     * being mapped by this object. Note that only a subset of the commands that work against a table will work
     * against a secondary index.
     *
     * @param indexName The name of the secondary index to build the command interface for.
     * @return A {@link DynamoDbIndex} object that can be used to execute database commands against.
     */
    DynamoDbIndex<T> index(String indexName);

    default Void createTable(CreateTableEnhancedRequest request) {
        throw new UnsupportedOperationException();
    }

    default Void createTable(Consumer<CreateTableEnhancedRequest.Builder> requestConsumer) {
        throw new UnsupportedOperationException();
    }

    default Void createTable() {
        throw new UnsupportedOperationException();
    }

    default T deleteItem(DeleteItemEnhancedRequest request) {
        throw new UnsupportedOperationException();
    }

    default T deleteItem(Consumer<DeleteItemEnhancedRequest.Builder> requestConsumer) {
        throw new UnsupportedOperationException();
    }

    default T getItem(GetItemEnhancedRequest request) {
        throw new UnsupportedOperationException();
    }

    default T getItem(Consumer<GetItemEnhancedRequest.Builder> requestConsumer) {
        throw new UnsupportedOperationException();
    }

    default SdkIterable<Page<T>> query(QueryEnhancedRequest request) {
        throw new UnsupportedOperationException();
    }

    default SdkIterable<Page<T>> query(Consumer<QueryEnhancedRequest.Builder> requestConsumer) {
        throw new UnsupportedOperationException();
    }

    default Void putItem(PutItemEnhancedRequest<T> request) {
        throw new UnsupportedOperationException();
    }

    default Void putItem(Class<? extends T> itemClass, Consumer<PutItemEnhancedRequest.Builder<T>> requestConsumer) {
        throw new UnsupportedOperationException();
    }

    default SdkIterable<Page<T>> scan(ScanEnhancedRequest request) {
        throw new UnsupportedOperationException();
    }

    default SdkIterable<Page<T>> scan(Consumer<ScanEnhancedRequest.Builder> requestConsumer) {
        throw new UnsupportedOperationException();
    }

    default SdkIterable<Page<T>> scan() {
        throw new UnsupportedOperationException();
    }

    default T updateItem(UpdateItemEnhancedRequest<T> request) {
        throw new UnsupportedOperationException();
    }

    default T updateItem(Class<? extends T> itemClass, Consumer<UpdateItemEnhancedRequest.Builder<T>> requestConsumer) {
        throw new UnsupportedOperationException();
    }
}
