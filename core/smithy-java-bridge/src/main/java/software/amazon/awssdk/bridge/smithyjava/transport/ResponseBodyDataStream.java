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

package software.amazon.awssdk.bridge.smithyjava.transport;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import org.reactivestreams.FlowAdapters;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.async.InputStreamSubscriber;
import software.amazon.smithy.java.io.datastream.DataStream;

/**
 * A one-shot {@link DataStream} over a transport's response-body publisher.
 *
 * <p>This is {@code DataStream.ofPublisher(publisher, type, length, false)} reimplemented, and it exists
 * only because the original cannot be read the way smithy-java's own codecs read it. In smithy-java 1.6.1,
 * {@code PublisherDataStream.asByteBuffer()} marks itself consumed and then subscribes through its own
 * public {@code subscribe()}, which checks the flag it has just set and throws "DataStream is not
 * replayable and has already been consumed". {@code asInputStream()} does not have the bug, because it
 * subscribes through a private path. So every non-streaming response — every XML document the
 * deserializer reads with {@code asByteBuffer()} — fails on first read. smithy-java's own transport never
 * shows it, because it hands the pipeline {@code InputStream}-backed bodies. See
 * {@code compatability_issues.md} 15.6.
 *
 * <p>Semantics are otherwise the original's: bytes arrive once, any second read fails, and reading
 * streams rather than buffers — {@link #asInputStream()} is v2's own {@link InputStreamSubscriber}, fed by
 * the publisher as the caller reads with a bounded buffer in between, so a virtual thread reading it parks
 * on each chunk rather than on the whole body. (Not the JDK's {@code BodySubscribers.ofInputStream()},
 * which is what smithy-java uses: {@code java.net.http} is outside {@code java.base}, and the SDK does not
 * take module dependencies it does not need.)
 */
@SdkInternalApi
public final class ResponseBodyDataStream implements DataStream {

    private final Publisher<ByteBuffer> publisher;
    private final String contentType;
    private final long contentLength;
    private final AtomicBoolean consumed = new AtomicBoolean();

    public ResponseBodyDataStream(Publisher<ByteBuffer> publisher, String contentType, long contentLength) {
        this.publisher = publisher;
        this.contentType = contentType;
        this.contentLength = contentLength;
    }

    @Override
    public long contentLength() {
        return contentLength;
    }

    @Override
    public String contentType() {
        return contentType;
    }

    @Override
    public boolean isReplayable() {
        return false;
    }

    @Override
    public boolean isAvailable() {
        return !consumed.get();
    }

    @Override
    public void subscribe(Flow.Subscriber<? super ByteBuffer> subscriber) {
        claim();
        publisher.subscribe(FlowAdapters.toSubscriber(subscriber));
    }

    @Override
    public InputStream asInputStream() {
        claim();
        InputStreamSubscriber stream = new InputStreamSubscriber();
        publisher.subscribe(stream);
        return stream;
    }

    @Override
    public ByteBuffer asByteBuffer() {
        try (InputStream in = asInputStream()) {
            return ByteBuffer.wrap(in.readAllBytes());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void claim() {
        if (!consumed.compareAndSet(false, true)) {
            throw new IllegalStateException("DataStream is not replayable and has already been consumed");
        }
    }
}
