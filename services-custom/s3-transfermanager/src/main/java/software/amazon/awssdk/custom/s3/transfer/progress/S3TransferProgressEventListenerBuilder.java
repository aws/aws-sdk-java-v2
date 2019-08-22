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

package software.amazon.awssdk.custom.s3.transfer.progress;

import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.progress.AbstractProgressEventListenerBuilder;
import software.amazon.awssdk.core.progress.ProgressEventResult;


/**
 * Extended listener to listen to {@link S3TransferEvent}s.
 */
@SdkInternalApi
public final class S3TransferProgressEventListenerBuilder extends AbstractProgressEventListenerBuilder<S3TransferProgressEventListener.Builder> implements S3TransferProgressEventListener.Builder {

    private S3TransferEventHandler transferEventHandler;

    S3TransferProgressEventListenerBuilder() {
    }

    @Override
    public S3TransferProgressEventListenerBuilder onTransferEvent(S3TransferEventHandler transferEventHandler) {
        this.transferEventHandler = transferEventHandler;
        return this;
    }

    @Override
    public S3TransferProgressEventListener build() {
        return new DefaultS3TransferProgressEventListener(this);
    }

    static final class DefaultS3TransferProgressEventListener implements S3TransferProgressEventListener {
        private final S3TransferEventHandler transferEventHandler;

        DefaultS3TransferProgressEventListener(S3TransferProgressEventListenerBuilder builder) {
            this.transferEventHandler = builder.transferEventHandler == null ?
                                        S3TransferProgressEventListener.super::onDefault : builder.transferEventHandler;
        }

        @Override
        public CompletableFuture<? extends ProgressEventResult> onTransferEvent(S3TransferEvent progressEvent) {
            return transferEventHandler.onTransferEvent(progressEvent);
        }
    }
}
