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
import java.nio.file.attribute.FileTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
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
import software.amazon.awssdk.utils.NumericUtils;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.async.SimplePublisher;

/**
 * A helper class to split a {@link FileAsyncRequestBody} to multiple smaller async request bodies. It ensures the buffer used to
 * be under the configured size via {@link AsyncRequestBodySplitConfiguration#bufferSizeInBytes()} by tracking the number of
 * concurrent ongoing {@link AsyncRequestBody}s.
 */
@SdkInternalApi
public final class FileAsyncRequestBodySplitHelper {

    private final AtomicBoolean isSendingRequestBody = new AtomicBoolean(false);
    private final AtomicLong remainingBytes;

    private final long totalContentLength;
    private final Path path;
    private final int bufferPerAsyncRequestBody;
    private final long totalBufferSize;
    private final long chunkSize;

    private volatile boolean isDone = false;

    private Set<Long> requestBodyStartPositionsInFlight = Collections.synchronizedSet(new HashSet<>());
    private AtomicInteger chunkIndex = new AtomicInteger(0);
    private final FileTime modifiedTimeAtStart;
    private final long sizeAtStart;

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
        this.bufferPerAsyncRequestBody = Math.min(asyncRequestBody.chunkSizeInBytes(),
                                                  NumericUtils.saturatedCast(totalBufferSize));
        this.modifiedTimeAtStart = asyncRequestBody.modifiedTimeAtStart();
        this.sizeAtStart = asyncRequestBody.sizeAtStart();
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
            long position = chunkSize * chunkIndex.getAndIncrement();
            AsyncRequestBody currentAsyncRequestBody = newFileAsyncRequestBody(position, simplePublisher);
            simplePublisher.send(currentAsyncRequestBody);
            requestBodyStartPositionsInFlight.add(position);
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

    private void startNextRequestBody(SimplePublisher<AsyncRequestBody> simplePublisher, long completedPosition) {
        requestBodyStartPositionsInFlight.remove(completedPosition);
        sendAsyncRequestBody(simplePublisher);
    }

    private AsyncRequestBody newFileAsyncRequestBody(long position, SimplePublisher<AsyncRequestBody> simplePublisher) {
        long numBytesToReadForThisChunk = Math.min(totalContentLength - position, chunkSize);
        FileAsyncRequestBody fileAsyncRequestBody = FileAsyncRequestBody.builder()
                                                                        .path(path)
                                                                        .position(position)
                                                                        .numBytesToRead(numBytesToReadForThisChunk)
                                                                        .chunkSizeInBytes(bufferPerAsyncRequestBody)
                                                                        .modifiedTimeAtStart(modifiedTimeAtStart)
                                                                        .sizeAtStart(sizeAtStart)
                                                                        .build();
        return new FileAsyncRequestBodyWrapper(fileAsyncRequestBody, simplePublisher, position);
    }

    /**
     * Should not send more if it's done OR sending next request body would exceed the total buffer size
     */
    private boolean shouldSendMore() {
        if (isDone) {
            return false;
        }

        long currentUsedBuffer = (long) requestBodyStartPositionsInFlight.size() * bufferPerAsyncRequestBody;
        return currentUsedBuffer + bufferPerAsyncRequestBody <= totalBufferSize;
    }

    @SdkTestInternalApi
    int numAsyncRequestBodiesInFlight() {
        return requestBodyStartPositionsInFlight.size();
    }

    private final class FileAsyncRequestBodyWrapper implements AsyncRequestBody {

        private final FileAsyncRequestBody fileAsyncRequestBody;
        private final SimplePublisher<AsyncRequestBody> simplePublisher;
        private final long position;

        FileAsyncRequestBodyWrapper(FileAsyncRequestBody fileAsyncRequestBody,
                                    SimplePublisher<AsyncRequestBody> simplePublisher, long position) {
            this.fileAsyncRequestBody = fileAsyncRequestBody;
            this.simplePublisher = simplePublisher;
            this.position = position;
        }

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> s) {
            fileAsyncRequestBody.doAfterOnComplete(() -> startNextRequestBody(simplePublisher, position))
                                // The reason we still need to call startNextRequestBody when the subscription is
                                // cancelled is that upstream could cancel the subscription even though the stream has
                                // finished successfully before onComplete. If this happens, doAfterOnComplete callback
                                // will never be invoked, and if the current buffer is full, the publisher will stop
                                // sending new FileAsyncRequestBody, leading to uncompleted future.
                                .doAfterOnCancel(() -> startNextRequestBody(simplePublisher, position))
                                .subscribe(s);
        }

        @Override
        public Optional<Long> contentLength() {
            return fileAsyncRequestBody.contentLength();
        }
    }
}
