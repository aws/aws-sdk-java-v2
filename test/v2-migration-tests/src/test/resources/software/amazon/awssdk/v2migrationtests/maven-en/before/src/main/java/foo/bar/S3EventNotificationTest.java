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

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.event.S3EventNotification;
import com.amazonaws.services.s3.event.S3EventNotification.S3BucketEntity;
import com.amazonaws.services.s3.event.S3EventNotification.S3Entity;
import com.amazonaws.services.s3.event.S3EventNotification.S3EventNotificationRecord;
import com.amazonaws.services.s3.event.S3EventNotification.S3ObjectEntity;
import com.amazonaws.services.s3.event.S3EventNotification.RequestParametersEntity;
import com.amazonaws.services.s3.event.S3EventNotification.ResponseElementsEntity;
import com.amazonaws.services.s3.event.S3EventNotification.RestoreEventDataEntity;
import com.amazonaws.services.s3.event.S3EventNotification.UserIdentityEntity;
import com.amazonaws.services.s3.event.S3EventNotification.GlacierEventDataEntity;
// import com.amazonaws.services.s3.event.S3EventNotification.LifecycleEventDataEntity;
// import com.amazonaws.services.s3.event.S3EventNotification.IntelligentTieringEventDataEntity;
// import com.amazonaws.services.s3.event.S3EventNotification.ReplicationEventDataEntity;
import org.joda.time.DateTime;

public class S3EventNotificationTest {
    public void parseEvent(String jsonInput) {
        S3EventNotification notification = S3EventNotification.parseJson(jsonInput);

        for (S3EventNotificationRecord record : notification.getRecords()) {
            S3Entity s3 = record.getS3();

            S3BucketEntity bucket = s3.getBucket();

            S3ObjectEntity object = s3.getObject();

            String eventName = record.getEventName();

            //S3Event eventNameEnum = record.getEventNameAsEnum();

            DateTime eventTime = record.getEventTime();

            RequestParametersEntity requestParams = record.getRequestParameters();

            ResponseElementsEntity responseElements = record.getResponseElements();

            UserIdentityEntity userIdentity = record.getUserIdentity();

            GlacierEventDataEntity glacierEventData = record.getGlacierEventData();

            RestoreEventDataEntity restoreEventData = glacierEventData.getRestoreEventData();

            // LifecycleEventDataEntity lifecycleEventData = record.getLifecycleEventData();
            //
            // IntelligentTieringEventDataEntity intelligentTieringEventData = record.getIntelligentTieringEventData();
            //
            // ReplicationEventDataEntity replicationEventData = record.getReplicationEventDataEntity();
        }
    }

}