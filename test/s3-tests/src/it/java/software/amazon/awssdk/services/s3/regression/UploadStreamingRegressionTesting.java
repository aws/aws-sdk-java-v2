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

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.services.s3.regression.S3ChecksumsTestUtils.assumeNotAccessPointWithPathStyle;
import static software.amazon.awssdk.services.s3.regression.S3ChecksumsTestUtils.crc32;
import static software.amazon.awssdk.services.s3.regression.S3ChecksumsTestUtils.makeAsyncClient;
import static software.amazon.awssdk.services.s3.regression.S3ChecksumsTestUtils.makeSyncClient;
import static software.amazon.awssdk.services.s3.regression.S3ClientFlavor.MULTIPART_ENABLED;
import static software.amazon.awssdk.services.s3.regression.S3ClientFlavor.STANDARD_ASYNC;
import static software.amazon.awssdk.services.s3.regression.TestConfig.testConfigs;

import io.reactivex.Flowable;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.auth.signer.S3SignerExecutionAttribute;
import software.amazon.awssdk.core.ResponseInputStream;
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
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ChecksumMode;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.CompletedUpload;
import software.amazon.awssdk.transfer.s3.model.Upload;
import software.amazon.awssdk.utils.CancellableOutputStream;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.FunctionalUtils;
import software.amazon.awssdk.utils.Logger;

public class UploadStreamingRegressionTesting extends BaseS3RegressionTest {
    private static final Logger LOG = Logger.loggerFor(UploadStreamingRegressionTesting.class);

    private static final ExecutorService ASYNC_REQUEST_BODY_EXECUTOR = Executors.newSingleThreadExecutor();

    static final byte[] smallContent = "Hello world".getBytes(StandardCharsets.UTF_8);
    static final byte[] largeContent = largeContent();
    static final String smallContentCrc32 = crc32(smallContent);
    static final String largeContentCrc32 = crc32(largeContent);

    static String smallContentCRC32ForBuffersAPI;
    static String largeContentCRC32ForBuffersAPI;

    private static Path testFileSmall;
    private static Path testFileLarge;

    @BeforeAll
    static void setupClass() throws IOException {
        testFileSmall = S3ChecksumsTestUtils.createRandomFile16KB();
        testFileLarge = S3ChecksumsTestUtils.createRandomFile80MB();

        // used in RequestBody.*buffers(...) API
        // we calculate crc32 once to try to accelerate test execution
        byte[] crcArraySmallContentForBuffersApi = new byte[smallContent.length + smallContent.length];
        System.arraycopy(smallContent, 0, crcArraySmallContentForBuffersApi, 0, smallContent.length);
        System.arraycopy(smallContent, 0, crcArraySmallContentForBuffersApi, smallContent.length, smallContent.length);
        smallContentCRC32ForBuffersAPI = crc32(crcArraySmallContentForBuffersApi);

        byte[] crcArrayLargeContentForBuffersApi = new byte[largeContent.length + largeContent.length];
        System.arraycopy(largeContent, 0, crcArrayLargeContentForBuffersApi, 0, largeContent.length);
        System.arraycopy(largeContent, 0, crcArrayLargeContentForBuffersApi, largeContent.length, largeContent.length);
        largeContentCRC32ForBuffersAPI = crc32(crcArrayLargeContentForBuffersApi);
    }

    @AfterAll
    public static void cleanup() {
        ASYNC_REQUEST_BODY_EXECUTOR.shutdownNow();
    }

    @ParameterizedTest
    @MethodSource("uploadConfigs")
    void putObject(UploadConfig config) throws Exception {
        assumeNotAccessPointWithPathStyle(config.getBaseConfig());

        // For testing purposes, ContentProvider is Publisher<ByteBuffer> for async clients
        // There is no way to create AsyncRequestBody with a Publisher<ByteBuffer> and also provide the content length
        S3ClientFlavor flavor = config.getBaseConfig().getFlavor();
        Assumptions.assumeFalse(config.getBodyType() == BodyType.CONTENT_PROVIDER_WITH_LENGTH
                                && flavor.isAsync(),
                                "No way to create AsyncRequestBody by giving both an Publisher and the content length");

        // Payload signing doesn't work correctly for async java based
        // TODO(sra-identity-auth) remove when chunked encoding support is added in async code path
        Assumptions.assumeFalse(
            (flavor == STANDARD_ASYNC || flavor == MULTIPART_ENABLED)
            && (config.payloadSigning()
                // MRAP requires body signing
                || config.getBaseConfig().getBucketType() == BucketType.MRAP),
            "Async payload signing doesn't work with Java based clients");

        // For testing purposes, ContentProvider is Publisher<ByteBuffer> for async clients
        // Async java based clients don't currently support unknown content-length bodies
        Assumptions.assumeFalse(
            flavor == STANDARD_ASYNC
            && config.getBodyType() == BodyType.CONTENT_PROVIDER_NO_LENGTH,
            "Async Java based support unknown content length");

        LOG.info(() -> "Running putObject with config: " + config);

        BucketType bucketType = config.getBaseConfig().getBucketType();

        String bucket = bucketForType(bucketType);
        String key = S3ChecksumsTestUtils.randomKey();

        PutObjectRequest request = PutObjectRequest.builder()
                                                   .bucket(bucket)
                                                   .key(key)
                                                   .build();


        RequestRecorder recorder = new RequestRecorder();

        ClientOverrideConfiguration.Builder overrideConfiguration =
            ClientOverrideConfiguration.builder()
                                       .addExecutionInterceptor(recorder);

        if (config.payloadSigning()) {
            overrideConfiguration.addExecutionInterceptor(new EnablePayloadSigningInterceptor());
        }

        TestCallable<PutObjectResponse> callable = null;
        try {

            Long actualContentLength = null;
            boolean requestBodyHasContentLength = false;
            String actualCrc32;

            if (!flavor.isAsync()) {
                TestRequestBody body = getRequestBody(config.getBodyType(), config.getContentSize());
                callable = callPutObject(request, body, config.getBaseConfig(), overrideConfiguration.build());
                actualContentLength = body.getActualContentLength();
                requestBodyHasContentLength = body.optionalContentLength().isPresent();
                actualCrc32 = body.getChecksum();
            } else if (flavor == MULTIPART_ENABLED) {
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

            PutObjectResponse response = callable.runnable().call();

            recordObjectToCleanup(bucketType, key);

            // mpu not supported
            if (flavor == MULTIPART_ENABLED) {
                return;
            }

            // We only validate when configured to WHEN_SUPPORTED since checksums are optional for PutObject
            if (config.getBaseConfig().getRequestChecksumValidation() == RequestChecksumCalculation.WHEN_SUPPORTED
                // CRT switches to MPU under the hood which doesn't support checksums
                && flavor != S3ClientFlavor.CRT_BASED) {
                assertThat(response.checksumCRC32()).isEqualTo(actualCrc32);
            }

            // We can't set an execution interceptor when using CRT
            if (flavor == S3ClientFlavor.CRT_BASED) {
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
                    verifyChecksumResponsePayload(config, key, actualCrc32);
                } else {
                    Optional<String> contentLength = httpRequest.firstMatchingHeader("Content-Length");
                    if (requestBodyHasContentLength) {
                        assertThat(Long.parseLong(contentLength.get())).isEqualTo(actualContentLength);
                    }
                }
            }

        } finally {
            if (callable != null) {
                callable.client().close();
            }
        }
    }

    private void verifyChecksumResponsePayload(UploadConfig config, String key, String expectedCRC32) {
        String bucket = bucketForType(config.getBaseConfig().getBucketType());
        ResponseInputStream<GetObjectResponse> response = s3.getObject(req -> req.checksumMode(ChecksumMode.ENABLED)
                                                                                 .key(key)
                                                                                 .bucket(bucket));
        assertThat(response.response().checksumCRC32()).isEqualTo(expectedCRC32);

    }

    private TestCallable<PutObjectResponse> callPutObject(PutObjectRequest request, TestRequestBody requestBody,
                                                          TestConfig config,
                                                          ClientOverrideConfiguration overrideConfiguration) {
        S3Client s3Client = makeSyncClient(config, overrideConfiguration, REGION, CREDENTIALS_PROVIDER_CHAIN);
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
        S3AsyncClient s3Client = makeAsyncClient(config, overrideConfiguration, REGION, CREDENTIALS_PROVIDER_CHAIN);
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
        S3TransferManager transferManager = S3ChecksumsTestUtils.makeTm(config, overrideConfiguration,
                                                                        REGION, CREDENTIALS_PROVIDER_CHAIN);
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

    private TestRequestBody getRequestBody(BodyType bodyType, ContentSize contentSize) throws IOException {
        switch (bodyType) {
            case STRING: {
                String content = contentSize.stringContent();
                return new TestRequestBody(RequestBody.fromString(content),
                                           content.getBytes(StandardCharsets.UTF_8).length,
                                           crc32(content));
            }
            case FILE:
                return new TestRequestBody(RequestBody.fromFile(contentSize.fileContent()),
                                           Files.size(contentSize.fileContent()), crc32(contentSize.fileContent()));
            case CONTENT_PROVIDER_NO_LENGTH: {
                RequestBody wrapped =
                    RequestBody.fromContentProvider(() -> FunctionalUtils.invokeSafely(() -> Files.newInputStream(contentSize.fileContent())),
                                                    "application/octet-stream");

                return new TestRequestBody(wrapped, Files.size(contentSize.fileContent()), crc32(contentSize.fileContent()));
            }
            case CONTENT_PROVIDER_WITH_LENGTH: {
                long contentLength = Files.size(contentSize.fileContent());
                RequestBody wrapped =
                    RequestBody.fromContentProvider(() -> FunctionalUtils.invokeSafely(() -> Files.newInputStream(contentSize.fileContent())),
                                                    Files.size(contentSize.fileContent()),
                                                    "application/octet-stream");
                return new TestRequestBody(wrapped, contentLength, crc32(contentSize.fileContent()));
            }
            case INPUTSTREAM_RESETABLE: {
                byte[] content = contentSize.byteContent();
                RequestBody wrapped = RequestBody.fromInputStream(new ByteArrayInputStream(content), content.length);
                return new TestRequestBody(wrapped, content.length, contentSize.precalculatedCrc32());
            }
            case INPUTSTREAM_NOT_RESETABLE: {
                byte[] content = contentSize.byteContent();
                RequestBody wrapped = RequestBody.fromInputStream(new NonResettableByteStream(content), content.length);
                return new TestRequestBody(wrapped, content.length, contentSize.precalculatedCrc32());
            }
            case BYTES: {
                byte[] content = contentSize.byteContent();
                RequestBody wrapped = RequestBody.fromBytes(content);
                return new TestRequestBody(wrapped, content.length, contentSize.precalculatedCrc32());
            }
            case BYTE_BUFFER: {
                byte[] content = contentSize.byteContent();
                RequestBody wrapped = RequestBody.fromByteBuffer(ByteBuffer.wrap(content));
                return new TestRequestBody(wrapped, content.length, contentSize.precalculatedCrc32());
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
            case INPUTSTREAM_NO_LENGTH:
                Assumptions.abort("Test BodyType not supported for sync client: " + bodyType);
            default:
                throw new RuntimeException("Unsupported body type: " + bodyType);
        }
    }

    private TestAsyncBody getAsyncRequestBody(BodyType bodyType, ContentSize contentSize) throws IOException {
        switch (bodyType) {
            case STRING: {
                String content = contentSize.stringContent();
                return new TestAsyncBody(AsyncRequestBody.fromString(content), content.getBytes(StandardCharsets.UTF_8).length,
                                         crc32(content), bodyType);
            }
            case FILE: {
                long contentLength = Files.size(contentSize.fileContent());
                return new TestAsyncBody(AsyncRequestBody.fromFile(contentSize.fileContent()), contentLength,
                                         crc32(contentSize.fileContent()), bodyType);
            }
            case INPUTSTREAM_RESETABLE: {
                byte[] content = contentSize.byteContent();
                AsyncRequestBody asyncRequestBody = AsyncRequestBody.fromInputStream(new ByteArrayInputStream(content),
                                                                                     (long) content.length,
                                                                                     ASYNC_REQUEST_BODY_EXECUTOR);
                return new TestAsyncBody(asyncRequestBody, content.length, contentSize.precalculatedCrc32(), bodyType);
            }
            case INPUTSTREAM_NOT_RESETABLE: {
                byte[] content = contentSize.byteContent();
                AsyncRequestBody asyncRequestBody = AsyncRequestBody.fromInputStream(new NonResettableByteStream(content),
                                                                                     (long) content.length,
                                                                                     ASYNC_REQUEST_BODY_EXECUTOR);
                return new TestAsyncBody(asyncRequestBody, content.length, contentSize.precalculatedCrc32(), bodyType);
            }
            case INPUTSTREAM_NO_LENGTH: {
                byte[] content = contentSize.byteContent();
                AsyncRequestBody asyncRequestBody = AsyncRequestBody
                    .fromInputStream(conf -> conf.inputStream(new ByteArrayInputStream(content))
                                                 .executor(ASYNC_REQUEST_BODY_EXECUTOR));
                return new TestAsyncBody(asyncRequestBody, content.length, contentSize.precalculatedCrc32(), bodyType);
            }
            case CONTENT_PROVIDER_NO_LENGTH: {
                byte[] content = contentSize.byteContent();
                Flowable<ByteBuffer> publisher = Flowable.just(ByteBuffer.wrap(content));
                AsyncRequestBody asyncRequestBody = AsyncRequestBody.fromPublisher(publisher);
                return new TestAsyncBody(asyncRequestBody, content.length, contentSize.precalculatedCrc32(), bodyType);
            }
            case BYTES: {
                byte[] content = contentSize.byteContent();
                AsyncRequestBody asyncRequestBody = AsyncRequestBody.fromBytes(content);
                return new TestAsyncBody(asyncRequestBody, content.length, contentSize.precalculatedCrc32(), bodyType);
            }
            case BYTE_BUFFER: {
                byte[] content = contentSize.byteContent();
                AsyncRequestBody asyncRequestBody = AsyncRequestBody.fromByteBuffer(ByteBuffer.wrap(content));
                return new TestAsyncBody(asyncRequestBody, content.length, contentSize.precalculatedCrc32(), bodyType);
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
            case BYTES_UNSAFE: {
                byte[] content = contentSize.byteContent();
                AsyncRequestBody asyncRequestBody = AsyncRequestBody.fromBytesUnsafe(content);
                return new TestAsyncBody(asyncRequestBody, content.length, contentSize.precalculatedCrc32(), bodyType);
            }
            case BYTE_BUFFER_UNSAFE: {
                byte[] content = contentSize.byteContent();
                AsyncRequestBody asyncRequestBody = AsyncRequestBody.fromByteBufferUnsafe(ByteBuffer.wrap(content));
                return new TestAsyncBody(asyncRequestBody, content.length, contentSize.precalculatedCrc32(), bodyType);
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
                return new TestAsyncBody(asyncRequestBody,
                                         content1.length + content2.length,
                                         contentSize.precalculatedCrc32forBuffersAPI(),
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
                                         contentSize.precalculatedCrc32forBuffersAPI(),
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
                                         contentSize.precalculatedCrc32forBuffersAPI(),
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
                                         contentSize.precalculatedCrc32forBuffersAPI(),
                                         bodyType);
            }
            case BLOCKING_INPUT_STREAM: {
                byte[] content = contentSize.byteContent();
                long streamToSendLength = content.length;
                BlockingInputStreamAsyncRequestBody body = AsyncRequestBody.forBlockingInputStream(streamToSendLength);
                return new TestAsyncBodyForBlockingInputStream(body,
                                                               new ByteArrayInputStream(content),
                                                               content.length,
                                                               contentSize.precalculatedCrc32(),
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
                                                                contentSize.precalculatedCrc32(),
                                                                bodyType);
            }
            default:
                throw new RuntimeException("Unsupported async body type: " + bodyType);
        }
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

    private static List<UploadConfig> uploadConfigs() {
        List<UploadConfig> configs = new ArrayList<>();

        boolean[] payloadSigningEnabled = {true, false};
        for (BodyType bodyType : BodyType.values()) {
            for (TestConfig baseConfig : testConfigs()) {
                for (ContentSize size : ContentSize.values()) {
                    for (boolean payloadSigning : payloadSigningEnabled) {
                        UploadConfig config = new UploadConfig();
                        config.setPayloadSigning(payloadSigning);
                        config.setBaseConfig(baseConfig);
                        config.setBodyType(bodyType);
                        config.setContentSize(size);
                        configs.add(config);
                    }
                }
            }
        }
        return configs;
    }

    static class UploadConfig {
        private TestConfig baseConfig;
        private BodyType bodyType;
        private ContentSize contentSize;
        private boolean payloadSigning;

        public void setPayloadSigning(boolean payloadSigning) {
            this.payloadSigning = payloadSigning;
        }

        public boolean payloadSigning() {
            return payloadSigning;
        }

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

    enum BodyType {
        INPUTSTREAM_RESETABLE,
        INPUTSTREAM_NOT_RESETABLE,
        INPUTSTREAM_NO_LENGTH,

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
                case SMALL:
                    return smallContent;
                case LARGE:
                    return largeContent;
                default:
                    throw new IllegalArgumentException("not supported ContentSize " + this);
            }
        }

        String stringContent() {
            switch (this) {
                case SMALL:
                    return "Hello World!";
                case LARGE:
                    return new String(largeContent(), StandardCharsets.UTF_8);
                default:
                    throw new IllegalArgumentException("not supported ContentSize " + this);
            }
        }

        Path fileContent() {
            switch (this) {
                case SMALL:
                    return testFileSmall;
                case LARGE:
                    return testFileLarge;
                default:
                    throw new IllegalArgumentException("not supported ContentSize " + this);
            }
        }

        String precalculatedCrc32() {
            switch (this) {
                case SMALL:
                    return smallContentCrc32;
                case LARGE:
                    return largeContentCrc32;
                default:
                    throw new IllegalArgumentException("not supported ContentSize " + this);
            }
        }

        String precalculatedCrc32forBuffersAPI() {
            switch (this) {
                case SMALL:
                    return smallContentCRC32ForBuffersAPI;
                case LARGE:
                    return largeContentCRC32ForBuffersAPI;
                default:
                    throw new IllegalArgumentException("not supported ContentSize " + this);
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

}
