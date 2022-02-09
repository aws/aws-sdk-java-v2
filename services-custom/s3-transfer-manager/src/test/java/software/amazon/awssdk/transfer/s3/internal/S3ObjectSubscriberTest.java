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

package software.amazon.awssdk.transfer.s3.internal;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Matchers.any;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.junit.AfterClass;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.transfer.s3.CompletedDirectoryDownload;
import software.amazon.awssdk.transfer.s3.DownloadDirectoryRequest;
import software.amazon.awssdk.transfer.s3.DownloadFileRequest;
import software.amazon.awssdk.transfer.s3.FileDownload;


class S3ObjectSubscriberTest {
    private static FileSystem fs = Jimfs.newFileSystem(Configuration.unix());
    private DownloadDirectoryHelper.S3ObjectSubscriber s3ObjectSubscriber;
    private Function<DownloadFileRequest, FileDownload> downloadFileFunction;
    private CompletableFuture<CompletedDirectoryDownload> returnFuture;

    @BeforeEach
    public void setUp() {
        Path temp = fs.getPath("/", UUID.randomUUID().toString());
        DownloadDirectoryRequest request = DownloadDirectoryRequest.builder().bucket("bucket").destinationDirectory(temp).build();
        downloadFileFunction = Mockito.mock(Function.class);
        returnFuture = new CompletableFuture<>();
        s3ObjectSubscriber = new DownloadDirectoryHelper.S3ObjectSubscriber(request, returnFuture, downloadFileFunction, 5);
    }

    @AfterClass
    public void cleanUp() throws IOException {
        fs.close();
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 4, 11})
    void differentNumberOfS3Objects_shouldCompleteSuccessfully(int numberOfS3Objects) throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        new TestPublisher(numberOfS3Objects, countDownLatch).subscribe(s3ObjectSubscriber);
        countDownLatch.await(10, TimeUnit.SECONDS);
        assertThat(returnFuture).isNotCompletedExceptionally();
        Mockito.verify(downloadFileFunction, Mockito.times(numberOfS3Objects)).apply(any(DownloadFileRequest.class));
    }

    @Test
    void onErrorInvoked_shouldCompleteFutureExceptionally() throws InterruptedException {
        s3ObjectSubscriber.onSubscribe(new Subscription() {
            @Override
            public void request(long n) {

            }

            @Override
            public void cancel() {

            }
        });
        RuntimeException exception = new RuntimeException("test");
        s3ObjectSubscriber.onError(exception);
        assertThat(returnFuture).isCompletedExceptionally();
    }


    private static final class TestPublisher implements SdkPublisher<S3Object> {
        private final int numberOfS3Objects;
        private final CountDownLatch countDownLatch;
        private volatile boolean isDone = false;
        private final AtomicInteger requestNumber = new AtomicInteger(0);

        private TestPublisher(int numberOfS3Objects, CountDownLatch countDownLatch) {
            this.numberOfS3Objects = numberOfS3Objects;
            this.countDownLatch = countDownLatch;
        }

        @Override
        public void subscribe(Subscriber<? super S3Object> subscriber) {
            subscriber.onSubscribe(new Subscription() {
                @Override
                public void request(long n) {
                    if (isDone) {
                        return;
                    }

                    if (requestNumber.incrementAndGet() > numberOfS3Objects) {
                        isDone = true;
                        subscriber.onComplete();
                        countDownLatch.countDown();
                        return;
                    }

                    subscriber.onNext(S3Object.builder().key("key" + requestNumber.get()).build());
                }

                @Override
                public void cancel() {
                    countDownLatch.countDown();
                }
            });
        }
    }
}
