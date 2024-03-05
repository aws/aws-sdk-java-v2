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


import java.util.List;

/**
 * A helper class that represents a strongly typed S3 EventNotification item sent
 * to SQS, SNS, or Lambda.
 */
public class S3EventNotification {

    private final List<S3EventNotificationRecord> records;

    // @JsonCreator
    public S3EventNotification(
        // @JsonProperty("Records")
        List<S3EventNotificationRecord> records) {
        this.records = records;
    }

    // @JsonProperty("Records")
    public List<S3EventNotificationRecord> getRecords() {
        return records;
    }
}
