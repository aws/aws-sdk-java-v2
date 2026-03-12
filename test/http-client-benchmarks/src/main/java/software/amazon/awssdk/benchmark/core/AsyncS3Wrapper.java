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
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class AsyncS3Wrapper implements S3Wrapper {
    private final S3AsyncClient s3;

    public AsyncS3Wrapper(S3AsyncClient s3) {
        this.s3 = s3;
    }

    @Override
    public void createBucket(CreateBucketRequest request) {
        s3.createBucket(request).join();
    }

    @Override
    public void putObject(PutObjectRequest request, Path file) {
        s3.putObject(request, AsyncRequestBody.fromFile(file)).join();
    }

    @Override
    public ListObjectsV2Response listObjectsV2(ListObjectsV2Request request) {
        return s3.listObjectsV2(request).join();
    }

    @Override
    public void deleteObject(DeleteObjectRequest request) {
        s3.deleteObject(request).join();
    }

    @Override
    public void deleteBucket(DeleteBucketRequest request) {
        s3.deleteBucket(request).join();
    }

    @Override
    public void waitUntilBucketExists(String bucketName) {
        s3.waiter().waitUntilBucketExists(r -> r.bucket(bucketName)).join();
    }
}
