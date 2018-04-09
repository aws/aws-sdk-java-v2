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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.fail;
import static software.amazon.awssdk.testutils.service.S3BucketUtils.temporaryBucketName;

import java.io.IOException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.core.regions.Region;
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.NoSuchUploadException;
import software.amazon.awssdk.services.s3.model.S3Exception;

public class ExceptionUnmarshallingIntegrationTest extends S3IntegrationTestBase {

    private static final String BUCKET = temporaryBucketName(ExceptionUnmarshallingIntegrationTest.class);

    private static final String KEY = "some-key";

    @BeforeClass
    public static void setupFixture() throws IOException {
        createBucket(BUCKET);
    }

    @AfterClass
    public static void tearDownFixture() {
        deleteBucketAndAllContents(BUCKET);
    }

    @Test(expected = BucketAlreadyOwnedByYouException.class)
    public void createBucketAlreadyOwnedByYou() {
        s3.createBucket(b -> b.bucket(BUCKET));
    }

    @Test(expected = BucketAlreadyExistsException.class)
    public void createBucketAlreadyExists() {
        s3.createBucket(b -> b.bucket("development"));
    }

    @Test(expected = NoSuchKeyException.class)
    public void getObjectNoSuchKey() {
        s3.getObject(GetObjectRequest.builder().bucket(BUCKET).key(KEY).build());
    }

    @Test(expected = NoSuchBucketException.class)
    public void getObjectNoSuchBucket() {
        s3.getObject(GetObjectRequest.builder().bucket(BUCKET + KEY).key(KEY).build());
    }

    @Test(expected = NoSuchKeyException.class)
    public void getObjectAclNoSuchKey() {
        s3.getObjectAcl(b -> b.bucket(BUCKET).key(KEY));
    }

    @Test(expected = NoSuchBucketException.class)
    public void getObjectAclNoSuchBucket() {
        s3.getObjectAcl(b -> b.bucket(BUCKET + KEY).key(KEY));
    }

    @Test(expected = NoSuchUploadException.class)
    public void abortMultipartNoSuchUpload() {
        s3.abortMultipartUpload(b -> b.bucket(BUCKET).key(KEY).uploadId("23232"));
    }

    @Test
    public void listObjectsWrongRegion() {
        assertThatThrownBy(() -> {
            try (S3Client client = s3ClientBuilder().region(Region.EU_CENTRAL_1).build()) {
                client.listObjectsV2(b -> b.bucket(BUCKET));
            }
        }).isExactlyInstanceOf(S3Exception.class) // Make sure it's not a modeled exception, because that's what we're testing
          .hasMessageContaining("The bucket you are attempting to access must be addressed using the specified endpoint.");
    }

    @Test
    public void headObjectNoSuchKey() {
        try {

            s3.headObject(b -> b.bucket(BUCKET).key(KEY));
            fail("No exception has been thrown");
        } catch (S3Exception ex) {
            // This is a limitation of HEAD requests since S3 doesn't return an XML body containing the error details.
            assertThat(ex.statusCode()).isEqualTo(404);
            assertThat(ex.errorCode()).isEqualTo("404 Not Found");
        }
    }

    @Test
    public void headBuckettNoSuchBucket() {
        try {
            s3.headBucket(b -> b.bucket(KEY));
            fail("No exception has been thrown");
        } catch (S3Exception ex) {
            // This is a limitation of HEAD requests since S3 doesn't return an XML body containing the error details.
            assertThat(ex.statusCode()).isEqualTo(404);
            assertThat(ex.errorCode()).isEqualTo("404 Not Found");
        }
    }
}
