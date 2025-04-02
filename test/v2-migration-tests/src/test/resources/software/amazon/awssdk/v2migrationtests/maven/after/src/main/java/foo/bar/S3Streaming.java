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
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.RequestPayer;
import software.amazon.awssdk.services.s3.model.Tag;
import software.amazon.awssdk.services.s3.model.Tagging;

public class S3Streaming {

    S3Client s3 = S3Client.create();

    void getObject(String bucket, String key) throws Exception {
        ResponseInputStream<GetObjectResponse> s3Object = s3.getObject(GetObjectRequest.builder().bucket(bucket).key(key)
            .build());
        s3Object.close();
    }

    void putObject_bucketKeyContent(String bucket, String key, String content) {
        s3.putObject(PutObjectRequest.builder().bucket(bucket).key(key)
            .build(), RequestBody.fromString(content));
    }

    void putObject_bucketKeyFile(String bucket, String key, File file) {
        s3.putObject(PutObjectRequest.builder().bucket(bucket).key(key)
            .build(), RequestBody.fromFile(file));
    }

    void putObject_bucketKeyStreamMetadata(String bucket, String key, InputStream stream) {
        HeadObjectResponse metadataWithLength = HeadObjectResponse.builder()
            .build();
        s3.putObject(PutObjectRequest.builder().bucket(bucket).key(key).contentLength(22L)
            .build(), RequestBody.fromInputStream(stream, 22L));


        HeadObjectResponse metadataWithoutLength = HeadObjectResponse.builder()
            .build();
        /*AWS SDK for Java v2 migration: When using InputStream to upload with S3Client, Content-Length should be specified and used with RequestBody.fromInputStream(). Otherwise, the entire stream will be buffered in memory. If content length must be unknown, we recommend using the CRT-based S3 client - https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/crt-based-s3-client.html*/s3.putObject(PutObjectRequest.builder().bucket(bucket).key(key).build(), RequestBody.fromContentProvider(() -> stream, "application/octet-stream"));

        /*AWS SDK for Java v2 migration: When using InputStream to upload with S3Client, Content-Length should be specified and used with RequestBody.fromInputStream(). Otherwise, the entire stream will be buffered in memory. If content length must be unknown, we recommend using the CRT-based S3 client - https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/crt-based-s3-client.html*/s3.putObject(PutObjectRequest.builder().bucket("bucket").key("key").build(), RequestBody.fromContentProvider(() -> stream, "application/octet-stream"));
    }

    /**
     * Mixed ordering to ensure the files are assigned correctly
     */
    void putObject_requestPojoWithFile(String bucket, String key) {
        File file4 = new File("file4.txt");
        File file3 = new File("file3.txt");
        File file1 = new File("file1.txt");
        File file2 = new File("file2.txt");

        PutObjectRequest request1 = PutObjectRequest.builder().bucket(bucket).key(key)
            .build();

        PutObjectRequest request2 = PutObjectRequest.builder().bucket(bucket).key(key).websiteRedirectLocation("location")
            .build();

        s3.putObject(PutObjectRequest.builder().bucket(bucket).key(key).websiteRedirectLocation("location")
            .build(), RequestBody.fromFile(file3));
        s3.putObject(request2, RequestBody.fromFile(file2));
        s3.putObject(PutObjectRequest.builder().bucket(bucket).key(key).websiteRedirectLocation("location")
            .build(), RequestBody.fromFile(file4));
        s3.putObject(request1, RequestBody.fromFile(file1));
    }

    void putObject_requestPojoWithInputStream(String bucket, String key) {
        InputStream inputStream1 = new ByteArrayInputStream(("HelloWorld").getBytes());
        InputStream inputStream2 = new ByteArrayInputStream(("HolaWorld").getBytes());

        PutObjectRequest request1 = PutObjectRequest.builder().bucket(bucket).key(key).websiteRedirectLocation("location")
            .build();
        /*AWS SDK for Java v2 migration: When using InputStream to upload with S3Client, Content-Length should be specified and used with RequestBody.fromInputStream(). Otherwise, the entire stream will be buffered in memory. If content length must be unknown, we recommend using the CRT-based S3 client - https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/crt-based-s3-client.html*/s3.putObject(request1, RequestBody.fromContentProvider(() -> inputStream1, "application/octet-stream"));

        HeadObjectResponse metadata = HeadObjectResponse.builder()
            .build();
        s3.putObject(PutObjectRequest.builder().bucket(bucket).key(key).websiteRedirectLocation("location").contentLength(11L)
            .build(), RequestBody.fromInputStream(inputStream2, 11L));
    }

    void putObject_requestPojoWithoutPayload(String bucket, String key) {
        PutObjectRequest request = PutObjectRequest.builder().bucket(bucket).key(key).websiteRedirectLocation("location")
            .build();
        s3.putObject(request, RequestBody.empty());
    }


    void putObjectSetters() {
        List<Tag> tags = new ArrayList<>();
        Tagging objectTagging = Tagging.builder().tagSet(tags)
            .build();

        PutObjectRequest putObjectRequest =
            PutObjectRequest.builder().bucket("bucket").key("key").websiteRedirectLocation("location")
                .bucket("bucketName")
                .websiteRedirectLocation("redirectLocation")
                .acl(ObjectCannedACL.AWS_EXEC_READ)
                .tagging(objectTagging)
            .build();
    }

    void putObjectRequesterPaysSetter() {
        PutObjectRequest requestWithTrue = PutObjectRequest.builder().bucket("bucket").key("key").websiteRedirectLocation("location").requestPayer(RequestPayer.REQUESTER)
            .build();

        PutObjectRequest requestWithFalse =PutObjectRequest.builder().bucket("bucket").key("key").websiteRedirectLocation("location")
            .build();
    }

    void putObjectRequest_setMetadata() {
        HeadObjectResponse metadata = HeadObjectResponse.builder()
            .build();

        PutObjectRequest request = PutObjectRequest.builder().bucket("bucket").key("key").websiteRedirectLocation("location")
            .build();
        request = request.toBuilder().contentLength(66L)
            .contentEncoding("UTF-8")
            .contentType("text/plain")
            .build();
    }

    void putObjectRequest_withMetadata() {
        HeadObjectResponse metadata = HeadObjectResponse.builder()
            .build();
        long contentLen = 66;
        Date expiry = new Date();

        Map<String, String> userMetadata = new HashMap<>();
        userMetadata.put("key", "value");

        PutObjectRequest request = PutObjectRequest.builder().bucket("bucket").key("key").websiteRedirectLocation("location").contentLength(contentLen)
            .contentEncoding("UTF-8")
            .contentType("text/plain")
            .contentLanguage("en-US")
            .cacheControl("must-revalidate")
            .contentDisposition("inline")
            .contentMD5("md5Val")
            .serverSideEncryption("sseEncryptionVal")
            .serverSideEncryption("sseAlgorithmVal")
            .sseCustomerKeyMD5("sseCustomerKeyMd5Val")
            .bucketKeyEnabled(true)
            .metadata(userMetadata)
            .expires(expiry.toInstant())
            .build();
    }

    void putObjectRequest_emptyMetadata() {
        HeadObjectResponse emptyMetadata1 = HeadObjectResponse.builder()
            .build();
        PutObjectRequest request1 =PutObjectRequest.builder().bucket("bucket").key("key").websiteRedirectLocation("location")
            .build();

        HeadObjectResponse emptyMetadata2 = HeadObjectResponse.builder()
            .build();
        PutObjectRequest request2 = PutObjectRequest.builder().bucket("bucket").key("key").websiteRedirectLocation("location")
            .build();
    }
}