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
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.testutils.RandomTempFile;

public class FileAsyncRequestBodyTest {
    private static final long MiB = 1024 * 1024;
    private static final long TEST_FILE_SIZE = 10 * MiB;
    private static Path testFile;

    @BeforeClass
    public static void setup() throws IOException {
        testFile = new RandomTempFile(TEST_FILE_SIZE).toPath();
    }

    @AfterClass
    public static void teardown() throws IOException {
        Files.delete(testFile);
    }

    // If we issue just enough requests to read the file entirely but not more (to go past EOF), we should still receive
    // an onComplete
    @Test
    public void readFully_doesNotRequestPastEndOfFile_receivesComplete() throws InterruptedException, ExecutionException, TimeoutException {
        int chunkSize = 16384;
        AsyncRequestBody asyncRequestBody = FileAsyncRequestBody.builder()
                .path(testFile)
                .chunkSizeInBytes(chunkSize)
                .build();

        long totalRequests = TEST_FILE_SIZE / chunkSize;

        CompletableFuture<Void> completed = new CompletableFuture<>();
        asyncRequestBody.subscribe(new Subscriber<ByteBuffer>() {
            private Subscription sub;
            private long requests = 0;
            @Override
            public void onSubscribe(Subscription subscription) {
                this.sub = subscription;
                if (requests++ < totalRequests) {
                    this.sub.request(1);
                }
            }

            @Override
            public void onNext(ByteBuffer byteBuffer) {
                if (requests++ < totalRequests) {
                    this.sub.request(1);
                }
            }

            @Override
            public void onError(Throwable throwable) {
            }

            @Override
            public void onComplete() {
                completed.complete(null);
            }
        });

        completed.get(5, TimeUnit.SECONDS);
    }
}
