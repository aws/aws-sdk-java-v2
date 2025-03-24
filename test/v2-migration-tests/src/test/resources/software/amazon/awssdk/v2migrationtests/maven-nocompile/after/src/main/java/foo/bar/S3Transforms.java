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

package foo.bar;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Date;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.UploadRequest;

public class S3Transforms {

    void upload_streamWithLiteralLength(S3TransferManager tm, String bucket, String key) {
        HeadObjectResponse metadata = HeadObjectResponse.builder()
                .build();
        InputStream inputStream = new ByteArrayInputStream(("HelloWorld").getBytes());
        PutObjectRequest requestWithStreamAndLiteralLength = PutObjectRequest.builder().bucket(bucket).key(key).websiteRedirectLocation("location").contentLength(333L)
                .build();
        /*AWS SDK for Java v2 migration: When using InputStream to upload with TransferManager, you must specify Content-Length and ExecutorService.*/tm.upload(UploadRequest.builder().putObjectRequest(requestWithStreamAndLiteralLength).requestBody(AsyncRequestBody.fromInputStream(inputStream, 333, newExecutorServiceVariableToDefine)).build());
    }

    void upload_streamWithAssignedLength(S3TransferManager tm, String bucket, String key) {
        HeadObjectResponse metadata = HeadObjectResponse.builder()
                .build();
        long contentLen = 777;
        InputStream inputStream = new ByteArrayInputStream(("HelloWorld").getBytes());
        PutObjectRequest requestWithStreamAndAssignedLength = PutObjectRequest.builder().bucket(bucket).key(key).websiteRedirectLocation("location").contentLength(contentLen)
                .build();
        /*AWS SDK for Java v2 migration: When using InputStream to upload with TransferManager, you must specify Content-Length and ExecutorService.*/tm.upload(UploadRequest.builder().putObjectRequest(requestWithStreamAndAssignedLength).requestBody(AsyncRequestBody.fromInputStream(inputStream, contentLen, newExecutorServiceVariableToDefine)).build());
    }

    void upload_streamWithoutLength(S3TransferManager tm, String bucket, String key) {
        InputStream inputStream = new ByteArrayInputStream(("HelloWorld").getBytes());
        PutObjectRequest requestWithStreamAndNoLength = PutObjectRequest.builder().bucket(bucket).key(key).websiteRedirectLocation("location")
                .build();
        /*AWS SDK for Java v2 migration: When using InputStream to upload with TransferManager, you must specify Content-Length and ExecutorService.*/tm.upload(UploadRequest.builder().putObjectRequest(requestWithStreamAndNoLength).requestBody(AsyncRequestBody.fromInputStream(inputStream, -1L, newExecutorServiceVariableToDefine)).build());
    }

    void objectmetadata_unsupportedSetters(Date dateVal) {
        HeadObjectResponse metadata = HeadObjectResponse.builder()
                .build();

        /*AWS SDK for Java v2 migration: Transform for ObjectMetadata setter - expirationTimeRuleId - is not supported, please manually migrate the code by setting it on the v2 request/response object.*/metadata.expirationTimeRuleId("expirationTimeRuleId");
        /*AWS SDK for Java v2 migration: Transform for ObjectMetadata setter - ongoingRestore - is not supported, please manually migrate the code by setting it on the v2 request/response object.*/metadata.ongoingRestore(false);
        /*AWS SDK for Java v2 migration: Transform for ObjectMetadata setter - requesterCharged - is not supported, please manually migrate the code by setting it on the v2 request/response object.*/metadata.requesterCharged(false);

        /*AWS SDK for Java v2 migration: Transform for ObjectMetadata setter - lastModified - is not supported, please manually migrate the code by setting it on the v2 request/response object.*/metadata.lastModified(dateVal);
        /*AWS SDK for Java v2 migration: Transform for ObjectMetadata setter - expirationTime - is not supported, please manually migrate the code by setting it on the v2 request/response object.*/metadata.expirationTime(dateVal);
        /*AWS SDK for Java v2 migration: Transform for ObjectMetadata setter - restoreExpirationTime - is not supported, please manually migrate the code by setting it on the v2 request/response object.*/metadata.restoreExpirationTime(dateVal);

        /*AWS SDK for Java v2 migration: Transform for ObjectMetadata setter - header - is not supported, please manually migrate the code by setting it on the v2 request/response object.*/metadata.header("key", "val");
        /*AWS SDK for Java v2 migration: Transform for ObjectMetadata setter - addUserMetadata - is not supported, please manually migrate the code by setting it on the v2 request/response object.*/metadata.addUserMetadata("a", "b");
    }
}
