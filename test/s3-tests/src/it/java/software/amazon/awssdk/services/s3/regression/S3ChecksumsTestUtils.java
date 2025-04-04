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
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.Assumptions;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.checksums.DefaultChecksumAlgorithm;
import software.amazon.awssdk.checksums.SdkChecksum;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketAccelerateStatus;
import software.amazon.awssdk.services.s3.model.BucketLocationConstraint;
import software.amazon.awssdk.services.s3.model.CreateBucketConfiguration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DataRedundancy;
import software.amazon.awssdk.services.s3.model.LocationInfo;
import software.amazon.awssdk.services.s3.model.LocationType;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3control.S3ControlClient;
import software.amazon.awssdk.services.s3control.model.CreateMultiRegionAccessPointRequest;
import software.amazon.awssdk.services.s3control.model.GetMultiRegionAccessPointResponse;
import software.amazon.awssdk.services.s3control.model.MultiRegionAccessPointStatus;
import software.amazon.awssdk.services.s3control.model.S3ControlException;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.Logger;

public final class S3ChecksumsTestUtils {

    private static final SdkChecksum CRC32 = SdkChecksum.forAlgorithm(DefaultChecksumAlgorithm.CRC32);

    private S3ChecksumsTestUtils() {
    }

    public static String createBucket(S3Client s3, String name, Logger log) {
        log.debug(() -> "Creating bucket: " + name);
        createBucket(s3, name, 3, log);
        s3.putBucketAccelerateConfiguration(r -> r.bucket(name)
                                                  .accelerateConfiguration(c -> c.status(BucketAccelerateStatus.ENABLED)));
        return name;
    }

    public static void createBucket(S3Client s3, String bucketName, int retryCount, Logger log) {
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
            log.debug(() -> "Error attempting to create bucket: " + bucketName);
            if ("BucketAlreadyOwnedByYou".equals(e.awsErrorDetails().errorCode())) {
                log.debug(() -> String.format("%s bucket already exists, likely leaked by a previous run%n", bucketName));
            } else if ("TooManyBuckets".equals(e.awsErrorDetails().errorCode())) {
                log.debug(() -> "Printing all buckets for debug:");
                s3.listBuckets().buckets().forEach(l -> log.debug(l::toString));
                if (retryCount < 2) {
                    log.debug(() -> "Retrying...");
                    createBucket(s3, bucketName, retryCount + 1, log);
                } else {
                    throw e;
                }
            } else {
                throw e;
            }
        }

        s3.waiter().waitUntilBucketExists(r -> r.bucket(bucketName));
    }

    public static String createEozBucket(S3Client s3, String bucketName, Logger log) {
        String eozBucketName = bucketName;
        log.debug(() -> "Creating EOZ bucket: " + eozBucketName);
        CreateBucketConfiguration cfg =
            CreateBucketConfiguration.builder()
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

    public static String createMrap(S3ControlClient s3Control, String accountId, String mrapName, String bucketName, Logger log)
        throws InterruptedException {
        try {
            s3Control.getMultiRegionAccessPoint(r -> r.accountId(accountId).name(mrapName));
        } catch (S3ControlException e) {
            if (e.awsErrorDetails().sdkHttpResponse().statusCode() != 404) {
                throw e;
            }

            CreateMultiRegionAccessPointRequest createMrap =
                CreateMultiRegionAccessPointRequest.builder()
                                                   .accountId(accountId)
                                                   .details(d -> d.name(mrapName)
                                                                  .regions(software.amazon.awssdk.services.s3control.model.Region.builder()
                                                                                                                                 .bucket(bucketName)
                                                                                                                                 .build()))
                                                   .build();

            s3Control.createMultiRegionAccessPoint(createMrap);
        }

        return waitForMrapToBeReady(s3Control, accountId, mrapName, log);
    }

    private static String waitForMrapToBeReady(S3ControlClient s3Control, String accountId, String mrapName, Logger log)
        throws InterruptedException {
        GetMultiRegionAccessPointResponse getMrapResponse = null;

        Instant waitStart = Instant.now();
        boolean initial = true;
        do {
            if (!initial) {
                Thread.sleep(Duration.ofSeconds(10).toMillis());
                initial = true;
            }
            GetMultiRegionAccessPointResponse response =
                s3Control.getMultiRegionAccessPoint(r -> r.accountId(accountId).name(mrapName));
            log.debug(() -> "Wait response: " + response);
            getMrapResponse = response;
        } while (MultiRegionAccessPointStatus.READY != getMrapResponse.accessPoint().status()
                 && Duration.between(Instant.now(), waitStart).compareTo(Duration.ofMinutes(5)) < 0);

        return "arn:aws:s3::" + accountId + ":accesspoint/" + getMrapResponse.accessPoint().alias();
    }

    public static String getAccountId(StsClient sts) {
        return sts.getCallerIdentity().account();
    }

    public static String createAccessPoint(S3ControlClient s3Control, String accountId, String apName, String bucketName) {
        try {
            s3Control.getAccessPoint(r -> r.accountId(accountId).name(apName));
        } catch (S3ControlException e) {
            if (e.awsErrorDetails().sdkHttpResponse().statusCode() != 404) {
                throw e;
            }

            s3Control.createAccessPoint(r -> r.bucket(bucketName).name(apName).accountId(accountId));
        }

        // wait for AP to be ready
        return s3Control.getAccessPoint(r -> r.accountId(accountId).name(apName)).accessPointArn();
    }


    public static void assumeNotAccessPointWithPathStyle(TestConfig config) {
        BucketType bucketType = config.getBucketType();
        Assumptions.assumeFalse(config.isForcePathStyle() && bucketType.isArnType(),
                                "Path style doesn't work with ARN type buckets");
    }

    public static void assumeNotAccelerateWithPathStyle(TestConfig config) {
        Assumptions.assumeFalse(config.isForcePathStyle() && config.isAccelerateEnabled(),
                                "Path style doesn't work with Accelerate");
    }

    public static void assumeNotAccelerateWithArnType(TestConfig config) {
        Assumptions.assumeFalse(config.isAccelerateEnabled() && config.getBucketType().isArnType(),
                                "Accelerate doesn't work with ARN buckets");
    }

    public static void assumeNotAccelerateWithEoz(TestConfig config) {
        Assumptions.assumeFalse(config.isAccelerateEnabled() && config.getBucketType() == BucketType.EOZ,
                                "Accelerate is not supported with Express One Zone");
    }


    public static String crc32(String s) {
        return crc32(s.getBytes(StandardCharsets.UTF_8));
    }

    public static String crc32(byte[] bytes) {
        CRC32.reset();
        CRC32.update(bytes);
        return BinaryUtils.toBase64(CRC32.getChecksumBytes());
    }

    public static String crc32(Path p) throws IOException {
        CRC32.reset();

        byte[] buff = new byte[4096];
        int read;
        try (InputStream is = Files.newInputStream(p)) {
            while (true) {
                read = is.read(buff);
                if (read == -1) {
                    break;
                }
                CRC32.update(buff, 0, read);
            }
        }

        return BinaryUtils.toBase64(CRC32.getChecksumBytes());
    }

    public static S3Client makeSyncClient(TestConfig config, Region region, AwsCredentialsProvider provider) {
        switch (config.getFlavor()) {
            case STANDARD_SYNC:
                return S3Client.builder()
                               .forcePathStyle(config.isForcePathStyle())
                               .requestChecksumCalculation(config.getRequestChecksumValidation())
                               .region(region)
                               .credentialsProvider(provider)
                               .accelerate(config.isAccelerateEnabled())
                               .build();
            default:
                throw new RuntimeException("Unsupported sync flavor: " + config.getFlavor());
        }
    }

    public static S3AsyncClient makeAsyncClient(TestConfig config, Region region, AwsCredentialsProvider provider) {
        switch (config.getFlavor()) {
            case STANDARD_ASYNC:
                return S3AsyncClient.builder()
                                    .forcePathStyle(config.isForcePathStyle())
                                    .requestChecksumCalculation(config.getRequestChecksumValidation())
                                    .region(region)
                                    .credentialsProvider(provider)
                                    .accelerate(config.isAccelerateEnabled())
                                    .build();
            case MULTIPART_ENABLED:
                return S3AsyncClient.builder()
                                    .forcePathStyle(config.isForcePathStyle())
                                    .requestChecksumCalculation(config.getRequestChecksumValidation())
                                    .region(region)
                                    .credentialsProvider(provider)
                                    .accelerate(config.isAccelerateEnabled())
                                    .multipartEnabled(true)
                                    .build();
            case CRT_BASED: {
                return S3AsyncClient.crtBuilder()
                                    .forcePathStyle(config.isForcePathStyle())
                                    .requestChecksumCalculation(config.getRequestChecksumValidation())
                                    .region(region)
                                    .credentialsProvider(provider)
                                    .accelerate(config.isAccelerateEnabled())
                                    .build();
            }
            default:
                throw new RuntimeException("Unsupported async flavor: " + config.getFlavor());
        }
    }

    public static S3Client makeSyncClient(TestConfig config, ClientOverrideConfiguration overrideConfiguration,
                                   Region region, AwsCredentialsProvider provider) {
        switch (config.getFlavor()) {
            case STANDARD_SYNC:
                return S3Client.builder()
                               .overrideConfiguration(overrideConfiguration)
                               .forcePathStyle(config.isForcePathStyle())
                               .requestChecksumCalculation(config.getRequestChecksumValidation())
                               .region(region)
                               .credentialsProvider(provider)
                               .accelerate(config.isAccelerateEnabled())
                               .build();
            default:
                throw new RuntimeException("Unsupported sync flavor: " + config.getFlavor());
        }
    }

    public static S3AsyncClient makeAsyncClient(TestConfig config, ClientOverrideConfiguration overrideConfiguration,
                                         Region region, AwsCredentialsProvider provider) {
        switch (config.getFlavor()) {
            case STANDARD_ASYNC:
                return S3AsyncClient.builder()
                                    .overrideConfiguration(overrideConfiguration)
                                    .forcePathStyle(config.isForcePathStyle())
                                    .requestChecksumCalculation(config.getRequestChecksumValidation())
                                    .region(region)
                                    .credentialsProvider(provider)
                                    .accelerate(config.isAccelerateEnabled())
                                    .build();
            case MULTIPART_ENABLED:
                return S3AsyncClient.builder()
                                    .overrideConfiguration(overrideConfiguration)
                                    .forcePathStyle(config.isForcePathStyle())
                                    .requestChecksumCalculation(config.getRequestChecksumValidation())
                                    .region(region)
                                    .credentialsProvider(provider)
                                    .accelerate(config.isAccelerateEnabled())
                                    .multipartEnabled(true)
                                    .build();
            case CRT_BASED: {
                return S3AsyncClient.crtBuilder()
                                    .forcePathStyle(config.isForcePathStyle())
                                    .requestChecksumCalculation(config.getRequestChecksumValidation())
                                    .region(region)
                                    .credentialsProvider(provider)
                                    .accelerate(config.isAccelerateEnabled())
                                    .build();
            }
            default:
                throw new RuntimeException("Unsupported async flavor: " + config.getFlavor());
        }
    }

    public static S3TransferManager makeTm(TestConfig config, ClientOverrideConfiguration overrideConfiguration,
                                            Region region, AwsCredentialsProvider provider) {
        S3AsyncClient s3AsyncClient = makeAsyncClient(config, overrideConfiguration, region, provider);
        return S3TransferManager.builder().s3Client(s3AsyncClient).build();
    }

    public static Path createRandomFile16KB() throws IOException {
        Path tmp = Files.createTempFile(null, null);
        byte[] randomBytes = new byte[1024];
        new Random().nextBytes(randomBytes);
        try (OutputStream os = Files.newOutputStream(tmp)) {
            for (int i = 0; i < 16; ++i) {
                os.write(randomBytes);
            }
        }
        return tmp;
    }

    public static Path createRandomFile80MB() throws IOException {
        Path tmp = Files.createTempFile(null, null);
        byte[] randomBytes = new byte[1024 * 1024];
        new Random().nextBytes(randomBytes);
        try (OutputStream os = Files.newOutputStream(tmp)) {
            for (int i = 0; i < 80; ++i) {
                os.write(randomBytes);
            }
        }
        return tmp;
    }

    public static String randomKey() {
        return BinaryUtils.toHex(UUID.randomUUID().toString().getBytes());
    }

}
