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

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncRequestBodySplitConfiguration;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.async.SimplePublisher;

/**
 * A helper class to split a {@link FileAsyncRequestBody} to multiple smaller async request bodies. It ensures the buffer used to
 * be under the configured size via {@link AsyncRequestBodySplitConfiguration#bufferSizeInBytes()} by tracking the number of
 * concurrent ongoing {@link AsyncRequestBody}s.
 */
@SdkInternalApi
public final class FileAsyncRequestBodySplitHelper {
    private static final Logger log = Logger.loggerFor(FileAsyncRequestBodySplitHelper.class);

    private final AtomicBoolean isSendingRequestBody = new AtomicBoolean(false);
    private final AtomicLong remainingBytes;

    private final long totalContentLength;
    private final Path path;
    private final int bufferPerAsyncRequestBody;
    private final long totalBufferSize;
    private final long chunkSize;

    private volatile boolean isDone = false;

    private AtomicInteger numAsyncRequestBodiesInFlight = new AtomicInteger(0);
    private AtomicInteger chunkIndex = new AtomicInteger(0);

    public FileAsyncRequestBodySplitHelper(FileAsyncRequestBody asyncRequestBody,
                                           AsyncRequestBodySplitConfiguration splitConfiguration) {
        Validate.notNull(asyncRequestBody, "asyncRequestBody");
        Validate.notNull(splitConfiguration, "splitConfiguration");
        Validate.isTrue(asyncRequestBody.contentLength().isPresent(), "Content length must be present", asyncRequestBody);
        this.totalContentLength = asyncRequestBody.contentLength().get();
        this.remainingBytes = new AtomicLong(totalContentLength);
        this.path = asyncRequestBody.path();
        this.chunkSize = splitConfiguration.chunkSizeInBytes() == null ?
                         AsyncRequestBodySplitConfiguration.defaultConfiguration().chunkSizeInBytes() :
                         splitConfiguration.chunkSizeInBytes();
        this.totalBufferSize = splitConfiguration.bufferSizeInBytes() == null ?
                               AsyncRequestBodySplitConfiguration.defaultConfiguration().bufferSizeInBytes() :
                               splitConfiguration.bufferSizeInBytes();
        this.bufferPerAsyncRequestBody = asyncRequestBody.chunkSizeInBytes();
    }

    public SdkPublisher<AsyncRequestBody> split() {

        SimplePublisher<AsyncRequestBody> simplePublisher = new SimplePublisher<>();

        try {
            sendAsyncRequestBody(simplePublisher);
        } catch (Throwable throwable) {
            simplePublisher.error(throwable);
        }

        return SdkPublisher.adapt(simplePublisher);
    }

    private void sendAsyncRequestBody(SimplePublisher<AsyncRequestBody> simplePublisher) {
        do {
            if (!isSendingRequestBody.compareAndSet(false, true)) {
                return;
            }

            try {
                doSendAsyncRequestBody(simplePublisher);
            } finally {
                isSendingRequestBody.set(false);
            }
        } while (shouldSendMore());
    }

    private void doSendAsyncRequestBody(SimplePublisher<AsyncRequestBody> simplePublisher) {
        while (shouldSendMore()) {
            AsyncRequestBody currentAsyncRequestBody = newFileAsyncRequestBody(simplePublisher);
            simplePublisher.send(currentAsyncRequestBody);
            numAsyncRequestBodiesInFlight.incrementAndGet();
            checkCompletion(simplePublisher, currentAsyncRequestBody);
        }
    }

    private void checkCompletion(SimplePublisher<AsyncRequestBody> simplePublisher, AsyncRequestBody currentAsyncRequestBody) {
        long remaining = remainingBytes.addAndGet(-currentAsyncRequestBody.contentLength().get());

        if (remaining == 0) {
            isDone = true;
            simplePublisher.complete();
        } else if (remaining < 0) {
            isDone = true;
            simplePublisher.error(SdkClientException.create(
                "Unexpected error occurred. Remaining data is negative: " + remaining));
        }
    }

    private void startNextRequestBody(SimplePublisher<AsyncRequestBody> simplePublisher) {
        numAsyncRequestBodiesInFlight.decrementAndGet();
        sendAsyncRequestBody(simplePublisher);
    }

    private AsyncRequestBody newFileAsyncRequestBody(SimplePublisher<AsyncRequestBody> simplePublisher) {
        long position = chunkSize * chunkIndex.getAndIncrement();
        long numBytesToReadForThisChunk = Math.min(totalContentLength - position, chunkSize);
        FileAsyncRequestBody fileAsyncRequestBody = FileAsyncRequestBody.builder()
                                                                        .path(path)
                                                                        .position(position)
                                                                        .numBytesToRead(numBytesToReadForThisChunk)
                                                                        .build();
        return new FileAsyncRequestBodyWrapper(fileAsyncRequestBody, simplePublisher);
    }

    /**
     * Should not send more if it's done OR sending next request body would exceed the total buffer size
     */
    private boolean shouldSendMore() {
        if (isDone) {
            return false;
        }

        long currentUsedBuffer = (long) numAsyncRequestBodiesInFlight.get() * bufferPerAsyncRequestBody;
        return currentUsedBuffer + bufferPerAsyncRequestBody <= totalBufferSize;
    }

    @SdkTestInternalApi
    AtomicInteger numAsyncRequestBodiesInFlight() {
        return numAsyncRequestBodiesInFlight;
    }

    private final class FileAsyncRequestBodyWrapper implements AsyncRequestBody {

        private final FileAsyncRequestBody fileAsyncRequestBody;
        private final SimplePublisher<AsyncRequestBody> simplePublisher;

        FileAsyncRequestBodyWrapper(FileAsyncRequestBody fileAsyncRequestBody,
                                    SimplePublisher<AsyncRequestBody> simplePublisher) {
            this.fileAsyncRequestBody = fileAsyncRequestBody;
            this.simplePublisher = simplePublisher;
        }

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> s) {
            fileAsyncRequestBody.doAfterOnComplete(() -> startNextRequestBody(simplePublisher))
                                // The reason we still need to call startNextRequestBody when the subscription is
                                // cancelled is that upstream could cancel the subscription even though the stream has
                                // finished successfully before onComplete. If this happens, doAfterOnComplete callback
                                // will never be invoked, and if the current buffer is full, the publisher will stop
                                // sending new FileAsyncRequestBody, leading to uncompleted future.
                                .doAfterOnCancel(() -> startNextRequestBody(simplePublisher))
                                .subscribe(s);
        }

        @Override
        public Optional<Long> contentLength() {
            return fileAsyncRequestBody.contentLength();
        }
    }
}
