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
 * The request cycle {@link ProgressEvent} related to the execution of a single http request-response.
 */
@SdkPublicApi
public final class RequestCycleEvent implements ProgressEvent {
    private final RequestCycleEventType eventType;
    private final RequestCycleEventData eventData;

    public RequestCycleEvent(RequestCycleEventType eventType, RequestCycleEventData eventData) {
        this.eventType = eventType;
        this.eventData = eventData;
    }

    /**
     * Create a {@link ProgressEvent} with event type and {@link ProgressEventData}.
     *
     * @param eventType the type of the event
     * @param eventData the progress event data
     * @return an instance of ProgressEvent
     */
    public static RequestCycleEvent create(RequestCycleEventType eventType, RequestCycleEventData eventData) {
        return new RequestCycleEvent(eventType, eventData);
    }

    /**
     * @return the type of the progress event
     */
    public RequestCycleEventType eventType() {
        return eventType;
    }

    /**
     * @return the optional event data
     */
    public RequestCycleEventData eventData() {
        return eventData;
    }

    @Override
    public String toString() {
        return ToString.builder("ProgressEvent")
                       .add("eventType", eventType)
                       .add("eventData", eventData)
                       .build();
    }

    public static final class RequestCycleEventData implements ProgressEventData {
        private final EventContext context;

        public RequestCycleEventData(EventContext context) {
            this.context = context;
        }

        @Override
        public EventContext eventContext() {
            return context;
        }

    }
}
