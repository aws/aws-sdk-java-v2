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

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.progress.EventContext;
import software.amazon.awssdk.core.progress.ProgressEvent;
import software.amazon.awssdk.core.progress.ProgressEventData;
import software.amazon.awssdk.custom.s3.transfer.Transfer;

/**
 * The {@link ProgressEvent}s related to a {@link Transfer}.
 */
@SdkPublicApi
public final class S3TransferEvent implements ProgressEvent {

    private final S3TransferEventType eventType;
    private final TransferProgressEventData eventData;

    public S3TransferEvent(S3TransferEventType eventType, TransferProgressEventData eventData) {
        this.eventType = eventType;
        this.eventData = eventData;
    }

    @Override
    public S3TransferEventType eventType() {
        return eventType;
    }

    @Override
    public TransferProgressEventData eventData() {
        return null;
    }

    public static class TransferProgressEventData implements ProgressEventData {

        @Override
        public TransferProgressContext eventContext() {
            return null;
        }
    }


    public final class TransferProgressContext implements EventContext {
        private final Transfer transfer;

        public TransferProgressContext(Transfer transfer) {
            this.transfer = transfer;
        }

        /**
         * @return
         */
        public Transfer transfer() {
            return transfer;
        }

        @Override
        public SdkRequest request() {
            return null;
        }
    }
}
