/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.core.progress;

import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.progress.ProgressEventHandler.ByteCountEventHandler;
import software.amazon.awssdk.core.progress.ProgressEventHandler.RequestLifeCycleEventHandler;

/**
 * Builder for an {@link ProgressEventListener}.
 * @param <B>
 */
@SdkProtectedApi
public abstract class AbstractProgressEventListenerBuilder<B extends ProgressEventListener.Builder> implements ProgressEventListener.Builder<B> {

    private ByteCountEventHandler byteCountEventHandler;
    private RequestLifeCycleEventHandler requestLifeCycleEventHandler;
    private ProgressEventHandler defaultEventHandler;

    @Override
    public B onDefault(ProgressEventHandler defaultEventHandler) {
        this.defaultEventHandler = defaultEventHandler;
        return (B) this;
    }

    @Override
    public B onByteCountEvent(ByteCountEventHandler byteCountEventHandler) {
        this.byteCountEventHandler = byteCountEventHandler;
        return (B) this;
    }

    @Override
    public B onRequestLifeCycleEvent(RequestLifeCycleEventHandler requestLifeCycleEventHandler) {
        this.requestLifeCycleEventHandler = requestLifeCycleEventHandler;
        return (B) this;
    }

    static final class DefaultProgressEventListenerBuilder extends AbstractProgressEventListenerBuilder implements ProgressEventListener.Builder {
        @Override
        public ProgressEventListener build() {
            return new DefaultProgressEventListener(this);
        }
    }

    final class DefaultProgressEventListener implements ProgressEventListener {
        private final ByteCountEventHandler byteCountEventHandler;
        private final RequestLifeCycleEventHandler requestLifeCycleEventHandler;
        private final ProgressEventHandler defaultEventHandler;

        DefaultProgressEventListener(AbstractProgressEventListenerBuilder builder) {
            this.defaultEventHandler = builder.defaultEventHandler == null ? ProgressEventListener.super::onDefault : builder.defaultEventHandler;
            this.byteCountEventHandler = builder.byteCountEventHandler == null ? ProgressEventListener.super::onDefault :
                                         builder.byteCountEventHandler;
            this.requestLifeCycleEventHandler = builder.requestLifeCycleEventHandler == null ?
                                                ProgressEventListener.super::onDefault :
                                                builder.requestLifeCycleEventHandler;
        }

        @Override
        public CompletableFuture<? extends ProgressEventResult>  onDefault(ProgressEvent progressEventHandler) {
            return defaultEventHandler.onDefault(progressEventHandler);
        }

        @Override
        public CompletableFuture<? extends ProgressEventResult> onRequestLifeCycleEvent(RequestCycleEvent progressEvent) {
            return requestLifeCycleEventHandler.onRequestLifeCycleEvent(progressEvent);
        }

        @Override
        public CompletableFuture<? extends ProgressEventResult> onByteCountEvent(ByteCountEvent progressEvent) {
            return byteCountEventHandler.onByteCountEvent(progressEvent);
        }
    }
}
