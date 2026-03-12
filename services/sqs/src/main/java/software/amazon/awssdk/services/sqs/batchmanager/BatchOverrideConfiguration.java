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
 * Configuration values for the BatchManager implementation used for controlling batch operations.
 * All values are optional, and default values will be used if they are not specified.
 */
@SdkPublicApi
public final class BatchOverrideConfiguration implements ToCopyableBuilder<BatchOverrideConfiguration.Builder,
    BatchOverrideConfiguration> {

    private final Integer maxBatchSize;
    private final Duration sendRequestFrequency;
    private final Duration receiveMessageVisibilityTimeout;
    private final Duration receiveMessageMinWaitDuration;
    private final List<MessageSystemAttributeName> receiveMessageSystemAttributeNames;
    private final List<String> receiveMessageAttributeNames;


    private BatchOverrideConfiguration(Builder builder) {
        this.maxBatchSize = Validate.isPositiveOrNull(builder.maxBatchSize,
                                                                "maxBatchSize");
        Validate.isTrue(this.maxBatchSize == null || this.maxBatchSize <= 10,
                        "The maxBatchSize must be less than or equal to 10. A batch can contain up to 10 messages.");

        this.sendRequestFrequency = Validate.isPositiveOrNull(builder.sendRequestFrequency,
                                                                     "sendRequestFrequency");
        this.receiveMessageVisibilityTimeout = Validate.isPositiveOrNull(builder.receiveMessageVisibilityTimeout,
                                                                         "receiveMessageVisibilityTimeout");
        this.receiveMessageMinWaitDuration = Validate.isPositiveOrNull(builder.receiveMessageMinWaitDuration,
                                                                   "receiveMessageMinWaitDuration");
        this.receiveMessageSystemAttributeNames =
            builder.receiveMessageSystemAttributeNames == null ? Collections.emptyList() :
            Collections.unmodifiableList(builder.receiveMessageSystemAttributeNames);

        this.receiveMessageAttributeNames =
            builder.receiveMessageAttributeNames == null ? Collections.emptyList() :
            Collections.unmodifiableList(builder.receiveMessageAttributeNames);
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return the maximum number of items that can be batched together in a single outbound SQS request
     *         (e.g., for {@link SendMessageBatchRequest}, {@link ChangeMessageVisibilityBatchRequest}, or
     *         {@link DeleteMessageBatchRequest}). A batch can contain up to a maximum of 10 messages.
     *         The default value is 10.
     */
    public Integer maxBatchSize() {
        return maxBatchSize;
    }

    /**
     * @return the maximum duration an outgoing call waits for additional messages of the same type before being sent.
     *         If the {@link #maxBatchSize()} is reached before this duration, the batch will be sent immediately.
     *         The default value is 200 milliseconds.
     */
    public Duration sendRequestFrequency() {
        return sendRequestFrequency;
    }

    /**
     * @return the custom visibility timeout to use when retrieving messages from SQS. If not set,
     *         the default visibility timeout configured on the SQS queue will be used.
     */
    public Duration receiveMessageVisibilityTimeout() {
        return receiveMessageVisibilityTimeout;
    }

    /**
     * @return the minimum wait time for incoming receive message requests. Without a non-zero minimum wait time,
     *         threads can waste CPU resources busy-waiting for messages. The default value is 50 milliseconds.
     */
    public Duration receiveMessageMinWaitDuration() {
        return receiveMessageMinWaitDuration;
    }

    /**
     * @return the system attribute names to request for {@link ReceiveMessageRequest}. Requests with differing
     *         system attribute names will bypass the batch manager and make a direct call to SQS.
     */
    public List<MessageSystemAttributeName> receiveMessageSystemAttributeNames() {
        return receiveMessageSystemAttributeNames;
    }

    /**
     * @return the message attribute names to request for {@link ReceiveMessageRequest}. Requests with different
     *         message attribute names will bypass the batch manager and make a direct call to SQS.
     */
    public List<String> receiveMessageAttributeNames() {
        return receiveMessageAttributeNames;
    }


    @Override
    public Builder toBuilder() {
        return new Builder()
            .maxBatchSize(maxBatchSize)
            .sendRequestFrequency(sendRequestFrequency)
            .receiveMessageVisibilityTimeout(receiveMessageVisibilityTimeout)
            .receiveMessageMinWaitDuration(receiveMessageMinWaitDuration)
            .receiveMessageSystemAttributeNames(receiveMessageSystemAttributeNames)
            .receiveMessageAttributeNames(receiveMessageAttributeNames);
    }

    @Override
    public String toString() {
        return ToString.builder("BatchOverrideConfiguration")
                       .add("maxBatchSize", maxBatchSize)
                       .add("sendRequestFrequency", sendRequestFrequency)
                       .add("receiveMessageVisibilityTimeout", receiveMessageVisibilityTimeout)
                       .add("receiveMessageMinWaitDuration", receiveMessageMinWaitDuration)
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

        if (maxBatchSize != null ? !maxBatchSize.equals(that.maxBatchSize) : that.maxBatchSize != null) {
            return false;
        }
        if (sendRequestFrequency != null ? !sendRequestFrequency.equals(that.sendRequestFrequency) :
            that.sendRequestFrequency != null) {
            return false;
        }
        if (receiveMessageVisibilityTimeout != null
            ? !receiveMessageVisibilityTimeout.equals(that.receiveMessageVisibilityTimeout) :
            that.receiveMessageVisibilityTimeout != null) {
            return false;
        }
        if (receiveMessageMinWaitDuration != null ? !receiveMessageMinWaitDuration.equals(that.receiveMessageMinWaitDuration) :
            that.receiveMessageMinWaitDuration != null) {
            return false;
        }
        if (receiveMessageSystemAttributeNames != null ?
            !receiveMessageSystemAttributeNames.equals(that.receiveMessageSystemAttributeNames)
                                                       : that.receiveMessageSystemAttributeNames != null) {
            return false;
        }
        return receiveMessageAttributeNames != null ? receiveMessageAttributeNames.equals(that.receiveMessageAttributeNames) :
               that.receiveMessageAttributeNames == null;
    }

    @Override
    public int hashCode() {
        int result = maxBatchSize != null ? maxBatchSize.hashCode() : 0;
        result = 31 * result + (sendRequestFrequency != null ? sendRequestFrequency.hashCode() : 0);
        result = 31 * result + (receiveMessageVisibilityTimeout != null ? receiveMessageVisibilityTimeout.hashCode() : 0);
        result = 31 * result + (receiveMessageMinWaitDuration != null ? receiveMessageMinWaitDuration.hashCode() : 0);
        result = 31 * result + (receiveMessageSystemAttributeNames != null ? receiveMessageSystemAttributeNames.hashCode() : 0);
        result = 31 * result + (receiveMessageAttributeNames != null ? receiveMessageAttributeNames.hashCode() : 0);
        return result;
    }

    public static final class Builder implements CopyableBuilder<Builder, BatchOverrideConfiguration> {

        private Integer maxBatchSize = 10;
        private Duration sendRequestFrequency ;
        private Duration receiveMessageVisibilityTimeout;
        private Duration receiveMessageMinWaitDuration ;
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
         * @param maxBatchSize The maximum number of items to be batched together in a single request.
         * @return This Builder object for method chaining.
         */
        public Builder maxBatchSize(Integer maxBatchSize) {
            this.maxBatchSize = maxBatchSize;
            return this;
        }

        /**
         * Specifies the frequency at which outbound batches are sent.
         * This defines the maximum duration that an outbound batch is held open for additional outbound
         * requests before being sent. Outbound requests include SendMessageBatchRequest,
         * ChangeMessageVisibilityBatchRequest, and DeleteMessageBatchRequest. If the maxBatchSize is reached
         * before this duration, the batch will be sent immediately.
         * Increasing the {@code sendRequestFrequency} gives more time for additional messages to be added to
         * the batch, which can reduce the number of requests and increase throughput. However, a higher
         * frequency may also result in increased average message latency. The default value is 200 milliseconds.
         *
         * @param sendRequestFrequency The new value for the frequency at which outbound requests are sent.
         * @return This Builder object for method chaining.
         */
        public Builder sendRequestFrequency(Duration sendRequestFrequency) {
            this.sendRequestFrequency = sendRequestFrequency;
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
         * Configures the minimum wait time for incoming receive message requests. The default value is 50 milliseconds.
         * Without a non-zero minimum wait time, threads can easily waste CPU time by busy-waiting against empty local buffers.
         * Avoid setting this to 0 unless you are confident that threads will perform useful work between each call
         * to receive messages.
         * The call may return sooner than the configured `WaitTimeSeconds` if there are messages in the buffer.
         * If no messages are available and the wait time expires, the call will return an empty message list.
         *
         * @param receiveMessageMinWaitDuration The new minimum wait time value.
         * @return This Builder object for method chaining.
         */
        public Builder receiveMessageMinWaitDuration(Duration receiveMessageMinWaitDuration) {
            this.receiveMessageMinWaitDuration = receiveMessageMinWaitDuration;
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
