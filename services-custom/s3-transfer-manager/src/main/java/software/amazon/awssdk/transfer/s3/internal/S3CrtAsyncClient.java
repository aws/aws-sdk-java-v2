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

package software.amazon.awssdk.transfer.s3.internal;

import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientAsyncConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.utils.builder.SdkBuilder;

/**
 * Service client for accessing Amazon S3 asynchronously using the AWS Common Runtime S3 client. This can be created using the
 * static {@link #builder()} method.
 */
@SdkInternalApi
public interface S3CrtAsyncClient extends S3AsyncClient {

    interface S3CrtAsyncClientBuilder extends SdkBuilder<S3CrtAsyncClientBuilder, S3CrtAsyncClient> {
        S3CrtAsyncClientBuilder credentialsProvider(AwsCredentialsProvider credentialsProvider);

        S3CrtAsyncClientBuilder region(Region region);

        S3CrtAsyncClientBuilder minimumPartSizeInBytes(Long uploadPartSize);

        S3CrtAsyncClientBuilder targetThroughputInGbps(Double targetThroughputInGbps);

        S3CrtAsyncClientBuilder maxConcurrency(Integer maxConcurrency);

        /**
         * Specify overrides to the default SDK async configuration that should be used for clients created by this builder.
         */
        S3CrtAsyncClientBuilder asyncConfiguration(ClientAsyncConfiguration configuration);

        /**
         * Similar to {@link #asyncConfiguration(ClientAsyncConfiguration)}, but takes a lambda to configure a new
         * {@link ClientAsyncConfiguration.Builder}. This removes the need to called {@link ClientAsyncConfiguration#builder()}
         * and {@link ClientAsyncConfiguration.Builder#build()}.
         */
        default S3CrtAsyncClientBuilder asyncConfiguration(Consumer<ClientAsyncConfiguration.Builder> clientAsyncConfiguration) {
            return asyncConfiguration(ClientAsyncConfiguration.builder().applyMutation(clientAsyncConfiguration).build());
        }

        @Override
        S3CrtAsyncClient build();
    }

    /**
     * Create a builder that can be used to configure and create a {@link S3AsyncClient}.
     */
    static S3CrtAsyncClientBuilder builder() {
        return new DefaultS3CrtAsyncClient.DefaultS3CrtClientBuilder();
    }
}
