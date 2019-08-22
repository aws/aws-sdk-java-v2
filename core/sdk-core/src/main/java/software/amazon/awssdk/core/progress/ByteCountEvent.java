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

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.ToString;

/**
 * The Byte count {@link ProgressEvent} indicating the number of bytes in the execution of a single http request-response.
 */
@SdkPublicApi
public final class ByteCountEvent implements ProgressEvent {
    private final ByteCountEventType eventType;
    private final ByteCountEventData eventData;

    public ByteCountEvent(ByteCountEventType eventType,
                          ByteCountEventData eventData) {
        this.eventType = eventType;
        this.eventData = eventData;
    }

    /**
     * @return the type of the progress event
     */
    public ByteCountEventType eventType() {
        return eventType;
    }

    @Override
    public ByteCountEventData eventData() {
        return eventData;
    }

    @Override
    public String toString() {
        return ToString.builder("ProgressEvent")
                       .add("eventType", eventType)
                       .add("eventData", eventData)
                       .build();
    }

    public static final class ByteCountEventData implements ProgressEventData {
        private final EventContext context;
        private final long bytes;

        public ByteCountEventData(EventContext context, long bytes) {
            this.context = context;
            this.bytes = bytes;
        }

        @Override
        public EventContext eventContext() {
            return context;
        }

        /**
         * @return number of bytes associated with the event.
         */
        public long bytes() {
            return bytes;
        }
    }


}
