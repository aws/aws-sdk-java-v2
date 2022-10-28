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

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.imds.internal.DefaultEc2MetadataAsyncClient;

/**
 *  Interface to represent the Ec2Metadata Client Class. Used to access instance metadata from a running instance.
 */
@SdkPublicApi
public interface Ec2MetadataAsyncClient extends SdkClient {
    String SERVICE_NAME = "EC2metadata";

    CompletableFuture<MetadataResponse> get(String path);

    /**
     * @return The Builder Object consisting all the fields.
     */
    Ec2MetadataAsyncClient.Builder toBuilder();

    static Ec2MetadataAsyncClient create() {
        return builder().build();
    }

    static Ec2MetadataAsyncClient.Builder builder() {
        return DefaultEc2MetadataAsyncClient.builder();
    }

    /**
     * The builder definition for a {@link Ec2MetadataClient}. All parameters are optional and have default values if not
     * specified. Therefore, an instance can be simply created with {@code Ec2MetadataAsyncClient.builder().build()} or {@code
     * Ec2MetadataAsyncClient.create()}, both having the same result.
     */
    interface Builder  {

        /**
         * Define the retry policy which includes the number of retry attempts for any failed request.
         *
         * @param retryPolicy The retry policy which includes the number of retry attempts for any failed request.
         * @return Returns a reference to this builder
         */
        Builder retryPolicy(Ec2MetadataRetryPolicy retryPolicy);

        /**
         * Define the endpoint of IMDS.
         *
         * @param endpoint The endpoint of IMDS.
         * @return Returns a reference to this builder
         */
        Builder endpoint(URI endpoint);

        /**
         * Define the Time to live (TTL) of the token.
         *
         * @param tokenTtl The Time to live (TTL) of the token.
         * @return Returns a reference to this builder
         */
        Builder tokenTtl(Duration tokenTtl);

        /**
         * Define the endpoint mode of IMDS. Supported values include IPv4 and IPv6.
         *
         * @param endpointMode The endpoint mode of IMDS.Supported values include IPv4 and IPv6.
         * @return Returns a reference to this builder
         */
        Builder endpointMode(EndpointMode endpointMode);

        /**
         * Define the {@link SdkAsyncHttpClient} instance to make the http requests.
         *
         * @param httpClient The SdkHttpClient instance to make the http requests.
         * @return Returns a reference to this builder
         */
        Builder httpClient(SdkAsyncHttpClient httpClient);

        /**
         * Define the {@link ScheduledExecutorService} used to schedule asynchronous retry attempts.
         *
         * @param scheduledExecutorService the ScheduledExecutorService to use for retry attempt.
         * @return a reference to this builder
         */
        Builder scheduledExecutorService(ScheduledExecutorService scheduledExecutorService);

        Ec2MetadataAsyncClient build();

    }
}
