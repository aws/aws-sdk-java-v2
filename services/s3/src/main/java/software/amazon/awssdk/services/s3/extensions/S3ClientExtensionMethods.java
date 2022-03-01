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

package software.amazon.awssdk.services.s3.extensions;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Extension methods for the {@link S3Client} interface.
 */
@SdkPublicApi
public interface S3ClientExtensionMethods {

    default boolean doesBucketExist(String bucketName) {
        throw new UnsupportedOperationException("not yet implemented");
    }
}
