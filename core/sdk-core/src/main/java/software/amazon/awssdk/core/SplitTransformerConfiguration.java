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

package software.amazon.awssdk.core;

import java.util.Objects;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.internal.async.SplittingTransformer;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Configuration options for {@link AsyncResponseTransformer#split(SplitTransformerConfiguration)} to configure how the SDK
 * should split the {@link AsyncResponseTransformer}.
 *
 * @see #builder()
 */
@SdkPublicApi
public final class SplitTransformerConfiguration implements ToCopyableBuilder<SplitTransformerConfiguration.Builder,
    SplitTransformerConfiguration> {

    private final Long bufferSize;

    private SplitTransformerConfiguration(DefaultBuilder builder) {
        this.bufferSize = Validate.paramNotNull(builder.bufferSize, "bufferSize");
    }

    /**
     * Create a {@link Builder}, used to create a {@link SplitTransformerConfiguration}.
     */
    public static Builder builder() {
        return new DefaultBuilder();
    }

    /**
     * @return the buffer size
     */
    public Long bufferSize() {
        return bufferSize;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SplitTransformerConfiguration that = (SplitTransformerConfiguration) o;

        return Objects.equals(bufferSize, that.bufferSize);
    }

    @Override
    public int hashCode() {
        return bufferSize != null ? bufferSize.hashCode() : 0;
    }

    @Override
    public Builder toBuilder() {
        return new DefaultBuilder(this);
    }

    public interface Builder extends CopyableBuilder<Builder, SplitTransformerConfiguration> {

        /**
         * Configures the buffer size of the {@link SplittingTransformer}.
         *
         * @param bufferSize the buffer size
         * @return This object for method chaining.
         */
        Builder bufferSize(Long bufferSize);
    }

    private static final class DefaultBuilder implements Builder {
        private Long bufferSize;

        private DefaultBuilder(SplitTransformerConfiguration configuration) {
            this.bufferSize = configuration.bufferSize;
        }

        private DefaultBuilder() {
        }

        @Override
        public Builder bufferSize(Long bufferSize) {
            this.bufferSize = bufferSize;
            return this;
        }

        @Override
        public SplitTransformerConfiguration build() {
            return new SplitTransformerConfiguration(this);
        }
    }
}
