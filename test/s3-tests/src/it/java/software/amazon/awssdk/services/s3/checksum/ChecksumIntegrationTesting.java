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
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.BlockingInputStreamAsyncRequestBody;
import software.amazon.awssdk.core.async.BlockingOutputStreamAsyncRequestBody;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
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
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.CancellableOutputStream;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.FunctionalUtils;
import software.amazon.awssdk.utils.Logger;

// Running putObject with config:
//   UploadConfig{
//     baseConfig=[
//       flavor=ASYNC_JAVA_BASED_MULTI,
//       bucketType=STANDARD_BUCKET,
//       forcePathStyle=true,
//       requestChecksumValidation=WHEN_REQUIRED,
//       accelerateEnabled=false,
//       payloadSigning=false
//     ],
//     bodyType=BLOCKING_OUTPUT_STREAM,
//     contentSize=SMALL
//   }
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

    private static final SdkChecksum CRC32 = SdkChecksum.forAlgorithm(DefaultChecksumAlgorithm.CRC32);

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

        accountId = getAccountId();

        bucketName = createBucket();

        mrapArn = createMrap();

        eozBucket = createEozBucket();

        apArn = createAccessPoint();

        testFileSmall = createRandomFile16KB();
        testFileLarge = createRandomFile200MB();
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

    private void assumeNotAccessPointWithPathStyle(TestConfig config) {
        BucketType bucketType = config.getBucketType();
        Assumptions.assumeFalse(config.isForcePathStyle() && bucketType.isArnType(),
                                "Path style doesn't work with ARN type buckets");
    }

    private void assumeNotAccelerateWithPathStyle(TestConfig config) {
        Assumptions.assumeFalse(config.isForcePathStyle() && config.isAccelerateEnabled(),
                                "Path style doesn't work with Accelerate");
    }

    private void assumeNotAccelerateWithArnType(TestConfig config) {
        Assumptions.assumeFalse(config.isAccelerateEnabled() && config.getBucketType().isArnType(),
                                "Accelerate doesn't work with ARN buckets");
    }

    private void assumeNotAccelerateWithEoz(TestConfig config) {
        Assumptions.assumeFalse(config.isAccelerateEnabled() && config.getBucketType() == BucketType.EOZ,
                                "Accelerate is not supported with Express One Zone");
    }

    // Request checksum required
    @ParameterizedTest
    @MethodSource("testConfigs")
    void deleteObject(TestConfig config) throws Exception {
        LOG.debug(() -> "Running deleteObject with config: " + config.toString());
        assumeNotAccessPointWithPathStyle(config);
        assumeNotAccelerateWithPathStyle(config);
        assumeNotAccelerateWithArnType(config);
        assumeNotAccelerateWithEoz(config);

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
        LOG.debug(() -> "Running restoreObject with config: " + config.toString());
        assumeNotAccessPointWithPathStyle(config);
        assumeNotAccelerateWithPathStyle(config);
        assumeNotAccelerateWithArnType(config);

        Assumptions.assumeFalse(config.getBucketType() == BucketType.EOZ,
                                "Restore is not supported for S3 Express");

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

    // @ParameterizedTest
    // @MethodSource("")
    // void getObject(TestConfig config) {
    //     LOG.debug(() -> "Running putObject with config: " + config.toString());
    //
    // }

    @ParameterizedTest
    @MethodSource("uploadConfigs")
    void putObject(UploadConfig config) throws Exception {
        LOG.debug(() -> "Running putObject with config: " + config.toString());
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
            (config.getBaseConfig().getFlavor() == S3ClientFlavor.ASYNC_JAVA_BASED
             // || config.getBaseConfig().getFlavor() == S3ClientFlavor.ASYNC_JAVA_BASED_MULTI
            )
                                && (config.getBaseConfig().isPayloadSigning()
                                    // MRAP requires body signing
                                    || config.getBaseConfig().getBucketType() == BucketType.MRAP),
                                "Async payload signing doesn't work with Java based clients");

        // For testing purposes, ContentProvider is Publisher<ByteBuffer> for async clients
        // Async java based clients don't currently support unknown content-length bodies
        Assumptions.assumeFalse(
            (config.getBaseConfig().getFlavor() == S3ClientFlavor.ASYNC_JAVA_BASED
             // || config.getBaseConfig().getFlavor() == S3ClientFlavor.ASYNC_JAVA_BASED_MULTI
            )
                                && config.getBodyType() == BodyType.CONTENT_PROVIDER_NO_LENGTH,
                                "Async Java based support unknown content length");

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
            } else {
                TestAsyncBody body = getAsyncRequestBody(config.getBodyType(), config.contentSize);
                callable = callPutObject(request, body, config.getBaseConfig(), overrideConfiguration.build());
                actualContentLength = body.getActualContentLength();
                requestBodyHasContentLength = body.getAsyncRequestBody().contentLength().isPresent();
                actualCrc32 = body.getChecksum();
            }

            PutObjectResponse response = callable.runnable.call();

            recordObjectToCleanup(bucketType, key);

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
                                       ClientOverrideConfiguration overrideConfiguration) throws IOException {
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
                                       ClientOverrideConfiguration overrideConfiguration) throws IOException {
        S3AsyncClient s3Client = makeAsyncClient(config, overrideConfiguration);
        Callable<PutObjectResponse> callable = () -> {
            try {
                AsyncRequestBody asyncRequestBody = requestBody.getAsyncRequestBody();
                CompletableFuture<PutObjectResponse> future = s3Client.putObject(request, asyncRequestBody);
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
                return CompletableFutureUtils.joinLikeSync(future);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
        return new TestCallable<>(s3Client, callable);
    }

    private static class TestCallable<ResponseT> {
        private AwsClient client;
        private Callable<ResponseT> runnable;

        TestCallable(AwsClient client, Callable<ResponseT> runnable) {
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
            // case ASYNC_JAVA_BASED_MULTI:
            //     return S3AsyncClient.builder()
            //         .forcePathStyle(config.isForcePathStyle())
            //         .requestChecksumCalculation(config.getRequestChecksumValidation())
            //         .region(REGION)
            //         .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
            //         .accelerate(config.isAccelerateEnabled())
            //         .overrideConfiguration(overrideConfiguration)
            //         .multipartEnabled(true)
            //         .build();
            case ASYNC_CRT: {
                if (overrideConfiguration != null) {
                    LOG.warn(() -> "Override configuration cannot be set for Async S3 CRT!");
                }
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
        STANDARD_BUCKET(false),
        ACCESS_POINT(true),
        // Multi-region access point
        MRAP(true),
        // Express one zone/S3 express
        EOZ(false),
        ;

        private final boolean arnType;

        private BucketType(boolean arnType) {
            this.arnType = arnType;
        }

        public boolean isArnType() {
            return arnType;
        }
    }

    enum S3ClientFlavor {
        JAVA_BASED(false),
        ASYNC_JAVA_BASED(true),
        // ASYNC_JAVA_BASED_MULTI(true),

        ASYNC_CRT(true)
        ;

        private final boolean async;

        private S3ClientFlavor(boolean async) {
            this.async = async;
        }

        public boolean isAsync() {
            return async;
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

    enum ResponseTransformerType {
        FILE,
        BYTES,
        INPUT_STREAM,
        OUTPUT_STREAM,
        UNMANAGED,
        PUBLISHER
    }

    static class DownloadConfig {
        private TestConfig testConfig;
        private ResponseTransformerType responseTransformerType;
        private ContentSize contentSize;

        public DownloadConfig(TestConfig testConfig, ResponseTransformerType responseTransformerType, ContentSize contentSize) {
            this.testConfig = testConfig;
            this.responseTransformerType = responseTransformerType;
            this.contentSize = contentSize;
        }

        public TestConfig getTestConfig() {
            return this.testConfig;
        }

        public void setTestConfig(TestConfig testConfig) {
            this.testConfig = testConfig;
        }

        public ResponseTransformerType getResponseTransformerType() {
            return responseTransformerType;
        }

        public void setResponseTransformerType(ResponseTransformerType responseTransformerType) {
            this.responseTransformerType = responseTransformerType;
        }

        public ContentSize getContentSize() {
            return contentSize;
        }

        public void setContentSize(ContentSize contentSize) {
            this.contentSize = contentSize;
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

    static class TestConfig {
        private S3ClientFlavor flavor;
        private BucketType bucketType;
        private boolean forcePathStyle;
        private RequestChecksumCalculation requestChecksumValidation;
        private boolean accelerateEnabled;
        private boolean payloadSigning;

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

        public boolean isAccelerateEnabled() {
            return accelerateEnabled;
        }

        public void setAccelerateEnabled(boolean accelerateEnabled) {
            this.accelerateEnabled = accelerateEnabled;
        }

        public boolean isPayloadSigning() {
            return payloadSigning;
        }

        public void setPayloadSigning(boolean payloadSigning) {
            this.payloadSigning = payloadSigning;
        }

        @Override
        public String toString() {
            return "[" +
                   "flavor=" + flavor +
                   ", bucketType=" + bucketType +
                   ", forcePathStyle=" + forcePathStyle +
                   ", requestChecksumValidation=" + requestChecksumValidation +
                   ", accelerateEnabled=" + accelerateEnabled +
                   ", payloadSigning=" + payloadSigning +
                   ']';
        }
    }

    static List<TestConfig> testConfigs() {
        List<TestConfig> configs = new ArrayList<>();

        boolean[] forcePathStyle = {true, false};
        RequestChecksumCalculation[] checksumValidations = {RequestChecksumCalculation.WHEN_REQUIRED,
                                                            RequestChecksumCalculation.WHEN_SUPPORTED};
        boolean[] accelerateEnabled = {true, false};
        boolean[] payloadSigningEnabled = {true, false};
        for (boolean pathStyle : forcePathStyle) {
            for (RequestChecksumCalculation checksumValidation : checksumValidations) {
                for (S3ClientFlavor flavor : S3ClientFlavor.values()) {
                    for (BucketType bucketType : BucketType.values()) {
                        for (boolean accelerate : accelerateEnabled) {
                            for (boolean payloadSigning : payloadSigningEnabled) {
                                TestConfig testConfig = new TestConfig();
                                testConfig.setFlavor(flavor);
                                testConfig.setBucketType(bucketType);
                                testConfig.setForcePathStyle(pathStyle);
                                testConfig.setRequestChecksumValidation(checksumValidation);
                                testConfig.setAccelerateEnabled(accelerate);
                                testConfig.setPayloadSigning(payloadSigning);
                                configs.add(testConfig);
                            }
                        }
                    }
                }
            }
        }

        return configs;
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


        byte[] content() {
            switch (this) {
                case SMALL: return smallContent;
                case LARGE: return largeContent;
                default: throw new IllegalArgumentException("not supported ContentSize " + this);
            }
        }
    }

    private static byte[] largeContent() {
        // 200 MiB
        Random r = new Random();
        // byte[] b = new byte[200 * 1024 * 1024];
        byte[] b = new byte[2 * 1024 * 1024];
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

    private static List<DownloadConfig> downloadConfigs() {
        List<DownloadConfig> configs = new ArrayList<>();
        for (ResponseTransformerType responseTransformerType : ResponseTransformerType.values()) {
            for (TestConfig baseConfig : testConfigs()) {
                for (ContentSize contentSize : ContentSize.values()) {
                    DownloadConfig config = new DownloadConfig(baseConfig, responseTransformerType, contentSize);
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
                byte[] content = contentSize.content();
                long contentLength = content.length;
                return new TestRequestBody(RequestBody.fromString(new String(content, StandardCharsets.UTF_8)),
                                           contentLength,
                                           crc32(content));
            }
            case FILE:
                return new TestRequestBody(RequestBody.fromFile(testFileSmall), Files.size(testFileSmall), crc32(testFileSmall));
            case CONTENT_PROVIDER_NO_LENGTH: {
                RequestBody wrapped =
                    RequestBody.fromContentProvider(() -> FunctionalUtils.invokeSafely(() -> Files.newInputStream(testFileSmall)),
                                                    "application/octet-stream");

                return new TestRequestBody(wrapped, Files.size(testFileSmall), crc32(testFileSmall));
            }
            case CONTENT_PROVIDER_WITH_LENGTH: {
                long contentLength = Files.size(testFileSmall);
                RequestBody wrapped = RequestBody.fromContentProvider(() -> FunctionalUtils.invokeSafely(() -> Files.newInputStream(testFileSmall)),
                                                                          Files.size(testFileSmall),
                                                                          "application/octet-stream");
                return new TestRequestBody(wrapped, contentLength, crc32(testFileSmall));
            }
            case INPUTSTREAM_RESETABLE: {
                byte[] content = contentSize.content();
                RequestBody wrapped = RequestBody.fromInputStream(new ByteArrayInputStream(content), content.length);
                return new TestRequestBody(wrapped, content.length, crc32(content));
            }
            case INPUTSTREAM_NOT_RESETABLE: {
                byte[] content = contentSize.content();
                RequestBody wrapped = RequestBody.fromInputStream(new NonResettableByteStream(content), content.length);
                return new TestRequestBody(wrapped, content.length, crc32(content));
            }
            case BYTES: {
                byte[] content = contentSize.content();
                RequestBody wrapped = RequestBody.fromBytes(content);
                return new TestRequestBody(wrapped, content.length, crc32(content));
            }
            case BYTE_BUFFER: {
                byte[] content = contentSize.content();
                RequestBody wrapped = RequestBody.fromByteBuffer(ByteBuffer.wrap(content));
                return new TestRequestBody(wrapped, content.length, crc32(content));
            }
            case REMAINING_BYTE_BUFFER: {
                byte[] content = contentSize.content();
                ByteBuffer buff = ByteBuffer.wrap(content);
                int offset = 2;
                buff.position(offset);
                RequestBody asyncRequestBody = RequestBody.fromRemainingByteBuffer(buff);
                byte[] crcArray = new byte[content.length - offset];
                System.arraycopy(content, offset, crcArray, 0, crcArray.length);
                return new TestRequestBody(asyncRequestBody, content.length, crc32(crcArray));
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
                byte[] content = contentSize.content();
                long contentLength = content.length;
                return new TestAsyncBody(AsyncRequestBody.fromString(new String(content)), contentLength, crc32(content), bodyType);
            }
            case FILE: {
                long contentLength = Files.size(testFileSmall);
                return new TestAsyncBody(AsyncRequestBody.fromFile(testFileSmall), contentLength, crc32(testFileSmall), bodyType);
            }
            case INPUTSTREAM_RESETABLE: {
                byte[] content = contentSize.content();
                AsyncRequestBody asyncRequestBody = AsyncRequestBody.fromInputStream(new ByteArrayInputStream(content),
                                                                                     (long) content.length,
                                                                                     ASYNC_REQUEST_BODY_EXECUTOR);
                return new TestAsyncBody(asyncRequestBody, content.length, crc32(content), bodyType);
            }
            case INPUTSTREAM_NOT_RESETABLE: {
                byte[] content = contentSize.content();
                AsyncRequestBody asyncRequestBody = AsyncRequestBody.fromInputStream(new NonResettableByteStream(content),
                                                                                     (long) content.length,
                                                                                     ASYNC_REQUEST_BODY_EXECUTOR);
                return new TestAsyncBody(asyncRequestBody, content.length, crc32(content), bodyType);
            }
            case CONTENT_PROVIDER_NO_LENGTH: {
                byte[] content = contentSize.content();
                Flowable<ByteBuffer> publisher = Flowable.just(ByteBuffer.wrap(content));
                AsyncRequestBody asyncRequestBody = AsyncRequestBody.fromPublisher(publisher);
                return new TestAsyncBody(asyncRequestBody, content.length, crc32(content), bodyType);
            }

            case BYTES: {
                byte[] content = contentSize.content();
                AsyncRequestBody asyncRequestBody = AsyncRequestBody.fromBytes(content);
                return new TestAsyncBody(asyncRequestBody, content.length, crc32(content), bodyType);
            }
            case BYTE_BUFFER: {
                byte[] content = contentSize.content();
                AsyncRequestBody asyncRequestBody = AsyncRequestBody.fromByteBuffer(ByteBuffer.wrap(content));
                return new TestAsyncBody(asyncRequestBody, content.length, crc32(content), bodyType);
            }
            case REMAINING_BYTE_BUFFER: {
                byte[] content = contentSize.content();
                ByteBuffer buff = ByteBuffer.wrap(content);
                int offset = 2;
                buff.position(offset);
                AsyncRequestBody asyncRequestBody = AsyncRequestBody.fromRemainingByteBuffer(buff);
                byte[] crcArray = new byte[content.length - offset];
                System.arraycopy(content, offset, crcArray, 0, crcArray.length);
                return new TestAsyncBody(asyncRequestBody, content.length, crc32(crcArray), bodyType);
            }
            case BYTES_UNSAFE:{
                byte[] content = contentSize.content();
                AsyncRequestBody asyncRequestBody = AsyncRequestBody.fromBytesUnsafe(content);
                return new TestAsyncBody(asyncRequestBody, content.length, crc32(content), bodyType);
            }
            case BYTE_BUFFER_UNSAFE: {
                byte[] content = contentSize.content();
                AsyncRequestBody asyncRequestBody = AsyncRequestBody.fromByteBufferUnsafe(ByteBuffer.wrap(content));
                return new TestAsyncBody(asyncRequestBody, content.length, crc32(content), bodyType);
            }
            case REMAINING_BYTE_BUFFER_UNSAFE: {
                byte[] content = contentSize.content();
                ByteBuffer buff = ByteBuffer.wrap(content);
                int offset = 2;
                buff.position(offset);
                AsyncRequestBody asyncRequestBody = AsyncRequestBody.fromRemainingByteBufferUnsafe(buff);
                byte[] crcArray = new byte[content.length - offset];
                System.arraycopy(content, 2, crcArray, 0, crcArray.length);
                return new TestAsyncBody(asyncRequestBody, content.length, crc32(crcArray), bodyType);
            }
            case BUFFERS: {
                byte[] content1 = contentSize.content();
                byte[] content2 = contentSize.content();
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
                byte[] content1 = contentSize.content();
                byte[] content2 = contentSize.content();
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
                byte[] content1 = contentSize.content();
                byte[] content2 = contentSize.content();
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
                byte[] content1 = contentSize.content();
                byte[] content2 = contentSize.content();
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
                byte[] content = contentSize.content();
                long streamToSendLength = content.length;
                BlockingInputStreamAsyncRequestBody body = AsyncRequestBody.forBlockingInputStream(streamToSendLength);
                return new TestAsyncBodyForBlockingInputStream(body,
                                                               new ByteArrayInputStream(content),
                                                               content.length,
                                                               crc32(content),
                                                               bodyType);
            }
            case BLOCKING_OUTPUT_STREAM: {
                byte[] content = contentSize.content();
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
        s3.putBucketAccelerateConfiguration(r -> r.bucket(name)
                                                  .accelerateConfiguration(c -> c.status(BucketAccelerateStatus.ENABLED)));
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
            if ("BucketAlreadyOwnedByYou".equals(e.awsErrorDetails().errorCode())) {
                LOG.debug(() -> String.format("%s bucket already exists, likely leaked by a previous run%n", bucketName));
            } else if ("TooManyBuckets".equals(e.awsErrorDetails().errorCode())) {
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

    private static Path createRandomFile200MB() throws IOException {
        Path tmp = Files.createTempFile(null, null);
        byte[] randomBytes = new byte[1024 * 1024];
        new Random().nextBytes(randomBytes);
        try (OutputStream os = Files.newOutputStream(tmp)) {
            for (int i = 0; i < 200; ++i) {
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

    private static String crc32(String s) {
        return crc32(s.getBytes(StandardCharsets.UTF_8));
    }

    private static String crc32(byte[] bytes) {
        CRC32.reset();
        CRC32.update(bytes);
        return BinaryUtils.toBase64(CRC32.getChecksumBytes());
    }

    private static String crc32(Path p) throws IOException {
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
