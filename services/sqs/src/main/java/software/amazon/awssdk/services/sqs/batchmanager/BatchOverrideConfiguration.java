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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Configuration values for the BatchManager Implementation. All values are optional, and default values will be used if they
 * are not specified.
 */
@SdkPublicApi
public final class BatchOverrideConfiguration implements ToCopyableBuilder<BatchOverrideConfiguration.Builder,
    BatchOverrideConfiguration> {

    private final Integer outboundBatchSizeLimit;
    private final Duration outboundBatchWindowDuration;
    private final Duration receiveMessageVisibilityTimeout;
    private final Duration receiveMessageLongPollWaitDuration;
    private final Duration receiveMessageMinWaitTime;
    private final List<MessageSystemAttributeName> receiveMessageSystemAttributeNames;
    private final List<String> receiveMessageAttributeNames;


    private BatchOverrideConfiguration(Builder builder) {
        this.outboundBatchSizeLimit = Validate.isPositiveOrNull(builder.outboundBatchSizeLimit,
                                                                "outboundBatchSizeLimit");
        Validate.isTrue(this.outboundBatchSizeLimit == null || this.outboundBatchSizeLimit <= 10,
                        "A batch can contain up to 10 messages.");

        this.outboundBatchWindowDuration = Validate.isPositiveOrNull(builder.outboundBatchWindowDuration,
                                                                     "outboundBatchWindowDuration");
        this.receiveMessageVisibilityTimeout = Validate.isPositiveOrNull(builder.receiveMessageVisibilityTimeout,
                                                                         "receiveMessageVisibilityTimeout");
        this.receiveMessageLongPollWaitDuration = Validate.isPositiveOrNull(builder.receiveMessageLongPollWaitDuration,
                                                                            "receiveMessageLongPollWaitDuration");
        this.receiveMessageMinWaitTime = Validate.isPositiveOrNull(builder.receiveMessageMinWaitTime,
                                                                   "receiveMessageMinWaitTime");

        this.receiveMessageSystemAttributeNames = builder.receiveMessageSystemAttributeNames != null ?
                                                  Collections.unmodifiableList(
                                                      new ArrayList<>(builder.receiveMessageSystemAttributeNames)) :
                                                  Collections.emptyList();

        this.receiveMessageAttributeNames = builder.receiveMessageAttributeNames != null ?
                                            Collections.unmodifiableList(new ArrayList<>(builder.receiveMessageAttributeNames)) :
                                            Collections.emptyList();
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return the maximum number of items that are batched together in a single outbound request.
     * A batch can contain up to a maximum of 10 messages.
     * The default value is 10.
     */
    public Integer outboundBatchSizeLimit() {
        return outboundBatchSizeLimit;
    }

    /**
     * @return the maximum amount of time that an outgoing call waits to be batched with messages of the same type.
     * The default value is 200 milliseconds.
     */
    public Duration outboundBatchWindowDuration() {
        return outboundBatchWindowDuration;
    }

    /**
     * @return the custom visibility timeout to use when retrieving messages from SQS.
     */
    public Duration receiveMessageVisibilityTimeout() {
        return receiveMessageVisibilityTimeout;
    }

    /**
     * @return the amount of time the receive call will block on the server, waiting for messages to arrive if the
     * queue is empty when the call is initially made.
     */
    public Duration receiveMessageLongPollWaitDuration() {
        return receiveMessageLongPollWaitDuration;
    }

    /**
     * @return the minimum wait time for incoming receive message requests.
     */
    public Duration receiveMessageMinWaitTime() {
        return receiveMessageMinWaitTime;
    }

    /**
     * @return the system attribute names specific to the {@link ReceiveMessageRequest}
     * that will be requested via {@link ReceiveMessageRequest#messageSystemAttributeNames()}.
     */
    public List<MessageSystemAttributeName> receiveMessageSystemAttributeNames() {
        return receiveMessageSystemAttributeNames;
    }

    /**
     * @return the message attribute names that are specific to receive calls
     * and will be requested via {@link ReceiveMessageRequest#messageAttributeNames()}.
     */
    public List<String> receiveMessageAttributeNames() {
        return receiveMessageAttributeNames;
    }


    @Override
    public Builder toBuilder() {
        return new Builder()
            .outboundBatchSizeLimit(outboundBatchSizeLimit)
            .outboundBatchWindowDuration(outboundBatchWindowDuration)
            .receiveMessageVisibilityTimeout(receiveMessageVisibilityTimeout)
            .receiveMessageLongPollWaitDuration(receiveMessageLongPollWaitDuration)
            .receiveMessageMinWaitTime(receiveMessageMinWaitTime)
            .receiveMessageSystemAttributeNames(receiveMessageSystemAttributeNames)
            .receiveMessageAttributeNames(receiveMessageAttributeNames);
    }


    @Override
    public String toString() {
        return ToString.builder("BatchOverrideConfiguration")
                       .add("outboundBatchSizeLimit", outboundBatchSizeLimit)
                       .add("outboundBatchWindowDuration", outboundBatchWindowDuration)
                       .add("receiveMessageVisibilityTimeout", receiveMessageVisibilityTimeout)
                       .add("receiveMessageLongPollWaitDuration", receiveMessageLongPollWaitDuration)
                       .add("receiveMessageMinWaitTime", receiveMessageMinWaitTime)
                       .add("receiveMessageSystemAttributeNames", receiveMessageSystemAttributeNames)
                       .add("receiveMessageAttributeNames", receiveMessageAttributeNames)
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

        if (outboundBatchSizeLimit != null ? !outboundBatchSizeLimit.equals(that.outboundBatchSizeLimit) : that.outboundBatchSizeLimit != null) {
            return false;
        }
        if (outboundBatchWindowDuration != null ? !outboundBatchWindowDuration.equals(that.outboundBatchWindowDuration) :
            that.outboundBatchWindowDuration != null) {
            return false;
        }
        if (receiveMessageVisibilityTimeout != null ? !receiveMessageVisibilityTimeout.equals(that.receiveMessageVisibilityTimeout) :
            that.receiveMessageVisibilityTimeout != null) {
            return false;
        }
        if (receiveMessageLongPollWaitDuration != null ? !receiveMessageLongPollWaitDuration.equals(that.receiveMessageLongPollWaitDuration) :
            that.receiveMessageLongPollWaitDuration != null) {
            return false;
        }
        if (receiveMessageMinWaitTime != null ? !receiveMessageMinWaitTime.equals(that.receiveMessageMinWaitTime) :
            that.receiveMessageMinWaitTime != null) {
            return false;
        }
        if (receiveMessageSystemAttributeNames != null ? !receiveMessageSystemAttributeNames.equals(that.receiveMessageSystemAttributeNames) :
            that.receiveMessageSystemAttributeNames != null) {
            return false;
        }
        return receiveMessageAttributeNames != null ? receiveMessageAttributeNames.equals(that.receiveMessageAttributeNames) :
               that.receiveMessageAttributeNames == null;
    }

    @Override
    public int hashCode() {
        int result = outboundBatchSizeLimit != null ? outboundBatchSizeLimit.hashCode() : 0;
        result = 31 * result + (outboundBatchWindowDuration != null ? outboundBatchWindowDuration.hashCode() : 0);
        result = 31 * result + (receiveMessageVisibilityTimeout != null ? receiveMessageVisibilityTimeout.hashCode() : 0);
        result = 31 * result + (receiveMessageLongPollWaitDuration != null ? receiveMessageLongPollWaitDuration.hashCode() : 0);
        result = 31 * result + (receiveMessageMinWaitTime != null ? receiveMessageMinWaitTime.hashCode() : 0);
        result = 31 * result + (receiveMessageSystemAttributeNames != null ? receiveMessageSystemAttributeNames.hashCode() : 0);
        result = 31 * result + (receiveMessageAttributeNames != null ? receiveMessageAttributeNames.hashCode() : 0);
        return result;
    }

    public static final class Builder implements CopyableBuilder<Builder, BatchOverrideConfiguration> {

        private Integer outboundBatchSizeLimit = 10;
        private Duration outboundBatchWindowDuration = Duration.ofMillis(200);
        private Duration receiveMessageVisibilityTimeout;
        private Duration receiveMessageLongPollWaitDuration;
        private Duration receiveMessageMinWaitTime = Duration.ofMillis(50);
        private List<MessageSystemAttributeName> receiveMessageSystemAttributeNames = Collections.emptyList();
        private List<String> receiveMessageAttributeNames = Collections.emptyList();


        private Builder() {
        }

        /**
         * Specifies the maximum number of items that the buffered client will include in a single outbound batch request.
         * Outbound requests include {@link SendMessageBatchRequest}, {@link ChangeMessageVisibilityBatchRequest},
         * and {@link DeleteMessageBatchRequest}.
         * A batch can contain up to a maximum of 10 messages. The default value is 10.
         *
         * @param outboundBatchSizeLimit The maximum number of items to be batched together in a single request.
         * @return This Builder object for method chaining.
         */
        public Builder outboundBatchSizeLimit(Integer outboundBatchSizeLimit) {
            this.outboundBatchSizeLimit = outboundBatchSizeLimit;
            return this;
        }

        /**
         * Specifies the maximum duration that an outbound batch is held open for additional outbound requests
         * before being sent. Outbound requests include {@link SendMessageBatchRequest},
         * {@link ChangeMessageVisibilityBatchRequest}, and {@link DeleteMessageBatchRequest}. If the
         * {@link #outboundBatchSizeLimit} is reached before this duration, the batch will be sent immediately.
         * The longer this duration, the more time messages have to be added to the batch, which can reduce the
         * number of calls made and increase throughput, but it may also increase average message latency.
         * The default value is 200 milliseconds.
         *
         * @param outboundBatchWindowDuration The new outboundBatchWindowDuration value.
         * @return This Builder object for method chaining.
         */
        public Builder outboundBatchWindowDuration(Duration outboundBatchWindowDuration) {
            this.outboundBatchWindowDuration = outboundBatchWindowDuration;
            return this;
        }

        /**
         * Defines the custom visibility timeout to use when retrieving messages from SQS. If set to a positive value,
         * this timeout will override the default visibility timeout set on the SQS queue. If no value is set,
         * then by default, the visibility timeout of the queue will be used. Only positive values are supported.
         *
         * @param receiveMessageVisibilityTimeout The new visibilityTimeout value.
         * @return This Builder object for method chaining.
         */
        public Builder receiveMessageVisibilityTimeout(Duration receiveMessageVisibilityTimeout) {
            this.receiveMessageVisibilityTimeout = receiveMessageVisibilityTimeout;
            return this;
        }

        /**
         * Specifies the amount of time the receive call will block on the server waiting for messages to arrive if the
         * queue is empty when the receive call is first made. By default, this value is not set, meaning no long
         * polling wait time on the server side.
         *
         * @param receiveMessageLongPollWaitDuration The new longPollWaitTimeout value.
         * @return This Builder object for method chaining.
         */
        public Builder receiveMessageLongPollWaitDuration(Duration receiveMessageLongPollWaitDuration) {
            this.receiveMessageLongPollWaitDuration = receiveMessageLongPollWaitDuration;
            return this;
        }

        /**
         * Configures the minimum wait time for incoming receive message requests. The default value is 50 milliseconds.
         * Without a non-zero minimum wait time, threads can easily waste CPU time by busy-waiting against empty local buffers.
         * Avoid setting this to 0 unless you are confident that threads will perform useful work between each call
         * to receive messages.
         * The call may return sooner than the configured `WaitTimeSeconds` if there are messages in the buffer.
         * If no messages are available and the wait time expires, the call will return an empty message list.
         *
         * @param receiveMessageMinWaitTime The new minimum wait time value.
         * @return This Builder object for method chaining.
         */
        public Builder receiveMessageMinWaitTime(Duration receiveMessageMinWaitTime) {
            this.receiveMessageMinWaitTime = receiveMessageMinWaitTime;
            return this;
        }

        /**
         * Defines the list of message system attribute names to request in receive message calls.
         * If no `messageSystemAttributeNames` are set in the individual request, the ones configured here will be used.
         * <p>
         * Requests with different `messageSystemAttributeNames` than those configured here will bypass the
         * BatchManager and make a direct call to SQS. Only requests with matching attribute names will be
         * batched and fulfilled from the receive buffers.
         *
         * @param receiveMessageSystemAttributeNames The list of message system attribute names to request.
         *                                    If null, an empty list will be used.
         * @return This builder object for method chaining.
         */
        public Builder receiveMessageSystemAttributeNames(List<MessageSystemAttributeName> receiveMessageSystemAttributeNames) {
            this.receiveMessageSystemAttributeNames = receiveMessageSystemAttributeNames != null ?
                                                      new ArrayList<>(receiveMessageSystemAttributeNames) :
                                                      Collections.emptyList();
            return this;
        }

        /**
         * Defines the list of message attribute names to request in receive message calls.
         * If no `receiveMessageAttributeNames` are set in the individual requests, the ones configured here will be used.
         * <p>
         * Requests with different `receiveMessageAttributeNames` than those configured here will bypass the batched and
         * fulfilled from the receive buffers.
         *
         * @param receiveMessageAttributeNames The list of message attribute names to request.
         *                                     If null, an empty list will be used.
         * @return This builder object for method chaining.
         */
        public Builder receiveMessageAttributeNames(List<String> receiveMessageAttributeNames) {
            this.receiveMessageAttributeNames = receiveMessageAttributeNames != null ?
                                                new ArrayList<>(receiveMessageAttributeNames) :
                                                Collections.emptyList();
            return this;
        }

        /**
         * Builds a new {@link BatchOverrideConfiguration} object based on the values set in this builder.
         *
         * @return A new {@link BatchOverrideConfiguration} object.
         */
        public BatchOverrideConfiguration build() {
            return new BatchOverrideConfiguration(this);
        }
    }
}
