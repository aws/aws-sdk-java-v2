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
import static software.amazon.awssdk.services.s3.checksum.ChecksumIntegrationTesting.testConfigs;
import static software.amazon.awssdk.services.s3.checksum.S3ChecksumsTestUtils.assumeNotAccelerateWithArnType;
import static software.amazon.awssdk.services.s3.checksum.S3ChecksumsTestUtils.assumeNotAccelerateWithEoz;
import static software.amazon.awssdk.services.s3.checksum.S3ChecksumsTestUtils.assumeNotAccelerateWithPathStyle;
import static software.amazon.awssdk.services.s3.checksum.S3ChecksumsTestUtils.assumeNotAccessPointWithPathStyle;
import static software.amazon.awssdk.services.s3.checksum.S3ChecksumsTestUtils.crc32;

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
import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.ResponsePublisher;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.core.waiters.Waiter;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;
import software.amazon.awssdk.services.s3.waiters.S3Waiter;
import software.amazon.awssdk.services.s3control.S3ControlClient;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.testutils.InputStreamUtils;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.ToString;

public class DownloadStreamingIntegrationTesting {
    private static final Logger LOG = Logger.loggerFor(DownloadStreamingIntegrationTesting.class);

    private static final String BUCKET_NAME_PREFIX = "do-not-delete-dl-streaming-";
    private static final String MRAP_NAME = "do-not-delete-dl-streaming-testing";
    private static final String AP_NAME = "do-not-delete-dl-streaming-testing-ap";
    private static final String EOZ_SUFFIX = "--usw2-az3--x-s3";

    private static final Region REGION = Region.US_WEST_2;
    private static final String TEST_CREDENTIALS_PROFILE_NAME = "aws-test-account";

    public static final AwsCredentialsProviderChain CREDENTIALS_PROVIDER_CHAIN =
        AwsCredentialsProviderChain.of(ProfileCredentialsProvider.builder()
                                                                 .profileName(TEST_CREDENTIALS_PROFILE_NAME)
                                                                 .build(),
                                       DefaultCredentialsProvider.create());

    static ObjectWithCRC smallObject;
    static ObjectWithCRC largeObject;
    static ObjectWithCRC largeObjectMulti;

    private static String accountId;
    private static String bucketName;
    private static String mrapArn;
    private static String eozBucket;
    private static String apArn;

    private static S3ControlClient s3Control;
    private static S3Client s3;
    private static StsClient sts;

    private static Path tempDirPath;

    private List<Path> pathsToDelete;

    @BeforeAll
    static void init() throws Exception {

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

        tempDirPath = createTempDir("DownloadStreamingIntegrationTesting");

        smallObject = uploadObjectSmall(); // 16 KiB
        largeObject = uploadObjectLarge(); // 200MiB
        largeObjectMulti = uploadMultiPartObject(); // 200 MiB, default multipart config
    }

    @AfterAll
    static void cleanup() {

    }

    @BeforeEach
    void setup() {
        pathsToDelete = new ArrayList<>();
    }

    @AfterEach
    void methodCleanup() {
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
        assumeNotAccelerateWithPathStyle(config.getBaseConfig());
        assumeNotAccessPointWithPathStyle(config.getBaseConfig());
        assumeNotAccelerateWithArnType(config.getBaseConfig());
        assumeNotAccelerateWithEoz(config.getBaseConfig());

        LOG.debug(() -> "Running downloadObject with config: " + config);

        String key = config.getContentSize() == ContentSize.SMALL ? smallObject.key() : largeObject.key();
        GetObjectRequest request = GetObjectRequest.builder()
                                                   .bucket(bucketForType(config.getBaseConfig().getBucketType()))
                                                   .key(key)
                                                   .build();

        byte[] content;
        switch (config.getBaseConfig().getFlavor()) {
            case JAVA_BASED: {
                content = callSyncGetObject(config, request);
                break;
            }
            case ASYNC_JAVA_BASED:
            case TM_JAVA:
            case ASYNC_CRT: {
                content = callAsyncGetObject(request, config);
                break;
            }
            default:
                throw new RuntimeException("Unsupported java client flavor: " + config.getBaseConfig().getFlavor());
        }

        String receivedContentCRC32 = crc32(content);
        String expectedCRC32 = objectForConfig(config).crc32;
        assertThat(receivedContentCRC32).isEqualTo(expectedCRC32);
    }

    // 16 KiB
    static ObjectWithCRC uploadObjectSmall() throws IOException {
        LOG.debug(() -> "test setup - uploading small test object");
        String name = String.format("%s-%s.dat", System.currentTimeMillis(), UUID.randomUUID());
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] rand = new byte[1024];
        for (int i = 0; i < 16; i++) {
            new Random().nextBytes(rand);
            os.write(rand);
        }
        byte[] fullContent = os.toByteArray();
        for (BucketType bucketType : BucketType.values()) {
            String bucket = bucketForType(bucketType);
            PutObjectRequest req = PutObjectRequest.builder()
                                                   .bucket(bucket)
                                                   .key(name)
                                                   .build();

            s3.putObject(req, RequestBody.fromBytes(fullContent));
        }
        return new ObjectWithCRC(name, crc32(fullContent));
    }

    // 80 MiB
    static ObjectWithCRC uploadObjectLarge() throws IOException {
        LOG.debug(() -> "test setup - uploading large test object");
        String name = String.format("%s-%s.dat", System.currentTimeMillis(), UUID.randomUUID());
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] rand = new byte[1024 * 1024];
        for (int i = 0; i < 80; i++) {
            new Random().nextBytes(rand);
            os.write(rand);
        }
        byte[] fullContent = os.toByteArray();
        for (BucketType bucketType : BucketType.values()) {
            String bucket = bucketForType(bucketType);
            PutObjectRequest req = PutObjectRequest.builder()
                                                   .bucket(bucket)
                                                   .key(name)
                                                   .build();

            s3.putObject(req, RequestBody.fromBytes(fullContent));
        }
        return new ObjectWithCRC(name, crc32(fullContent));
    }

    // 80MiB, multipart default config
    static ObjectWithCRC uploadMultiPartObject() throws Exception {
        LOG.debug(() -> "test setup - uploading large test object - multipart");

        String name = String.format("%s-%s.dat", System.currentTimeMillis(), UUID.randomUUID());
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] rand = new byte[8 * 1024 * 1024];
        for (int i = 0; i < 10; i++) {
            new Random().nextBytes(rand);
            os.write(rand);
        }
        byte[] fullContent = os.toByteArray();
        for (BucketType bucketType : BucketType.values()) {
            doMultipartUpload(bucketType, name, fullContent);
        }
        return new ObjectWithCRC(name, crc32(fullContent));
    }

    static void doMultipartUpload(BucketType bucketType, String objectName, byte[] content) {
        String bucket = bucketForType(bucketType);
        LOG.debug(() -> String.format("Uploading multipart object for bucket type: %s - %s", bucketType, bucket)
        );
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
                    DownloadConfig config = new DownloadConfig(baseConfig, responseTransformerType, contentSize);
                    configs.add(config);
                }
            }
        }
        return configs;
    }

    byte[] callSyncGetObject(DownloadConfig config, GetObjectRequest request) throws IOException {
        S3Client s3Client = makeSyncClient(config.getBaseConfig());
        byte[] content;
        switch (config.getResponseTransformerType()) {
            case FILE: {
                String filename = request.key();
                Path filePath = Paths.get(tempDirPath.toString(), filename);
                pathsToDelete.add(filePath);
                s3Client.getObject(request, ResponseTransformer.toFile(filePath));
                content = Files.readAllBytes(filePath);
                break;
            }

            case BYTES: {
                ResponseBytes<GetObjectResponse> res = s3Client.getObject(request, ResponseTransformer.toBytes());
                content = res.asByteArray();
                break;
            }

            case INPUT_STREAM: {
                ResponseInputStream<GetObjectResponse> res = s3Client.getObject(request, ResponseTransformer.toInputStream());
                content = InputStreamUtils.drainInputStream(res);
                break;
            }

            case OUTPUT_STREAM: {
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                s3Client.getObject(request, ResponseTransformer.toOutputStream(os));
                content = os.toByteArray();
                break;
            }

            case UNMANAGED: {
                UnmanagedResponseTransformer tr = new UnmanagedResponseTransformer();
                s3Client.getObject(request, tr);
                content = tr.content;
                break;
            }

            case PUBLISHER:
                Assumptions.abort("Skipping 'publisher' transformer type for sync client: " + config);
                content = null;
                break;
            default:
                throw new UnsupportedOperationException("unsupported response transformer type: " + config.getResponseTransformerType());

        }
        return content;
    }

    byte[] callAsyncGetObject(GetObjectRequest request, DownloadConfig config) throws Exception {
        S3AsyncClient s3AsyncClient = makeAsyncClient(config.getBaseConfig());
        byte[] content;
        switch (config.getResponseTransformerType()) {
            case FILE: {
                String filename = randomFileName();
                Path filePath = Paths.get(tempDirPath.toString(), filename);
                s3AsyncClient.getObject(request, AsyncResponseTransformer.toFile(filePath))
                             .get(5, TimeUnit.MINUTES);
                content = Files.readAllBytes(filePath);
                break;
            }

            case BYTES: {
                ResponseBytes<GetObjectResponse> res = s3AsyncClient.getObject(request, AsyncResponseTransformer.toBytes())
                                                                    .get(5, TimeUnit.MINUTES);
                content = res.asByteArray();
                break;
            }

            case INPUT_STREAM: {
                ResponseInputStream<GetObjectResponse> res = s3AsyncClient.getObject(request,
                                                                                     AsyncResponseTransformer.toBlockingInputStream())
                                                                          .get(5, TimeUnit.MINUTES);
                content = InputStreamUtils.drainInputStream(res);
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
                break;
            }

            case OUTPUT_STREAM:
            case UNMANAGED:
                Assumptions.abort(String.format("Skipping '%s' transformer type for async client: %s",
                                                config.getResponseTransformerType(), config));
                content = null;
                break;
            default:
                throw new UnsupportedOperationException("unsupported response transformer type: " + config.getResponseTransformerType());
        }

        return content;
    }

    private static String getBucketName() {
        return BUCKET_NAME_PREFIX + accountId;
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

    static ObjectWithCRC objectForConfig(DownloadConfig config) {
        if (config.getContentSize() == ContentSize.SMALL) {
            return smallObject;
        }

        if (config.getBaseConfig().getFlavor() == S3ClientFlavor.TM_JAVA) {
            return largeObjectMulti;
        }
        return largeObject;

    }

    static class DownloadConfig {
        private TestConfig baseConfig;
        private ResponseTransformerType responseTransformerType;
        private ContentSize contentSize;

        public DownloadConfig(TestConfig baseConfig, ResponseTransformerType responseTransformerType,
                              ContentSize contentSize) {
            this.baseConfig = baseConfig;
            this.responseTransformerType = responseTransformerType;
            this.contentSize = contentSize;
        }

        public TestConfig getBaseConfig() {
            return this.baseConfig;
        }

        public void setBaseConfig(TestConfig baseConfig) {
            this.baseConfig = baseConfig;
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

        @Override
        public String toString() {
            return ToString.builder("DownloadConfig")
                           .add("baseConfig", baseConfig)
                           .add("responseTransformerType", responseTransformerType)
                           .add("contentSize", contentSize)
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
            case JAVA_BASED:
                return S3Client.builder()
                               .forcePathStyle(config.isForcePathStyle())
                               .requestChecksumCalculation(config.getRequestChecksumValidation())
                               .region(REGION)
                               .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                               .accelerate(config.isAccelerateEnabled())
                               .build();
            default:
                throw new RuntimeException("Unsupported sync flavor: " + config.getFlavor());
        }
    }

    private S3AsyncClient makeAsyncClient(TestConfig config) {
        switch (config.getFlavor()) {
            case ASYNC_JAVA_BASED:
                return S3AsyncClient.builder()
                                    .forcePathStyle(config.isForcePathStyle())
                                    .requestChecksumCalculation(config.getRequestChecksumValidation())
                                    .region(REGION)
                                    .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                                    .accelerate(config.isAccelerateEnabled())
                                    .build();
            case TM_JAVA:
                return S3AsyncClient.builder()
                                    .forcePathStyle(config.isForcePathStyle())
                                    .requestChecksumCalculation(config.getRequestChecksumValidation())
                                    .region(REGION)
                                    .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                                    .accelerate(config.isAccelerateEnabled())
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

    enum ContentSize {
        SMALL,
        LARGE
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

        public void key(String key) {
            this.key = key;
        }

        public String crc32() {
            return crc32;
        }

        public void crc32(String crc32) {
            this.crc32 = crc32;
        }
    }

    private static class UnmanagedResponseTransformer implements ResponseTransformer<GetObjectResponse, byte[]> {
        byte[] content;

        @Override
        public byte[] transform(GetObjectResponse response, AbortableInputStream inputStream) throws Exception {
            this.content = InputStreamUtils.drainInputStream(inputStream); // stream will be closed
            return content;
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
