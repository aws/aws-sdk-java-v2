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
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AccessControlList;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.SSEAwsKeyManagementParams;
import com.amazonaws.services.s3.model.SSECustomerKey;
import com.amazonaws.services.s3.transfer.TransferManager;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;

public class S3Transforms {

    void upload_streamWithLiteralLength(TransferManager tm, String bucket, String key) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(333);
        InputStream inputStream = new ByteArrayInputStream(("HelloWorld").getBytes());
        PutObjectRequest requestWithStreamAndLiteralLength = new PutObjectRequest(bucket, key, "location").withMetadata(metadata);
        requestWithStreamAndLiteralLength.setInputStream(inputStream);
        tm.upload(requestWithStreamAndLiteralLength);
    }

    void upload_streamWithAssignedLength(TransferManager tm, String bucket, String key) {
        ObjectMetadata metadata = new ObjectMetadata();
        long contentLen = 777;
        metadata.setContentLength(contentLen);
        InputStream inputStream = new ByteArrayInputStream(("HelloWorld").getBytes());
        PutObjectRequest requestWithStreamAndAssignedLength = new PutObjectRequest(bucket, key, "location").withMetadata(metadata);
        requestWithStreamAndAssignedLength.setInputStream(inputStream);
        tm.upload(requestWithStreamAndAssignedLength);
    }

    void upload_streamWithoutLength(TransferManager tm, String bucket, String key) {
        InputStream inputStream = new ByteArrayInputStream(("HelloWorld").getBytes());
        PutObjectRequest requestWithStreamAndNoLength = new PutObjectRequest(bucket, key, "location");
        requestWithStreamAndNoLength.setInputStream(inputStream);
        tm.upload(requestWithStreamAndNoLength);
    }

    void putObjectRequest_unsupportedSetters() {
        SSECustomerKey sseCustomerKey = new SSECustomerKey("val");
        SSEAwsKeyManagementParams sseParams = new SSEAwsKeyManagementParams();
        AccessControlList accessControlList = new AccessControlList();

        PutObjectRequest request = new PutObjectRequest("bucket", "key", "location")
            .withSSECustomerKey(sseCustomerKey)
            .withSSEAwsKeyManagementParams(sseParams)
            .withAccessControlList(accessControlList);
    }

    void objectmetadata_unsupportedSetters(Date dateVal) {
        ObjectMetadata metadata = new ObjectMetadata();

        metadata.setExpirationTimeRuleId("expirationTimeRuleId");
        metadata.setOngoingRestore(false);
        metadata.setRequesterCharged(false);

        metadata.setLastModified(dateVal);
        metadata.setExpirationTime(dateVal);
        metadata.setRestoreExpirationTime(dateVal);

        metadata.setHeader("key", "val");
        metadata.addUserMetadata("a", "b");
    }

    private void generatePresignedUrl(AmazonS3 s3, String bucket, String key, Date expiration) {
        URL urlHead = s3.generatePresignedUrl(bucket, key, expiration, HttpMethod.HEAD);

        URL urlPatch = s3.generatePresignedUrl(bucket, key, expiration, HttpMethod.PATCH);

        URL urlPost = s3.generatePresignedUrl(bucket, key, expiration, HttpMethod.POST);

        HttpMethod httpMethod = HttpMethod.PUT;
        URL urlWithHttpMethodVariable = s3.generatePresignedUrl(bucket, key, expiration, httpMethod);

        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, key);
        s3.generatePresignedUrl(request);
    }
}
