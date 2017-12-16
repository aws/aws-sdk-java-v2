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

package software.amazon.awssdk.services.cloudtrail;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static software.amazon.awssdk.testutils.service.S3BucketUtils.temporaryBucketName;

import java.io.IOException;
import java.util.Iterator;
import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.services.cloudtrail.model.CreateTrailRequest;
import software.amazon.awssdk.services.cloudtrail.model.CreateTrailResponse;
import software.amazon.awssdk.services.cloudtrail.model.DeleteTrailRequest;
import software.amazon.awssdk.services.cloudtrail.model.DescribeTrailsRequest;
import software.amazon.awssdk.services.cloudtrail.model.DescribeTrailsResponse;
import software.amazon.awssdk.services.cloudtrail.model.StartLoggingRequest;
import software.amazon.awssdk.services.cloudtrail.model.StopLoggingRequest;
import software.amazon.awssdk.services.cloudtrail.model.Trail;
import software.amazon.awssdk.services.cloudtrail.model.UpdateTrailRequest;
import software.amazon.awssdk.services.cloudtrail.model.UpdateTrailResponse;
import software.amazon.awssdk.services.s3.model.CreateBucketConfiguration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectVersionsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectVersionsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.ObjectVersion;
import software.amazon.awssdk.services.s3.model.PutBucketPolicyRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

public class CloudTrailIntegrationTest extends IntegrationTestBase {
    private static final String BUCKET_NAME = temporaryBucketName("aws-java-cloudtrail-integ");
    private static final String TRAIL_NAME = "aws-java-trail-" + System.currentTimeMillis();
    /**
     * Path to the sample policy for this test
     */
    private static final String POLICY_FILE = "/software/amazon/awssdk/services/cloudtrail/samplePolicy.json";

    @BeforeClass
    public static void setUp() throws IOException {
        IntegrationTestBase.setUp();
        s3.createBucket(CreateBucketRequest.builder()
                                           .bucket(BUCKET_NAME)
                                           .createBucketConfiguration(
                                                   CreateBucketConfiguration.builder()
                                                                            .locationConstraint(region.value())
                                                                            .build())
                                           .build());
    }

    @AfterClass
    public static void tearDown() {
        deleteBucketAndAllContents(BUCKET_NAME);

        try {
            for (Trail trail : cloudTrail.describeTrails(DescribeTrailsRequest.builder().build()).trailList()) {
                cloudTrail.deleteTrail(DeleteTrailRequest.builder().name(trail.name()).build());
            }
        } catch (Exception e) {
            // Expected.
        }
    }

    public static void deleteBucketAndAllContents(String bucketName) {
        System.out.println("Deleting S3 bucket: " + bucketName);
        ListObjectsResponse response = s3.listObjects(ListObjectsRequest.builder().bucket(bucketName).build());

        while (true) {
            if (response.contents() == null) {
                break;
            }
            for (Iterator<?> iterator = response.contents().iterator(); iterator
                    .hasNext(); ) {
                S3Object objectSummary = (S3Object) iterator.next();
                s3.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(objectSummary.key()).build());
            }

            if (response.isTruncated()) {
                response = s3.listObjects(ListObjectsRequest.builder().marker(response.nextMarker()).build());
            } else {
                break;
            }
        }

        ListObjectVersionsResponse versionsResponse = s3
                .listObjectVersions(ListObjectVersionsRequest.builder().bucket(bucketName).build());
        if (versionsResponse.versions() != null) {
            for (ObjectVersion s : versionsResponse.versions()) {
                s3.deleteObject(DeleteObjectRequest.builder()
                                                   .bucket(bucketName)
                                                   .key(s.key())
                                                   .versionId(s.versionId())
                                                   .build());
            }
        }

        s3.deleteBucket(DeleteBucketRequest.builder().bucket(bucketName).build());
    }

    @Test
    public void testServiceOperations() throws IOException, InterruptedException {
        String policyText = IOUtils.toString(getClass().getResourceAsStream(POLICY_FILE));
        policyText = policyText.replace("@BUCKET_NAME@", BUCKET_NAME);
        System.out.println(policyText);
        s3.putBucketPolicy(PutBucketPolicyRequest.builder().bucket(BUCKET_NAME).policy(policyText).build());

        Thread.sleep(1000 * 5);

        // create trail
        CreateTrailResponse createTrailResult =
                cloudTrail.createTrail(CreateTrailRequest.builder()
                                                         .name(TRAIL_NAME)
                                                         .s3BucketName(BUCKET_NAME)
                                                         .includeGlobalServiceEvents(true)
                                                         .build());

        assertEquals(TRAIL_NAME, createTrailResult.name());
        assertEquals(BUCKET_NAME, createTrailResult.s3BucketName());
        assertNull(createTrailResult.s3KeyPrefix());
        assertTrue(createTrailResult.includeGlobalServiceEvents());

        // describe trail
        DescribeTrailsResponse describeTrails = cloudTrail.describeTrails(DescribeTrailsRequest.builder().build());
        assertTrue(describeTrails.trailList().size() > 0);

        describeTrails = cloudTrail
                .describeTrails(DescribeTrailsRequest.builder().trailNameList(TRAIL_NAME).build());
        assertTrue(describeTrails.trailList().size() == 1);
        Trail trail = describeTrails.trailList().get(0);

        assertEquals(TRAIL_NAME, trail.name());
        assertEquals(BUCKET_NAME, trail.s3BucketName());
        assertNull(trail.s3KeyPrefix());
        assertTrue(trail.includeGlobalServiceEvents());

        // update the trail
        UpdateTrailResponse updateTrailResult =
                cloudTrail.updateTrail(UpdateTrailRequest.builder()
                                                         .name(TRAIL_NAME)
                                                         .s3BucketName(BUCKET_NAME)
                                                         .includeGlobalServiceEvents(false)
                                                         .s3KeyPrefix("123")
                                                         .build());

        assertEquals(TRAIL_NAME, updateTrailResult.name());
        assertEquals(BUCKET_NAME, updateTrailResult.s3BucketName());
        assertEquals("123", updateTrailResult.s3KeyPrefix());
        assertFalse(updateTrailResult.includeGlobalServiceEvents());

        // start and stop the logging
        cloudTrail.startLogging(StartLoggingRequest.builder().name(TRAIL_NAME).build());
        cloudTrail.stopLogging(StopLoggingRequest.builder().name(TRAIL_NAME).build());

        // delete the trail
        cloudTrail.deleteTrail(DeleteTrailRequest.builder().name(TRAIL_NAME).build());

        // try to get the deleted trail
        DescribeTrailsResponse describeTrailResult = cloudTrail
                .describeTrails(DescribeTrailsRequest.builder().trailNameList(TRAIL_NAME).build());
        assertEquals(0, describeTrailResult.trailList().size());
    }
}
