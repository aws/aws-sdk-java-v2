/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.io.File;
import java.util.Date;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CreateBucketConfiguration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Integration test for the copyObject operation.
 */
public class CopyObjectIntegrationTest extends S3IntegrationTestBase {

    /**
     * The S3 bucket created and used by these tests.
     */
    private static final String BUCKET_NAME = "copy-object-integ-test-" + new Date().getTime();
    /**
     * The key of the object being copied.
     */
    private static final String SOURCE_KEY = "source-key";
    /**
     * The key of the copied object.
     */
    private static final String DESTINATION_KEY = "destination-key";
    /**
     * Length of the data uploaded to S3.
     */
    private static final long CONTENT_LENGTH = 345L;
    /**
     * The file of random data uploaded to S3.
     */
    private static File file;

    /**
     * Releases resources used by tests.
     */
    @AfterClass
    public static void tearDown() {
        deleteBucketAndAllContents(BUCKET_NAME);
        file.delete();
    }

    /**
     * Creates/populates all the test data needed for these tests (bucket,
     * source object, file, source object ETag, etc).
     */
    @BeforeClass
    public static void initializeTestData() throws Exception {
        s3.createBucket(CreateBucketRequest.builder()
                                           .bucket(BUCKET_NAME)
                                           .createBucketConfiguration(CreateBucketConfiguration.builder()
                                                                                               .locationConstraint("us-west-2")
                                                                                               .build())
                                           .build());

        file = getRandomTempFile("copy-object-integ-test-" + new Date().getTime(), CONTENT_LENGTH);

        s3.putObject(PutObjectRequest.builder()
                                     .bucket(BUCKET_NAME)
                                     .key(SOURCE_KEY)
                                     .build(),
                     RequestBody.of(file));
    }

    /**
     * Tests that the simple form of the copy object operation correctly copies
     * an object.
     */
    @Test
    public void copyObject_CopiesObjectToNewKey() throws Exception {
        s3.copyObject(CopyObjectRequest.builder()
                                       .copySource(BUCKET_NAME + "/" + SOURCE_KEY)
                                       .bucket(BUCKET_NAME)
                                       .key(DESTINATION_KEY)
                                       .build());

        s3.headObject(HeadObjectRequest.builder()
                                       .bucket(BUCKET_NAME)
                                       .key(DESTINATION_KEY)
                                       .build());
    }
}
