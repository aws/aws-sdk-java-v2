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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.progress.ProgressEvent;
import software.amazon.awssdk.core.progress.ProgressEventListener;
import software.amazon.awssdk.core.progress.ProgressEventResult;
import software.amazon.awssdk.core.progress.ProgressEventType;


/**
 * Extended listener to listen to additional {@link S3TransferEvent}s.
 */
@SdkPublicApi
public interface S3TransferProgressEventListener extends ProgressEventListener, S3TransferEventHandler {
    Set<Class<? extends ProgressEvent>> SUPPORTED_TYPES =
        Collections.unmodifiableSet(new HashSet<>(Arrays.asList(S3TransferEvent.class)));

    @Override
    default Set<Class<? extends ProgressEvent>> extendedTypes() {
        return SUPPORTED_TYPES;
    }

    @Override
    default CompletableFuture<? extends ProgressEventResult> onExtendedEvents(ProgressEvent progressEvent) {

        ProgressEventType progressEventType = progressEvent.eventType();
        if (progressEventType instanceof S3TransferEventType) {

            return onTransferEvent((S3TransferEvent) progressEvent);
        }

        return onDefault(progressEvent);
    }

    @Override
    default CompletableFuture<? extends ProgressEventResult> onTransferEvent(S3TransferEvent progressEvent) {
        return onDefault(progressEvent);
    }

    static Builder builder() {
        return new S3TransferProgressEventListenerBuilder();
    }

    interface Builder extends ProgressEventListener.Builder<Builder> {
        Builder onTransferEvent(S3TransferEventHandler transferEventHandler);

        S3TransferProgressEventListener build();
    }
}
