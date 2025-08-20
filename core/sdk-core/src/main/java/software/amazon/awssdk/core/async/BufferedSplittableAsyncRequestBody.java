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

/**
 * An {@link AsyncRequestBody} decorator that can be split into buffered sub {@link AsyncRequestBody}s. Each sub
 * {@link AsyncRequestBody} can be retried/resubscribed if all data has been successfully been published to first subscriber.
 */
@SdkPublicApi
public final class BufferedSplittableAsyncRequestBody implements AsyncRequestBody {
    private final AsyncRequestBody delegate;

    private BufferedSplittableAsyncRequestBody(AsyncRequestBody delegate) {
        this.delegate = delegate;
    }

    public static BufferedSplittableAsyncRequestBody create(AsyncRequestBody delegate) {
        return new BufferedSplittableAsyncRequestBody(delegate);
    }

    @Override
    public Optional<Long> contentLength() {
        return delegate.contentLength();
    }

    @Override
    public SdkPublisher<ClosableAsyncRequestBody> splitClosable(AsyncRequestBodySplitConfiguration splitConfiguration) {
        return new SplittingPublisher(this, splitConfiguration, true);
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
