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

package software.amazon.awssdk.imds;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.imds.internal.DefaultEc2MetadataAsyncClient;
import software.amazon.awssdk.utils.SdkAutoCloseable;

/**
 * Interface to represent the Ec2Metadata Client Class. Used to access instance metadata from a running instance.
 */
@SdkPublicApi
public interface Ec2MetadataAsyncClient extends SdkAutoCloseable {

    /**
     * Gets the specified instance metadata value by the given path.
     *
     * @param path Input path
     * @return A CompletableFuture that completes when the MetadataResponse is made available.
     */
    CompletableFuture<MetadataResponse> get(String path);

    /**
     * Create an {@link Ec2MetadataAsyncClient} instance using the default values.
     *
     * @return
     */
    static Ec2MetadataAsyncClient create() {
        return builder().build();
    }

    static Ec2MetadataAsyncClient.Builder builder() {
        return DefaultEc2MetadataAsyncClient.builder();
    }

    /**
     * The builder definition for a {@link Ec2MetadataClient}. All parameters are optional and have default values if not
     * specified. Therefore, an instance can be simply created with {@code Ec2MetadataAsyncClient.builder().build()} or
     * {@code Ec2MetadataAsyncClient.create()}, both having the same result.
     */
    interface Builder extends Ec2MetadataClientBuilder<Ec2MetadataAsyncClient.Builder, Ec2MetadataAsyncClient> {

        /**
         * Define the {@link ScheduledExecutorService} used to schedule asynchronous retry attempts. If provided, the
         * Ec2MetadataClient will <em>NOT</em> manage the lifetime if the httpClient and must therefore be
         * closed explicitly by calling the {@link SdkAsyncHttpClient#close()} method on it.
         * <p>
         * If not specified, defaults to {@link Executors#newScheduledThreadPool} with a default value of 3 thread in the
         * pool.
         *
         * @param scheduledExecutorService the ScheduledExecutorService to use for retry attempt.
         * @return a reference to this builder
         */
        Builder scheduledExecutorService(ScheduledExecutorService scheduledExecutorService);

        /**
         * Define the http client used by the Ec2 Metadata client. If provided, the Ec2MetadataClient will <em>NOT</em> manage the
         * lifetime if the httpClient and must therefore be closed explicitly by calling the {@link SdkAsyncHttpClient#close()}
         * method on it.
         * <p>
         * If not specified, the IMDS client will look for a SdkAsyncHttpClient class included in the classpath of the
         * application and creates a new instance of that class, managed by the IMDS Client, that will be closed when the IMDS
         * Client is closed. If no such class can be found, will throw a {@link SdkClientException}.
         *
         * @param httpClient the http client
         * @return a reference to this builder
         */
        Builder httpClient(SdkAsyncHttpClient httpClient);
    }
}
