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
import java.util.stream.Stream;
import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.checksums.Algorithm;
import software.amazon.awssdk.core.internal.util.Mimetype;
import software.amazon.awssdk.http.async.SimpleSubscriber;
import software.amazon.awssdk.utils.BinaryUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ChecksumCalculatingAsyncRequestBodyTest {
    private static final String testString = "Hello world";
    private static final String expectedTestString = "b\r\n" +
            testString + "\r\n" +
            "0\r\n" +
            "x-amz-checksum-crc32:i9aeUg==\r\n\r\n";
    private static final String emptyString = "";
    private static final String expectedEmptyString = "0\r\n" +
                                                      "x-amz-checksum-crc32:AAAAAA==\r\n\r\n";
    private static final Path path;
    private static final Path pathToEmpty;

    static {
        FileSystem fs = Jimfs.newFileSystem(Configuration.unix());
        path = fs.getPath("./test");
        pathToEmpty = fs.getPath("./testEmpty");
        try {
            Files.write(path, testString.getBytes());
            Files.write(pathToEmpty, emptyString.getBytes());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Stream<Arguments> publishers() {
        return Stream.of(
            Arguments.of("RequestBody from string, test string",
                         checksumPublisher(AsyncRequestBody.fromString(testString)),
                         expectedTestString),
            Arguments.of("RequestBody from file, test string",
                         checksumPublisher(AsyncRequestBody.fromFile(path)),
                         expectedTestString),
            Arguments.of("RequestBody from buffer, 0 pos, test string",
                         checksumPublisher(AsyncRequestBody.fromRemainingByteBuffer(posZeroByteBuffer(testString))),
                         expectedTestString),
            Arguments.of("RequestBody from buffer, random pos, test string",
                         checksumPublisher(AsyncRequestBody.fromRemainingByteBufferUnsafe(nonPosZeroByteBuffer(testString))),
                         expectedTestString),
            Arguments.of("RequestBody from string, empty string",
                         checksumPublisher(AsyncRequestBody.fromString(emptyString)),
                         expectedEmptyString),
            //Note: FileAsyncRequestBody with empty file does not call onNext, only onComplete()
            Arguments.of("RequestBody from file, empty string",
                         checksumPublisher(AsyncRequestBody.fromFile(pathToEmpty)),
                         expectedEmptyString),
            Arguments.of("RequestBody from buffer, 0 pos, empty string",
                         checksumPublisher(AsyncRequestBody.fromRemainingByteBuffer(posZeroByteBuffer(emptyString))),
                         expectedEmptyString),
            Arguments.of("RequestBody from string, random pos, empty string",
                         checksumPublisher(AsyncRequestBody.fromRemainingByteBufferUnsafe(nonPosZeroByteBuffer(emptyString))),
                         expectedEmptyString));
    }

    private static ChecksumCalculatingAsyncRequestBody checksumPublisher(AsyncRequestBody sourcePublisher) {
        return ChecksumCalculatingAsyncRequestBody.builder()
                                                  .asyncRequestBody(sourcePublisher)
                                                  .algorithm(Algorithm.CRC32)
                                                  .trailerHeader("x-amz-checksum-crc32").build();
    }

    private static ByteBuffer posZeroByteBuffer(String content) {
        byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
        ByteBuffer bytes = ByteBuffer.allocate(contentBytes.length);
        bytes.put(contentBytes);
        bytes.flip();
        return bytes;
    }

    private static ByteBuffer nonPosZeroByteBuffer(String content) {
        byte[] randomContent = RandomStringUtils.randomAscii(1024).getBytes(StandardCharsets.UTF_8);
        byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);

        ByteBuffer bytes = ByteBuffer.allocate(contentBytes.length + randomContent.length);
        bytes.put(randomContent)
             .put(contentBytes);
        bytes.position(randomContent.length);
        return bytes;
    }

    @ParameterizedTest(name = "{index} {0}")
    @MethodSource("publishers")
    public void publish_differentAsyncRequestBodiesAndSources_produceCorrectData(String description,
                                                                                 AsyncRequestBody provider,
                                                                                 String expectedContent) throws InterruptedException {
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

        assertThat(provider.contentLength()).hasValue((long) expectedContent.length());
        assertThat(sb).hasToString(expectedContent);
    }

    @Test
    public void constructor_asyncRequestBodyFromString_hasCorrectContentType() {
        AsyncRequestBody requestBody = ChecksumCalculatingAsyncRequestBody.builder()
                                                                          .asyncRequestBody(AsyncRequestBody.fromString("Hello world"))
                                                                          .algorithm(Algorithm.CRC32)
                                                                          .trailerHeader("x-amz-checksum-crc32")
                                                                          .build();
        assertThat(requestBody.contentType()).startsWith(Mimetype.MIMETYPE_TEXT_PLAIN);
    }

    @Test
    public void constructor_asyncRequestBodyFromFile_hasCorrectContentType() {
        AsyncRequestBody requestBody = ChecksumCalculatingAsyncRequestBody.builder()
                                                                          .asyncRequestBody(AsyncRequestBody.fromFile(path))
                                                                          .algorithm(Algorithm.CRC32)
                                                                          .trailerHeader("x-amz-checksum-crc32")
                                                                          .build();
        assertThat(requestBody.contentType()).isEqualTo(Mimetype.MIMETYPE_OCTET_STREAM);
    }

    @Test
    public void constructor_asyncRequestBodyFromBytes_hasCorrectContentType() {
        AsyncRequestBody requestBody = ChecksumCalculatingAsyncRequestBody.builder()
                                                                          .asyncRequestBody(AsyncRequestBody.fromBytes("hello world".getBytes()))
                                                                          .algorithm(Algorithm.CRC32)
                                                                          .trailerHeader("x-amz-checksum-crc32")
                                                                          .build();
        assertThat(requestBody.contentType()).isEqualTo(Mimetype.MIMETYPE_OCTET_STREAM);
    }

    @Test
    public void constructor_asyncRequestBodyFromByteBuffer_hasCorrectContentType() {
        ByteBuffer byteBuffer = ByteBuffer.wrap("hello world".getBytes());
        AsyncRequestBody requestBody = ChecksumCalculatingAsyncRequestBody.builder()
                                                                          .asyncRequestBody(AsyncRequestBody.fromByteBuffer(byteBuffer))
                                                                          .algorithm(Algorithm.CRC32)
                                                                          .trailerHeader("x-amz-checksum-crc32")
                                                                          .build();
        assertThat(requestBody.contentType()).isEqualTo(Mimetype.MIMETYPE_OCTET_STREAM);
    }

    @Test
    public void constructor_asyncRequestBodyFromEmpty_hasCorrectContentType() {
        AsyncRequestBody requestBody = ChecksumCalculatingAsyncRequestBody.builder()
                                                                          .asyncRequestBody(AsyncRequestBody.empty())
                                                                          .algorithm(Algorithm.CRC32)
                                                                          .trailerHeader("x-amz-checksum-crc32")
                                                                          .build();
        assertThat(requestBody.contentType()).isEqualTo(Mimetype.MIMETYPE_OCTET_STREAM);
    }

    @Test
    public void constructor_asyncRequestBodyFromPublisher_NoContentLength_throwsException() {
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
    public void constructor_checksumIsNull_throwsException() {
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(
            () ->  ChecksumCalculatingAsyncRequestBody.builder()
                                                      .asyncRequestBody(AsyncRequestBody.fromString("Hello world"))
                                                      .trailerHeader("x-amzn-checksum-crc32")
                                                      .build()).withMessage("algorithm cannot be null");
    }

    @Test
    public void constructor_asyncRequestBodyIsNull_throwsException() {
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(
            () ->  ChecksumCalculatingAsyncRequestBody.builder()
                                                      .algorithm(Algorithm.CRC32)
                                                      .trailerHeader("x-amzn-checksum-crc32")
                                                      .build()).withMessage("wrapped AsyncRequestBody cannot be null");
    }

    @Test
    public void constructor_trailerHeaderIsNull_throwsException() {
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(
            () ->  ChecksumCalculatingAsyncRequestBody.builder()
                                                      .algorithm(Algorithm.CRC32)
                                                      .asyncRequestBody(AsyncRequestBody.fromString("Hello world"))
                                                      .build()).withMessage("trailerHeader cannot be null");

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