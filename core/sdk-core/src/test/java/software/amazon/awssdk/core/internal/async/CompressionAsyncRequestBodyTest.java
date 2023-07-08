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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import io.reactivex.Flowable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.assertj.core.util.Lists;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.compression.Compressor;
import software.amazon.awssdk.core.internal.compression.GzipCompressor;
import software.amazon.awssdk.core.internal.util.Mimetype;
import software.amazon.awssdk.http.async.SimpleSubscriber;

@RunWith(Parameterized.class)
public class CompressionAsyncRequestBodyTest {
    private static final Compressor compressor = new GzipCompressor();;
    private final static String testString =
        "RequestCompressionTest-RequestCompressionTest-RequestCompressionTest-RequestCompressionTest-RequestCompressionTest";
    private static String expectedTestString = new String(compressor.compress(testString.getBytes(StandardCharsets.UTF_8)));
    private final static Path path;

    static {
        FileSystem fs = Jimfs.newFileSystem(Configuration.unix());
        path = fs.getPath("./test");
        try {
            Files.write(path, testString.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private final AsyncRequestBody provider;

    public CompressionAsyncRequestBodyTest(AsyncRequestBody provider) {
        this.provider = provider;
    }

    @Parameterized.Parameters
    public static AsyncRequestBody[] data() {
        AsyncRequestBody[] asyncRequestBodies = {
            CompressionAsyncRequestBody.builder()
                                       .asyncRequestBody(AsyncRequestBody.fromString(testString))
                                       .compressor(compressor).build(),

            CompressionAsyncRequestBody.builder()
                                       .asyncRequestBody(AsyncRequestBody.fromFile(path))
                                       .compressor(compressor).build(),
            };
        return asyncRequestBodies;
    }

    @Test
    public void doesNotHaveContentLength() {
        assertThat(provider.contentLength()).isEmpty();
    }

    @Test
    public void hasCorrectContent() throws InterruptedException {
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

        provider.subscribe(subscriber);
        done.await(10, TimeUnit.SECONDS);
        assertThat(sb).hasToString(expectedTestString);
    }

    @Test
    public void stringConstructorHasCorrectContentType() {
        AsyncRequestBody requestBody = CompressionAsyncRequestBody.builder()
                                                                  .asyncRequestBody(AsyncRequestBody.fromString("Hello world"))
                                                                  .compressor(new GzipCompressor())
                                                                  .build();
        assertThat(requestBody.contentType()).startsWith(Mimetype.MIMETYPE_TEXT_PLAIN);
    }

    @Test
    public void fileConstructorHasCorrectContentType() {
        AsyncRequestBody requestBody = CompressionAsyncRequestBody.builder()
                                                                  .asyncRequestBody(AsyncRequestBody.fromFile(path))
                                                                  .compressor(new GzipCompressor())
                                                                  .build();
        assertThat(requestBody.contentType()).isEqualTo(Mimetype.MIMETYPE_OCTET_STREAM);
    }

    @Test
    public void bytesArrayConstructorHasCorrectContentType() {
        AsyncRequestBody requestBody = CompressionAsyncRequestBody.builder()
                                                                  .asyncRequestBody(AsyncRequestBody.fromBytes("hello world".getBytes()))
                                                                  .compressor(new GzipCompressor())
                                                                  .build();
        assertThat(requestBody.contentType()).isEqualTo(Mimetype.MIMETYPE_OCTET_STREAM);
    }

    @Test
    public void bytesBufferConstructorHasCorrectContentType() {
        ByteBuffer byteBuffer = ByteBuffer.wrap("hello world".getBytes());
        AsyncRequestBody requestBody = CompressionAsyncRequestBody.builder()
                                                                  .asyncRequestBody(AsyncRequestBody.fromByteBuffer(byteBuffer))
                                                                  .compressor(new GzipCompressor())
                                                                  .build();
        assertThat(requestBody.contentType()).isEqualTo(Mimetype.MIMETYPE_OCTET_STREAM);
    }

    @Test
    public void emptyBytesConstructorHasCorrectContentType() {
        AsyncRequestBody requestBody = CompressionAsyncRequestBody.builder()
                                                                  .asyncRequestBody(AsyncRequestBody.empty())
                                                                  .compressor(new GzipCompressor())
                                                                  .build();
        assertThat(requestBody.contentType()).isEqualTo(Mimetype.MIMETYPE_OCTET_STREAM);
    }

    @Test
    public void publisherConstructorThrowsExceptionIfNoContentLength() {
        List<String> requestBodyStrings = Lists.newArrayList("A", "B", "C");
        List<ByteBuffer> bodyBytes = requestBodyStrings.stream()
                                                       .map(s -> ByteBuffer.wrap(s.getBytes(StandardCharsets.UTF_8)))
                                                       .collect(Collectors.toList());
        Publisher<ByteBuffer> bodyPublisher = Flowable.fromIterable(bodyBytes);

        CompressionAsyncRequestBody.Builder builder = CompressionAsyncRequestBody.builder()
                                                                                 .asyncRequestBody(AsyncRequestBody.fromPublisher(bodyPublisher))
                                                                                 .compressor(new GzipCompressor());

        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->  builder.build());
    }
}
