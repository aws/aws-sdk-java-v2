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

import com.amazonaws.HttpMethod;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GeneratePresignedUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.UploadRequest;

public class S3Transforms {

    void upload(S3TransferManager tm, String bucket, String key) {
        InputStream inputStream = new ByteArrayInputStream(("HelloWorld").getBytes());
        PutObjectRequest requestWithInputStream = PutObjectRequest.builder().bucket(bucket).key(key).websiteRedirectLocation("location")
                .build();
        /*AWS SDK for Java v2 migration: When using InputStream to upload with TransferManager, you must specify Content-Length and ExecutorService.*/tm.upload(UploadRequest.builder().putObjectRequest(requestWithInputStream).requestBody(AsyncRequestBody.fromInputStream(inputStream, -1L, newExecutorServiceVariableToDefine)).build());
    }

    private void generatePresignedUrl(S3Client s3, String bucket, String key, Date expiration) {
        URL urlHead = /*AWS SDK for Java v2 migration: S3 generatePresignedUrl() with HEAD HTTP method is not supported in v2. Only GET, PUT, and DELETE are supported - https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3/presigner/S3Presigner.html*/s3.generatePresignedUrl(bucket, key, expiration, HttpMethod.HEAD);

        URL urlPatch = /*AWS SDK for Java v2 migration: S3 generatePresignedUrl() with PATCH HTTP method is not supported in v2. Only GET, PUT, and DELETE are supported - https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3/presigner/S3Presigner.html*/s3.generatePresignedUrl(bucket, key, expiration, HttpMethod.PATCH);

        URL urlPost = /*AWS SDK for Java v2 migration: S3 generatePresignedUrl() with POST HTTP method is not supported in v2. Only GET, PUT, and DELETE are supported - https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3/presigner/S3Presigner.html*/s3.generatePresignedUrl(bucket, key, expiration, HttpMethod.POST);


        HttpMethod httpMethod = HttpMethod.PUT;
        URL urlWithHttpMethodVariable = /*AWS SDK for Java v2 migration: Transform for S3 generatePresignedUrl() with an assigned variable for HttpMethod is not supported. Update your code to pass in enum instead and run the migration tool again, or manually migrate your code - https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3/presigner/S3Presigner.html*/s3.generatePresignedUrl(bucket, key, expiration, httpMethod);

        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, key);
        /*AWS SDK for Java v2 migration: Transforms are not supported for GeneratePresignedUrlRequest, please manually migrate your code.- https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3/presigner/S3Presigner.html*/s3.generatePresignedUrl(request);
    }
}
