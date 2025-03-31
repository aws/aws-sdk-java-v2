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

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.services.s3.checksum.S3ChecksumsTestUtils.assumeNotAccelerateWithArnType;
import static software.amazon.awssdk.services.s3.checksum.S3ChecksumsTestUtils.assumeNotAccelerateWithEoz;
import static software.amazon.awssdk.services.s3.checksum.S3ChecksumsTestUtils.assumeNotAccelerateWithPathStyle;
import static software.amazon.awssdk.services.s3.checksum.S3ChecksumsTestUtils.assumeNotAccessPointWithPathStyle;
import static software.amazon.awssdk.services.s3.checksum.S3ChecksumsTestUtils.crc32;

import io.reactivex.Flowable;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.auth.signer.S3SignerExecutionAttribute;
import software.amazon.awssdk.awscore.AwsClient;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.checksums.DefaultChecksumAlgorithm;
import software.amazon.awssdk.checksums.SdkChecksum;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.BlockingInputStreamAsyncRequestBody;
import software.amazon.awssdk.core.async.BlockingOutputStreamAsyncRequestBody;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketAccelerateStatus;
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
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
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
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.CompletedUpload;
import software.amazon.awssdk.transfer.s3.model.Upload;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.CancellableOutputStream;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.FunctionalUtils;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.SdkAutoCloseable;

public class ChecksumIntegrationTesting {
    private static final String BUCKET_NAME_PREFIX = "do-not-delete-checksums-";
    private static final String MRAP_NAME = "do-not-delete-checksum-testing";
    private static final String AP_NAME = "do-not-delete-checksum-testing-ap";
    private static final String EOZ_SUFFIX = "--usw2-az3--x-s3";

    private static final Logger LOG = Logger.loggerFor(ChecksumIntegrationTesting.class);
    private static final Region REGION = Region.US_WEST_2;
    private static final String TEST_CREDENTIALS_PROFILE_NAME = "aws-test-account";

    static final byte[] smallContent = "Hello world".getBytes(StandardCharsets.UTF_8);
    static final byte[] largeContent = largeContent();

    public static final AwsCredentialsProviderChain CREDENTIALS_PROVIDER_CHAIN =
        AwsCredentialsProviderChain.of(ProfileCredentialsProvider.builder()
                                                                 .profileName(TEST_CREDENTIALS_PROFILE_NAME)
                                                                 .build(),
                                       DefaultCredentialsProvider.create());

    private static final ExecutorService ASYNC_REQUEST_BODY_EXECUTOR = Executors.newSingleThreadExecutor();

    private static String accountId;
    private static String bucketName;
    private static String mrapArn;
    private static String eozBucket;
    private static String apArn;

    private static S3ControlClient s3Control;
    private static S3Client s3;
    private static StsClient sts;

    private static Path testFileSmall;
    private static Path testFileLarge;

    private Map<BucketType, List<String>> bucketCleanup = new HashMap<>();

    @BeforeAll
    static void setup() throws InterruptedException, IOException {
        // Log.initLoggingToStdout(Log.LogLevel.Trace);

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

        testFileSmall = createRandomFile16KB();
        testFileLarge = createRandomFile80MB();
    }

    @AfterEach
    public void methodCleanup() {
        bucketCleanup.forEach((bt, keys) -> {
            String bucket = bucketForType(bt);
            keys.forEach(k -> s3.deleteObject(r -> r.bucket(bucket).key(k)));
        });

        bucketCleanup.clear();
    }

    @AfterAll
    public static void cleanup() {
        ASYNC_REQUEST_BODY_EXECUTOR.shutdownNow();
    }


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
            callable.runnable.call();
        } finally {
            if (callable != null) {
                callable.client.close();
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
            callable.runnable.call();
        } finally {
            if (callable != null) {
                callable.client.close();
            }
        }
    }

    @ParameterizedTest
    @MethodSource("uploadConfigs")
    void putObject(UploadConfig config) throws Exception {
        assumeNotAccelerateWithPathStyle(config.getBaseConfig());
        assumeNotAccessPointWithPathStyle(config.getBaseConfig());
        assumeNotAccelerateWithArnType(config.getBaseConfig());
        assumeNotAccelerateWithEoz(config.getBaseConfig());

        // For testing purposes, ContentProvider is Publisher<ByteBuffer> for async clients
        // There is no way to create AsyncRequestBody with a Publisher<ByteBuffer> and also provide the content length
        Assumptions.assumeFalse(config.getBodyType() == BodyType.CONTENT_PROVIDER_WITH_LENGTH
                                && config.getBaseConfig().getFlavor().isAsync(),
                                "No way to create AsyncRequestBody by giving both an Publisher and the content length");

        // Payload signing doesn't work correctly for async java based
        Assumptions.assumeFalse(
            (config.getBaseConfig().getFlavor() == S3ClientFlavor.ASYNC_JAVA_BASED ||
            config.getBaseConfig().getFlavor() == S3ClientFlavor.TM_JAVA)
                                && (config.getBaseConfig().isPayloadSigning()
                                    // MRAP requires body signing
                                    || config.getBaseConfig().getBucketType() == BucketType.MRAP),
                                "Async payload signing doesn't work with Java based clients");

        // For testing purposes, ContentProvider is Publisher<ByteBuffer> for async clients
        // Async java based clients don't currently support unknown content-length bodies
        Assumptions.assumeFalse(
            (config.getBaseConfig().getFlavor() == S3ClientFlavor.ASYNC_JAVA_BASED ||
                        config.getBaseConfig().getFlavor() == S3ClientFlavor.TM_JAVA)
                                && config.getBodyType() == BodyType.CONTENT_PROVIDER_NO_LENGTH,
                                "Async Java based support unknown content length");

        LOG.debug(() -> "Running putObject with config: " + config);

        BucketType bucketType = config.getBaseConfig().getBucketType();

        String bucket = bucketForType(bucketType);
        String key = randomKey();

        PutObjectRequest request = PutObjectRequest.builder()
                                                   .bucket(bucket)
                                                   .key(key)
                                                   .build();


        RequestRecorder recorder = new RequestRecorder();

        ClientOverrideConfiguration.Builder overrideConfiguration =
            ClientOverrideConfiguration.builder()
                                       .addExecutionInterceptor(recorder);

        if (config.getBaseConfig().isPayloadSigning()) {
            overrideConfiguration.addExecutionInterceptor(new EnablePayloadSigningInterceptor());
        }

        TestCallable<PutObjectResponse> callable = null;
        try {

            Long actualContentLength = null;
            boolean requestBodyHasContentLength = false;
            String actualCrc32;

            if (!config.getBaseConfig().getFlavor().isAsync()) {
                TestRequestBody body = getRequestBody(config.getBodyType(), config.getContentSize());
                callable = callPutObject(request, body, config.getBaseConfig(), overrideConfiguration.build());
                actualContentLength = body.getActualContentLength();
                requestBodyHasContentLength = body.optionalContentLength().isPresent();
                actualCrc32 = body.getChecksum();
            } else if (config.getBaseConfig().getFlavor() == S3ClientFlavor.TM_JAVA) {
                TestAsyncBody body = getAsyncRequestBody(config.getBodyType(), config.contentSize);
                callable = callTmUpload(request, body, config.getBaseConfig(), overrideConfiguration.build());
                actualContentLength = body.getActualContentLength();
                requestBodyHasContentLength = body.getAsyncRequestBody().contentLength().isPresent();
                actualCrc32 = body.getChecksum();
            } else {
                TestAsyncBody body = getAsyncRequestBody(config.getBodyType(), config.contentSize);
                callable = callPutObject(request, body, config.getBaseConfig(), overrideConfiguration.build());
                actualContentLength = body.getActualContentLength();
                requestBodyHasContentLength = body.getAsyncRequestBody().contentLength().isPresent();
                actualCrc32 = body.getChecksum();
            }

            PutObjectResponse response = callable.runnable.call();

            recordObjectToCleanup(bucketType, key);

            // mpu not supported
            if (config.getBaseConfig().getFlavor() == S3ClientFlavor.TM_JAVA) {
                return;
            }

            // We only validate when configured to WHEN_SUPPORTED since checksums are optional for PutObject
            if (config.getBaseConfig().getRequestChecksumValidation() == RequestChecksumCalculation.WHEN_SUPPORTED
                // CRT switches to MPU under the hood which doesn't support checksums
                && config.getBaseConfig().getFlavor() != S3ClientFlavor.ASYNC_CRT) {
                assertThat(response.checksumCRC32()).isEqualTo(actualCrc32);
            }

            // We can't set an execution interceptor when using CRT
            if (config.getBaseConfig().getFlavor() == S3ClientFlavor.ASYNC_CRT) {
                return;
            }

            assertThat(recorder.getRequests()).isNotEmpty();

            for (SdkHttpRequest httpRequest : recorder.getRequests()) {
                // skip any non-PUT requests, e.g. GetSession for EOZ requests
                if (httpRequest.method() != SdkHttpMethod.PUT) {
                    continue;
                }

                String payloadSha = httpRequest.firstMatchingHeader("x-amz-content-sha256").get();
                if (payloadSha.startsWith("STREAMING")) {
                    String decodedContentLength = httpRequest.firstMatchingHeader("x-amz-decoded-content-length").get();
                    assertThat(Long.parseLong(decodedContentLength)).isEqualTo(actualContentLength);
                } else {
                    Optional<String> contentLength = httpRequest.firstMatchingHeader("Content-Length");
                    if (requestBodyHasContentLength) {
                        assertThat(Long.parseLong(contentLength.get())).isEqualTo(actualContentLength);
                    }
                }
            }

        } finally {
            if (callable != null) {
                callable.client.close();
            }
        }
    }


    private TestCallable<Void> callDeleteObjects(DeleteObjectsRequest request, TestConfig config) {
        AwsClient toClose;
        Callable<Void> runnable = null;

        if (config.getFlavor().isAsync()) {
            S3AsyncClient s3Async = makeAsyncClient(config, null);
            toClose = s3Async;
            runnable = () -> {
                CompletableFutureUtils.joinLikeSync(s3Async.deleteObjects(request));
                return null;
            };
        } else {
            S3Client s3 = makeSyncClient(config, null);
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
            S3AsyncClient s3Async = makeAsyncClient(config, null);
            toClose = s3Async;
            callable = () -> {
                s3Async.restoreObject(request).join();
                return null;
            };
        } else {
            S3Client s3 = makeSyncClient(config, null);
            toClose = s3;
            callable = () -> {
                s3.restoreObject(request);
                return null;
            };
        }

        return new TestCallable<>(toClose, callable);
    }

    private TestCallable<PutObjectResponse> callPutObject(PutObjectRequest request, TestRequestBody requestBody, TestConfig config,
                                       ClientOverrideConfiguration overrideConfiguration) {
        S3Client s3Client = makeSyncClient(config, overrideConfiguration);
        Callable<PutObjectResponse> callable = () -> {
            try {
                return s3Client.putObject(request, requestBody);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
        return new TestCallable<>(s3Client, callable);
    }

    private TestCallable<PutObjectResponse> callPutObject(PutObjectRequest request, TestAsyncBody requestBody, TestConfig config,
                                       ClientOverrideConfiguration overrideConfiguration) {
        S3AsyncClient s3Client = makeAsyncClient(config, overrideConfiguration);
        Callable<PutObjectResponse> callable = () -> {
            try {
                AsyncRequestBody asyncRequestBody = requestBody.getAsyncRequestBody();
                CompletableFuture<PutObjectResponse> future = s3Client.putObject(request, asyncRequestBody);
                performWriteIfNeeded(requestBody);
                return CompletableFutureUtils.joinLikeSync(future);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
        return new TestCallable<>(s3Client, callable);
    }

    private TestCallable<PutObjectResponse> callTmUpload(PutObjectRequest request, TestAsyncBody requestBody, TestConfig config,
                                                         ClientOverrideConfiguration overrideConfiguration) {
        S3TransferManager transferManager = makeTm(config, overrideConfiguration);
        Callable<PutObjectResponse> callable = () -> {
            try {
                Upload upload = transferManager.upload(
                    r -> r.requestBody(requestBody.getAsyncRequestBody()).putObjectRequest(request));
                performWriteIfNeeded(requestBody);
                CompletedUpload completedUpload = CompletableFutureUtils.joinLikeSync(upload.completionFuture());
                return completedUpload.response();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
        return new TestCallable<>(transferManager, callable);
    }

    void performWriteIfNeeded(TestAsyncBody requestBody) throws IOException {
        if (requestBody.bodyType == BodyType.BLOCKING_INPUT_STREAM) {
            BlockingInputStreamAsyncRequestBody body = (BlockingInputStreamAsyncRequestBody) requestBody.asyncRequestBody;
            InputStream inputStream = ((TestAsyncBodyForBlockingInputStream) requestBody).inputStream;
            body.writeInputStream(inputStream);
            inputStream.close();
        }
        if (requestBody.bodyType == BodyType.BLOCKING_OUTPUT_STREAM) {
            TestAsyncBodyForBlockingOutputStream body = (TestAsyncBodyForBlockingOutputStream) requestBody;
            CancellableOutputStream outputStream =
                ((BlockingOutputStreamAsyncRequestBody) body.getAsyncRequestBody()).outputStream();
            body.bodyWrite.accept(outputStream);
            outputStream.close();
        }
    }

    private static class TestCallable<ResponseT> {
        private SdkAutoCloseable client;
        private Callable<ResponseT> runnable;

        TestCallable(SdkAutoCloseable client, Callable<ResponseT> runnable) {
            this.client = client;
            this.runnable = runnable;
        }
    }

    private S3Client makeSyncClient(TestConfig config, ClientOverrideConfiguration overrideConfiguration) {
        switch (config.getFlavor()) {
            case JAVA_BASED:
                return S3Client.builder()
                    .forcePathStyle(config.isForcePathStyle())
                    .requestChecksumCalculation(config.getRequestChecksumValidation())
                    .region(REGION)
                    .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                    .accelerate(config.isAccelerateEnabled())
                    .overrideConfiguration(overrideConfiguration)
                    .build();
            default:
                throw new RuntimeException("Unsupported sync flavor: " + config.getFlavor());
        }
    }

    private S3AsyncClient makeAsyncClient(TestConfig config, ClientOverrideConfiguration overrideConfiguration) {
        switch (config.getFlavor()) {
            case ASYNC_JAVA_BASED:
                return S3AsyncClient.builder()
                                     .forcePathStyle(config.isForcePathStyle())
                                     .requestChecksumCalculation(config.getRequestChecksumValidation())
                                     .region(REGION)
                                     .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                                     .accelerate(config.isAccelerateEnabled())
                                     .overrideConfiguration(overrideConfiguration)
                                     .build();
            case TM_JAVA:
                return S3AsyncClient.builder()
                                    .forcePathStyle(config.isForcePathStyle())
                                    .requestChecksumCalculation(config.getRequestChecksumValidation())
                                    .region(REGION)
                                    .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                                    .accelerate(config.isAccelerateEnabled())
                                    .overrideConfiguration(overrideConfiguration)
                                    .multipartEnabled(true)
                                    .build();
            case ASYNC_CRT: {
                return S3AsyncClient.crtBuilder()
                                    .forcePathStyle(config.isForcePathStyle())
                                    .requestChecksumCalculation(config.getRequestChecksumValidation())
                                    .region(REGION)
                                    .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                                    .accelerate(config.isAccelerateEnabled())
                                    .build();
            }
            default:
                throw new RuntimeException("Unsupported async flavor: " + config.getFlavor());
        }
    }

    private S3TransferManager makeTm(TestConfig config, ClientOverrideConfiguration overrideConfiguration) {
        S3AsyncClient s3AsyncClient = makeAsyncClient(config, overrideConfiguration);
        return S3TransferManager.builder().s3Client(s3AsyncClient).build();
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

    static class UploadConfig {
        private TestConfig baseConfig;
        private BodyType bodyType;
        private ContentSize contentSize;

        public TestConfig getBaseConfig() {
            return baseConfig;
        }

        public void setBaseConfig(TestConfig baseConfig) {
            this.baseConfig = baseConfig;
        }

        public BodyType getBodyType() {
            return bodyType;
        }

        public void setBodyType(BodyType bodyType) {
            this.bodyType = bodyType;
        }

        public void setContentSize(ContentSize contentSize) {
            this.contentSize = contentSize;
        }

        public ContentSize getContentSize() {
            return this.contentSize;
        }

        @Override
        public String toString() {
            return "UploadConfig{" +
                   "baseConfig=" + baseConfig +
                   ", bodyType=" + bodyType +
                   ", contentSize=" + contentSize +
                   '}';
        }

    }

    static class TestRequestBody extends RequestBody {
        private final long contentLength;
        private final String checksum;

        protected TestRequestBody(RequestBody wrapped, long contentLength, String checksum) {
            super(wrapped.contentStreamProvider(), wrapped.optionalContentLength().orElse(null), wrapped.contentType());
            this.contentLength = contentLength;
            this.checksum = checksum;
        }

        public long getActualContentLength() {
            return contentLength;
        }

        public String getChecksum() {
            return checksum;
        }
    }

    private static class TestAsyncBody {
        private final AsyncRequestBody asyncRequestBody;
        private final long actualContentLength;
        private final String checksum;
        private final BodyType bodyType;

        private TestAsyncBody(AsyncRequestBody asyncRequestBody, long actualContentLength, String checksum, BodyType bodyType) {
            this.asyncRequestBody = asyncRequestBody;
            this.actualContentLength = actualContentLength;
            this.checksum = checksum;
            this.bodyType = bodyType;
        }

        public AsyncRequestBody getAsyncRequestBody() {
            return asyncRequestBody;
        }

        public long getActualContentLength() {
            return actualContentLength;
        }

        public String getChecksum() {
            return checksum;
        }
    }

    private static class TestAsyncBodyForBlockingOutputStream extends TestAsyncBody {
        private final Consumer<CancellableOutputStream> bodyWrite;
        private TestAsyncBodyForBlockingOutputStream(AsyncRequestBody asyncRequestBody,
                                                     Consumer<CancellableOutputStream> bodyWrite,
                                                     long actualContentLength,
                                                     String checksum,
                                                     BodyType bodyType) {
            super(asyncRequestBody, actualContentLength, checksum, bodyType);
            this.bodyWrite = bodyWrite;
        }
    }

    private static class TestAsyncBodyForBlockingInputStream extends TestAsyncBody {
        private final InputStream inputStream;
        private TestAsyncBodyForBlockingInputStream(AsyncRequestBody asyncRequestBody,
                                                    InputStream inputStream,
                                                    long actualContentLength,
                                                    String checksum,
                                                    BodyType bodyType) {
            super(asyncRequestBody, actualContentLength, checksum, bodyType);
            this.inputStream = inputStream;
        }
    }

    static List<TestConfig> testConfigs() {
        return TestConfig.testConfigs();
    }

    enum BodyType {
        INPUTSTREAM_RESETABLE,
        INPUTSTREAM_NOT_RESETABLE,

        STRING,

        FILE,

        CONTENT_PROVIDER_WITH_LENGTH,

        CONTENT_PROVIDER_NO_LENGTH,

        BYTES,
        BYTE_BUFFER,
        REMAINING_BYTE_BUFFER,

        BYTES_UNSAFE,
        BYTE_BUFFER_UNSAFE,
        REMAINING_BYTE_BUFFER_UNSAFE,

        BUFFERS,
        BUFFERS_REMAINING,
        BUFFERS_UNSAFE,
        BUFFERS_REMAINING_UNSAFE,

        BLOCKING_INPUT_STREAM,
        BLOCKING_OUTPUT_STREAM
    }

    enum ContentSize {
        SMALL,
        LARGE; // 200 MiB

        byte[] byteContent() {
            switch (this) {
                case SMALL: return smallContent;
                case LARGE: return largeContent;
                default: throw new IllegalArgumentException("not supported ContentSize " + this);
            }
        }

        String stringContent() {
            switch (this) {
                case SMALL: return "Hello World!";
                case LARGE: return new String(largeContent(), StandardCharsets.UTF_8);
                default: throw new IllegalArgumentException("not supported ContentSize " + this);
            }
        }

        Path fileContent() {
            switch (this) {
                case SMALL: return testFileSmall;
                case LARGE: return testFileLarge;
                default: throw new IllegalArgumentException("not supported ContentSize " + this);
            }
        }
    }

    private static byte[] largeContent() {
        // 80 MiB
        Random r = new Random();
        byte[] b = new byte[80 * 1024 * 1024];
        r.nextBytes(b);
        return b;
    }

    private static List<UploadConfig> uploadConfigs() {
        List<UploadConfig> configs = new ArrayList<>();

        for (BodyType bodyType : BodyType.values()) {
            for (TestConfig baseConfig : testConfigs()) {
                for (ContentSize size : ContentSize.values()) {
                    UploadConfig config = new UploadConfig();
                    config.setBaseConfig(baseConfig);
                    config.setBodyType(bodyType);
                    config.setContentSize(size);
                    configs.add(config);
                }
            }
        }
        return configs;
    }

    private String putRandomObject(BucketType bucketType) {
        String key = randomKey();
        String bucketName = bucketForType(bucketType);
        s3.putObject(r -> r.bucket(bucketName).key(key), RequestBody.fromString("hello"));
        recordObjectToCleanup(bucketType, key);
        return key;
    }

    private String putRandomArchivedObject(BucketType bucketType) {
        String key = randomKey();
        String bucketName = bucketForType(bucketType);
        s3.putObject(r -> r.bucket(bucketName).key(key).storageClass(StorageClass.GLACIER), RequestBody.fromString("hello"));
        recordObjectToCleanup(bucketType, key);
        return key;
    }

    private TestRequestBody getRequestBody(BodyType bodyType, ContentSize contentSize) throws IOException {
        switch (bodyType) {
            case STRING: {
                String content = contentSize.stringContent();
                return new TestRequestBody(RequestBody.fromString(content),
                                           content.getBytes(StandardCharsets.UTF_8).length,
                                           crc32(content));
            }
            case FILE:
                return new TestRequestBody(RequestBody.fromFile(contentSize.fileContent()), Files.size(contentSize.fileContent()), crc32(contentSize.fileContent()));
            case CONTENT_PROVIDER_NO_LENGTH: {
                RequestBody wrapped =
                    RequestBody.fromContentProvider(() -> FunctionalUtils.invokeSafely(() -> Files.newInputStream(contentSize.fileContent())),
                                                    "application/octet-stream");

                return new TestRequestBody(wrapped, Files.size(contentSize.fileContent()), crc32(contentSize.fileContent()));
            }
            case CONTENT_PROVIDER_WITH_LENGTH: {
                long contentLength = Files.size(contentSize.fileContent());
                RequestBody wrapped = RequestBody.fromContentProvider(() -> FunctionalUtils.invokeSafely(() -> Files.newInputStream(contentSize.fileContent())),
                                                                          Files.size(contentSize.fileContent()),
                                                                          "application/octet-stream");
                return new TestRequestBody(wrapped, contentLength, crc32(contentSize.fileContent()));
            }
            case INPUTSTREAM_RESETABLE: {
                byte[] content = contentSize.byteContent();
                RequestBody wrapped = RequestBody.fromInputStream(new ByteArrayInputStream(content), content.length);
                return new TestRequestBody(wrapped, content.length, crc32(content));
            }
            case INPUTSTREAM_NOT_RESETABLE: {
                byte[] content = contentSize.byteContent();
                RequestBody wrapped = RequestBody.fromInputStream(new NonResettableByteStream(content), content.length);
                return new TestRequestBody(wrapped, content.length, crc32(content));
            }
            case BYTES: {
                byte[] content = contentSize.byteContent();
                RequestBody wrapped = RequestBody.fromBytes(content);
                return new TestRequestBody(wrapped, content.length, crc32(content));
            }
            case BYTE_BUFFER: {
                byte[] content = contentSize.byteContent();
                RequestBody wrapped = RequestBody.fromByteBuffer(ByteBuffer.wrap(content));
                return new TestRequestBody(wrapped, content.length, crc32(content));
            }
            case REMAINING_BYTE_BUFFER: {
                byte[] content = contentSize.byteContent();
                ByteBuffer buff = ByteBuffer.wrap(content);
                int offset = 2;
                buff.position(offset);
                RequestBody asyncRequestBody = RequestBody.fromRemainingByteBuffer(buff);
                byte[] crcArray = Arrays.copyOfRange(content, offset, content.length);
                return new TestRequestBody(asyncRequestBody, content.length - offset, crc32(crcArray));
            }
            case BUFFERS:
            case BUFFERS_REMAINING:
            case BUFFERS_UNSAFE:
            case BUFFERS_REMAINING_UNSAFE:
            case BYTES_UNSAFE:
            case BYTE_BUFFER_UNSAFE:
            case REMAINING_BYTE_BUFFER_UNSAFE:
            case BLOCKING_INPUT_STREAM:
            case BLOCKING_OUTPUT_STREAM:
                Assumptions.abort("Test BodyType not supported for sync client: " + bodyType);
            default:
                throw new RuntimeException("Unsupported body type: " + bodyType);
        }
    }

    private TestAsyncBody getAsyncRequestBody(BodyType bodyType, ContentSize contentSize) throws IOException {
        switch (bodyType) {
            case STRING: {
                String content = contentSize.stringContent();
                return new TestAsyncBody(AsyncRequestBody.fromString(content), content.getBytes(StandardCharsets.UTF_8).length, crc32(content), bodyType);
            }
            case FILE: {
                long contentLength = Files.size(contentSize.fileContent());
                return new TestAsyncBody(AsyncRequestBody.fromFile(contentSize.fileContent()), contentLength, crc32(contentSize.fileContent()), bodyType);
            }
            case INPUTSTREAM_RESETABLE: {
                byte[] content = contentSize.byteContent();
                AsyncRequestBody asyncRequestBody = AsyncRequestBody.fromInputStream(new ByteArrayInputStream(content),
                                                                                     (long) content.length,
                                                                                     ASYNC_REQUEST_BODY_EXECUTOR);
                return new TestAsyncBody(asyncRequestBody, content.length, crc32(content), bodyType);
            }
            case INPUTSTREAM_NOT_RESETABLE: {
                byte[] content = contentSize.byteContent();
                AsyncRequestBody asyncRequestBody = AsyncRequestBody.fromInputStream(new NonResettableByteStream(content),
                                                                                     (long) content.length,
                                                                                     ASYNC_REQUEST_BODY_EXECUTOR);
                return new TestAsyncBody(asyncRequestBody, content.length, crc32(content), bodyType);
            }
            case CONTENT_PROVIDER_NO_LENGTH: {
                byte[] content = contentSize.byteContent();
                Flowable<ByteBuffer> publisher = Flowable.just(ByteBuffer.wrap(content));
                AsyncRequestBody asyncRequestBody = AsyncRequestBody.fromPublisher(publisher);
                return new TestAsyncBody(asyncRequestBody, content.length, crc32(content), bodyType);
            }

            case BYTES: {
                byte[] content = contentSize.byteContent();
                AsyncRequestBody asyncRequestBody = AsyncRequestBody.fromBytes(content);
                return new TestAsyncBody(asyncRequestBody, content.length, crc32(content), bodyType);
            }
            case BYTE_BUFFER: {
                byte[] content = contentSize.byteContent();
                AsyncRequestBody asyncRequestBody = AsyncRequestBody.fromByteBuffer(ByteBuffer.wrap(content));
                return new TestAsyncBody(asyncRequestBody, content.length, crc32(content), bodyType);
            }
            case REMAINING_BYTE_BUFFER: {
                byte[] content = contentSize.byteContent();
                ByteBuffer buff = ByteBuffer.wrap(content);
                int offset = 2;
                buff.position(offset);
                AsyncRequestBody asyncRequestBody = AsyncRequestBody.fromRemainingByteBuffer(buff);
                byte[] crcArray = Arrays.copyOfRange(content, offset, content.length);
                return new TestAsyncBody(asyncRequestBody, content.length - offset, crc32(crcArray), bodyType);
            }
            case BYTES_UNSAFE:{
                byte[] content = contentSize.byteContent();
                AsyncRequestBody asyncRequestBody = AsyncRequestBody.fromBytesUnsafe(content);
                return new TestAsyncBody(asyncRequestBody, content.length, crc32(content), bodyType);
            }
            case BYTE_BUFFER_UNSAFE: {
                byte[] content = contentSize.byteContent();
                AsyncRequestBody asyncRequestBody = AsyncRequestBody.fromByteBufferUnsafe(ByteBuffer.wrap(content));
                return new TestAsyncBody(asyncRequestBody, content.length, crc32(content), bodyType);
            }
            case REMAINING_BYTE_BUFFER_UNSAFE: {
                byte[] content = contentSize.byteContent();
                ByteBuffer buff = ByteBuffer.wrap(content);
                int offset = 2;
                buff.position(offset);
                AsyncRequestBody asyncRequestBody = AsyncRequestBody.fromRemainingByteBufferUnsafe(buff);
                byte[] crcArray = Arrays.copyOfRange(content, offset, content.length);
                return new TestAsyncBody(asyncRequestBody, content.length - offset, crc32(crcArray), bodyType);
            }
            case BUFFERS: {
                byte[] content1 = contentSize.byteContent();
                byte[] content2 = contentSize.byteContent();
                AsyncRequestBody asyncRequestBody = AsyncRequestBody.fromByteBuffers(ByteBuffer.wrap(content1),
                                                                                     ByteBuffer.wrap(content2));
                byte[] crcArray = new byte[content2.length + content2.length];
                System.arraycopy(content1, 0, crcArray, 0, content1.length);
                System.arraycopy(content2, 0, crcArray, content1.length, content2.length);
                return new TestAsyncBody(asyncRequestBody,
                                         content1.length + content2.length,
                                         crc32(crcArray),
                                         bodyType);
            }
            case BUFFERS_REMAINING: {
                byte[] content1 = contentSize.byteContent();
                byte[] content2 = contentSize.byteContent();
                AsyncRequestBody asyncRequestBody = AsyncRequestBody.fromRemainingByteBuffers(ByteBuffer.wrap(content1),
                                                                                     ByteBuffer.wrap(content2));
                byte[] crcArray = new byte[content2.length + content2.length];
                System.arraycopy(content1, 0, crcArray, 0, content1.length);
                System.arraycopy(content2, 0, crcArray, content1.length, content2.length);
                return new TestAsyncBody(asyncRequestBody,
                                         content1.length + content2.length,
                                         crc32(crcArray),
                                         bodyType);
            }
            case BUFFERS_UNSAFE: {
                byte[] content1 = contentSize.byteContent();
                byte[] content2 = contentSize.byteContent();
                AsyncRequestBody asyncRequestBody = AsyncRequestBody.fromByteBuffersUnsafe(ByteBuffer.wrap(content1),
                                                                                           ByteBuffer.wrap(content2));
                byte[] crcArray = new byte[content2.length + content2.length];
                System.arraycopy(content1, 0, crcArray, 0, content1.length);
                System.arraycopy(content2, 0, crcArray, content1.length, content2.length);
                return new TestAsyncBody(asyncRequestBody,
                                         content1.length + content2.length,
                                         crc32(crcArray),
                                         bodyType);
            }
            case BUFFERS_REMAINING_UNSAFE: {
                byte[] content1 = contentSize.byteContent();
                byte[] content2 = contentSize.byteContent();
                AsyncRequestBody asyncRequestBody = AsyncRequestBody.fromRemainingByteBuffersUnsafe(ByteBuffer.wrap(content1),
                                                                                                    ByteBuffer.wrap(content2));
                byte[] crcArray = new byte[content2.length + content2.length];
                System.arraycopy(content1, 0, crcArray, 0, content1.length);
                System.arraycopy(content2, 0, crcArray, content1.length, content2.length);
                return new TestAsyncBody(asyncRequestBody,
                                         content1.length + content2.length,
                                         crc32(crcArray),
                                         bodyType);
            }
            case BLOCKING_INPUT_STREAM: {
                byte[] content = contentSize.byteContent();
                long streamToSendLength = content.length;
                BlockingInputStreamAsyncRequestBody body = AsyncRequestBody.forBlockingInputStream(streamToSendLength);
                return new TestAsyncBodyForBlockingInputStream(body,
                                                               new ByteArrayInputStream(content),
                                                               content.length,
                                                               crc32(content),
                                                               bodyType);
            }
            case BLOCKING_OUTPUT_STREAM: {
                byte[] content = contentSize.byteContent();
                long streamToSendLength = content.length;
                BlockingOutputStreamAsyncRequestBody body = AsyncRequestBody.forBlockingOutputStream(streamToSendLength);
                Consumer<CancellableOutputStream> bodyWrite = outputStream -> {
                    try {
                        outputStream.write(content);
                    } catch (IOException ioe) {
                        throw new RuntimeException(ioe);
                    }
                };
                return new TestAsyncBodyForBlockingOutputStream(body,
                                                                bodyWrite,
                                                                content.length,
                                                                crc32(content),
                                                                bodyType);
            }
            default:
                throw new RuntimeException("Unsupported async body type: " + bodyType);
        }
    }

    private String randomKey() {
        return BinaryUtils.toHex(UUID.randomUUID().toString().getBytes());
    }

    private static String getBucketName() {
        return BUCKET_NAME_PREFIX + accountId;
    }

    private static String waitForApToBeReady() {
        return s3Control.getAccessPoint(r -> r.accountId(accountId).name(AP_NAME)).accessPointArn();
    }

    private static Path createRandomFile16KB() throws IOException {
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

    private static Path createRandomFile80MB() throws IOException {
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

    private static class NonResettableByteStream extends ByteArrayInputStream {
        public NonResettableByteStream(byte[] buf) {
            super(buf);
        }

        @Override
        public boolean markSupported() {
            return false;
        }

        @Override
        public synchronized void reset() {
            throw new UnsupportedOperationException();
        }
    }

    private void recordObjectToCleanup(BucketType type, String key) {
        bucketCleanup.computeIfAbsent(type, k -> new ArrayList<>()).add(key);
    }

    private static class RequestRecorder implements ExecutionInterceptor {
        private final List<SdkHttpRequest> requests = new ArrayList<>();
        @Override
        public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
            requests.add(context.httpRequest());
        }

        public List<SdkHttpRequest> getRequests() {
            return requests;
        }
    }

    private static class EnablePayloadSigningInterceptor implements ExecutionInterceptor {
        @Override
        public void beforeExecution(Context.BeforeExecution context, ExecutionAttributes executionAttributes) {
            executionAttributes.putAttribute(S3SignerExecutionAttribute.ENABLE_PAYLOAD_SIGNING, true);
            ExecutionInterceptor.super.beforeExecution(context, executionAttributes);
        }
    }
}
