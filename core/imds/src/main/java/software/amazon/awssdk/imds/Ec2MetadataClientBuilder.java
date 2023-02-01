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
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.core.retry.backoff.BackoffStrategy;
import software.amazon.awssdk.imds.internal.Ec2MetadataEndpointProvider;
import software.amazon.awssdk.utils.builder.SdkBuilder;

/**
 * Base shared builder interface for Ec2MetadataClients, sync and async.
 * @param <B> the Builder Type
 * @param <T> the Ec2MetadataClient Type
 */
@SdkPublicApi
public interface Ec2MetadataClientBuilder<B, T> extends SdkBuilder<Ec2MetadataClientBuilder<B, T>, T> {
    /**
     * Define the retry policy which includes the number of retry attempts for any failed request.
     * <p>
     * If not specified, defaults to 3 retry attempts and a {@link BackoffStrategy#defaultStrategy()} backoff strategy} that
     * uses {@link RetryMode#STANDARD}. Can be also specified by using the
     * {@link Ec2MetadataClientBuilder#retryPolicy(Consumer)} method. if any of the retryPolicy methods are called multiple times,
     * only the last invocation will be considered.
     *
     * @param retryPolicy The retry policy which includes the number of retry attempts for any failed request.
     * @return a reference to this builder
     */
    B retryPolicy(Ec2MetadataRetryPolicy retryPolicy);

    /**
     * Define the retry policy which includes the number of retry attempts for any failed request. Can be used instead of
     * {@link Ec2MetadataClientBuilder#retryPolicy(Ec2MetadataRetryPolicy)} to use a "fluent consumer" syntax. User
     * <em>should not</em> manually build the builder in the consumer.
     * <p>
     * If not specified, defaults to 3 retry attempts and a {@link BackoffStrategy#defaultStrategy()} backoff strategy} that
     * uses {@link RetryMode#STANDARD}. Can be also specified by using the
     * {@link Ec2MetadataClientBuilder#retryPolicy(Ec2MetadataRetryPolicy)} method. if any of the retryPolicy methods are
     * called multiple times, only the last invocation will be considered.
     *
     * @param builderConsumer the consumer
     * @return a reference to this builder
     */
    B retryPolicy(Consumer<Ec2MetadataRetryPolicy.Builder> builderConsumer);

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

    /**
     * Define the Time to live (TTL) of the token. The token represents a logical session. The TTL specifies the length of time
     * that the token is valid and, therefore, the duration of the session. Defaults to 21,600 seconds (6 hours) if not specified.
     *
     * @param tokenTtl The Time to live (TTL) of the token.
     * @return a reference to this builder
     */
    B tokenTtl(Duration tokenTtl);

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

}
