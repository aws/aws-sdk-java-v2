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

package software.amazon.awssdk.services.s3.extensions;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.testutils.service.S3BucketUtils.temporaryBucketName;

import java.io.IOException;
import java.util.Random;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.services.s3.S3IntegrationTestBase;

public class BucketExistsIntegrationTest extends S3IntegrationTestBase {

    private static final String BUCKET = temporaryBucketName(BucketExistsIntegrationTest.class);

    @BeforeClass
    public static void setupFixture() throws IOException {
        createBucket(BUCKET);
    }

    @AfterClass
    public static void tearDownFixture() {
        deleteBucketAndAllContents(BUCKET);
    }

    @Test
    public void bucketExists() {
        assertThat(s3.doesBucketExist(BUCKET)).isTrue();
    }

    @Test
    public void bucketDoesNotExist() {
        assertThat(s3.doesBucketExist(temporaryBucketName("noexist"))).isFalse();
    }
}
