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

package software.amazon.awssdk.s3benchmarks.s3express;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.BucketInfo;
import software.amazon.awssdk.services.s3.model.BucketType;
import software.amazon.awssdk.services.s3.model.CreateBucketConfiguration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DataRedundancy;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.LocationInfo;
import software.amazon.awssdk.services.s3.model.LocationType;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.testutils.Waiter;
import software.amazon.awssdk.utils.Logger;

public class S3BenchmarkTestUtils {
    protected static final String S3EXPRESS_BUCKET = "s3express-integ--use1-az2-d-s3";
    protected static final String TEST_CREDENTIALS_PROFILE_NAME = "aws-test-account";
    protected static final AwsCredentialsProviderChain CREDENTIALS_PROVIDER_CHAIN =
        AwsCredentialsProviderChain.of(ProfileCredentialsProvider.builder()
                                                                 .profileName(TEST_CREDENTIALS_PROFILE_NAME)
                                                                 .build(),
                                       DefaultCredentialsProvider.create());

    private static final Logger logger = Logger.loggerFor("S3Benchmark");


    private S3BenchmarkTestUtils() {

    }

    static S3ClientBuilder s3ClientBuilder(Region region) {
        return S3Client.builder()
                       .region(region)
                       .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN);

    }

    static S3AsyncClientBuilder s3AsyncClientBuilder(Region region) {
        return S3AsyncClient.builder()
                            .region(region)
                            .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN);

    }

    public static String getTemporaryBucketName(int i, String suffix) {
        return String.format("Java-S3ExpressBenchmark-%s-%s", i, suffix);
    }

    public static String keyNameFromPrefix(int i, String prefix) {
        return String.format("%s-%s", prefix, i);
    }

    public static void createBucketSafely(S3Client client, String bucketName, boolean useS3Express) {
        CreateBucketRequest.Builder createBucketRequestBuilder = CreateBucketRequest.builder().bucket(bucketName);

        if (useS3Express) {
            LocationInfo location = LocationInfo.builder().name("use1-az5").type(LocationType.AVAILABILITY_ZONE).build();
            BucketInfo bucketInfo = BucketInfo.builder()
                                              .dataRedundancy(DataRedundancy.SINGLE_AVAILABILITY_ZONE)
                                              .type(BucketType.DIRECTORY)
                                              .build();
            CreateBucketConfiguration bucketConfiguration = CreateBucketConfiguration.builder()
                                                                                     .location(location)
                                                                                     .bucket(bucketInfo)
                                                                                     .build();
            createBucketRequestBuilder.createBucketConfiguration(bucketConfiguration);
        }
        try {
            client.createBucket(createBucketRequestBuilder.build());
        } catch (S3Exception e) {
            logger.error(() -> "Error attempting to create bucket: " + bucketName);
            if (e.awsErrorDetails().errorCode().equals("BucketAlreadyOwnedByYou")) {
                logger.error(() -> String.format("%s bucket already exists, likely leaked by a previous run%n", bucketName));
            } else {
                throw e;
            }
        }
        client.waiter().waitUntilBucketExists(r -> r.bucket(bucketName));
    }

    public static void deleteBucketSafe(S3Client s3, String bucketName) {
        try {
            logger.info(() -> "Deleting S3 bucket: " + bucketName);
            s3.deleteBucket(DeleteBucketRequest.builder().bucket(bucketName).build());

        } catch (Exception e) {
            logger.error(() -> "Failed to delete bucket: " + bucketName);
            e.printStackTrace();
        }
    }

    public static void deleteBucketAndContentSafely(S3Client s3, String bucketName) {
        try {
            ListObjectsV2Response response = Waiter.run(() -> s3.listObjectsV2(r -> r.bucket(bucketName)))
                                                   .ignoringException(NoSuchBucketException.class)
                                                   .orFail();
            List<S3Object> objectListing = response.contents();

            if (objectListing != null) {
                while (true) {
                    for (Iterator<?> iterator = objectListing.iterator(); iterator.hasNext(); ) {
                        S3Object objectSummary = (S3Object) iterator.next();
                        s3.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(objectSummary.key()).build());
                    }

                    if (response.isTruncated()) {
                        objectListing = s3.listObjectsV2(ListObjectsV2Request.builder()
                                                                             .bucket(bucketName)
                                                                             .continuationToken(response.continuationToken())
                                                                             .build())
                                          .contents();
                    } else {
                        break;
                    }
                }
            }
            s3.deleteBucket(DeleteBucketRequest.builder().bucket(bucketName).build());
        } catch (Exception e) {
            logger.error(() -> "Failed to delete bucket: " + bucketName);
            e.printStackTrace();
        }
    }

    public static byte[] randomBytes(long size) {
        byte[] bytes = new byte[Math.toIntExact(size)];
        ThreadLocalRandom.current().nextBytes(bytes);
        return bytes;
    }

    public static void printOutResult(List<Double> metrics, String name, long contentLengthInByte) {
        logger.info(() -> String.format("===============  %s Result ================", name));
        logger.info(() -> String.valueOf(metrics));
        double averageLatency = metrics.stream()
                                       .mapToDouble(a -> a)
                                       .average()
                                       .orElse(0.0);

        double lowestLatency = metrics.stream()
                                      .mapToDouble(a -> a)
                                      .min().orElse(0.0);

        double contentLengthInKilobytes = (contentLengthInByte / (double) 1024) * 8.0;
        logger.info(() -> "Average latency (s): " + averageLatency);
        logger.info(() -> "Latency variance (s): " + variance(metrics, averageLatency));
        logger.info(() -> "Object size (Gigabit): " + contentLengthInKilobytes);
        logger.info(() -> "Average throughput (Gbps): " + contentLengthInKilobytes / averageLatency);
        logger.info(() -> "Highest average throughput (Gbps): " + contentLengthInKilobytes / lowestLatency);
        logger.info(() -> "==========================================================");
    }

    /**
     * calculates the variance (std deviation squared) of the sample
     * @param sample the values to calculate the variance for
     * @param mean the known mean of the sample
     * @return the variance value
     */
    private static double variance(Collection<Double> sample, double mean) {
        double numerator = 0;
        for (double value : sample) {
            double diff = value - mean;
            numerator += (diff * diff);
        }
        return numerator / sample.size();
    }
}
