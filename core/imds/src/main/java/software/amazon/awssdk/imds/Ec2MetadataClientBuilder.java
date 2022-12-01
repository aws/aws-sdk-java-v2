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
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.core.retry.backoff.BackoffStrategy;
import software.amazon.awssdk.imds.internal.Ec2MetadataEndpointProvider;
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
     * <p>
     * If not specified, defaults to 3 retry attempts and a {@link BackoffStrategy#defaultStrategy()}  backoff strategy} that
     * uses {@link RetryMode#STANDARD}
     *
     * @param retryPolicy The retry policy which includes the number of retry attempts for any failed request.
     * @return a reference to this builder
     */
    B retryPolicy(Ec2MetadataRetryPolicy retryPolicy);

    Ec2MetadataRetryPolicy getRetryPolicy();

    /**
     * Define the endpoint of IMDS.
     * <p>
     * If not specified, the IMDS client will attempt to automatically resolve the endpoint value
     * based on the logic of {@link Ec2MetadataEndpointProvider}.
     *
     * @param endpoint The endpoint of IMDS.
     * @return a reference to this builder
     */
    B endpoint(URI endpoint);

    URI getEndpoint();

    /**
     * Define the Time to live (TTL) of the token. The token represents a logical session. The TTL specifies the length of time
     * that the token is valid and, therefore, the duration of the session. Defaults to 21,600 seconds (6 hours) if not specified.
     *
     * @param tokenTtl The Time to live (TTL) of the token.
     * @return a reference to this builder
     */
    B tokenTtl(Duration tokenTtl);

    Duration getTokenTtl();

    /**
     * Define the endpoint mode of IMDS. Supported values include IPv4 and IPv6. Used to determine the endpoint of the IMDS
     * Client only if {@link Ec2MetadataClientBuilder#endpoint(URI)} is not specified. Only one of both endpoint or endpoint mode
     * but not both. If both are specified in the Builder, a {@link IllegalArgumentException} will be thrown.
     * <p>
     * If not specified, the IMDS client will attempt to automatically resolve the endpoint mode value
     * based on the logic of {@link Ec2MetadataEndpointProvider}.
     *
     * @param endpointMode The endpoint mode of IMDS. Supported values include IPv4 and IPv6.
     * @return a reference to this builder
     */
    B endpointMode(EndpointMode endpointMode);

    EndpointMode getEndpointMode();

    /**
     * Define the token caching strategy to use when executing IMDS requests.
     * <p>
     * If not specified, defaults to {@link TokenCacheStrategy#NONE}, and a request to fetch a new token will be executed for each
     * metadata request.
     *
     * @param tokenCacheStrategy the strategy to use for token caching
     * @return a reference to this builder
     */
    B tokenCacheStrategy(TokenCacheStrategy tokenCacheStrategy);

    TokenCacheStrategy getTokenCacheStrategy();

}
