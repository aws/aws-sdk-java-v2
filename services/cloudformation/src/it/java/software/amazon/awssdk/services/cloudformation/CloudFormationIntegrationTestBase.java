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

package software.amazon.awssdk.services.cloudformation;

import static software.amazon.awssdk.testutils.service.S3BucketUtils.temporaryBucketName;

import java.io.File;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import software.amazon.awssdk.core.regions.Region;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.testutils.service.AwsTestBase;

/**
 * Base class for CloudFormation integration tests. Loads AWS credentials from a properties file and
 * creates a client for callers to use.
 */
public class CloudFormationIntegrationTestBase extends AwsTestBase {

    protected static CloudFormationClient cf;
    protected static String bucketName = temporaryBucketName("cloudformation-templates");
    protected static String templateForCloudFormationIntegrationTests = "templateForCloudFormationIntegrationTests";
    protected static String templateForStackIntegrationTests = "templateForStackIntegrationTests";
    protected static String templateUrlForCloudFormationIntegrationTests = "https://s3.amazonaws.com/" + bucketName
                                                                           + "/" + templateForCloudFormationIntegrationTests;
    protected static String templateUrlForStackIntegrationTests = "https://s3.amazonaws.com/" + bucketName + "/"
                                                                  + templateForStackIntegrationTests;
    protected static S3Client s3;

    /**
     * Loads the AWS account info for the integration tests and creates an S3 client for tests to
     * use.
     */
    @BeforeClass
    public static void setUp() throws Exception {
        setUpCredentials();
        cf = CloudFormationClient.builder()
                                 .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                                 .region(Region.AP_NORTHEAST_1)
                                 .build();
        s3 = S3Client.builder().credentialsProvider(CREDENTIALS_PROVIDER_CHAIN).region(Region.AP_NORTHEAST_1).build();

        s3.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());

        s3.putObject(PutObjectRequest.builder()
                                     .bucket(bucketName)
                                     .key(templateForCloudFormationIntegrationTests)
                                     .build(),
                     RequestBody.of(new File("tst/" + templateForCloudFormationIntegrationTests)));

        s3.putObject(PutObjectRequest.builder()
                                     .bucket(bucketName)
                                     .key(templateForStackIntegrationTests)
                                     .build(),
                     RequestBody.of(new File("tst/" + templateForStackIntegrationTests)));
    }

    @AfterClass
    public static void tearDown() {
        deleteBucketAndAllContents(bucketName);
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