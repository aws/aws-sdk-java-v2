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

import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncRequestBodySplitConfiguration;
import software.amazon.awssdk.core.async.CloseableAsyncRequestBody;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.exception.NonRetryableException;
import software.amazon.awssdk.core.internal.util.Mimetype;

/**
 * Tests for {@link ByteBuffersAsyncRequestBody#split(AsyncRequestBodySplitConfiguration)} and
 * {@link ByteBuffersAsyncRequestBody#splitCloseable(AsyncRequestBodySplitConfiguration)}.
 *
 * <p>Because all of the data is already resident in memory, each split part must be independently replayable so that a
 * failed part upload can be retried.
 */
class ByteBuffersAsyncRequestBodySplitTest {

    private static Stream<Arguments> splitTestCases() {
        byte[] content = bytesOfLength(100);
        return Stream.of(
            Arguments.of("chunk size divides content evenly", buffers(content), 10L, 10),
            Arguments.of("chunk size does not divide content evenly", buffers(content), 30L, 4),
            Arguments.of("chunk size equal to content length", buffers(content), 100L, 1),
            Arguments.of("chunk size larger than content length", buffers(content), 1000L, 1),
            Arguments.of("chunk size of one byte", buffers(bytesOfLength(5)), 1L, 5),
            // Each source buffer holds a distinct range of the content so that data assembled out of order is detected.
            Arguments.of("multiple source buffers, chunk spans buffers",
                         Arrays.asList(ByteBuffer.wrap(Arrays.copyOfRange(content, 0, 10)),
                                       ByteBuffer.wrap(Arrays.copyOfRange(content, 10, 20)),
                                       ByteBuffer.wrap(Arrays.copyOfRange(content, 20, 30))),
                         7L, 5),
            Arguments.of("multiple source buffers, chunk smaller than each buffer",
                         Arrays.asList(ByteBuffer.wrap(Arrays.copyOfRange(content, 0, 10)),
                                       ByteBuffer.wrap(Arrays.copyOfRange(content, 10, 20))),
                         4L, 5),
            Arguments.of("source buffers include an empty buffer",
                         Arrays.asList(ByteBuffer.wrap(Arrays.copyOfRange(content, 0, 5)),
                                       ByteBuffer.wrap(new byte[0]),
                                       ByteBuffer.wrap(Arrays.copyOfRange(content, 5, 10))),
                         5L, 2)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("splitTestCases")
    void splitCloseable_splitsAtChunkBoundariesPreservingContent(String description,
                                                                 List<ByteBuffer> sourceBuffers,
                                                                 long chunkSize,
                                                                 int expectedNumParts) throws Exception {
        byte[] expectedContent = concat(sourceBuffers);
        ByteBuffersAsyncRequestBody body = ByteBuffersAsyncRequestBody.of(sourceBuffers, expectedContent.length);

        List<CloseableAsyncRequestBody> parts = collectParts(body.splitCloseable(c -> c.chunkSizeInBytes(chunkSize)));

        assertThat(parts).hasSize(expectedNumParts);
        for (int i = 0; i < parts.size(); i++) {
            long expectedPartLength = i == parts.size() - 1
                                      ? expectedContent.length - (i * chunkSize)
                                      : chunkSize;
            assertThat(parts.get(i).contentLength()).hasValue(expectedPartLength);
        }

        assertThat(concatDrained(parts)).isEqualTo(expectedContent);
    }

    @ParameterizedTest(name = "chunk size {0}")
    @ValueSource(longs = {1L, 7L, 10L, 100L, 1000L})
    void splitCloseable_eachPartIsReplayable(long chunkSize) throws Exception {
        byte[] content = bytesOfLength(100);
        List<CloseableAsyncRequestBody> parts =
            collectParts(AsyncRequestBody.fromBytes(content).splitCloseable(c -> c.chunkSizeInBytes(chunkSize)));

        // Drain every part three times; each pass must reproduce the full content. The second and subsequent passes are
        // what a retry of an individual part upload does.
        for (int pass = 0; pass < 3; pass++) {
            assertThat(concatDrained(parts)).as("pass %d", pass).isEqualTo(content);
        }
    }

    @Test
    void splitCloseable_closingPart_doesNotAffectSiblingsOrSource() throws Exception {
        byte[] content = bytesOfLength(30);
        ByteBuffersAsyncRequestBody body = ByteBuffersAsyncRequestBody.from(content);

        List<CloseableAsyncRequestBody> parts = collectParts(body.splitCloseable(c -> c.chunkSizeInBytes(10L)));
        assertThat(parts).hasSize(3);

        parts.get(1).close();

        assertThat(drain(parts.get(0))).isEqualTo(Arrays.copyOfRange(content, 0, 10));
        assertThat(drain(parts.get(2))).isEqualTo(Arrays.copyOfRange(content, 20, 30));
        assertThat(drain(body)).isEqualTo(content);

        assertThatThrownBy(() -> drain(parts.get(1)))
            .hasCauseInstanceOf(NonRetryableException.class)
            .hasMessageContaining("AsyncRequestBody has been closed");
    }

    @Test
    void splitCloseable_closingSource_doesNotAffectParts() throws Exception {
        byte[] content = bytesOfLength(30);
        ByteBuffersAsyncRequestBody body = ByteBuffersAsyncRequestBody.from(content);

        List<CloseableAsyncRequestBody> parts = collectParts(body.splitCloseable(c -> c.chunkSizeInBytes(10L)));
        body.close();

        assertThat(concatDrained(parts)).isEqualTo(content);
    }

    @Test
    void splitCloseable_whenSourceIsClosed_shouldError() {
        ByteBuffersAsyncRequestBody body = ByteBuffersAsyncRequestBody.from(bytesOfLength(30));
        body.close();

        assertThatThrownBy(() -> collectParts(body.splitCloseable(c -> c.chunkSizeInBytes(10L))))
            .hasCauseInstanceOf(NonRetryableException.class)
            .hasMessageContaining("AsyncRequestBody has been closed");
    }

    @Test
    void splitCloseable_doesNotMutateSourceBuffers() throws Exception {
        ByteBuffer first = ByteBuffer.wrap(bytesOfLength(20));
        ByteBuffer second = ByteBuffer.wrap(bytesOfLength(20));
        second.position(5);
        int firstPosition = first.position();
        int secondPosition = second.position();

        ByteBuffersAsyncRequestBody body = ByteBuffersAsyncRequestBody.of(first, second);
        collectParts(body.splitCloseable(c -> c.chunkSizeInBytes(7L)));

        assertThat(first.position()).isEqualTo(firstPosition);
        assertThat(second.position()).isEqualTo(secondPosition);
    }

    /**
     * The parts view the source data rather than copying it, so a part must not be usable to mutate that data. Asserts
     * on the slices the parts actually hold: asserting on the buffers delivered to a subscriber would prove nothing,
     * since {@code ByteBuffersAsyncRequestBody} makes every published buffer read-only on the way out regardless.
     */
    @Test
    void splitCloseable_partSlicesAreReadOnly_soPartsCannotMutateSourceData() throws Exception {
        // fromBytesUnsafe does not copy, so the parts slice the caller's array directly.
        byte[] callerOwnedArray = bytesOfLength(20);
        List<CloseableAsyncRequestBody> parts = collectParts(
            AsyncRequestBody.fromBytesUnsafe(callerOwnedArray).splitCloseable(c -> c.chunkSizeInBytes(10L)));

        assertThat(parts).hasSize(2);
        for (CloseableAsyncRequestBody part : parts) {
            List<ByteBuffer> slices = ((ByteBuffersAsyncRequestBody) part).bufferedData();
            assertThat(slices).isNotEmpty().allMatch(ByteBuffer::isReadOnly);
            for (ByteBuffer slice : slices) {
                assertThatThrownBy(() -> slice.put(0, (byte) 99)).isInstanceOf(ReadOnlyBufferException.class);
            }
        }

        assertThat(callerOwnedArray).isEqualTo(bytesOfLength(20));
    }

    @Test
    void splitCloseable_emptyBody_shouldEmitSingleEmptyPart() throws Exception {
        List<CloseableAsyncRequestBody> parts =
            collectParts(AsyncRequestBody.empty().splitCloseable(c -> c.chunkSizeInBytes(10L)));

        assertThat(parts).hasSize(1);
        assertThat(parts.get(0).contentLength()).hasValue(0L);
        assertThat(drain(parts.get(0))).isEmpty();
    }

    @Test
    void splitCloseable_propagatesContentType() throws Exception {
        AsyncRequestBody body = AsyncRequestBody.fromString("hello world");
        assertThat(body.contentType()).isNotEqualTo(Mimetype.MIMETYPE_OCTET_STREAM);

        List<CloseableAsyncRequestBody> parts = collectParts(body.splitCloseable(c -> c.chunkSizeInBytes(4L)));

        assertThat(parts).isNotEmpty().allSatisfy(p -> assertThat(p.contentType()).isEqualTo(body.contentType()));
    }

    @Test
    void splitCloseable_propagatesBodyType() throws Exception {
        List<CloseableAsyncRequestBody> parts =
            collectParts(AsyncRequestBody.fromBytes(bytesOfLength(20)).splitCloseable(c -> c.chunkSizeInBytes(10L)));

        assertThat(parts).allSatisfy(p -> assertThat(p.body()).isEqualTo(AsyncRequestBody.BodyType.BYTES.getName()));
    }

    @Test
    void splitCloseable_nullConfiguration_shouldThrow() {
        AsyncRequestBodySplitConfiguration configuration = null;
        assertThatThrownBy(() -> AsyncRequestBody.fromBytes(bytesOfLength(10)).splitCloseable(configuration))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("splitConfiguration");
    }

    @Test
    void splitCloseable_noChunkSizeConfigured_shouldUseDefaultChunkSize() throws Exception {
        long defaultChunkSize = AsyncRequestBodySplitConfiguration.defaultConfiguration().chunkSizeInBytes();
        byte[] content = bytesOfLength(Math.toIntExact(defaultChunkSize + 1));

        List<CloseableAsyncRequestBody> parts = collectParts(
            AsyncRequestBody.fromBytes(content).splitCloseable(AsyncRequestBodySplitConfiguration.builder().build()));

        assertThat(parts).hasSize(2);
        assertThat(parts.get(0).contentLength()).hasValue(defaultChunkSize);
        assertThat(parts.get(1).contentLength()).hasValue(1L);
    }

    @Test
    @SuppressWarnings("deprecation")
    void legacySplit_producesReplayableParts() throws Exception {
        byte[] content = bytesOfLength(30);
        SdkPublisher<AsyncRequestBody> publisher =
            AsyncRequestBody.fromBytes(content).split(c -> c.chunkSizeInBytes(10L));

        List<AsyncRequestBody> parts = new ArrayList<>();
        subscribeAndCollect(publisher, parts);

        assertThat(parts).hasSize(3);
        assertThat(concatDrained(parts)).isEqualTo(content);
        assertThat(concatDrained(parts)).isEqualTo(content);
    }

    /**
     * Content where each byte encodes its own offset, so that content assembled in the wrong order is detected.
     */
    private static byte[] bytesOfLength(int length) {
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++) {
            bytes[i] = (byte) (i % 256);
        }
        return bytes;
    }

    private static List<ByteBuffer> buffers(byte[] content) {
        return Arrays.asList(ByteBuffer.wrap(content));
    }

    private static byte[] concat(List<ByteBuffer> buffers) {
        return concatBytes(buffers.stream()
                                  .map(b -> {
                                      ByteBuffer duplicate = b.duplicate();
                                      byte[] bytes = new byte[duplicate.remaining()];
                                      duplicate.get(bytes);
                                      return bytes;
                                  })
                                  .collect(Collectors.toList()));
    }

    private static byte[] concatBytes(List<byte[]> chunks) {
        int total = chunks.stream().mapToInt(c -> c.length).sum();
        byte[] result = new byte[total];
        int offset = 0;
        for (byte[] chunk : chunks) {
            System.arraycopy(chunk, 0, result, offset, chunk.length);
            offset += chunk.length;
        }
        return result;
    }

    private static byte[] concatDrained(List<? extends AsyncRequestBody> parts) throws Exception {
        List<byte[]> drained = new ArrayList<>();
        for (AsyncRequestBody part : parts) {
            drained.add(drain(part));
        }
        return concatBytes(drained);
    }

    private static List<CloseableAsyncRequestBody> collectParts(SdkPublisher<CloseableAsyncRequestBody> publisher)
        throws Exception {
        List<CloseableAsyncRequestBody> parts = new ArrayList<>();
        subscribeAndCollect(publisher, parts);
        return parts;
    }

    private static <T> void subscribeAndCollect(SdkPublisher<T> publisher, List<T> collected) throws Exception {
        CompletableFuture<Void> completed = new CompletableFuture<>();
        publisher.subscribe(new Subscriber<T>() {
            @Override
            public void onSubscribe(Subscription s) {
                s.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(T part) {
                collected.add(part);
            }

            @Override
            public void onError(Throwable t) {
                completed.completeExceptionally(t);
            }

            @Override
            public void onComplete() {
                completed.complete(null);
            }
        });
        completed.get(5, TimeUnit.SECONDS);
    }

    private static byte[] drain(AsyncRequestBody body) throws Exception {
        return concatBytes(drainToBuffers(body).stream()
                                               .map(b -> {
                                                   byte[] bytes = new byte[b.remaining()];
                                                   b.get(bytes);
                                                   return bytes;
                                               })
                                               .collect(Collectors.toList()));
    }

    private static List<ByteBuffer> drainToBuffers(AsyncRequestBody body) throws Exception {
        List<ByteBuffer> received = new ArrayList<>();
        CompletableFuture<Void> completed = new CompletableFuture<>();
        body.subscribe(new Subscriber<ByteBuffer>() {
            @Override
            public void onSubscribe(Subscription s) {
                s.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(ByteBuffer byteBuffer) {
                received.add(byteBuffer);
            }

            @Override
            public void onError(Throwable t) {
                completed.completeExceptionally(t);
            }

            @Override
            public void onComplete() {
                completed.complete(null);
            }
        });
        completed.get(5, TimeUnit.SECONDS);
        return received;
    }
}
