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

package software.amazon.awssdk.services.s3.internal.crt;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3CrtAsyncClientBuilder;

/**
 * Service client for accessing Amazon S3 asynchronously using the AWS Common Runtime S3 client. This can be created using the
 * static {@link #builder()} method.
 */
@SdkInternalApi
public interface S3CrtAsyncClient extends S3AsyncClient {

    /**
     * Create a builder that can be used to configure and create a {@link S3AsyncClient}.
     */
    static S3CrtAsyncClientBuilder builder() {
        return new DefaultS3CrtAsyncClient.DefaultS3CrtClientBuilder();
    }
}
