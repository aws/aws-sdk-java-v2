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
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.internal.http.loader.DefaultSdkHttpClientBuilder;
import software.amazon.awssdk.enhanced.dynamodb.converter.ItemAttributeValueConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.DefaultDynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.model.ConverterAware;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.utils.SdkAutoCloseable;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * A synchronous client for interacting with DynamoDB.
 *
 * <p>
 * This enhanced DynamoDB client replaces the generated {@link DynamoDbClient} with one that is easier for a Java customer to
 * use.  It does this by converting Java types into DynamoDB types.
 *
 * <p>
 * For example, the client makes it possible to persist a Book object in DynamoDB, without needing to manually convert the fields
 * (e.g. {@code List<Author>}) into DynamoDB-specific types (e.g. {@code List<Map<String, AttributeValue>>}).
 *
 * <p>
 * This can be created using the static {@link #builder()} or {@link #create()} methods. The client must be {@link #close()}d
 * when it is done being used.
 *
 * <p>
 * A {@code DynamoDbEnhancedClient} is thread-safe and relatively expensive to create. We strongly advise you to create a single
 * {@code DynamoDbEnhancedClient} instance that is reused throughout your whole application.
 *
 * <p>
 * Example Usage:
 * <code>
 * // Create a client to use for this example, and then close it. Usually, one client would be used throughout an application.
 * try (DynamoDbEnhancedClient client = DynamoDbEnhancedClient.create()) {
 *     Table booksTable = client.table("books");
 *     ResponseItem book = booksTable.getItem(...);
 * }
 * </code>
 *
 * @see DynamoDbEnhancedAsyncClient
 */
@SdkPublicApi
@ThreadSafe
@Immutable
public interface DynamoDbEnhancedClient extends ToCopyableBuilder<DynamoDbEnhancedClient.Builder, DynamoDbEnhancedClient>,
                                                SdkAutoCloseable {
    /**
     * Create a {@link DynamoDbEnhancedClient} with default configuration.
     *
     * <p>
     * The credentials and AWS Region will be loaded automatically, using the same semantics as {@link DynamoDbClient#create()}.
     *
     * <p>
     * Equivalent to {@code DynamoDbEnhancedClient.builder().build()}.
     *
     * <p>
     * Example Usage:
     * <code>
     * // Create a client to use for this example, and then close it. Usually, one client would be used throughout an application.
     * try (DynamoDbEnhancedClient client = DynamoDbEnhancedClient.create()) {
     *     Table booksTable = client.table("books");
     *     ResponseItem book = booksTable.getItem(...);
     * }
     * </code>
     *
     * <p>
     * Reasons this call may fail with a {@link RuntimeException}:
     * <ol>
     *     <li>We cannot automatically determine your AWS region. See {@link DefaultAwsRegionProviderChain} for where we check
     *     for the region.</li>
     *     <li>We cannot automatically determine your credentials. See {@link DefaultCredentialsProvider} for where we check
     *     for your credentials.</li>
     *     <li>We cannot automatically determine your HTTP client implementation. See {@link DefaultSdkHttpClientBuilder} for
     *     where we check for an HTTP implementation.</li>
     * </ol>
     *
     * @see #builder()
     */
    static DynamoDbEnhancedClient create() {
        return builder().build();
    }

    /**
     * Create a {@link DynamoDbEnhancedClient.Builder} that can be used to create a {@link DynamoDbEnhancedClient} with custom
     * configuration.
     *
     * <p>
     * The credentials and AWS Region will be loaded from the configured {@link DynamoDbClient} (or
     * {@link DynamoDbClient#create()} if one is not configured).
     *
     * <p>
     * Sensible defaults will be used for any values not directly configured.
     *
     * <p>
     * Example Usage:
     * <code>
     * // Create a client to use for this example, and then close it. Usually, one client would be used throughout an application.
     * try (DynamoDbEnhancedClient client = DynamoDbEnhancedClient.builder()
     *                                                            .dynamoDbClient(DynamoDbClient.create())
     *                                                            .build()) {
     *     Table booksTable = client.table("books");
     *     ResponseItem book = booksTable.getItem(...);
     * }
     * </code>
     *
     * <p>
     * This call should never fail with an {@link Exception}.
     *
     * @see #create()
     */
    static DynamoDbEnhancedClient.Builder builder() {
        return DefaultDynamoDbEnhancedClient.builder();
    }

    /**
     * Retrieve a {@link Table} that can be used for interacting with DynamoDB. This does not make any remote calls, and as a
     * result does not validate that the requested table actually exists.
     *
     * <p>
     * If the table does not exist, exceptions will be thrown when trying to load or retrieve data from the returned
     * {@link Table} object. The returned {@link Table} will stop working if the enhanced client is {@link #close()}d.
     *
     * <p>
     * Example Usage:
     * <code>
     * // Create a client to use for this example, and then close it. Usually, one client would be used throughout an application.
     * try (DynamoDbEnhancedClient client = DynamoDbEnhancedClient.create()) {
     *     Table booksTable = client.table("books");
     *     ResponseItem book = booksTable.getItem(...);
     * }
     * </code>
     *
     * <p>
     * Reasons this call may fail with a {@link RuntimeException}:
     * <ol>
     *     <li>The provided table name is null.</li>
     * </ol>
     */
    Table table(String tableName);

    /**
     * Close this client, and release all resources it is using. If a client was configured with
     * {@link Builder#dynamoDbClient(DynamoDbClient)}, it <b>will not</b> be closed, and <b>must</b> be closed separately.
     *
     * <p>
     * This call should never fail with an {@link Exception}.
     */
    @Override
    void close();

    /**
     * A builder for {@link DynamoDbEnhancedClient}.
     *
     * <p>
     * This can be created using the static {@link DynamoDbEnhancedClient#builder()} method.
     *
     * <p>
     * Multiple clients can be created by the same builder, but unlike clients the builder <b>is not thread-safe</b> and
     * should not be used from multiple threads at the same time.
     *
     * <p>
     * Example Usage:
     * <code>
     * // Create a client to use for this example, and then close it. Usually, one client would be used throughout an application.
     * try (DynamoDbEnhancedClient client = DynamoDbEnhancedClient.builder()
     *                                                            .dynamoDbClient(DynamoDbClient.create())
     *                                                            .build()) {
     *     Table booksTable = client.table("books");
     *     ResponseItem book = booksTable.getItem(...);
     * }
     * </code>
     */
    @NotThreadSafe
    interface Builder extends CopyableBuilder<Builder, DynamoDbEnhancedClient>, ConverterAware.Builder {
        /**
         * Configure a generated client to be used by the enhanced client to interact with DynamoDB. The enhanced client
         * will use the credentials and AWS Region of the provided generated client.
         *
         * <p>
         * The provided client <b>will not be closed</b> when {@link DynamoDbEnhancedClient#close()} is invoked, and <b>must</b>
         * be closed separately when it is done being used to prevent leaking resources.
         *
         * <p>
         * If this is not configured, {@link DynamoDbClient#create()} will be used (and cleaned up with
         * {@link DynamoDbEnhancedClient#close()}).
         *
         * <p>
         * Reasons this call may fail with a {@link RuntimeException}:
         * <ol>
         *     <li>If this method is called in parallel from multiple threads. This method is not thread safe.</li>
         * </ol>
         */
        Builder dynamoDbClient(DynamoDbClient client);

        @Override
        Builder addConverters(Collection<? extends ItemAttributeValueConverter> converters);

        @Override
        Builder addConverter(ItemAttributeValueConverter converter);

        @Override
        Builder clearConverters();

        /**
         * Build a {@link DynamoDbEnhancedClient} from the provided configuration. This method can be invoked multiple times to
         * create multiple {@code DynamoDbEnhancedClient} instances.
         *
         * <p>
         * Reasons this call may fail with a {@link RuntimeException}:
         * <ol>
         *     <li>We cannot automatically determine your AWS region. See {@link DefaultAwsRegionProviderChain} for where we check
         *     for the region.</li>
         *     <li>We cannot automatically determine your credentials. See {@link DefaultCredentialsProvider} for where we check
         *     for your credentials.</li>
         *     <li>We cannot automatically determine your HTTP client implementation. See {@link DefaultSdkHttpClientBuilder} for
         *     where we check for an HTTP implementation.</li>
         *     <li>If any mutating methods are called in parallel with this one. This class is not thread safe.</li>
         * </ol>
         */
        @Override
        DynamoDbEnhancedClient build();
    }
}
