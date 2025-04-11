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
import com.amazonaws.services.s3.model.SSEAwsKeyManagementParams;
import com.amazonaws.services.s3.model.SSECustomerKey;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.AccessControlPolicy;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GeneratePresignedUrlRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.UploadRequest;

public class S3Transforms {

    void s3Pojos() {
        MultiFactorAuthentication mfa = /*AWS SDK for Java v2 migration: v2 does not have a MultiFactorAuthentication POJO. Please manually set the String value on the request POJO.*/new MultiFactorAuthentication("serialNum", "token");

        DeleteObjectRequest deleteVersionRequest =
            DeleteObjectRequest.builder().bucket("bucket").key("key").versionId("id").mfa(mfa)
                .build();
    }

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

    void putObjectRequest_unsupportedSetters() {
        SSECustomerKey sseCustomerKey = new SSECustomerKey("val");
        SSEAwsKeyManagementParams sseParams = new SSEAwsKeyManagementParams();
        AccessControlPolicy accessControlList = AccessControlPolicy.builder()
            .build();

        PutObjectRequest request = /*AWS SDK for Java v2 migration: Transform for PutObjectRequest setter accessControlList is not supported, please manually migrate your code to use the v2 setters: acl, grantReadACP, grantWriteACP - https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3/model/PutObjectRequest.Builder.html#acl(java.lang.String)*/
/*AWS SDK for Java v2 migration: Transform for PutObjectRequest setter sseCustomerKey is not supported, please manually migrate your code to use the v2 setters: sseCustomerKey, sseCustomerKeyMD5 - https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3/model/PutObjectRequest.Builder.html#sseCustomerKey(java.lang.String)*/
/*AWS SDK for Java v2 migration: Transform for PutObjectRequest setter sseAwsKeyManagementParam is not supported, please manually migrate your code to use the v2 setters: ssekmsKeyId, serverSideEncryption, sseCustomerAlgorithm - https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3/model/PutObjectRequest.Builder.html#ssekmsKeyId(java.lang.String)*/
PutObjectRequest.builder().bucket("bucket").key("key").websiteRedirectLocation("location")
            .sseCustomerKey(sseCustomerKey)
            .sseAwsKeyManagementParams(sseParams)
            .accessControlList(accessControlList)
            .build();
    }

    void objectmetadata_unsupportedSetters(Date dateVal) {
        HeadObjectResponse metadata = HeadObjectResponse.builder()
            .build();

        /*AWS SDK for Java v2 migration: Transform for ObjectMetadata setter - expirationTimeRuleId - is not supported, please manually migrate the code by setting it on the v2 request/response object.*/metadata.expirationTimeRuleId("expirationTimeRuleId");
        /*AWS SDK for Java v2 migration: Transform for ObjectMetadata setter - ongoingRestore - is not supported, please manually migrate the code by setting it on the v2 request/response object.*/metadata.ongoingRestore(false);
        /*AWS SDK for Java v2 migration: Transform for ObjectMetadata setter - requesterCharged - is not supported, please manually migrate the code by setting it on the v2 request/response object.*/metadata.requesterCharged(false);

        /*AWS SDK for Java v2 migration: Transform for ObjectMetadata setter - lastModified - is not supported, please manually migrate the code by setting it on the v2 request/response object.*/metadata.lastModified(dateVal.toInstant());
        /*AWS SDK for Java v2 migration: Transform for ObjectMetadata setter - expirationTime - is not supported, please manually migrate the code by setting it on the v2 request/response object.*/metadata.expirationTime(dateVal.toInstant());
        /*AWS SDK for Java v2 migration: Transform for ObjectMetadata setter - restoreExpirationTime - is not supported, please manually migrate the code by setting it on the v2 request/response object.*/metadata.restoreExpirationTime(dateVal.toInstant());

        /*AWS SDK for Java v2 migration: Transform for ObjectMetadata setter - header - is not supported, please manually migrate the code by setting it on the v2 request/response object.*/metadata.header("key", "val");
        /*AWS SDK for Java v2 migration: Transform for ObjectMetadata setter - addUserMetadata - is not supported, please manually migrate the code by setting it on the v2 request/response object.*/metadata.addUserMetadata("a", "b");
    }

    private void generatePresignedUrl(S3Client s3, String bucket, String key, Date expiration) {
        URL urlHead = /*AWS SDK for Java v2 migration: S3 generatePresignedUrl() with HEAD HTTP method is not supported in v2. Only GET, PUT, and DELETE are supported - https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3/presigner/S3Presigner.html*/s3.generatePresignedUrl(bucket, key, expiration, HttpMethod.HEAD);

        URL urlPatch = /*AWS SDK for Java v2 migration: S3 generatePresignedUrl() with PATCH HTTP method is not supported in v2. Only GET, PUT, and DELETE are supported - https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3/presigner/S3Presigner.html*/s3.generatePresignedUrl(bucket, key, expiration, HttpMethod.PATCH);

        URL urlPost = /*AWS SDK for Java v2 migration: S3 generatePresignedUrl() with POST HTTP method is not supported in v2. Only GET, PUT, and DELETE are supported - https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3/presigner/S3Presigner.html*/s3.generatePresignedUrl(bucket, key, expiration, HttpMethod.POST);

        HttpMethod httpMethod = HttpMethod.PUT;
        URL urlWithHttpMethodVariable = /*AWS SDK for Java v2 migration: Transform for S3 generatePresignedUrl() with an assigned variable for HttpMethod is not supported. Please manually migrate your code - https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3/presigner/S3Presigner.html*/s3.generatePresignedUrl(bucket, key, expiration, httpMethod);

        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, key);
        /*AWS SDK for Java v2 migration: Transforms are not supported for GeneratePresignedUrlRequest, please manually migrate your code - https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3/presigner/S3Presigner.html*/s3.generatePresignedUrl(request);
    }
}
