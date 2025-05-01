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
import software.amazon.awssdk.eventnotifications.s3.model.S3EventNotification;
import software.amazon.awssdk.eventnotifications.s3.model.S3EventNotificationRecord;
import software.amazon.awssdk.eventnotifications.s3.model.RestoreEventData;
import software.amazon.awssdk.eventnotifications.s3.model.GlacierEventData;
import org.joda.time.DateTime;

public class S3EnDateTime {

    public void parseEvent(String jsonInput) {
        S3EventNotification notification = S3EventNotification.fromJson(jsonInput);

        for (S3EventNotification.S3EventNotificationRecord record : notification.getRecords()) {
            DateTime eventTime = /*AWS SDK for Java v2 migration: getEventTime returns Instant instead of DateTime in v2. AWS SDK v2 does not include org.joda.time as a dependency. If you want to keep using DateTime, you'll need to manually add "org.joda.time:joda-time" dependency to your project after migration.*/new DateTime(record.getEventTime().toEpochMilli());

            GlacierEventData glacierEventData = record.getGlacierEventData();

            RestoreEventData restoreEventData = glacierEventData.getRestoreEventData();

            DateTime expireTime = /*AWS SDK for Java v2 migration: getLifecycleRestorationExpiryTime returns Instant instead of DateTime in v2. AWS SDK v2 does not include org.joda.time as a dependency. If you want to keep using DateTime, you'll need to manually add "org.joda.time:joda-time" dependency to your project after migration.*/new DateTime(restoreEventData.getLifecycleRestorationExpiryTime().toEpochMilli());

        }
    }

}