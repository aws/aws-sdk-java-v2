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

import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.UploadRequest;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class S3Transforms {

    void upload(S3TransferManager tm, String bucket, String key) {
        InputStream inputStream = new ByteArrayInputStream(("HelloWorld").getBytes());
        PutObjectRequest requestWithInputStream = PutObjectRequest.builder().bucket(bucket).key(key).websiteRedirectLocation("location")
                .build();
        /*AWS SDK for Java v2 migration: When using InputStream to upload with TransferManager, you must specify Content-Length and ExecutorService.*/tm.upload(UploadRequest.builder().putObjectRequest(requestWithInputStream).requestBody(AsyncRequestBody.fromInputStream(inputStream, -1L, "executorService")).build());
    }
}
