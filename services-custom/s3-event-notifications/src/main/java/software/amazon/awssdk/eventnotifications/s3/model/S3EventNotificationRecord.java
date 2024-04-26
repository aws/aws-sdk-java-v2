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
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.utils.ToString;

/**
 * A record representing a notification for a single event. The
 * <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/notification-content-structure.html">Event message structure</a>
 * page of S3 user guide contains additional information about the different fields of the
 * notification record.
 */
@SdkPublicApi
public class S3EventNotificationRecord {

    private String awsRegion;
    private String eventName;
    private String eventSource;
    private String eventVersion;
    private RequestParameters requestParameters;
    private ResponseElements responseElements;
    private S3 s3;
    private UserIdentity userIdentity;
    private GlacierEventData glacierEventData;
    private LifecycleEventData lifecycleEventData;
    private IntelligentTieringEventData intelligentTieringEventData;
    private ReplicationEventData replicationEventData;
    private Instant eventTime;

    public S3EventNotificationRecord() {
    }

    @SdkTestInternalApi
    S3EventNotificationRecord(
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

    @SdkTestInternalApi
    S3EventNotificationRecord(
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

    public String getAwsRegion() {
        return awsRegion;
    }

    /**
     * The name of the event type for this notification. For more information about the various event type, visit the
     * <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/notification-how-to-event-types-and-destinations.html#supported-notification-event-types">
     *     Event notification types and destinations
     *     </a> page of S3 user guide.
     * It references the list of event notification types but doesn't contain the s3: prefix.
     * @return the event name.
     */
    public String getEventName() {
        return eventName;
    }


    /**
     * The service from which this event was generated, usually {@code "aws:s3"}.
     * @return the event source.
     */
    public String getEventSource() {
        return eventSource;
    }

    /**
     * @return The time, in ISO-8601 format, for example, 1970-01-01T00:00:00.000Z, when Amazon S3 finished processing the
     * request.
     */
    public Instant getEventTime() {
        return eventTime;
    }

    /**
     * The eventVersion key value contains a major and minor version in the form {@code <major>.<minor>}.
     * @return the event version.
     */
    public String getEventVersion() {
        return eventVersion;
    }

    /**
     * Request Parameters contains the {@code sourceIPAddress} field, which is the ip address where request came from.
     * @return the request parameter containing the source IP address.
     */
    public RequestParameters getRequestParameters() {
        return requestParameters;
    }

    /**
     * The responseElements key value is useful if you want to trace a request by following up with AWS Support.
     * Both x-amz-request-id and x-amz-id-2 help Amazon S3 trace an individual request.
     * These values are the same as those that Amazon S3 returns in the response to the request that initiates the events.
     * This is so they can be used to match the event to the request.
     * @return The response element containing the trace information.
     */
    public ResponseElements getResponseElements() {
        return responseElements;
    }

    /**
     * Contains information about the bucket and object involved in the event. The object key name value is URL encoded. For
     * example, "red flower.jpg" becomes "red+flower.jpg" (Amazon S3 returns "application/x-www-form-urlencoded" as the
     * content type in the response).
     * @return the instance of {@link S3} containing object information.
     */
    public S3 getS3() {
        return s3;
    }

    /**
     * The user identity contains the {@code principalId} field, which has the Amazon customer ID of the user who caused the
     * event.
     * @return the user identity containing the {@code principalId}.
     */
    public UserIdentity getUserIdentity() {
        return userIdentity;
    }

    /**
     * The GlacierEventData is only visible for s3:ObjectRestore:Completed events.
     * Contains information related to restoring an archived object. For more information about archive and storage classes, see
     * <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/restoring-objects.html">Restoring an archived object</a>
     * @return the glacier event data.
     */
    public GlacierEventData getGlacierEventData() {
        return glacierEventData;
    }

    /**
     * The LifecycleEventData is only visible for
     * <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/lifecycle-transition-general-considerations.html">
     *     S3 Lifecycle transition</a> related events.
     * @return the lifecycle event data.
     */
    public LifecycleEventData getLifecycleEventData() {
        return lifecycleEventData;
    }

    /**
     * The IntelligentTieringEventData key is only visible for
     * <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/intelligent-tiering.html">S3 Intelligent-Tiering</a>
     * related events.
     * @return the intelligent tiering event data.
     */
    public IntelligentTieringEventData getIntelligentTieringEventData() {
        return intelligentTieringEventData;
    }

    /**
     * The ReplicationEventData is only visible for
     * <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/replication.html">replication</a> related events.
     * @return
     */
    public ReplicationEventData getReplicationEventData() {
        return replicationEventData;
    }

    public void setAwsRegion(String awsRegion) {
        this.awsRegion = awsRegion;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public void setEventSource(String eventSource) {
        this.eventSource = eventSource;
    }

    public void setEventVersion(String eventVersion) {
        this.eventVersion = eventVersion;
    }

    public void setRequestParameters(RequestParameters requestParameters) {
        this.requestParameters = requestParameters;
    }

    public void setResponseElements(ResponseElements responseElements) {
        this.responseElements = responseElements;
    }

    public void setS3(S3 s3) {
        this.s3 = s3;
    }

    public void setUserIdentity(UserIdentity userIdentity) {
        this.userIdentity = userIdentity;
    }

    public void setGlacierEventData(GlacierEventData glacierEventData) {
        this.glacierEventData = glacierEventData;
    }

    public void setLifecycleEventData(LifecycleEventData lifecycleEventData) {
        this.lifecycleEventData = lifecycleEventData;
    }

    public void setIntelligentTieringEventData(IntelligentTieringEventData intelligentTieringEventData) {
        this.intelligentTieringEventData = intelligentTieringEventData;
    }

    public void setReplicationEventData(ReplicationEventData replicationEventData) {
        this.replicationEventData = replicationEventData;
    }

    public void setEventTime(Instant eventTime) {
        this.eventTime = eventTime;
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
