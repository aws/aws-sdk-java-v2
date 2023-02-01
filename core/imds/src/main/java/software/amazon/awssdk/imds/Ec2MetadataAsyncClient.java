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
 * Interface to represent the Ec2Metadata Client Class. Used to access instance metadata from a running EC2 instance.
 *  <h2>Instantiate the Ec2MetadataAsyncClient</h2>
 *  <h3>Default configuration</h3>
 * {@snippet :
 * Ec2MetadataAsyncClient client = Ec2MetadataAsyncClient.create();
 * }
 * <h3>Custom configuration</h3>
 * Example of a client configured for using IPV6 and a fixed delay for retry attempts :
 * {@snippet :
 * Ec2MetadataAsyncClient client = Ec2MetadataAsyncClient.builder()
 *     .retryPolicy(p -> p.backoffStrategy(FixedDelayBackoffStrategy.create(Duration.ofMillis(500))))
 *     .endpointMode(EndpointMode.IPV6)
 *     .build();
 * }
 * <h2>Use the client</h2>
 * Call the {@code get} method on the client with a path to an instance metadata:
 * {@snippet :
 *  Ec2MetadataAsyncClient client = Ec2MetadataAsyncClient.create();
 *  CompletableFuture<Ec2MetadataResponse> response = client.get("/latest/meta-data/");
 *  response.thenAccept(System.out::println);
 * }
 * <h2>Closing the client</h2>
 * Once all operations are done, you may close the client to free any resources used by it.
 * {@snippet :
 * Ec2MetadataAsyncClient client = Ec2MetadataAsyncClient.create();
 * // ... do the things
 * client.close();
 * }
 * <br/>Note: A single client instance should be reused for multiple requests when possible.
 */
@SdkPublicApi
public interface Ec2MetadataAsyncClient extends SdkAutoCloseable {

    /**
     * Gets the specified instance metadata value by the given path. For more information about instance metadata, check the
     * <a href=https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ec2-instance-metadata.html>Instance metadata documentation</a>.
     *
     * @param path Input path
     * @return A CompletableFuture that completes when the MetadataResponse is made available.
     */
    CompletableFuture<Ec2MetadataResponse> get(String path);

    /**
     * Create an {@link Ec2MetadataAsyncClient} instance using the default values.
     *
     * @return the client instance.
     */
    static Ec2MetadataAsyncClient create() {
        return builder().build();
    }

    /**
     * Creates a builder for an async client instance.
     * @return the newly created builder instance.
     */
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

        /**
         * An http client builder used to retrieve an instance of an {@link SdkAsyncHttpClient}. If specified, the Ec2
         * Metadata Client will use the instance returned by the builder and manage its lifetime by closing the http client
         * once the Ec2 Client itself is closed.
         *
         * @param builder the builder to used to retrieve an instance.
         * @return a reference to this builder
         */
        Builder httpClient(SdkAsyncHttpClient.Builder<?> builder);
    }
}
