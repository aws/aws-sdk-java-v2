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

package software.amazon.awssdk.retries.internal.circuitbreaker;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * The number of tokens in the token bucket after a specific token acquisition succeeds.
 */
@SdkInternalApi
public final class AcquireResponse implements ToCopyableBuilder<AcquireResponse.Builder, AcquireResponse> {
    private final int maxCapacity;
    private final int capacityRequested;
    private final int capacityAcquired;
    private final int capacityRemaining;
    private final boolean acquisitionFailed;

    private AcquireResponse(Builder builder) {
        this.maxCapacity = Validate.notNull(builder.maxCapacity, "maxCapacity");
        this.capacityRequested = Validate.notNull(builder.capacityRequested, "capacityRequested");
        this.capacityAcquired = Validate.notNull(builder.capacityAcquired, "capacityAcquired");
        this.capacityRemaining = Validate.notNull(builder.capacityRemaining, "capacityRemaining");
        this.acquisitionFailed = Validate.notNull(builder.acquisitionFailed, "acquisitionFailed");
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * The max capacity.
     */
    public int maxCapacity() {
        return maxCapacity;
    }

    /**
     * The numbers of token requested by the last token acquisition.
     */
    public int capacityRequested() {
        return capacityRequested;
    }

    /**
     * The number of tokens acquired by the last token acquisition.
     */
    public int capacityAcquired() {
        return capacityAcquired;
    }

    /**
     * The number of tokens in the token bucket.
     */
    public int capacityRemaining() {
        return capacityRemaining;
    }

    /**
     * Returns {@code true} if the requested capacity was not successfully acquired.
     */
    public boolean acquisitionFailed() {
        return acquisitionFailed;
    }

    @Override
    public Builder toBuilder() {
        return new Builder(this);
    }

    public static class Builder implements CopyableBuilder<Builder, AcquireResponse> {
        private Integer maxCapacity;
        private Integer capacityRequested;
        private Integer capacityAcquired;
        private Integer capacityRemaining;
        private Boolean acquisitionFailed;

        private Builder() {
        }

        private Builder(AcquireResponse instance) {
            this.maxCapacity = instance.maxCapacity;
            this.capacityRequested = instance.capacityRequested;
            this.capacityAcquired = instance.capacityAcquired;
            this.capacityRemaining = instance.capacityRemaining;
            this.acquisitionFailed = instance.acquisitionFailed;
        }

        public Builder maxCapacity(Integer maxCapacity) {
            this.maxCapacity = maxCapacity;
            return this;
        }

        public Builder capacityRequested(Integer capacityRequested) {
            this.capacityRequested = capacityRequested;
            return this;
        }

        public Builder capacityAcquired(Integer capacityAcquired) {
            this.capacityAcquired = capacityAcquired;
            return this;
        }

        public Builder capacityRemaining(Integer capacityRemaining) {
            this.capacityRemaining = capacityRemaining;
            return this;
        }

        public Builder acquisitionFailed(Boolean acquisitionFailed) {
            this.acquisitionFailed = acquisitionFailed;
            return this;
        }

        @Override
        public AcquireResponse build() {
            return new AcquireResponse(this);
        }
    }
}
