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

import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.eventnotifications.s3.model.GlacierEventData;
import software.amazon.awssdk.eventnotifications.s3.model.IntelligentTieringEventData;
import software.amazon.awssdk.eventnotifications.s3.model.LifecycleEventData;
import software.amazon.awssdk.eventnotifications.s3.model.ReplicationEventData;
import software.amazon.awssdk.eventnotifications.s3.model.RequestParameters;
import software.amazon.awssdk.eventnotifications.s3.model.ResponseElements;
import software.amazon.awssdk.eventnotifications.s3.model.RestoreEventData;
import software.amazon.awssdk.eventnotifications.s3.model.S3;
import software.amazon.awssdk.eventnotifications.s3.model.S3Bucket;
import software.amazon.awssdk.eventnotifications.s3.model.S3EventNotification;
import software.amazon.awssdk.eventnotifications.s3.model.S3EventNotificationRecord;
import software.amazon.awssdk.eventnotifications.s3.model.S3Object;
import software.amazon.awssdk.eventnotifications.s3.model.TransitionEventData;
import software.amazon.awssdk.eventnotifications.s3.model.UserIdentity;

class S3EventNotificationWriterTest {

    @Test
    void testPrettyPrint_requiredFieldOnly() {
        S3EventNotification event = new S3EventNotification(
            Arrays.asList(new S3EventNotificationRecord(
                "us-west-2",
                "ObjectCreated:Get",
                "aws:s3",
                "1970-01-01T01:01:01.001Z",
                "2.1",
                new RequestParameters("127.1.2.3"),
                new ResponseElements(
                    "FMyUVURIY8/IgAtTv8xRjskZQpcIZ9KG4V5Wp6S7S/JRWeUWerMUE5JgHvANxid2", "C3D13FE58DE4CRID"),
                new S3(
                    "testConfigRule",
                    new S3Bucket(
                        "mybucket-test",
                        new UserIdentity("A3NL1KOZZKExample"),
                        "arn:aws:s3:::mybucket"),
                    new S3Object(
                        "HappyFace-test.jpg",
                        2048L,
                        "d41d8cd98f00b204e9800998ecf8etag",
                        "096fKKXTRTtl3on89fVO.nfljtsv6vid",
                        "0055AED6DCD9028SEQ"),
                    "1.0"
                ),
                new UserIdentity("AIDAJDPLRKLG7UEXAMUID")
            ))
        );

        String expected = "{\n"
                           + "  \"Records\" : [ {\n"
                           + "    \"eventVersion\" : \"2.1\",\n"
                           + "    \"eventSource\" : \"aws:s3\",\n"
                           + "    \"awsRegion\" : \"us-west-2\",\n"
                           + "    \"eventTime\" : \"1970-01-01T01:01:01.001Z\",\n"
                           + "    \"eventName\" : \"ObjectCreated:Get\",\n"
                           + "    \"userIdentity\" : {\n"
                           + "      \"principalId\" : \"AIDAJDPLRKLG7UEXAMUID\"\n"
                           + "    },\n"
                           + "    \"requestParameters\" : {\n"
                           + "      \"sourceIPAddress\" : \"127.1.2.3\"\n"
                           + "    },\n"
                           + "    \"responseElements\" : {\n"
                           + "      \"x-amz-request-id\" : \"C3D13FE58DE4CRID\",\n"
                           + "      \"x-amz-id-2\" : \"FMyUVURIY8/IgAtTv8xRjskZQpcIZ9KG4V5Wp6S7S/JRWeUWerMUE5JgHvANxid2\"\n"
                           + "    },\n"
                           + "    \"s3\" : {\n"
                           + "      \"s3SchemaVersion\" : \"1.0\",\n"
                           + "      \"configurationId\" : \"testConfigRule\",\n"
                           + "      \"bucket\" : {\n"
                           + "        \"name\" : \"mybucket-test\",\n"
                           + "        \"ownerIdentity\" : {\n"
                           + "          \"principalId\" : \"A3NL1KOZZKExample\"\n"
                           + "        },\n"
                           + "        \"arn\" : \"arn:aws:s3:::mybucket\"\n"
                           + "      },\n"
                           + "      \"object\" : {\n"
                           + "        \"key\" : \"HappyFace-test.jpg\",\n"
                           + "        \"size\" : 2048,\n"
                           + "        \"eTag\" : \"d41d8cd98f00b204e9800998ecf8etag\",\n"
                           + "        \"versionId\" : \"096fKKXTRTtl3on89fVO.nfljtsv6vid\",\n"
                           + "        \"sequencer\" : \"0055AED6DCD9028SEQ\"\n"
                           + "      }\n"
                           + "    }\n"
                           + "  } ]\n"
                           + "}";
        assertThat(event.toJsonPretty()).isEqualTo(expected);
    }

    @Test
    void testToJson_requiredFielsdOnly() {
        String expected = "{\"Records\":[{\"eventVersion\":\"2.1\",\"eventSource\":\"aws:s3\",\"awsRegion\":\"us-west-2\","
                          + "\"eventTime\":\"1970-01-01T01:01:01.001Z\",\"eventName\":\"ObjectCreated:Get\","
                          + "\"userIdentity\":{\"principalId\""
                          + ":\"AIDAJDPLRKLG7UEXAMUID\"},\"requestParameters\":{\"sourceIPAddress\":\"127.1.2.3\"},"
                          + "\"responseElements\":{\"x-amz-request-id\":\"C3D13FE58DE4CRID\",\"x-amz-id-2\":"
                          + "\"FMyUVURIY8/IgAtTv8xRjskZQpcIZ9KG4V5Wp6S7S/JRWeUWerMUE5JgHvANxid2\"},"
                          + "\"s3\":{\"s3SchemaVersion\":\"1.0\",\"configurationId\":\"testConfigRule\",\"bucket\":"
                          + "{\"name\":\"mybucket-test\",\"ownerIdentity\":{\"principalId\":\"A3NL1KOZZKExample\"},"
                          + "\"arn\":\"arn:aws:s3:::mybucket\"},\"object\":{\"key\":\"HappyFace-test.jpg\",\"size\":2048,"
                          + "\"eTag\":\"d41d8cd98f00b204e9800998ecf8etag\",\"versionId\":\"096fKKXTRTtl3on89fVO.nfljtsv6vid\","
                          + "\"sequencer\":\"0055AED6DCD9028SEQ\"}}}]}";

        S3EventNotification event = new S3EventNotification(
            Arrays.asList(new S3EventNotificationRecord(
                "us-west-2",
                "ObjectCreated:Get",
                "aws:s3",
                "1970-01-01T01:01:01.001Z",
                "2.1",
                new RequestParameters("127.1.2.3"),
                new ResponseElements(
                    "FMyUVURIY8/IgAtTv8xRjskZQpcIZ9KG4V5Wp6S7S/JRWeUWerMUE5JgHvANxid2", "C3D13FE58DE4CRID"),
                new S3(
                    "testConfigRule",
                    new S3Bucket(
                        "mybucket-test",
                        new UserIdentity("A3NL1KOZZKExample"),
                        "arn:aws:s3:::mybucket"),
                    new S3Object(
                        "HappyFace-test.jpg",
                        2048L,
                        "d41d8cd98f00b204e9800998ecf8etag",
                        "096fKKXTRTtl3on89fVO.nfljtsv6vid",
                        "0055AED6DCD9028SEQ"),
                    "1.0"
                ),
                new UserIdentity("AIDAJDPLRKLG7UEXAMUID")
            ))
        );
        assertThat(event.toJson()).isEqualTo(expected);
    }

    @Test
    void testPrettyPrint_allFields() {
        String expected = "{\n"
                          + "  \"Records\" : [ {\n"
                          + "    \"eventVersion\" : \"2.1\",\n"
                          + "    \"eventSource\" : \"aws:s3\",\n"
                          + "    \"awsRegion\" : \"us-west-2\",\n"
                          + "    \"eventTime\" : \"1970-01-01T01:01:01.001Z\",\n"
                          + "    \"eventName\" : \"ObjectCreated:Put\",\n"
                          + "    \"userIdentity\" : {\n"
                          + "      \"principalId\" : \"AIDAJDPLRKLG7UEXAMPLE\"\n"
                          + "    },\n"
                          + "    \"requestParameters\" : {\n"
                          + "      \"sourceIPAddress\" : \"127.0.0.1\"\n"
                          + "    },\n"
                          + "    \"responseElements\" : {\n"
                          + "      \"x-amz-request-id\" : \"C3D13FE58DE4C810\",\n"
                          + "      \"x-amz-id-2\" : \"FMyUVURIY8/IgAtTv8xRjskZQpcIZ9KG4V5Wp6S7S/JRWeUWerMUE5JgHvANOjpD\"\n"
                          + "    },\n"
                          + "    \"s3\" : {\n"
                          + "      \"s3SchemaVersion\" : \"1.0\",\n"
                          + "      \"configurationId\" : \"testConfigRule\",\n"
                          + "      \"bucket\" : {\n"
                          + "        \"name\" : \"mybucket\",\n"
                          + "        \"ownerIdentity\" : {\n"
                          + "          \"principalId\" : \"A3NL1KOZZKExample\"\n"
                          + "        },\n"
                          + "        \"arn\" : \"arn:aws:s3:::mybucket\"\n"
                          + "      },\n"
                          + "      \"object\" : {\n"
                          + "        \"key\" : \"HappyFace.jpg\",\n"
                          + "        \"size\" : 1024,\n"
                          + "        \"eTag\" : \"d41d8cd98f00b204e9800998ecf8427e\",\n"
                          + "        \"versionId\" : \"096fKKXTRTtl3on89fVO.nfljtsv6qko\",\n"
                          + "        \"sequencer\" : \"0055AED6DCD90281E5\"\n"
                          + "      }\n"
                          + "    },\n"
                          + "    \"glacierEventData\" : {\n"
                          + "      \"restoreEventData\" : {\n"
                          + "        \"lifecycleRestorationExpiryTime\" : \"1971-02-02T01:01:01.001Z\",\n"
                          + "        \"lifecycleRestoreStorageClass\" : \"testStorageClass\"\n"
                          + "      }\n"
                          + "    },\n"
                          + "    \"replicationEventData\" : {\n"
                          + "      \"replicationRuleId\" : \"replicationRuleIdTest\",\n"
                          + "      \"destinationBucket\" : \"destinationBucketTest\",\n"
                          + "      \"s3Operation\" : \"s3OperationTest\",\n"
                          + "      \"requestTime\" : \"requestTimeTest\",\n"
                          + "      \"failureReason\" : \"failureReasonTest\",\n"
                          + "      \"threshold\" : \"thresholdTest\",\n"
                          + "      \"replicationTime\" : \"replicationTimeTest\"\n"
                          + "    },\n"
                          + "    \"intelligentTieringEventData\" : {\n"
                          + "      \"destinationAccessTier\" : \"destinationAccessTierTest\"\n"
                          + "    },\n"
                          + "    \"lifecycleEventData\" : {\n"
                          + "      \"transitionEventData\" : {\n"
                          + "        \"destinationStorageClass\" : \"destinationStorageClassTest\"\n"
                          + "      }\n"
                          + "    }\n"
                          + "  } ]\n"
                          + "}";
        S3EventNotification event = new S3EventNotification(
            Collections.singletonList(new S3EventNotificationRecord(
                "us-west-2",
                "ObjectCreated:Put",
                "aws:s3",
                "1970-01-01T01:01:01.001Z",
                "2.1",
                new RequestParameters("127.0.0.1"),
                new ResponseElements(
                    "FMyUVURIY8/IgAtTv8xRjskZQpcIZ9KG4V5Wp6S7S/JRWeUWerMUE5JgHvANOjpD", "C3D13FE58DE4C810"),
                new S3(
                    "testConfigRule",
                    new S3Bucket(
                        "mybucket",
                        new UserIdentity("A3NL1KOZZKExample"),
                        "arn:aws:s3:::mybucket"),
                    new S3Object(
                        "HappyFace.jpg",
                        1024L,
                        "d41d8cd98f00b204e9800998ecf8427e",
                        "096fKKXTRTtl3on89fVO.nfljtsv6qko",
                        "0055AED6DCD90281E5"),
                    "1.0"
                ),
                new UserIdentity("AIDAJDPLRKLG7UEXAMPLE"),
                new GlacierEventData(new RestoreEventData("1971-02-02T01:01:01.001Z", "testStorageClass")),
                new LifecycleEventData(new TransitionEventData("destinationStorageClassTest")),
                new IntelligentTieringEventData("destinationAccessTierTest"),
                new ReplicationEventData(
                    "replicationRuleIdTest",
                    "destinationBucketTest",
                    "s3OperationTest",
                    "requestTimeTest",
                    "failureReasonTest",
                    "thresholdTest",
                    "replicationTimeTest")))
        );

        assertThat(event.toJsonPretty()).isEqualTo(expected);
    }

    @Test
    void testToJson_allFields() {
        String expected = "{\"Records\":[{\"eventVersion\":\"2.1\",\"eventSource\":\"aws:s3\",\"awsRegion\":"
                          + "\"us-west-2\",\"eventTime\":\"1970-01-01T01:01:01.001Z\",\"eventName\":\"ObjectCreated:Get\","
                          + "\"userIdentity\":{\"principalId\":\"PRINCIPALIDTEST\"},\"requestParameters\":{\"sourceIPAddress\":"
                          + "\"127.1.2.3\"},\"responseElements\":{\"x-amz-request-id\":\"C3D13FE58DE4CRID\",\"x-amz-id-2\":"
                          + "\"FMyUVURIY8/IgAtTv8xRjskZQpcIZ9KG4V5Wp6S7S/JRWeUWerMUE5JgHvANxid2\"},\"s3\":{\"s3SchemaVersion\":"
                          + "\"1.0\",\"configurationId\":\"testConfigRule\",\"bucket\":{\"name\":\"mybucket-test\","
                          + "\"ownerIdentity\":{\"principalId\":\"PRINCIPALIDTESTBUCKET\"},\"arn\":\"arn:aws:s3:::mybucket\"},"
                          + "\"object\":{\"key\":\"HappyFace-test.jpg\",\"size\":2048,\"eTag\":"
                          + "\"d41d8cd98f00b204e9800998ecf8etag\",\"versionId\":\"096fKKXTRTtl3on89fVO.nfljtsv6vid\","
                          + "\"sequencer\":\"0055AED6DCD9028SEQ\"}},\"glacierEventData\":{\"restoreEventData\":"
                          + "{\"lifecycleRestorationExpiryTime\":\"1971-02-02T01:01:01.001Z\",\"lifecycleRestoreStorageClass\":"
                          + "\"testStorageClass\"}},\"replicationEventData\":{\"replicationRuleId\":\"replicationRuleIdTest\","
                          + "\"destinationBucket\":\"destinationBucketTest\",\"s3Operation\":\"s3OperationTest\","
                          + "\"requestTime\":\"requestTimeTest\",\"failureReason\":\"failureReasonTest\",\"threshold\":"
                          + "\"thresholdTest\",\"replicationTime\":\"replicationTimeTest\"},\"intelligentTieringEventData\":"
                          + "{\"destinationAccessTier\":\"destinationAccessTierTest\"},\"lifecycleEventData\":"
                          + "{\"transitionEventData\":{\"destinationStorageClass\":\"destinationStorageClassTest\"}}}]}";

        S3EventNotification event = new S3EventNotification(
            Collections.singletonList(new S3EventNotificationRecord(
                "us-west-2",
                "ObjectCreated:Get",
                "aws:s3",
                "1970-01-01T01:01:01.001Z",
                "2.1",
                new RequestParameters("127.1.2.3"),
                new ResponseElements(
                    "FMyUVURIY8/IgAtTv8xRjskZQpcIZ9KG4V5Wp6S7S/JRWeUWerMUE5JgHvANxid2", "C3D13FE58DE4CRID"),
                new S3(
                    "testConfigRule",
                    new S3Bucket(
                        "mybucket-test",
                        new UserIdentity("PRINCIPALIDTESTBUCKET"),
                        "arn:aws:s3:::mybucket"),
                    new S3Object(
                        "HappyFace-test.jpg",
                        2048L,
                        "d41d8cd98f00b204e9800998ecf8etag",
                        "096fKKXTRTtl3on89fVO.nfljtsv6vid",
                        "0055AED6DCD9028SEQ"),
                    "1.0"
                ),
                new UserIdentity("PRINCIPALIDTEST"),
                new GlacierEventData(new RestoreEventData("1971-02-02T01:01:01.001Z", "testStorageClass")),
                new LifecycleEventData(new TransitionEventData("destinationStorageClassTest")),
                new IntelligentTieringEventData("destinationAccessTierTest"),
                new ReplicationEventData(
                    "replicationRuleIdTest",
                    "destinationBucketTest",
                    "s3OperationTest",
                    "requestTimeTest",
                    "failureReasonTest",
                    "thresholdTest",
                    "replicationTimeTest")))
        );

        assertThat(event.toJson()).isEqualTo(expected);
    }

    @Test
    void nullList() {
        S3EventNotification event = new S3EventNotification(null);
        assertThat(event.toJson()).isEqualTo("{\"Records\":null}");
    }

    @Test
    void emptyList() {
        S3EventNotification event = new S3EventNotification(Collections.emptyList());
        assertThat(event.toJson()).isEqualTo("{\"Records\":[]}");
    }

    @Test
    void nullRecordValue() {
        S3EventNotification event = new S3EventNotification(Collections.singletonList(
            new S3EventNotificationRecord(null, null, null, null, null, null, null, null, null)
        ));
        assertThat(event.toJson()).isEqualTo(
            "{\"Records\":[{"
            + "\"eventVersion\":null,"
            + "\"eventSource\":null,"
            + "\"awsRegion\":null,"
            + "\"eventTime\":null,"
            + "\"eventName\":null,"
            + "\"userIdentity\":null,"
            + "\"requestParameters\":null,"
            + "\"responseElements\":null,"
            + "\"s3\":null}]}");
    }

    @Test
    void nullableNumberFieldsHandledCorrectly() {
        String expected = "{\"Records\":[{\"eventVersion\":null,\"eventSource\":null,\"awsRegion\":null,"
                          + "\"eventTime\":null,\"eventName\":null,\"userIdentity\":null,"
                          + "\"requestParameters\":null,\"responseElements\":null,"
                          + "\"s3\":{\"s3SchemaVersion\":\"1.0\",\"configurationId\":\"testConfigRule\",\"bucket\":null,"
                          + "\"object\":{\"key\":\"HappyFace-test.jpg\",\"size\":null,"
                          + "\"eTag\":\"d41d8cd98f00b204e9800998ecf8etag\",\"versionId\":\"096fKKXTRTtl3on89fVO.nfljtsv6vid\","
                          + "\"sequencer\":null}}}]}";

        S3EventNotification event = new S3EventNotification(Collections.singletonList(
            new S3EventNotificationRecord(null, null, null, null, null, null, null,
                                          new S3(
                                              "testConfigRule",
                                              null,
                                              new S3Object(
                                                  "HappyFace-test.jpg",
                                                  null,
                                                  "d41d8cd98f00b204e9800998ecf8etag",
                                                  "096fKKXTRTtl3on89fVO.nfljtsv6vid",
                                                  null),
                                              "1.0"
                                          ),
                                          null)));

        assertThat(event.toJson()).isEqualTo(expected);
    }

}
