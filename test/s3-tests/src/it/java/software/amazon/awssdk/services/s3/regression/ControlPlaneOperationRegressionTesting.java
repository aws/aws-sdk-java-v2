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

import static software.amazon.awssdk.services.s3.regression.S3ChecksumsTestUtils.assumeNotAccelerateWithArnType;
import static software.amazon.awssdk.services.s3.regression.S3ChecksumsTestUtils.assumeNotAccelerateWithEoz;
import static software.amazon.awssdk.services.s3.regression.S3ChecksumsTestUtils.assumeNotAccelerateWithPathStyle;
import static software.amazon.awssdk.services.s3.regression.S3ChecksumsTestUtils.assumeNotAccessPointWithPathStyle;
import static software.amazon.awssdk.services.s3.regression.S3ChecksumsTestUtils.makeAsyncClient;
import static software.amazon.awssdk.services.s3.regression.S3ChecksumsTestUtils.makeSyncClient;

import java.util.List;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.awscore.AwsClient;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.GlacierJobParameters;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.RestoreObjectRequest;
import software.amazon.awssdk.services.s3.model.RestoreRequest;
import software.amazon.awssdk.services.s3.model.StorageClass;
import software.amazon.awssdk.services.s3.model.Tier;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.Logger;

public class ControlPlaneOperationRegressionTesting extends BaseS3RegressionTest {
    private static final Logger LOG = Logger.loggerFor(ControlPlaneOperationRegressionTesting.class);

    // Request checksum required
    @ParameterizedTest
    @MethodSource("testConfigs")
    void deleteObject(TestConfig config) throws Exception {
        assumeNotAccessPointWithPathStyle(config);
        assumeNotAccelerateWithPathStyle(config);
        assumeNotAccelerateWithArnType(config);
        assumeNotAccelerateWithEoz(config);

        LOG.debug(() -> "Running deleteObject with config: " + config.toString());

        String bucket = bucketForType(config.getBucketType());
        String key = putRandomObject(config.getBucketType());
        TestCallable<Void> callable = null;
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
            callable.runnable().call();
        } finally {
            if (callable != null) {
                callable.client().close();
            }
        }
    }

    // Request checksum optional
    @ParameterizedTest
    @MethodSource("testConfigs")
    void restoreObject(TestConfig config) throws Exception {
        assumeNotAccessPointWithPathStyle(config);
        assumeNotAccelerateWithPathStyle(config);
        assumeNotAccelerateWithArnType(config);

        Assumptions.assumeFalse(config.getBucketType() == BucketType.EOZ,
                                "Restore is not supported for S3 Express");

        LOG.debug(() -> "Running restoreObject with config: " + config);

        String bucket = bucketForType(config.getBucketType());
        String key = putRandomArchivedObject(config.getBucketType());
        TestCallable<Void> callable = null;
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
            callable.runnable().call();
        } finally {
            if (callable != null) {
                callable.client().close();
            }
        }
    }

    private TestCallable<Void> callDeleteObjects(DeleteObjectsRequest request, TestConfig config) {
        AwsClient toClose;
        Callable<Void> runnable = null;

        if (config.getFlavor().isAsync()) {
            S3AsyncClient s3Async = makeAsyncClient(config, REGION, CREDENTIALS_PROVIDER_CHAIN);
            toClose = s3Async;
            runnable = () -> {
                CompletableFutureUtils.joinLikeSync(s3Async.deleteObjects(request));
                return null;
            };
        } else {
            S3Client s3 = makeSyncClient(config, REGION, CREDENTIALS_PROVIDER_CHAIN);
            toClose = s3;
            runnable = () -> {
                s3.deleteObjects(request);
                return null;
            };
        }

        return new TestCallable<>(toClose, runnable);
    }

    private TestCallable<Void> callRestoreObject(RestoreObjectRequest request, TestConfig config) {
        AwsClient toClose;
        Callable<Void> callable = null;

        if (config.getFlavor().isAsync()) {
            S3AsyncClient s3Async = makeAsyncClient(config, REGION, CREDENTIALS_PROVIDER_CHAIN);
            toClose = s3Async;
            callable = () -> {
                s3Async.restoreObject(request).join();
                return null;
            };
        } else {
            S3Client s3 = makeSyncClient(config, REGION, CREDENTIALS_PROVIDER_CHAIN);
            toClose = s3;
            callable = () -> {
                s3.restoreObject(request);
                return null;
            };
        }

        return new TestCallable<>(toClose, callable);
    }

    static List<TestConfig> testConfigs() {
        return TestConfig.testConfigs();
    }

    private String putRandomObject(BucketType bucketType) {
        String key = S3ChecksumsTestUtils.randomKey();
        String bucketName = bucketForType(bucketType);
        s3.putObject(r -> r.bucket(bucketName).key(key), RequestBody.fromString("hello"));
        recordObjectToCleanup(bucketType, key);
        return key;
    }

    private String putRandomArchivedObject(BucketType bucketType) {
        String key = S3ChecksumsTestUtils.randomKey();
        String bucketName = bucketForType(bucketType);
        s3.putObject(r -> r.bucket(bucketName).key(key).storageClass(StorageClass.GLACIER), RequestBody.fromString("hello"));
        recordObjectToCleanup(bucketType, key);
        return key;
    }


}
