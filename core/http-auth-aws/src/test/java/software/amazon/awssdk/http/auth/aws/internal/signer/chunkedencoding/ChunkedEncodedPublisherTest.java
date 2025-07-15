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

import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.PrimitiveIterator;
import java.util.Random;
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
    void subscribe_wrappedDoesNotFillBuffer_allDataInSingleChunk() {
        ByteBuffer element = ByteBuffer.wrap("hello world".getBytes(StandardCharsets.UTF_8));
        Flowable<ByteBuffer> upstream = Flowable.just(element.duplicate());

        ChunkedEncodedPublisher publisher = ChunkedEncodedPublisher.builder()
                                                                   .chunkSize(CHUNK_SIZE)
                                                                   .publisher(upstream)
                                                                   .build();

        List<ByteBuffer> chunks = getAllElements(publisher);

        assertThat(chunks.size()).isEqualTo(1);
        assertThat(stripEncoding(chunks.get(0)))
            .isEqualTo(element);
    }

    @Test
    void subscribe_extensionHasNoValue_formattedCorrectly() {
        TestPublisher testPublisher = randomPublisherOfLength(8);

        ChunkExtensionProvider extensionProvider = new StaticExtensionProvider("foo", "");

        ChunkedEncodedPublisher chunkPublisher =
            ChunkedEncodedPublisher.builder()
                                   .publisher(testPublisher)
                                   .addExtension(extensionProvider)
                                   .chunkSize(CHUNK_SIZE)
                                   .build();

        List<ByteBuffer> chunks = getAllElements(chunkPublisher);

        assertThat(getHeaderAsString(chunks.get(0))).endsWith(";foo");
    }

    @Test
    void subscribe_multipleExtensions_formattedCorrectly() {
        TestPublisher testPublisher = randomPublisherOfLength(8);

        ChunkedEncodedPublisher.Builder chunkPublisher =
            ChunkedEncodedPublisher.builder()
                                   .publisher(testPublisher)
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
            TestPublisher byteBufferPublisher = randomPublisherOfLength(CHUNK_SIZE * 24);

            ChunkedEncodedPublisher chunkedPublisher = ChunkedEncodedPublisher.builder()
                                                                              .publisher(byteBufferPublisher)
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
            TestPublisher byteBufferPublisher = randomPublisherOfLength(CHUNK_SIZE * 24);

            StaticExtensionProvider extensionProvider = Mockito.spy(new StaticExtensionProvider("foo", "bar"));

            ChunkedEncodedPublisher chunkedPublisher = ChunkedEncodedPublisher.builder()
                                                                              .publisher(byteBufferPublisher)
                                                                              .addExtension(extensionProvider)
                                                                              .chunkSize(CHUNK_SIZE)
                                                                              .build();

            List<ByteBuffer> chunks = getAllElements(chunkedPublisher);

            chunks.forEach(c -> {
                String header = StandardCharsets.UTF_8.decode(getHeader(c)).toString();
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
        TestPublisher testPublisher = randomPublisherOfLength(CHUNK_SIZE * 2);

        ChunkedEncodedPublisher chunkedPublisher = ChunkedEncodedPublisher.builder()
                                                                          .publisher(testPublisher)
                                                                          .chunkSize(CHUNK_SIZE)
                                                                          .addEmptyTrailingChunk(true)
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
            ChunkedEncodedPublisher.builder().publisher(empty).chunkSize(CHUNK_SIZE).addEmptyTrailingChunk(true).build();

        List<ByteBuffer> chunks = getAllElements(chunkedPublisher);

        assertThat(chunks.size()).isEqualTo(1);
    }

    @Test
    void subscribe_extensionsPresent_extensionsInvokedForEachChunk() {
        ChunkExtensionProvider mockProvider = Mockito.spy(new StaticExtensionProvider("foo", "bar"));

        int nChunks = 16;
        TestPublisher elements = randomPublisherOfLength(nChunks * CHUNK_SIZE);

        ChunkedEncodedPublisher chunkPublisher =
            ChunkedEncodedPublisher.builder().publisher(elements).chunkSize(CHUNK_SIZE).addExtension(mockProvider).build();

        List<ByteBuffer> chunks = getAllElements(chunkPublisher);

        ArgumentCaptor<ByteBuffer> chunkCaptor = ArgumentCaptor.forClass(ByteBuffer.class);

        Mockito.verify(mockProvider, Mockito.times(nChunks)).get(chunkCaptor.capture());
        List<ByteBuffer> extensionChunks = chunkCaptor.getAllValues();

        for (int i = 0; i < chunks.size(); ++i) {
            ByteBuffer chunk = chunks.get(i);
            ByteBuffer extensionChunk = extensionChunks.get(i);
            assertThat(stripEncoding(chunk)).isEqualTo(extensionChunk);
        }
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
            RNG.nextBytes(elementContent);
            CRC32.update(elementContent);
            elements.add(ByteBuffer.wrap(elementContent));
        }

        Flowable<ByteBuffer> publisher = Flowable.fromIterable(elements);

        return new TestPublisher(publisher, CRC32.getChecksumBytes());
    }

    private List<ByteBuffer> getAllElements(Publisher<ByteBuffer> publisher) {
        return Flowable.fromPublisher(publisher).toList().blockingGet();
    }

    private String getHeaderAsString(ByteBuffer chunk) {
        return StandardCharsets.UTF_8.decode(getHeader(chunk)).toString();
    }

    private ByteBuffer getHeader(ByteBuffer chunk) {
        ByteBuffer header = chunk.duplicate();
        byte a = header.get(0);
        byte b = header.get(1);

        int i = 2;
        for (; i < header.limit() && a != '\r' && b != '\n'; ++i) {
            a = b;
            b = header.get(i);
        }

        header.limit(i - 2);
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

        public StaticExtensionProvider(String key, String value) {
            this.key = key.getBytes(StandardCharsets.UTF_8);
            this.value = value == null ? null : value.getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public Pair<byte[], byte[]> get(ByteBuffer chunk) {
            return Pair.of(key, value);
        }
    }
}
