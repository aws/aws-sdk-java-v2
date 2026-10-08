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

package software.amazon.awssdk.eventnotifications.s3.model;

import java.util.Objects;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.ToString;

/**
 * The ObjectRetentionEventData is only visible for {@code s3:ObjectRetention:Put} events, which fire on the explicit
 * <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_PutObjectRetention.html">PutObjectRetention</a> API call.
 * The {@code eventHold} and {@code eventHoldDuration} fields are only present when variable (event-hold) retention is used;
 * they are absent for fixed retention.
 */
@SdkPublicApi
public class ObjectRetentionEventData {

    private final String mode;
    private final String retainUntilDate;
    private final String eventHold;
    private final EventHoldDuration eventHoldDuration;

    public ObjectRetentionEventData(String mode, String retainUntilDate, String eventHold,
                                    EventHoldDuration eventHoldDuration) {
        this.mode = mode;
        this.retainUntilDate = retainUntilDate;
        this.eventHold = eventHold;
        this.eventHoldDuration = eventHoldDuration;
    }

    /**
     * @return the retention mode, {@code COMPLIANCE} or {@code GOVERNANCE}.
     */
    public String getMode() {
        return mode;
    }

    /**
     * @return the retain-until-date in ISO-8601 format after the operation completed. For variable retention with event
     * hold ON, this is dynamically computed as {@code now + duration}.
     */
    public String getRetainUntilDate() {
        return retainUntilDate;
    }

    /**
     * @return the event hold state, {@code ON} or {@code OFF}. Only present when variable retention is used; absent for
     * fixed retention.
     */
    public String getEventHold() {
        return eventHold;
    }

    /**
     * @return the event hold duration. Only present when variable retention is used; absent for fixed retention.
     */
    public EventHoldDuration getEventHoldDuration() {
        return eventHoldDuration;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ObjectRetentionEventData that = (ObjectRetentionEventData) o;

        if (!Objects.equals(mode, that.mode)) {
            return false;
        }
        if (!Objects.equals(retainUntilDate, that.retainUntilDate)) {
            return false;
        }
        if (!Objects.equals(eventHold, that.eventHold)) {
            return false;
        }
        return Objects.equals(eventHoldDuration, that.eventHoldDuration);
    }

    @Override
    public int hashCode() {
        int result = mode != null ? mode.hashCode() : 0;
        result = 31 * result + (retainUntilDate != null ? retainUntilDate.hashCode() : 0);
        result = 31 * result + (eventHold != null ? eventHold.hashCode() : 0);
        result = 31 * result + (eventHoldDuration != null ? eventHoldDuration.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return ToString.builder("ObjectRetentionEventData")
                       .add("mode", mode)
                       .add("retainUntilDate", retainUntilDate)
                       .add("eventHold", eventHold)
                       .add("eventHoldDuration", eventHoldDuration)
                       .build();
    }
}
