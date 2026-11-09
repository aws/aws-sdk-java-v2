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

package software.amazon.awssdk.transfer.s3.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.CompletedDirectoryUpload;
import software.amazon.awssdk.transfer.s3.model.UploadDirectoryRequest;
import software.amazon.awssdk.transfer.s3.model.UploadFileRequest;

/**
 * Tests how uploadDirectory applies {@code followSymbolicLinks} when it reads the files it listed.
 */
public class UploadDirectoryFollowSymbolicLinksTest {
    private static final byte[] IN_TREE = "file content under the root dir!".getBytes(StandardCharsets.UTF_8);
    private static final byte[] OTHER_CONTENT = "other content, not under source!".getBytes(StandardCharsets.UTF_8);

    @TempDir
    Path temp;

    private Path source;
    private Path otherDir;
    private BodyCollectingS3Client s3Client;
    private S3TransferManager transferManager;

    @BeforeEach
    public void setUp() throws IOException {
        source = Files.createDirectory(temp.resolve("source"));
        otherDir = Files.createDirectory(temp.resolve("other"));
        Files.write(otherDir.resolve("data.txt"), OTHER_CONTENT);
        Files.write(source.resolve("safe.txt"), IN_TREE);

        s3Client = new BodyCollectingS3Client();
        transferManager = S3TransferManager.builder().s3Client(s3Client).build();
    }

    @AfterEach
    public void tearDown() {
        if (transferManager != null) {
            transferManager.close();
        }
    }

    @Test
    void uploadDirectory_fileBecomesSymlinkBeforeUpload_fileFails() throws Exception {
        Path data = Files.write(source.resolve("data.txt"), IN_TREE);

        UploadOutcome outcome = uploadDirectory(b -> b.uploadFileRequestTransformer(runWhenUploading(data, () -> {
            Files.delete(data);
            Files.createSymbolicLink(data, otherDir.resolve("data.txt"));
        })));

        assertOtherContentNotUploaded();
        assertThat(outcome.failed("data.txt")).isTrue();
    }

    @Test
    void uploadDirectory_parentBecomesSymlinkBeforeUpload_fileFails() throws Exception {
        Path sub = Files.createDirectory(source.resolve("sub"));
        Path data = Files.write(sub.resolve("data.txt"), IN_TREE);

        UploadOutcome outcome = uploadDirectory(b -> b.uploadFileRequestTransformer(runWhenUploading(data, () -> {
            Files.move(sub, source.resolve("sub-moved"));
            Files.createSymbolicLink(sub, otherDir);
        })));

        assertOtherContentNotUploaded();
        assertThat(outcome.failed("sub/data.txt")).isTrue();
    }

    @Test
    void uploadDirectory_fileBecomesSymlinkDuringUpload_fileFails() throws Exception {
        Path data = Files.write(source.resolve("data.txt"), IN_TREE);
        sameModifiedTime(data);
        s3Client.beforeSubscribe("data.txt", () -> {
            Files.delete(data);
            Files.createSymbolicLink(data, otherDir.resolve("data.txt"));
        });

        UploadOutcome outcome = uploadDirectory(b -> { });

        assertOtherContentNotUploaded();
        assertThat(outcome.result).isNotNull();
        assertThat(outcome.result.failedTransfers())
            .extracting(f -> f.request().putObjectRequest().key())
            .containsExactly("data.txt");
        assertThat(s3Client.uploaded).containsOnlyKeys("safe.txt");
        assertThat(s3Client.uploaded.get("safe.txt")).isEqualTo(IN_TREE);
    }

    @Test
    void uploadDirectory_parentBecomesSymlinkDuringUpload_fileFails() throws Exception {
        Path sub = Files.createDirectory(source.resolve("sub"));
        sameModifiedTime(Files.write(sub.resolve("data.txt"), IN_TREE));
        s3Client.beforeSubscribe("sub/data.txt", () -> {
            Files.move(sub, source.resolve("sub-moved"));
            Files.createSymbolicLink(sub, otherDir);
        });

        UploadOutcome outcome = uploadDirectory(b -> { });

        assertOtherContentNotUploaded();
        assertThat(outcome.result).isNotNull();
        assertThat(outcome.result.failedTransfers())
            .extracting(f -> f.request().putObjectRequest().key())
            .containsExactly("sub/data.txt");
    }

    @Test
    void uploadDirectory_noSymlinks_allFilesUploaded() throws Exception {
        Files.write(Files.createDirectory(source.resolve("sub")).resolve("data.txt"), IN_TREE);

        UploadOutcome outcome = uploadDirectory(b -> { });

        assertThat(outcome.result).isNotNull();
        assertThat(outcome.result.failedTransfers()).isEmpty();
        assertThat(s3Client.uploaded).containsOnlyKeys("safe.txt", "sub/data.txt");
        assertThat(s3Client.uploaded.get("sub/data.txt")).isEqualTo(IN_TREE);
    }

    @Test
    void uploadDirectory_followSymbolicLinksTrue_linkIsFollowed() throws Exception {
        Files.createSymbolicLink(source.resolve("link.txt"), otherDir.resolve("data.txt"));

        UploadOutcome outcome = uploadDirectory(b -> b.followSymbolicLinks(true));

        assertThat(outcome.result).isNotNull();
        assertThat(outcome.result.failedTransfers()).isEmpty();
        assertThat(s3Client.uploaded.get("link.txt")).isEqualTo(OTHER_CONTENT);
    }

    private UploadOutcome uploadDirectory(Consumer<UploadDirectoryRequest.Builder> customizer) {
        UploadDirectoryRequest.Builder builder = UploadDirectoryRequest.builder().source(source).bucket("bucket");
        customizer.accept(builder);
        UploadOutcome outcome = new UploadOutcome();
        try {
            outcome.result = transferManager.uploadDirectory(builder.build()).completionFuture().get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            outcome.error = e;
        }
        return outcome;
    }

    /**
     * Gives the file in {@code otherDir} the same modified time as {@code data}.
     */
    private void sameModifiedTime(Path data) throws IOException {
        Files.setLastModifiedTime(otherDir.resolve("data.txt"), Files.getLastModifiedTime(data));
    }

    private void assertOtherContentNotUploaded() {
        for (byte[] body : s3Client.received.values()) {
            assertThat(new String(body, StandardCharsets.UTF_8)).doesNotContain("other content");
        }
    }

    private static Consumer<UploadFileRequest.Builder> runWhenUploading(Path file, IoRunnable action) {
        return b -> {
            if (b.build().source().equals(file)) {
                try {
                    action.run();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        };
    }

    private interface IoRunnable {
        void run() throws IOException;
    }

    private static final class UploadOutcome {
        private CompletedDirectoryUpload result;
        private Exception error;

        /**
         * Whether the file's upload failed, either on its own or by failing the whole directory upload.
         */
        boolean failed(String key) {
            if (error != null) {
                return true;
            }
            return result.failedTransfers().stream().anyMatch(f -> f.request().putObjectRequest().key().equals(key));
        }
    }

    /**
     * Reads every request body to the end, like an HTTP client would, and records the bytes it received.
     */
    private static final class BodyCollectingS3Client implements S3AsyncClient {
        private final Map<String, byte[]> received = new ConcurrentHashMap<>();
        private final Map<String, byte[]> uploaded = new ConcurrentHashMap<>();
        private final Map<String, IoRunnable> beforeSubscribe = new ConcurrentHashMap<>();

        void beforeSubscribe(String key, IoRunnable action) {
            beforeSubscribe.put(key, action);
        }

        @Override
        public CompletableFuture<PutObjectResponse> putObject(PutObjectRequest request, AsyncRequestBody body) {
            CompletableFuture<PutObjectResponse> future = new CompletableFuture<>();
            IoRunnable action = beforeSubscribe.get(request.key());
            if (action != null) {
                try {
                    action.run();
                } catch (IOException e) {
                    future.completeExceptionally(e);
                    return future;
                }
            }

            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            body.subscribe(new Subscriber<ByteBuffer>() {
                @Override
                public void onSubscribe(Subscription s) {
                    s.request(Long.MAX_VALUE);
                }

                @Override
                public void onNext(ByteBuffer byteBuffer) {
                    byte[] chunk = new byte[byteBuffer.remaining()];
                    byteBuffer.get(chunk);
                    synchronized (bytes) {
                        bytes.write(chunk, 0, chunk.length);
                        received.put(request.key(), bytes.toByteArray());
                    }
                }

                @Override
                public void onError(Throwable t) {
                    future.completeExceptionally(t);
                }

                @Override
                public void onComplete() {
                    synchronized (bytes) {
                        uploaded.put(request.key(), bytes.toByteArray());
                    }
                    future.complete(PutObjectResponse.builder().build());
                }
            });
            return future;
        }

        @Override
        public String serviceName() {
            return "s3";
        }

        @Override
        public void close() {
        }
    }
}
