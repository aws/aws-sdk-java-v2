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

package software.amazon.awssdk.stability.tests.s3;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.testutils.RandomTempFile;
import software.amazon.awssdk.testutils.service.AwsTestBase;
import software.amazon.awssdk.utils.Logger;

public abstract class S3BaseStabilityTest extends AwsTestBase {
    private static final Logger log = Logger.loggerFor(S3BaseStabilityTest.class);
    protected static final int CONCURRENCY = 100;
    protected static final int TOTAL_RUNS = 50;
    protected static final String LARGE_KEY_NAME = "2GB";

    protected static S3AsyncClient s3NettyClient;
    protected static S3Client s3ApacheClient;

    static {
        s3NettyClient = S3AsyncClient.builder()
                                     .httpClientBuilder(NettyNioAsyncHttpClient.builder()
                                                                               .maxConcurrency(CONCURRENCY))
                                     .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                                     .overrideConfiguration(b -> b.apiCallTimeout(Duration.ofMinutes(10))
                                                                  // Retry at test level
                                                                  .retryPolicy(RetryPolicy.none()))
                                     .build();


        s3ApacheClient = S3Client.builder()
                                 .httpClientBuilder(ApacheHttpClient.builder()
                                                                    .maxConnections(CONCURRENCY))
                                 .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                                 .overrideConfiguration(b -> b.apiCallTimeout(Duration.ofMinutes(10)))
                                 .build();
    }

    protected String computeKeyName(int i) {
        return "key_" + i;
    }

    protected static void deleteBucketAndAllContents(String bucketName) {
        try {
            List<CompletableFuture<?>> futures = new ArrayList<>();

            s3NettyClient.listObjectsV2Paginator(b -> b.bucket(bucketName))
                         .subscribe(r -> r.contents().forEach(s -> futures.add(s3NettyClient.deleteObject(o -> o.bucket(bucketName).key(s.key())))))
                         .join();

            CompletableFuture<?>[] futureArray = futures.toArray(new CompletableFuture<?>[0]);

            CompletableFuture.allOf(futureArray).join();

            s3NettyClient.deleteBucket(DeleteBucketRequest.builder().bucket(bucketName).build()).join();
        } catch (Exception e) {
            log.error(() -> "Failed to delete bucket: " +bucketName);
        }
    }

    protected void verifyObjectExist(String bucketName, String keyName, long size) throws IOException {
        try {
            s3ApacheClient.headBucket(b -> b.bucket(bucketName));
        } catch (NoSuchBucketException e) {
            log.info(() -> "NoSuchBucketException was thrown, staring to create the bucket");
            s3ApacheClient.createBucket(b -> b.bucket(bucketName));
        }

        try {
            s3ApacheClient.headObject(b -> b.key(keyName).bucket(bucketName));
        } catch (NoSuchKeyException e) {
            log.info(() -> "NoSuchKeyException was thrown, starting to upload the object");
            RandomTempFile file = new RandomTempFile(size);
            s3ApacheClient.putObject(b -> b.bucket(bucketName).key(keyName), RequestBody.fromFile(file));
            file.delete();
        }
    }

    public abstract void putObject_getObject_highConcurrency();

    public abstract void largeObject_put_get_usingFile();
}
