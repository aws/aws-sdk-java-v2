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

package foo.bar;

import com.amazonaws.services.s3.event.S3EventNotification;
import com.amazonaws.services.s3.event.S3EventNotification.S3EventNotificationRecord;
import com.amazonaws.services.s3.event.S3EventNotification.RestoreEventDataEntity;
import com.amazonaws.services.s3.event.S3EventNotification.GlacierEventDataEntity;
import com.amazonaws.services.s3.model.S3Event;
import org.joda.time.DateTime;

public class S3EnDateTime {

    public void parseEvent(String jsonInput) {
        S3EventNotification notification = S3EventNotification.parseJson(jsonInput);

        for (S3EventNotification.S3EventNotificationRecord record : notification.getRecords()) {
            DateTime eventTime = record.getEventTime();

            GlacierEventDataEntity glacierEventData = record.getGlacierEventData();

            RestoreEventDataEntity restoreEventData = glacierEventData.getRestoreEventData();

            DateTime expireTime = restoreEventData.getLifecycleRestorationExpiryTime();

        }
    }

}