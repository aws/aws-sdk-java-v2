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

import java.util.function.BiFunction;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.SdkPublisher;

/**
 * Publisher implementation that wraps the content of another publisher in an envelope with an optional prefix (or
 * header) and suffix (or footer). The prefix content will be prepended to the first published object from the
 * wrapped publisher, and the suffix content will be published when the wrapped publisher signals completion.
 * <p>
 * The envelope prefix will not be published until the wrapped publisher publishes something or is completed.
 * The envelope suffix will not be published until the wrapped publisher is completed.
 * <p>
 * This class can be used in an asynchronous interceptor in the AWS SDK to wrap content around the incoming
 * bytestream from a response.
 * <p>
 * A function must be supplied that can be used to concatenate the envelope content with the content being published by
 * the wrapped publisher. Example usage:
 * {@code
 *    Publisher<String> wrappedPublisher = ContentEnvelopeWrappingPublisher.of(publisher, "S", "E", (s1, s2) -> s1 + s2);
 * }
 * If publisher publishes a single string "1", wrappedPublisher will publish "S1" (prepending the envelop prefix). If
 * publisher then publishes a second string "2", wrappedPublisher will then publish "2" (no added content). If
 * publisher then completes, wrappedPublisher will then publish "E" and then complete.
 * <p>
 * WARNING: This publisher implementation does not comply with the complete flow spec (as it inserts data into the
 * middle of a flow between a third-party publisher and subscriber rather than acting as a fully featured
 * independent publisher) and therefore should only be used in a limited fashion when we have complete control over
 * how data is being published to the publisher it wraps.
 *
 * @param <T> The type of objects being published
 */

@SdkInternalApi
public class EnvelopeWrappedSdkPublisher<T> implements SdkPublisher<T> {
    private final Publisher<T> wrappedPublisher;
    private final T contentPrefix;
    private final T contentSuffix;
    private final BiFunction<T, T, T> mergeContentFunction;

    private EnvelopeWrappedSdkPublisher(Publisher<T> wrappedPublisher,
                                        T contentPrefix,
                                        T contentSuffix,
                                        BiFunction<T, T, T> mergeContentFunction) {
        this.wrappedPublisher = wrappedPublisher;
        this.contentPrefix = contentPrefix;
        this.contentSuffix = contentSuffix;
        this.mergeContentFunction = mergeContentFunction;
    }

    /**
     * Create a new publisher that wraps the content of an existing publisher.
     * @param wrappedPublisher The publisher who's content will be wrapped.
     * @param contentPrefix The content to be inserted in front of the wrapped content.
     * @param contentSuffix The content to be inserted at the back of the wrapped content.
     * @param mergeContentFunction A function that will be used to merge the inserted content into the wrapped content.
     * @param <T> The content type.
     * @return A newly initialized instance of this class.
     */
    public static <T> EnvelopeWrappedSdkPublisher<T> of(Publisher<T> wrappedPublisher,
                                                        T contentPrefix,
                                                        T contentSuffix,
                                                        BiFunction<T, T, T> mergeContentFunction) {
        return new EnvelopeWrappedSdkPublisher<>(wrappedPublisher, contentPrefix, contentSuffix, mergeContentFunction);
    }

    /**
     * See {@link Publisher#subscribe(Subscriber)}
     */
    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        if (subscriber == null) {
            throw new NullPointerException("subscriber must be non-null on call to subscribe()");
        }

        wrappedPublisher.subscribe(new ContentWrappedSubscriber(subscriber));
    }

    private class ContentWrappedSubscriber implements Subscriber<T> {
        private final Subscriber<? super T> wrappedSubscriber;
        private boolean prefixApplied = false;
        private boolean suffixApplied = false;

        private ContentWrappedSubscriber(Subscriber<? super T> wrappedSubscriber) {
            this.wrappedSubscriber = wrappedSubscriber;
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            wrappedSubscriber.onSubscribe(subscription);
        }

        @Override
        public void onNext(T t) {
            T contentToSend = t;

            if (!prefixApplied) {
                prefixApplied = true;

                if (contentPrefix != null) {
                    contentToSend = mergeContentFunction.apply(contentPrefix, t);
                }
            }

            wrappedSubscriber.onNext(contentToSend);
        }

        @Override
        public void onError(Throwable throwable) {
            wrappedSubscriber.onError(throwable);
        }

        @Override
        public void onComplete() {
            try {
                // Only transmit the close of the envelope once and only if the prefix has been previously sent.
                if (!suffixApplied && prefixApplied) {
                    suffixApplied = true;

                    if (contentSuffix != null) {
                        // TODO: This should respect the demand from the subscriber as technically an onComplete
                        //  signal could be received even if there is no demand. We have minimized the impact of this
                        //  by only making it applicable in situations where there data has already been transmitted
                        //  over the stream.
                        wrappedSubscriber.onNext(contentSuffix);
                    }
                }
            } finally {
                wrappedSubscriber.onComplete();
            }
        }
    }
}
