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

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.testutils.RandomTempFile;

public class UserMetadataIntegrationTest extends S3IntegrationTestBase {

    /**
     * The S3 bucket created and used by these tests.
     */
    private static final String BUCKET_NAME = temporaryBucketName("user-metadata-integ-test");
    /**
     * Length of the data uploaded to S3.
     */
    private static final long CONTENT_LENGTH = 345L;
    /**
     * The file of random data uploaded to S3.
     */
    private static File file;

    /**
     * Creates/populates all the test data needed for these tests (bucket,
     * source object, file, source object ETag, etc).
     */
    @BeforeClass
    public static void initializeTestData() throws Exception {
        createBucket(BUCKET_NAME);

        file = new RandomTempFile("user-metadata-integ-test-" + new Date().getTime(), CONTENT_LENGTH);
    }

    @AfterClass
    public static void deleteAllBuckets() {
        deleteBucketAndAllContents(BUCKET_NAME);
    }

    @Test
    public void putObject_PutsUserMetadata() throws Exception {
        Map<String, String> userMetadata = new HashMap<>();
        userMetadata.put("thing1", "IAmThing1");
        userMetadata.put("thing2", "IAmThing2");

        final String key = "user-metadata-key";
        s3.putObject(PutObjectRequest.builder()
                                     .bucket(BUCKET_NAME)
                                     .key(key)
                                     .metadata(userMetadata)
                                     .build(),
                     RequestBody.of(file));

        HeadObjectResponse response = s3.headObject(HeadObjectRequest.builder()
                                                                     .bucket(BUCKET_NAME)
                                                                     .key(key)
                                                                     .build());

        Map<String, String> returnedMetadata = response.metadata();

        assertThat(returnedMetadata).containsAllEntriesOf(userMetadata);
    }
}
