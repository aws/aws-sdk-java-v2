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

package software.amazon.awssdk.services.s3.s3express;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.testutils.service.S3BucketUtils.temporaryBucketName;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Verify that doesObjectExist/doesBucketExist extension methods work with S3 Express (directory buckets).
 */
class S3ExpressExtensionMethodsTest extends S3ExpressIntegrationTestBase {

    private static final Region TEST_REGION = Region.US_EAST_1;
    private static final String AZ = "use1-az4";
    private static final String BUCKET = String.format(
        temporaryBucketName(S3ExpressExtensionMethodsTest.class) + "--%s--x-s3", AZ);
    private static final String KEY = "test-key";

    private static S3Client s3;

    @BeforeAll
    static void setup() {
        s3 = s3ClientBuilder(TEST_REGION).build();
        createBucketS3Express(s3, BUCKET, AZ);
        s3.putObject(r -> r.bucket(BUCKET).key(KEY), RequestBody.fromString("hello"));
    }

    @AfterAll
    static void teardown() {
        deleteBucketAndAllContents(s3, BUCKET);
        s3.close();
    }

    @Test
    void doesObjectExist_existingObject_returnsTrue() {
        assertThat(s3.doesObjectExist(BUCKET, KEY)).isTrue();
    }

    @Test
    void doesObjectExist_nonExistentObject_returnsFalse() {
        assertThat(s3.doesObjectExist(BUCKET, "fake-key")).isFalse();
    }

    @Test
    void doesBucketExist_existingBucket_returnsTrue() {
        assertThat(s3.doesBucketExist(BUCKET)).isTrue();
    }

    @Test
    void doesBucketExist_nonExistentBucket_returnsFalse() {
        String fakeBucket = String.format(
            temporaryBucketName("nonexistent") + "--%s--x-s3", AZ);
        assertThat(s3.doesBucketExist(fakeBucket)).isFalse();
    }
}
