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

package software.amazon.awssdk.http.auth.aws.internal.signer.chunkedencoding;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;

import io.reactivex.Flowable;
import io.reactivex.subscribers.TestSubscriber;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.PrimitiveIterator;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.checksums.DefaultChecksumAlgorithm;
import software.amazon.awssdk.checksums.SdkChecksum;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.Pair;

public class ChunkedEncodedPublisherTest {
    private static final int CHUNK_SIZE = 16 * 1024;
    private final Random RNG = new Random();
    private final SdkChecksum CRC32 = SdkChecksum.forAlgorithm(DefaultChecksumAlgorithm.CRC32);

    @BeforeEach
    public void setup() {
        CRC32.reset();
    }

    @Test
    public void subscribe_publisherEmpty_onlyProducesTrailer() {
        Publisher<ByteBuffer> emptyPublisher = Flowable.empty();

        ChunkedEncodedPublisher build = newChunkedBuilder(emptyPublisher)
            .addTrailer(() -> Pair.of("foo", Collections.singletonList("1")))
            .addTrailer(() -> Pair.of("bar", Collections.singletonList("2")))
            .addEmptyTrailingChunk(true)
            .contentLength(0)
            .build();

        List<ByteBuffer> chunks = getAllElements(build);

        assertThat(chunks.size()).isEqualTo(1);

        String trailerAsString = StandardCharsets.UTF_8.decode(chunks.get(0)).toString();

        assertThat(trailerAsString).isEqualTo(
            "0\r\n" +
            "foo:1\r\n" +
            "bar:2\r\n" +
            "\r\n");
    }

    @Test
    void subscribe_trailerProviderPresent_trailerPartAdded() {
        int contentLength = 8;
        TestPublisher upstream = randomPublisherOfLength(8);

        TrailerProvider trailerProvider = new StaticTrailerProvider("foo", "bar");

        ChunkedEncodedPublisher chunkedPublisher = ChunkedEncodedPublisher.builder()
                                                                          .publisher(upstream)
                                                                          .contentLength(contentLength)
                                                                          .chunkSize(CHUNK_SIZE)
                                                                          .addEmptyTrailingChunk(true)
                                                                          .addTrailer(trailerProvider)
                                                                          .build();

        List<ByteBuffer> chunks = getAllElements(chunkedPublisher);

        String expectedTrailer = "foo:bar";
        String trailerAsString = StandardCharsets.UTF_8.decode(chunks.get(1)).toString().trim();
        assertThat(trailerAsString).endsWith(expectedTrailer);
    }

    @Test
    void subscribe_trailerProviderPresent_multipleValues_trailerPartAdded() {
        int contentLength = 8;
        TestPublisher upstream = randomPublisherOfLength(contentLength);

        TrailerProvider trailerProvider = new StaticTrailerProvider("foo", Arrays.asList("bar1", "bar2", "bar3"));

        ChunkedEncodedPublisher chunkedPublisher = ChunkedEncodedPublisher.builder()
                                                                          .publisher(upstream)
                                                                          .contentLength(contentLength)
                                                                          .chunkSize(CHUNK_SIZE)
                                                                          .addEmptyTrailingChunk(true)
                                                                          .addTrailer(trailerProvider)
                                                                          .build();

        List<ByteBuffer> chunks = getAllElements(chunkedPublisher);

        String expectedTrailer = "foo:bar1,bar2,bar3";
        String trailerAsString = StandardCharsets.UTF_8.decode(chunks.get(1)).toString().trim();
        assertThat(trailerAsString).endsWith(expectedTrailer);
    }

    @Test
    void subscribe_trailerProviderPresent_onlyInvokedOnce() {
        int contentLength = 8;
        TestPublisher upstream = randomPublisherOfLength(contentLength);

        TrailerProvider trailerProvider = Mockito.spy(new StaticTrailerProvider("foo", "bar"));

        ChunkedEncodedPublisher chunkedPublisher = ChunkedEncodedPublisher.builder()
                                                                          .publisher(upstream)
                                                                          .addEmptyTrailingChunk(true)
                                                                          .chunkSize(CHUNK_SIZE)
                                                                          .contentLength(contentLength)
                                                                          .addTrailer(trailerProvider).build();

        getAllElements(chunkedPublisher);

        Mockito.verify(trailerProvider, Mockito.times(1)).get();
    }

    @Test
    void subscribe_trailerPresent_trailerFormattedCorrectly() {
        int contentLength = 32;
        TestPublisher testPublisher = randomPublisherOfLength(contentLength);

        TrailerProvider trailerProvider = new StaticTrailerProvider("foo", "bar");

        ChunkedEncodedPublisher chunkedPublisher = newChunkedBuilder(testPublisher)
            .addTrailer(trailerProvider)
            .addEmptyTrailingChunk(true)
            .contentLength(contentLength)
            .build();

        List<ByteBuffer> chunks = getAllElements(chunkedPublisher);

        ByteBuffer last = chunks.get(chunks.size() - 1);

        String expected = "0\r\n" +
                          "foo:bar\r\n" +
                          "\r\n";

        assertThat(chunkAsString(last)).isEqualTo(expected);
    }

    @Test
    void subscribe_wrappedDoesNotFillBuffer_allDataInSingleChunk() {
        byte[] content = "hello world".getBytes(StandardCharsets.UTF_8);
        ByteBuffer element = ByteBuffer.wrap(content);
        Flowable<ByteBuffer> upstream = Flowable.just(element.duplicate());

        ChunkedEncodedPublisher publisher = ChunkedEncodedPublisher.builder()
                                                                   .chunkSize(CHUNK_SIZE)
                                                                   .contentLength(content.length)
                                                                   .publisher(upstream)
                                                                   .build();

        List<ByteBuffer> chunks = getAllElements(publisher);

        assertThat(chunks.size()).isEqualTo(1);
        assertThat(stripEncoding(chunks.get(0)))
            .isEqualTo(element);
    }

    @Test
    void subscribe_extensionHasNoValue_formattedCorrectly() {
        int contentLength = 8;
        TestPublisher testPublisher = randomPublisherOfLength(contentLength);

        ChunkExtensionProvider extensionProvider = new StaticExtensionProvider("foo", "");

        ChunkedEncodedPublisher chunkPublisher =
            ChunkedEncodedPublisher.builder()
                                   .publisher(testPublisher)
                                   .addExtension(extensionProvider)
                                   .chunkSize(CHUNK_SIZE)
                                   .contentLength(contentLength)
                                   .build();

        List<ByteBuffer> chunks = getAllElements(chunkPublisher);

        assertThat(getHeaderAsString(chunks.get(0))).endsWith(";foo");
    }

    @Test
    void subscribe_multipleExtensions_formattedCorrectly() {
        int contentLength = 8;
        TestPublisher testPublisher = randomPublisherOfLength(contentLength);

        ChunkedEncodedPublisher.Builder chunkPublisher =
            ChunkedEncodedPublisher.builder()
                                   .publisher(testPublisher)
                                   .contentLength(contentLength)
                                   .chunkSize(CHUNK_SIZE);

        Stream.of("1", "2", "3")
              .map(v -> new StaticExtensionProvider("key" + v, "value" + v))
              .forEach(chunkPublisher::addExtension);

        List<ByteBuffer> chunks = getAllElements(chunkPublisher.build());

        chunks.forEach(chunk -> assertThat(getHeaderAsString(chunk)).endsWith(";key1=value1;key2=value2;key3=value3"));
    }

    @Test
    void subscribe_randomElementSizes_dataChunkedCorrectly() {
        for (int i = 0; i < 512; ++i) {
            int nChunks = 24;
            int contentLength = nChunks * CHUNK_SIZE;
            TestPublisher byteBufferPublisher = randomPublisherOfLength(contentLength);

            ChunkedEncodedPublisher chunkedPublisher = ChunkedEncodedPublisher.builder()
                                                                              .publisher(byteBufferPublisher)
                                                                              .contentLength(contentLength)
                                                                              .chunkSize(CHUNK_SIZE)
                                                                              .build();

            List<ByteBuffer> chunks = getAllElements(chunkedPublisher);

            List<ByteBuffer> stripped = chunks.stream().map(this::stripEncoding).collect(Collectors.toList());
            assertThat(stripped.size()).isEqualTo(nChunks);

            stripped.forEach(chunk -> assertThat(chunk.remaining()).isEqualTo(CHUNK_SIZE));

            CRC32.reset();
            stripped.forEach(CRC32::update);

            assertThat(CRC32.getChecksumBytes()).isEqualTo(byteBufferPublisher.wrappedChecksum);
        }
    }

    @Test
    void subscribe_randomElementSizes_chunksHaveExtensions_dataChunkedCorrectly() {
        for (int i = 0; i < 512; ++i) {
            int nChunks = 24;
            int contentLength = CHUNK_SIZE * nChunks;
            TestPublisher byteBufferPublisher = randomPublisherOfLength(contentLength);

            StaticExtensionProvider extensionProvider = Mockito.spy(new StaticExtensionProvider("foo", "bar"));

            ChunkedEncodedPublisher chunkedPublisher = ChunkedEncodedPublisher.builder()
                                                                              .publisher(byteBufferPublisher)
                                                                              .addExtension(extensionProvider)
                                                                              .chunkSize(CHUNK_SIZE)
                                                                              .contentLength(contentLength)
                                                                              .build();

            List<ByteBuffer> chunks = getAllElements(chunkedPublisher);
            assertThat(chunks.size()).isEqualTo(24);

            chunks.forEach(c -> {
                String header = StandardCharsets.UTF_8.decode(getHeader(c.duplicate())).toString();
                assertThat(header).isEqualTo("4000;foo=bar");
            });

            List<ByteBuffer> stripped = chunks.stream().map(this::stripEncoding).collect(Collectors.toList());

            assertThat(stripped.size()).isEqualTo(nChunks);

            stripped.forEach(chunk -> assertThat(chunk.remaining()).isEqualTo(CHUNK_SIZE));

            CRC32.reset();
            stripped.forEach(CRC32::update);

            assertThat(CRC32.getChecksumBytes()).isEqualTo(byteBufferPublisher.wrappedChecksum);
        }
    }

    @Test
    void subscribe_addTrailingChunkTrue_trailingChunkAdded() {
        int contentLength = CHUNK_SIZE * 2;
        TestPublisher testPublisher = randomPublisherOfLength(contentLength);

        ChunkedEncodedPublisher chunkedPublisher = ChunkedEncodedPublisher.builder()
                                                                          .publisher(testPublisher)
                                                                          .chunkSize(CHUNK_SIZE)
                                                                          .addEmptyTrailingChunk(true)
                                                                          .contentLength(contentLength)
                                                                          .build();

        List<ByteBuffer> chunks = getAllElements(chunkedPublisher);

        assertThat(chunks.size()).isEqualTo(3);

        ByteBuffer trailing = chunks.get(chunks.size() - 1);
        assertThat(stripEncoding(trailing).remaining()).isEqualTo(0);
    }

    @Test
    void subscribe_addTrailingChunkTrue_upstreamEmpty_trailingChunkAdded() {
        Publisher<ByteBuffer> empty = Flowable.empty();

        ChunkedEncodedPublisher chunkedPublisher =
            ChunkedEncodedPublisher.builder()
                                   .publisher(empty)
                                   .chunkSize(CHUNK_SIZE)
                                   .addEmptyTrailingChunk(true)
                                   .contentLength(0)
                                   .build();

        List<ByteBuffer> chunks = getAllElements(chunkedPublisher);

        assertThat(chunks.size()).isEqualTo(1);
    }

    @Test
    void subscribe_extensionsPresent_extensionsInvokedForEachChunk() {
        StaticExtensionProvider mockProvider = Mockito.spy(new StaticExtensionProvider("foo", "bar"));

        int chunkSize = CHUNK_SIZE;
        int nChunks = 16;
        int contentLength = chunkSize * nChunks;
        TestPublisher elements = randomPublisherOfLength(contentLength);

        ChunkedEncodedPublisher chunkPublisher = ChunkedEncodedPublisher.builder()
                                                                        .publisher(elements)
                                                                        .contentLength(contentLength)
                                                                        .chunkSize(chunkSize)
                                                                        .addExtension(mockProvider)
                                                                        .build();

        List<ByteBuffer> chunks = getAllElements(chunkPublisher);
        Mockito.verify(mockProvider, Mockito.times(nChunks)).get(any(ByteBuffer.class));

        for (int i = 0; i < chunks.size(); ++i) {
            ByteBuffer chunk = chunks.get(i);
            ByteBuffer extensionChunk = mockProvider.recordedChunks.get(i);

            assertThat(stripEncoding(chunk)).isEqualTo(extensionChunk);
        }
    }

    @Test
    void subscribe_wrappedExceedsContentLength_dataTruncatedToLength() {
        int contentLength = CHUNK_SIZE * 4 - 1;
        TestPublisher elements = randomPublisherOfLength(contentLength * 2);

        TestSubscriber<ByteBuffer> subscriber = new TestSubscriber<>();
        ChunkedEncodedPublisher chunkPublisher = newChunkedBuilder(elements).contentLength(contentLength)
                                                                            .build();

        chunkPublisher.subscribe(subscriber);

        subscriber.awaitTerminalEvent(30, TimeUnit.SECONDS);

        int totalRemaining = subscriber.values()
                                       .stream()
                                       .map(this::stripEncoding)
                                       .mapToInt(ByteBuffer::remaining)
                                       .sum();

        assertThat(totalRemaining).isEqualTo(contentLength);
    }

    private static ChunkedEncodedPublisher.Builder newChunkedBuilder(Publisher<ByteBuffer> publisher) {
        return ChunkedEncodedPublisher.builder().publisher(publisher).chunkSize(CHUNK_SIZE);
    }

    private TestPublisher randomPublisherOfLength(int bytes) {
        List<ByteBuffer> elements = new ArrayList<>();

        PrimitiveIterator.OfInt sizeIter = RNG.ints(16, 8192).iterator();

        CRC32.reset();
        while (bytes > 0) {
            int elementSize = sizeIter.next();
            elementSize = Math.min(elementSize, bytes);

            bytes -= elementSize;

            byte[] elementContent = new byte[elementSize];
            for (int i = 0; i < elementSize; ++i) {
                elementContent[i] = (byte) ('A' + RNG.nextInt(8));
            }
            CRC32.update(elementContent);
            elements.add(ByteBuffer.wrap(elementContent));
        }

        Flowable<ByteBuffer> publisher = Flowable.fromIterable(elements);

        return new TestPublisher(publisher, CRC32.getChecksumBytes());
    }

    private List<ByteBuffer> getAllElements(Publisher<ByteBuffer> publisher) {
        return Flowable.fromPublisher(publisher).toList().blockingGet();
    }

    private String chunkAsString(ByteBuffer chunk) {
        return StandardCharsets.UTF_8.decode(chunk).toString();
    }

    private String getHeaderAsString(ByteBuffer chunk) {
        return StandardCharsets.UTF_8.decode(getHeader(chunk)).toString();
    }

    private ByteBuffer getHeader(ByteBuffer chunk) {
        ByteBuffer header = chunk.duplicate();
        header.mark();
        byte a = header.get();
        byte b = header.get();

        int i = 2;
        for (; i < header.limit() && a != '\r' && b != '\n'; ++i) {
            a = b;
            b = header.get();
        }

        header.limit(i - 2);
        header.reset();
        return header;
    }

    private ByteBuffer stripEncoding(ByteBuffer chunk) {
        ByteBuffer header = getHeader(chunk);
        ByteBuffer lengthHex = header.duplicate();

        boolean semiFound = false;
        while (lengthHex.hasRemaining()) {
            byte b = lengthHex.get();
            if (b == ';') {
                semiFound = true;
                break;
            }
        }

        if (semiFound) {
            lengthHex.position(lengthHex.position() - 1);
        }
        // assume the whole line is the length (no extensions)
        lengthHex.flip();

        int length = Integer.parseInt(StandardCharsets.UTF_8.decode(lengthHex).toString(), 16);

        ByteBuffer stripped = chunk.duplicate();

        int chunkStart = header.remaining() + 2;
        stripped.position(chunkStart);
        stripped.limit(chunkStart + length);

        return stripped;
    }

    private long totalRemaining(List<ByteBuffer> buffers) {
        return buffers.stream().mapToLong(ByteBuffer::remaining).sum();
    }

    private static class TestPublisher implements Publisher<ByteBuffer> {
        private final Publisher<ByteBuffer> wrapped;
        private final byte[] wrappedChecksum;

        public TestPublisher(Publisher<ByteBuffer> wrapped, byte[] wrappedChecksum) {
            this.wrapped = wrapped;
            this.wrappedChecksum = new byte[wrappedChecksum.length];
            System.arraycopy(wrappedChecksum, 0, this.wrappedChecksum, 0, wrappedChecksum.length);
        }

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
            wrapped.subscribe(subscriber);
        }

        public byte[] wrappedChecksum() {
            return wrappedChecksum;
        }
    }

    private static class StaticExtensionProvider implements ChunkExtensionProvider {
        private final byte[] key;
        private final byte[] value;
        private final List<ByteBuffer> recordedChunks = new ArrayList<>();

        public StaticExtensionProvider(String key, String value) {
            this.key = key.getBytes(StandardCharsets.UTF_8);
            this.value = value == null ? null : value.getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public Pair<byte[], byte[]> get(ByteBuffer chunk) {
            this.recordedChunks.add(BinaryUtils.immutableCopyOf(chunk));
            return Pair.of(key, value);
        }
    }

    private static class StaticTrailerProvider implements TrailerProvider {
        private final String key;
        private final List<String> values;

        public StaticTrailerProvider(String key, String value) {
            this.key = key;
            this.values = Collections.singletonList(value);
        }

        public StaticTrailerProvider(String key, List<String> values) {
            this.key = key;
            this.values = values;
        }

        @Override
        public Pair<String, List<String>> get() {
            return Pair.of(key, values);
        }
    }
}
