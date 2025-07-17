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

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.Pair;
import software.amazon.awssdk.utils.async.AddingTrailingDataSubscriber;
import software.amazon.awssdk.utils.async.DelegatingSubscriber;
import software.amazon.awssdk.utils.async.FlatteningSubscriber;
import software.amazon.awssdk.utils.internal.MappingSubscriber;

/**
 * An implementation of chunk-transfer encoding, but by wrapping a {@link Publisher} of {@link ByteBuffer}. This implementation
 * supports chunk-headers, chunk-extensions.
 * <p>
 * Per <a href="https://datatracker.ietf.org/doc/html/rfc7230#section-4.1">RFC-7230</a>, a chunk-transfer encoded message is
 * defined as:
 * <pre>
 *     chunked-body   = *chunk
 *                      last-chunk
 *                      trailer-part
 *                      CRLF
 *     chunk          = chunk-size [ chunk-ext ] CRLF
 *                      chunk-data CRLF
 *     chunk-size     = 1*HEXDIG
 *     last-chunk     = 1*("0") [ chunk-ext ] CRLF
 *     chunk-data     = 1*OCTET ; a sequence of chunk-size octets
 *
 *     chunk-ext      = *( ";" chunk-ext-name [ "=" chunk-ext-val ] )
 *     chunk-ext-name = token
 *     chunk-ext-val  = token / quoted-string
 * </pre>
 *
 * @see ChunkedEncodedInputStream
 */
@SdkInternalApi
public class ChunkedEncodedPublisher implements Publisher<ByteBuffer> {
    private static final byte[] CRLF = {'\r', '\n'};
    private static final byte SEMICOLON = ';';
    private static final byte EQUALS = '=';

    private final Publisher<ByteBuffer> wrapped;
    private final List<ChunkExtensionProvider> extensions = new ArrayList<>();
    private final int chunkSize;
    private ByteBuffer chunkBuffer;
    private final boolean addEmptyTrailingChunk;

    public ChunkedEncodedPublisher(Builder b) {
        this.wrapped = b.publisher;
        this.chunkSize = b.chunkSize;
        this.extensions.addAll(b.extensions);
        this.addEmptyTrailingChunk = b.addEmptyTrailingChunk;
    }

    @Override
    public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
        Publisher<Iterable<ByteBuffer>> chunked = chunk(wrapped);
        Publisher<Iterable<ByteBuffer>> trailingAdded = addTrailingChunks(chunked);
        Publisher<ByteBuffer> flattened = flatten(trailingAdded);
        Publisher<ByteBuffer> encoded = map(flattened, this::encodeChunk);

        encoded.subscribe(subscriber);
    }

    public static Builder builder() {
        return new Builder();
    }

    private Iterable<Iterable<ByteBuffer>> getTrailingChunks() {
        List<ByteBuffer> trailing = new ArrayList<>();

        if (chunkBuffer != null) {
            chunkBuffer.flip();
            if (chunkBuffer.hasRemaining()) {
                trailing.add(chunkBuffer);
            }
        }

        if (addEmptyTrailingChunk) {
            trailing.add(ByteBuffer.allocate(0));
        }

        return Collections.singletonList(trailing);
    }

    private Publisher<Iterable<ByteBuffer>> chunk(Publisher<ByteBuffer> upstream) {
        return subscriber -> {
            upstream.subscribe(new ChunkingSubscriber(subscriber));
        };
    }

    private Publisher<ByteBuffer> flatten(Publisher<Iterable<ByteBuffer>> upstream) {
        return subscriber -> upstream.subscribe(new FlatteningSubscriber<>(subscriber));
    }

    public Publisher<Iterable<ByteBuffer>> addTrailingChunks(Publisher<Iterable<ByteBuffer>> upstream) {
        return subscriber -> {
            upstream.subscribe(new AddingTrailingDataSubscriber<>(subscriber, this::getTrailingChunks));
        };
    }

    public Publisher<ByteBuffer> map(Publisher<ByteBuffer> upstream, Function<? super ByteBuffer, ? extends ByteBuffer> mapper) {
        return subscriber -> upstream.subscribe(MappingSubscriber.create(subscriber, mapper));
    }

    // TODO: Trailing checksum
    private ByteBuffer encodeChunk(ByteBuffer byteBuffer) {
        int contentLen = byteBuffer.remaining();
        byte[] chunkSizeHex = Integer.toHexString(contentLen).getBytes(StandardCharsets.UTF_8);

        List<Pair<byte[], byte[]>> chunkExtensions = this.extensions.stream()
                                                                    .map(e -> {
                                                                        ByteBuffer duplicate = byteBuffer.duplicate();
                                                                        return e.get(duplicate);
                                                                    }).collect(Collectors.toList());

        int extensionsLength = calculateExtensionsLength(chunkExtensions);

        int encodedLen = chunkSizeHex.length + extensionsLength + CRLF.length + contentLen + CRLF.length;

        ByteBuffer encoded = ByteBuffer.allocate(encodedLen);
        encoded.put(chunkSizeHex);

        chunkExtensions.forEach(p -> {
            encoded.put(SEMICOLON);
            encoded.put(p.left());
            if (p.right() != null && p.right().length > 0) {
                encoded.put(EQUALS);
                encoded.put(p.right());
            }
        });

        encoded.put(CRLF);
        encoded.put(byteBuffer);
        encoded.put(CRLF);

        encoded.flip();

        return encoded;
    }

    private int calculateExtensionsLength(List<Pair<byte[], byte[]>> chunkExtensions) {
        return chunkExtensions.stream()
                       .mapToInt(p -> {
                           int keyLen = p.left().length;
                           byte[] value = p.right();
                           if (value.length > 0) {
                               return 1 + keyLen + 1 + value.length; // ';ext-key=ext-value'
                           }
                           // ';ext-key
                           return 1 + keyLen;
                       }).sum();
    }

    private class ChunkingSubscriber extends DelegatingSubscriber<ByteBuffer, Iterable<ByteBuffer>> {
        protected ChunkingSubscriber(Subscriber<? super Iterable<ByteBuffer>> subscriber) {
            super(subscriber);
        }

        @Override
        public void onNext(ByteBuffer byteBuffer) {
            if (chunkBuffer == null) {
                chunkBuffer = ByteBuffer.allocate(chunkSize);
            }

            long totalBufferedBytes = (long) chunkBuffer.position() + byteBuffer.remaining();
            int nBufferedChunks = (int) (totalBufferedBytes / chunkSize);

            List<ByteBuffer> chunks = new ArrayList<>(nBufferedChunks);

            if (nBufferedChunks > 0) {
                for (int i = 0; i < nBufferedChunks; i++) {
                    ByteBuffer slice = byteBuffer.slice();
                    int maxBytesToCopy = Math.min(chunkBuffer.remaining(), slice.remaining());
                    slice.limit(maxBytesToCopy);

                    chunkBuffer.put(slice);
                    if (!chunkBuffer.hasRemaining()) {
                        chunkBuffer.flip();
                        chunks.add(chunkBuffer);
                        chunkBuffer = ByteBuffer.allocate(chunkSize);
                    }

                    byteBuffer.position(byteBuffer.position() + maxBytesToCopy);
                }

                if (byteBuffer.hasRemaining()) {
                    chunkBuffer.put(byteBuffer);
                }
            } else {
                chunkBuffer.put(byteBuffer);
            }

            subscriber.onNext(chunks);
        }
    }

    public static class Builder {
        private Publisher<ByteBuffer> publisher;
        private int chunkSize;
        private boolean addEmptyTrailingChunk;
        private final List<ChunkExtensionProvider> extensions = new ArrayList<>();

        public Builder publisher(Publisher<ByteBuffer> publisher) {
            this.publisher = publisher;
            return this;
        }

        public Builder chunkSize(int chunkSize) {
            this.chunkSize = chunkSize;
            return this;
        }

        public Builder addEmptyTrailingChunk(boolean addEmptyTrailingChunk) {
            this.addEmptyTrailingChunk = addEmptyTrailingChunk;
            return this;
        }

        public Builder addExtension(ChunkExtensionProvider extension) {
            this.extensions.add(extension);
            return this;
        }

        public ChunkedEncodedPublisher build() {
            return new ChunkedEncodedPublisher(this);
        }
    }
}
