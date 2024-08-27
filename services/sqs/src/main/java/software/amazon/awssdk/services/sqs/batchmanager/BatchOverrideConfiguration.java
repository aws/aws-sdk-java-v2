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

package software.amazon.awssdk.services.sqs.batchmanager;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Configuration values for the BatchManager Implementation.  All values are optional, and the default values will be used if they
 * are not specified.
 */
@SdkPublicApi
public final class BatchOverrideConfiguration implements ToCopyableBuilder<BatchOverrideConfiguration.Builder,
    BatchOverrideConfiguration> {

    private final Integer maxBatchItems;
    private final Integer maxBatchKeys;
    private final Integer maxBufferSize;
    private final Duration maxBatchOpenDuration;
    private final Duration visibilityTimeout;
    private final Duration longPollWaitTimeout;
    private final Duration minReceiveWaitTime;
    private final Integer maxDoneReceiveBatches;
    private final List<MessageSystemAttributeName> messageSystemAttributeNames;
    private final List<String> receiveMessageAttributeNames;
    private final Boolean adaptivePrefetching;
    private final Integer maxInflightReceiveBatches;

    private BatchOverrideConfiguration(Builder builder) {
        this.maxBatchItems = Validate.isPositiveOrNull(builder.maxBatchItems, "maxBatchItems");
        this.maxBatchKeys = Validate.isPositiveOrNull(builder.maxBatchKeys, "maxBatchKeys");
        this.maxBufferSize = Validate.isPositiveOrNull(builder.maxBufferSize, "maxBufferSize");
        this.maxBatchOpenDuration = Validate.isPositiveOrNull(builder.maxBatchOpenDuration, "maxBatchOpenDuration");
        this.visibilityTimeout = Validate.isPositiveOrNull(builder.visibilityTimeout, "visibilityTimeout");
        this.longPollWaitTimeout = Validate.isPositiveOrNull(builder.longPollWaitTimeout, "longPollWaitTimeout");
        this.minReceiveWaitTime = Validate.isPositiveOrNull(builder.minReceiveWaitTime, "minReceiveWaitTime");
        this.messageSystemAttributeNames = builder.messageSystemAttributeNames;
        this.receiveMessageAttributeNames = builder.receiveMessageAttributeNames;
        this.adaptivePrefetching = builder.adaptivePrefetching;
        this.maxInflightReceiveBatches = builder.maxInflightReceiveBatches;
        this.maxDoneReceiveBatches = builder.maxDoneReceiveBatches;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return the optional maximum number of messages that are batched together in a single request.
     */
    public Integer maxBatchItems() {
        return maxBatchItems;
    }

    /**
     * @return the optional maximum number of batchKeys to keep track of.
     */
    public Integer maxBatchKeys() {
        return maxBatchKeys;
    }

    /**
     * @return the maximum number of items to allow to be buffered for each batchKey.
     */
    public Integer maxBufferSize() {
        return maxBufferSize;
    }

    public Integer maxDoneReceiveBatches() {
        return maxDoneReceiveBatches;
    }

    /**
     * @return the optional maximum amount of time that an outgoing call waits to be batched with messages of the same type.
     */
    public Duration maxBatchOpenDuration() {
        return maxBatchOpenDuration;
    }

    /**
     * @return the custom visibility timeout to use when retrieving messages from SQS.
     */
    public Duration visibilityTimeout() {
        return visibilityTimeout;
    }

    /**
     * @return the amount of time, the receive call will block on the server waiting for messages to arrive if the
     * queue is empty when the receive call is first made.
     */
    public Duration longPollWaitTimeout() {
        return longPollWaitTimeout;
    }

    /**
     * @return the minimum wait time for incoming receive message requests.
     */
    public Duration minReceiveWaitTime() {
        return minReceiveWaitTime;
    }

    /**
     * @return the message systemAttribute Name will request {@link ReceiveMessageRequest#messageSystemAttributeNames()}.
     */
    public List<MessageSystemAttributeName> messageSystemAttributeName() {
        return messageSystemAttributeNames;
    }

    /**
     * @return the message attributes receive calls will request.
     */
    public List<String> receiveMessageAttributeNames() {
        return receiveMessageAttributeNames;
    }

    /**
     * @return the behavior for prefetching with respect to the number of in-flight incoming receive requests.
     */
    public Boolean adaptivePrefetching() {
        return adaptivePrefetching;
    }

    /**
     * @return the maximum number of concurrent receive message batches.
     */
    public Integer maxInflightReceiveBatches() {
        return maxInflightReceiveBatches;
    }

    @Override
    public Builder toBuilder() {
        return new Builder().maxBatchItems(maxBatchItems)
                            .maxBatchKeys(maxBatchKeys)
                            .maxBufferSize(maxBufferSize)
                            .maxBatchOpenDuration(maxBatchOpenDuration)
                            .visibilityTimeout(visibilityTimeout)
                            .longPollWaitTimeout(longPollWaitTimeout)
                            .minReceiveWaitTime(minReceiveWaitTime)
                            .maxInflightReceiveBatches(maxInflightReceiveBatches)
                            .messageSystemAttributeName(messageSystemAttributeNames)
                            .receiveMessageAttributeNames(receiveMessageAttributeNames)
                            .adaptivePrefetching(adaptivePrefetching)
                            .maxDoneReceiveBatches(maxDoneReceiveBatches);
    }

    @Override
    public String toString() {
        return ToString.builder("BatchOverrideConfiguration")
                       .add("maxBatchItems", maxBatchItems)
                       .add("maxBatchKeys", maxBatchKeys)
                       .add("maxBufferSize", maxBufferSize)
                       .add("maxBatchOpenDuration", maxBatchOpenDuration.toMillis())
                       .add("visibilityTimeout", visibilityTimeout)
                       .add("longPollWaitTimeout", longPollWaitTimeout)
                       .add("minReceiveWaitTime", minReceiveWaitTime)
                       .add("receiveAttributeNames", messageSystemAttributeNames)
                       .add("receiveMessageAttributeNames", receiveMessageAttributeNames)
                       .add("adaptivePrefetching", adaptivePrefetching)
                       .add("maxInflightReceiveBatches", maxInflightReceiveBatches)
                       .add("maxDoneReceiveBatches", maxDoneReceiveBatches)
                       .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        BatchOverrideConfiguration that = (BatchOverrideConfiguration) o;

        if (maxBatchItems != null ? !maxBatchItems.equals(that.maxBatchItems) : that.maxBatchItems != null) {
            return false;
        }
        if (maxBatchKeys != null ? !maxBatchKeys.equals(that.maxBatchKeys) : that.maxBatchKeys != null) {
            return false;
        }
        if (maxBufferSize != null ? !maxBufferSize.equals(that.maxBufferSize) : that.maxBufferSize != null) {
            return false;
        }

        if (maxBatchOpenDuration != null ? !maxBatchOpenDuration.equals(that.maxBatchOpenDuration) :
            that.maxBatchOpenDuration != null) {
            return false;
        }
        if (visibilityTimeout != null ? !visibilityTimeout.equals(that.visibilityTimeout) :
            that.visibilityTimeout != null) {
            return false;
        }
        if (longPollWaitTimeout != null ? !longPollWaitTimeout.equals(that.longPollWaitTimeout) :
            that.longPollWaitTimeout != null) {
            return false;
        }
        if (minReceiveWaitTime != null ? !minReceiveWaitTime.equals(that.minReceiveWaitTime) :
            that.minReceiveWaitTime != null) {
            return false;
        }
        if (messageSystemAttributeNames != null ? !messageSystemAttributeNames.equals(that.messageSystemAttributeNames) :
            that.messageSystemAttributeNames != null) {
            return false;
        }
        if (receiveMessageAttributeNames != null ? !receiveMessageAttributeNames.equals(that.receiveMessageAttributeNames) :
            that.receiveMessageAttributeNames != null) {
            return false;
        }
        if (adaptivePrefetching != null ? !adaptivePrefetching.equals(that.adaptivePrefetching) :
            that.adaptivePrefetching != null) {
            return false;
        }
        if (maxInflightReceiveBatches != null ? !maxInflightReceiveBatches.equals(that.maxInflightReceiveBatches) :
            that.maxInflightReceiveBatches != null) {
            return false;
        }
        return maxDoneReceiveBatches != null ? maxDoneReceiveBatches.equals(that.maxDoneReceiveBatches) :
               that.maxDoneReceiveBatches == null;
    }

    @Override
    public int hashCode() {
        int result = maxBatchItems != null ? maxBatchItems.hashCode() : 0;
        result = 31 * result + (maxBatchKeys != null ? maxBatchKeys.hashCode() : 0);
        result = 31 * result + (maxBufferSize != null ? maxBufferSize.hashCode() : 0);
        result = 31 * result + (maxBatchOpenDuration != null ? maxBatchOpenDuration.hashCode() : 0);
        result = 31 * result + (visibilityTimeout != null ? visibilityTimeout.hashCode() : 0);
        result = 31 * result + (longPollWaitTimeout != null ? longPollWaitTimeout.hashCode() : 0);
        result = 31 * result + (minReceiveWaitTime != null ? minReceiveWaitTime.hashCode() : 0);
        result = 31 * result + (messageSystemAttributeNames != null ? messageSystemAttributeNames.hashCode() : 0);
        result = 31 * result + (receiveMessageAttributeNames != null ? receiveMessageAttributeNames.hashCode() : 0);
        result = 31 * result + (adaptivePrefetching != null ? adaptivePrefetching.hashCode() : 0);
        result = 31 * result + (maxInflightReceiveBatches != null ? maxInflightReceiveBatches.hashCode() : 0);
        result = 31 * result + (maxDoneReceiveBatches != null ? maxDoneReceiveBatches.hashCode() : 0);
        return result;
    }

    public static final class Builder implements CopyableBuilder<Builder, BatchOverrideConfiguration> {

        private Integer maxBatchItems;
        private Integer maxBatchKeys;
        private Integer maxBufferSize;
        private Duration maxBatchOpenDuration;
        private Duration visibilityTimeout;
        private Duration longPollWaitTimeout;
        private Duration minReceiveWaitTime;
        private Integer maxDoneReceiveBatches;
        private Integer maxInflightReceiveBatches;
        private List<MessageSystemAttributeName> messageSystemAttributeNames = Collections.emptyList();
        private List<String> receiveMessageAttributeNames = Collections.emptyList();
        private Boolean adaptivePrefetching;

        private Builder() {
        }

        /**
         * Define the maximum number of messages that are batched together in a single request.
         *
         * @param maxBatchItems The new maxBatchItems value.
         * @return This object for method chaining.
         */
        public Builder maxBatchItems(Integer maxBatchItems) {
            this.maxBatchItems = maxBatchItems;
            return this;
        }

        /**
         * Define the maximum number of batchKeys to keep track of. A batchKey determines which requests are batched together and
         * is calculated by the client based on the information in a request.
         * <p>
         * Ex. SQS determines a batchKey based on a request's queueUrl in combination with its overrideConfiguration, so requests
         * with the same queueUrl and overrideConfiguration will have the same batchKey and be batched together.
         *
         * @param maxBatchKeys the new maxBatchKeys value.
         * @return This object for method chaining.
         */
        public Builder maxBatchKeys(Integer maxBatchKeys) {
            this.maxBatchKeys = maxBatchKeys;
            return this;
        }

        /**
         * Define the maximum number of items to allow to be buffered for each batchKey.
         *
         * @param maxBufferSize the new maxBufferSize value.
         * @return This object for method chaining.
         */
        public Builder maxBufferSize(Integer maxBufferSize) {
            this.maxBufferSize = maxBufferSize;
            return this;
        }

        /**
         * Define the maximum amount of time that an outgoing call waits for other requests before sending out a
         * batch request.
         * TODO : Decide if Ms needs to be added to the name in surface API review meeting
         * @param maxBatchOpenDuration The new maxBatchOpenDuration value.
         * @return This object for method chaining.
         */
        public Builder maxBatchOpenDuration(Duration maxBatchOpenDuration) {
            this.maxBatchOpenDuration = maxBatchOpenDuration;
            return this;
        }

        /**
         * Define the custom visibility timeout to use when retrieving messages from SQS. If set to a value greater than zero,
         * this timeout will override the default visibility timeout set on the SQS queue. Set it to -1 to use the default
         * visibility timeout of the queue. Visibility timeout of 0 seconds is not supported.
         *
         * @param visibilityTimeout The new visibilityTimeout value.
         * @return This object for method chaining.
         */
        public Builder visibilityTimeout(Duration visibilityTimeout) {
            this.visibilityTimeout = visibilityTimeout;
            return this;
        }

        /**
         * Define the amount of time, the receive call will block on the server waiting for messages to arrive if the
         * queue is empty when the receive call is first made. This setting has no effect if long polling is disabled.
         *
         * @param longPollWaitTimeout The new longPollWaitTimeout value.
         * @return This object for method chaining.
         */
        public Builder longPollWaitTimeout(Duration longPollWaitTimeout) {
            this.longPollWaitTimeout = longPollWaitTimeout;
            return this;
        }

        /**
         * Define the minimum wait time for incoming receive message requests. Without a non-zero minimum wait time, threads can
         * easily waste CPU time busy-waiting against empty local buffers. Avoid setting this to 0 unless you are confident
         * threads will do useful work in-between each call to receive messages!
         *
         * @param minReceiveWaitTime The new minReceiveWaitTime value.
         * @return This object for method chaining.
         */
        public Builder minReceiveWaitTime(Duration minReceiveWaitTime) {
            this.minReceiveWaitTime = minReceiveWaitTime;
            return this;
        }

        /**
         * Define the maximum number of concurrent receive message batches. The greater this number, the faster the queue will be
         * pulling messages from the SQS servers (at the expense of consuming more threads).
         *
         * @param maxInflightReceiveBatches The new maxInflightReceiveBatches value.
         * @return This object for method chaining.
         */
        public Builder maxInflightReceiveBatches(Integer maxInflightReceiveBatches) {
            this.maxInflightReceiveBatches = maxInflightReceiveBatches;
            return this;
        }

        /**
         * Define the maximum number of done receive batches. If more than that number of completed receive batches are waiting in
         * the buffer, the querying for new messages will stop. The larger this number, the more messages the buffer queue will
         * pre-fetch and keep in the buffer on the client side, and the faster receive requests will be satisfied. The visibility
         * timeout of a pre-fetched message starts at the point of pre-fetch, which means that while the message is in the local
         * buffer it is unavailable for other clients to process, and when this client retrieves it, part of the visibility
         * timeout may have already expired. The number of messages prefetched will not exceed maxBatchSize *
         * maxDoneReceiveBatches.
         *
         * @param maxDoneReceiveBatches The new maxDoneReceiveBatches value.
         * @return This object for method chaining.
         */
        public Builder maxDoneReceiveBatches(Integer maxDoneReceiveBatches) {
            this.maxDoneReceiveBatches = maxDoneReceiveBatches;
            return this;
        }

        /**
         * Defines the list of message system attribute names that should be requested in receive message calls.
         * Only receive message requests that request the same set of message system attributes will be satisfied
         * from the receive buffers. Refer to {@link ReceiveMessageRequest#messageSystemAttributeNames()}.
         *
         * Note that if requests to the BatchManager are sent with different messageSystemAttributeNames than what is
         * configured, the request will bypass the BatchManager and will instead make a direct call to SQS.
         *
         * @param messageSystemAttributeNames The list of message system attribute names to be requested.
         *                                    If null, an empty list will be used.
         * @return This builder object for method chaining.
         */
        public Builder messageSystemAttributeName(List<MessageSystemAttributeName> messageSystemAttributeNames) {
            this.messageSystemAttributeNames = messageSystemAttributeNames != null ?
                                               Collections.unmodifiableList(messageSystemAttributeNames) :
                                               Collections.emptyList() ;
            return this;
        }

        /**
         * Defines the message attributes that receive calls will request. Only receive message requests that request the same set
         * of message attributes will be satisfied from the Receive buffer .Refer to
         * {@link ReceiveMessageRequest#messageAttributeNames()}.
         * <p>
         * Note that if requests to the BatchManager are sent with different receiveMessageAttributeNames than what is configured,
         * the request will bypass the BatchManager and will instead make a direct call to SQS.
         *
         * @param receiveMessageAttributeNames The list of message attributes to be requested. If null, an empty list will be
         *                                     used.
         * @return This builder object for method chaining.
         */
        public Builder receiveMessageAttributeNames(List<String> receiveMessageAttributeNames) {
            this.receiveMessageAttributeNames = receiveMessageAttributeNames != null ?
                                                Collections.unmodifiableList(receiveMessageAttributeNames) :
                                                Collections.emptyList();
            return this;
        }

        /**
         * Define the behavior for prefetching with respect to the number of in-flight incoming receive requests made to the
         * client. The advantage of this is reducing the number of outgoing requests made to SQS when incoming requests are
         * reduced: in particular, if all incoming requests stop no future requests to SQS will be made. The disadvantage is
         * increased latency when incoming requests first start occurring.
         *
         * @param adaptivePrefetching The new adaptivePrefetching value.
         * @return This object for method chaining.
         */
        public Builder adaptivePrefetching(Boolean adaptivePrefetching) {
            this.adaptivePrefetching = adaptivePrefetching;
            return this;
        }

        public BatchOverrideConfiguration build() {
            return new BatchOverrideConfiguration(this);
        }
    }
}
