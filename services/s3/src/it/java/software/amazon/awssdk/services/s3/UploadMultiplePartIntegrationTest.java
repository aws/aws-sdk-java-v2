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

import java.util.concurrent.Callable;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.ListMultipartUploadsResponse;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;

public class UploadMultiplePartIntegrationTest extends UploadMultiplePartTestBase {

    @Override
    public Callable<CreateMultipartUploadResponse> createMultipartUpload(String bucket, String key) {
        return () -> s3.createMultipartUpload(b -> b.bucket(bucket).key(key));
    }

    @Override
    public Callable<UploadPartResponse> uploadPart(UploadPartRequest request, String requestBody) {
        return () -> s3.uploadPart(request, RequestBody.fromString(requestBody));
    }

    @Override
    public Callable<ListMultipartUploadsResponse> listMultipartUploads(String bucket) {
        return () -> s3.listMultipartUploads(b -> b.bucket(bucket));
    }

    @Override
    public Callable<CompleteMultipartUploadResponse> completeMultipartUpload(CompleteMultipartUploadRequest request) {
        return () -> s3.completeMultipartUpload(request);
    }

    @Override
    public Callable<AbortMultipartUploadResponse> abortMultipartUploadResponseCallable(AbortMultipartUploadRequest request) {
        return () -> s3.abortMultipartUpload(request);
    }
}
