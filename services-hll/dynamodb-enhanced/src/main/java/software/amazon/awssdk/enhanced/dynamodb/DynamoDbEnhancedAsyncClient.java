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

import java.util.Collection;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.converter.ItemAttributeValueConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.DefaultDynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.model.ConverterAware;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.utils.SdkAutoCloseable;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * A synchronous client for interacting with DynamoDB.
 *
 * This enhanced DynamoDB client replaces the generated {@link DynamoDbAsyncClient} with one that is easier for a Java customer to
 * use. It does this by converting between Java built-in types (eg. java.time.Instant) and DynamoDB attribute value types.
 *
 * This can be created using the static {@link #builder()} or {@link #create()} methods. The client must be {@link #close()}d
 * when it is done being used.
 *
 * A {@code DynamoDbEnhancedAsyncClient} is thread-safe and relatively expensive to create. It's strongly advised to create a
 * single {@code DynamoDbEnhancedAsyncClient} instance that is reused throughout your whole application.
 *
 * Example Usage:
 * <code>
 * // Create a client to use for this example, and then close it. Usually, one client would be used throughout an application.
 * try (DynamoDbEnhancedAsyncClient client = DynamoDbEnhancedAsyncClient.create()) {
 *     AsyncTable booksTable = client.table("books");
 *     CompletableFuture<ResponseItem> book = booksTable.getItem(...);
 * }
 * </code>
 *
 * @see DynamoDbEnhancedClient
 */
@SdkPublicApi
@ThreadSafe
@Immutable
public interface DynamoDbEnhancedAsyncClient
        extends ToCopyableBuilder<DynamoDbEnhancedAsyncClient.Builder, DynamoDbEnhancedAsyncClient>,
                SdkAutoCloseable {
    /**
     * Create a {@link DynamoDbEnhancedAsyncClient} with default configuration.
     *
     * The credentials and region will be loaded automatically, using the same semantics as {@link DynamoDbAsyncClient#create()}.
     *
     * Equivalent to {@code DynamoDbEnhancedAsyncClient.builder().build()}.
     *
     * Example Usage:
     * <code>
     * // Create a client to use for this example, and then close it. Usually, one client would be used throughout an application.
     * try (DynamoDbEnhancedAsyncClient client = DynamoDbEnhancedAsyncClient.create()) {
     *     AsyncTable booksTable = client.table("books");
     *     CompletableFuture<ResponseItem> book = booksTable.getItem(...);
     * }
     * </code>
     *
     * @see #builder()
     */
    static DynamoDbEnhancedAsyncClient create() {
        return builder().build();
    }

    /**
     * Create a {@link DynamoDbEnhancedAsyncClient.Builder} that can be used to create a {@link DynamoDbEnhancedAsyncClient}
     * with custom configuration.
     *
     * The credentials and region will be loaded from the configured {@link DynamoDbAsyncClient} (or
     * {@link DynamoDbAsyncClient#create()} if one is not configured).
     *
     * Sensible defaults will be used for any values not directly configured.
     *
     * Example Usage:
     * <code>
     * // Create a client to use for this example, and then close it. Usually, one client would be used throughout an application.
     * try (DynamoDbEnhancedAsyncClient client = DynamoDbEnhancedAsyncClient.builder()
     *                                                                      .dynamoDbClient(DynamoDbAsyncClient.create())
     *                                                                      .build()) {
     *     AsyncTable booksTable = client.table("books");
     *     CompletableFuture<ResponseItem> book = booksTable.getItem(...);
     * }
     * </code>
     *
     * @see #create()
     */
    static Builder builder() {
        return DefaultDynamoDbEnhancedAsyncClient.builder();
    }

    /**
     * Retrieve an {@link AsyncTable} that can be used for interacting with DynamoDB. This does not make any remote calls,
     * and as a result does not validate that the requested table actually exists.
     *
     * If the table does not exist, exceptions will be thrown when trying to load or retrieve data from the returned
     * {@link AsyncTable} object. The returned {@link AsyncTable} will stop working if the enhanced client is {@link #close()}d.
     *
     * Example Usage:
     * <code>
     * // Create a client to use for this example, and then close it. Usually, one client would be used throughout an application.
     * try (DynamoDbEnhancedAsyncClient client = DynamoDbEnhancedAsyncClient.create()) {
     *     AsyncTable booksTable = client.table("books");
     *     CompletableFuture<ResponseItem> book = booksTable.getItem(...);
     * }
     * </code>
     */
    AsyncTable table(String tableName);

    /**
     * A builder for {@link DynamoDbEnhancedAsyncClient}.
     *
     * This can be created using the static {@link DynamoDbEnhancedAsyncClient#builder()} method.
     *
     * Multiple clients can be created by the same builder, but unlike clients the builder <b>is not thread safe</b> and
     * should not be used from multiple threads at the same time.
     *
     * Example Usage:
     * <code>
     * // Create a client to use for this example, and then close it. Usually, one client would be used throughout an application.
     * try (DynamoDbEnhancedAsyncClient client = DynamoDbEnhancedAsyncClient.builder()
     *                                                                      .dynamoDbClient(DynamoDbClient.create())
     *                                                                      .build()) {
     *     Table booksTable = client.table("books");
     *     CompletableFuture<ResponseItem> book = booksTable.getItem(...);
     * }
     * </code>
     */
    @NotThreadSafe
    interface Builder extends CopyableBuilder<Builder, DynamoDbEnhancedAsyncClient>, ConverterAware.Builder {
        /**
         * Configure a generated client to be used by the enhanced client to interact with DynamoDB. The enhanced client
         * will use the credentials and region of the provided generated client.
         *
         * The provided client <b>will not be closed</b> when {@link DynamoDbEnhancedAsyncClient#close()} is invoked, and
         * <b>must</b> be closed separately when it is done being used to prevent leaking resources.
         *
         * If this is not configured, {@link DynamoDbAsyncClient#create()} will be used (and cleaned up with
         * {@link DynamoDbEnhancedAsyncClient#close()}).
         */
        Builder dynamoDbClient(DynamoDbAsyncClient client);

        @Override
        Builder addConverters(Collection<? extends ItemAttributeValueConverter> converters);

        @Override
        Builder addConverter(ItemAttributeValueConverter converter);

        @Override
        Builder clearConverters();

        /**
         * Build a {@link DynamoDbEnhancedAsyncClient} from the provided configuration. This method can be invoked multiple
         * times to create multiple {@link DynamoDbEnhancedAsyncClient} instances.
         */
        @Override
        DynamoDbEnhancedAsyncClient build();
    }
}
