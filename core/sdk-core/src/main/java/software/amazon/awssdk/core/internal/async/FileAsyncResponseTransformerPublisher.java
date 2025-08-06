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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.FileTransformerConfiguration;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.utils.ContentRangeParser;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

/**
 * A publisher of {@link FileAsyncResponseTransformer} that uses the Content-Range header of a {@link SdkResponse} to write to
 * the offset defined in the range of the Content-Range. Correspond to the {@link SplittingTransformer} for non-linear write
 * cases. Will only work for {@link FileAsyncResponseTransformer} and will throw {@link IllegalArgumentException} if other
 * subclasses of AsyncResponseTransformer is used with it.
 * <p>
 * Note: Marked as protected API because the s3 multipart logic in the s3 module needs to know about this type.
 * @param <T>
 */
@SdkProtectedApi
public class FileAsyncResponseTransformerPublisher<T extends SdkResponse> implements SdkPublisher<AsyncResponseTransformer<T, T>> {
    private static final Logger log = Logger.loggerFor(FileAsyncResponseTransformerPublisher.class);

    private final Path path;
    private final FileTransformerConfiguration initialConfig;
    private final long initialPosition;
    private Subscriber subscriber;

    public FileAsyncResponseTransformerPublisher(AsyncResponseTransformer<?, ?> responseTransformer) {
        Validate.isTrue(responseTransformer instanceof FileAsyncResponseTransformer,
                        "Only FileAsyncResponseTransformer is supported for FileAsyncResponseTransformerPublisher");
        FileAsyncResponseTransformer<SdkResponse> transformer = (FileAsyncResponseTransformer<SdkResponse>) responseTransformer;
        this.path = Validate.paramNotNull(transformer.path(), "path");
        this.initialConfig = Validate.paramNotNull(transformer.config(), "fileTransformerConfiguration");
        this.initialPosition = transformer.initialPosition();
    }

    @Override
    public void subscribe(Subscriber<? super AsyncResponseTransformer<T, T>> s) {
        this.subscriber = s;
        s.onSubscribe(new ThreadSafeEmittingSubscription<>(
            s, new AtomicLong(0), this::onCancel, new AtomicBoolean(), log, this::createTransformer));
    }

    /**
     * Signal received from {@link Subscriber} (the one receive from the subscribe method) to stop sending
     * IndividualFileTransformer and clean-up resources. Not necessarily an error state.
     */
    private void onCancel() {
        // do nothing. We have no resources to clean and the demand is managed by ThreadSafeEmittingSubscription which will
        // already fulfill previously signaled demand
    }

    private AsyncResponseTransformer<T, T> createTransformer() {
        AsyncResponseTransformer<T, T> delegate = AsyncResponseTransformer.toFile(path, initialConfig);
        return new IndividualFileTransformer((FileAsyncResponseTransformer<T>) delegate);
    }

    /**
     * This is the AsyncResponseTransformer that will be used for each individual requests.
     * <p>
     * We delegate to new instances of the already existing class {@link FileAsyncResponseTransformer} to perform the
     * individual requests. This FileAsyncResponseTransformer will write the content of the request to the file at the
     * offset taken from the Content-Range header. As such, we don't need to manually manage the state of the
     * AsyncResponseTransformer passed by the user, like we do for {@link SplittingTransformer}. Here, we know it is a
     * FileAsyncResponseTransformer, so we can just ignore it, and instead rely on the individual FileAsyncResponseTransformer
     * of every part.
     * <p>
     * Note on retries: since we are delegating requests to {@link FileAsyncResponseTransformer}, each request made with this
     * transformer will retry independently based on the retry configuration of the client it is used with. We only need to
     * verify the completion state of the future of each individually
     */
    class IndividualFileTransformer implements AsyncResponseTransformer<T, T> {
        private final FileAsyncResponseTransformer<T> delegate;

        IndividualFileTransformer(FileAsyncResponseTransformer<T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public CompletableFuture<T> prepare() {
            return delegate.prepare(); // this future will be completed once the body has been written to file
        }

        @Override
        public void onResponse(T response) {
            log.info(() -> "Received response: " + response.toString());
            ContentRangeParser.range(response.sdkHttpResponse().headers().get("Content-Range").get(0))
                              .ifPresent(pair -> delegate.offsetPosition(initialPosition + pair.left()));
            delegate.onResponse(response);
        }

        @Override
        public void onStream(SdkPublisher<ByteBuffer> publisher) {
            delegate.onStream(publisher);
        }

        @Override
        public void exceptionOccurred(Throwable error) {
            delegate.exceptionOccurred(error);
        }
    }

}
