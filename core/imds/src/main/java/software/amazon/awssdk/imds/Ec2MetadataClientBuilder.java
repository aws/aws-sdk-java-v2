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
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.builder.SdkBuilder;

/**
 * Base shared builder interface for Ec2MetadataClient
 * @param <B> the Builder Type
 * @param <T> the Ec2MetadataClient Type
 */
@SdkPublicApi
public interface Ec2MetadataClientBuilder<B, T> extends SdkBuilder<Ec2MetadataClientBuilder<B, T>, T> {
    /**
     * Define the retry policy which includes the number of retry attempts for any failed request.
     *
     * @param retryPolicy The retry policy which includes the number of retry attempts for any failed request.
     * @return a reference to this builder
     */
    B retryPolicy(Ec2MetadataRetryPolicy retryPolicy);

    Ec2MetadataRetryPolicy getRetryPolicy();

    /**
     * Define the endpoint of IMDS.
     *
     * @param endpoint The endpoint of IMDS.
     * @return a reference to this builder
     */
    B endpoint(URI endpoint);

    URI getEndpoint();

    /**
     * Define the Time to live (TTL) of the token.
     *
     * @param tokenTtl The Time to live (TTL) of the token.
     * @return a reference to this builder
     */
    B tokenTtl(Duration tokenTtl);

    Duration getTokenTtl();

    /**
     * Define the endpoint mode of IMDS. Supported values include IPv4 and IPv6.
     *
     * @param endpointMode The endpoint mode of IMDS. Supported values include IPv4 and IPv6.
     * @return a reference to this builder
     */
    B endpointMode(EndpointMode endpointMode);

    EndpointMode getEndpointMode();

}
