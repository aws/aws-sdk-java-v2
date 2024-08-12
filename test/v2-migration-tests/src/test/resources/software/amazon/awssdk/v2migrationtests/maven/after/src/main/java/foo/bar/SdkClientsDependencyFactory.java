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

package foo.bar;

import java.time.Duration;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsClient;

public final class SdkClientsDependencyFactory {

    private SdkClientsDependencyFactory() {
    }

    public static SqsClient defaultSqsClient() {
        return SqsClient.builder()
            .build();
    }

    public static ClientOverrideConfiguration customClientConfiguration() {
        ClientOverrideConfiguration clientConfiguration = ClientOverrideConfiguration.builder()
            .retryPolicy(RetryMode.STANDARD)
            .apiCallTimeout(Duration.ofMillis(1000))
            .apiCallAttemptTimeout(Duration.ofMillis(1001))
            .putHeader("foo", "bar")
            .build();

        return clientConfiguration;
    }

    public static SqsClient sqsClientWithAllSettings() {
        return SqsClient.builder()
                .region(Region.of("us-west-2"))
                .httpClientBuilder(ApacheHttpClient.builder()
                    .connectionMaxIdleTime(Duration.ofMillis(1006))
                    .tcpKeepAlive(true)
                    .socketTimeout(Duration.ofMillis(1004))
                    .connectionTimeToLive(Duration.ofMillis(1005))
                    .connectionTimeout(Duration.ofMillis(1003))
                    .maxConnections(1002))
                .overrideConfiguration(customClientConfiguration())
            .credentialsProvider(CredentialsDependencyFactory.defaultCredentialsProviderChain())
            .build();
    }

    public static SqsClientBuilder sqsClientBuilder() {
        ClientOverrideConfiguration configuration = ClientOverrideConfiguration.builder()
            .build();
        return SqsClient.builder()
            .httpClientBuilder(ApacheHttpClient.builder()
                .maxConnections(500))
            .overrideConfiguration(configuration);
    }

    public static SqsAsyncClient defaultSqsAsyncClient() {
        return SqsAsyncClient.builder()
            .build();
    }

    public static SqsAsyncClient sqsAsyncClientWithAllSettings() {

        ClientOverrideConfiguration clientConfiguration = ClientOverrideConfiguration.builder()
            .retryPolicy(RetryMode.STANDARD)
            .apiCallTimeout(Duration.ofMillis(2001))
            .apiCallAttemptTimeout(Duration.ofMillis(2002))
            .putHeader("hello", "world")
            .build();

        return SqsAsyncClient.builder()
            .region(Region.US_WEST_2)
            .credentialsProvider(CredentialsDependencyFactory.defaultCredentialsProviderChain())
            .httpClientBuilder(NettyNioAsyncHttpClient.builder()
                .connectionTimeout(Duration.ofMillis(2004)))
            .overrideConfiguration(clientConfiguration)
            .build();
    }
}
