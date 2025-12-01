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

package software.amazon.awssdk.benchmark.core;

import java.nio.file.Path;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Simple wrapper for S3 APIs so we can implement it using sync or async S3 clients.
 */
public interface S3Wrapper {
    void createBucket(CreateBucketRequest request);
    void putObject(PutObjectRequest request, Path file);
    ListObjectsV2Response listObjectsV2(ListObjectsV2Request request);
    void deleteObject(DeleteObjectRequest request);
    void deleteBucket(DeleteBucketRequest request);
    void waitUntilBucketExists(String bucketName);
}
