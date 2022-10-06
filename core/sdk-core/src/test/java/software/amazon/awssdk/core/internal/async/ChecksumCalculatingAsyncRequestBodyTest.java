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
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import io.reactivex.Flowable;
import org.assertj.core.util.Lists;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.checksums.Algorithm;
import software.amazon.awssdk.core.internal.util.Mimetype;
import software.amazon.awssdk.http.async.SimpleSubscriber;
import software.amazon.awssdk.utils.BinaryUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@RunWith(Parameterized.class)
public class ChecksumCalculatingAsyncRequestBodyTest {
    private final static String testString = "Hello world";
    private final static String expectedTestString = "b\r\n" +
            testString + "\r\n" +
            "0\r\n" +
            "x-amz-checksum-crc32:i9aeUg==\r\n\r\n";
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

    public ChecksumCalculatingAsyncRequestBodyTest(AsyncRequestBody provider) {
        this.provider = provider;
    }

    @Parameterized.Parameters
    public static AsyncRequestBody[] data() {
        AsyncRequestBody[] asyncRequestBodies = {
                ChecksumCalculatingAsyncRequestBody.builder()
                        .asyncRequestBody(AsyncRequestBody.fromString(testString))
                        .algorithm(Algorithm.CRC32)
                        .trailerHeader("x-amz-checksum-crc32").build(),

                ChecksumCalculatingAsyncRequestBody.builder()
                        .asyncRequestBody(AsyncRequestBody.fromFile(path))
                        .algorithm(Algorithm.CRC32)
                        .trailerHeader("x-amz-checksum-crc32").build(),
        };
        return asyncRequestBodies;
    }

    @Test
    public void hasCorrectLength() {
        assertThat(provider.contentLength()).hasValue((long) expectedTestString.length());
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
        AsyncRequestBody requestBody = ChecksumCalculatingAsyncRequestBody.builder()
                .asyncRequestBody(AsyncRequestBody.fromString("Hello world"))
                .algorithm(Algorithm.CRC32)
                .trailerHeader("x-amz-checksum-crc32")
                .build();
        assertThat(requestBody.contentType()).startsWith(Mimetype.MIMETYPE_TEXT_PLAIN);
    }

    @Test
    public void fileConstructorHasCorrectContentType() {
        AsyncRequestBody requestBody = ChecksumCalculatingAsyncRequestBody.builder()
                .asyncRequestBody(AsyncRequestBody.fromFile(path))
                .algorithm(Algorithm.CRC32)
                .trailerHeader("x-amz-checksum-crc32")
                .build();
        assertThat(requestBody.contentType()).isEqualTo(Mimetype.MIMETYPE_OCTET_STREAM);
    }

    @Test
    public void bytesArrayConstructorHasCorrectContentType() {
        AsyncRequestBody requestBody = ChecksumCalculatingAsyncRequestBody.builder()
                .asyncRequestBody(AsyncRequestBody.fromBytes("hello world".getBytes()))
                .algorithm(Algorithm.CRC32)
                .trailerHeader("x-amz-checksum-crc32")
                .build();
        assertThat(requestBody.contentType()).isEqualTo(Mimetype.MIMETYPE_OCTET_STREAM);
    }

    @Test
    public void bytesBufferConstructorHasCorrectContentType() {
        ByteBuffer byteBuffer = ByteBuffer.wrap("hello world".getBytes());
        AsyncRequestBody requestBody = ChecksumCalculatingAsyncRequestBody.builder()
                .asyncRequestBody(AsyncRequestBody.fromByteBuffer(byteBuffer))
                .algorithm(Algorithm.CRC32)
                .trailerHeader("x-amz-checksum-crc32")
                .build();
        assertThat(requestBody.contentType()).isEqualTo(Mimetype.MIMETYPE_OCTET_STREAM);
    }

    @Test
    public void emptyBytesConstructorHasCorrectContentType() {
        AsyncRequestBody requestBody = ChecksumCalculatingAsyncRequestBody.builder()
                .asyncRequestBody(AsyncRequestBody.empty())
                .algorithm(Algorithm.CRC32)
                .trailerHeader("x-amz-checksum-crc32")
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

        ChecksumCalculatingAsyncRequestBody.Builder builder = ChecksumCalculatingAsyncRequestBody.builder()
                                                                                                 .asyncRequestBody(AsyncRequestBody.fromPublisher(bodyPublisher))
                                                                                                 .algorithm(Algorithm.SHA1)
                                                                                                 .trailerHeader("x-amz-checksum-sha1");

        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->  builder.build());
    }

    @Test
    public void fromBytes_NullChecks() {

        ChecksumCalculatingAsyncRequestBody.Builder noAlgorithmBuilder = ChecksumCalculatingAsyncRequestBody
            .builder()
            .asyncRequestBody(
                AsyncRequestBody.fromString("Hello world"));

        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> noAlgorithmBuilder.build());

        ChecksumCalculatingAsyncRequestBody.Builder noAsyncReqBodyBuilder = ChecksumCalculatingAsyncRequestBody
            .builder().algorithm(Algorithm.CRC32).trailerHeader("x-amzn-checksum-crc32");
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> noAsyncReqBodyBuilder.build());

        ChecksumCalculatingAsyncRequestBody.Builder noTrailerHeaderBuilder = ChecksumCalculatingAsyncRequestBody
            .builder().asyncRequestBody(AsyncRequestBody.fromString("Hello world")).algorithm(Algorithm.CRC32);
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> noTrailerHeaderBuilder.build());
    }

    @Test
    public void fromBytes_byteArrayNotNullChecksumSupplied() {
        byte[] original = {1, 2, 3, 4};
        // Checksum data in byte format.
        byte[] expected = {52, 13, 10,
                1, 2, 3, 4, 13, 10,
                48, 13, 10, 120, 45, 97, 109, 122, 110, 45, 99, 104, 101, 99, 107, 115,
                117, 109, 45, 99, 114, 99, 51, 50, 58, 116, 106, 122, 55, 122, 81, 61, 61, 13, 10, 13, 10};
        byte[] toModify = new byte[original.length];
        System.arraycopy(original, 0, toModify, 0, original.length);
        AsyncRequestBody body = ChecksumCalculatingAsyncRequestBody.builder()
                .asyncRequestBody(AsyncRequestBody.fromBytes(toModify))
                .algorithm(Algorithm.CRC32)
                .trailerHeader("x-amzn-checksum-crc32")
                .build();
        for (int i = 0; i < toModify.length; ++i) {
            toModify[i]++;
        }
        ByteBuffer publishedBb = Flowable.fromPublisher(body).toList().blockingGet().get(0);
        assertThat(BinaryUtils.copyAllBytesFrom(publishedBb)).isEqualTo(expected);
    }
}