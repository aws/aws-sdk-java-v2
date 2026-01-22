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

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import software.amazon.awssdk.annotations.SdkInternalApi;
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
import software.amazon.awssdk.protocols.jsoncore.JsonNodeParser;
import software.amazon.awssdk.protocols.jsoncore.JsonWriter;
import software.amazon.awssdk.protocols.jsoncore.JsonWriter.JsonGeneratorFactory;
import software.amazon.awssdk.thirdparty.jackson.core.JsonGenerator;

@SdkInternalApi
public final class DefaultS3EventNotificationWriter implements S3EventNotificationWriter {
    private static final S3EventNotificationWriter INSTANCE = S3EventNotificationWriter.builder().build();

    private final Boolean prettyPrint;
    private final JsonGeneratorFactory jsonGeneratorFactory;

    private DefaultS3EventNotificationWriter(DefaultBuilder builder) {
        this.prettyPrint = builder.prettyPrint;
        if (Boolean.TRUE.equals(builder.prettyPrint)) {
            this.jsonGeneratorFactory = os -> {
                JsonGenerator generator = JsonNodeParser.DEFAULT_JSON_FACTORY.createGenerator(os);
                generator.useDefaultPrettyPrinter();
                return generator;
            };
        } else {
            this.jsonGeneratorFactory = null;
        }
    }

    @Override
    public String writeToString(S3EventNotification event) {
        return new String(writeEvent(event), StandardCharsets.UTF_8);
    }

    private byte[] writeEvent(S3EventNotification event) {
        JsonWriter writer = JsonWriter.builder()
                                      .jsonGeneratorFactory(jsonGeneratorFactory)
                                      .build();
        writer.writeStartObject();
        writeRecords(writer, event.getRecords());
        writer.writeEndObject();
        return writer.getBytes();
    }

    private void writeRecords(JsonWriter writer, List<S3EventNotificationRecord> records) {
        writer.writeFieldName("Records");
        if (records == null) {
            writer.writeNull();
            return;
        }
        writer.writeStartArray();
        records.forEach(rec -> writeRecord(writer, rec));
        writer.writeEndArray();
    }

    private void writeRecord(JsonWriter writer, S3EventNotificationRecord rec) {
        if (rec == null) {
            writer.writeNull();
            return;
        }
        writer.writeStartObject();
        writeStringField(writer, "eventVersion", rec.getEventVersion());
        writeStringField(writer, "eventSource", rec.getEventSource());
        writeStringField(writer, "awsRegion", rec.getAwsRegion());
        String eventTime = rec.getEventTime() != null
                           ? DateTimeFormatter.ISO_INSTANT.format(rec.getEventTime())
                           : null;
        writeStringField(writer, "eventTime", eventTime);
        writeStringField(writer, "eventName", rec.getEventName());
        writeUserIdentity(writer, rec.getUserIdentity());
        writeRequestParam(writer, rec.getRequestParameters());
        writeResponseElements(writer, rec.getResponseElements());
        writeS3(writer, rec.getS3());

        if (rec.getGlacierEventData() != null) {
            writeGlacierEventData(writer, rec.getGlacierEventData());
        }

        if (rec.getReplicationEventData() != null) {
            writeReplicationEventData(writer, rec.getReplicationEventData());
        }

        if (rec.getIntelligentTieringEventData() != null) {
            writeIntelligentTieringEventData(writer, rec.getIntelligentTieringEventData());
        }

        if (rec.getLifecycleEventData() != null) {
            writeLifecyleEventData(writer, rec.getLifecycleEventData());
        }

        writer.writeEndObject();
    }

    private void writeLifecyleEventData(JsonWriter writer, LifecycleEventData lifecycleEventData) {
        writer.writeFieldName("lifecycleEventData");
        writer.writeStartObject();
        TransitionEventData transitionEventData = lifecycleEventData.getTransitionEventData();
        if (transitionEventData != null) {
            writer.writeFieldName("transitionEventData");
            writer.writeStartObject();
            writeStringField(writer, "destinationStorageClass", transitionEventData.getDestinationStorageClass());
            writer.writeEndObject();
        }
        writer.writeEndObject();
    }

    private void writeIntelligentTieringEventData(JsonWriter writer, IntelligentTieringEventData intelligentTieringEventData) {
        writer.writeFieldName("intelligentTieringEventData");
        writer.writeStartObject();
        writeStringField(writer, "destinationAccessTier", intelligentTieringEventData.getDestinationAccessTier());
        writer.writeEndObject();
    }

    private void writeReplicationEventData(JsonWriter writer, ReplicationEventData replicationEventData) {
        writer.writeFieldName("replicationEventData");
        writer.writeStartObject();
        writeStringField(writer, "replicationRuleId", replicationEventData.getReplicationRuleId());
        writeStringField(writer, "destinationBucket", replicationEventData.getDestinationBucket());
        writeStringField(writer, "s3Operation", replicationEventData.getS3Operation());
        writeStringField(writer, "requestTime", replicationEventData.getRequestTime());
        writeStringField(writer, "failureReason", replicationEventData.getFailureReason());
        writeStringField(writer, "threshold", replicationEventData.getThreshold());
        writeStringField(writer, "replicationTime", replicationEventData.getReplicationTime());
        writer.writeEndObject();
    }

    private void writeGlacierEventData(JsonWriter writer, GlacierEventData glacierEventData) {
        writer.writeFieldName("glacierEventData");
        writer.writeStartObject();
        RestoreEventData restoreEventData = glacierEventData.getRestoreEventData();
        if (restoreEventData != null) {
            writer.writeFieldName("restoreEventData");
            writer.writeStartObject();
            Instant lifecycleRestorationExpiryTime = restoreEventData.getLifecycleRestorationExpiryTime();
            String expiryTime = lifecycleRestorationExpiryTime == null
                ? null
                : DateTimeFormatter.ISO_INSTANT.format(lifecycleRestorationExpiryTime);
            writeStringField(writer, "lifecycleRestorationExpiryTime", expiryTime);
            writeStringField(writer, "lifecycleRestoreStorageClass", restoreEventData.getLifecycleRestoreStorageClass());
        }
        writer.writeEndObject();
        writer.writeEndObject();
    }

    private void writeS3(JsonWriter writer, S3 s3) {
        writer.writeFieldName("s3");
        if (s3 == null) {
            writer.writeNull();
            return;
        }
        writer.writeStartObject();
        writeStringField(writer, "s3SchemaVersion", s3.getS3SchemaVersion());
        writeStringField(writer, "configurationId", s3.getConfigurationId());
        writeS3Bucket(writer, s3.getBucket());
        writeS3Object(writer, s3.getObject());
        writer.writeEndObject();
    }

    private void writeS3Object(JsonWriter writer, S3Object s3Object) {
        writer.writeFieldName("object");
        if (s3Object == null) {
            writer.writeNull();
            return;
        }
        writer.writeStartObject();
        writeStringField(writer, "key", s3Object.getKey());
        writeNumericField(writer, "size", s3Object.getSizeAsLong());
        writeStringField(writer, "eTag", s3Object.getETag());
        writeStringField(writer, "versionId", s3Object.getVersionId());
        writeStringField(writer, "sequencer", s3Object.getSequencer());
        writer.writeEndObject();
    }

    private void writeS3Bucket(JsonWriter writer, S3Bucket bucket) {
        writer.writeFieldName("bucket");
        if (bucket == null) {
            writer.writeNull();
            return;
        }
        writer.writeStartObject();
        writeStringField(writer, "name", bucket.getName());
        writer.writeFieldName("ownerIdentity");
        if (bucket.getOwnerIdentity().getPrincipalId() == null) {
            writer.writeNull();
        } else {
            writer.writeStartObject();
            writeStringField(writer, "principalId", bucket.getOwnerIdentity().getPrincipalId());
            writer.writeEndObject();
        }
        writeStringField(writer, "arn", bucket.getArn());
        writer.writeEndObject();
    }

    private void writeResponseElements(JsonWriter writer, ResponseElements responseElements) {
        writer.writeFieldName("responseElements");
        if (responseElements == null) {
            writer.writeNull();
            return;
        }
        writer.writeStartObject();
        writeStringField(writer, "x-amz-request-id", responseElements.getXAmzRequestId());
        writeStringField(writer, "x-amz-id-2", responseElements.getXAmzId2());
        writer.writeEndObject();
    }

    private void writeRequestParam(JsonWriter writer, RequestParameters requestParameters) {
        writer.writeFieldName("requestParameters");
        if (requestParameters == null) {
            writer.writeNull();
            return;
        }
        writer.writeStartObject();
        writeStringField(writer, "sourceIPAddress", requestParameters.getSourceIpAddress());
        writer.writeEndObject();
    }

    private void writeUserIdentity(JsonWriter writer, UserIdentity userIdentity) {
        writer.writeFieldName("userIdentity");
        if (userIdentity == null) {
            writer.writeNull();
            return;
        }
        writer.writeStartObject();
        writeStringField(writer, "principalId", userIdentity.getPrincipalId());
        writer.writeEndObject();
    }

    public static S3EventNotificationWriter create() {
        return INSTANCE;
    }

    private void writeStringField(JsonWriter writer, String field, String value) {
        writer.writeFieldName(field);
        writer.writeValue(value);
    }

    private void writeNumericField(JsonWriter writer, String field, Long value) {
        writer.writeFieldName(field);
        if (value == null) {
            writer.writeNull();
        } else {
            writer.writeNumber(value.toString());
        }
    }

    @Override
    public DefaultBuilder toBuilder() {
        return new DefaultBuilder(this);
    }

    public static S3EventNotificationWriter.Builder builder() {
        return new DefaultBuilder();
    }

    public static final class DefaultBuilder implements S3EventNotificationWriter.Builder {
        private Boolean prettyPrint;

        private DefaultBuilder() {
        }

        private DefaultBuilder(DefaultS3EventNotificationWriter writer) {
            this.prettyPrint = writer.prettyPrint;
        }

        @Override
        public S3EventNotificationWriter.Builder prettyPrint(Boolean prettyPrint) {
            this.prettyPrint = prettyPrint;
            return this;
        }

        @Override
        public S3EventNotificationWriter build() {
            return new DefaultS3EventNotificationWriter(this);
        }
    }
}
