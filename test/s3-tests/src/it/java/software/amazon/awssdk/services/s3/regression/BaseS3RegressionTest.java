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

package software.amazon.awssdk.services.s3.regression;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3control.S3ControlClient;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.utils.Logger;

public abstract class BaseS3RegressionTest {
    private static final Logger LOG = Logger.loggerFor(BaseS3RegressionTest.class);

    private static final String BUCKET_NAME_PREFIX = "do-not-delete-checksums-";
    private static final String MRAP_NAME = "do-not-delete-checksum-testing";
    private static final String AP_NAME = "do-not-delete-checksum-testing-ap";
    private static final String EOZ_SUFFIX = "--usw2-az3--x-s3";
    protected static final Region REGION = Region.US_WEST_2;

    protected static final String TEST_CREDENTIALS_PROFILE_NAME = "aws-test-account";
    protected static final AwsCredentialsProviderChain CREDENTIALS_PROVIDER_CHAIN =
        AwsCredentialsProviderChain.of(ProfileCredentialsProvider.builder()
                                                                 .profileName(TEST_CREDENTIALS_PROFILE_NAME)
                                                                 .build(),
                                       DefaultCredentialsProvider.create());

    protected static String accountId;
    protected static String bucketName;
    protected static String mrapArn;
    protected static String eozBucket;
    protected static String apArn;

    protected static S3ControlClient s3Control;
    protected static S3Client s3;
    protected static StsClient sts;

    private Map<BucketType, List<String>> bucketCleanup = new HashMap<>();

    @BeforeAll
    static void setup() throws InterruptedException, IOException {

        s3 = S3Client.builder()
                     .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                     .region(REGION)
                     .build();

        s3Control = S3ControlClient.builder()
                                   .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                                   .region(REGION)
                                   .build();

        sts = StsClient.builder().credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                       .region(REGION)
                       .build();

        accountId = S3ChecksumsTestUtils.getAccountId(sts);
        bucketName = S3ChecksumsTestUtils.createBucket(s3, getBucketName(), LOG);
        mrapArn = S3ChecksumsTestUtils.createMrap(s3Control, accountId, MRAP_NAME, bucketName, LOG);
        eozBucket = S3ChecksumsTestUtils.createEozBucket(s3, getBucketName() + EOZ_SUFFIX, LOG);
        apArn = S3ChecksumsTestUtils.createAccessPoint(s3Control, accountId, AP_NAME, bucketName);

        LOG.info(() -> "Using bucket: " + bucketName);

    }

    @AfterEach
    public void methodCleanup() {
        bucketCleanup.forEach((bt, keys) -> {
            String bucket = bucketForType(bt);
            keys.forEach(k -> {
                try {
                    s3.deleteObject(r -> r.bucket(bucket).key(k));
                } catch (Exception e) {
                    LOG.error(() -> String.format("Error in cleaning for bucket %s, key: %s: %s", bucket, k, e.getMessage()));
                }
            });
        });

        bucketCleanup.clear();
    }

    protected void recordObjectToCleanup(BucketType type, String key) {
        bucketCleanup.computeIfAbsent(type, k -> new ArrayList<>()).add(key);
    }

    protected static String getBucketName() {
        return BUCKET_NAME_PREFIX + accountId;
    }

    protected static String bucketForType(BucketType type) {
        switch (type) {
            case STANDARD_BUCKET:
                return bucketName;
            case MRAP:
                return mrapArn;
            case EOZ:
                return eozBucket;
            case ACCESS_POINT:
                return apArn;
            default:
                throw new RuntimeException("Unknown bucket type: " + type);
        }
    }

}
