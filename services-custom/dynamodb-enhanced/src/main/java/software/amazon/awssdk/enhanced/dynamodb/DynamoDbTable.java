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

package software.amazon.awssdk.enhanced.dynamodb;

import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.CreateTableEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.DeleteItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/**
 * Synchronous interface for running commands against an object that is linked to a specific DynamoDb table resource
 * and therefore knows how to map records from that table into a modelled object.
 * <p>
 * By default, all command methods throw an {@link UnsupportedOperationException} to prevent interface extensions from
 * breaking implementing classes.
 * <p>
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

    /**
     * Creates a new table in DynamoDb with the name and schema already defined for this DynamoDbTable
     * together with additional parameters specified in the supplied request object, {@link CreateTableEnhancedRequest}.
     * <p>
     * Use {@link DynamoDbEnhancedClient#table(String, TableSchema)} to define the mapped table resource.
     * <p>
     * This operation calls the low-level DynamoDB API CreateTable operation. Note that this is an asynchronous
     * operation and that the table may not immediately be available for writes and reads. Currently, there is no
     * mechanism supported within this library to wait for/check the status of a created table. You must provide this
     * functionality yourself. Consult the CreateTable documentation for further details and constraints.
     * <p>
     * Example:
     * <pre>
     * {@code
     *
     * ProvisionedThroughput provisionedThroughput = ProvisionedThroughput.builder()
     *                                                                    .readCapacityUnits(50L)
     *                                                                    .writeCapacityUnits(50L)
     *                                                                    .build();
     * mappedTable.createTable(CreateTableEnhancedRequest.builder()
     *                                                   .provisionedThroughput(provisionedThroughput)
     *                                                   .build());
     * }
     * </pre>
     *
     * @param request A {@link CreateTableEnhancedRequest} containing optional parameters for table creation.
     */
    default void createTable(CreateTableEnhancedRequest request) {
        throw new UnsupportedOperationException();
    }

    /**
     * Creates a new table in DynamoDb with the name and schema already defined for this DynamoDbTable
     * together with additional parameters specified in the supplied request object, {@link CreateTableEnhancedRequest}.
     * <p>
     * Use {@link DynamoDbEnhancedClient#table(String, TableSchema)} to define the mapped table resource.
     * <p>
     * This operation calls the low-level DynamoDB API CreateTable operation. Note that this is an asynchronous
     * operation and that the table may not immediately be available for writes and reads. Currently, there is no
     * mechanism supported within this library to wait for/check the status of a created table. You must provide this
     * functionality yourself. Consult the CreateTable documentation for further details and constraints.
     * <p>
     * <b>Note:</b> This is a convenience method that creates an instance of the request builder avoiding the need to
     * create one manually via {@link CreateTableEnhancedRequest#builder()}.
     * <p>
     * Example:
     * <pre>
     * {@code
     *
     * ProvisionedThroughput provisionedThroughput = ProvisionedThroughput.builder()
     *                                                                    .readCapacityUnits(50L)
     *                                                                    .writeCapacityUnits(50L)
     *                                                                    .build();
     * mappedTable.createTable(r -> r.provisionedThroughput(provisionedThroughput));
     * }
     * </pre>
     *
     * @param requestConsumer A {@link Consumer} of {@link CreateTableEnhancedRequest.Builder} containing optional
     *                        parameters for table creation.
     */
    default void createTable(Consumer<CreateTableEnhancedRequest.Builder> requestConsumer) {
        throw new UnsupportedOperationException();
    }

    /**
     * Creates a new table in DynamoDb with the name and schema already defined for this DynamoDbTable.
     * <p>
     * Use {@link DynamoDbEnhancedClient#table(String, TableSchema)} to define the mapped table resource.
     * <p>
     * This operation calls the low-level DynamoDB API CreateTable operation. Note that this is an asynchronous
     * operation and that the table may not immediately be available for writes and reads. Currently, there is no
     * mechanism supported within this library to wait for/check the status of a created table. You must provide this
     * functionality yourself. Consult the CreateTable documentation for further details and constraints.
     * <p>
     * Example:
     * <pre>
     * {@code
     *
     * mappedTable.createTable();
     * }
     * </pre>
     *
     */
    default void createTable() {
        throw new UnsupportedOperationException();
    }

    /**
     * Deletes a single item from the mapped table using a supplied primary {@link Key}.
     * <p>
     * The additional configuration parameters that the enhanced client supports are defined
     * in the {@link DeleteItemEnhancedRequest}.
     * <p>
     * This operation calls the low-level DynamoDB API DeleteItem operation. Consult the DeleteItem documentation for
     * further details and constraints.
     * <p>
     * Example:
     * <pre>
     * {@code
     *
     * MyItem previouslyPersistedItem = mappedTable.delete(DeleteItemEnhancedRequest.builder().key(key).build());
     * }
     * </pre>
     *
     * @param request A {@link DeleteItemEnhancedRequest} with key and optional directives for deleting an item from the
     *                table.
     * @return The item that was persisted in the database before it was deleted.
     */
    default T deleteItem(DeleteItemEnhancedRequest request) {
        throw new UnsupportedOperationException();
    }

    /**
     * Deletes a single item from the mapped table using a supplied primary {@link Key}.
     * <p>
     * The additional configuration parameters that the enhanced client supports are defined
     * in the {@link DeleteItemEnhancedRequest}.
     * <p>
     * This operation calls the low-level DynamoDB API DeleteItem operation. Consult the DeleteItem documentation for
     * further details and constraints.
     * <p>
     * <b>Note:</b> This is a convenience method that creates an instance of the request builder avoiding the need to
     * create one manually via {@link DeleteItemEnhancedRequest#builder()}.
     * <p>
     * Example:
     * <pre>
     * {@code
     *
     * MyItem previouslyPersistedItem = mappedTable.delete(r -> r.key(key));
     * }
     * </pre>
     *
     * @param requestConsumer A {@link Consumer} of {@link DeleteItemEnhancedRequest} with key and
     * optional directives for deleting an item from the table.
     * @return The item that was persisted in the database before it was deleted.
     */
    default T deleteItem(Consumer<DeleteItemEnhancedRequest.Builder> requestConsumer) {
        throw new UnsupportedOperationException();
    }

    /**
     * Deletes a single item from the mapped table using a supplied primary {@link Key}.
     * <p>
     * This operation calls the low-level DynamoDB API DeleteItem operation. Consult the DeleteItem documentation for
     * further details and constraints.
     * <p>
     * Example:
     * <pre>
     * {@code
     *
     * MyItem previouslyPersistedItem = mappedTable.delete(key);
     * }
     * </pre>
     *
     * @param key A {@link Key} that will be used to match a specific record to delete from the database table.
     * @return The item that was persisted in the database before it was deleted.
     */
    default T deleteItem(Key key) {
        throw new UnsupportedOperationException();
    }

    /**
     * Deletes a single item from the mapped table using just the key of a supplied modelled 'key item' object.
     * <p>
     * This operation calls the low-level DynamoDB API DeleteItem operation. Consult the DeleteItem documentation for
     * further details and constraints.
     * <p>
     * Example:
     * <pre>
     * {@code
     *
     * MyItem previouslyPersistedItem = mappedTable.deleteItem(keyItem);
     * }
     * </pre>
     *
     * @param keyItem A modelled item with the primary key fields set that will be used to match a specific record to
     *                delete from the database table.
     * @return The item that was persisted in the database before it was deleted.
     */
    default T deleteItem(T keyItem) {
        throw new UnsupportedOperationException();
    }

    /**
     * Retrieves a single item from the mapped table using a supplied primary {@link Key}.
     * <p>
     * The additional configuration parameters that the enhanced client supports are defined
     * in the {@link GetItemEnhancedRequest}.
     * <p>
     * This operation calls the low-level DynamoDB API GetItem operation. Consult the GetItem documentation for
     * further details and constraints.
     * <p>
     * Example:
     * <pre>
     * {@code
     *
     * MyItem item = mappedTable.getItem(GetItemEnhancedRequest.builder().key(key).build());
     * }
     * </pre>
     *
     * @param request A {@link GetItemEnhancedRequest} with key and optional directives for retrieving an item from the
     *                table.
     * @return The retrieved item
     */
    default T getItem(GetItemEnhancedRequest request) {
        throw new UnsupportedOperationException();
    }

    /**
     * Retrieves a single item from the mapped table using a supplied primary {@link Key}.
     * <p>
     * The additional configuration parameters that the enhanced client supports are defined
     * in the {@link GetItemEnhancedRequest}.
     * <p>
     * This operation calls the low-level DynamoDB API GetItem operation. Consult the GetItem documentation for
     * further details and constraints.
     * <p>
     * <b>Note:</b> This is a convenience method that creates an instance of the request builder avoiding the need to
     * create one manually via {@link GetItemEnhancedRequest#builder()}.
     * <p>
     * Example:
     * <pre>
     * {@code
     *
     * MyItem item = mappedTable.getItem(r -> r.key(key));
     * }
     * </pre>
     *
     * @param requestConsumer A {@link Consumer} of {@link GetItemEnhancedRequest.Builder} with key and optional
     *                        directives for retrieving an item from the table.
     * @return The retrieved item
     */
    default T getItem(Consumer<GetItemEnhancedRequest.Builder> requestConsumer) {
        throw new UnsupportedOperationException();
    }

    /**
     * Retrieves a single item from the mapped table using a supplied primary {@link Key}.
     * <p>
     * This operation calls the low-level DynamoDB API GetItem operation. Consult the GetItem documentation for
     * further details and constraints.
     * <p>
     * Example:
     * <pre>
     * {@code
     *
     * MyItem item = mappedTable.getItem(key);
     * }
     * </pre>
     *
     * @param key A {@link Key} that will be used to match a specific record to retrieve from the database table.
     * @return The retrieved item
     */
    default T getItem(Key key) {
        throw new UnsupportedOperationException();
    }

    /**
     * Retrieves a single item from the mapped table using just the key of a supplied modelled 'key item'.
     * <p>
     * This operation calls the low-level DynamoDB API GetItem operation. Consult the GetItem documentation for
     * further details and constraints.
     * <p>
     * Example:
     * <pre>
     * {@code
     *
     * MyItem item = mappedTable.getItem(keyItem);
     * }
     * </pre>
     *
     * @param keyItem A modelled item with the primary key fields set that will be used to match a specific record to
     *                retrieve from the database table.
     * @return The retrieved item
     */
    default T getItem(T keyItem) {
        throw new UnsupportedOperationException();
    }

    /**
     * Executes a query against the primary index of the table using a {@link QueryConditional} expression to retrieve a
     * list of items matching the given conditions.
     * <p>
     * The result can be accessed either through iterable {@link Page}s or {@link Page#items()} directly. If you are iterating
     * the pages, the result is accessed through iterable pages (see {@link Page}) in an interactive way; each time a
     * result page is retrieved, a query call is made to DynamoDb to get those entries. If no matches are found,
     * the resulting iterator will contain an empty page. Results are sorted by sort key value in ascending order by default;
     * this behavior can be overridden in the {@link QueryEnhancedRequest}.
     * <p>
     * The additional configuration parameters that the enhanced client supports are defined
     * in the {@link QueryEnhancedRequest}.
     * <p>
     * This operation calls the low-level DynamoDB API Query operation. Consult the Query documentation for
     * further details and constraints.
     * <p>
     * Example:
     * <p>
     * 1) Iterating through pages
     *
     * <pre>
     * {@code
     * QueryConditional queryConditional = QueryConditional.keyEqualTo(Key.builder().partitionValue("id-value").build());
     * PageIterable<MyItem> results = table.query(QueryEnhancedRequest.builder()
     *                                                                        .queryConditional(queryConditional)
     *                                                                        .build());
     * results.stream().forEach(p -> p.items().forEach(item -> System.out.println(item)))
     * }
     * </pre>
     *
     * 2) Iterating through items
     *
     * <pre>
     * {@code
     * results.items().stream().forEach(item -> System.out.println(item));
     * }
     * </pre>
     *
     * @see #query(QueryConditional)
     * @see #query(Consumer)
     * @see DynamoDbClient#queryPaginator
     * @param request A {@link QueryEnhancedRequest} defining the query conditions and how
     * to handle the results.
     * @return an iterator of type {@link SdkIterable} with paginated results (see {@link Page}).
     */
    default PageIterable<T> query(QueryEnhancedRequest request) {
        throw new UnsupportedOperationException();
    }

    /**
     * This is a convenience method that creates an instance of the request builder avoiding the need to create one
     * manually via {@link QueryEnhancedRequest#builder()}.
     * <p>
     * Example:
     * <pre>
     * {@code
     *
     * PageIterable<MyItem> results =
     *     mappedTable.query(r -> r.queryConditional(QueryConditional.keyEqualTo(k -> k.partitionValue("id-value"))));
     * }
     * </pre>
     * @see #query(QueryEnhancedRequest)
     * @see #query(QueryConditional)
     * @param requestConsumer A {@link Consumer} of {@link QueryEnhancedRequest} defining the query conditions and how to
     * handle the results.
     * @return an iterator of type {@link SdkIterable} with paginated results (see {@link Page}).
     */
    default PageIterable<T> query(Consumer<QueryEnhancedRequest.Builder> requestConsumer) {
        throw new UnsupportedOperationException();
    }

    /**
     * Executes a query against the primary index of the table using a {@link QueryConditional} expression to retrieve a
     * list of items matching the given conditions.
     * <p>
     * Example:
     * <pre>
     * {@code
     *
     * PageIterable<MyItem> results =
     *     mappedTable.query(QueryConditional.keyEqualTo(Key.builder().partitionValue("id-value").build()));
     * }
     * </pre>
     *
     * @see #query(QueryEnhancedRequest)
     * @see #query(Consumer)
     * @see DynamoDbClient#queryPaginator
     * @param queryConditional A {@link QueryConditional} defining the matching criteria for records to be queried.
     * @return an iterator of type {@link SdkIterable} with paginated results (see {@link Page}).
     */
    default PageIterable<T> query(QueryConditional queryConditional) {
        throw new UnsupportedOperationException();
    }

    /**
     * Puts a single item in the mapped table. If the table contains an item with the same primary key, it will be
     * replaced with this item.
     * <p>
     * The additional configuration parameters that the enhanced client supports are defined
     * in the {@link PutItemEnhancedRequest}.
     * <p>
     * This operation calls the low-level DynamoDB API PutItem operation. Consult the PutItem documentation for
     * further details and constraints.
     * <p>
     * Example:
     * <pre>
     * {@code
     *
     * mappedTable.putItem(PutItemEnhancedRequest.builder(MyItem.class).item(item).build());
     * }
     * </pre>
     *
     * @param request A {@link PutItemEnhancedRequest} that includes the item to enter into
     * the table, its class and optional directives.
     */
    default void putItem(PutItemEnhancedRequest<T> request) {
        throw new UnsupportedOperationException();
    }

    /**
     * Puts a single item in the mapped table. If the table contains an item with the same primary key, it will be
     * replaced with this item.
     * <p>
     * The additional configuration parameters that the enhanced client supports are defined
     * in the {@link PutItemEnhancedRequest}.
     * <p>
     * This operation calls the low-level DynamoDB API PutItem operation. Consult the PutItem documentation for
     * further details and constraints.
     * <p>
     * Example:
     * <pre>
     * {@code
     *
     * mappedTable.putItem(r -> r.item(item));
     * }
     * </pre>
     *
     * @param requestConsumer A {@link Consumer} of {@link PutItemEnhancedRequest.Builder} that includes the item
     * to enter into the table, its class and optional directives.
     */
    default void putItem(Consumer<PutItemEnhancedRequest.Builder<T>> requestConsumer) {
        throw new UnsupportedOperationException();
    }

    /**
     * Puts a single item in the mapped table. If the table contains an item with the same primary key, it will be
     * replaced with this item.
     * <p>
     * This operation calls the low-level DynamoDB API PutItem operation. Consult the PutItem documentation for
     * further details and constraints.
     * <p>
     * Example:
     * <pre>
     * {@code
     *
     * mappedTable.putItem(item);
     * }
     * </pre>
     *
     * @param item the modelled item to be inserted into or overwritten in the database table.
     */
    default void putItem(T item) {
        throw new UnsupportedOperationException();
    }

    /**
     * Scans the table and retrieves all items.
     * <p>
     * The result can be accessed either through iterable {@link Page}s or items across all pages directly. Each time a
     * result page is retrieved, a query call is made to DynamoDb to get those entries. If no matches are found,
     * the resulting iterator will contain an empty page.
     * <p>
     * The additional configuration parameters that the enhanced client supports are defined
     * in the {@link ScanEnhancedRequest}.
     * <p>
     * Example:
     * <p>
     * 1) Iterating through pages
     * <pre>
     * {@code
     *
     * PageIterable<MyItem> results = mappedTable.scan(ScanEnhancedRequest.builder().consistentRead(true).build());
     * results.stream().forEach(p -> p.items().forEach(item -> System.out.println(item)))
     * }
     * </pre>
     *
     * <p>
     * 2) Iterating through items
     * <pre>
     * {@code
     *
     * PageIterable<MyItem> results = mappedTable.scan(ScanEnhancedRequest.builder().consistentRead(true).build());
     * results.items().stream().forEach(item -> System.out.println(item));
     * }
     * </pre>
     *
     * @see #scan(Consumer)
     * @see #scan()
     * @see DynamoDbClient#scanPaginator
     * @param request A {@link ScanEnhancedRequest} defining how to handle the results.
     * @return an iterator of type {@link SdkIterable} with paginated results (see {@link Page}).
     */
    default PageIterable<T> scan(ScanEnhancedRequest request) {
        throw new UnsupportedOperationException();
    }

    /**
     * This is a convenience method that creates an instance of the request builder avoiding the need to create one
     * manually via {@link ScanEnhancedRequest#builder()}.
     *
     * <p>
     * Example:
     * <pre>
     * {@code
     *
     * PageIterable<MyItem> results = mappedTable.scan(r -> r.limit(5));
     * }
     * </pre>
     *
     * @see #scan(ScanEnhancedRequest)
     * @see #scan()
     * @param requestConsumer A {@link Consumer} of {@link ScanEnhancedRequest} defining the query conditions and how to
     * handle the results.
     * @return an iterator of type {@link SdkIterable} with paginated results (see {@link Page}).
     */
    default PageIterable<T> scan(Consumer<ScanEnhancedRequest.Builder> requestConsumer) {
        throw new UnsupportedOperationException();
    }

    /**
     * Scans the table and retrieves all items using default settings.
     * <p>
     * The result can be accessed either through iterable {@link Page}s or items across all pages directly. Each time a
     * result page is retrieved, a query call is made to DynamoDb to get those entries. If no matches are found,
     * the resulting iterator will contain an empty page.
     * <p>
     * Example:
     * <pre>
     * {@code
     *
     * PageIterable<MyItem> results = mappedTable.scan();
     * }
     * </pre>
     *
     * @see #scan(ScanEnhancedRequest)
     * @see #scan(Consumer)
     * @see DynamoDbClient#scanPaginator
     * @return an iterator of type {@link SdkIterable} with paginated results (see {@link Page}).
     */
    default PageIterable<T> scan() {
        throw new UnsupportedOperationException();
    }

    /**
     * Updates an item in the mapped table, or adds it if it doesn't exist.
     * <p>
     * The additional configuration parameters that the enhanced client supports are defined
     * in the {@link UpdateItemEnhancedRequest}.
     * <p>
     * This operation calls the low-level DynamoDB API UpdateItem operation. Consult the UpdateItem documentation for
     * further details and constraints.
     * <p>
     * Example:
     * <pre>
     * {@code
     *
     * MyItem item = mappedTable.updateItem(UpdateItemEnhancedRequest.builder(MyItem.class).item(item).build());
     * }
     * </pre>
     *
     * @param request A {@link UpdateItemEnhancedRequest} that includes the item to be updated,
     * its class and optional directives.
     * @return The updated item
     */
    default T updateItem(UpdateItemEnhancedRequest<T> request) {
        throw new UnsupportedOperationException();
    }

    /**
     * Updates an item in the mapped table, or adds it if it doesn't exist.
     * <p>
     * The additional configuration parameters that the enhanced client supports are defined
     * in the {@link UpdateItemEnhancedRequest}.
     * <p>
     * This operation calls the low-level DynamoDB API UpdateItem operation. Consult the UpdateItem documentation for
     * further details and constraints.
     * <p>
     * Example:
     * <pre>
     * {@code
     *
     * MyItem item = mappedTable.updateItem(r -> r.item(item));
     * }
     * </pre>
     *
     * @param requestConsumer A {@link Consumer} of {@link UpdateItemEnhancedRequest.Builder} that includes the item
     * to be updated, its class and optional directives.
     * @return The updated item
     */
    default T updateItem(Consumer<UpdateItemEnhancedRequest.Builder<T>> requestConsumer) {
        throw new UnsupportedOperationException();
    }

    /**
     * Updates an item in the mapped table, or adds it if it doesn't exist.
     * <p>
     * This operation calls the low-level DynamoDB API UpdateItem operation. Consult the UpdateItem documentation for
     * further details and constraints.
     * <p>
     * Example:
     * <pre>
     * {@code
     *
     * MyItem item = mappedTable.updateItem(item);
     * }
     * </pre>
     *
     * @param item the modelled item to be inserted into or updated in the database table.
     * @return The updated item
     */
    default T updateItem(T item) {
        throw new UnsupportedOperationException();
    }
}
