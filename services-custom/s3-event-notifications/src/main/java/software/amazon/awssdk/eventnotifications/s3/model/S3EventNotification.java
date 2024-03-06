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


import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.eventnotifications.s3.internal.S3EventNotificationReader;
import software.amazon.awssdk.eventnotifications.s3.internal.S3EventNotificationWriter;
import software.amazon.awssdk.utils.ToString;

/**
 * A helper class that represents a strongly typed S3 EventNotification item sent to SQS, SNS, or Lambda.
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

    public static S3EventNotification fromJson(String json) {
        return S3EventNotificationReader.create().read(json);
    }


    public static S3EventNotification fromJson(byte[] json) {
        return S3EventNotificationReader.create().read(json);
    }

    public S3EventNotification fromJson(InputStream json) {
        return S3EventNotificationReader.create().read(json);
    }

    public String toJson() {
        return S3EventNotificationWriter.create().writeToString(this);
    }

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
