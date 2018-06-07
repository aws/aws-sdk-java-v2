/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.kinesis.model;

import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.BaseAsyncResponseTransformer;
import software.amazon.awssdk.core.pagination.async.SdkPublisher;

/**
 * Response transformer for the SubscribeToShard API.
 *
 * @param <ReturnT> Type of transformed response. May be {@link Void}.
 */
public interface SubscribeToShardResponseTransformer<ReturnT>
    extends BaseAsyncResponseTransformer<SubscribeToShardResponse, SubscribeToShardResponseTransformer.Publisher, ReturnT> {

    /**
     * Publisher for the SubscribeToShard API.
     */
    final class Publisher implements SdkPublisher<SubscribeToShardBaseEvent> {

        private final SdkPublisher<SubscribeToShardBaseEvent> delegate;

        private Publisher(SdkPublisher<SubscribeToShardBaseEvent> delegate) {
            this.delegate = delegate;
        }

        @Override
        public void subscribe(Subscriber<? super SubscribeToShardBaseEvent> subscriber) {
            delegate.subscribe(subscriber);
        }

        /**
         * Invokes the visitor for each event in the stream.
         *
         * @param visitor Visitor to invoke for each event.
         * @return CompletableFuture which will be notified when last event has been processed successfully or an
         * error occurs.
         */
        public final CompletableFuture<Void> forEach(SubscribeToShardBaseEvent.Visitor visitor) {
            return forEach(s -> s.visit(visitor));
        }

        @SdkInternalApi
        public static Publisher create(SdkPublisher<SubscribeToShardBaseEvent> delegate) {
            return new Publisher(delegate);
        }

    }
}
