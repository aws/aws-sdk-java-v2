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
import static software.amazon.awssdk.core.internal.async.SplittingPublisherTestUtils.verifyIndividualAsyncRequestBody;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.async.AsyncRequestBody.BodyType;
import software.amazon.awssdk.core.async.AsyncRequestBodySplitConfiguration;
import software.amazon.awssdk.testutils.RandomTempFile;

public class FileAsyncRequestBodySplitHelperTest {

    private static final int CHUNK_SIZE = 5;
    private static Path testFile;
    private static ScheduledExecutorService executor;


    @BeforeAll
    public static void setup() throws IOException {
        testFile = new RandomTempFile(2000).toPath();
        executor = Executors.newScheduledThreadPool(1);
    }

    @AfterAll
    public static void teardown() throws IOException {
        try {
            Files.delete(testFile);
        } catch (NoSuchFileException e) {
            // ignore
        }
        executor.shutdown();
    }

    @ParameterizedTest
    @ValueSource(ints = {CHUNK_SIZE, CHUNK_SIZE * 2 - 1, CHUNK_SIZE * 2})
    public void split_differentChunkSize_shouldSplitCorrectly(int chunkSize) throws Exception {
        long bufferSize = 55l;
        int chunkSizeInBytes = 10;
        FileAsyncRequestBody fileAsyncRequestBody = FileAsyncRequestBody.builder()
                                                                        .path(testFile)
                                                                        .chunkSizeInBytes(10)
                                                                        .build();
        AsyncRequestBodySplitConfiguration config =
            AsyncRequestBodySplitConfiguration.builder()
                                              .chunkSizeInBytes((long) chunkSize)
                                              .bufferSizeInBytes(55L)
                                              .build();
        FileAsyncRequestBodySplitHelper helper = new FileAsyncRequestBodySplitHelper(fileAsyncRequestBody, config);

        AtomicInteger maxConcurrency = new AtomicInteger(0);
        ScheduledFuture<?> scheduledFuture = executor.scheduleWithFixedDelay(verifyConcurrentRequests(helper, maxConcurrency),
                                                                             1, 50, TimeUnit.MICROSECONDS);

        verifyIndividualAsyncRequestBody(helper.split(),
                                         testFile,
                                         chunkSize);
        scheduledFuture.cancel(true);
        int expectedMaxConcurrency = (int) (bufferSize / chunkSizeInBytes);
        assertThat(maxConcurrency.get()).isLessThanOrEqualTo(expectedMaxConcurrency);
    }

    @Test
    public void split_fileRequestBody_partsReportFileBodyType() throws Exception {
        FileAsyncRequestBody fileAsyncRequestBody = FileAsyncRequestBody.builder()
                                                                        .path(testFile)
                                                                        .chunkSizeInBytes(10)
                                                                        .build();
        AsyncRequestBodySplitConfiguration config =
            AsyncRequestBodySplitConfiguration.builder()
                                              .chunkSizeInBytes((long) CHUNK_SIZE)
                                              .bufferSizeInBytes(55L)
                                              .build();
        FileAsyncRequestBodySplitHelper helper = new FileAsyncRequestBodySplitHelper(fileAsyncRequestBody, config);

        List<String> bodyTypes = new CopyOnWriteArrayList<>();
        helper.split()
              .subscribe(requestBody -> {
                  bodyTypes.add(requestBody.body());
                  // Draining each part lets the buffer-limited publisher emit the next one.
                  requestBody.subscribe(new DrainingSubscriber());
              })
              .get(5, TimeUnit.SECONDS);

        assertThat(bodyTypes).isNotEmpty();
        assertThat(bodyTypes).containsOnly(BodyType.FILE.getName());
    }

    private static final class DrainingSubscriber implements Subscriber<ByteBuffer> {
        @Override
        public void onSubscribe(Subscription subscription) {
            subscription.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(ByteBuffer byteBuffer) {
        }

        @Override
        public void onError(Throwable throwable) {
        }

        @Override
        public void onComplete() {
        }
    }

    private static Runnable verifyConcurrentRequests(FileAsyncRequestBodySplitHelper helper, AtomicInteger maxConcurrency) {
        return () -> {
            int concurrency = helper.numAsyncRequestBodiesInFlight().get();

            if (concurrency > maxConcurrency.get()) {
                maxConcurrency.set(concurrency);
            }
            assertThat(helper.numAsyncRequestBodiesInFlight()).hasValueBetween(0,10);
        };
    }
}
