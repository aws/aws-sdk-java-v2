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

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.model.RequestItem;
import software.amazon.awssdk.enhanced.dynamodb.model.ResponseItem;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.utils.CompletableFutureUtils;

/**
 * An asynchronous way of interacting with a DynamoDB table. This is usually created via
 * {@link DynamoDbEnhancedAsyncClient#table(String)}.
 *
 * <p>
 * This provides facilities for writing {@link RequestItem}s to a table and reading {@link ResponseItem}s from a table. All
 * operations are nonblocking, returning a {@link CompletableFuture} for the result, instead of the result itself.
 *
 * <p>
 * Supported operations:
 * <ol>
 *     <li>{@link #getItem(RequestItem)} to retrieve a single item from the table.</li>
 *     <li>{@link #putItem(RequestItem)} to write a single item to a table.</li>
 * </ol>
 *
 * <p>
 * This {@link AsyncTable} will throw exceptions from all operations if the {@link DynamoDbEnhancedAsyncClient} that was used to
 * create it is closed.
 */
@SdkPublicApi
@ThreadSafe
@Immutable
public interface AsyncTable {
    /**
     * Retrieve the name of the DynamoDB table. This does not make a service call, and does not guarantee that the DynamoDB table
     * actually exists.
     *
     * <p>
     * This call should never fail with an {@link Exception}.
     */
    default String name() {
        throw new UnsupportedOperationException();
    }

    /**
     * Call DynamoDB to write the requested item, potentially replacing an existing item that has a key matching this item. If
     * this does not throw an exception, the item was successfully written.
     *
     * <p>
     * This has the same semantics as {@link DynamoDbAsyncClient#putItem(PutItemRequest)}.
     *
     * <p>
     * This returns a future that will only be completed when the item is done being written.
     *
     * <p>
     * Example Usage:
     * <code>
     * // Create a client to use for this example, and then close it. Usually, one client would be used throughout an application.
     * try (DynamoDbEnhancedAsyncClient client = DynamoDbEnhancedAsyncClient.create()) {
     *     AsyncTable booksTable = client.table("books");
     *
     *     // Write a book to the "books" table.
     *     CompletableFuture<Void> serviceCallCompleteFuture =
     *             booksTable.putItem(RequestItem.builder()
     *                                           .putAttribute("isbn", "0-330-25864-8")
     *                                           .putAttribute("title", "The Hitchhiker's Guide to the Galaxy")
     *                                           .build());
     *
     *     // Log when the book is done being written.
     *     CompletableFuture<Void> resultLoggedFuture = serviceCallCompleteFuture.thenAccept(() -> {
     *         System.out.println("Book was successfully written!");
     *     });
     *
     *     // Block this thread until after we log that the book was written.
     *     resultLoggedFuture.join();
     * }
     * </code>
     *
     * <p>
     * This call should never throw an {@link Exception}. All exceptions will be returned via the produced
     * {@link CompletableFuture}.
     *
     * <p>
     * Reasons this may complete the future with a {@link RuntimeException}:
     * <ol>
     *     <li>This table may not exist.</li>
     *     <li>The item cannot be converted to a DynamoDB request.</li>
     *     <li>The call to DynamoDB failed.</li>
     * </ol>
     */
    default CompletableFuture<Void> putItem(RequestItem item) {
        throw new UnsupportedOperationException();
    }

    /**
     * Call DynamoDB to write the requested item, potentially replacing an existing item that has a key matching this item. If
     * this does not throw an exception, the item was successfully written.
     *
     * <p>
     * This has the same semantics as {@link DynamoDbAsyncClient#putItem(PutItemRequest)}.
     *
     * <p>
     * This returns a future that will only be completed when the item is done being written.
     *
     * <p>
     * This is a less verbose way of calling {@link #putItem(RequestItem)}.
     *
     * <p>
     * Example Usage:
     * <code>
     * // Create a client to use for this example, and then close it. Usually, one client would be used throughout an application.
     * try (DynamoDbEnhancedAsyncClient client = DynamoDbEnhancedAsyncClient.create()) {
     *     AsyncTable booksTable = client.table("books");
     *
     *     // Write a book to the "books" table.
     *     CompletableFuture<Void> serviceCallCompleteFuture =
     *             booksTable.putItem(item -> item.putAttribute("isbn", "0-330-25864-8")
     *                                            .putAttribute("title", "The Hitchhiker's Guide to the Galaxy"));
     *
     *     // Log when the book is done being written.
     *     CompletableFuture<Void> resultLoggedFuture = serviceCallCompleteFuture.thenAccept(ignored -> {
     *         System.out.println("Book was successfully written!");
     *     });
     *
     *     // Block this thread until after we log that the book was written.
     *     resultLoggedFuture.join();
     * }
     * </code>
     *
     * <p>
     * This call should never throw an {@link Exception}. All exceptions will be returned via the produced
     * {@link CompletableFuture}.
     *
     * <p>
     * Reasons this may complete the future with a {@link RuntimeException}:
     * <ol>
     *     <li>This table may not exist.</li>
     *     <li>The item cannot be converted to a DynamoDB request.</li>
     *     <li>The call to DynamoDB failed.</li>
     * </ol>
     */
    default CompletableFuture<Void> putItem(Consumer<RequestItem.Builder> item) {
        try {
            RequestItem.Builder itemBuilder = RequestItem.builder();
            item.accept(itemBuilder);
            return putItem(itemBuilder.build());
        } catch (RuntimeException e) {
            return CompletableFutureUtils.failedFuture(e);
        }
    }

    /**
     * Call DynamoDB to read the item that matches the provided key. This will throw an exception if the item cannot be found.
     *
     * <p>
     * This has the same semantics as {@link DynamoDbAsyncClient#getItem(GetItemRequest)}, which means this performs eventually
     * consistent reads by default.
     *
     * <p>
     * This returns a future that will only be completed when the item is done being read.
     *
     * <p>
     * Example Usage:
     * <code>
     * // Create a client to use for this example, and then close it. Usually, one client would be used throughout an application.
     * try (DynamoDbEnhancedAsyncClient client = DynamoDbEnhancedAsyncClient.create()) {
     *     AsyncTable booksTable = client.table("books");
     *
     *     // Get a book from the "books" table and print its title.
     *     CompletableFuture<ResponseItem> bookFuture =
     *             booksTable.getItem(RequestItem.builder()
     *                                           .putAttribute("isbn", "0-330-25864-8")
     *                                           .build());
     *
     *     // Log the book title when it's done being read from DynamoDB
     *     CompletableFuture<Void> resultLoggedFuture =
     *             bookFuture.thenAccept(book -> System.out.println(book.getAttribute("title").asString()));
     *
     *     // Block this thread until after we log the book.
     *     resultLoggedFuture.join();
     * }
     * </code>
     *
     * <p>
     * This call should never throw an {@link Exception}. All exceptions will be returned via the produced
     * {@link CompletableFuture}.
     *
     * <p>
     * Reasons this call may fail with a {@link RuntimeException}:
     * <ol>
     *     <li>This table may not exist.</li>
     *     <li>The call to DynamoDB failed.</li>
     *     <li>The request item cannot be converted to a DynamoDB request.</li>
     *     <li>The DynamoDB Response cannot be converted to an item.</li>
     * </ol>
     */
    default CompletableFuture<ResponseItem> getItem(RequestItem key) {
        throw new UnsupportedOperationException();
    }

    /**
     * Call DynamoDB to read the item that matches the provided key. This will throw an exception if the item cannot be found.
     *
     * <p>
     * This has the same semantics as {@link DynamoDbAsyncClient#getItem(GetItemRequest)}, which means this performs eventually
     * consistent reads by default.
     *
     * <p>
     * This returns a future that will only be completed when the item is done being read.
     *
     * <p>
     * This is a less verbose way of calling {@link #getItem(RequestItem)}.
     *
     * <p>
     * Example Usage:
     * <code>
     * // Create a client to use for this example, and then close it. Usually, one client would be used throughout an application.
     * try (DynamoDbEnhancedAsyncClient client = DynamoDbEnhancedAsyncClient.create()) {
     *     AsyncTable booksTable = client.table("books");
     *
     *     // Get a book from the "books" table and print its title.
     *     CompletableFuture<ResponseItem> bookFuture =
     *             booksTable.getItem(key -> key.putAttribute("isbn", "0-330-25864-8"));
     *
     *     // Log the book title when it's done being read from DynamoDB
     *     CompletableFuture<Void> resultLoggedFuture =
     *             bookFuture.thenAccept(book -> System.out.println(book.getAttribute("title").asString()));
     *
     *     // Block this thread until after we log the book.
     *     resultLoggedFuture.join();
     * }
     * </code>
     *
     * <p>
     * This call should never throw an {@link Exception}. All exceptions will be returned via the produced
     * {@link CompletableFuture}.
     *
     * <p>
     * Reasons this call may fail with a {@link RuntimeException}:
     * <ol>
     *     <li>This table may not exist.</li>
     *     <li>The call to DynamoDB failed.</li>
     *     <li>The request item cannot be converted to a DynamoDB request.</li>
     *     <li>The DynamoDB Response cannot be converted to an item.</li>
     * </ol>
     */
    default CompletableFuture<ResponseItem> getItem(Consumer<RequestItem.Builder> key) {
        try {
            RequestItem.Builder itemBuilder = RequestItem.builder();
            key.accept(itemBuilder);
            return getItem(itemBuilder.build());
        } catch (RuntimeException e) {
            return CompletableFutureUtils.failedFuture(e);
        }
    }
}
