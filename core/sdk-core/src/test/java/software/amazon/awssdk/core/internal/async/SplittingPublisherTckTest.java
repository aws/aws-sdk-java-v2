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
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncRequestBodySplitConfiguration;
import software.amazon.awssdk.core.async.CloseableAsyncRequestBody;

/**
 * Reactive Streams TCK verification for {@link SplittingPublisher}. Each element is a one-byte part. The TCK never
 * subscribes to the parts, so the buffer must hold the whole content.
 */
public class SplittingPublisherTckTest extends PublisherVerification<CloseableAsyncRequestBody> {

    public SplittingPublisherTckTest() {
        super(new TestEnvironment());
    }

    @Override
    public Publisher<CloseableAsyncRequestBody> createPublisher(long elements) {
        if (elements == 0) {
            // A known content length of 0 still emits one empty part, so use an empty unknown-length upstream.
            return splittingPublisher(new TerminatingAsyncRequestBody(null), 1);
        }
        int n = Math.toIntExact(elements);
        return splittingPublisher(AsyncRequestBody.fromBytes(new byte[n]), n);
    }

    @Override
    public Publisher<CloseableAsyncRequestBody> createFailedPublisher() {
        return splittingPublisher(new TerminatingAsyncRequestBody(new RuntimeException("upstream failure")), 1);
    }

    @Override
    public long maxElementsFromPublisher() {
        return 1024;
    }

    private static SplittingPublisher splittingPublisher(AsyncRequestBody upstream, long bufferSize) {
        return SplittingPublisher.builder()
                                 .asyncRequestBody(upstream)
                                 .splitConfiguration(AsyncRequestBodySplitConfiguration.builder()
                                                                                       .chunkSizeInBytes(1L)
                                                                                       .bufferSizeInBytes(bufferSize)
                                                                                       .build())
                                 .retryableSubAsyncRequestBodyEnabled(false)
                                 .build();
    }

    /**
     * Unknown-length upstream that signals onSubscribe followed by onComplete (error == null) or onError.
     */
    private static final class TerminatingAsyncRequestBody implements AsyncRequestBody {
        private final Throwable error;

        private TerminatingAsyncRequestBody(Throwable error) {
            this.error = error;
        }

        @Override
        public Optional<Long> contentLength() {
            return Optional.empty();
        }

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> s) {
            s.onSubscribe(new Subscription() {
                @Override
                public void request(long n) {
                }

                @Override
                public void cancel() {
                }
            });
            if (error == null) {
                s.onComplete();
            } else {
                s.onError(error);
            }
        }
    }
}
