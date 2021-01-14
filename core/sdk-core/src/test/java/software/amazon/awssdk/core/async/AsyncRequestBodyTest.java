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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import io.reactivex.Flowable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;
import org.assertj.core.util.Lists;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.core.internal.util.Mimetype;
import software.amazon.awssdk.http.async.SimpleSubscriber;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.StringInputStream;

@RunWith(Parameterized.class)
public class AsyncRequestBodyTest {
    private final static String testString = "Hello!";
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

    @Parameterized.Parameters
    public static AsyncRequestBody[] data() {
        return new AsyncRequestBody[]{
                AsyncRequestBody.fromString(testString),
                AsyncRequestBody.fromFile(path)
        };
    }

    private AsyncRequestBody provider;

    public AsyncRequestBodyTest(AsyncRequestBody provider) {
        this.provider = provider;
    }

    @Test
    public void hasCorrectLength() {
        assertThat(provider.contentLength().get()).isEqualTo(testString.length());
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
        done.await();
        assertThat(sb.toString()).isEqualTo(testString);
    }

    @Test
    public void stringConstructorHasCorrectContentType() {
        AsyncRequestBody requestBody = AsyncRequestBody.fromString("hello world");
        assertThat(requestBody.contentType()).isEqualTo(Mimetype.MIMETYPE_TEXT_PLAIN);
    }

    @Test
    public void fileConstructorHasCorrectContentType() {
        AsyncRequestBody requestBody = AsyncRequestBody.fromFile(path);
        assertThat(requestBody.contentType()).isEqualTo(Mimetype.MIMETYPE_OCTET_STREAM);
    }

    @Test
    public void bytesArrayConstructorHasCorrectContentType() {
        AsyncRequestBody requestBody = AsyncRequestBody.fromBytes("hello world".getBytes());
        assertThat(requestBody.contentType()).isEqualTo(Mimetype.MIMETYPE_OCTET_STREAM);
    }

    @Test
    public void bytesBufferConstructorHasCorrectContentType() {
        ByteBuffer byteBuffer = ByteBuffer.wrap("hello world".getBytes());
        AsyncRequestBody requestBody = AsyncRequestBody.fromByteBuffer(byteBuffer);
        assertThat(requestBody.contentType()).isEqualTo(Mimetype.MIMETYPE_OCTET_STREAM);
    }

    @Test
    public void emptyBytesConstructorHasCorrectContentType() {
        AsyncRequestBody requestBody = AsyncRequestBody.empty();
        assertThat(requestBody.contentType()).isEqualTo(Mimetype.MIMETYPE_OCTET_STREAM);
    }

    @Test
    public void publisherConstructorHasCorrectContentType() {
        List<String> requestBodyStrings = Lists.newArrayList("A", "B", "C");
        List<ByteBuffer> bodyBytes = requestBodyStrings.stream()
                                                .map(s -> ByteBuffer.wrap(s.getBytes(StandardCharsets.UTF_8)))
                                                .collect(Collectors.toList());
        Publisher<ByteBuffer> bodyPublisher = Flowable.fromIterable(bodyBytes);
        AsyncRequestBody requestBody = AsyncRequestBody.fromPublisher(bodyPublisher);
        assertThat(requestBody.contentType()).isEqualTo(Mimetype.MIMETYPE_OCTET_STREAM);
    }

    @Test
    public void fromBytes_byteArrayNotNull_createsCopy() {
        byte[] original = {0x1, 0x2, 0x3, 0x4};
        byte[] toModify = new byte[original.length];
        System.arraycopy(original, 0, toModify, 0, original.length);
        AsyncRequestBody body = AsyncRequestBody.fromBytes(toModify);
        for (int i = 0; i < toModify.length; ++i) {
            toModify[i]++;
        }
        ByteBuffer publishedBb = Flowable.fromPublisher(body).toList().blockingGet().get(0);
        assertThat(BinaryUtils.copyAllBytesFrom(publishedBb)).isEqualTo(original);
    }
}
