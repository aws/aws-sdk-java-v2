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

import java.net.URL;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkExtensionMethod;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Utilities;
import software.amazon.awssdk.services.s3.internal.extensions.DefaultS3ClientSdkExtension;
import software.amazon.awssdk.services.s3.internal.extensions.DeleteBucketAndAllContents;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.ListObjectVersionsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * {@link SdkExtensionMethod}s for the {@link S3Client} interface.
 */
@SdkPublicApi
public interface S3ClientSdkExtension {

    /**
     * Check whether the specified bucket exists in Amazon S3 (and you have permission to access it). If the bucket exists but is
     * not accessible (e.g., due to access being denied or the bucket existing in another region), an {@link S3Exception} will be
     * thrown.
     *
     * @param bucket the bucket to check
     * @return true if the bucket exists and you have permission to access it; false if the bucket does not exist
     * @throws S3Exception if the bucket exists but is not accessible
     */
    @SdkExtensionMethod
    default boolean doesBucketExist(String bucket) {
        return new DefaultS3ClientSdkExtension((S3Client) this).doesBucketExist(bucket);
    }

    /**
     * Check whether the specified object exists in Amazon S3 (and you have permission to access it). If the object exists but is
     * not accessible (e.g., due to access being denied or the bucket existing in another region), an {@link S3Exception} will be
     * thrown.
     *
     * @param bucket the bucket that contains the object
     * @param key    the name of the object
     * @return true if the bucket object exists and you have permission to access it; false if it does not exist
     * @throws S3Exception if the bucket exists but is not accessible
     */
    @SdkExtensionMethod
    default boolean doesObjectExist(String bucket, String key) {
        return new DefaultS3ClientSdkExtension((S3Client) this).doesObjectExist(bucket, key);
    }

    /**
     * Permanently delete a bucket and all of its content, including any versioned objects and delete markers.
     * <p>
     * Internally this method will use the {@link S3Client#listObjectsV2(ListObjectsV2Request)} and {@link
     * S3Client#listObjectVersions(ListObjectVersionsRequest)} APIs to list a bucket's content, buffer keys that are eligible for
     * deletion into batches of 1000, and delete them in bulk with the {@link S3Client#deleteObjects(DeleteObjectsRequest)} API.
     * <p>
     * While this method is optimized to use batch APIs for both listing and deleting, it may not be suitable for buckets
     * containing a very large number of objects (i.e., hundreds of thousands). For such use cases, it is usually preferable to
     * either create an <i>S3 Lifecycle configuration</i> to delete the objects, or to leverage <i>S3 Batch Operations</i> to
     * perform large-scale deletes.
     * <p>
     * Note that this method does not attempt to protect against concurrent writes or modifications to a bucket. It will iterate
     * and delete the entire contents of the bucket once. If a new key is created during or after the iteration, then the final
     * call to {@link S3Client#deleteBucket(DeleteBucketRequest)} may fail.
     *
     * @param bucket the bucket to delete
     * @throws SdkException if an error occurs
     */
    @SdkExtensionMethod
    default void deleteBucketAndAllContents(String bucket) {
        new DeleteBucketAndAllContents((S3Client) this).deleteBucketAndAllContents(bucket);
    }

    /**
     * Shortcut to the {@link S3Utilities#getUrl(GetUrlRequest)} method.
     *
     * @param getUrlRequest The get URL request
     * @return the URL for the object
     */
    @SdkExtensionMethod
    default URL getUrl(GetUrlRequest getUrlRequest) {
        return ((S3Client) this).utilities().getUrl(getUrlRequest);
    }

    /**
     * Shortcut to the {@link S3Utilities#getUrl(Consumer)} method.
     *
     * @param getUrlRequest The get URL request
     * @return the URL for the object
     */
    @SdkExtensionMethod
    default URL getUrl(Consumer<GetUrlRequest.Builder> getUrlRequest) {
        return ((S3Client) this).utilities().getUrl(getUrlRequest);
    }

    /**
     * Convenience method to retrieve the {@link S3Presigner} in order to generate presigned methods.
     *
     * @return The presigner
     */
    @SdkExtensionMethod
    default S3Presigner presigner() {
        return S3Presigner.create();
    }

    /**
     * Validate that the currently configured AWS Account is the owner of a given S3 bucket. Because Amazon S3 identifies buckets
     * based on their names, an application that uses an incorrect bucket name in a request could inadvertently perform operations
     * against a different bucket than expected.
     * <p>
     * Because buckets can be deleted and re-created at any time, this method should only be used when you know that the bucket in
     * question will not be deleted after its ownership is verified. For more robust protection, you should include the {@code
     * expectedBucketOwner} parameter with all eligible requests. For more information, see:
     * <p>
     * https://docs.aws.amazon.com/AmazonS3/latest/userguide/bucket-owner-condition.html
     *
     * @param bucket the bucket to verify ownership
     */
    @SdkExtensionMethod
    default void verifyBucketOwnership(String bucket) {
        new DefaultS3ClientSdkExtension((S3Client) this).verifyBucketOwnership(bucket);
    }
}
