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
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.utils.Validate;

/**
 * Extension methods for the {@link S3Client} interface.
 */
@SdkPublicApi
public interface S3ClientExtensionMethods {

    /**
     * Check whether the specified bucket exists in Amazon S3 (and you have permission to access it). If the bucket exists but is
     * not accessible (e.g., due to access being denied or the bucket existing in another region), an {@link S3Exception} will be
     * thrown.
     *
     * @param bucketName the bucket to check
     * @return true if the bucket exists and you have permission to access it; false if the bucket does not exist
     * @throws S3Exception if the bucket exists but is not accessible
     */
    default boolean doesBucketExist(String bucketName) {
        Validate.notNull(bucketName, "bucketName");
        S3Client s3Client = (S3Client) this;
        try {
            s3Client.headBucket(r -> r.bucket(bucketName));
            return true;
        } catch (NoSuchBucketException e) {
            return false;
        }
    }
}
