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
 * The duration of an event-hold retention, within {@link ObjectRetentionEventData}. Only present when variable
 * (event-hold) retention is used.
 *
 * <p>The duration is modeled as a single {@code unit}/{@code value} pair to mirror the message-format contract emitted
 * by the service: the unit becomes the JSON key and the value its number, producing {@code {"days": 90}} or
 * {@code {"years": 1}}. Modeling it this way (rather than fixed {@code days}/{@code years} fields) means the reader
 * surfaces whatever unit the service emits, so a new unit does not require an SDK change.
 */
@SdkPublicApi
public class EventHoldDuration {

    private final String unit;
    private final Long value;

    public EventHoldDuration(String unit, Long value) {
        this.unit = unit;
        this.value = value;
    }

    /**
     * @return the duration unit, for example {@code "days"} or {@code "years"}.
     */
    public String getUnit() {
        return unit;
    }

    /**
     * @return the duration value for the given {@link #getUnit() unit}.
     */
    public Long getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        EventHoldDuration that = (EventHoldDuration) o;

        if (!Objects.equals(unit, that.unit)) {
            return false;
        }
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        int result = unit != null ? unit.hashCode() : 0;
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return ToString.builder("EventHoldDuration")
                       .add("unit", unit)
                       .add("value", value)
                       .build();
    }
}
