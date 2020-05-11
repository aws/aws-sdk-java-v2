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

import com.google.common.jimfs.Jimfs;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.async.SdkPublisher;

/**
 * Tests for {@link FileAsyncResponseTransformer}.
 */
public class FileAsyncResponseTransformerTest {
    private static FileSystem testFs;

    @BeforeClass
    public static void setup() {
        testFs = Jimfs.newFileSystem();
    }

    @AfterClass
    public static void teardown() throws IOException {
        testFs.close();
    }

    @Test
    public void errorInStream_completesFuture() {
        Path testPath = testFs.getPath("test_file.txt");
        FileAsyncResponseTransformer xformer = new FileAsyncResponseTransformer(testPath);

        CompletableFuture prepareFuture = xformer.prepare();

        xformer.onResponse(new Object());
        xformer.onStream(subscriber -> {
            subscriber.onSubscribe(new Subscription() {
                @Override
                public void request(long l) {
                }

                @Override
                public void cancel() {
                }
            });

            subscriber.onError(new RuntimeException("Something went wrong"));
        });

        assertThat(prepareFuture.isCompletedExceptionally()).isTrue();
    }

    @Test
    public void synchronousPublisher_shouldNotHang() throws Exception {
        List<CompletableFuture> futures = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            Path testPath = testFs.getPath(i + "test_file.txt");
            FileAsyncResponseTransformer transformer = new FileAsyncResponseTransformer(testPath);

            CompletableFuture prepareFuture = transformer.prepare();

            transformer.onResponse(new Object());

            transformer.onStream(new TestPublisher());
            futures.add(prepareFuture);
        }

        CompletableFuture<Void> future = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        future.get(10, TimeUnit.SECONDS);
        assertThat(future.isCompletedExceptionally()).isFalse();
    }

    static class TestPublisher implements SdkPublisher<ByteBuffer> {
        private AtomicInteger requestNumber = new AtomicInteger(0);
        private volatile boolean isDone = false;

        @Override
        public void subscribe(Subscriber s) {

            s.onSubscribe(new Subscription() {
                @Override
                public void request(long l) {

                    if (isDone) {
                        return;
                    }
                    requestNumber.incrementAndGet();

                    if (requestNumber.get() == 2) {
                        isDone = true;
                        s.onComplete();
                        return;
                    }

                    s.onNext(ByteBuffer.wrap(RandomStringUtils.randomAlphanumeric(30000).getBytes()));
                }

                @Override
                public void cancel() {
                }
            });
        }
    }
}
