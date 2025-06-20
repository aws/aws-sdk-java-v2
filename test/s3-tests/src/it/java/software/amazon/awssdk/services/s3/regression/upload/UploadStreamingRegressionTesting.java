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

package software.amazon.awssdk.services.s3.regression.upload;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.services.s3.regression.S3ChecksumsTestUtils.assumeNotAccessPointWithPathStyle;
import static software.amazon.awssdk.services.s3.regression.S3ChecksumsTestUtils.crc32;
import static software.amazon.awssdk.services.s3.regression.S3ChecksumsTestUtils.makeAsyncClient;
import static software.amazon.awssdk.services.s3.regression.S3ChecksumsTestUtils.makeSyncClient;

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
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import software.amazon.awssdk.auth.signer.S3SignerExecutionAttribute;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.BlockingInputStreamAsyncRequestBody;
import software.amazon.awssdk.core.async.BlockingOutputStreamAsyncRequestBody;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ChecksumMode;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.regression.BaseS3RegressionTest;
import software.amazon.awssdk.services.s3.regression.S3ChecksumsTestUtils;
import software.amazon.awssdk.services.s3.regression.S3ClientFlavor;
import software.amazon.awssdk.services.s3.regression.TestCallable;
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

    protected static Path testFileSmall;
    protected static Path testFileLarge;

    @BeforeAll
    static void setupClass() throws IOException {
        testFileSmall = S3ChecksumsTestUtils.createRandomFile16KB();
        testFileLarge = S3ChecksumsTestUtils.createRandomFile60MB();

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

    protected void verifyChecksumResponsePayload(UploadConfig config, String key, String expectedCRC32) {
        String bucket = bucketForType(config.getBucketType());
        ResponseInputStream<GetObjectResponse> response = s3.getObject(req -> req.checksumMode(ChecksumMode.ENABLED)
                                                                                 .key(key)
                                                                                 .bucket(bucket));
        String crc32 = response.response().checksumCRC32();
        if (crc32 != null) {
            assertThat(crc32).isEqualTo(expectedCRC32);
        }

    }

    protected TestCallable<PutObjectResponse> callPutObject(PutObjectRequest request,
                                                            TestRequestBody requestBody,
                                                            UploadConfig config,
                                                            ClientOverrideConfiguration overrideConfiguration) {
        S3Client s3Client = makeSyncClient(config, overrideConfiguration, REGION, CREDENTIALS_PROVIDER_CHAIN);
        Callable<PutObjectResponse> callable = () -> {
            try {
                return s3Client.putObject(request, requestBody);
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                s3Client.close();
            }
        };
        return new TestCallable<>(s3Client, callable);
    }

    protected TestCallable<PutObjectResponse> callPutObject(PutObjectRequest request,
                                                          S3ClientFlavor flavor,
                                                          TestAsyncBody requestBody,
                                                          UploadConfig config,
                                                          ClientOverrideConfiguration overrideConfiguration) {
        S3AsyncClient s3Client = makeAsyncClient(config, flavor, overrideConfiguration, REGION, CREDENTIALS_PROVIDER_CHAIN);
        Callable<PutObjectResponse> callable = () -> {
            try {
                AsyncRequestBody asyncRequestBody = requestBody.getAsyncRequestBody();
                CompletableFuture<PutObjectResponse> future = s3Client.putObject(request, asyncRequestBody);
                performWriteIfNeeded(requestBody);
                return CompletableFutureUtils.joinLikeSync(future);
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                s3Client.close();
            }
        };
        return new TestCallable<>(s3Client, callable);
    }

    protected TestCallable<PutObjectResponse> callTmUpload(PutObjectRequest request,
                                                         S3ClientFlavor flavor,
                                                         TestAsyncBody requestBody,
                                                         UploadConfig config,
                                                         ClientOverrideConfiguration overrideConfiguration) {
        S3TransferManager transferManager = S3ChecksumsTestUtils.makeTm(config, flavor, overrideConfiguration,
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
            } finally {
                transferManager.close();
            }
        };
        return new TestCallable<>(transferManager, callable);
    }

    protected TestRequestBody getRequestBody(BodyType bodyType, ContentSize contentSize) throws IOException {
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
            case BLOCKING_INPUT_STREAM:
            case BLOCKING_OUTPUT_STREAM:
            case INPUTSTREAM_NO_LENGTH:
                Assumptions.abort("Test BodyType not supported for sync client: " + bodyType);
            default:
                throw new RuntimeException("Unsupported body type: " + bodyType);
        }
    }

    protected TestAsyncBody getAsyncRequestBody(BodyType bodyType, ContentSize contentSize) throws IOException {
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

    protected enum BodyType {
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

        BUFFERS,
        BUFFERS_REMAINING,

        BLOCKING_INPUT_STREAM,
        BLOCKING_OUTPUT_STREAM
    }

    protected enum ContentSize {
        SMALL,
        LARGE;

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
        // 60 MiB
        Random r = new Random();
        byte[] b = new byte[60 * 1024 * 1024];
        r.nextBytes(b);
        return b;
    }

    protected static class TestRequestBody extends RequestBody {
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

    protected static class TestAsyncBody {
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

    protected static class TestAsyncBodyForBlockingOutputStream extends TestAsyncBody {
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

    protected static class TestAsyncBodyForBlockingInputStream extends TestAsyncBody {
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

    protected static class RequestRecorder implements ExecutionInterceptor {
        private final List<SdkHttpRequest> requests = new ArrayList<>();

        @Override
        public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
            requests.add(context.httpRequest());
        }

        public List<SdkHttpRequest> getRequests() {
            return requests;
        }
    }

    protected static class EnablePayloadSigningInterceptor implements ExecutionInterceptor {
        @Override
        public void beforeExecution(Context.BeforeExecution context, ExecutionAttributes executionAttributes) {
            executionAttributes.putAttribute(S3SignerExecutionAttribute.ENABLE_PAYLOAD_SIGNING, true);
            ExecutionInterceptor.super.beforeExecution(context, executionAttributes);
        }
    }

    protected static class NonResettableByteStream extends ByteArrayInputStream {
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
