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

@SdkInternalApi
public final class ReleaseResponse implements ToCopyableBuilder<ReleaseResponse.Builder, ReleaseResponse> {
    private final int capacityReleased;
    private final int currentCapacity;
    private final int maxCapacity;

    private ReleaseResponse(Builder builder) {
        this.capacityReleased = Validate.paramNotNull(builder.capacityReleased, "capacityReleased");
        this.currentCapacity = Validate.paramNotNull(builder.currentCapacity, "currentCapacity");
        this.maxCapacity = Validate.paramNotNull(builder.maxCapacity, "maxCapacity");
    }

    /**
     * Returns the capacity released from the request.
     */
    public int capacityReleased() {
        return capacityReleased;
    }

    /**
     * Returns the capacity of the token bucket after the release.
     */
    public int currentCapacity() {
        return currentCapacity;
    }

    /**
     * Returns the max capacity for the token bucket.
     */
    public int maxCapacity() {
        return maxCapacity;
    }

    /**
     * Creates a new builder to build a {@link ReleaseResponse} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Builder toBuilder() {
        return new Builder(this);
    }

    public static class Builder implements CopyableBuilder<Builder, ReleaseResponse> {
        private Integer capacityReleased;
        private Integer currentCapacity;
        private Integer maxCapacity;

        Builder(ReleaseResponse releaseResponse) {
            this.capacityReleased = releaseResponse.capacityReleased;
            this.currentCapacity = releaseResponse.currentCapacity;
            this.maxCapacity = releaseResponse.maxCapacity;
        }

        Builder() {
        }

        public Builder capacityReleased(Integer capacityReleased) {
            this.capacityReleased = capacityReleased;
            return this;
        }

        public Builder currentCapacity(Integer currentCapacity) {
            this.currentCapacity = currentCapacity;
            return this;
        }

        public Builder maxCapacity(Integer maxCapacity) {
            this.maxCapacity = maxCapacity;
            return this;
        }

        public ReleaseResponse build() {
            return new ReleaseResponse(this);
        }

    }
}
