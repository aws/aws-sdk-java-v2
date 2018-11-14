/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;

public class UploadMultiplePartIntegrationTest extends S3IntegrationTestBase {

    private static final String BUCKET = temporaryBucketName(UploadMultiplePartIntegrationTest.class);

    private static final String KEY = "uploadpart";

    @BeforeClass
    public static void setupFixture() {
        createBucket(BUCKET);
    }

    @AfterClass
    public static void tearDownFixture() {
        deleteBucketAndAllContents(BUCKET);
    }

    @Test
    public void uploadMultiplePart() {
        CreateMultipartUploadResponse multipartUpload = s3.createMultipartUpload(b ->
                                                                                     b.bucket(BUCKET).key(KEY));
        long length = "helloworld".getBytes().length;
        UploadPartResponse uploadPartResponse =
            s3.uploadPart(b -> b.bucket(BUCKET).key(KEY)
                                .uploadId(multipartUpload.uploadId())
                                .partNumber(1)
                                .contentLength(length), RequestBody.fromString("helloworld"));

        String etag = uploadPartResponse.eTag();

        CompleteMultipartUploadResponse completeMultipartUploadResponse =
            s3.completeMultipartUpload(b -> b.bucket(BUCKET).key(KEY).uploadId(multipartUpload.uploadId())
                                             .multipartUpload(CompletedMultipartUpload.builder()
                                                                                      .parts(CompletedPart.builder().
                                                                                          eTag(etag).partNumber(1).build())
                                                                                      .build()).build());

        assertThat(completeMultipartUploadResponse).isNotNull();
    }

}
