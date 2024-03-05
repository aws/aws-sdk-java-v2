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

package software.amazon.awssdk.eventnotifications.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.CalendarSerializer;
import com.fasterxml.jackson.databind.util.ISO8601DateFormat;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import software.amazon.awssdk.eventnotifications.InstantDeserializer;
import software.amazon.awssdk.eventnotifications.InstantSerializer;

public class RestoreEventDataEntity {

    @JsonDeserialize(using = InstantDeserializer.class)
    @JsonSerialize(using = InstantSerializer.class)
    private Instant lifecycleRestorationExpiryTime;
    private final String lifecycleRestoreStorageClass;

    @JsonCreator
    public RestoreEventDataEntity(
        @JsonProperty("lifecycleRestorationExpiryTime") String lifecycleRestorationExpiryTime,
        @JsonProperty("lifecycleRestoreStorageClass") String lifecycleRestoreStorageClass) {
        if (lifecycleRestorationExpiryTime != null) {
            this.lifecycleRestorationExpiryTime = Instant.parse(lifecycleRestorationExpiryTime);
        }
        this.lifecycleRestoreStorageClass = lifecycleRestoreStorageClass;
    }

    @JsonProperty("lifecycleRestorationExpiryTime")
    public Instant getLifecycleRestorationExpiryTime() {
        return lifecycleRestorationExpiryTime;
    }

    @JsonProperty("lifecycleRestoreStorageClass")
    public String getLifecycleRestoreStorageClass() {
        return lifecycleRestoreStorageClass;
    }

}
