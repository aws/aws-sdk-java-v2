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

import org.junit.BeforeClass;
import software.amazon.awssdk.core.ClientType;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.BucketLocationConstraint;
import software.amazon.awssdk.services.s3.model.CreateBucketConfiguration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.utils.S3TestUtils;
import software.amazon.awssdk.testutils.service.AwsTestBase;

/**
 * Base class for S3 integration tests. Loads AWS credentials from a properties
 * file and creates an S3 client for callers to use.
 */
public class S3IntegrationTestBase extends AwsTestBase {

    protected static final Region DEFAULT_REGION = Region.US_WEST_2;
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
                       .region(DEFAULT_REGION)
                       .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                       .overrideConfiguration(o -> o.addExecutionInterceptor(
                           new UserAgentVerifyingExecutionInterceptor("Apache", ClientType.SYNC)));
    }

    protected static S3AsyncClientBuilder s3AsyncClientBuilder() {
        return S3AsyncClient.builder()
                            .region(DEFAULT_REGION)
                            .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                            .overrideConfiguration(o -> o.addExecutionInterceptor(
                                new UserAgentVerifyingExecutionInterceptor("NettyNio", ClientType.ASYNC)));
    }

    protected static void createBucket(String bucketName) {
        createBucket(bucketName, 0);
    }

    private static void createBucket(String bucketName, int retryCount) {
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
            System.err.println("Error attempting to create bucket: " + bucketName);
            if (e.awsErrorDetails().errorCode().equals("BucketAlreadyOwnedByYou")) {
                System.err.printf("%s bucket already exists, likely leaked by a previous run\n", bucketName);
            } else if (e.awsErrorDetails().errorCode().equals("TooManyBuckets")) {
                System.err.println("Printing all buckets for debug:");
                s3.listBuckets().buckets().forEach(System.err::println);
                if (retryCount < 2) {
                    System.err.println("Retrying...");
                    createBucket(bucketName, retryCount + 1);
                } else {
                    throw e;
                }
            } else {
                throw e;
            }
        }
    }

    protected static void deleteBucketAndAllContents(String bucketName) {
        S3TestUtils.deleteBucketAndAllContents(s3, bucketName);
    }

    private static class UserAgentVerifyingExecutionInterceptor implements ExecutionInterceptor {

        private final String clientName;
        private final ClientType clientType;

        public UserAgentVerifyingExecutionInterceptor(String clientName, ClientType clientType) {
            this.clientName = clientName;
            this.clientType = clientType;
        }

        @Override
        public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
            assertThat(context.httpRequest().firstMatchingHeader("User-Agent").get()).containsIgnoringCase("io/" + clientType.name());
            assertThat(context.httpRequest().firstMatchingHeader("User-Agent").get()).containsIgnoringCase("http/" + clientName);
        }
    }
}
