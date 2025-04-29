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

import software.amazon.awssdk.eventnotifications.s3.model.S3Bucket;
import software.amazon.awssdk.eventnotifications.s3.model.S3EventNotification;
import software.amazon.awssdk.eventnotifications.s3.model.S3;
import software.amazon.awssdk.eventnotifications.s3.model.S3EventNotificationRecord;
import software.amazon.awssdk.eventnotifications.s3.model.S3Object;
import software.amazon.awssdk.eventnotifications.s3.model.RequestParameters;
import software.amazon.awssdk.eventnotifications.s3.model.ResponseElements;
import software.amazon.awssdk.eventnotifications.s3.model.RestoreEventData;
import software.amazon.awssdk.eventnotifications.s3.model.UserIdentity;
import software.amazon.awssdk.eventnotifications.s3.model.GlacierEventData;
import software.amazon.awssdk.eventnotifications.s3.model.LifecycleEventData;
import software.amazon.awssdk.eventnotifications.s3.model.IntelligentTieringEventData;
import software.amazon.awssdk.eventnotifications.s3.model.ReplicationEventData;
import org.joda.time.DateTime;

public class S3EventNotificationTest {
    public void parseEvent(String jsonInput) {
        S3EventNotification notification = S3EventNotification.fromJson(jsonInput);

        for (S3EventNotificationRecord record : notification.getRecords()) {
            S3 s3 = record.getS3();

            S3Bucket bucket = s3.getBucket();

            S3Object object = s3.getObject();

            String eventName = record.getEventName();

            String eventNameEnum = record.getEventName();

            DateTime eventTime = /*AWS SDK for Java v2 migration: getEventTime returns Instant instead of DateTime in v2. AWS SDK v2 does not include org.joda.time as a dependency. If you want to keep using DateTime, you'll need to manually add "org.joda.time:joda-time" dependency to your project after migration.*/new DateTime(record.getEventTime().toEpochMilli());

            RequestParameters requestParams = record.getRequestParameters();

            ResponseElements responseElements = record.getResponseElements();

            UserIdentity userIdentity = record.getUserIdentity();

            GlacierEventData glacierEventData = record.getGlacierEventData();

            RestoreEventData restoreEventData = glacierEventData.getRestoreEventData();

            DateTime expireTime = /*AWS SDK for Java v2 migration: getLifecycleRestorationExpiryTime returns Instant instead of DateTime in v2. AWS SDK v2 does not include org.joda.time as a dependency. If you want to keep using DateTime, you'll need to manually add "org.joda.time:joda-time" dependency to your project after migration.*/new DateTime(restoreEventData.getLifecycleRestorationExpiryTime().toEpochMilli());

            LifecycleEventData lifecycleEventData = record.getLifecycleEventData();

            IntelligentTieringEventData intelligentTieringEventData = record.getIntelligentTieringEventData();

            ReplicationEventData replicationEventData = record.getReplicationEventData();
        }
    }

}