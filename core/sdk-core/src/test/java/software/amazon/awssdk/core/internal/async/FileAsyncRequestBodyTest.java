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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertTrue;
import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.testutils.RandomTempFile;
import software.amazon.awssdk.utils.BinaryUtils;

public class FileAsyncRequestBodyTest {
    private static final long MiB = 1024 * 1024;
    private static final long TEST_FILE_SIZE = 10 * MiB;
    private static Path testFile;

    @Before
    public void setup() throws IOException {
        testFile = new RandomTempFile(TEST_FILE_SIZE).toPath();
    }

    @After
    public void teardown() throws IOException {
        try {
            Files.delete(testFile);
        } catch (NoSuchFileException e) {
            // ignore
        }
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

    @Test
    public void changingFile_fileGetsShorterThanAlreadyRead_failsBecauseTooShort() throws Exception {
        AsyncRequestBody asyncRequestBody = FileAsyncRequestBody.builder()
                                                                .path(testFile)
                                                                .build();

        ControllableSubscriber subscriber = new ControllableSubscriber();

        // Start reading file
        asyncRequestBody.subscribe(subscriber);
        subscriber.sub.request(1);
        assertTrue(subscriber.onNextSemaphore.tryAcquire(5, TimeUnit.SECONDS));

        // Change the file to be shorter than the amount read so far
        Files.write(testFile, "Hello".getBytes(StandardCharsets.UTF_8));

        // Finishing reading the file
        subscriber.sub.request(Long.MAX_VALUE);

        assertThatThrownBy(() -> subscriber.completed.get(5, TimeUnit.SECONDS))
            .hasCauseInstanceOf(IOException.class);
    }

    @Test
    public void changingFile_fileGetsShorterThanExistingLength_failsBecauseTooShort() throws Exception {
        AsyncRequestBody asyncRequestBody = FileAsyncRequestBody.builder()
                                                                .path(testFile)
                                                                .build();

        ControllableSubscriber subscriber = new ControllableSubscriber();

        // Start reading file
        asyncRequestBody.subscribe(subscriber);
        subscriber.sub.request(1);
        assertTrue(subscriber.onNextSemaphore.tryAcquire(5, TimeUnit.SECONDS));

        // Change the file to be 1 byte shorter than when we started
        int currentSize = Math.toIntExact(Files.size(testFile));
        byte[] slightlyShorterFileContent = new byte[currentSize - 1];
        ThreadLocalRandom.current().nextBytes(slightlyShorterFileContent);
        Files.write(testFile, slightlyShorterFileContent);

        // Finishing reading the file
        subscriber.sub.request(Long.MAX_VALUE);

        assertThatThrownBy(() -> subscriber.completed.get(5, TimeUnit.SECONDS))
            .hasCauseInstanceOf(IOException.class);
    }

    @Test
    public void changingFile_fileGetsLongerThanExistingLength_failsBecauseTooLong() throws Exception {
        AsyncRequestBody asyncRequestBody = FileAsyncRequestBody.builder()
                                                                .path(testFile)
                                                                .build();

        ControllableSubscriber subscriber = new ControllableSubscriber();

        // Start reading file
        asyncRequestBody.subscribe(subscriber);
        subscriber.sub.request(1);
        assertTrue(subscriber.onNextSemaphore.tryAcquire(5, TimeUnit.SECONDS));

        // Change the file to be 1 byte longer than when we started
        int currentSize = Math.toIntExact(Files.size(testFile));
        byte[] slightlyLongerFileContent = new byte[currentSize + 1];
        ThreadLocalRandom.current().nextBytes(slightlyLongerFileContent);
        Files.write(testFile, slightlyLongerFileContent);

        // Finishing reading the file
        subscriber.sub.request(Long.MAX_VALUE);

        assertThatThrownBy(() -> subscriber.completed.get(5, TimeUnit.SECONDS))
            .hasCauseInstanceOf(IOException.class);
    }

    @Test
    public void changingFile_fileGetsTouched_failsBecauseUpdatedModificationTime() throws Exception {
        AsyncRequestBody asyncRequestBody = FileAsyncRequestBody.builder()
                                                                .path(testFile)
                                                                .build();

        ControllableSubscriber subscriber = new ControllableSubscriber();

        // Start reading file
        asyncRequestBody.subscribe(subscriber);
        subscriber.sub.request(1);
        assertTrue(subscriber.onNextSemaphore.tryAcquire(5, TimeUnit.SECONDS));

        // Change the file to be updated
        Thread.sleep(1_000); // Wait for 1 second so that we are definitely in a different second than when the file was created
        Files.setLastModifiedTime(testFile, FileTime.from(Instant.now()));

        // Finishing reading the file
        subscriber.sub.request(Long.MAX_VALUE);

        assertThatThrownBy(() -> subscriber.completed.get(5, TimeUnit.SECONDS))
            .hasCauseInstanceOf(IOException.class);
    }

    @Test
    public void changingFile_fileGetsDeleted_failsBecauseDeleted() throws Exception {
        AsyncRequestBody asyncRequestBody = FileAsyncRequestBody.builder()
                                                                .path(testFile)
                                                                .build();

        ControllableSubscriber subscriber = new ControllableSubscriber();

        // Start reading file
        asyncRequestBody.subscribe(subscriber);
        subscriber.sub.request(1);
        assertTrue(subscriber.onNextSemaphore.tryAcquire(5, TimeUnit.SECONDS));

        // Delete the file
        Files.delete(testFile);

        // Finishing reading the file
        subscriber.sub.request(Long.MAX_VALUE);

        assertThatThrownBy(() -> subscriber.completed.get(5, TimeUnit.SECONDS))
            .hasCauseInstanceOf(IOException.class);
    }

    private static class ControllableSubscriber implements Subscriber<ByteBuffer> {
        private final ByteArrayOutputStream output = new ByteArrayOutputStream();
        private final CompletableFuture<Void> completed = new CompletableFuture<>();
        private final Semaphore onNextSemaphore = new Semaphore(0);
        private Subscription sub;

        @Override
        public void onSubscribe(Subscription subscription) {
            this.sub = subscription;
        }

        @Override
        public void onNext(ByteBuffer byteBuffer) {
            invokeSafely(() -> output.write(BinaryUtils.copyBytesFrom(byteBuffer)));
            onNextSemaphore.release();
        }

        @Override
        public void onError(Throwable throwable) {
            completed.completeExceptionally(throwable);
        }

        @Override
        public void onComplete() {
            completed.complete(null);
        }
    }
}
