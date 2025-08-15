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
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncRequestBodySplitConfiguration;
import software.amazon.awssdk.core.async.ClosableAsyncRequestBody;
import software.amazon.awssdk.core.exception.NonRetryableException;
import software.amazon.awssdk.core.internal.util.NoopSubscription;

/**
 * Adaptor to convert a {@link ClosableAsyncRequestBody} to an {@link AsyncRequestBody}
 *
 * <p>
 * This is needed to maintain backwards compatibility for the deprecated
 * {@link AsyncRequestBody#split(AsyncRequestBodySplitConfiguration)}
 */
@SdkInternalApi
public final class ClosableAsyncRequestBodyAdaptor implements AsyncRequestBody {

    private final AtomicBoolean subscribeCalled;
    private final ClosableAsyncRequestBody delegate;

    public ClosableAsyncRequestBodyAdaptor(ClosableAsyncRequestBody delegate) {
        this.delegate = delegate;
        subscribeCalled = new AtomicBoolean(false);
    }

    @Override
    public Optional<Long> contentLength() {
        return delegate.contentLength();
    }

    @Override
    public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
        if (subscribeCalled.compareAndSet(false, true)) {
            delegate.doAfterOnComplete(() -> delegate.close())
                    .doAfterOnCancel(() -> delegate.close())
                    .doAfterOnError(t -> delegate.close())
                    .subscribe(subscriber);
        } else {
            subscriber.onSubscribe(new NoopSubscription(subscriber));
            subscriber.onError(NonRetryableException.create(
                "A retry was attempted, but AsyncRequestBody.split does not "
                + "support retries."));
        }
    }

}
