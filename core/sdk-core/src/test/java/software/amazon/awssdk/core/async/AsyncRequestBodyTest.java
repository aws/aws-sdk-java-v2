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

package software.amazon.awssdk.core.async;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import io.reactivex.Flowable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.core.internal.util.Mimetype;
import software.amazon.awssdk.http.async.SimpleSubscriber;
import software.amazon.awssdk.utils.BinaryUtils;

public class AsyncRequestBodyTest {

    private static final String testString = "Hello!";
    private static final Path path;

    static {
        FileSystem fs = Jimfs.newFileSystem(Configuration.unix());
        path = fs.getPath("./test");
        try {
            Files.write(path, testString.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @ParameterizedTest
    @MethodSource("contentIntegrityChecks")
    void hasCorrectLength(AsyncRequestBody asyncRequestBody) {
        assertEquals(testString.length(), asyncRequestBody.contentLength().get());
    }


    @ParameterizedTest
    @MethodSource("contentIntegrityChecks")
    void hasCorrectContent(AsyncRequestBody asyncRequestBody) throws InterruptedException {
        StringBuilder sb = new StringBuilder();
        CountDownLatch done = new CountDownLatch(1);

        Subscriber<ByteBuffer> subscriber = new SimpleSubscriber(buffer -> {
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            sb.append(new String(bytes, StandardCharsets.UTF_8));
        }) {
            @Override
            public void onError(Throwable t) {
                super.onError(t);
                done.countDown();
            }

            @Override
            public void onComplete() {
                super.onComplete();
                done.countDown();
            }
        };

        asyncRequestBody.subscribe(subscriber);
        done.await();
        assertEquals(testString, sb.toString());
    }

    private static AsyncRequestBody[] contentIntegrityChecks() {
        return new AsyncRequestBody[] {
            AsyncRequestBody.fromString(testString),
            AsyncRequestBody.fromFile(path)
        };
    }

    @Test
    void fromBytesCopiesTheProvidedByteArray() {
        byte[] bytes = testString.getBytes(StandardCharsets.UTF_8);
        byte[] bytesClone = bytes.clone();

        AsyncRequestBody asyncRequestBody = AsyncRequestBody.fromBytes(bytes);

        for (int i = 0; i < bytes.length; i++) {
            bytes[i] += 1;
        }

        AtomicReference<ByteBuffer> publishedBuffer = new AtomicReference<>();
        Subscriber<ByteBuffer> subscriber = new SimpleSubscriber(publishedBuffer::set);

        asyncRequestBody.subscribe(subscriber);

        byte[] publishedByteArray = BinaryUtils.copyAllBytesFrom(publishedBuffer.get());
        assertArrayEquals(bytesClone, publishedByteArray);
    }

    @Test
    void fromBytesUnsafeDoesNotCopyTheProvidedByteArray() {
        byte[] bytes = testString.getBytes(StandardCharsets.UTF_8);

        AsyncRequestBody asyncRequestBody = AsyncRequestBody.fromBytesUnsafe(bytes);

        for (int i = 0; i < bytes.length; i++) {
            bytes[i] += 1;
        }

        AtomicReference<ByteBuffer> publishedBuffer = new AtomicReference<>();
        Subscriber<ByteBuffer> subscriber = new SimpleSubscriber(publishedBuffer::set);

        asyncRequestBody.subscribe(subscriber);

        byte[] publishedByteArray = BinaryUtils.copyAllBytesFrom(publishedBuffer.get());
        assertArrayEquals(bytes, publishedByteArray);
    }

    @ParameterizedTest
    @MethodSource("safeByteBufferBodyBuilders")
    void safeByteBufferBuildersCopyTheProvidedBuffer(Function<ByteBuffer, AsyncRequestBody> bodyBuilder) {
        byte[] bytes = testString.getBytes(StandardCharsets.UTF_8);
        byte[] bytesClone = bytes.clone();

        AsyncRequestBody asyncRequestBody = bodyBuilder.apply(ByteBuffer.wrap(bytes));

        for (int i = 0; i < bytes.length; i++) {
            bytes[i] += 1;
        }

        AtomicReference<ByteBuffer> publishedBuffer = new AtomicReference<>();
        Subscriber<ByteBuffer> subscriber = new SimpleSubscriber(publishedBuffer::set);

        asyncRequestBody.subscribe(subscriber);

        byte[] publishedByteArray = BinaryUtils.copyAllBytesFrom(publishedBuffer.get());
        assertArrayEquals(bytesClone, publishedByteArray);
    }

    private static Function<ByteBuffer, AsyncRequestBody>[] safeByteBufferBodyBuilders() {
        Function<ByteBuffer, AsyncRequestBody> fromByteBuffer = AsyncRequestBody::fromByteBuffer;
        Function<ByteBuffer, AsyncRequestBody> fromRemainingByteBuffer = AsyncRequestBody::fromRemainingByteBuffer;
        Function<ByteBuffer, AsyncRequestBody> fromByteBuffers = AsyncRequestBody::fromByteBuffers;
        Function<ByteBuffer, AsyncRequestBody> fromRemainingByteBuffers = AsyncRequestBody::fromRemainingByteBuffers;
        return new Function[] {fromByteBuffer, fromRemainingByteBuffer, fromByteBuffers, fromRemainingByteBuffers};
    }

    @ParameterizedTest
    @MethodSource("unsafeByteBufferBodyBuilders")
    void unsafeByteBufferBuildersDoNotCopyTheProvidedBuffer(Function<ByteBuffer, AsyncRequestBody> bodyBuilder) {
        byte[] bytes = testString.getBytes(StandardCharsets.UTF_8);

        AsyncRequestBody asyncRequestBody = bodyBuilder.apply(ByteBuffer.wrap(bytes));

        for (int i = 0; i < bytes.length; i++) {
            bytes[i] += 1;
        }

        AtomicReference<ByteBuffer> publishedBuffer = new AtomicReference<>();
        Subscriber<ByteBuffer> subscriber = new SimpleSubscriber(publishedBuffer::set);

        asyncRequestBody.subscribe(subscriber);

        byte[] publishedByteArray = BinaryUtils.copyAllBytesFrom(publishedBuffer.get());
        assertArrayEquals(bytes, publishedByteArray);
    }

    private static Function<ByteBuffer, AsyncRequestBody>[] unsafeByteBufferBodyBuilders() {
        Function<ByteBuffer, AsyncRequestBody> fromByteBuffer = AsyncRequestBody::fromByteBufferUnsafe;
        Function<ByteBuffer, AsyncRequestBody> fromRemainingByteBuffer = AsyncRequestBody::fromRemainingByteBufferUnsafe;
        Function<ByteBuffer, AsyncRequestBody> fromByteBuffers = AsyncRequestBody::fromByteBuffersUnsafe;
        Function<ByteBuffer, AsyncRequestBody> fromRemainingByteBuffers = AsyncRequestBody::fromRemainingByteBuffersUnsafe;
        return new Function[] {fromByteBuffer, fromRemainingByteBuffer, fromByteBuffers, fromRemainingByteBuffers};
    }

    @ParameterizedTest
    @MethodSource("nonRewindingByteBufferBodyBuilders")
    void nonRewindingByteBufferBuildersReadFromTheInputBufferPosition(
        Function<ByteBuffer, AsyncRequestBody> bodyBuilder) {
        byte[] bytes = testString.getBytes(StandardCharsets.UTF_8);
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        int expectedPosition = bytes.length / 2;
        bb.position(expectedPosition);

        AsyncRequestBody asyncRequestBody = bodyBuilder.apply(bb);

        AtomicReference<ByteBuffer> publishedBuffer = new AtomicReference<>();
        Subscriber<ByteBuffer> subscriber = new SimpleSubscriber(publishedBuffer::set);

        asyncRequestBody.subscribe(subscriber);

        int remaining = bb.remaining();
        assertEquals(remaining, publishedBuffer.get().remaining());
        for (int i = 0; i < remaining; i++) {
            assertEquals(bb.get(), publishedBuffer.get().get());
        }
    }

    private static Function<ByteBuffer, AsyncRequestBody>[] nonRewindingByteBufferBodyBuilders() {
        Function<ByteBuffer, AsyncRequestBody> fromRemainingByteBuffer = AsyncRequestBody::fromRemainingByteBuffer;
        Function<ByteBuffer, AsyncRequestBody> fromRemainingByteBufferUnsafe = AsyncRequestBody::fromRemainingByteBufferUnsafe;
        Function<ByteBuffer, AsyncRequestBody> fromRemainingByteBuffers = AsyncRequestBody::fromRemainingByteBuffers;
        Function<ByteBuffer, AsyncRequestBody> fromRemainingByteBuffersUnsafe = AsyncRequestBody::fromRemainingByteBuffersUnsafe;
        return new Function[] {fromRemainingByteBuffer, fromRemainingByteBufferUnsafe, fromRemainingByteBuffers,
                               fromRemainingByteBuffersUnsafe};
    }

    @ParameterizedTest
    @MethodSource("safeNonRewindingByteBufferBodyBuilders")
    void safeNonRewindingByteBufferBuildersCopyFromTheInputBufferPosition(
        Function<ByteBuffer, AsyncRequestBody> bodyBuilder) {
        byte[] bytes = testString.getBytes(StandardCharsets.UTF_8);
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        int expectedPosition = bytes.length / 2;
        bb.position(expectedPosition);

        AsyncRequestBody asyncRequestBody = bodyBuilder.apply(bb);

        AtomicReference<ByteBuffer> publishedBuffer = new AtomicReference<>();
        Subscriber<ByteBuffer> subscriber = new SimpleSubscriber(publishedBuffer::set);

        asyncRequestBody.subscribe(subscriber);

        int remaining = bb.remaining();
        assertEquals(remaining, publishedBuffer.get().capacity());
        for (int i = 0; i < remaining; i++) {
            assertEquals(bb.get(), publishedBuffer.get().get());
        }
    }

    private static Function<ByteBuffer, AsyncRequestBody>[] safeNonRewindingByteBufferBodyBuilders() {
        Function<ByteBuffer, AsyncRequestBody> fromRemainingByteBuffer = AsyncRequestBody::fromRemainingByteBuffer;
        Function<ByteBuffer, AsyncRequestBody> fromRemainingByteBuffers = AsyncRequestBody::fromRemainingByteBuffers;
        return new Function[] {fromRemainingByteBuffer, fromRemainingByteBuffers};
    }

    @ParameterizedTest
    @MethodSource("rewindingByteBufferBodyBuilders")
    void rewindingByteBufferBuildersDoNotRewindTheInputBuffer(Function<ByteBuffer, AsyncRequestBody> bodyBuilder) {
        byte[] bytes = testString.getBytes(StandardCharsets.UTF_8);
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        int expectedPosition = bytes.length / 2;
        bb.position(expectedPosition);

        AsyncRequestBody asyncRequestBody = bodyBuilder.apply(bb);

        Subscriber<ByteBuffer> subscriber = new SimpleSubscriber(buffer -> {
        });

        asyncRequestBody.subscribe(subscriber);

        assertEquals(expectedPosition, bb.position());
    }

    @ParameterizedTest
    @MethodSource("rewindingByteBufferBodyBuilders")
    void rewindingByteBufferBuildersReadTheInputBufferFromTheBeginning(
        Function<ByteBuffer, AsyncRequestBody> bodyBuilder) {
        byte[] bytes = testString.getBytes(StandardCharsets.UTF_8);
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        bb.position(bytes.length / 2);

        AsyncRequestBody asyncRequestBody = bodyBuilder.apply(bb);

        AtomicReference<ByteBuffer> publishedBuffer = new AtomicReference<>();
        Subscriber<ByteBuffer> subscriber = new SimpleSubscriber(publishedBuffer::set);

        asyncRequestBody.subscribe(subscriber);

        assertEquals(0, publishedBuffer.get().position());
        publishedBuffer.get().rewind();
        bb.rewind();
        assertEquals(bb, publishedBuffer.get());
    }

    private static Function<ByteBuffer, AsyncRequestBody>[] rewindingByteBufferBodyBuilders() {
        Function<ByteBuffer, AsyncRequestBody> fromByteBuffer = AsyncRequestBody::fromByteBuffer;
        Function<ByteBuffer, AsyncRequestBody> fromByteBufferUnsafe = AsyncRequestBody::fromByteBufferUnsafe;
        Function<ByteBuffer, AsyncRequestBody> fromByteBuffers = AsyncRequestBody::fromByteBuffers;
        Function<ByteBuffer, AsyncRequestBody> fromByteBuffersUnsafe = AsyncRequestBody::fromByteBuffersUnsafe;
        return new Function[] {fromByteBuffer, fromByteBufferUnsafe, fromByteBuffers, fromByteBuffersUnsafe};
    }

    @ParameterizedTest
    @ValueSource(strings = {"US-ASCII", "ISO-8859-1", "UTF-8", "UTF-16BE", "UTF-16LE", "UTF-16"})
    void charsetsAreConvertedToTheCorrectContentType(Charset charset) {
        AsyncRequestBody requestBody = AsyncRequestBody.fromString("hello world", charset);
        assertEquals("text/plain; charset=" + charset.name(), requestBody.contentType());
    }

    @Test
    void stringConstructorHasCorrectDefaultContentType() {
        AsyncRequestBody requestBody = AsyncRequestBody.fromString("hello world");
        assertEquals("text/plain; charset=UTF-8", requestBody.contentType());
    }

    @Test
    void fileConstructorHasCorrectContentType() {
        AsyncRequestBody requestBody = AsyncRequestBody.fromFile(path);
        assertEquals(Mimetype.MIMETYPE_OCTET_STREAM, requestBody.contentType());
    }

    @Test
    void bytesArrayConstructorHasCorrectContentType() {
        AsyncRequestBody requestBody = AsyncRequestBody.fromBytes("hello world".getBytes());
        assertEquals(Mimetype.MIMETYPE_OCTET_STREAM, requestBody.contentType());
    }

    @Test
    void bytesBufferConstructorHasCorrectContentType() {
        ByteBuffer byteBuffer = ByteBuffer.wrap("hello world".getBytes());
        AsyncRequestBody requestBody = AsyncRequestBody.fromByteBuffer(byteBuffer);
        assertEquals(Mimetype.MIMETYPE_OCTET_STREAM, requestBody.contentType());
    }

    @Test
    void emptyBytesConstructorHasCorrectContentType() {
        AsyncRequestBody requestBody = AsyncRequestBody.empty();
        assertEquals(Mimetype.MIMETYPE_OCTET_STREAM, requestBody.contentType());
    }

    @Test
    void publisherConstructorHasCorrectContentType() {
        List<String> requestBodyStrings = Lists.newArrayList("A", "B", "C");
        List<ByteBuffer> bodyBytes = requestBodyStrings.stream()
                                                       .map(s -> ByteBuffer.wrap(s.getBytes(StandardCharsets.UTF_8)))
                                                       .collect(Collectors.toList());
        Publisher<ByteBuffer> bodyPublisher = Flowable.fromIterable(bodyBytes);
        AsyncRequestBody requestBody = AsyncRequestBody.fromPublisher(bodyPublisher);
        assertEquals(Mimetype.MIMETYPE_OCTET_STREAM, requestBody.contentType());
    }
}
