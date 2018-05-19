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

/*
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *    http://aws.amazon.com/apache2.0
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and
 * limitations under the License.
 */

package software.amazon.awssdk.services.dynamodb.datamodeling;

import static org.junit.Assert.fail;
import static software.amazon.awssdk.testutils.service.S3BucketUtils.temporaryBucketName;

import java.util.Iterator;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketConfiguration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.S3Object;
import utils.test.util.DynamoDBIntegrationTestBase;

public class DynamoDBS3IntegrationTestBase extends DynamoDBIntegrationTestBase {
    public static final String WEST_BUCKET = temporaryBucketName("java-dynamo-s3-integ-test-west");
    public static final String EAST_BUCKET = temporaryBucketName("java-dynamo-s3-integ-test-east");

    protected static S3Client s3East;
    protected static S3Client s3West;

    @BeforeClass
    public static void setUp() throws Exception {
        DynamoDBIntegrationTestBase.setUp();
        s3East = S3Client.builder()
                         .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                         .region(Region.US_EAST_1)
                         .build();

        s3West = S3Client.builder()
                         .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                         .region(Region.US_WEST_2)
                         .build();

        createBucket(s3East, EAST_BUCKET, null);
        createBucket(s3West, WEST_BUCKET, Region.US_WEST_2.value());
    }

    @AfterClass
    public static void tearDown() {
        deleteBucketAndAllContents(s3East, EAST_BUCKET);
        deleteBucketAndAllContents(s3West, WEST_BUCKET);
    }

    /**
     * Deletes all objects in the specified bucket, and then deletes the bucket.
     *
     * @param s3         The AmazonS3 client to use.
     * @param bucketName The bucket to empty and delete.
     */
    protected static void deleteBucketAndAllContents(S3Client s3, String bucketName) {
        ListObjectsResponse response = s3.listObjects(ListObjectsRequest.builder()
                                                                        .bucket(bucketName)
                                                                        .build());

        while (true) {
            for (Iterator<?> iterator = response.contents().iterator(); iterator.hasNext(); ) {
                S3Object objectSummary = (S3Object) iterator.next();
                s3.deleteObject(DeleteObjectRequest.builder()
                                                   .bucket(bucketName)
                                                   .key(objectSummary.key())
                                                   .build());
            }

            if (response.isTruncated()) {
                response = s3.listObjects(ListObjectsRequest.builder()
                                                            .marker(response.nextMarker())
                                                            .bucket(bucketName)
                                                            .build());
            } else {
                break;
            }
        }
        ;

        s3.deleteBucket(DeleteBucketRequest.builder().bucket(bucketName).build());
    }

    /**
     * Creates a bucket and waits for it to exist.
     *
     * @param s3         The AmazonS# client to use.
     * @param bucketName The name of the bucket to create.
     */
    protected static void createBucket(S3Client s3, String bucketName, String region) throws InterruptedException {
        s3.createBucket(CreateBucketRequest.builder()
                                           .bucket(bucketName)
                                           .createBucketConfiguration(CreateBucketConfiguration.builder()
                                                                                               .locationConstraint(region)
                                                                                               .build())
                                           .build());

        Thread.sleep(1000);
    }

    protected static void maxPollTimeExceeded() {
        throw new RuntimeException("Max poll time exceeded");
    }

    /**
     * Asserts that the object stored in the specified bucket and key doesn't
     * exist If it does exist, this method will fail the current test.
     *
     * @param s3         The AmazonS3 client to use.
     * @param bucketName The name of the bucket containing the object to test.
     * @param key        The key under which the object is stored in the specified
     *                   bucket.
     */
    protected void assertObjectDoesntExist(S3Client s3, String bucketName, String key) throws Exception {
        long timeoutTime = System.currentTimeMillis() + 10000;

        while (true) {
            try {
                s3.headObject(HeadObjectRequest.builder().bucket(bucketName).key(key).build());
                Thread.sleep(1000);
                if (System.currentTimeMillis() > timeoutTime) {
                    fail("object " + bucketName + "/" + key + " still exists");
                }
            } catch (SdkServiceException exception) {
                /*
                 * We expect a 404 indicating that the object version we requested
                 * doesn't exist. If we get anything other than that, then we want
                 * to let the exception keep going up the chain.
                 */
                if (exception.statusCode() != 404) {
                    throw exception;
                }
                return; // doesn't exist!
            }
        }
    }

    /**
     * Asserts that the object stored in the specified bucket and key exists. If
     * it doesn't exist, this method will fail the current test.
     *
     * @param s3         The AmazonS3 client to use.
     * @param bucketName The name of the bucket containing the object to test.
     * @param key        The key under which the object is stored in the specified
     *                   bucket.
     */
    protected void assertObjectExists(S3Client s3, String bucketName, String key) throws Exception {
        long timeoutTime = System.currentTimeMillis() + 10000;

        while (true) {
            try {
                s3.headObject(HeadObjectRequest.builder().bucket(bucketName).key(key).build());
                return; // exists!
            } catch (SdkServiceException exception) {
                /*
                 * We expect a 404 indicating that the object version we requested
                 * doesn't exist. If we get anything other than that, then we want
                 * to let the exception keep going up the chain.
                 */
                if (exception.statusCode() != 404) {
                    throw exception;
                }
                Thread.sleep(1000);
                if (System.currentTimeMillis() > timeoutTime) {
                    fail("object " + bucketName + "/" + key + " doesn't exist");
                }
            }
        }
    }
}