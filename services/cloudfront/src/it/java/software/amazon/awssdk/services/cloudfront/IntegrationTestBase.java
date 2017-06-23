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

package software.amazon.awssdk.services.cloudfront;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import org.junit.BeforeClass;
import software.amazon.awssdk.auth.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudfront.model.GetDistributionRequest;
import software.amazon.awssdk.services.cloudfront.model.GetDistributionResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.test.AwsTestBase;

/**
 * Base class for CloudFront integration tests.
 */
public abstract class IntegrationTestBase extends AwsTestBase {

    /**
     * Shared CloudFront client for all tests to use.
     */
    protected static CloudFrontClient cloudfront;

    /**
     * Shared S3 client for all tests to use.
     */
    protected static S3Client s3;

    /**
     * Loads the AWS account info for the integration tests and creates an
     * AutoScaling client for tests to use.
     */
    @BeforeClass
    public static void setUp() throws FileNotFoundException, IOException {
        setUpCredentials();
        cloudfront = CloudFrontClient.builder()
                                     .credentialsProvider(new StaticCredentialsProvider(credentials))
                                     .region(Region.AWS_GLOBAL)
                                     .build();
        s3 = S3Client.builder().credentialsProvider(new StaticCredentialsProvider(credentials)).region(Region.AWS_GLOBAL).build();
    }

    /**
     * Polls the test distribution until it moves into the "Deployed" state, or
     * throws an exception and gives up after waiting too long.
     *
     * @param distributionId The distribution to delete
     */
    protected static void waitForDistributionToDeploy(String distributionId) throws Exception {
        int timeoutInMinutes = 20;
        long startTime = System.currentTimeMillis();
        while (true) {
            GetDistributionResponse getDistributionResult = cloudfront.getDistribution(GetDistributionRequest.builder()
                                                                                                           .id(distributionId)
                                                                                                           .build());
            String status = getDistributionResult.distribution().status();
            System.out.println(status);
            if (status.equalsIgnoreCase("Deployed")) {
                return;
            }

            if ((System.currentTimeMillis() - startTime) > (1000 * 60 * timeoutInMinutes)) {
                throw new RuntimeException("Waited " + timeoutInMinutes
                                           + " minutes for distribution to be deployed, but never happened");
            }

            Thread.sleep(1000 * 20);
        }
    }

    /**
     * Deletes all objects in the specified bucket, and then deletes the bucket.
     *
     * @param bucketName The bucket to empty and delete.
     */
    protected static void deleteBucketAndAllContents(String bucketName) {
        ListObjectsResponse listObjectResponse = s3.listObjects(ListObjectsRequest.builder().bucket(bucketName).build());
        List<S3Object> objectListing = listObjectResponse.contents();

        while (true) {
            for (S3Object objectSummary : objectListing) {
                s3.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(objectSummary.key()).build());
            }

            if (listObjectResponse.isTruncated()) {
                listObjectResponse = s3.listObjects(ListObjectsRequest.builder()
                                                                 .bucket(bucketName)
                                                                 .marker(listObjectResponse.marker())
                                                                 .build());
            } else {
                break;
            }
        }
        ;

        s3.deleteBucket(DeleteBucketRequest.builder().bucket(bucketName).build());
    }
}