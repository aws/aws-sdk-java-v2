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

import org.junit.BeforeClass;
import software.amazon.awssdk.core.regions.Region;
import software.amazon.awssdk.services.s3.model.BucketLocationConstraint;
import software.amazon.awssdk.services.s3.model.CreateBucketConfiguration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.testutils.service.AwsTestBase;
import utils.BucketNamingStrategy;
import utils.S3TestUtils;

/**
 * Base class for S3 integration tests. Loads AWS credentials from a properties
 * file and creates an S3 client for callers to use.
 */
public class S3IntegrationTestBase extends AwsTestBase {

    private static final BucketNamingStrategy BUCKET_NAMING_STRATEGY = new BucketNamingStrategy();

    /**
     * The S3 client for all tests to use.
     */
    protected static S3Client s3;

    protected static S3AsyncClient s3Async;

    /**
     * Loads the AWS account info for the integration tests and creates an S3
     * client for tests to use.
     */
    @BeforeClass
    public static void setUp() throws Exception {
        s3 = s3ClientBuilder().build();
        s3Async = s3AsyncClientBuilder().build();
    }

    protected static S3ClientBuilder s3ClientBuilder() {
        return S3Client.builder()
                       .region(Region.US_WEST_2)
                       .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN);
    }

    protected static S3AsyncClientBuilder s3AsyncClientBuilder() {
        return S3AsyncClient.builder()
                            .region(Region.US_WEST_2)
                            .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN);
    }

    protected static void createBucket(String bucketName) {
        try {
            s3.createBucket(
                    CreateBucketRequest.builder()
                                       .bucket(bucketName)
                                       .createBucketConfiguration(
                                               CreateBucketConfiguration.builder()
                                                                        .locationConstraint(BucketLocationConstraint.US_WEST_2)
                                                                        .build())
                                       .build());
        } catch (S3Exception e) {
            if (e.getErrorCode().equals("BucketAlreadyOwnedByYou")) {
                System.err.printf("%s bucket already exists, likely leaked by a previous run\n", bucketName);
            } else {
                throw e;
            }
        }
    }

    protected static void deleteBucketAndAllContents(String bucketName) {
        S3TestUtils.deleteBucketAndAllContents(s3, bucketName);
    }

    protected static String getBucketName(Class<?> testClass) {
        return BUCKET_NAMING_STRATEGY.getBucketName(testClass);
    }

    protected String getBucketName() {
        return BUCKET_NAMING_STRATEGY.getBucketName(getClass());
    }

}
