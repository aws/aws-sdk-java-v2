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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static software.amazon.awssdk.testutils.service.S3BucketUtils.temporaryBucketName;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.internal.extensions.DeleteBucketAndAllContents;
import software.amazon.awssdk.services.s3.model.BucketVersioningStatus;

/**
 * Tests the {@link S3Client#deleteBucketAndAllContents(String)} extension method.
 *
 * @see DeleteBucketAndAllContents
 */
public class DeleteBucketAndAllContentsIntegrationTest extends S3IntegrationTestBase {

    private static final String BUCKET = temporaryBucketName(DeleteBucketAndAllContentsIntegrationTest.class);

    @BeforeClass
    public static void initializeTestData() {
        createBucket(BUCKET);
        s3.putBucketVersioning(r -> r
            .bucket(BUCKET)
            .versioningConfiguration(v -> v.status(BucketVersioningStatus.ENABLED)));
    }

    @AfterClass
    public static void tearDown() {
        if (s3.doesBucketExist(BUCKET)) {
            deleteBucketAndAllContents(BUCKET);
        }
    }

    @Test
    public void deleteBucketAndAllContents_WithVersioning_DeletesBucket() {
        // Populate the bucket with >1000 objects in order to exercise pagination behavior.
        int maxDeleteObjectsSize = 1_000;
        int numObjectsToCreate = maxDeleteObjectsSize + 50;
        IntStream.range(0, numObjectsToCreate).parallel().forEach(this::putObject);
        // Overwrite some keys to create multiple versions of objects
        int numKeysToOverwrite = 50;
        IntStream.range(0, numKeysToOverwrite).parallel().forEach(this::putObject);
        // Test deleting the bucket
        s3.deleteBucketAndAllContents(BUCKET);
        assertThat(s3.doesBucketExist(BUCKET), is(false));
    }

    private void putObject(int i) {
        s3.putObject(r -> r.bucket(BUCKET)
                           .key(String.valueOf(i)),
                     RequestBody.fromString(UUID.randomUUID().toString(), StandardCharsets.UTF_8));
    }
}
