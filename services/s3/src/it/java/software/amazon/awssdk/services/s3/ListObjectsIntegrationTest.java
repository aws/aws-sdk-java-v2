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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static software.amazon.awssdk.testutils.service.S3BucketUtils.temporaryBucketName;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.EncodingType;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * Integration tests for the listObjects operation in the Amazon S3 Java
 * client.
 */
public class ListObjectsIntegrationTest extends S3IntegrationTestBase {
    /**
     * One hour in milliseconds for verifying that a last modified date is recent.
     */
    private static final long ONE_HOUR_IN_MILLISECONDS = 1000 * 60 * 60;

    private static final String KEY_NAME_WITH_SPECIAL_CHARS = "special-chars-@$%";
    private static final int BUCKET_OBJECTS = 15;
    /**
     * The name of the bucket created, used, and deleted by these tests.
     */
    private static String bucketName = temporaryBucketName("list-objects-integ-test");
    /**
     * List of all keys created  by these tests.
     */
    private static List<String> keys = new ArrayList<>();


    /**
     * Releases all resources created in this test.
     */
    @AfterClass
    public static void tearDown() {
        deleteBucketAndAllContents(bucketName);
    }

    /**
     * Creates all the test resources for the tests.
     */
    @BeforeClass
    public static void createResources() throws Exception {
        createBucket(bucketName);

        NumberFormat numberFormatter = new DecimalFormat("##00");
        for (int i = 1; i <= BUCKET_OBJECTS; i++) {
            createKey("key-" + numberFormatter.format(i));
        }
        createKey("aaaaa");
        createKey("aaaaa/aaaaa/aaaaa");
        createKey("aaaaa/aaaaa+a");
        createKey("aaaaa/aaaaa//aaaaa");
        createKey(KEY_NAME_WITH_SPECIAL_CHARS);
    }

    private static void createKey(String key) {
        s3.putObject(PutObjectRequest.builder()
                                     .bucket(bucketName)
                                     .key(key)
                                     .build(),
                     RequestBody.fromString(RandomStringUtils.random(1000)));
        keys.add(key);
    }

    @Test
    public void listObjectsNoParameters() {
        ListObjectsResponse result = s3.listObjects(ListObjectsRequest.builder().bucket(bucketName).build());
        List<S3Object> objects = result.contents();

        assertEquals(keys.size(), objects.size());
        assertEquals(bucketName, result.name());
        assertS3ObjectSummariesAreValid(objects);
        assertNotNull(result.maxKeys());

        // We didn't use a delimiter, so we expect these to be empty/null
        assertNull(result.delimiter());

        // We don't expect any truncated results
        assertFalse(result.isTruncated());

        // We didn't set other request parameters, so we expect them to be empty
        assertNull(result.encodingType());
        assertThat(result.prefix()).isEmpty();
    }

    @Test
    public void listObjectsWithAllElements() {
        String delimiter = "/";
        String marker = "aaa";
        ListObjectsResponse result = s3.listObjects(ListObjectsRequest.builder()
                                                                      .bucket(bucketName)
                                                                      .prefix(KEY_NAME_WITH_SPECIAL_CHARS)
                                                                      .marker(marker)
                                                                      .encodingType(EncodingType.URL)
                                                                      .delimiter(delimiter)
                                                                      .build());
        List<S3Object> objects = result.contents();

        assertEquals(bucketName, result.name());
        assertS3ObjectSummariesAreValid(objects);
        assertEquals(marker, result.marker());
        assertEquals(delimiter, result.delimiter());
        assertEquals(KEY_NAME_WITH_SPECIAL_CHARS, result.prefix());

        assertFalse(result.isTruncated());
        assertTrue(result.maxKeys() >= 1000);
    }

    @Test
    public void listObjectsWithMaxKeys() {
        int maxKeys = 4;
        ListObjectsResponse result = s3.listObjects(ListObjectsRequest.builder()
                                                                      .bucket(bucketName)
                                                                      .maxKeys(maxKeys)
                                                                      .build());

        List<S3Object> objects = result.contents();

        assertEquals(maxKeys, objects.size());
        assertEquals(bucketName, result.name());
        assertThat(maxKeys).isEqualTo(result.maxKeys());
        assertS3ObjectSummariesAreValid(objects);

        // We didn't use a delimiter, so we expect this to be empty/null
        assertNull(result.delimiter());

        // We expect truncated results since we set maxKeys
        assertTrue(result.isTruncated());

        // URL encoding is requested by default

        // We didn't set other request parameters, so we expect them to be empty
        assertNull(result.encodingType());
        assertThat(result.prefix()).isEmpty();
        assertNull(result.delimiter());
    }

    /**
     * Asserts that a list of S3Object objects are valid, by checking
     * that expected fields are not null or empty, that ETag values don't
     * contain leading or trailing quotes, that the last modified date is
     * recent, etc.
     *  @param objectSummaries The list of objects to validate.
     *
     */
    private void assertS3ObjectSummariesAreValid(List<S3Object> objectSummaries) {
        for (S3Object obj : objectSummaries) {
            assertTrue(obj.eTag().length() > 1);
            assertTrue(obj.key().length() > 1);

            // Verify that the last modified date is within an hour
            assertNotNull(obj.lastModified());
            long offset = obj.lastModified().toEpochMilli() - Instant.now().toEpochMilli();
            assertTrue(offset < ONE_HOUR_IN_MILLISECONDS);

            assertTrue(obj.storageClassAsString().length() > 1);
        }
    }
}
