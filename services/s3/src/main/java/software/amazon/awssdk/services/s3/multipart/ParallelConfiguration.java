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

package software.amazon.awssdk.services.s3.multipart;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

@SdkPublicApi
public class ParallelConfiguration implements ToCopyableBuilder<ParallelConfiguration.Builder, ParallelConfiguration> {

    private final Integer maxInFlightParts;

    public ParallelConfiguration(Builder builder) {
        this.maxInFlightParts = builder.maxInFlightParts;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Integer maxInFlightParts() {
        return maxInFlightParts;
    }

    @Override
    public Builder toBuilder() {
        return builder().maxInFlightParts(maxInFlightParts);
    }

    public static class Builder implements CopyableBuilder<Builder, ParallelConfiguration> {
        private int maxInFlightParts;

        public Builder maxInFlightParts(int maxInFlightParts) {
            this.maxInFlightParts = maxInFlightParts;
            return this;
        }

        public int maxInFlightParts() {
            return maxInFlightParts;
        }

        @Override
        public ParallelConfiguration build() {
            return new ParallelConfiguration(this);
        }
    }
}
