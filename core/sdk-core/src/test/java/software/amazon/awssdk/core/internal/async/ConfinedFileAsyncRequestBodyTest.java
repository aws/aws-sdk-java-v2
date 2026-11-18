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

package software.amazon.awssdk.core.internal.async;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncRequestBodySplitConfiguration;

/**
 * Tests {@link FileAsyncRequestBody} with a {@code confineToRoot}, which does not read through a symbolic link below the
 * root.
 */
public class ConfinedFileAsyncRequestBodyTest {
    private static final byte[] IN_TREE = "file content under the root dir!".getBytes(StandardCharsets.UTF_8);
    private static final byte[] OTHER_CONTENT = "other content, not under source!".getBytes(StandardCharsets.UTF_8);

    @TempDir
    Path temp;

    private Path root;
    private Path otherDir;

    @BeforeEach
    public void setUp() throws IOException {
        // Both files have the same length.
        assertThat(OTHER_CONTENT).hasSameSizeAs(IN_TREE);

        root = Files.createDirectory(temp.resolve("root"));
        otherDir = Files.createDirectory(temp.resolve("other"));
        Files.write(otherDir.resolve("data.txt"), OTHER_CONTENT);
    }

    @Test
    public void confinedNestedFile_streamsAllBytesOnEverySubscribe() throws Exception {
        Path dir = Files.createDirectories(root.resolve("a").resolve("b"));
        byte[] content = new byte[100_000];
        ThreadLocalRandom.current().nextBytes(content);
        Path file = Files.write(dir.resolve("file.bin"), content);

        FileAsyncRequestBody body = confined(file).chunkSizeInBytes(1000).build();

        assertThat(body.confineToRoot()).isEqualTo(root);
        assertThat(body.contentLength()).contains((long) content.length);
        assertThat(read(body).get(10, TimeUnit.SECONDS)).isEqualTo(content);
        assertThat(read(body).get(10, TimeUnit.SECONDS)).isEqualTo(content);
    }

    @Test
    public void fileBecomesSymlinkAfterBuild_readFails() throws Exception {
        Path data = Files.write(Files.createDirectory(root.resolve("sub")).resolve("data.txt"), IN_TREE);
        FileAsyncRequestBody body = confined(data).build();

        Files.delete(data);
        Files.createSymbolicLink(data, otherDir.resolve("data.txt"));

        assertReadFailsWithNoBytes(body);
    }

    @Test
    public void parentBecomesSymlinkAfterBuild_readFails() throws Exception {
        Path sub = Files.createDirectory(root.resolve("sub"));
        Path data = Files.write(sub.resolve("data.txt"), IN_TREE);
        FileAsyncRequestBody body = confined(data).build();

        Files.move(sub, root.resolve("sub-moved"));
        Files.createSymbolicLink(sub, otherDir);

        assertReadFailsWithNoBytes(body);
    }

    @Test
    public void fileBecomesSymlinkAfterFirstRead_resubscribeFails() throws Exception {
        Path data = Files.write(root.resolve("data.txt"), IN_TREE);
        FileAsyncRequestBody body = confined(data).build();
        assertThat(read(body).get(10, TimeUnit.SECONDS)).isEqualTo(IN_TREE);

        Files.delete(data);
        Files.createSymbolicLink(data, otherDir.resolve("data.txt"));

        assertReadFailsWithNoBytes(body);
    }

    @Test
    public void symlinkInFinalComponentAtBuild_buildFails() throws Exception {
        Path link = Files.createSymbolicLink(root.resolve("data.txt"), otherDir.resolve("data.txt"));

        assertThatThrownBy(() -> confined(link).build()).isInstanceOf(UncheckedIOException.class);
    }

    @Test
    public void symlinkInIntermediateComponentAtBuild_buildFails() throws Exception {
        Path link = Files.createSymbolicLink(root.resolve("sub"), otherDir);

        assertThatThrownBy(() -> confined(link.resolve("data.txt")).build()).isInstanceOf(UncheckedIOException.class);
    }

    @Test
    public void directoryAsFinalComponent_buildFails() throws Exception {
        Path dir = Files.createDirectory(root.resolve("dir"));

        assertThatThrownBy(() -> confined(dir).build()).isInstanceOf(UncheckedIOException.class)
                                                         .hasMessageContaining("not a regular file");
    }

    @Test
    public void pathNotBelowRoot_buildFails() {
        assertThatThrownBy(() -> confined(otherDir.resolve("data.txt")).build())
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> confined(root).build()).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> confined(root.resolve("..").resolve("other").resolve("data.txt")).build())
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void dotOrDotDotComponentBelowRoot_buildFails() throws Exception {
        Path baz = Files.createDirectories(root.resolve("foo").resolve("bar").resolve("baz"));
        Files.write(root.resolve("foo").resolve("bar").resolve("file.txt"), IN_TREE);
        Path link = Files.createSymbolicLink(root.resolve("link"), Files.createDirectory(otherDir.resolve("inner")));

        // foo/bar/baz/.. names foo/bar, which is below the root, but a ".." component is still rejected.
        assertThatThrownBy(() -> confined(baz.resolve("..")).build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("component below the directory");
        assertThatThrownBy(() -> confined(baz.resolve("..").resolve("file.txt")).build())
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> confined(baz.resolve(".").resolve("..").resolve("file.txt")).build())
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> confined(link.resolve("..").resolve("data.txt")).build())
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void split_partsAreConfined() throws Exception {
        Path sub = Files.createDirectory(root.resolve("sub"));
        Path data = Files.write(sub.resolve("data.txt"), IN_TREE);
        FileAsyncRequestBody body = confined(data).chunkSizeInBytes(8).build();

        List<AsyncRequestBody> parts = new ArrayList<>();
        body.split(AsyncRequestBodySplitConfiguration.builder()
                                                     .chunkSizeInBytes(8L)
                                                     .bufferSizeInBytes(1024L)
                                                     .build())
            .subscribe(parts::add)
            .get(10, TimeUnit.SECONDS);
        assertThat(parts).hasSize(IN_TREE.length / 8);

        Files.move(sub, root.resolve("sub-moved"));
        Files.createSymbolicLink(sub, otherDir);

        for (AsyncRequestBody part : parts) {
            assertReadFailsWithNoBytes(part);
        }
    }

    @Test
    public void split_partsReadConfinedFile() throws Exception {
        Path data = Files.write(Files.createDirectory(root.resolve("sub")).resolve("data.txt"), IN_TREE);
        FileAsyncRequestBody body = confined(data).chunkSizeInBytes(8).build();

        List<AsyncRequestBody> parts = new ArrayList<>();
        body.split(AsyncRequestBodySplitConfiguration.builder()
                                                     .chunkSizeInBytes(8L)
                                                     .bufferSizeInBytes(1024L)
                                                     .build())
            .subscribe(parts::add)
            .get(10, TimeUnit.SECONDS);

        ByteArrayOutputStream all = new ByteArrayOutputStream();
        for (AsyncRequestBody part : parts) {
            all.write(read(part).get(10, TimeUnit.SECONDS));
        }
        assertThat(all.toByteArray()).isEqualTo(IN_TREE);
    }

    @Test
    public void unconfined_existingSymlinkIsStillFollowed() throws Exception {
        Path link = Files.createSymbolicLink(root.resolve("data.txt"), otherDir.resolve("data.txt"));

        FileAsyncRequestBody body = FileAsyncRequestBody.builder().path(link).build();

        assertThat(body.confineToRoot()).isNull();
        assertThat(read(body).get(10, TimeUnit.SECONDS)).isEqualTo(OTHER_CONTENT);
    }

    @Test
    public void confinedReads_runOnConfinedReadPool() throws Exception {
        byte[] content = new byte[10_000];
        ThreadLocalRandom.current().nextBytes(content);
        Path file = Files.write(root.resolve("file.bin"), content);
        Set<String> threadNames = ConcurrentHashMap.newKeySet();
        CompletableFuture<Void> done = new CompletableFuture<>();

        confined(file).chunkSizeInBytes(1000).build().subscribe(new Subscriber<ByteBuffer>() {
            @Override
            public void onSubscribe(Subscription s) {
                s.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(ByteBuffer byteBuffer) {
                threadNames.add(Thread.currentThread().getName());
            }

            @Override
            public void onError(Throwable t) {
                done.completeExceptionally(t);
            }

            @Override
            public void onComplete() {
                done.complete(null);
            }
        });

        done.get(10, TimeUnit.SECONDS);
        assertThat(threadNames).isNotEmpty().allMatch(name -> name.startsWith("sdk-confined-file-read"));
    }

    @Test
    public void cancelAfterFirstChunk_stopsReading() throws Exception {
        byte[] content = new byte[10_000];
        ThreadLocalRandom.current().nextBytes(content);
        Path file = Files.write(root.resolve("file.bin"), content);
        CompletableFuture<Integer> firstChunk = new CompletableFuture<>();
        AtomicInteger chunks = new AtomicInteger();
        AtomicReference<Throwable> error = new AtomicReference<>();

        confined(file).chunkSizeInBytes(1000).build().subscribe(new Subscriber<ByteBuffer>() {
            private Subscription subscription;

            @Override
            public void onSubscribe(Subscription s) {
                subscription = s;
                s.request(1);
            }

            @Override
            public void onNext(ByteBuffer byteBuffer) {
                chunks.incrementAndGet();
                subscription.cancel();
                firstChunk.complete(byteBuffer.remaining());
            }

            @Override
            public void onError(Throwable t) {
                error.set(t);
            }

            @Override
            public void onComplete() {
            }
        });

        assertThat(firstChunk.get(10, TimeUnit.SECONDS)).isEqualTo(1000);
        Thread.sleep(200);
        assertThat(chunks).hasValue(1);
        assertThat(error).hasValue(null);
    }

    private FileAsyncRequestBody.Builder confined(Path path) {
        return FileAsyncRequestBody.builder().path(path).confineToRoot(root);
    }

    private static void assertReadFailsWithNoBytes(AsyncRequestBody body) throws Exception {
        ReadResult result = subscribe(body);
        assertThatThrownBy(() -> result.future.get(10, TimeUnit.SECONDS)).isInstanceOf(ExecutionException.class);
        byte[] received = result.received.toByteArray();
        assertThat(new String(received, StandardCharsets.UTF_8)).doesNotContain("other content");
        assertThat(received).isEmpty();
    }

    private static CompletableFuture<byte[]> read(AsyncRequestBody body) {
        return subscribe(body).future;
    }

    private static ReadResult subscribe(AsyncRequestBody body) {
        ReadResult result = new ReadResult();
        body.subscribe(new Subscriber<ByteBuffer>() {
            @Override
            public void onSubscribe(Subscription s) {
                s.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(ByteBuffer byteBuffer) {
                byte[] bytes = new byte[byteBuffer.remaining()];
                byteBuffer.get(bytes);
                synchronized (result.received) {
                    result.received.write(bytes, 0, bytes.length);
                }
            }

            @Override
            public void onError(Throwable t) {
                result.future.completeExceptionally(t);
            }

            @Override
            public void onComplete() {
                synchronized (result.received) {
                    result.future.complete(result.received.toByteArray());
                }
            }
        });
        return result;
    }

    private static final class ReadResult {
        private final ByteArrayOutputStream received = new ByteArrayOutputStream();
        private final CompletableFuture<byte[]> future = new CompletableFuture<>();
    }
}
