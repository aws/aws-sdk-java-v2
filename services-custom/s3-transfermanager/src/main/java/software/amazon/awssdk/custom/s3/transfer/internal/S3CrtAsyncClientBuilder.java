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

package software.amazon.awssdk.custom.s3.transfer.internal;

import java.net.URI;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;

/**
 * A builder for creating an instance of {@link S3CrtAsyncClient}. This can be created with the static
 * {@link S3AsyncClient#crtBuilder()} method.
 */
@SdkInternalApi
public interface S3CrtAsyncClientBuilder extends AwsClientBuilder<S3CrtAsyncClientBuilder, S3CrtAsyncClient> {
    @Override
    S3CrtAsyncClientBuilder credentialsProvider(AwsCredentialsProvider credentialsProvider);

    @Override
    S3CrtAsyncClientBuilder region(Region region);

    @Override
    S3CrtAsyncClientBuilder overrideConfiguration(ClientOverrideConfiguration overrideConfiguration);

    @Override
    S3CrtAsyncClientBuilder overrideConfiguration(Consumer<ClientOverrideConfiguration.Builder> overrideConfiguration);

    @Override
    S3CrtAsyncClientBuilder endpointOverride(URI endpointOverride);

    S3CrtAsyncClientBuilder partSizeBytes(Long partSizeBytes);

    S3CrtAsyncClientBuilder maxThroughputGbps(Double maxThroughputGbps);

    @Override
    S3CrtAsyncClient build();
}
