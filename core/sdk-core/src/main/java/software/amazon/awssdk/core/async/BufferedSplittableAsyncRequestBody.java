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

import java.nio.ByteBuffer;
import java.util.Optional;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.internal.async.SplittingPublisher;
import software.amazon.awssdk.utils.Validate;

/**
 * An {@link AsyncRequestBody} decorator that enables splitting into retryable sub-request bodies.
 *
 * <p>This wrapper allows any {@link AsyncRequestBody} to be split into multiple parts where each part
 * can be retried independently. When split, each sub-body buffers its portion of data, enabling
 * resubscription if a retry is needed (e.g., due to network failures or service errors).</p>
 *
 * <p><b>Retry Requirements:</b></p>
 * <p>Retry is only possible if all the data has been successfully buffered during the first subscription.
 * If the first subscriber fails to consume all the data (e.g., due to early cancellation or errors),
 * subsequent retry attempts will fail since the complete data set is not available for resubscription.</p>
 *
 * <p><b>Usage Example:</b></p>
 * {@snippet :
 * AsyncRequestBody originalBody = AsyncRequestBody.fromString("Hello World");
 * BufferedSplittableAsyncRequestBody retryableBody =
 *     BufferedSplittableAsyncRequestBody.create(originalBody);
 * }
 *
 * <p><b>Performance Considerations:</b></p>
 * <p>This implementation buffers data in memory to enable retries, but memory usage is controlled by
 * the {@code bufferSizeInBytes} configuration. However, this buffering limits the ability to request
 * more data from the original AsyncRequestBody until buffered data is consumed (i.e., when subscribers
 * closes sub-body), which may increase latency compared to non-buffered implementations.
 *
 * @see AsyncRequestBody
 * @see AsyncRequestBodySplitConfiguration
 * @see CloseableAsyncRequestBody
 */
@SdkPublicApi
public final class BufferedSplittableAsyncRequestBody implements AsyncRequestBody {
    private final AsyncRequestBody delegate;

    private BufferedSplittableAsyncRequestBody(AsyncRequestBody delegate) {
        this.delegate = delegate;
    }

    /**
     * Creates a new {@link BufferedSplittableAsyncRequestBody} that wraps the provided {@link AsyncRequestBody}.
     *
     * @param delegate the {@link AsyncRequestBody} to wrap and make retryable. Must not be null.
     * @return a new {@link BufferedSplittableAsyncRequestBody} instance
     * @throws NullPointerException if delegate is null
     */
    public static BufferedSplittableAsyncRequestBody create(AsyncRequestBody delegate) {
        Validate.paramNotNull(delegate, "delegate");
        return new BufferedSplittableAsyncRequestBody(delegate);
    }

    @Override
    public Optional<Long> contentLength() {
        return delegate.contentLength();
    }

    /**
     * Splits this request body into multiple retryable parts based on the provided configuration.
     *
     * <p>Each part returned by the publisher will be a {@link CloseableAsyncRequestBody} that buffers
     * its portion of data, enabling resubscription for retry scenarios. This is the key difference from non-buffered splitting -
     * each part can be safely retried without data loss.
     *
     * <p>The splitting process respects the chunk size and buffer size specified in the configuration
     * to optimize memory usage.
     *
     * <p>The subscriber MUST close each {@link CloseableAsyncRequestBody} to ensure resource is released
     *
     * @param splitConfiguration configuration specifying how to split the request body
     * @return a publisher that emits retryable closable request body parts
     * @see AsyncRequestBodySplitConfiguration
     */
    @Override
    public SdkPublisher<CloseableAsyncRequestBody> splitCloseable(AsyncRequestBodySplitConfiguration splitConfiguration) {
        return SplittingPublisher.builder()
                .asyncRequestBody(this)
                .splitConfiguration(splitConfiguration)
                .retryableSubAsyncRequestBodyEnabled(true)
                .build();
    }

    @Override
    public void subscribe(Subscriber<? super ByteBuffer> s) {
        delegate.subscribe(s);
    }

    @Override
    public String body() {
        return delegate.body();
    }
}
