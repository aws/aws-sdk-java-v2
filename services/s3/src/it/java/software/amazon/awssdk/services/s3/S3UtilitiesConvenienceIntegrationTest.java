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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.core.sync.RequestBody;

public class S3UtilitiesConvenienceIntegrationTest extends S3IntegrationTestBase {

    private static final String BUCKET = temporaryBucketName(S3UtilitiesConvenienceIntegrationTest.class);
    private static final String KEY = "test-key";

    @BeforeClass
    public static void setupFixture() throws Exception {
        setUp();
        createBucket(BUCKET);
        s3.putObject(r -> r.bucket(BUCKET).key(KEY), RequestBody.fromString("hello"));
    }

    @AfterClass
    public static void tearDown() {
        deleteBucketAndAllContents(BUCKET);
    }

    @Test
    public void doesObjectExist_existingObject_returnsTrue() {
        assertThat(s3.utilities().doesObjectExist(BUCKET, KEY)).isTrue();
    }

    @Test
    public void doesObjectExist_nonExistentObject_returnsFalse() {
        assertThat(s3.utilities().doesObjectExist(BUCKET, "no-such-key")).isFalse();
    }

    @Test
    public void doesBucketExist_existingBucket_returnsTrue() {
        assertThat(s3.utilities().doesBucketExist(BUCKET)).isTrue();
    }

    @Test
    public void doesBucketExist_nonExistentBucket_returnsFalse() {
        assertThat(s3.utilities().doesBucketExist("no-such-bucket-" + java.util.UUID.randomUUID())).isFalse();
    }
}
