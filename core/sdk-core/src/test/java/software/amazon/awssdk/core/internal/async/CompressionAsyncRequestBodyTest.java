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

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import io.reactivex.Flowable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncRequestBody.BodyType;
import software.amazon.awssdk.core.internal.compression.Compressor;
import software.amazon.awssdk.core.internal.compression.GzipCompressor;
import software.amazon.awssdk.core.internal.util.Mimetype;
import software.amazon.awssdk.http.async.SimpleSubscriber;

public final class CompressionAsyncRequestBodyTest {
    private static final Compressor compressor = new GzipCompressor();

    @ParameterizedTest
    @ValueSource(ints = {80, 1000})
    public void hasCorrectContent(int bodySize) throws Exception {
        String testString = createCompressibleStringOfGivenSize(bodySize);
        byte[] testBytes = testString.getBytes();
        int chunkSize = 133;
        AsyncRequestBody provider = CompressionAsyncRequestBody.builder()
                                                               .compressor(compressor)
                                                               .asyncRequestBody(customAsyncRequestBodyWithoutContentLength(testBytes))
                                                               .chunkSize(chunkSize)
                                                               .build();

        ByteBuffer byteBuffer = ByteBuffer.allocate(testString.length());
        CountDownLatch done = new CountDownLatch(1);
        AtomicInteger pos = new AtomicInteger();

        Subscriber<ByteBuffer> subscriber = new SimpleSubscriber(buffer -> {
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            byteBuffer.put(bytes);

            // verify each chunk
            byte[] chunkToVerify = new byte[chunkSize];
            System.arraycopy(testBytes, pos.get(), chunkToVerify, 0, chunkSize);
            chunkToVerify = compressor.compress(chunkToVerify);

            assertThat(bytes).isEqualTo(chunkToVerify);
            pos.addAndGet(chunkSize);
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

        provider.subscribe(subscriber);
        done.await(10, TimeUnit.SECONDS);

        byte[] retrieved = byteBuffer.array();
        byte[] uncompressed = decompress(retrieved);
        assertThat(new String(uncompressed)).isEqualTo(testString);
    }

    @Test
    public void emptyBytesConstructor_hasEmptyContent() throws Exception {
        AsyncRequestBody requestBody = CompressionAsyncRequestBody.builder()
                                                                  .compressor(compressor)
                                                                  .asyncRequestBody(AsyncRequestBody.empty())
                                                                  .build();

        ByteBuffer byteBuffer = ByteBuffer.allocate(0);
        CountDownLatch done = new CountDownLatch(1);

        Subscriber<ByteBuffer> subscriber = new SimpleSubscriber(buffer -> {
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            byteBuffer.put(bytes);
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

        requestBody.subscribe(subscriber);
        done.await(10, TimeUnit.SECONDS);
        assertThat(byteBuffer.array()).isEmpty();
        assertThat(byteBuffer.array()).isEqualTo(new byte[0]);
        assertThat(requestBody.contentType()).isEqualTo(Mimetype.MIMETYPE_OCTET_STREAM);
    }

    private static Stream<Arguments> bodyTypeCases() {
        return Stream.of(
            Arguments.of("file source -> File", AsyncRequestBody.fromFile(fileBody()), BodyType.FILE),
            Arguments.of("bytes source -> Bytes", AsyncRequestBody.fromString("Hello world"), BodyType.BYTES));
    }

    @ParameterizedTest(name = "{index} {0}")
    @MethodSource("bodyTypeCases")
    public void body_forwardsUnderlyingBodyType(String description, AsyncRequestBody wrapped, BodyType expected) {
        assertThat(compressionBody(wrapped).body()).isEqualTo(expected.getName());
    }

    private static CompressionAsyncRequestBody compressionBody(AsyncRequestBody wrapped) {
        return CompressionAsyncRequestBody.builder()
                                          .compressor(compressor)
                                          .asyncRequestBody(wrapped)
                                          .build();
    }

    private static Path fileBody() {
        try {
            FileSystem fs = Jimfs.newFileSystem(Configuration.unix());
            Path path = fs.getPath("./test");
            Files.write(path, "Hello world".getBytes());
            return path;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String createCompressibleStringOfGivenSize(int size) {
        ByteBuffer data = ByteBuffer.allocate(size);

        byte[] a = new byte[size / 4];
        byte[] b = new byte[size / 4];
        Arrays.fill(a, (byte) 'a');
        Arrays.fill(b, (byte) 'b');

        data.put(a);
        data.put(b);
        data.put(a);
        data.put(b);

        return new String(data.array());
    }

    private static byte[] decompress(byte[] compressedData) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(compressedData);
        GZIPInputStream gzipInputStream = new GZIPInputStream(bais);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = gzipInputStream.read(buffer)) != -1) {
            baos.write(buffer, 0, bytesRead);
        }
        gzipInputStream.close();
        byte[] decompressedData = baos.toByteArray();
        return decompressedData;
    }

    private static AsyncRequestBody customAsyncRequestBodyWithoutContentLength(byte[] content) {
        return new AsyncRequestBody() {
            @Override
            public Optional<Long> contentLength() {
                return Optional.empty();
            }

            @Override
            public void subscribe(Subscriber<? super ByteBuffer> s) {
                Flowable.fromPublisher(AsyncRequestBody.fromBytes(content))
                        .subscribe(s);
            }
        };
    }
}