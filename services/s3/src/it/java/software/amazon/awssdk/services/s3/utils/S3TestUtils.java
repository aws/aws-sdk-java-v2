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

package software.amazon.awssdk.services.s3.utils;

import java.rmi.NoSuchObjectException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ExpirationStatus;
import software.amazon.awssdk.services.s3.model.ListObjectVersionsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectVersionsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.testutils.Waiter;
import software.amazon.awssdk.utils.Logger;

public class S3TestUtils {
    private static final Logger log = Logger.loggerFor(S3TestUtils.class);
    private static final String TEST_BUCKET_PREFIX = "s3-test-bucket-";
    private static final String NON_DNS_COMPATIBLE_TEST_BUCKET_PREFIX = "s3.test.bucket.";

    private static Map<Class<?>, List<Runnable>> cleanupTasks = new ConcurrentHashMap<>();

    public static String getTestBucket(S3Client s3) {
        return getBucketWithPrefix(s3, TEST_BUCKET_PREFIX);
    }

    public static String getNonDnsCompatibleTestBucket(S3Client s3) {
        return getBucketWithPrefix(s3, NON_DNS_COMPATIBLE_TEST_BUCKET_PREFIX);
    }

    private static String getBucketWithPrefix(S3Client s3, String bucketPrefix) {
        String testBucket =
            s3.listBuckets()
              .buckets()
              .stream()
              .map(Bucket::name)
              .filter(name -> name.startsWith(bucketPrefix))
              .findAny()
              .orElse(null);

        if (testBucket == null) {
            String newTestBucket = bucketPrefix + UUID.randomUUID();
            s3.createBucket(r -> r.bucket(newTestBucket));
            Waiter.run(() -> s3.headBucket(r -> r.bucket(newTestBucket)))
                  .ignoringException(NoSuchBucketException.class)
                  .orFail();
            testBucket = newTestBucket;
        }

        String finalTestBucket = testBucket;

        s3.putBucketLifecycleConfiguration(blc -> blc
            .bucket(finalTestBucket)
            .lifecycleConfiguration(lc -> lc
                .rules(r -> r.expiration(ex -> ex.days(1))
                             .status(ExpirationStatus.ENABLED)
                             .filter(f -> f.prefix(""))
                             .id("delete-old"))));


        return finalTestBucket;
    }

    public static void putObject(Class<?> testClass, S3Client s3, String bucketName, String objectKey, String content) {
        s3.putObject(r -> r.bucket(bucketName).key(objectKey), RequestBody.fromString(content));
        Waiter.run(() -> s3.getObjectAcl(r -> r.bucket(bucketName).key(objectKey)))
              .ignoringException(NoSuchBucketException.class, NoSuchObjectException.class)
              .orFail();
        addCleanupTask(testClass, () -> s3.deleteObject(r -> r.bucket(bucketName).key(objectKey)));
    }

    public static void addCleanupTask(Class<?> testClass, Runnable cleanupTask) {
        cleanupTasks.compute(testClass, (k, tasks) -> {
            if (tasks == null) {
                tasks = new ArrayList<>();
            }
            tasks.add(cleanupTask);
            return tasks;
        });
    }

    public static void runCleanupTasks(Class<?> testClass) {
        List<Runnable> tasksToRun = cleanupTasks.remove(testClass);
        tasksToRun.forEach(r -> {
            try {
                r.run();
            } catch (Exception e) {
                log.warn(() -> "Test cleanup task failed. The failure will be ignored.", e);
            }
        });
    }

    public static void deleteBucketAndAllContents(S3Client s3, String bucketName) {
        try {
            System.out.println("Deleting S3 bucket: " + bucketName);
            ListObjectsResponse response = Waiter.run(() -> s3.listObjects(r -> r.bucket(bucketName)))
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
                        objectListing = s3.listObjects(ListObjectsRequest.builder()
                                                                         .bucket(bucketName)
                                                                         .marker(response.marker())
                                                                         .build())
                                          .contents();
                    } else {
                        break;
                    }
                }
            }


            ListObjectVersionsResponse versions = s3
                    .listObjectVersions(ListObjectVersionsRequest.builder().bucket(bucketName).build());

            if (versions.deleteMarkers() != null) {
                versions.deleteMarkers().forEach(v -> s3.deleteObject(DeleteObjectRequest.builder()
                                                                                         .versionId(v.versionId())
                                                                                         .bucket(bucketName)
                                                                                         .key(v.key())
                                                                                         .build()));
            }

            if (versions.versions() != null) {
                versions.versions().forEach(v -> s3.deleteObject(DeleteObjectRequest.builder()
                                                                                    .versionId(v.versionId())
                                                                                    .bucket(bucketName)
                                                                                    .key(v.key())
                                                                                    .build()));
            }

            s3.deleteBucket(DeleteBucketRequest.builder().bucket(bucketName).build());
        } catch (Exception e) {
            System.err.println("Failed to delete bucket: " + bucketName);
            e.printStackTrace();
        }
    }
}
