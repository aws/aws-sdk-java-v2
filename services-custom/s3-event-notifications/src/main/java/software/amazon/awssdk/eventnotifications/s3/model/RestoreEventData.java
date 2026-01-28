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

import java.time.Instant;
import java.util.Objects;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.ToString;

/**
 * The RestoreEventData contains attributes that are related to the restore request.
 */
@SdkPublicApi
public class RestoreEventData {

    private final Instant lifecycleRestorationExpiryTime;
    private final String lifecycleRestoreStorageClass;

    public RestoreEventData(String lifecycleRestorationExpiryTime, String lifecycleRestoreStorageClass) {
        this.lifecycleRestorationExpiryTime =
            lifecycleRestorationExpiryTime != null ? Instant.parse(lifecycleRestorationExpiryTime) : null;
        this.lifecycleRestoreStorageClass = lifecycleRestoreStorageClass;
    }

    /**
     * @return The time, in ISO-8601 format, for example, 1970-01-01T00:00:00.000Z, of Restore Expiry.
     */
    public Instant getLifecycleRestorationExpiryTime() {
        return lifecycleRestorationExpiryTime;
    }

    /**
     * @return The source storage class for restore.
     */
    public String getLifecycleRestoreStorageClass() {
        return lifecycleRestoreStorageClass;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        RestoreEventData that = (RestoreEventData) o;

        if (!Objects.equals(lifecycleRestorationExpiryTime, that.lifecycleRestorationExpiryTime)) {
            return false;
        }
        return Objects.equals(lifecycleRestoreStorageClass, that.lifecycleRestoreStorageClass);
    }

    @Override
    public int hashCode() {
        int result = lifecycleRestorationExpiryTime != null ? lifecycleRestorationExpiryTime.hashCode() : 0;
        result = 31 * result + (lifecycleRestoreStorageClass != null ? lifecycleRestoreStorageClass.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return ToString.builder("RestoreEventData")
                       .add("lifecycleRestorationExpiryTime", lifecycleRestorationExpiryTime)
                       .add("lifecycleRestoreStorageClass", lifecycleRestoreStorageClass)
                       .build();
    }
}
