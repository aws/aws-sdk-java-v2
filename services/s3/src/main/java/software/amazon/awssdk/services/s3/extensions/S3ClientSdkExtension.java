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

import software.amazon.awssdk.annotations.SdkExtensionMethod;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.internal.extensions.DefaultS3ClientSdkExtension;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * Extension methods for the {@link S3Client} interface.
 *
 * <p>These are convenience methods that provide higher-level abstractions over S3 service API calls.
 * They are available directly on the {@link S3Client} interface.
 */
@SdkPublicApi
public interface S3ClientSdkExtension {

    /**
     * Check whether the specified bucket exists in Amazon S3 and is accessible to the caller.
     *
     * <p>This method calls {@code HeadBucket} and interprets the response:
     * <ul>
     *   <li>200 — the bucket exists and is accessible, returns {@code true}</li>
     *   <li>404 — the bucket does not exist, returns {@code false}</li>
     *   <li>403 — the bucket may exist but the caller does not have access, throws {@link S3Exception}</li>
     * </ul>
     *
     * @param bucket the bucket name
     * @return {@code true} if the bucket exists and is accessible; {@code false} if it does not exist
     * @throws S3Exception if S3 returns an error other than 404 (e.g., 403 Access Denied)
     */
    @SdkExtensionMethod
    default boolean doesBucketExist(String bucket) {
        return new DefaultS3ClientSdkExtension((S3Client) this).doesBucketExist(bucket);
    }

    /**
     * Check whether the specified object exists in Amazon S3 and is accessible to the caller.
     *
     * <p>This method calls {@code HeadObject} and interprets the response:
     * <ul>
     *   <li>200 — the object exists, returns {@code true}</li>
     *   <li>404 — the object does not exist, returns {@code false}</li>
     *   <li>403 — access is denied, throws {@link S3Exception}. Note that S3 returns 403 instead of 404
     *       when the caller lacks {@code s3:ListBucket} permission on the bucket.</li>
     * </ul>
     *
     * @param bucket the bucket containing the object
     * @param key the object key
     * @return {@code true} if the object exists and is accessible; {@code false} if it does not exist
     * @throws S3Exception if S3 returns an error other than 404 (e.g., 403 Access Denied)
     */
    @SdkExtensionMethod
    default boolean doesObjectExist(String bucket, String key) {
        return new DefaultS3ClientSdkExtension((S3Client) this).doesObjectExist(bucket, key);
    }
}
