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

package software.amazon.awssdk.core.internal.async;

import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.Validate;

/**
 * Configuration class containing shared properties for SubAsyncRequestBody implementations.
 */
@SdkInternalApi
public final class SubAsyncRequestBodyConfiguration {
    private final boolean contentLengthKnown;
    private final long maxLength;
    private final int partNumber;
    private final Consumer<Long> onNumBytesReceived;
    private final Consumer<Long> onNumBytesConsumed;
    private final String sourceBodyName;

    private SubAsyncRequestBodyConfiguration(Builder builder) {
        this.contentLengthKnown = Validate.paramNotNull(builder.contentLengthKnown, "contentLengthKnown");
        this.maxLength = Validate.paramNotNull(builder.maxLength, "maxLength");
        this.partNumber = Validate.paramNotNull(builder.partNumber, "partNumber");
        this.onNumBytesReceived = Validate.paramNotNull(builder.onNumBytesReceived, "onNumBytesReceived");
        this.onNumBytesConsumed = Validate.paramNotNull(builder.onNumBytesConsumed, "onNumBytesConsumed");
        this.sourceBodyName = Validate.paramNotNull(builder.sourceBodyName, "sourceBodyName");
    }

    /**
     * Returns a newly initialized builder object for a {@link SubAsyncRequestBodyConfiguration}
     */
    public static Builder builder() {
        return new Builder();
    }

    public boolean contentLengthKnown() {
        return contentLengthKnown;
    }

    public long maxLength() {
        return maxLength;
    }

    public int partNumber() {
        return partNumber;
    }

    public Consumer<Long> onNumBytesReceived() {
        return onNumBytesReceived;
    }

    public Consumer<Long> onNumBytesConsumed() {
        return onNumBytesConsumed;
    }

    public String sourceBodyName() {
        return sourceBodyName;
    }

    public static final class Builder {
        private Boolean contentLengthKnown;
        private Long maxLength;
        private Integer partNumber;
        private Consumer<Long> onNumBytesReceived;
        private Consumer<Long> onNumBytesConsumed;
        private String sourceBodyName;

        private Builder() {
        }

        /**
         * Sets whether the content length is known.
         */
        public Builder contentLengthKnown(Boolean contentLengthKnown) {
            this.contentLengthKnown = contentLengthKnown;
            return this;
        }

        /**
         * Sets the maximum length of the content this AsyncRequestBody can hold.
         */
        public Builder maxLength(Long maxLength) {
            this.maxLength = maxLength;
            return this;
        }

        /**
         * Sets the part number for this request body.
         */
        public Builder partNumber(Integer partNumber) {
            this.partNumber = partNumber;
            return this;
        }

        /**
         * Sets the callback to be invoked when bytes are received.
         */
        public Builder onNumBytesReceived(Consumer<Long> onNumBytesReceived) {
            this.onNumBytesReceived = onNumBytesReceived;
            return this;
        }

        /**
         * Sets the callback to be invoked when bytes are consumed.
         */
        public Builder onNumBytesConsumed(Consumer<Long> onNumBytesConsumed) {
            this.onNumBytesConsumed = onNumBytesConsumed;
            return this;
        }

        /**
         * Sets the source body name for identification.
         */
        public Builder sourceBodyName(String sourceBodyName) {
            this.sourceBodyName = sourceBodyName;
            return this;
        }

        /**
         * Builds a {@link SubAsyncRequestBodyConfiguration} object based on the values held by this builder.
         */
        public SubAsyncRequestBodyConfiguration build() {
            return new SubAsyncRequestBodyConfiguration(this);
        }
    }
}
