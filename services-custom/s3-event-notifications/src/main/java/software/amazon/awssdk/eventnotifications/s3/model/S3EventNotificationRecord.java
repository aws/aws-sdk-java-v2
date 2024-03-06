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

import java.time.Instant;
import java.util.Objects;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

@SdkPublicApi
public class S3EventNotificationRecord {

    private final String awsRegion;
    private final String eventName;
    private final String eventSource;
    private final String eventVersion;
    private final RequestParameters requestParameters;
    private final ResponseElements responseElements;
    private final S3 s3;
    private final UserIdentity userIdentity;
    private final GlacierEventData glacierEventData;
    private final LifecycleEventData lifecycleEventData;
    private final IntelligentTieringEventData intelligentTieringEventData;
    private final ReplicationEventData replicationEventData;
    private final Instant eventTime;

    public S3EventNotificationRecord(
        String awsRegion,
        String eventName,
        String eventSource,
        String eventTime,
        String eventVersion,
        RequestParameters requestParameters,
        ResponseElements responseElements,
        S3 s3,
        UserIdentity userIdentity,
        GlacierEventData glacierEventData,
        LifecycleEventData lifecycleEventData,
        IntelligentTieringEventData intelligentTieringEventData,
        ReplicationEventData replicationEventData) {
        this.awsRegion = awsRegion;
        this.eventName = eventName;
        this.eventSource = eventSource;
        this.eventTime = eventName != null ? Instant.parse(eventTime) : null;
        this.eventVersion = eventVersion;
        this.requestParameters = requestParameters;
        this.responseElements = responseElements;
        this.s3 = s3;
        this.userIdentity = userIdentity;
        this.glacierEventData = glacierEventData;
        this.lifecycleEventData = lifecycleEventData;
        this.intelligentTieringEventData = intelligentTieringEventData;
        this.replicationEventData = replicationEventData;
    }

    public S3EventNotificationRecord(
        String awsRegion,
        String eventName,
        String eventSource,
        String eventTime,
        String eventVersion,
        RequestParameters requestParameters,
        ResponseElements responseElements,
        S3 s3,
        UserIdentity userIdentity) {
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


    public String getAwsRegion() {
        return awsRegion;
    }

    public String getEventName() {
        return eventName;
    }

    public String getEventSource() {
        return eventSource;
    }

    public Instant getEventTime() {
        return eventTime;
    }

    public String getEventVersion() {
        return eventVersion;
    }

    public RequestParameters getRequestParameters() {
        return requestParameters;
    }

    public ResponseElements getResponseElements() {
        return responseElements;
    }

    public S3 getS3() {
        return s3;
    }

    public UserIdentity getUserIdentity() {
        return userIdentity;
    }

    public GlacierEventData getGlacierEventData() {
        return glacierEventData;
    }

    public LifecycleEventData getLifecycleEventData() {
        return lifecycleEventData;
    }

    public IntelligentTieringEventData getIntelligentTieringEventData() {
        return intelligentTieringEventData;
    }

    public ReplicationEventData getReplicationEventData() {
        return replicationEventData;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        S3EventNotificationRecord that = (S3EventNotificationRecord) o;

        if (!Objects.equals(awsRegion, that.awsRegion)) {
            return false;
        }
        if (!Objects.equals(eventName, that.eventName)) {
            return false;
        }
        if (!Objects.equals(eventSource, that.eventSource)) {
            return false;
        }
        if (!Objects.equals(eventVersion, that.eventVersion)) {
            return false;
        }
        if (!Objects.equals(requestParameters, that.requestParameters)) {
            return false;
        }
        if (!Objects.equals(responseElements, that.responseElements)) {
            return false;
        }
        if (!Objects.equals(s3, that.s3)) {
            return false;
        }
        if (!Objects.equals(userIdentity, that.userIdentity)) {
            return false;
        }
        if (!Objects.equals(glacierEventData, that.glacierEventData)) {
            return false;
        }
        if (!Objects.equals(lifecycleEventData, that.lifecycleEventData)) {
            return false;
        }
        if (!Objects.equals(intelligentTieringEventData, that.intelligentTieringEventData)) {
            return false;
        }
        if (!Objects.equals(replicationEventData, that.replicationEventData)) {
            return false;
        }
        return Objects.equals(eventTime, that.eventTime);
    }

    @Override
    public int hashCode() {
        int result = awsRegion != null ? awsRegion.hashCode() : 0;
        result = 31 * result + (eventName != null ? eventName.hashCode() : 0);
        result = 31 * result + (eventSource != null ? eventSource.hashCode() : 0);
        result = 31 * result + (eventVersion != null ? eventVersion.hashCode() : 0);
        result = 31 * result + (requestParameters != null ? requestParameters.hashCode() : 0);
        result = 31 * result + (responseElements != null ? responseElements.hashCode() : 0);
        result = 31 * result + (s3 != null ? s3.hashCode() : 0);
        result = 31 * result + (userIdentity != null ? userIdentity.hashCode() : 0);
        result = 31 * result + (glacierEventData != null ? glacierEventData.hashCode() : 0);
        result = 31 * result + (lifecycleEventData != null ? lifecycleEventData.hashCode() : 0);
        result = 31 * result + (intelligentTieringEventData != null ? intelligentTieringEventData.hashCode() : 0);
        result = 31 * result + (replicationEventData != null ? replicationEventData.hashCode() : 0);
        result = 31 * result + (eventTime != null ? eventTime.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return ToString.builder("S3EventNotificationRecord")
                       .add("awsRegion", awsRegion)
                       .add("eventName", eventName)
                       .add("eventSource", eventSource)
                       .add("eventVersion", eventVersion)
                       .add("requestParameters", requestParameters)
                       .add("responseElements", responseElements)
                       .add("s3", s3)
                       .add("userIdentity", userIdentity)
                       .add("glacierEventData", glacierEventData)
                       .add("lifecycleEventData", lifecycleEventData)
                       .add("intelligentTieringEventData", intelligentTieringEventData)
                       .add("replicationEventData", replicationEventData)
                       .add("eventTime", eventTime)
                       .build();
    }
}
