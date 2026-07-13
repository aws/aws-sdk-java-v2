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
package com.amazonaws.services.dynamodbv2.datamodeling;

import static org.junit.Assert.fail;

import com.amazonaws.waiters.WaiterParameters;
import java.util.Iterator;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.amazonaws.dynamodbv2.test.util.DynamoDBIntegrationTestBase;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GetObjectMetadataRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;

public class DynamoDBS3IntegrationTestBase extends DynamoDBIntegrationTestBase {
    public static final String WEST_BUCKET = "java-dynamo-s3-integ-test-west-" + System.currentTimeMillis();
    public static final String EAST_BUCKET = "java-dynamo-s3-integ-test-east-" + System.currentTimeMillis();

    protected static AmazonS3Client s3East;
    protected static AmazonS3Client s3West;

    @BeforeClass
    public static void setUp() throws Exception {
        DynamoDBIntegrationTestBase.setUp();
        s3East = new AmazonS3Client(credentials);
        s3East.setRegion(Region.getRegion(Regions.US_EAST_1));
        s3West = new AmazonS3Client(credentials);
        s3West.setRegion(Region.getRegion(Regions.US_WEST_2));

        createBucket(s3East, EAST_BUCKET);
        createBucket(s3West, WEST_BUCKET);
    }

    @AfterClass
    public static void tearDown() {
        deleteBucketAndAllContents(s3East, EAST_BUCKET);
        deleteBucketAndAllContents(s3West, WEST_BUCKET);
    }

    /**
     * Deletes all objects in the specified bucket, and then deletes the bucket.
     *
     * @param s3
     *            The AmazonS3 client to use.
     * @param bucketName
     *            The bucket to empty and delete.
     */
    protected static void deleteBucketAndAllContents(AmazonS3 s3, String bucketName) {
        ObjectListing objectListing = s3.listObjects(bucketName);

        while (true) {
            for ( Iterator<?> iterator = objectListing.getObjectSummaries().iterator(); iterator.hasNext(); ) {
                S3ObjectSummary objectSummary = (S3ObjectSummary) iterator.next();
                s3.deleteObject(bucketName, objectSummary.getKey());
            }

            if (objectListing.isTruncated()) {
                objectListing = s3.listNextBatchOfObjects(objectListing);
            } else {
                break;
            }
        };

        s3.deleteBucket(bucketName);
    }

    /**
     * Asserts that the object stored in the specified bucket and key doesn't
     * exist If it does exist, this method will fail the current test.
     *
     * @param s3
     *            The AmazonS3 client to use.
     * @param bucketName
     *            The name of the bucket containing the object to test.
     * @param key
     *            The key under which the object is stored in the specified
     *            bucket.
     */
    protected void assertObjectDoesntExist(AmazonS3 s3, String bucketName, String key) throws Exception {
        long timeoutTime = System.currentTimeMillis() + 10000;

        while (true) {
            try {
                s3.getObjectMetadata(new GetObjectMetadataRequest(bucketName, key));
                Thread.sleep(1000);
                if (System.currentTimeMillis() > timeoutTime) {
                    fail("object " + bucketName + "/" + key + " still exists");
                }
            } catch (AmazonS3Exception ase) {
                /*
                 * We expect a 404 indicating that the object version we requested
                 * doesn't exist. If we get anything other than that, then we want
                 * to let the exception keep going up the chain.
                 */
                if (ase.getStatusCode() != 404) throw ase;
                return; // doesn't exist!
            }
        }
    }

    /**
     * Asserts that the object stored in the specified bucket and key exists. If
     * it doesn't exist, this method will fail the current test.
     *
     * @param s3
     *            The AmazonS3 client to use.
     * @param bucketName
     *            The name of the bucket containing the object to test.
     * @param key
     *            The key under which the object is stored in the specified
     *            bucket.
     */
    protected void assertObjectExists(AmazonS3 s3, String bucketName, String key) throws Exception {
        s3.waiters().objectExists().run(new WaiterParameters<GetObjectMetadataRequest>(new GetObjectMetadataRequest(bucketName, key)));
    }

    /**
     * Creates a bucket and waits for it to exist.
     *
     * @param s3
     *            The AmazonS# client to use.
     * @param bucketName
     *            The name of the bucket to create.
     */
    protected static void createBucket(AmazonS3 s3, String bucketName) throws InterruptedException {
        try {
            s3.createBucket(bucketName);
        } catch (AmazonS3Exception e) {
            if (!e.getErrorCode().equals("BucketAlreadyOwnedByYou")) {
                throw e;
            }
        }

        int poll = 0;
        while ( !s3.doesBucketExist(bucketName) && poll++ < 60 ) {
            Thread.sleep(1000);
        }
        if ( poll >= 60 * 5 ) {
            maxPollTimeExceeded();
        }
    }

    protected static void maxPollTimeExceeded() {
        throw new RuntimeException("Max poll time exceeded");
    }
}
