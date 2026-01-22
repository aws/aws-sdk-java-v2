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
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Configuration options for {@link AsyncResponseTransformer#split(SplittingTransformerConfiguration)} to configure how the SDK
 * should split the {@link AsyncResponseTransformer}.
 *
 * @see #builder()
 */
@SdkPublicApi
public final class SplittingTransformerConfiguration implements ToCopyableBuilder<SplittingTransformerConfiguration.Builder,
    SplittingTransformerConfiguration> {

    private final Long bufferSizeInBytes;

    private SplittingTransformerConfiguration(DefaultBuilder builder) {
        this.bufferSizeInBytes = Validate.paramNotNull(builder.bufferSize, "bufferSize");
    }

    /**
     * Create a {@link Builder}, used to create a {@link SplittingTransformerConfiguration}.
     */
    public static Builder builder() {
        return new DefaultBuilder();
    }

    /**
     * @return the buffer size
     */
    public Long bufferSizeInBytes() {
        return bufferSizeInBytes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SplittingTransformerConfiguration that = (SplittingTransformerConfiguration) o;

        return Objects.equals(bufferSizeInBytes, that.bufferSizeInBytes);
    }

    @Override
    public int hashCode() {
        return bufferSizeInBytes != null ? bufferSizeInBytes.hashCode() : 0;
    }

    @Override
    public String toString() {
        return ToString.builder("SplittingTransformerConfiguration")
            .add("bufferSizeInBytes", bufferSizeInBytes)
            .build();
    }

    @Override
    public Builder toBuilder() {
        return new DefaultBuilder(this);
    }

    public interface Builder extends CopyableBuilder<Builder, SplittingTransformerConfiguration> {

        /**
         * Configures the maximum amount of memory in bytes buffered by the {@link SplittingTransformer}.
         *
         * @param bufferSize the buffer size in bytes
         * @return This object for method chaining.
         */
        Builder bufferSizeInBytes(Long bufferSize);
    }

    private static final class DefaultBuilder implements Builder {
        private Long bufferSize;

        private DefaultBuilder(SplittingTransformerConfiguration configuration) {
            this.bufferSize = configuration.bufferSizeInBytes;
        }

        private DefaultBuilder() {
        }

        @Override
        public Builder bufferSizeInBytes(Long bufferSize) {
            this.bufferSize = bufferSize;
            return this;
        }

        @Override
        public SplittingTransformerConfiguration build() {
            return new SplittingTransformerConfiguration(this);
        }
    }
}
