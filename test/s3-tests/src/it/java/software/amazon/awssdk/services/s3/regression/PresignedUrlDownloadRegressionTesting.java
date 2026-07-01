package software.amazon.awssdk.services.s3.regression;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.services.s3.regression.S3ChecksumsTestUtils.crc32;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.ChecksumAlgorithm;
import software.amazon.awssdk.services.s3.model.ChecksumMode;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presignedurl.model.PresignedUrlDownloadRequest;
import software.amazon.awssdk.utils.ToString;

@Timeout(value = 15, unit = TimeUnit.MINUTES)
public class PresignedUrlDownloadRegressionTesting extends BaseS3RegressionTest {

    private static final int KIB = 1024;
    private static final int MIB = 1024 * 1024;

    private static S3AsyncClient singlePartClient;
    private static S3AsyncClient multipartClient;
    private static S3Presigner presigner;

    @TempDir
    Path tempDir;
    @BeforeAll
    static void init() {
        singlePartClient = S3AsyncClient.builder()
            .region(REGION).credentialsProvider(CREDENTIALS_PROVIDER_CHAIN).build();

        multipartClient = S3AsyncClient.builder()
            .region(REGION).credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
            .multipartEnabled(true)
            .multipartConfiguration(c -> c.minimumPartSizeInBytes(8L * MIB))
            .build();

        presigner = S3Presigner.builder()
            .region(REGION).credentialsProvider(CREDENTIALS_PROVIDER_CHAIN).build();

        for (ContentSize size : ContentSize.values()) {
            size.upload();
        }
    }

    @AfterAll
    static void cleanup() {
        for (ContentSize size : ContentSize.values()) {
            size.delete();
        }
        if (singlePartClient != null) singlePartClient.close();
        if (multipartClient != null) multipartClient.close();
        if (presigner != null) presigner.close();
    }
    @ParameterizedTest
    @MethodSource("downloadConfigs")
    void download_withPresignedUrl_contentMatchesUpload(DownloadConfig config) throws Exception {

        S3AsyncClient client = config.clientType == ClientType.SINGLE_PART ? singlePartClient : multipartClient;
        ObjectWithCRC testObj = config.contentSize.object();

        PresignedUrlDownloadRequest request = presignAndBuildRequest(testObj.key, config.range);
        DownloadResult result = download(client, request, config.transformerType);

        String downloadedCrc32 = crc32(result.bytes);
        if (config.range == null) {
            assertThat(downloadedCrc32)
                .as("CRC32 mismatch: %s — downloaded content differs from uploaded", config)
                .isEqualTo(testObj.crc32);

            if (config.clientType == ClientType.SINGLE_PART
                && config.contentSize != ContentSize.MPU
                && result.responseCrc32 != null) {
                assertThat(downloadedCrc32)
                    .as("CRC32 mismatch with S3 header: %s", config)
                    .isEqualTo(result.responseCrc32);
            }
        } else {
            int expectedSize = parseRangeSize(config.range);
            assertThat(result.bytes.length)
                .as("Size mismatch for range request: %s", config)
                .isEqualTo(expectedSize);
        }

    }
    private static List<DownloadConfig> downloadConfigs() {
        List<DownloadConfig> configs = new ArrayList<>();
        for (ClientType clientType : ClientType.values()) {
            configs.add(new DownloadConfig(clientType, ContentSize.EMPTY, TransformerType.BYTES, null));
            configs.add(new DownloadConfig(clientType, ContentSize.SMALL, TransformerType.BYTES, null));
            configs.add(new DownloadConfig(clientType, ContentSize.SMALL, TransformerType.FILE, null));
            configs.add(new DownloadConfig(clientType, ContentSize.SMALL, TransformerType.CUSTOM, null));
            configs.add(new DownloadConfig(clientType, ContentSize.LARGE_27MB, TransformerType.FILE, null));
            configs.add(new DownloadConfig(clientType, ContentSize.MPU, TransformerType.BYTES, null));
        }
        configs.add(new DownloadConfig(ClientType.MULTIPART, ContentSize.LARGE_27MB, TransformerType.FILE, "bytes=5242880-10485759"));
        return configs;
    }
    enum ClientType {
        SINGLE_PART, MULTIPART
    }

    enum TransformerType {
        BYTES, FILE, CUSTOM
    }

    enum ContentSize {
        EMPTY(0, false),
        SMALL(16 * KIB, false),
        LARGE_27MB(27 * MIB, false),
        MPU(16 * MIB, true);

        private final int size;
        private final boolean multipartUpload;
        private ObjectWithCRC obj;

        ContentSize(int size, boolean multipartUpload) {
            this.size = size;
            this.multipartUpload = multipartUpload;
        }

        ObjectWithCRC object() {
            return obj;
        }

        void upload() {
            if (multipartUpload) {
                obj = doMpuUpload(name().toLowerCase(), size);
            } else {
                obj = doSingleUpload(name().toLowerCase(), size);
            }
        }

        void delete() {
            if (obj != null) {
                try {
                    s3.deleteObject(r -> r.bucket(bucketName).key(obj.key));
                } catch (Exception e) {
                    // ignore cleanup failures
                }
            }
        }
    }
    static class DownloadConfig {
        final ClientType clientType;
        final ContentSize contentSize;
        final TransformerType transformerType;
        final String range;

        DownloadConfig(ClientType clientType, ContentSize contentSize, TransformerType transformerType, String range) {
            this.clientType = clientType;
            this.contentSize = contentSize;
            this.transformerType = transformerType;
            this.range = range;
        }

        @Override
        public String toString() {
            return ToString.builder("DownloadConfig")
                .add("client", clientType)
                .add("size", contentSize)
                .add("transformer", transformerType)
                .add("range", range)
                .build();
        }
    }

    private static class ObjectWithCRC {
        final String key;
        final String crc32;

        ObjectWithCRC(String key, String crc32) {
            this.key = key;
            this.crc32 = crc32;
        }
    }

    private static class DownloadResult {
        final byte[] bytes;
        final String responseCrc32;

        DownloadResult(byte[] bytes, String responseCrc32) {
            this.bytes = bytes;
            this.responseCrc32 = responseCrc32;
        }
    }
    private static ObjectWithCRC doSingleUpload(String label, int sizeBytes) {
        String key = String.format("presigned-dl-%s-%s.dat", label, UUID.randomUUID());
        byte[] data = new byte[sizeBytes];
        if (sizeBytes > 0) {
            new Random().nextBytes(data);
        }
        String dataCrc32 = crc32(data);
        s3.putObject(r -> r.bucket(bucketName).key(key), RequestBody.fromBytes(data));
        return new ObjectWithCRC(key, dataCrc32);
    }

    private static ObjectWithCRC doMpuUpload(String label, int sizeBytes) {
        String key = String.format("presigned-dl-%s-%s.dat", label, UUID.randomUUID());
        byte[] fullData = new byte[sizeBytes];
        new Random().nextBytes(fullData);
        String dataCrc32 = crc32(fullData);

        CreateMultipartUploadResponse createResp = s3.createMultipartUpload(
            b -> b.bucket(bucketName).key(key).checksumAlgorithm(ChecksumAlgorithm.CRC32));
        String uploadId = createResp.uploadId();
        List<CompletedPart> parts = new ArrayList<>();
        int partSize = 8 * MIB;
        int numParts = (sizeBytes + partSize - 1) / partSize;

        for (int i = 0; i < numParts; i++) {
            int start = i * partSize;
            int end = Math.min(start + partSize, sizeBytes);
            byte[] partData = Arrays.copyOfRange(fullData, start, end);
            final int partNum = i + 1;
            UploadPartResponse uploadResp = s3.uploadPart(
                b -> b.bucket(bucketName).key(key).uploadId(uploadId).partNumber(partNum)
                      .checksumAlgorithm(ChecksumAlgorithm.CRC32),
                RequestBody.fromBytes(partData));
            parts.add(CompletedPart.builder()
                .partNumber(partNum).eTag(uploadResp.eTag())
                .checksumCRC32(uploadResp.checksumCRC32()).build());
        }

        s3.completeMultipartUpload(b -> b.bucket(bucketName).key(key).uploadId(uploadId)
            .multipartUpload(CompletedMultipartUpload.builder().parts(parts).build()));
        return new ObjectWithCRC(key, dataCrc32);
    }
    private PresignedUrlDownloadRequest presignAndBuildRequest(String key, String range) {
        PresignedGetObjectRequest presigned = presigner.presignGetObject(r -> r
            .getObjectRequest(req -> {
                req.bucket(bucketName).key(key).checksumMode(ChecksumMode.ENABLED);
                if (range != null) req.range(range);
            })
            .signatureDuration(Duration.ofMinutes(10)));

        PresignedUrlDownloadRequest.Builder reqBuilder = PresignedUrlDownloadRequest.builder()
            .presignedUrl(presigned.url());
        if (range != null) reqBuilder.range(range);
        return reqBuilder.build();
    }

    private DownloadResult download(S3AsyncClient client, PresignedUrlDownloadRequest request,
                                    TransformerType transformerType) throws Exception {
        switch (transformerType) {
            case BYTES: {
                ResponseBytes<GetObjectResponse> resp = client.presignedUrlExtension()
                    .getObject(request, AsyncResponseTransformer.toBytes()).get(5, TimeUnit.MINUTES);
                return new DownloadResult(resp.asByteArray(), resp.response().checksumCRC32());
            }
            case FILE: {
                Path file = tempDir.resolve("dl-" + UUID.randomUUID() + ".bin");
                GetObjectResponse resp = client.presignedUrlExtension()
                    .getObject(request, file).get(5, TimeUnit.MINUTES);
                return new DownloadResult(Files.readAllBytes(file), resp.checksumCRC32());
            }
            case CUSTOM:
                return downloadWithCustomTransformer(client, request);
            default:
                throw new IllegalArgumentException("Unknown transformer: " + transformerType);
        }
    }

    private DownloadResult downloadWithCustomTransformer(S3AsyncClient client, PresignedUrlDownloadRequest request)
        throws Exception {
        AsyncResponseTransformer<GetObjectResponse, DownloadResult> transformer =
            new AsyncResponseTransformer<GetObjectResponse, DownloadResult>() {
                private CompletableFuture<DownloadResult> future;
                private GetObjectResponse response;
                private ByteArrayOutputStream baos;

                @Override
                public CompletableFuture<DownloadResult> prepare() {
                    future = new CompletableFuture<>();
                    baos = new ByteArrayOutputStream();
                    return future;
                }

                @Override public void onResponse(GetObjectResponse r) { this.response = r; }

                @Override
                public void onStream(SdkPublisher<ByteBuffer> publisher) {
                    publisher.subscribe(new Subscriber<ByteBuffer>() {
                        @Override public void onSubscribe(Subscription s) { s.request(Long.MAX_VALUE); }
                        @Override public void onNext(ByteBuffer b) {
                            byte[] bytes = new byte[b.remaining()];
                            b.get(bytes);
                            baos.write(bytes, 0, bytes.length);
                        }
                        @Override public void onError(Throwable t) { future.completeExceptionally(t); }
                        @Override public void onComplete() {
                            future.complete(new DownloadResult(baos.toByteArray(),
                                response != null ? response.checksumCRC32() : null));
                        }
                    });
                }

                @Override public void exceptionOccurred(Throwable error) { future.completeExceptionally(error); }
            };

        return client.presignedUrlExtension().getObject(request, transformer).get(5, TimeUnit.MINUTES);
    }

    private static int parseRangeSize(String range) {
        String[] parts = range.replace("bytes=", "").split("-");
        return Integer.parseInt(parts[1]) - Integer.parseInt(parts[0]) + 1;
    }
}
