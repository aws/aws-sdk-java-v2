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
import static software.amazon.awssdk.services.s3.regression.ControlPlaneOperationRegressionTesting.testConfigs;
import static software.amazon.awssdk.services.s3.regression.S3ChecksumsTestUtils.assumeNotAccessPointWithPathStyle;
import static software.amazon.awssdk.services.s3.regression.S3ChecksumsTestUtils.crc32;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.ResponsePublisher;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ChecksumMode;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;
import software.amazon.awssdk.testutils.InputStreamUtils;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.ToString;

public class DownloadStreamingRegressionTesting extends BaseS3RegressionTest {
    private static final Logger LOG = Logger.loggerFor(DownloadStreamingRegressionTesting.class);

    static ObjectWithCRC smallObject;
    static ObjectWithCRC largeObject;
    static ObjectWithCRC largeObjectMulti;

    private static Path tempDirPath;

    private List<Path> pathsToDelete;

    @BeforeAll
    static void init() throws Exception {
        tempDirPath = createTempDir("DownloadStreamingIntegrationTesting");
        smallObject = uploadObjectSmall(); // 16 KiB
        largeObject = uploadObjectLarge(); // 80 MiB
        largeObjectMulti = uploadMultiPartObject(); // 80 MiB, default multipart config
    }

    @AfterAll
    static void cleanup() {
        for (BucketType bucketType : BucketType.values()) {
            String bucket = bucketForType(bucketType);
            s3.deleteObject(req -> req.bucket(bucket).key(smallObject.key()));
            s3.deleteObject(req -> req.bucket(bucket).key(largeObject.key()));
            s3.deleteObject(req -> req.bucket(bucket).key(largeObjectMulti.key()));
        }
    }

    @BeforeEach
    void setupMethod() {
        pathsToDelete = new ArrayList<>();
    }

    @AfterEach
    void testCleanup() {
        pathsToDelete.forEach(p -> {
            try {
                Files.delete(p);
            } catch (Exception e) {
                LOG.error(() -> String.format("Unable to delete file %s", p.toString()), e);
            }
        });
    }

    @ParameterizedTest
    @MethodSource("downloadConfigs")
    void downloadObject(DownloadConfig config) throws Exception {
        assumeNotAccessPointWithPathStyle(config.baseConfig());

        LOG.debug(() -> "Running downloadObject with config: " + config);

        String key = config.contentSize().s3Object().key();
        GetObjectRequest.Builder b = GetObjectRequest.builder()
                                                     .bucket(bucketForType(config.baseConfig().getBucketType()))
                                                     .key(key);
        if (config.checksumModeEnabled()) {
            b.checksumMode(ChecksumMode.ENABLED);
        }

        GetObjectRequest request = b.build();

        CallResponse response;
        switch (config.baseConfig().getFlavor()) {
            case STANDARD_SYNC: {
                response = callSyncGetObject(config, request);
                break;
            }
            case STANDARD_ASYNC:
            case MULTIPART_ENABLED:
            case CRT_BASED: {
                response = callAsyncGetObject(request, config);
                break;
            }
            default:
                throw new RuntimeException("Unsupported java client flavor: " + config.baseConfig().getFlavor());
        }

        String receivedContentCRC32 = crc32(response.content());
        String s3Crc32 = response.crc32();
        if (config.checksumModeEnabled() && StringUtils.isNotBlank(s3Crc32)) {
            assertThat(receivedContentCRC32)
                .withFailMessage("Mismatch with s3 crc32 for config " + config)
                .isEqualTo(s3Crc32);
        }
        String expectedCRC32 = config.contentSize().s3Object().crc32();
        assertThat(receivedContentCRC32)
            .withFailMessage("Mismatch with calculated crc32 for config " + config)
            .isEqualTo(expectedCRC32);
    }

    // 16 KiB
    static ObjectWithCRC uploadObjectSmall() throws IOException {
        String name = String.format("%s-%s.dat", System.currentTimeMillis(), UUID.randomUUID());
        LOG.debug(() -> "test setup - uploading small test object: " + name);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] rand = new byte[1024];
        for (int i = 0; i < 16; i++) {
            new Random().nextBytes(rand);
            os.write(rand);
        }
        byte[] fullContent = os.toByteArray();
        String crc32 = crc32(fullContent);
        for (BucketType bucketType : BucketType.values()) {
            String bucket = bucketForType(bucketType);
            PutObjectRequest req = PutObjectRequest
                .builder()
                .bucket(bucket)
                .key(name)
                .build();
            s3.putObject(req, RequestBody.fromBytes(fullContent));
        }
        return new ObjectWithCRC(name, crc32);
    }

    // 80 MiB
    static ObjectWithCRC uploadObjectLarge() throws IOException {
        String name = String.format("%s-%s.dat", System.currentTimeMillis(), UUID.randomUUID());
        LOG.debug(() -> "test setup - uploading large test object: " + name);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] rand = new byte[1024 * 1024];
        for (int i = 0; i < 80; i++) {
            new Random().nextBytes(rand);
            os.write(rand);
        }
        byte[] fullContent = os.toByteArray();
        String crc32 = crc32(fullContent);
        for (BucketType bucketType : BucketType.values()) {
            String bucket = bucketForType(bucketType);
            PutObjectRequest req = PutObjectRequest
                .builder()
                .bucket(bucket)
                .key(name)
                .build();

            s3.putObject(req, RequestBody.fromBytes(fullContent));
        }
        return new ObjectWithCRC(name, crc32);
    }

    // 80MiB, multipart default config
    static ObjectWithCRC uploadMultiPartObject() throws Exception {
        String name = String.format("%s-%s.dat", System.currentTimeMillis(), UUID.randomUUID());
        LOG.debug(() -> "test setup - uploading large test object - multipart: " + name);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] rand = new byte[8 * 1024 * 1024];
        for (int i = 0; i < 10; i++) {
            new Random().nextBytes(rand);
            os.write(rand);
        }
        byte[] fullContent = os.toByteArray();
        String crc32 = crc32(fullContent);
        for (BucketType bucketType : BucketType.values()) {
            doMultipartUpload(bucketType, name, fullContent);
        }
        return new ObjectWithCRC(name, crc32);
    }

    static void doMultipartUpload(BucketType bucketType, String objectName, byte[] content) {
        String bucket = bucketForType(bucketType);
        LOG.debug(() -> String.format("Uploading multipart object for bucket type: %s - %s", bucketType, bucket));
        CreateMultipartUploadRequest createMulti = CreateMultipartUploadRequest.builder()
                                                                               .bucket(bucket)
                                                                               .key(objectName)
                                                                               .build();

        CreateMultipartUploadResponse res = s3.createMultipartUpload(createMulti);
        String uploadId = res.uploadId();

        List<CompletedPart> completedParts = new ArrayList<>();
        int partAmount = 10;
        int partSize = 8 * 1024 * 1024;
        for (int i = 0; i < partAmount; i++) {
            final int partNumber = i + 1;
            int startIndex = partSize * i;
            int endIndex = startIndex + partSize;
            byte[] partContent = Arrays.copyOfRange(content, startIndex, endIndex);
            LOG.debug(() -> "Uploading part: " + partNumber);
            UploadPartResponse partResponse = s3.uploadPart(req -> req.partNumber(partNumber)
                                                                      .uploadId(uploadId)
                                                                      .key(objectName)
                                                                      .bucket(bucket),
                                                            RequestBody.fromBytes(partContent));
            completedParts.add(CompletedPart.builder()
                                            .eTag(partResponse.eTag())
                                            .partNumber(partNumber)
                                            .build());
            LOG.debug(() -> String.format("done part %s - etag: %s: ", partNumber, partResponse.eTag()));
        }

        LOG.debug(() -> "Finishing MPU, completed parts: " + completedParts);

        s3.completeMultipartUpload(req -> req.multipartUpload(u -> u.parts(completedParts))
                                             .bucket(bucket)
                                             .key(objectName)
                                             .uploadId(uploadId));
        s3.waiter().waitUntilObjectExists(r -> r.bucket(bucket).key(objectName),
                                          c -> c.waitTimeout(Duration.ofMinutes(5)));
    }

    private static List<DownloadConfig> downloadConfigs() {
        List<DownloadConfig> configs = new ArrayList<>();
        for (ResponseTransformerType responseTransformerType : ResponseTransformerType.values()) {
            for (TestConfig baseConfig : testConfigs()) {
                for (ContentSize contentSize : ContentSize.values()) {
                    DownloadConfig checksumEnabled =
                        new DownloadConfig(baseConfig, responseTransformerType, contentSize, true);
                    DownloadConfig checksumDisabled =
                        new DownloadConfig(baseConfig, responseTransformerType, contentSize, false);
                    configs.add(checksumEnabled);
                    configs.add(checksumDisabled);
                }
            }
        }
        return configs;
    }

    CallResponse callSyncGetObject(DownloadConfig config, GetObjectRequest request) throws IOException {
        S3Client s3Client = makeSyncClient(config.baseConfig());

        byte[] content;
        String s3Crc32 = null;
        switch (config.responseTransformerType()) {
            case FILE: {
                String filename = request.key();
                Path filePath = Paths.get(tempDirPath.toString(), filename);
                pathsToDelete.add(filePath);
                GetObjectResponse res = s3Client.getObject(request, ResponseTransformer.toFile(filePath));
                s3Crc32 = res.checksumCRC32();
                content = Files.readAllBytes(filePath);
                break;
            }

            case BYTES: {
                ResponseBytes<GetObjectResponse> res = s3Client.getObject(request, ResponseTransformer.toBytes());
                content = res.asByteArray();
                s3Crc32 = res.response().checksumCRC32();
                break;
            }

            case INPUT_STREAM: {
                ResponseInputStream<GetObjectResponse> res = s3Client.getObject(request, ResponseTransformer.toInputStream());
                content = InputStreamUtils.drainInputStream(res);
                s3Crc32 = res.response().checksumCRC32();
                break;
            }

            case OUTPUT_STREAM: {
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                GetObjectResponse res = s3Client.getObject(request, ResponseTransformer.toOutputStream(os));
                content = os.toByteArray();
                s3Crc32 = res.checksumCRC32();
                break;
            }

            case UNMANAGED: {
                UnmanagedResponseTransformer tr = new UnmanagedResponseTransformer();
                s3Client.getObject(request, ResponseTransformer.unmanaged(tr));
                content = tr.content;
                s3Crc32 = tr.response().checksumCRC32();
                break;
            }

            case PUBLISHER:
                Assumptions.abort("Skipping 'publisher' transformer type for sync client: " + config);
                content = null;
                break;

            default:
                throw new UnsupportedOperationException("unsupported response transformer type: " + config.responseTransformerType());

        }
        s3Client.close();
        return new CallResponse(content, s3Crc32);
    }

    CallResponse callAsyncGetObject(GetObjectRequest request, DownloadConfig config) throws Exception {
        S3AsyncClient s3AsyncClient = makeAsyncClient(config.baseConfig());

        byte[] content;
        String s3crc32 = null;
        switch (config.responseTransformerType()) {
            case FILE: {
                String filename = randomFileName();
                Path filePath = Paths.get(tempDirPath.toString(), filename);
                pathsToDelete.add(filePath);
                GetObjectResponse res = s3AsyncClient.getObject(request, AsyncResponseTransformer.toFile(filePath))
                                                     .get(5, TimeUnit.MINUTES);
                content = Files.readAllBytes(filePath);
                s3crc32 = res.checksumCRC32();
                break;
            }

            case BYTES: {
                ResponseBytes<GetObjectResponse> res = s3AsyncClient.getObject(request, AsyncResponseTransformer.toBytes())
                                                                    .get(5, TimeUnit.MINUTES);
                content = res.asByteArray();
                s3crc32 = res.response().checksumCRC32();
                break;
            }

            case INPUT_STREAM: {
                ResponseInputStream<GetObjectResponse> res = s3AsyncClient.getObject(request,
                                                                                     AsyncResponseTransformer.toBlockingInputStream())
                                                                          .get(5, TimeUnit.MINUTES);
                content = InputStreamUtils.drainInputStream(res);
                s3crc32 = res.response().checksumCRC32();
                break;
            }

            case PUBLISHER: {
                ResponsePublisher<GetObjectResponse> res = s3AsyncClient.getObject(request,
                                                                                   AsyncResponseTransformer.toPublisher())
                                                                        .get(5, TimeUnit.MINUTES);
                ContentConsumer consumer = new ContentConsumer();
                CompletableFuture<Void> fut = res.subscribe(consumer);
                fut.get(5, TimeUnit.MINUTES);
                content = consumer.getFullContent();
                s3crc32 = res.response().checksumCRC32();
                break;
            }

            case OUTPUT_STREAM:
            case UNMANAGED:
                Assumptions.abort(String.format("Skipping '%s' transformer type for async client: %s",
                                                config.responseTransformerType(), config));
                content = null;
                break;
            default:
                throw new UnsupportedOperationException("unsupported response transformer type: " + config.responseTransformerType());
        }
        s3AsyncClient.close();
        return new CallResponse(content, s3crc32);
    }

    private static class CallResponse {
        byte[] content;
        String crc32;

        public CallResponse(byte[] content, String crc32) {
            this.content = content;
            this.crc32 = crc32;
        }

        public byte[] content() {
            return content;
        }

        public String crc32() {
            return crc32;
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

    private String randomFileName() {
        return String.format("%s-%S", System.currentTimeMillis(), UUID.randomUUID());
    }

    static class DownloadConfig {
        private TestConfig baseConfig;
        private ResponseTransformerType responseTransformerType;
        private ContentSize contentSize;
        private boolean checksumModeEnabled;

        public DownloadConfig(TestConfig baseConfig, ResponseTransformerType responseTransformerType,
                              ContentSize contentSize, boolean checksumModeEnabled) {
            this.baseConfig = baseConfig;
            this.responseTransformerType = responseTransformerType;
            this.contentSize = contentSize;
            this.checksumModeEnabled = checksumModeEnabled;
        }

        public TestConfig baseConfig() {
            return this.baseConfig;
        }

        public ResponseTransformerType responseTransformerType() {
            return responseTransformerType;
        }

        public ContentSize contentSize() {
            return contentSize;
        }

        private boolean checksumModeEnabled() {
            return this.checksumModeEnabled;
        }

        @Override
        public String toString() {
            return ToString.builder("DownloadConfig")
                           .add("baseConfig", baseConfig)
                           .add("responseTransformerType", responseTransformerType)
                           .add("contentSize", contentSize)
                           .add("checksumModeEnabled", checksumModeEnabled)
                           .build();
        }
    }

    private static Path createTempDir(String path) {
        try {
            return Files.createDirectories(Paths.get(path));
        } catch (Exception e) {
            LOG.error(() -> "Unable to create directory", e);
            throw new RuntimeException(e);
        }
    }

    private S3Client makeSyncClient(TestConfig config) {
        switch (config.getFlavor()) {
            case STANDARD_SYNC:
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
            case STANDARD_ASYNC:
                return S3AsyncClient.builder()
                                    .forcePathStyle(config.isForcePathStyle())
                                    .requestChecksumCalculation(config.getRequestChecksumValidation())
                                    .region(REGION)
                                    .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                                    .build();
            case MULTIPART_ENABLED:
                return S3AsyncClient.builder()
                                    .forcePathStyle(config.isForcePathStyle())
                                    .requestChecksumCalculation(config.getRequestChecksumValidation())
                                    .region(REGION)
                                    .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                                    .multipartEnabled(true)
                                    .build();
            case CRT_BASED: {
                return S3AsyncClient.crtBuilder()
                                    .forcePathStyle(config.isForcePathStyle())
                                    .requestChecksumCalculation(config.getRequestChecksumValidation())
                                    .region(REGION)
                                    .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                                    .build();
            }
            default:
                throw new RuntimeException("Unsupported async flavor: " + config.getFlavor());
        }
    }

    enum ContentSize {
        SMALL,
        LARGE,
        LARGE_MULTI;

        ObjectWithCRC s3Object() {
            switch (this) {
                case SMALL:
                    return smallObject;
                case LARGE:
                    return largeObject;
                case LARGE_MULTI:
                    return largeObjectMulti;
                default:
                    throw new IllegalArgumentException("Unknown ContentSize " + this);
            }
        }
    }

    private static class ObjectWithCRC {
        private String key;
        private String crc32;

        public ObjectWithCRC(String key, String crc32) {
            this.key = key;
            this.crc32 = crc32;
        }

        public String key() {
            return key;
        }

        public String crc32() {
            return crc32;
        }
    }

    private static class UnmanagedResponseTransformer implements ResponseTransformer<GetObjectResponse, byte[]> {
        byte[] content;
        GetObjectResponse response;

        @Override
        public byte[] transform(GetObjectResponse response, AbortableInputStream inputStream) throws Exception {
            this.response = response;
            this.content = InputStreamUtils.drainInputStream(inputStream); // stream will be closed
            return content;
        }

        public GetObjectResponse response() {
            return this.response;
        }
    }

    private static class ContentConsumer implements Consumer<ByteBuffer> {
        private List<ByteBuffer> buffs = new ArrayList<>();

        @Override
        public void accept(ByteBuffer byteBuffer) {
            buffs.add(byteBuffer);
        }

        byte[] getFullContent() {
            int totalSize = buffs.stream()
                                 .mapToInt(ByteBuffer::remaining)
                                 .sum();
            byte[] result = new byte[totalSize];
            int offset = 0;
            for (ByteBuffer buff : buffs) {
                int length = buff.remaining();
                buff.get(result, offset, length);
                offset += length;
            }
            return result;
        }
    }
}
