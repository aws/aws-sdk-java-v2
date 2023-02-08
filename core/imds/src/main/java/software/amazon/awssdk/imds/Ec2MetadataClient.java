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
import software.amazon.awssdk.imds.internal.DefaultEc2MetadataClient;
import software.amazon.awssdk.utils.SdkAutoCloseable;


/**
 *  Interface to represent the Ec2Metadata Client Class. Used to access instance metadata from a running EC2 instance.
 *  <h2>Instantiate the Ec2MetadataClient</h2>
 *  <h3>Default configuration</h3>
 * {@snippet :
 * Ec2MetadataClient client = Ec2MetadataClient.create();
 * }
 * <h3>Custom configuration</h3>
 *  Example of a client configured for using IPV6 and a fixed delay for retry attempts :
 * {@snippet :
 * Ec2MetadataClient client = Ec2MetadataClient.builder()
 *     .retryPolicy(p -> p.backoffStrategy(FixedDelayBackoffStrategy.create(Duration.ofMillis(500))))
 *     .endpointMode(EndpointMode.IPV6)
 *     .build();
 * }
 * <h2>Use the client</h2>
 * To retrieve EC2 Instance Metadata, call the {@code get} method on the client with a path to an instance metadata:
 * {@snippet :
 * Ec2MetadataClient client = Ec2MetadataClient.create();
 * Ec2MetadataResponse response = client.get("/latest/meta-data/");
 * System.out.println(response.asString());
 * }
 * <h2>Closing the client</h2>
 * Once all operations are done, you may close the client to free any resources used by it.
 * {@snippet :
 * Ec2MetadataClient client = Ec2MetadataClient.create();
 * // ... do the things
 * client.close();
 * }
 * <br/>Note: A single client instance should be reused for multiple requests when possible.
 */
@SdkPublicApi
public interface Ec2MetadataClient extends SdkAutoCloseable {

    /**
     * Gets the specified instance metadata value by the given path. For more information about instance metadata, check the
     * <a href=https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ec2-instance-metadata.html>Instance metadata documentation</a>
     *
     * @param path  Input path
     * @return Instance metadata value as part of MetadataResponse Object
     */
    Ec2MetadataResponse get(String path);

    /**
     * Create an {@link Ec2MetadataClient} instance using the default values.
     *
     * @return the client instance.
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
         * lifetime if the httpClient and must therefore be closed explicitly by calling the {@link SdkHttpClient#close()}
         * method on it.
         * <p>
         * If not specified, the IMDS client will look for a SdkHttpClient class included in the classpath of the
         * application and create a new instance of that class, managed by the IMDS Client, that will be closed when the IMDS
         * Client is closed. If no such class can be found, will throw a {@link  SdkClientException}.
         *
         * @param httpClient the http client
         * @return a reference to this builder
         */
        Builder httpClient(SdkHttpClient httpClient);

        /**
         * A http client builder used to retrieve an instance of an {@link SdkHttpClient}. If specified, the Ec2 Metadata Client
         * will use the instance returned by the builder and manage its lifetime by closing the http client once the Ec2 Client
         * itself is closed.
         *
         * @param builder the builder to used to retrieve an instance.
         * @return a reference to this builder
         */
        Builder httpClient(SdkHttpClient.Builder<?> builder);
    }

}
