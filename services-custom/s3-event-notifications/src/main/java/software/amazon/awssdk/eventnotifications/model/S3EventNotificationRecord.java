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

import java.time.Instant;
import software.amazon.awssdk.services.s3.model.Event;

public class S3EventNotificationRecord {

    private final String awsRegion;
    private final String eventName;
    private final String eventSource;
    private final String eventVersion;
    private final RequestParametersEntity requestParameters;
    private final ResponseElementsEntity responseElements;
    private final S3Entity s3;
    private final UserIdentityEntity userIdentity;
    private final GlacierEventDataEntity glacierEventData;
    private final LifecycleEventDataEntity lifecycleEventData;
    private final IntelligentTieringEventDataEntity intelligentTieringEventData;
    private final ReplicationEventDataEntity replicationEventDataEntity;

    // @JsonDeserialize(using = InstantDeserializer.class)
    // @JsonSerialize(using = InstantSerializer.class)
    private Instant eventTime;

    public S3EventNotificationRecord(
        String awsRegion,
        String eventName,
        String eventSource,
        String eventTime,
        String eventVersion,
        RequestParametersEntity requestParameters,
        ResponseElementsEntity responseElements,
        S3Entity s3,
        UserIdentityEntity userIdentity) {
        this(awsRegion,
             eventName,
             eventSource,
             eventTime,
             eventVersion,
             requestParameters,
             responseElements,
             s3,
             userIdentity,
             null,
             null,
             null,
             null);
    }

    // @JsonCreator
    public S3EventNotificationRecord(
        // @JsonProperty("awsRegion")
        String awsRegion,
        // @JsonProperty("eventName")
        String eventName,
        // @JsonProperty("eventSource")
        String eventSource,
        // @JsonProperty("eventTime")
        String eventTime,
        // @JsonProperty("eventVersion")
        String eventVersion,
        // @JsonProperty("requestParameters")
        RequestParametersEntity requestParameters,
        // @JsonProperty("responseElements")
        ResponseElementsEntity responseElements,
        // @JsonProperty("s3")
        S3Entity s3,
        // @JsonProperty("userIdentity")
        UserIdentityEntity userIdentity,
        // @JsonProperty("glacierEventData")
        GlacierEventDataEntity glacierEventData,
        // @JsonProperty("lifecycleEventData")
        LifecycleEventDataEntity lifecycleEventData,
        // @JsonProperty("intelligentTieringEventData")
        IntelligentTieringEventDataEntity intelligentTieringEventData,
        // @JsonProperty("replicationEventData")
        ReplicationEventDataEntity replicationEventData) {
        this.awsRegion = awsRegion;
        this.eventName = eventName;
        this.eventSource = eventSource;

        if (eventTime != null) {
            this.eventTime = Instant.parse(eventTime);
        }

        this.eventVersion = eventVersion;
        this.requestParameters = requestParameters;
        this.responseElements = responseElements;
        this.s3 = s3;
        this.userIdentity = userIdentity;
        this.glacierEventData = glacierEventData;
        this.lifecycleEventData = lifecycleEventData;
        this.intelligentTieringEventData = intelligentTieringEventData;
        this.replicationEventDataEntity = replicationEventData;
    }

    public String getAwsRegion() {
        return awsRegion;
    }

    public String getEventName() {
        return eventName;
    }

    // @JsonIgnore
    public Event getEventNameAsEnum() {
        return Event.fromValue(eventName);
    }

    public String getEventSource() {
        return eventSource;
    }

    // @JsonSerialize(using = InstantSerializer.class)
    public Instant getEventTime() {
        return eventTime;
    }

    public String getEventVersion() {
        return eventVersion;
    }

    public RequestParametersEntity getRequestParameters() {
        return requestParameters;
    }

    public ResponseElementsEntity getResponseElements() {
        return responseElements;
    }

    public S3Entity getS3() {
        return s3;
    }

    public UserIdentityEntity getUserIdentity() {
        return userIdentity;
    }

    public GlacierEventDataEntity getGlacierEventData() {
        return glacierEventData;
    }

    public LifecycleEventDataEntity getLifecycleEventData() {
        return lifecycleEventData;
    }

    public IntelligentTieringEventDataEntity getIntelligentTieringEventData() {
        return intelligentTieringEventData;
    }

    public ReplicationEventDataEntity getReplicationEventDataEntity() {
        return replicationEventDataEntity;
    }
}
