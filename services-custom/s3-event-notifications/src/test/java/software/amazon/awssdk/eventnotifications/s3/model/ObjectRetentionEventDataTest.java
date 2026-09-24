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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import org.junit.jupiter.api.Test;

class ObjectRetentionEventDataTest {

    @Test
    void readEvent_withObjectRetentionEventData_allFieldsPopulated() {
        String json = "{\n"
                      + "  \"Records\": [{\n"
                      + "    \"eventVersion\": \"2.5\",\n"
                      + "    \"eventSource\": \"aws:s3\",\n"
                      + "    \"awsRegion\": \"us-east-1\",\n"
                      + "    \"eventTime\": \"2024-06-15T12:00:00.000Z\",\n"
                      + "    \"eventName\": \"ObjectRetention:Put\",\n"
                      + "    \"userIdentity\": { \"principalId\": \"EXAMPLE\" },\n"
                      + "    \"requestParameters\": { \"sourceIPAddress\": \"127.0.0.1\" },\n"
                      + "    \"responseElements\": { \"x-amz-request-id\": \"REQ1\", \"x-amz-id-2\": \"ID2\" },\n"
                      + "    \"s3\": {\n"
                      + "      \"s3SchemaVersion\": \"1.0\",\n"
                      + "      \"configurationId\": \"retentionConfig\",\n"
                      + "      \"bucket\": {\n"
                      + "        \"name\": \"my-bucket\",\n"
                      + "        \"ownerIdentity\": { \"principalId\": \"OWNER\" },\n"
                      + "        \"arn\": \"arn:aws:s3:::my-bucket\"\n"
                      + "      },\n"
                      + "      \"object\": {\n"
                      + "        \"key\": \"my-object.txt\",\n"
                      + "        \"size\": 512,\n"
                      + "        \"eTag\": \"abc123\",\n"
                      + "        \"versionId\": \"v1\",\n"
                      + "        \"sequencer\": \"SEQ1\"\n"
                      + "      }\n"
                      + "    },\n"
                      + "    \"objectRetentionEventData\": {\n"
                      + "      \"mode\": \"COMPLIANCE\",\n"
                      + "      \"retainUntilDate\": \"2025-06-15T12:00:00.000Z\",\n"
                      + "      \"eventHold\": \"ON\",\n"
                      + "      \"eventHoldDuration\": { \"days\": 90 }\n"
                      + "    }\n"
                      + "  }]\n"
                      + "}";

        S3EventNotification event = S3EventNotification.fromJson(json);

        assertThat(event.getRecords()).hasSize(1);
        S3EventNotificationRecord rec = event.getRecords().get(0);

        ObjectRetentionEventData retention = rec.getObjectRetentionEventData();
        assertThat(retention).isNotNull();
        assertThat(retention.getMode()).isEqualTo("COMPLIANCE");
        assertThat(retention.getRetainUntilDate()).isEqualTo("2025-06-15T12:00:00.000Z");
        assertThat(retention.getEventHold()).isEqualTo("ON");

        EventHoldDuration duration = retention.getEventHoldDuration();
        assertThat(duration).isNotNull();
        assertThat(duration.getUnit()).isEqualTo("days");
        assertThat(duration.getValue()).isEqualTo(90L);
    }

    @Test
    void readEvent_withObjectRetentionEventData_yearsUnit() {
        String json = "{\n"
                      + "  \"Records\": [{\n"
                      + "    \"eventVersion\": \"2.5\",\n"
                      + "    \"eventSource\": \"aws:s3\",\n"
                      + "    \"awsRegion\": \"eu-west-1\",\n"
                      + "    \"eventTime\": \"2024-06-15T12:00:00.000Z\",\n"
                      + "    \"eventName\": \"ObjectRetention:Put\",\n"
                      + "    \"userIdentity\": { \"principalId\": \"EXAMPLE\" },\n"
                      + "    \"requestParameters\": { \"sourceIPAddress\": \"10.0.0.1\" },\n"
                      + "    \"responseElements\": { \"x-amz-request-id\": \"REQ2\", \"x-amz-id-2\": \"ID2\" },\n"
                      + "    \"s3\": {\n"
                      + "      \"s3SchemaVersion\": \"1.0\",\n"
                      + "      \"configurationId\": \"retentionConfig\",\n"
                      + "      \"bucket\": {\n"
                      + "        \"name\": \"my-bucket\",\n"
                      + "        \"ownerIdentity\": { \"principalId\": \"OWNER\" },\n"
                      + "        \"arn\": \"arn:aws:s3:::my-bucket\"\n"
                      + "      },\n"
                      + "      \"object\": {\n"
                      + "        \"key\": \"doc.pdf\",\n"
                      + "        \"size\": 2048,\n"
                      + "        \"eTag\": \"def456\",\n"
                      + "        \"versionId\": \"v2\",\n"
                      + "        \"sequencer\": \"SEQ2\"\n"
                      + "      }\n"
                      + "    },\n"
                      + "    \"objectRetentionEventData\": {\n"
                      + "      \"mode\": \"GOVERNANCE\",\n"
                      + "      \"retainUntilDate\": \"2027-06-15T12:00:00.000Z\",\n"
                      + "      \"eventHold\": \"ON\",\n"
                      + "      \"eventHoldDuration\": { \"years\": 3 }\n"
                      + "    }\n"
                      + "  }]\n"
                      + "}";

        S3EventNotification event = S3EventNotification.fromJson(json);
        S3EventNotificationRecord rec = event.getRecords().get(0);

        EventHoldDuration duration = rec.getObjectRetentionEventData().getEventHoldDuration();
        assertThat(duration).isNotNull();
        assertThat(duration.getUnit()).isEqualTo("years");
        assertThat(duration.getValue()).isEqualTo(3L);
    }

    @Test
    void readEvent_withObjectRetentionEventData_fixedRetention_noEventHoldFields() {
        String json = "{\n"
                      + "  \"Records\": [{\n"
                      + "    \"eventVersion\": \"2.5\",\n"
                      + "    \"eventSource\": \"aws:s3\",\n"
                      + "    \"awsRegion\": \"us-west-2\",\n"
                      + "    \"eventTime\": \"2024-06-15T12:00:00.000Z\",\n"
                      + "    \"eventName\": \"ObjectRetention:Put\",\n"
                      + "    \"userIdentity\": { \"principalId\": \"EXAMPLE\" },\n"
                      + "    \"requestParameters\": { \"sourceIPAddress\": \"127.0.0.1\" },\n"
                      + "    \"responseElements\": { \"x-amz-request-id\": \"REQ3\", \"x-amz-id-2\": \"ID3\" },\n"
                      + "    \"s3\": {\n"
                      + "      \"s3SchemaVersion\": \"1.0\",\n"
                      + "      \"configurationId\": \"retentionConfig\",\n"
                      + "      \"bucket\": {\n"
                      + "        \"name\": \"my-bucket\",\n"
                      + "        \"ownerIdentity\": { \"principalId\": \"OWNER\" },\n"
                      + "        \"arn\": \"arn:aws:s3:::my-bucket\"\n"
                      + "      },\n"
                      + "      \"object\": {\n"
                      + "        \"key\": \"report.csv\",\n"
                      + "        \"size\": 100,\n"
                      + "        \"eTag\": \"ghi789\",\n"
                      + "        \"versionId\": \"v3\",\n"
                      + "        \"sequencer\": \"SEQ3\"\n"
                      + "      }\n"
                      + "    },\n"
                      + "    \"objectRetentionEventData\": {\n"
                      + "      \"mode\": \"COMPLIANCE\",\n"
                      + "      \"retainUntilDate\": \"2030-01-01T00:00:00.000Z\"\n"
                      + "    }\n"
                      + "  }]\n"
                      + "}";

        S3EventNotification event = S3EventNotification.fromJson(json);
        S3EventNotificationRecord rec = event.getRecords().get(0);

        ObjectRetentionEventData retention = rec.getObjectRetentionEventData();
        assertThat(retention).isNotNull();
        assertThat(retention.getMode()).isEqualTo("COMPLIANCE");
        assertThat(retention.getRetainUntilDate()).isEqualTo("2030-01-01T00:00:00.000Z");
        assertThat(retention.getEventHold()).isNull();
        assertThat(retention.getEventHoldDuration()).isNull();
    }

    @Test
    void readEvent_withoutObjectRetentionEventData_fieldIsNull() {
        String json = "{\n"
                      + "  \"Records\": [{\n"
                      + "    \"eventVersion\": \"2.5\",\n"
                      + "    \"eventSource\": \"aws:s3\",\n"
                      + "    \"awsRegion\": \"us-west-2\",\n"
                      + "    \"eventTime\": \"1970-01-01T00:00:00.000Z\",\n"
                      + "    \"eventName\": \"ObjectCreated:Put\",\n"
                      + "    \"userIdentity\": { \"principalId\": \"EXAMPLE\" },\n"
                      + "    \"requestParameters\": { \"sourceIPAddress\": \"127.0.0.1\" },\n"
                      + "    \"responseElements\": { \"x-amz-request-id\": \"REQ\", \"x-amz-id-2\": \"ID\" },\n"
                      + "    \"s3\": {\n"
                      + "      \"s3SchemaVersion\": \"1.0\",\n"
                      + "      \"configurationId\": \"testConfig\",\n"
                      + "      \"bucket\": {\n"
                      + "        \"name\": \"mybucket\",\n"
                      + "        \"ownerIdentity\": { \"principalId\": \"OWNER\" },\n"
                      + "        \"arn\": \"arn:aws:s3:::mybucket\"\n"
                      + "      },\n"
                      + "      \"object\": {\n"
                      + "        \"key\": \"test.txt\",\n"
                      + "        \"size\": 1024,\n"
                      + "        \"eTag\": \"etag\",\n"
                      + "        \"versionId\": \"vid\",\n"
                      + "        \"sequencer\": \"seq\"\n"
                      + "      }\n"
                      + "    }\n"
                      + "  }]\n"
                      + "}";

        S3EventNotification event = S3EventNotification.fromJson(json);
        S3EventNotificationRecord rec = event.getRecords().get(0);
        assertThat(rec.getObjectRetentionEventData()).isNull();
    }

    @Test
    void writeEvent_withObjectRetentionEventData_roundTrips() {
        ObjectRetentionEventData retentionData = new ObjectRetentionEventData(
            "COMPLIANCE", "2025-06-15T12:00:00.000Z", "ON", new EventHoldDuration("days", 90L));

        S3EventNotificationRecord record = new S3EventNotificationRecord(
            "us-east-1",
            "ObjectRetention:Put",
            "aws:s3",
            "2024-06-15T12:00:00.000Z",
            "2.5",
            new RequestParameters("127.0.0.1"),
            new ResponseElements("ID2", "REQ1"),
            new S3(
                "retentionConfig",
                new S3Bucket("my-bucket", new UserIdentity("OWNER"), "arn:aws:s3:::my-bucket"),
                new S3Object("my-object.txt", 512L, "abc123", "v1", "SEQ1"),
                "1.0"),
            new UserIdentity("EXAMPLE"));
        record.setObjectRetentionEventData(retentionData);

        S3EventNotification event = new S3EventNotification(Collections.singletonList(record));
        String json = event.toJson();

        assertThat(json).contains("\"objectRetentionEventData\"");
        assertThat(json).contains("\"mode\":\"COMPLIANCE\"");
        assertThat(json).contains("\"retainUntilDate\":\"2025-06-15T12:00:00.000Z\"");
        assertThat(json).contains("\"eventHold\":\"ON\"");
        assertThat(json).contains("\"eventHoldDuration\":{\"days\":90}");

        // Round trip
        S3EventNotification parsed = S3EventNotification.fromJson(json);
        assertThat(parsed.getRecords().get(0).getObjectRetentionEventData()).isEqualTo(retentionData);
    }

    @Test
    void writeEvent_withObjectRetentionEventData_noEventHoldDuration_roundTrips() {
        ObjectRetentionEventData retentionData = new ObjectRetentionEventData(
            "GOVERNANCE", "2030-01-01T00:00:00.000Z", null, null);

        S3EventNotificationRecord record = new S3EventNotificationRecord(
            "us-west-2",
            "ObjectRetention:Put",
            "aws:s3",
            "2024-06-15T12:00:00.000Z",
            "2.5",
            new RequestParameters("10.0.0.1"),
            new ResponseElements("ID3", "REQ3"),
            new S3(
                "retentionConfig",
                new S3Bucket("my-bucket", new UserIdentity("OWNER"), "arn:aws:s3:::my-bucket"),
                new S3Object("report.csv", 100L, "ghi789", "v3", "SEQ3"),
                "1.0"),
            new UserIdentity("EXAMPLE"));
        record.setObjectRetentionEventData(retentionData);

        S3EventNotification event = new S3EventNotification(Collections.singletonList(record));
        String json = event.toJson();

        assertThat(json).contains("\"objectRetentionEventData\"");
        assertThat(json).contains("\"mode\":\"GOVERNANCE\"");
        assertThat(json).doesNotContain("\"eventHold\"");
        assertThat(json).doesNotContain("\"eventHoldDuration\"");

        // Round trip
        S3EventNotification parsed = S3EventNotification.fromJson(json);
        ObjectRetentionEventData parsedRetention = parsed.getRecords().get(0).getObjectRetentionEventData();
        assertThat(parsedRetention).isEqualTo(retentionData);
        assertThat(parsedRetention.getEventHold()).isNull();
        assertThat(parsedRetention.getEventHoldDuration()).isNull();
    }

    @Test
    void writeEvent_withoutObjectRetentionEventData_fieldOmitted() {
        S3EventNotification event = new S3EventNotification(
            Collections.singletonList(new S3EventNotificationRecord(
                "us-west-2",
                "ObjectCreated:Put",
                "aws:s3",
                "1970-01-01T00:00:00.000Z",
                "2.5",
                new RequestParameters("127.0.0.1"),
                new ResponseElements("ID", "REQ"),
                new S3(
                    "testConfig",
                    new S3Bucket("mybucket", new UserIdentity("OWNER"), "arn:aws:s3:::mybucket"),
                    new S3Object("test.txt", 1024L, "etag", "vid", "seq"),
                    "1.0"),
                new UserIdentity("EXAMPLE"))));

        String json = event.toJson();
        assertThat(json).doesNotContain("objectRetentionEventData");
    }

    @Test
    void equalsAndHashCode_sameValues_areEqual() {
        EventHoldDuration duration1 = new EventHoldDuration("days", 90L);
        EventHoldDuration duration2 = new EventHoldDuration("days", 90L);
        assertThat(duration1).isEqualTo(duration2);
        assertThat(duration1.hashCode()).isEqualTo(duration2.hashCode());

        ObjectRetentionEventData data1 = new ObjectRetentionEventData("COMPLIANCE", "2025-01-01T00:00:00Z", "ON", duration1);
        ObjectRetentionEventData data2 = new ObjectRetentionEventData("COMPLIANCE", "2025-01-01T00:00:00Z", "ON", duration2);
        assertThat(data1).isEqualTo(data2);
        assertThat(data1.hashCode()).isEqualTo(data2.hashCode());
    }

    @Test
    void equalsAndHashCode_differentValues_areNotEqual() {
        EventHoldDuration durationDays = new EventHoldDuration("days", 90L);
        EventHoldDuration durationYears = new EventHoldDuration("years", 1L);
        assertThat(durationDays).isNotEqualTo(durationYears);

        ObjectRetentionEventData compliance = new ObjectRetentionEventData(
            "COMPLIANCE", "2025-01-01T00:00:00Z", "ON", durationDays);
        ObjectRetentionEventData governance = new ObjectRetentionEventData(
            "GOVERNANCE", "2025-01-01T00:00:00Z", "ON", durationDays);
        assertThat(compliance).isNotEqualTo(governance);
    }

    @Test
    void equalsAndHashCode_nullFields() {
        ObjectRetentionEventData allNull = new ObjectRetentionEventData(null, null, null, null);
        ObjectRetentionEventData anotherAllNull = new ObjectRetentionEventData(null, null, null, null);
        assertThat(allNull).isEqualTo(anotherAllNull);
        assertThat(allNull.hashCode()).isEqualTo(anotherAllNull.hashCode());

        EventHoldDuration nullDuration = new EventHoldDuration(null, null);
        EventHoldDuration anotherNullDuration = new EventHoldDuration(null, null);
        assertThat(nullDuration).isEqualTo(anotherNullDuration);
        assertThat(nullDuration.hashCode()).isEqualTo(anotherNullDuration.hashCode());
    }

    @Test
    void toString_containsAllFields() {
        EventHoldDuration duration = new EventHoldDuration("days", 90L);
        assertThat(duration.toString()).contains("days");
        assertThat(duration.toString()).contains("90");

        ObjectRetentionEventData data = new ObjectRetentionEventData(
            "COMPLIANCE", "2025-01-01T00:00:00Z", "ON", duration);
        assertThat(data.toString()).contains("COMPLIANCE");
        assertThat(data.toString()).contains("2025-01-01T00:00:00Z");
        assertThat(data.toString()).contains("ON");
        assertThat(data.toString()).contains("EventHoldDuration");
    }

    @Test
    void prettyPrint_withObjectRetentionEventData_formatsCorrectly() {
        ObjectRetentionEventData retentionData = new ObjectRetentionEventData(
            "COMPLIANCE", "2025-06-15T12:00:00.000Z", "ON", new EventHoldDuration("days", 90L));

        S3EventNotificationRecord record = new S3EventNotificationRecord(
            "us-east-1",
            "ObjectRetention:Put",
            "aws:s3",
            "2024-06-15T12:00:00.000Z",
            "2.5",
            new RequestParameters("127.0.0.1"),
            new ResponseElements("ID2", "REQ1"),
            new S3(
                "retentionConfig",
                new S3Bucket("my-bucket", new UserIdentity("OWNER"), "arn:aws:s3:::my-bucket"),
                new S3Object("my-object.txt", 512L, "abc123", "v1", "SEQ1"),
                "1.0"),
            new UserIdentity("EXAMPLE"));
        record.setObjectRetentionEventData(retentionData);

        S3EventNotification event = new S3EventNotification(Collections.singletonList(record));
        String prettyJson = event.toJsonPretty();

        assertThat(prettyJson).contains("\"objectRetentionEventData\"");
        assertThat(prettyJson).contains("\"mode\" : \"COMPLIANCE\"");
        assertThat(prettyJson).contains("\"retainUntilDate\" : \"2025-06-15T12:00:00.000Z\"");
        assertThat(prettyJson).contains("\"eventHold\" : \"ON\"");
        assertThat(prettyJson).contains("\"eventHoldDuration\"");
        assertThat(prettyJson).contains("\"days\" : 90");

        // Round trip from pretty JSON
        S3EventNotification parsed = S3EventNotification.fromJson(prettyJson);
        assertThat(parsed.getRecords().get(0).getObjectRetentionEventData()).isEqualTo(retentionData);
    }
}
