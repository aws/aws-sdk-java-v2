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

package software.amazon.awssdk.eventnotifications.s3.internal;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.eventnotifications.s3.model.GlacierEventData;
import software.amazon.awssdk.eventnotifications.s3.model.IntelligentTieringEventData;
import software.amazon.awssdk.eventnotifications.s3.model.LifecycleEventData;
import software.amazon.awssdk.eventnotifications.s3.model.ReplicationEventData;
import software.amazon.awssdk.eventnotifications.s3.model.RequestParameters;
import software.amazon.awssdk.eventnotifications.s3.model.ResponseElements;
import software.amazon.awssdk.eventnotifications.s3.model.RestoreEventData;
import software.amazon.awssdk.eventnotifications.s3.model.S3Bucket;
import software.amazon.awssdk.eventnotifications.s3.model.S3;
import software.amazon.awssdk.eventnotifications.s3.model.S3EventNotification;
import software.amazon.awssdk.eventnotifications.s3.model.S3EventNotificationRecord;
import software.amazon.awssdk.eventnotifications.s3.model.S3Object;
import software.amazon.awssdk.eventnotifications.s3.model.TransitionEventData;
import software.amazon.awssdk.eventnotifications.s3.model.UserIdentity;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;
import software.amazon.awssdk.protocols.jsoncore.JsonNodeParser;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
public final class DefaultS3EventNotificationReader implements S3EventNotificationReader {
    private static final JsonNodeParser JSON_NODE_PARSER = JsonNodeParser.create();

    @Override
    public S3EventNotification read(String event) {
        return read(event.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public S3EventNotification read(byte[] event) {
        return read(new ByteArrayInputStream(event));
    }

    @Override
    public S3EventNotification read(InputStream event) {
        return readEvent(JSON_NODE_PARSER.parse(event));
    }

    private S3EventNotification readEvent(JsonNode jsonNode) {
        Map<String, JsonNode> records = expectObject(jsonNode, "Records");
        return new S3EventNotification(readRecords(records.get("Records")));
    }

    private List<S3EventNotificationRecord> readRecords(JsonNode node) {
        List<JsonNode> recordArray = expectArray(node, "Records");
        return recordArray.stream().map(this::readEventNotificationRecord).collect(Collectors.toList());
    }

    private S3EventNotificationRecord readEventNotificationRecord(JsonNode jsonNode) {
        Map<String, JsonNode> recordNode = expectObject(jsonNode, "Records[]");
        String eventVersion = expectString(recordNode.get("eventVersion"), "eventVersion");
        String awsRegion = expectString(recordNode.get("awsRegion"), "awsRegion");
        String eventName = expectString(recordNode.get("eventName"), "eventName");
        String eventSource = expectString(recordNode.get("eventSource"), "eventSource");
        String eventTime = expectString(recordNode.get("eventTime"), "eventTime");
        RequestParameters requestParameters = readRequestParameters(recordNode.get("requestParameters"));
        ResponseElements responseElements = readResponseElements(recordNode.get("responseElements"));
        S3 s3 = readS3(recordNode.get("s3"));
        UserIdentity userIdentity = readUserIdentity(recordNode.get("userIdentity"));
        GlacierEventData glacierEventData = readGlacierEventData(recordNode.get("glacierEventData"));
        LifecycleEventData lifecycleEventData = readLifecycleEventData(recordNode.get("lifecycleEventData"));
        IntelligentTieringEventData intelligentTieringEventData =
            readIntelligentTieringEventData(recordNode.get("intelligentTieringEventData"));
        ReplicationEventData replicationEventData = readReplicationEventData(recordNode.get("replicationEventData"));
        return new S3EventNotificationRecord(
            awsRegion,
            eventName,
            eventSource,
            eventTime,
            eventVersion,
            requestParameters,
            responseElements,
            s3,
            userIdentity,
            glacierEventData,
            lifecycleEventData,
            intelligentTieringEventData,
            replicationEventData
        );
    }

    private ReplicationEventData readReplicationEventData(JsonNode jsonNode) {
        Map<String, JsonNode> replicationDataNode = expectObjectOrNull(jsonNode, "replicationEventData");
        if (replicationDataNode == null) {
            return null;
        }

        String replicationRuleId = expectString(replicationDataNode.get("replicationRuleId"), "replicationRuleId");
        String destinationBucket = expectString(replicationDataNode.get("destinationBucket"), "destinationBucket");
        String s3Operation = expectString(replicationDataNode.get("s3Operation"), "s3Operation");
        String requestTime = expectString(replicationDataNode.get("requestTime"), "requestTime");
        String failureReason = expectString(replicationDataNode.get("failureReason"), "failureReason");
        String threshold = expectString(replicationDataNode.get("threshold"), "threshold");
        String replicationTime = expectString(replicationDataNode.get("replicationTime"), "replicationTime");

        return new ReplicationEventData(replicationRuleId,
                                        destinationBucket,
                                        s3Operation,
                                        requestTime,
                                        failureReason,
                                        threshold,
                                        replicationTime);
    }

    private IntelligentTieringEventData readIntelligentTieringEventData(JsonNode jsonNode) {
        Map<String, JsonNode> lifeCycleEventDataNode = expectObjectOrNull(jsonNode, "intelligentTieringEventData");
        if (lifeCycleEventDataNode == null) {
            return null;
        }
        String destinationAccessTier = expectString(
            lifeCycleEventDataNode.get("destinationAccessTier"), "destinationAccessTier");
        return new IntelligentTieringEventData(destinationAccessTier);
    }

    private LifecycleEventData readLifecycleEventData(JsonNode jsonNode) {
        Map<String, JsonNode> lifeCycleEventDataNode = expectObjectOrNull(jsonNode, "lifecycleEventData");
        if (lifeCycleEventDataNode == null) {
            return null;
        }
        Map<String, JsonNode> transitionEventDataNode =
            expectObject(lifeCycleEventDataNode.get("transitionEventData"), "transitionEventData");
        String destinationStorageClass =
            expectString(transitionEventDataNode.get("destinationStorageClass"), "destinationStorageClass");
        return new LifecycleEventData(new TransitionEventData(destinationStorageClass));
    }

    private GlacierEventData readGlacierEventData(JsonNode jsonNode) {
        Map<String, JsonNode> glacierEventDataNode = expectObjectOrNull(jsonNode, "glacierEventData");
        if (glacierEventDataNode == null) {
            return null;
        }
        Map<String, JsonNode> restoreEventDataNode =
            expectObject(glacierEventDataNode.get("restoreEventData"), "restoreEventData");
        String lifecycleRestorationExpiryTime = expectStringOrNull(
            restoreEventDataNode.get("lifecycleRestorationExpiryTime"), "lifecycleRestorationExpiryTime");
        String lifecycleRestoreStorageClass = expectString(
            restoreEventDataNode.get("lifecycleRestoreStorageClass"), "lifecycleRestoreStorageClass");
        return new GlacierEventData(new RestoreEventData(lifecycleRestorationExpiryTime,
                                                         lifecycleRestoreStorageClass));
    }

    private UserIdentity readUserIdentity(JsonNode jsonNode) {
        Map<String, JsonNode> userIdentityNode = expectObject(jsonNode, "userIdentity");
        String principalId = expectString(userIdentityNode.get("principalId"), "principalId");
        return new UserIdentity(principalId);
    }

    private S3 readS3(JsonNode jsonNode) {
        Map<String, JsonNode> s3 = expectObject(jsonNode, "s3");
        String configurationId = expectString(s3.get("configurationId"), "configurationId");
        S3Bucket bucket = readBucket(s3.get("bucket"));
        S3Object object = readObject(s3.get("object"));
        String s3SchemaVersion = expectString(s3.get("s3SchemaVersion"), "s3SchemaVersion");
        return new S3(configurationId, bucket, object, s3SchemaVersion);
    }

    private S3Object readObject(JsonNode jsonNode) {
        Map<String, JsonNode> objectNode = expectObject(jsonNode, "object");
        String key = expectString(objectNode.get("key"), "key");
        Long size = expectLong(objectNode.get("size"), "size");
        String eTag = expectString(objectNode.get("eTag"), "eTag");
        String versionId = expectString(objectNode.get("versionId"), "versionId");
        String sequencer = expectString(objectNode.get("sequencer"), "sequencer");
        return new S3Object(key, size, eTag, versionId, sequencer);
    }

    private S3Bucket readBucket(JsonNode jsonNode) {
        Map<String, JsonNode> bucketNode = expectObject(jsonNode, "bucket");
        String name = expectString(bucketNode.get("name"), "name");
        UserIdentity ownerIdentity = readOwnerIdentity(bucketNode.get("ownerIdentity"));
        String arn = expectString(bucketNode.get("arn"), "arn");
        return new S3Bucket(name, ownerIdentity, arn);
    }

    private UserIdentity readOwnerIdentity(JsonNode jsonNode) {
        Map<String, JsonNode> ownerIdentityNode = expectObject(jsonNode, "ownerIdentity");
        String principalId = expectString(ownerIdentityNode.get("principalId"), "principalId");
        return new UserIdentity(principalId);
    }

    private ResponseElements readResponseElements(JsonNode jsonNode) {
        Map<String, JsonNode> responseElementNode = expectObject(jsonNode, "responseElements");
        String requestId = expectString(responseElementNode.get("x-amz-request-id"), "x-amz-request-id");
        String id2 = expectString(responseElementNode.get("x-amz-id-2"), "x-amz-id-2");
        return new ResponseElements(id2, requestId);
    }

    private RequestParameters readRequestParameters(JsonNode jsonNode) {
        Map<String, JsonNode> requestParametersNode = expectObject(jsonNode, "requestParameters");
        JsonNode sourceIpAddress = requestParametersNode.get("sourceIPAddress");
        String sourceIpAddressString = expectString(sourceIpAddress, "sourceIPAddress");
        return new RequestParameters(sourceIpAddressString);
    }


    // =========================== UTILS ==================================
    private String expectStringOrNull(JsonNode node, String name) {
        if (node == null) {
            return null;
        }
        return expectString(node, name);
    }

    private String expectString(JsonNode node, String name) {
        Validate.isTrue(node.isString(), "'%s' was not a string", name);
        return node.asString();
    }

    private List<JsonNode> expectArray(JsonNode node, String name) {
        Validate.isTrue(node.isArray(), "expected '%s' to be an array, but was not.", name);
        return node.asArray();
    }

    private Map<String, JsonNode> expectObjectOrNull(JsonNode node, String name) {
        if (node == null) {
            return null;
        }
        return expectObject(node, name);
    }

    private Map<String, JsonNode> expectObject(JsonNode node, String name) {
        Validate.isTrue(node.isObject(), "expected '%s' to be an object, but was not.", name);
        return node.asObject();
    }

    private Long expectLong(JsonNode node, String name) {
        Validate.isTrue(node.isNumber(), "expected '%s' to be numeric, but was not", name);
        return Long.parseLong(node.asNumber());
    }
}
