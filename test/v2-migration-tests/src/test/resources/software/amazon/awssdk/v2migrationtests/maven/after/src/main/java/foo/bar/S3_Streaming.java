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
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.RequestPayer;

public class S3_Streaming {

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

    /**
     * Mixed ordering to ensure the files are assigned correctly
     */
    void putObject_requestPojoWithFile(String bucket, String key) {
        File file1 = new File("file1.txt");
        File file2 = new File("file2.txt");
        File file3 = new File("file3.txt");
        File file4 = new File("file4.txt");

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
        s3.putObject(request1, RequestBody.fromContentProvider(() -> inputStream1, "binary/octet-stream"));

        s3.putObject(PutObjectRequest.builder().bucket(bucket).key(key).websiteRedirectLocation("location")
            .build(), RequestBody.fromContentProvider(() -> inputStream2, "binary/octet-stream"));
    }

    void putObject_requestPojoWithoutPayload(String bucket, String key) {
        PutObjectRequest request = PutObjectRequest.builder().bucket(bucket).key(key).websiteRedirectLocation("location")
            .build();
        s3.putObject(request, RequestBody.empty());
    }


    void putObjectSetters() {
        PutObjectRequest putObjectRequest =
            PutObjectRequest.builder().bucket("bucket").key("key").websiteRedirectLocation("location")
                .bucket("bucketName")
                .acl(ObjectCannedACL.AWS_EXEC_READ)
            .build();
    }

    void putObjectRequesterPaysSetter() {
        PutObjectRequest requestWithTrue = PutObjectRequest.builder().bucket("bucket").key("key").websiteRedirectLocation("location").requestPayer(RequestPayer.REQUESTER)
            .build();

        PutObjectRequest requestWithFalse =PutObjectRequest.builder().bucket("bucket").key("key").websiteRedirectLocation("location")
            .build();
    }
}