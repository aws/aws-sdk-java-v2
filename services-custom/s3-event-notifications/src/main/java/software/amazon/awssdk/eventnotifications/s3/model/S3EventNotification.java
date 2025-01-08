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


import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.eventnotifications.s3.internal.S3EventNotificationReader;
import software.amazon.awssdk.eventnotifications.s3.internal.S3EventNotificationWriter;
import software.amazon.awssdk.utils.ToString;

/**
 * A helper class that represents a strongly typed S3 Event Notification item sent to SQS, SNS, or Lambda. For more information
 * about Amazon S3 Event Notifications, visit the
 * <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/EventNotifications.html">S3 User Guide</a>.
 * This class can be used to parse notification messages in the json format or to serialize a S3EventNotification instance to
 * json.
 */
@SdkPublicApi
public class S3EventNotification {

    private final List<S3EventNotificationRecord> records;

    public S3EventNotification(List<S3EventNotificationRecord> records) {
        this.records = records;
    }

    public List<S3EventNotificationRecord> getRecords() {
        return records;
    }

    /**
     * Converts a json representation of the notification message to an instance of S3EventNotification. Any missing fields
     * of the json will be null in the resulting object.
     * Any extra fields will be ignored.
     * @param json the notification message in json format
     * @return an instance of notification message S3EventNotification
     */
    public static S3EventNotification fromJson(String json) {
        return S3EventNotificationReader.create().read(json);
    }

    /**
     * Converts a json representation of the notification message to an instance of S3EventNotification. Any missing fields
     * of the json will be null in the resulting object.
     * Any extra fields will be ignored.
     * @param json the notification message in json format
     * @return an instance of notification message S3EventNotification
     */
    public static S3EventNotification fromJson(byte[] json) {
        return S3EventNotificationReader.create().read(json);
    }

    /**
     * Converts a json representation of the notification message to an instance of S3EventNotification. Any missing fields
     * of the json will be null in the resulting object.
     * Any extra fields will be ignored.
     * @param json the notification message in json format
     * @return an instance of notification message S3EventNotification
     * @see S3EventNotification#fromInputStream
     */
    public S3EventNotification fromJson(InputStream json) {
        return S3EventNotificationReader.create().read(json);
    }

    /**
     * Converts a json representation of the notification message to an instance of S3EventNotification. Any missing fields
     * of the json will be null in the resulting object.
     * Any extra fields will be ignored.
     * @param json the notification message in json format
     * @return an instance of notification message S3EventNotification
     */
    public static S3EventNotification fromInputStream(InputStream json) {
        return S3EventNotificationReader.create().read(json);
    }

    /**
     * Serialize this instance to json format. {@link GlacierEventData}, {@link ReplicationEventData},
     * {@link IntelligentTieringEventData} and {@link LifecycleEventData} keys
     * will be excluded from the json if {@code null}. Any other null fields of the object will be serialized as
     * json {@code null}.
     * @return the json representation of this class.
     */
    public String toJson() {
        return S3EventNotificationWriter.create().writeToString(this);
    }

    /**
     * Serialize this instance to json format, with new line and correct indentation levels. {@link GlacierEventData},
     * {@link ReplicationEventData},
     * {@link IntelligentTieringEventData} and {@link LifecycleEventData} keys
     * will be excluded from the json if {@code null}. Any other null fields of the object will be serialized as
     * json {@code null}.
     * @return the json representation of this class.
     */
    public String toJsonPretty() {
        S3EventNotificationWriter writer = S3EventNotificationWriter.builder()
                                                                    .prettyPrint(true)
                                                                    .build();
        return writer.writeToString(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        S3EventNotification that = (S3EventNotification) o;

        return Objects.equals(records, that.records);
    }

    @Override
    public int hashCode() {
        return records != null ? records.hashCode() : 0;
    }

    @Override
    public String toString() {
        return ToString.builder("S3EventNotification")
                       .add("records", records)
                       .build();
    }
}
