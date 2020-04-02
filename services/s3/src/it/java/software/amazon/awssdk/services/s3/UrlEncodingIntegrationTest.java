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

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.testutils.service.S3BucketUtils.temporaryBucketName;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.EncodingType;
import software.amazon.awssdk.services.s3.model.ListMultipartUploadsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectVersionsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;

/**
 * Integration tests for the operations that support encoding type
 */
public class UrlEncodingIntegrationTest extends S3IntegrationTestBase {
    /**
     * The name of the bucket created, used, and deleted by these tests.
     */
    private static final String BUCKET_NAME = temporaryBucketName(UrlEncodingIntegrationTest.class);
    private static final String KEY_NAME_WITH_SPECIAL_CHARS = "filename_@_=_&_?_+_)_.temp";

    @BeforeClass
    public static void createResources() {
        createBucket(BUCKET_NAME);
        s3.putObject(PutObjectRequest.builder()
                                     .bucket(BUCKET_NAME)
                                     .key(KEY_NAME_WITH_SPECIAL_CHARS)
                                     .build(), RequestBody.fromString(RandomStringUtils.random(1000)));
    }

    /**
     * Releases all resources created in this test.
     */
    @AfterClass
    public static void tearDown() {
        deleteBucketAndAllContents(BUCKET_NAME);
    }

    @Test
    public void listObjectVersionsWithUrlEncodingType_shouldDecode() {
        ListObjectVersionsResponse listObjectVersionsResponse =
            s3.listObjectVersions(b -> b.bucket(BUCKET_NAME).encodingType(EncodingType.URL));
        listObjectVersionsResponse.versions().forEach(v -> assertKeyIsDecoded(v.key()));

        ListObjectVersionsResponse asyncResponse =
            s3Async.listObjectVersions(b -> b.bucket(BUCKET_NAME).encodingType(EncodingType.URL)).join();

        asyncResponse.versions().forEach(v -> assertKeyIsDecoded(v.key()));
    }

    @Test
    public void listObjectV2WithUrlEncodingType_shouldDecode() {
        ListObjectsV2Response listObjectsV2Response =
            s3.listObjectsV2(b -> b.bucket(BUCKET_NAME).encodingType(EncodingType.URL));

        listObjectsV2Response.contents().forEach(c -> assertKeyIsDecoded(c.key()));
        ListObjectVersionsResponse asyncResponse =
            s3Async.listObjectVersions(b -> b.bucket(BUCKET_NAME).encodingType(EncodingType.URL)).join();

        asyncResponse.versions().forEach(v -> assertKeyIsDecoded(v.key()));
    }

    @Test
    public void listObjectWithUrlEncodingType_shouldDecode() {
        ListObjectsResponse listObjectsV2Response =
            s3.listObjects(b -> b.bucket(BUCKET_NAME).encodingType(EncodingType.URL));

        listObjectsV2Response.contents().forEach(c -> assertKeyIsDecoded(c.key()));
        ListObjectVersionsResponse asyncResponse =
            s3Async.listObjectVersions(b -> b.bucket(BUCKET_NAME).encodingType(EncodingType.URL)).join();

        asyncResponse.versions().forEach(v -> assertKeyIsDecoded(v.key()));
    }

    @Test
    public void listMultipartUploadsWithUrlEncodingType_shouldDecode() {
        String uploaddId = null;
        try {
            CreateMultipartUploadResponse multipartUploadResponse =
                s3.createMultipartUpload(b -> b.bucket(BUCKET_NAME).key(KEY_NAME_WITH_SPECIAL_CHARS));
            uploaddId = multipartUploadResponse.uploadId();

            String finalUploadId = uploaddId;
            UploadPartResponse uploadPartResponse = s3.uploadPart(b -> b.bucket(BUCKET_NAME)
                                                                        .key(KEY_NAME_WITH_SPECIAL_CHARS)
                                                                        .partNumber(1)
                                                                        .uploadId(finalUploadId),
                                                                  RequestBody.fromString(RandomStringUtils.random(1000)));


            ListMultipartUploadsResponse listMultipartUploadsResponse =
                s3.listMultipartUploads(b -> b.encodingType(EncodingType.URL).bucket(BUCKET_NAME));

            listMultipartUploadsResponse.uploads().forEach(upload -> assertThat(upload.key()).isEqualTo(KEY_NAME_WITH_SPECIAL_CHARS));

            ListMultipartUploadsResponse asyncListMultipartUploadsResponse =
                s3Async.listMultipartUploads(b -> b.encodingType(EncodingType.URL).bucket(BUCKET_NAME)).join();

            asyncListMultipartUploadsResponse.uploads().forEach(upload -> assertKeyIsDecoded(upload.key()));
        } finally {
            if (uploaddId != null) {
                String finalUploadId = uploaddId;
                s3.abortMultipartUpload(b -> b.bucket(BUCKET_NAME).key(KEY_NAME_WITH_SPECIAL_CHARS).uploadId(finalUploadId));
            }
        }
    }

    private void assertKeyIsDecoded(String key) {
        assertThat(key).isEqualTo(KEY_NAME_WITH_SPECIAL_CHARS);
    }
}
