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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.time.Instant;
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
import software.amazon.awssdk.thirdparty.jackson.core.JsonParseException;

class S3EventNotificationReaderTest {

    @Test
    void fromJson_containsNullValues_shouldSucceed() {
        S3EventNotification eventNotification = new S3EventNotification(
            Collections.singletonList(new S3EventNotificationRecord(
                "us-west-2",
                "ObjectCreated:Put",
                "aws:s3",
                "1970-01-01T00:00:00.000Z",
                "2.1",
                null,
                new ResponseElements(
                    null, null),
                new S3(
                    "testConfigRule",
                    new S3Bucket(
                        "mybucket",
                        new UserIdentity("A3NL1KOZZKExample"),
                        "arn:aws:s3:::mybucket"),
                    new S3Object(
                        "HappyFace.jpg",
                        null,
                        "d41d8cd98f00b204e9800998ecf8427e",
                        null,
                        "0055AED6DCD90281E5"),
                    "1.0"
                ),
                new UserIdentity("AIDAJDPLRKLG7UEXAMPLE"),
                null, null, null, null)
            ));
        String json = eventNotification.toJsonPretty();
        assertThat(json).isEqualTo("{\n"
                                   + "  \"Records\" : [ {\n"
                                   + "    \"eventVersion\" : \"2.1\",\n"
                                   + "    \"eventSource\" : \"aws:s3\",\n"
                                   + "    \"awsRegion\" : \"us-west-2\",\n"
                                   + "    \"eventTime\" : \"1970-01-01T00:00:00Z\",\n"
                                   + "    \"eventName\" : \"ObjectCreated:Put\",\n"
                                   + "    \"userIdentity\" : {\n"
                                   + "      \"principalId\" : \"AIDAJDPLRKLG7UEXAMPLE\"\n"
                                   + "    },\n"
                                   + "    \"requestParameters\" : null,\n"
                                   + "    \"responseElements\" : {\n"
                                   + "      \"x-amz-request-id\" : null,\n"
                                   + "      \"x-amz-id-2\" : null\n"
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
                                   + "        \"size\" : null,\n"
                                   + "        \"eTag\" : \"d41d8cd98f00b204e9800998ecf8427e\",\n"
                                   + "        \"versionId\" : null,\n"
                                   + "        \"sequencer\" : \"0055AED6DCD90281E5\"\n"
                                   + "      }\n"
                                   + "    }\n"
                                   + "  } ]\n"
                                   + "}");
        S3EventNotification actual = S3EventNotification.fromJson(json);
        assertThat(actual).isEqualTo(eventNotification);
    }

    @Test
    void givenEventWithoutOptionalFields_whenReadingJson_expectOnlyRequiredFields() {
        String eventJson = "{  "
                           + "   \"Records\":[  "
                           + "      {  "
                           + "         \"eventVersion\":\"2.1\","
                           + "         \"eventSource\":\"aws:s3\","
                           + "         \"awsRegion\":\"us-west-2\","
                           + "         \"eventTime\":\"1970-01-01T00:00:00.000Z\","
                           + "         \"eventName\":\"ObjectCreated:Put\","
                           + "         \"userIdentity\":{  "
                           + "            \"principalId\":\"AIDAJDPLRKLG7UEXAMPLE\""
                           + "         },"
                           + "         \"requestParameters\":{  "
                           + "            \"sourceIPAddress\":\"127.0.0.1\""
                           + "         },"
                           + "         \"responseElements\":{  "
                           + "            \"x-amz-request-id\":\"C3D13FE58DE4C810\","
                           + "            \"x-amz-id-2\":\"FMyUVURIY8/IgAtTv8xRjskZQpcIZ9KG4V5Wp6S7S/JRWeUWerMUE5JgHvANOjpD\""
                           + "         },"
                           + "         \"s3\":{  "
                           + "            \"s3SchemaVersion\":\"1.0\","
                           + "            \"configurationId\":\"testConfigRule\","
                           + "            \"bucket\":{  "
                           + "               \"name\":\"mybucket\","
                           + "               \"ownerIdentity\":{  "
                           + "                  \"principalId\":\"A3NL1KOZZKExample\""
                           + "               },"
                           + "               \"arn\":\"arn:aws:s3:::mybucket\""
                           + "            },"
                           + "            \"object\":{  "
                           + "               \"key\":\"HappyFace.jpg\","
                           + "               \"size\":1024,"
                           + "               \"eTag\":\"d41d8cd98f00b204e9800998ecf8427e\","
                           + "               \"versionId\":\"096fKKXTRTtl3on89fVO.nfljtsv6qko\","
                           + "               \"sequencer\":\"0055AED6DCD90281E5\""
                           + "            }"
                           + "         }"
                           + "      }"
                           + "   ]"
                           + "}";

        S3EventNotification event = S3EventNotification.fromJson(eventJson);

        assertThat(event.getRecords()).hasSize(1);

        // verify constructors
        S3EventNotification expected = new S3EventNotification(
            Collections.singletonList(new S3EventNotificationRecord(
                "us-west-2",
                "ObjectCreated:Put",
                "aws:s3",
                "1970-01-01T00:00:00.000Z",
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
                new UserIdentity("AIDAJDPLRKLG7UEXAMPLE")
            ))
        );
        assertThat(event).isEqualTo(expected);

        // verify getters
        S3EventNotificationRecord rec = event.getRecords().get(0);
        assertThat(rec).isNotNull();
        assertThat(rec.getAwsRegion()).isEqualTo("us-west-2");
        assertThat(rec.getEventName()).isEqualTo("ObjectCreated:Put");
        assertThat(rec.getEventTime()).isEqualTo(Instant.parse("1970-01-01T00:00:00.000Z"));
        assertThat(rec.getEventVersion()).isEqualTo("2.1");

        UserIdentity userIdentity = rec.getUserIdentity();
        assertThat(userIdentity).isNotNull();
        assertThat(userIdentity.getPrincipalId()).isEqualTo("AIDAJDPLRKLG7UEXAMPLE");

        RequestParameters requestParameters = rec.getRequestParameters();
        assertThat(requestParameters).isNotNull();
        assertThat(requestParameters.getSourceIpAddress()).isEqualTo("127.0.0.1");

        ResponseElements responseElements = rec.getResponseElements();
        assertThat(responseElements).isNotNull();
        assertThat(responseElements.getXAmzRequestId()).isEqualTo("C3D13FE58DE4C810");
        assertThat(responseElements.getXAmzId2())
            .isEqualTo("FMyUVURIY8/IgAtTv8xRjskZQpcIZ9KG4V5Wp6S7S/JRWeUWerMUE5JgHvANOjpD");

        S3 s3 = rec.getS3();
        assertThat(s3).isNotNull();
        assertThat(s3.getS3SchemaVersion()).isEqualTo("1.0");
        assertThat(s3.getConfigurationId()).isEqualTo("testConfigRule");
        S3Bucket s3Bucket = s3.getBucket();
        assertThat(s3Bucket).isNotNull();
        assertThat(s3Bucket.getName()).isEqualTo("mybucket");
        assertThat(s3Bucket.getArn()).isEqualTo("arn:aws:s3:::mybucket");
        UserIdentity ownerIdentity = s3Bucket.getOwnerIdentity();
        assertThat(ownerIdentity).isNotNull();
        assertThat(ownerIdentity.getPrincipalId()).isEqualTo("A3NL1KOZZKExample");
        S3Object s3Object = s3.getObject();
        assertThat(s3Object).isNotNull();
        assertThat(s3Object.getKey()).isEqualTo("HappyFace.jpg");
        assertThat(s3Object.getETag()).isEqualTo("d41d8cd98f00b204e9800998ecf8427e");
        assertThat(s3Object.getSizeAsLong()).isEqualTo(1024L);
        assertThat(s3Object.getVersionId()).isEqualTo("096fKKXTRTtl3on89fVO.nfljtsv6qko");
        assertThat(s3Object.getSequencer()).isEqualTo("0055AED6DCD90281E5");

        assertThat(rec.getGlacierEventData()).isNull();
        assertThat(rec.getIntelligentTieringEventData()).isNull();
        assertThat(rec.getLifecycleEventData()).isNull();
        assertThat(rec.getReplicationEventData()).isNull();
    }

    @Test
    void givenEventContainingOptionalFields_whenReadingJson_expectAllFields() {
        String eventJson = "{\n"
                           + "  \"Records\":[\n"
                           + "    {\n"
                           + "      \"eventVersion\":\"2.1\",\n"
                           + "      \"eventSource\":\"aws:s3\",\n"
                           + "      \"awsRegion\":\"us-west-2\",\n"
                           + "      \"eventTime\":\"1970-01-01T00:00:00.000Z\",\n"
                           + "      \"eventName\":\"ObjectCreated:Put\",\n"
                           + "      \"userIdentity\":{\n"
                           + "        \"principalId\":\"AIDAJDPLRKLG7UEXAMPLE\"\n"
                           + "      },\n"
                           + "      \"requestParameters\":{\n"
                           + "        \"sourceIPAddress\":\"127.0.0.1\"\n"
                           + "      },\n"
                           + "      \"responseElements\":{\n"
                           + "        \"x-amz-request-id\":\"C3D13FE58DE4C810\",\n"
                           + "        \"x-amz-id-2\":\"FMyUVURIY8/IgAtTv8xRjskZQpcIZ9KG4V5Wp6S7S/JRWeUWerMUE5JgHvANOjpD\"\n"
                           + "      },\n"
                           + "      \"s3\":{\n"
                           + "        \"s3SchemaVersion\":\"1.0\",\n"
                           + "        \"configurationId\":\"testConfigRule\",\n"
                           + "        \"bucket\":{\n"
                           + "          \"name\":\"mybucket\",\n"
                           + "          \"ownerIdentity\":{\n"
                           + "            \"principalId\":\"A3NL1KOZZKExample\"\n"
                           + "          },\n"
                           + "          \"arn\":\"arn:aws:s3:::mybucket\"\n"
                           + "        },\n"
                           + "        \"object\":{\n"
                           + "          \"key\":\"HappyFace.jpg\",\n"
                           + "          \"size\":1024,\n"
                           + "          \"eTag\":\"d41d8cd98f00b204e9800998ecf8427e\",\n"
                           + "          \"versionId\":\"096fKKXTRTtl3on89fVO.nfljtsv6qko\",\n"
                           + "          \"sequencer\":\"0055AED6DCD90281E5\"\n"
                           + "        }\n"
                           + "      },\n"
                           + "      \"glacierEventData\": {\n"
                           + "        \"restoreEventData\": {\n"
                           + "          \"lifecycleRestorationExpiryTime\": \"1970-02-02T00:00:00.000Z\",\n"
                           + "          \"lifecycleRestoreStorageClass\": \"testStorageClass\"\n"
                           + "        }\n"
                           + "      },\n"
                           + "      \"replicationEventData\": {\n"
                           + "        \"replicationRuleId\": \"replicationRuleIdTest\",\n"
                           + "        \"destinationBucket\": \"destinationBucketTest\",\n"
                           + "        \"s3Operation\": \"s3OperationTest\",\n"
                           + "        \"requestTime\": \"requestTimeTest\",\n"
                           + "        \"failureReason\": \"failureReasonTest\",\n"
                           + "        \"threshold\": \"thresholdTest\",\n"
                           + "        \"replicationTime\": \"replicationTimeTest\"\n"
                           + "      },\n"
                           + "      \"intelligentTieringEventData\": {\n"
                           + "        \"destinationAccessTier\": \"destinationAccessTierTest\"\n"
                           + "      },\n"
                           + "      \"lifecycleEventData\": {\n"
                           + "        \"transitionEventData\": {\n"
                           + "          \"destinationStorageClass\": \"destinationStorageClassTest\"\n"
                           + "        }\n"
                           + "      }\n"
                           + "    }\n"
                           + "  ]\n"
                           + "}\n";

        S3EventNotification event = S3EventNotification.fromJson(eventJson);

        assertThat(event.getRecords()).hasSize(1);

        // verify constructors
        S3EventNotification expected = new S3EventNotification(
            Collections.singletonList(new S3EventNotificationRecord(
                "us-west-2",
                "ObjectCreated:Put",
                "aws:s3",
                "1970-01-01T00:00:00.000Z",
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
                new GlacierEventData(new RestoreEventData("1970-02-02T00:00:00.000Z", "testStorageClass")),
                new LifecycleEventData(new TransitionEventData("destinationStorageClassTest")),
                new IntelligentTieringEventData("destinationAccessTierTest"),
                new ReplicationEventData(
                    "replicationRuleIdTest",
                    "destinationBucketTest",
                    "s3OperationTest",
                    "requestTimeTest",
                    "failureReasonTest",
                    "thresholdTest",
                    "replicationTimeTest")
            ))
        );
        assertThat(event).isEqualTo(expected);

        S3EventNotificationRecord rec = event.getRecords().get(0);
        assertThat(rec).isNotNull();

        GlacierEventData glacierEventData = rec.getGlacierEventData();
        assertThat(glacierEventData).isNotNull();
        RestoreEventData restoreEvent = glacierEventData.getRestoreEventData();
        assertThat(restoreEvent).isNotNull();
        assertThat(restoreEvent.getLifecycleRestorationExpiryTime()).isEqualTo(Instant.parse("1970-02-02T00:00:00.000Z"));
        assertThat(restoreEvent.getLifecycleRestoreStorageClass()).isEqualTo("testStorageClass");

        LifecycleEventData lifecycle = rec.getLifecycleEventData();
        assertThat(lifecycle).isNotNull();
        TransitionEventData transitionEvent = lifecycle.getTransitionEventData();
        assertThat(transitionEvent).isNotNull();
        assertThat(transitionEvent.getDestinationStorageClass()).isEqualTo("destinationStorageClassTest");

        IntelligentTieringEventData tieringEventData = rec.getIntelligentTieringEventData();
        assertThat(tieringEventData).isNotNull();
        assertThat(tieringEventData.getDestinationAccessTier()).isEqualTo("destinationAccessTierTest");

        ReplicationEventData replication = rec.getReplicationEventData();
        assertThat(replication).isNotNull();
        assertThat(replication.getReplicationRuleId()).isEqualTo("replicationRuleIdTest");
        assertThat(replication.getReplicationTime()).isEqualTo("replicationTimeTest");
        assertThat(replication.getDestinationBucket()).isEqualTo("destinationBucketTest");
        assertThat(replication.getRequestTime()).isEqualTo("requestTimeTest");
        assertThat(replication.getFailureReason()).isEqualTo("failureReasonTest");
        assertThat(replication.getThreshold()).isEqualTo("thresholdTest");
        assertThat(replication.getS3Operation()).isEqualTo("s3OperationTest");
    }

    @Test
    void emptyJson_shouldContainsNullRecords() {
        String json = "{}";
        S3EventNotification event = S3EventNotification.fromJson(json);
        assertThat(event).isNotNull();
        assertThat(event.getRecords()).isNull();
    }

    @Test
    void nullRecords_shouldContainNullRecords() {
        String json = "{\"Records\":null}";
        S3EventNotification event = S3EventNotification.fromJson(json);
        assertThat(event).isNotNull();
        assertThat(event.getRecords()).isNull();
    }

    @Test
    void emptyRecordList_shouldContainEmptyRecordList() {
        String json = "{\"Records\":[]}";
        S3EventNotification event = S3EventNotification.fromJson(json);
        assertThat(event).isNotNull();
        assertThat(event.getRecords()).isEmpty();
    }

    @Test
    void missingField_shouldBeNull() {
        String json = "{\n"
                      + "  \"Records\" : [ {\n"
                      + "    \"eventVersion\" : \"2.1\",\n"
                      + "    \"eventSource\" : \"aws:s3\",\n"
                      + "    \"awsRegion\" : \"us-west-2\",\n"
                      + "    \"eventTime\" : \"1970-01-01T01:01:01.001Z\",\n"
                      // missing eventName
                      + "    \"userIdentity\" : {\n"
                      + "      \"principalId\" : \"AIDAJDPLRKLG7UEXAMUID\"\n"
                      + "    },\n"
                      + "    \"requestParameters\" : {\n"
                      + "      \"sourceIPAddress\" : \"127.1.2.3\"\n"
                      + "    },\n"
                      // missing response element
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

        S3EventNotification event = S3EventNotification.fromJson(json);
        S3EventNotificationRecord rec = event.getRecords().get(0);
        assertThat(rec).isNotNull();
        assertThat(rec.getEventName()).isNull();
        assertThat(rec.getResponseElements()).isNull();
    }

    @Test
    void extraFields_areIgnored() {
        String json = "{\"Records\":[], \"toto\":123}";
        S3EventNotification event = S3EventNotification.fromJson(json);
        assertThat(event).isNotNull();
        assertThat(event.getRecords()).isEmpty();
    }

    @Test
    void malformedJson_throwsException() {
        String json = "{\"Records\":[], \"toto\"}";
        assertThatThrownBy(() -> S3EventNotification.fromJson(json)).hasCauseInstanceOf(JsonParseException.class);
    }

    @Test
    void fromJsonInputStream_handlesCorrectly() {
        String eventJson = "{  "
                           + "   \"Records\":[  "
                           + "      {  "
                           + "         \"eventVersion\":\"2.1\","
                           + "         \"eventSource\":\"aws:s3\","
                           + "         \"awsRegion\":\"us-west-2\","
                           + "         \"eventTime\":\"1970-01-01T00:00:00.000Z\","
                           + "         \"eventName\":\"ObjectCreated:Put\","
                           + "         \"userIdentity\":{  "
                           + "            \"principalId\":\"AIDAJDPLRKLG7UEXAMPLE\""
                           + "         },"
                           + "         \"requestParameters\":{  "
                           + "            \"sourceIPAddress\":\"127.0.0.1\""
                           + "         },"
                           + "         \"responseElements\":{  "
                           + "            \"x-amz-request-id\":\"C3D13FE58DE4C810\","
                           + "            \"x-amz-id-2\":\"FMyUVURIY8/IgAtTv8xRjskZQpcIZ9KG4V5Wp6S7S/JRWeUWerMUE5JgHvANOjpD\""
                           + "         },"
                           + "         \"s3\":{  "
                           + "            \"s3SchemaVersion\":\"1.0\","
                           + "            \"configurationId\":\"testConfigRule\","
                           + "            \"bucket\":{  "
                           + "               \"name\":\"mybucket\","
                           + "               \"ownerIdentity\":{  "
                           + "                  \"principalId\":\"A3NL1KOZZKExample\""
                           + "               },"
                           + "               \"arn\":\"arn:aws:s3:::mybucket\""
                           + "            },"
                           + "            \"object\":{  "
                           + "               \"key\":\"HappyFace.jpg\","
                           + "               \"size\":1024,"
                           + "               \"eTag\":\"d41d8cd98f00b204e9800998ecf8427e\","
                           + "               \"versionId\":\"096fKKXTRTtl3on89fVO.nfljtsv6qko\","
                           + "               \"sequencer\":\"0055AED6DCD90281E5\""
                           + "            }"
                           + "         }"
                           + "      }"
                           + "   ]"
                           + "}";

        S3EventNotification event = S3EventNotification.fromJson(new ByteArrayInputStream(eventJson.getBytes()));

        assertThat(event.getRecords()).hasSize(1);

        // verify constructors
        S3EventNotification expected = new S3EventNotification(
            Collections.singletonList(new S3EventNotificationRecord(
                "us-west-2",
                "ObjectCreated:Put",
                "aws:s3",
                "1970-01-01T00:00:00.000Z",
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
                new UserIdentity("AIDAJDPLRKLG7UEXAMPLE")
            ))
        );
        assertThat(event).isEqualTo(expected);

        // verify getters
        S3EventNotificationRecord rec = event.getRecords().get(0);
        assertThat(rec).isNotNull();
        assertThat(rec.getAwsRegion()).isEqualTo("us-west-2");
        assertThat(rec.getEventName()).isEqualTo("ObjectCreated:Put");
        assertThat(rec.getEventTime()).isEqualTo(Instant.parse("1970-01-01T00:00:00.000Z"));
        assertThat(rec.getEventVersion()).isEqualTo("2.1");

        UserIdentity userIdentity = rec.getUserIdentity();
        assertThat(userIdentity).isNotNull();
        assertThat(userIdentity.getPrincipalId()).isEqualTo("AIDAJDPLRKLG7UEXAMPLE");

        RequestParameters requestParameters = rec.getRequestParameters();
        assertThat(requestParameters).isNotNull();
        assertThat(requestParameters.getSourceIpAddress()).isEqualTo("127.0.0.1");

        ResponseElements responseElements = rec.getResponseElements();
        assertThat(responseElements).isNotNull();
        assertThat(responseElements.getXAmzRequestId()).isEqualTo("C3D13FE58DE4C810");
        assertThat(responseElements.getXAmzId2())
            .isEqualTo("FMyUVURIY8/IgAtTv8xRjskZQpcIZ9KG4V5Wp6S7S/JRWeUWerMUE5JgHvANOjpD");

        S3 s3 = rec.getS3();
        assertThat(s3).isNotNull();
        assertThat(s3.getS3SchemaVersion()).isEqualTo("1.0");
        assertThat(s3.getConfigurationId()).isEqualTo("testConfigRule");
        S3Bucket s3Bucket = s3.getBucket();
        assertThat(s3Bucket).isNotNull();
        assertThat(s3Bucket.getName()).isEqualTo("mybucket");
        assertThat(s3Bucket.getArn()).isEqualTo("arn:aws:s3:::mybucket");
        UserIdentity ownerIdentity = s3Bucket.getOwnerIdentity();
        assertThat(ownerIdentity).isNotNull();
        assertThat(ownerIdentity.getPrincipalId()).isEqualTo("A3NL1KOZZKExample");
        S3Object s3Object = s3.getObject();
        assertThat(s3Object).isNotNull();
        assertThat(s3Object.getKey()).isEqualTo("HappyFace.jpg");
        assertThat(s3Object.getETag()).isEqualTo("d41d8cd98f00b204e9800998ecf8427e");
        assertThat(s3Object.getSizeAsLong()).isEqualTo(1024L);
        assertThat(s3Object.getVersionId()).isEqualTo("096fKKXTRTtl3on89fVO.nfljtsv6qko");
        assertThat(s3Object.getSequencer()).isEqualTo("0055AED6DCD90281E5");

        assertThat(rec.getGlacierEventData()).isNull();
        assertThat(rec.getIntelligentTieringEventData()).isNull();
        assertThat(rec.getLifecycleEventData()).isNull();
        assertThat(rec.getReplicationEventData()).isNull();

    }
}
