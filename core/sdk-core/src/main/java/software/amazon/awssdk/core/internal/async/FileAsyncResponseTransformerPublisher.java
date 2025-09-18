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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.FileTransformerConfiguration;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.ContentRangeParser;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Pair;
import software.amazon.awssdk.utils.Validate;

/**
 * A publisher of {@link FileAsyncResponseTransformer} that uses the Content-Range header of a {@link SdkResponse} to write to the
 * offset defined in the range of the Content-Range. Correspond to the {@link SplittingTransformer} for non-linear write cases.
 */
@SdkInternalApi
public class FileAsyncResponseTransformerPublisher<T extends SdkResponse>
    implements SdkPublisher<AsyncResponseTransformer<T, T>> {
    private static final Logger log = Logger.loggerFor(FileAsyncResponseTransformerPublisher.class);

    private final Path path;
    private final FileTransformerConfiguration initialConfig;
    private final long initialPosition;
    private Subscriber<?> subscriber;
    private final AtomicLong transformerCount;


    public FileAsyncResponseTransformerPublisher(FileAsyncResponseTransformer<?> responseTransformer) {
        this.path = Validate.paramNotNull(responseTransformer.path(), "path");
        Validate.isTrue(responseTransformer.config().fileWriteOption()
                        != FileTransformerConfiguration.FileWriteOption.CREATE_OR_APPEND_TO_EXISTING,
                        "CREATE_OR_APPEND_TO_EXISTING is not supported for non-serial operations");
        this.initialConfig = Validate.paramNotNull(responseTransformer.config(), "fileTransformerConfiguration");
        this.initialPosition = responseTransformer.position();
        this.transformerCount = new AtomicLong(0);
    }

    @Override
    public void subscribe(Subscriber<? super AsyncResponseTransformer<T, T>> s) {
        if (s == null) {
            throw new NullPointerException("Subscription must not be null");
        }
        this.subscriber = s;
        s.onSubscribe(ThreadSafeEmittingSubscription.<AsyncResponseTransformer<T, T>>builder()
                                                    .downstreamSubscriber(s)
                                                    .onCancel(this::onCancel)
                                                    .log(log)
                                                    .supplier(this::createTransformer)
                                                    .build());
    }

    private AsyncResponseTransformer<T, T> createTransformer() {
        return new IndividualFileTransformer();
    }

    private void onCancel() {
        subscriber = null;
    }

    /**
     * This is the AsyncResponseTransformer that will be used for each individual requests.
     * <p>
     * We delegate to new instances of the already existing class {@link FileAsyncResponseTransformer} to perform the individual
     * requests. This FileAsyncResponseTransformer will write the content of the request to the file at the offset taken from the
     * Content-Range header ('x-amz-content-range'). As such, we don't need to manually manage the state of the
     * AsyncResponseTransformer passed by the user, like we do for {@link SplittingTransformer}. Here, we know it is a
     * FileAsyncResponseTransformer, so we can just ignore it, and instead rely on the individual FileAsyncResponseTransformer of
     * every part.
     * <p>
     * Note on retries: since we are delegating requests to {@link FileAsyncResponseTransformer}, each request made with this
     * transformer will retry independently based on the retry configuration of the client it is used with. We only need to verify
     * the completion state of the future of each individually
     */
    private class IndividualFileTransformer implements AsyncResponseTransformer<T, T> {
        private AsyncResponseTransformer<T, T> delegate;
        private CompletableFuture<T> future;

        @Override
        public CompletableFuture<T> prepare() {
            this.future = new CompletableFuture<>();
            return this.future;
        }

        @Override
        public void onResponse(T response) {
            Optional<String> contentRangeList = response.sdkHttpResponse().firstMatchingHeader("x-amz-content-range");
            if (!contentRangeList.isPresent()) {
                if (subscriber != null) {
                    IllegalStateException e = new IllegalStateException("Content range header is missing");
                    subscriber.onError(e);
                    future.completeExceptionally(e);
                }
                return;
            }

            String contentRange = contentRangeList.get();
            Optional<Pair<Long, Long>> contentRangePair = ContentRangeParser.range(contentRange);
            if (!contentRangePair.isPresent()) {
                if (subscriber != null) {
                    IllegalStateException e = new IllegalStateException("Could not parse content range header " + contentRange);
                    subscriber.onError(e);
                    future.completeExceptionally(e);
                }
                return;
            }

            this.delegate = getDelegateTransformer(contentRangePair.get().left());
            CompletableFuture<T> delegateFuture = delegate.prepare();
            CompletableFutureUtils.forwardResultTo(delegateFuture, future);
            CompletableFutureUtils.forwardExceptionTo(future, delegateFuture);
            transformerCount.incrementAndGet();
            delegate.onResponse(response);
        }

        private AsyncResponseTransformer<T, T> getDelegateTransformer(Long startAt) {
            if (transformerCount.get() == 0) {
                return AsyncResponseTransformer.toFile(path, initialConfig);
            }
            switch (initialConfig.fileWriteOption()) {
                case CREATE_NEW:
                case CREATE_OR_REPLACE_EXISTING: {
                    FileTransformerConfiguration newConfig = initialConfig.copy(c -> c
                        .fileWriteOption(FileTransformerConfiguration.FileWriteOption.WRITE_TO_POSITION)
                        .position(startAt));
                    return AsyncResponseTransformer.toFile(path, newConfig);
                }
                case WRITE_TO_POSITION: {
                    long initialOffset = initialConfig.position();
                    FileTransformerConfiguration newConfig = initialConfig.copy(c -> c
                        .fileWriteOption(FileTransformerConfiguration.FileWriteOption.WRITE_TO_POSITION)
                        .position(initialOffset + startAt));
                    return AsyncResponseTransformer.toFile(path, newConfig);
                }
                // APPEND mode is not supported for non-serial operations,
                case CREATE_OR_APPEND_TO_EXISTING:
                default:
                    throw new UnsupportedOperationException("Unsupported fileWriteOption: " + initialConfig.fileWriteOption());
            }
        }

        @Override
        public void onStream(SdkPublisher<ByteBuffer> publisher) {
            // should never be null as per AsyncResponseTransformer runtime contract, but we never know
            if (delegate == null) {
               if (future != null) {
                   future.completeExceptionally(new IllegalStateException("onStream called before onResponse"));
               }
               return;
            }
            delegate.onStream(publisher);
        }

        @Override
        public void exceptionOccurred(Throwable error) {
            if (delegate != null) {
                // do not call onError, because exceptionOccurred may be called multiple times due to retries, simply forward the
                // error to the delegate async response transformer which will let the service call pipeline handle the error.
                delegate.exceptionOccurred(error);
            } else {
                // If we received an error without even having a delegate, this means we have thrown an error before even
                // getting a onResponse signal. We complete the prepared future, to let the
                // service call pipeline handle the error
                if (future != null) {
                    future.completeExceptionally(error);
                }
            }
        }
    }

}
