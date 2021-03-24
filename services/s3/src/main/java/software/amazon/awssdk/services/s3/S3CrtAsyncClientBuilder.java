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

package software.amazon.awssdk.services.s3;

import software.amazon.awssdk.annotations.SdkPreviewApi;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;

/**
 * A builder for creating an instance of {@link S3CrtAsyncClient}. This can be created with the static
 * {@link S3AsyncClient#crtBuilder()} method.
 */
@SdkPreviewApi
@SdkPublicApi
public interface S3CrtAsyncClientBuilder extends AwsClientBuilder<S3CrtAsyncClientBuilder, S3CrtAsyncClient> {

    S3CrtAsyncClientBuilder partSizeBytes(long partSizeBytes);

    S3CrtAsyncClientBuilder maxThroughputGbps(double maxThroughputGbps);
}
