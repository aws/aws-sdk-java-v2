/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.model.RequestItem;
import software.amazon.awssdk.enhanced.dynamodb.model.ResponseItem;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

/**
 * A way to interact with a DynamoDB table. This is usually created via {@link DynamoDbEnhancedClient#table(String)}.
 *
 * This provides facilities for writing {@link RequestItem}s to a table and reading {@link ResponseItem}s from a table.
 *
 * Supported operations:
 * <ol>
 *     <li>{@link #getItem(RequestItem)} to retrieve a single item from the table.</li>
 *     <li>{@link #putItem(RequestItem)} to write a single item to a table.</li>
 * </ol>
 *
 * This {@link Table} will throw exceptions from all operations if the {@link DynamoDbEnhancedClient} that was used to create it
 * is closed.
 */
@SdkPublicApi
@ThreadSafe
@Immutable
public interface Table {
    /**
     * Retrieve the name of the DynamoDB table. This does not make a service call, and does not guarantee that the DynamoDB table
     * actually exists.
     */
    default String name() {
        throw new UnsupportedOperationException();
    }

    /**
     * Call DynamoDB to write the requested item, potentially replacing an existing item that has a key matching this item. If
     * this does not throw an exception, the item was successfully written.
     *
     * This has the same semantics as {@link DynamoDbClient#putItem(PutItemRequest)}.
     *
     * Example Usage:
     * <code>
     * // Create a client to use for this example, and then close it. Usually, one client would be used throughout an application.
     * try (DynamoDbEnhancedClient client = DynamoDbEnhancedClient.create()) {
     *     Table booksTable = client.table("books");
     *
     *     // Write a book to the "books" table.
     *     booksTable.putItem(RequestItem.builder()
     *                                   .putAttribute("isbn", "0-330-25864-8")
     *                                   .putAttribute("title", "The Hitchhiker's Guide to the Galaxy")
     *                                   .build());
     * }
     * </code>
     */
    default void putItem(RequestItem item) {
        throw new UnsupportedOperationException();
    }

    /**
     * Call DynamoDB to write the requested item, potentially replacing an existing item that has a key matching this item. If
     * this does not throw an exception, the item was successfully written.
     *
     * This has the same semantics as {@link DynamoDbClient#putItem(PutItemRequest)}.
     *
     * This is a less verbose way of calling {@link #putItem(RequestItem)}.
     *
     * Example Usage:
     * <code>
     * // Create a client to use for this example, and then close it. Usually, one client would be used throughout an application.
     * try (DynamoDbEnhancedClient client = DynamoDbEnhancedClient.create()) {
     *     Table booksTable = client.table("books");
     *
     *     // Write a book to the "books" table.
     *     booksTable.putItem(item -> item.putAttribute("isbn", "0-330-25864-8")
     *                                    .putAttribute("title", "The Hitchhiker's Guide to the Galaxy"));
     * }
     * </code>
     */
    default void putItem(Consumer<RequestItem.Builder> item) {
        RequestItem.Builder itemBuilder = RequestItem.builder();
        item.accept(itemBuilder);
        putItem(itemBuilder.build());
    }

    /**
     * Call DynamoDB to read the item that matches the provided key. This will throw an exception if the item cannot be found.
     *
     * This has the same semantics as {@link DynamoDbClient#getItem(GetItemRequest)}, which means this performs eventually
     * consistent reads by default.
     *
     * Example Usage:
     * <code>
     * // Create a client to use for this example, and then close it. Usually, one client would be used throughout an application.
     * try (DynamoDbEnhancedClient client = DynamoDbEnhancedClient.create()) {
     *     Table booksTable = client.table("books");
     *
     *     // Get a book from the "books" table and print its title.
     *     ResponseItem book = booksTable.getItem(RequestItem.builder()
     *                                                       .putAttribute("isbn", "0-330-25864-8")
     *                                                       .build());
     *     System.out.println(book.getAttribute("title").asString());
     * }
     * </code>
     */
    default ResponseItem getItem(RequestItem key) {
        throw new UnsupportedOperationException();
    }


    /**
     * Call DynamoDB to read the item that matches the provided key. This will throw an exception if the item cannot be found.
     *
     * This has the same semantics as {@link DynamoDbClient#getItem(GetItemRequest)}, which means this performs eventually
     * consistent reads by default.
     *
     * This is a less verbose way of calling {@link #getItem(RequestItem)}.
     *
     * Example Usage:
     * <code>
     * // Create a client to use for this example, and then close it. Usually, one client would be used throughout an application.
     * try (DynamoDbEnhancedClient client = DynamoDbEnhancedClient.create()) {
     *     Table booksTable = client.table("books");
     *
     *     // Get a book from the "books" table and print its title.
     *     ResponseItem book = booksTable.getItem(key -> key.putAttribute("isbn", "0-330-25864-8"));
     *     System.out.println(book.getAttribute("title").asString());
     * }
     * </code>
     */
    default ResponseItem getItem(Consumer<RequestItem.Builder> key) {
        RequestItem.Builder itemBuilder = RequestItem.builder();
        key.accept(itemBuilder);
        return getItem(itemBuilder.build());
    }
}
