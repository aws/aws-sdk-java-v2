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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.isEmptyString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static software.amazon.awssdk.testutils.service.S3BucketUtils.temporaryBucketName;

import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.testutils.RandomTempFile;

/**
 * Integration tests for the listObjectsV2 operation in the Amazon S3 Java
 * client.
 */
public class S3ListObjectsV2IntegrationTest extends S3IntegrationTestBase {
    /**
     * One hour in milliseconds for verifying that a last modified date is recent.
     */
    private static final long ONE_HOUR_IN_MILLISECONDS = 1000 * 60 * 60;

    /**
     * Content length for sample keys created by these tests.
     */
    private static final long CONTENT_LENGTH = 123;

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

    /**
     * Creates a test object in S3 with the specified name, using random ASCII
     * data of the default content length as defined in this test class.
     *
     * @param key The key under which to create the object in this test class'
     *            test bucket.
     */
    private static void createKey(String key) throws Exception {

        File file = new RandomTempFile("list-objects-integ-test-" + new Date().getTime(), CONTENT_LENGTH);

        s3.putObject(PutObjectRequest.builder()
                                     .bucket(bucketName)
                                     .key(key)
                                     .build(),
                     RequestBody.fromFile(file));
        keys.add(key);
    }

    /*
     * Individual Tests
     */
    @Test
    public void testListNoParameters() {
        ListObjectsV2Response result = s3.listObjectsV2(ListObjectsV2Request.builder().bucket(bucketName).build());
        List<S3Object> objects = result.contents();

        assertEquals(keys.size(), objects.size());
        assertThat(keys.size(), equalTo(result.keyCount()));
        assertEquals(bucketName, result.name());
        assertS3ObjectSummariesAreValid(objects, false);
        assertNotNull(result.maxKeys());

        // We didn't use a delimiter, so we expect these to be empty/null
        assertNull(result.delimiter());

        // We don't expect any truncated results
        assertFalse(result.isTruncated());
        assertNull(result.nextContinuationToken());

        // We didn't set other request parameters, so we expect them to be empty
        assertNull(result.encodingType());
        assertThat(result.prefix(), equalTo(""));
        assertNull(result.continuationToken());
    }

    @Test
    public void testListWithPrefixAndStartAfter() {
        String prefix = "key";
        String startAfter = "key-01";
        ListObjectsV2Response result = s3.listObjectsV2(ListObjectsV2Request.builder()
                                                                            .bucket(bucketName)
                                                                            .prefix(prefix)
                                                                            .startAfter(startAfter)
                                                                            .build());
        List<S3Object> objects = result.contents();

        assertEquals(BUCKET_OBJECTS - 1, objects.size());
        assertEquals(bucketName, result.name());
        assertS3ObjectSummariesAreValid(objects, false);
        assertEquals(startAfter, result.startAfter());
        assertEquals(prefix, result.prefix());

        // We didn't use a delimiter, so we expect it to be empty/null
        assertNull(result.delimiter());

        // We don't expect any truncated results
        assertFalse(result.isTruncated());
        assertNull(result.nextContinuationToken());

        // We didn't set any other request parameters, so we expect them to be
        // set to the defaults.
        assertTrue(result.maxKeys() >= 1000);
        assertNull(result.encodingType());
    }

    @Test
    public void testListWithPrefixAndDelimiter() {
        String prefix = "a";
        String delimiter = "/";
        ListObjectsV2Response result = s3.listObjectsV2(ListObjectsV2Request.builder()
                                                                            .bucket(bucketName)
                                                                            .prefix(prefix)
                                                                            .delimiter(delimiter)
                                                                            .build());

        List<S3Object> objects = result.contents();

        assertEquals(1, objects.size());
        assertEquals(bucketName, result.name());
        assertS3ObjectSummariesAreValid(objects, false);
        assertEquals(prefix, result.prefix());
        assertEquals(delimiter, result.delimiter());

        // We don't expect any truncated results
        assertFalse(result.isTruncated());
        assertNull(result.nextContinuationToken());

        // We didn't set other request parameters, so we expect them to be empty
        assertNull(result.startAfter());
        assertNull(result.encodingType());
        assertTrue(result.maxKeys() >= 1000);
    }

    @Test
    public void testListWithMaxKeys() {
        int maxKeys = 4;
        ListObjectsV2Response result = s3.listObjectsV2(ListObjectsV2Request.builder()
                                                                            .bucket(bucketName)
                                                                            .maxKeys(maxKeys)
                                                                            .build());

        List<S3Object> objects = result.contents();

        assertEquals(maxKeys, objects.size());
        assertEquals(bucketName, result.name());
        assertThat(maxKeys, equalTo(result.maxKeys()));
        assertS3ObjectSummariesAreValid(objects, false);

        // We didn't use a delimiter, so we expect this to be empty/null
        assertNull(result.delimiter());

        // We expect truncated results since we set maxKeys
        assertTrue(result.isTruncated());
        assertNotNull(result.nextContinuationToken());
        assertTrue(result.nextContinuationToken().length() > 1);

        // URL encoding is requested by default

        // We didn't set other request parameters, so we expect them to be empty
        assertNull(result.encodingType());
        assertThat(result.prefix(), isEmptyString());
        assertNull(result.startAfter());
        assertNull(result.delimiter());
    }

    @Test
    public void testListWithEncodingType() {
        String encodingType = "url";
        ListObjectsV2Response result = s3.listObjectsV2(ListObjectsV2Request.builder()
                                                                            .bucket(bucketName)
                                                                            .prefix(KEY_NAME_WITH_SPECIAL_CHARS)
                                                                            .encodingType(encodingType)
                                                                            .build());
        List<S3Object> objects = result.contents();

        // EncodingType should be returned in the response.
        assertEquals(encodingType, result.encodingTypeAsString());

        System.out.println(result.contents().get(0).key());

        // The key name returned in the response should have been decoded
        // from the URL encoded form S3 returned us.
        assertEquals(KEY_NAME_WITH_SPECIAL_CHARS,
                     objects.get(0).key());
    }

    @Test
    public void testListWithFetchOwner() {
        ListObjectsV2Response result = s3.listObjectsV2(ListObjectsV2Request.builder()
                                                                            .bucket(bucketName)
                                                                            .fetchOwner(true)
                                                                            .build());
        List<S3Object> objects = result.contents();
        assertS3ObjectSummariesAreValid(objects, true);
    }


    /*
     * Private Test Utilities
     */

    @Test
    public void testListPagination() {
        int firstRequestMaxKeys = 4;
        String prefix = "key";
        ListObjectsV2Response result = s3.listObjectsV2(ListObjectsV2Request.builder()
                                                                            .bucket(bucketName)
                                                                            .prefix(prefix)
                                                                            .maxKeys(firstRequestMaxKeys)
                                                                            .build());
        List<S3Object> objects = result.contents();

        assertEquals(firstRequestMaxKeys, objects.size());
        assertEquals(bucketName, result.name());
        assertEquals(prefix, result.prefix());
        assertNotNull(result.nextContinuationToken());
        assertTrue(result.isTruncated());
        assertS3ObjectSummariesAreValid(objects, false);

        for (int i = 0; i < firstRequestMaxKeys; i++) {
            assertEquals(keys.get(i), objects.get(i).key());
        }

        ListObjectsV2Response nextResults = s3.listObjectsV2(ListObjectsV2Request.builder()
                                                                                 .bucket(bucketName)
                                                                                 .prefix(prefix)
                                                                                 .continuationToken(
                                                                                         result.nextContinuationToken())
                                                                                 .build());
        List<S3Object> nextObjects = nextResults.contents();

        assertNull(nextResults.nextContinuationToken());
        assertEquals(nextResults.continuationToken(), result.nextContinuationToken());
        assertFalse(nextResults.isTruncated());
        assertEquals(prefix, nextResults.prefix());
        assertS3ObjectSummariesAreValid(nextObjects, false);
        assertEquals(nextObjects.size(), BUCKET_OBJECTS - firstRequestMaxKeys);
    }

    /**
     * Asserts that a list of S3Object objects are valid, by checking
     * that expected fields are not null or empty, that ETag values don't
     * contain leading or trailing quotes, that the last modified date is
     * recent, etc.
     *
     * @param objectSummaries    The list of objects to validate.
     * @param shouldIncludeOwner Whether owner information was requested and should be present in results.
     */

    private void assertS3ObjectSummariesAreValid(List<S3Object> objectSummaries,
                                                 boolean shouldIncludeOwner) {

        for (java.util.Iterator iterator = objectSummaries.iterator(); iterator.hasNext(); ) {
            S3Object obj = (S3Object) iterator.next();
            assertTrue(obj.eTag().length() > 1);
            assertTrue(obj.key().length() > 1);

            // Verify that the last modified date is within an hour
            assertNotNull(obj.lastModified());
            long offset = obj.lastModified().toEpochMilli() - Instant.now().toEpochMilli();
            assertTrue(offset < ONE_HOUR_IN_MILLISECONDS);

            assertTrue(obj.storageClassAsString().length() > 1);

            if (shouldIncludeOwner) {
                assertNotNull(obj.owner());
                assertTrue(obj.owner().displayName().length() > 1);
                assertTrue(obj.owner().id().length() > 1);
            }
        }
    }
}
