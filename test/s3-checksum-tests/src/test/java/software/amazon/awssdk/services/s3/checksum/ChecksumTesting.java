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

package software.amazon.awssdk.services.s3.checksum;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.awscore.AwsClient;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketLocationConstraint;
import software.amazon.awssdk.services.s3.model.CreateBucketConfiguration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DataRedundancy;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.GlacierJobParameters;
import software.amazon.awssdk.services.s3.model.LocationInfo;
import software.amazon.awssdk.services.s3.model.LocationType;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.RestoreObjectRequest;
import software.amazon.awssdk.services.s3.model.RestoreRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.StorageClass;
import software.amazon.awssdk.services.s3.model.Tier;
import software.amazon.awssdk.services.s3control.S3ControlClient;
import software.amazon.awssdk.services.s3control.model.CreateMultiRegionAccessPointRequest;
import software.amazon.awssdk.services.s3control.model.GetMultiRegionAccessPointResponse;
import software.amazon.awssdk.services.s3control.model.MultiRegionAccessPointStatus;
import software.amazon.awssdk.services.s3control.model.S3ControlException;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.Logger;

public class ChecksumTesting {
    private static final String BUCKET_NAME_PREFIX = "do-not-delete-checksums-";
    private static final String MRAP_NAME = "do-not-delete-checksum-testing";
    private static final String AP_NAME = "do-not-delete-checksum-testing-ap";
    private static final String EOZ_SUFFIX = "--usw2-az3--x-s3";

    private static final Logger LOG = Logger.loggerFor(ChecksumTesting.class);
    private static final Region REGION = Region.US_WEST_2;
    private static final String TEST_CREDENTIALS_PROFILE_NAME = "aws-test-account";


    public static final AwsCredentialsProviderChain CREDENTIALS_PROVIDER_CHAIN =
        AwsCredentialsProviderChain.of(ProfileCredentialsProvider.builder()
                                                                 .profileName(TEST_CREDENTIALS_PROFILE_NAME)
                                                                 .build(),
                                       DefaultCredentialsProvider.create());

    private static String accountId;
    private static String bucketName;
    private static String mrapArn;
    private static String eozBucket;
    private static String apArn;

    private static S3ControlClient s3Control;
    private static S3Client s3;
    private static StsClient sts;

    private Map<BucketType, List<String>> bucketCleanup = new HashMap<>();

    @BeforeAll
    static void setup() throws InterruptedException {
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

        accountId = getAccountId();

        bucketName = createBucket();

        mrapArn = createMrap();

        eozBucket = createEozBucket();

        apArn = createAccessPoint();
    }

    @AfterEach
    public void methodCleanup() {
        bucketCleanup.forEach((bt, keys) -> {
            String bucket = bucketForType(bt);
            keys.forEach(k -> s3.deleteObject(r -> r.bucket(bucket).key(k)));
        });

        bucketCleanup.clear();
    }

    private void assumeNotAccessPointWithPathStyle(TestConfig config) {
        BucketType bucketType = config.getBucketType();
        boolean isAccessPoint = BucketType.ACCESS_POINT == bucketType || BucketType.MRAP == bucketType;
        Assumptions.assumeFalse(config.isForcePathStyle() && isAccessPoint,
                                "Path style doesn't work with ARN type buckets");
    }

    // Request checksum required
    @ParameterizedTest
    @MethodSource("testConfigs")
    void deleteObject(TestConfig config) {
        assumeNotAccessPointWithPathStyle(config);

        String bucket = bucketForType(config.getBucketType());
        String key = putRandomObject(config.getBucketType());
        TestCallable callable = null;
        try {
            DeleteObjectsRequest req = DeleteObjectsRequest.builder()
                                                           .bucket(bucket)
                                                           .delete(Delete.builder()
                                                                         .objects(ObjectIdentifier.builder()
                                                                                                  .key(key)
                                                                                                  .build())
                                                                         .build())
                                                           .build();

            callable = callDeleteObjects(req, config);
            callable.runnable.run();
        } finally {
            if (callable != null) {
                callable.client.close();
            }
        }
    }

    // Request checksum optional
    @ParameterizedTest
    @MethodSource("testConfigs")
    void restoreObject(TestConfig config) {
        assumeNotAccessPointWithPathStyle(config);

        Assumptions.assumeFalse(config.getBucketType() == BucketType.EOZ,
                                "Restore is not supported for S3 Express");

        String bucket = bucketForType(config.getBucketType());
        String key = putRandomArchivedObject(config.getBucketType());
        TestCallable callable = null;
        try {
            RestoreObjectRequest request = RestoreObjectRequest.builder()
                                                               .bucket(bucket)
                                                               .key(key)
                                                               .restoreRequest(RestoreRequest.builder()
                                                                                             .days(5)
                                                                                             .glacierJobParameters(GlacierJobParameters.builder()
                                                                                                                                       .tier(Tier.STANDARD)
                                                                                                                                       .build())
                                                                                             .build())
                                                               .build();

            callable = callRestoreObject(request, config);
            callable.runnable.run();
        } finally {
            callable.client.close();
        }
    }

    private TestCallable callDeleteObjects(DeleteObjectsRequest request, TestConfig config) {
        AwsClient toClose;
        Runnable runnable = null;

        if (config.getFlavor().isAsync()) {
            S3AsyncClient s3Async = makeAsyncClient(config);
            toClose = s3Async;
            runnable = () -> {s3Async.deleteObjects(request).join();};
        } else {
            S3Client s3 = makeSyncClient(config);
            toClose = s3;
            runnable = () -> {s3.deleteObjects(request);};
        }

        return new TestCallable(toClose, runnable);
    }

    private TestCallable callRestoreObject(RestoreObjectRequest request, TestConfig config) {
        AwsClient toClose;
        Runnable runnable = null;

        if (config.getFlavor().isAsync()) {
            S3AsyncClient s3Async = makeAsyncClient(config);
            toClose = s3Async;
            runnable = () -> {s3Async.restoreObject(request).join();};
        } else {
            S3Client s3 = makeSyncClient(config);
            toClose = s3;
            runnable = () -> {s3.restoreObject(request);};
        }

        return new TestCallable(toClose, runnable);
    }

    private static class TestCallable {
        private AwsClient client;
        private Runnable runnable;

        TestCallable(AwsClient client, Runnable runnable) {
            this.client = client;
            this.runnable = runnable;
        }
    }

    private S3Client makeSyncClient(TestConfig config) {
        switch (config.getFlavor()) {
            case JAVA_BASED:
                return S3Client.builder()
                    .forcePathStyle(config.isForcePathStyle())
                    .requestChecksumCalculation(config.getRequestChecksumValidation())
                    .region(REGION)
                    .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                    .build();
            default:
                throw new RuntimeException("Unsupported sync flavor: " + config.getFlavor());
        }
    }

    private S3AsyncClient makeAsyncClient(TestConfig config) {
        switch (config.getFlavor()) {
            case ASYNC_JAVA_BASED:
                return S3AsyncClient.builder()
                    .forcePathStyle(config.isForcePathStyle())
                    .requestChecksumCalculation(config.getRequestChecksumValidation())
                    .region(REGION)
                    .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                    .build();
            case ASYNC_CRT:
                return S3AsyncClient.crtBuilder()
                    .forcePathStyle(config.isForcePathStyle())
                    .requestChecksumCalculation(config.getRequestChecksumValidation())
                    .region(REGION)
                    .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                    .build();
            default:
                throw new RuntimeException("Unsupported async flavor: " + config.getFlavor());
        }
    }

    private static String bucketForType(BucketType type) {
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

    enum BucketType {
        STANDARD_BUCKET,
        ACCESS_POINT,
        // Multi-region access point
        MRAP,
        // Express one zone/S3 express
        EOZ,
    }

    enum S3ClientFlavor {
        JAVA_BASED(false),
        ASYNC_JAVA_BASED(true),

        ASYNC_CRT(true)
        ;

        private boolean async;

        private S3ClientFlavor(boolean async) {
            this.async = async;
        }

        public boolean isAsync() {
            return async;
        }
    }

    static class TestConfig {
        private S3ClientFlavor flavor;
        private BucketType bucketType;
        private boolean forcePathStyle;
        private RequestChecksumCalculation requestChecksumValidation;

        public S3ClientFlavor getFlavor() {
            return flavor;
        }

        public void setFlavor(S3ClientFlavor flavor) {
            this.flavor = flavor;
        }

        public BucketType getBucketType() {
            return bucketType;
        }

        public void setBucketType(BucketType bucketType) {
            this.bucketType = bucketType;
        }

        public boolean isForcePathStyle() {
            return forcePathStyle;
        }

        public void setForcePathStyle(boolean forcePathStyle) {
            this.forcePathStyle = forcePathStyle;
        }

        public RequestChecksumCalculation getRequestChecksumValidation() {
            return requestChecksumValidation;
        }

        public void setRequestChecksumValidation(RequestChecksumCalculation requestChecksumValidation) {
            this.requestChecksumValidation = requestChecksumValidation;
        }

        @Override
        public String toString() {
            return "[" +
                   "flavor=" + flavor +
                   ", bucketType=" + bucketType +
                   ", forcePathStyle=" + forcePathStyle +
                   ", requestChecksumValidation=" + requestChecksumValidation +
                   ']';
        }
    }

    static List<TestConfig> testConfigs() {
        List<TestConfig> configs = new ArrayList<>();

        boolean[] forcePathStyle = {true, false};
        RequestChecksumCalculation[] checksumValidations = {RequestChecksumCalculation.WHEN_REQUIRED,
                                                            RequestChecksumCalculation.WHEN_SUPPORTED};

        for (boolean pathStyle : forcePathStyle) {
            for (RequestChecksumCalculation checksumValidation : checksumValidations) {
                for (S3ClientFlavor flavor : S3ClientFlavor.values()) {
                    for (BucketType bucketType : BucketType.values()) {
                        TestConfig testConfig = new TestConfig();
                        testConfig.setFlavor(flavor);
                        testConfig.setBucketType(bucketType);
                        testConfig.setForcePathStyle(pathStyle);
                        testConfig.setRequestChecksumValidation(checksumValidation);
                        configs.add(testConfig);
                    }
                }
            }
        }

        return configs;
    }

    private String putRandomObject(BucketType bucketType) {
        String key = randomKey();
        String bucketName = bucketForType(bucketType);
        s3.putObject(r -> r.bucket(bucketName).key(key), RequestBody.fromString("hello"));
        bucketCleanup.computeIfAbsent(bucketType, k -> new ArrayList<>()).add(key);
        return key;
    }


    private String putRandomArchivedObject(BucketType bucketType) {
        String key = randomKey();
        String bucketName = bucketForType(bucketType);
        s3.putObject(r -> r.bucket(bucketName).key(key).storageClass(StorageClass.GLACIER), RequestBody.fromString("hello"));
        bucketCleanup.computeIfAbsent(bucketType, k -> new ArrayList<>()).add(key);
        return key;
    }

    private String randomKey() {
        return BinaryUtils.toHex(UUID.randomUUID().toString().getBytes());
    }

    private static String getAccountId() {
        return sts.getCallerIdentity().account();
    }

    private static String getBucketName() {
        return BUCKET_NAME_PREFIX + accountId;
    }

    private static String createAccessPoint() {
        try {
            s3Control.getAccessPoint(r -> r.accountId(accountId).name(AP_NAME));
        } catch (S3ControlException e) {
            if (e.awsErrorDetails().sdkHttpResponse().statusCode() != 404) {
                throw e;
            }

            s3Control.createAccessPoint(r -> r.bucket(bucketName).name(AP_NAME).accountId(accountId));
        }

        return waitForApToBeReady();
    }

    private static String createMrap() throws InterruptedException {
        try {
            s3Control.getMultiRegionAccessPoint(r -> r.accountId(accountId).name(MRAP_NAME));
        } catch (S3ControlException e) {
            if (e.awsErrorDetails().sdkHttpResponse().statusCode() != 404) {
                throw e;
            }

            CreateMultiRegionAccessPointRequest createMrap =
                CreateMultiRegionAccessPointRequest.builder()
                                                   .accountId(accountId)
                                                   .details(d -> d.name(MRAP_NAME)
                                                                  .regions(software.amazon.awssdk.services.s3control.model.Region.builder()
                                                                                                                                 .bucket(bucketName)
                                                                                                                                 .build()))
                                                   .build();

            s3Control.createMultiRegionAccessPoint(createMrap);
        }

        return waitForMrapToBeReady();
    }


    private static String createBucket() {
        String name = getBucketName();
        LOG.debug(() -> "Creating bucket: " + name);
        createBucket(name, 3);
        return name;
    }

    private static String createEozBucket() {
        String eozBucketName = getBucketName() + EOZ_SUFFIX;
        LOG.debug(() -> "Creating EOZ bucket: " + eozBucketName);
        CreateBucketConfiguration cfg = CreateBucketConfiguration.builder()
                                                                 .bucket(info -> info.dataRedundancy(DataRedundancy.SINGLE_AVAILABILITY_ZONE)
                                                                                     .type(software.amazon.awssdk.services.s3.model.BucketType.DIRECTORY))
                                                                 .location(LocationInfo.builder()
                                                                                       .name("usw2-az3")
                                                                                       .type(LocationType.AVAILABILITY_ZONE)
                                                                                       .build())
                                                                 .build();

        try {
            s3.createBucket(r -> r.bucket(eozBucketName).createBucketConfiguration(cfg));
        } catch (S3Exception e) {
            AwsErrorDetails awsErrorDetails = e.awsErrorDetails();
            if (!"BucketAlreadyOwnedByYou".equals(awsErrorDetails.errorCode())) {
                throw e;
            }
        }
        return eozBucketName;
    }

    private static String waitForMrapToBeReady() throws InterruptedException {
        GetMultiRegionAccessPointResponse getMrapResponse = null;

        Instant waitStart = Instant.now();
        boolean initial = true;
        do {
            if (!initial) {
                Thread.sleep(Duration.ofSeconds(10).toMillis());
                initial = true;
            }
            GetMultiRegionAccessPointResponse response = s3Control.getMultiRegionAccessPoint(r -> r.accountId(accountId).name(MRAP_NAME));
            LOG.debug(() -> "Wait response: " + response);
            getMrapResponse = response;
        } while (MultiRegionAccessPointStatus.READY != getMrapResponse.accessPoint().status()
                 && Duration.between(Instant.now(), waitStart).compareTo(Duration.ofMinutes(5)) < 0);

        return "arn:aws:s3::" + accountId + ":accesspoint/" + getMrapResponse.accessPoint().alias();
    }

    private static String waitForApToBeReady() {
        return s3Control.getAccessPoint(r -> r.accountId(accountId).name(AP_NAME)).accessPointArn();
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
            LOG.debug(() -> "Error attempting to create bucket: " + bucketName);
            if (e.awsErrorDetails().errorCode().equals("BucketAlreadyOwnedByYou")) {
                LOG.debug(() -> String.format("%s bucket already exists, likely leaked by a previous run%n", bucketName));
            } else if (e.awsErrorDetails().errorCode().equals("TooManyBuckets")) {
                LOG.debug(() -> "Printing all buckets for debug:");
                s3.listBuckets().buckets().forEach(l -> LOG.debug(l::toString));
                if (retryCount < 2) {
                    LOG.debug(() -> "Retrying...");
                    createBucket(bucketName, retryCount + 1);
                } else {
                    throw e;
                }
            } else {
                throw e;
            }
        }

        s3.waiter().waitUntilBucketExists(r -> r.bucket(bucketName));
    }
}
