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

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.imds.internal.DefaultEc2MetadataClient;
import software.amazon.awssdk.utils.SdkAutoCloseable;


/**
 *  Interface to represent the Ec2Metadata Client Class. Used to access instance metadata from a running instance.
 */
@SdkPublicApi
public interface Ec2MetadataClient extends SdkAutoCloseable {

    /**
     * Gets the specified instance metadata value by the given path.
     * @param path  Input path
     * @return Instance metadata value as part of MetadataResponse Object
     */
    MetadataResponse get(String path);

    /**
     * Create an {@link Ec2MetadataClient} instance using the default values.
     */
    static Ec2MetadataClient create() {
        return builder().build();
    }

    /**
     * Creates a default builder for {@link Ec2MetadataClient}.
     */
    static Builder builder() {
        return DefaultEc2MetadataClient.builder();
    }

    /**
     * The builder definition for a {@link Ec2MetadataClient}.
     */
    interface Builder extends Ec2MetadataClientBuilder<Ec2MetadataClient.Builder, Ec2MetadataClient> {

        /**
         * Define the http client used by the Ec2 Metadata client. If provided, the Ec2MetadataClient will <em>NOT</em> manage the
         * lifetime if the httpClient and must therefore be closed explicitly by calling the {@link SdkAsyncHttpClient#close()}
         * method on it.
         * <p>
         * If not specified, the IMDS client will look for a SdkHttpClient class included in the classpath of the
         * application and creates a new instance of that class, managed by the IMDS Client, that will be closed when the IMDS
         * Client is closed. If no such class can be found, will throw a {@link  SdkClientException}.
         * </p>
         * @param httpClient the http client
         * @return a reference to this builder
         */
        Builder httpClient(SdkHttpClient httpClient);
    }

}
