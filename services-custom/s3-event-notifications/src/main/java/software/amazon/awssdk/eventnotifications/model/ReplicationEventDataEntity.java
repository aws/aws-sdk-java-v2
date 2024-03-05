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


public class ReplicationEventDataEntity {

    private final String replicationRuleId;
    private final String destinationBucket;
    private final String s3Operation;
    private final String requestTime;
    private final String failureReason;
    private final String threshold;
    private final String replicationTime;

    // @JsonCreator
    public ReplicationEventDataEntity(
        // @JsonProperty("replicationRuleId")
        String replicationRuleId,
        // @JsonProperty("destinationBucket")
        String destinationBucket,
        // @JsonProperty("s3Operation")
        String s3Operation,
        // @JsonProperty("requestTime")
        String requestTime,
        // @JsonProperty("failureReason")
        String failureReason,
        // @JsonProperty("threshold")
        String threshold,
        // @JsonProperty("replicationTime")
        String replicationTime) {
        this.replicationRuleId = replicationRuleId;
        this.destinationBucket = destinationBucket;
        this.s3Operation = s3Operation;
        this.requestTime = requestTime;
        this.failureReason = failureReason;
        this.threshold = threshold;
        this.replicationTime = replicationTime;
    }

    // @JsonProperty("replicationRuleId")
    public String getReplicationRuleId() {
        return replicationRuleId;
    }

    // @JsonProperty("destinationBucket")
    public String getDestinationBucket() {
        return destinationBucket;
    }

    // @JsonProperty("s3Operation")
    public String getS3Operation() {
        return s3Operation;
    }

    // @JsonProperty("requestTime")
    public String getRequestTime() {
        return requestTime;
    }

    // @JsonProperty("failureReason")
    public String getFailureReason() {
        return failureReason;
    }

    // @JsonProperty("threshold")
    public String getThreshold() {
        return threshold;
    }

    // @JsonProperty("replicationTime")
    public String getReplicationTime() {
        return replicationTime;
    }
}
