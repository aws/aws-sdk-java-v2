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
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Configuration options for operations with the RequestCompression trait to disable request configuration and set the minimum
 * compression threshold in bytes.
 */
@SdkPublicApi
public final class CompressionConfiguration implements ToCopyableBuilder<CompressionConfiguration.Builder,
    CompressionConfiguration> {

    private final Boolean requestCompressionEnabled;
    private final Integer minimumCompressionThresholdInBytes;

    private CompressionConfiguration(DefaultBuilder builder) {
        this.requestCompressionEnabled = builder.requestCompressionEnabled;
        this.minimumCompressionThresholdInBytes = builder.minimumCompressionThresholdInBytes;
    }

    /**
     * If set, returns true if request compression is enabled, else false if request compression is disabled.
     */
    public Boolean requestCompressionEnabled() {
        return requestCompressionEnabled;
    }

    /**
     * If set, returns the minimum compression threshold in bytes, inclusive, in order to trigger request compression.
     */
    public Integer minimumCompressionThresholdInBytes() {
        return minimumCompressionThresholdInBytes;
    }

    /**
     * Create a {@link CompressionConfiguration.Builder}, used to create a {@link CompressionConfiguration}.
     */
    public static Builder builder() {
        return new DefaultBuilder();
    }

    @Override
    public Builder toBuilder() {
        return new DefaultBuilder(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CompressionConfiguration that = (CompressionConfiguration) o;

        if (!requestCompressionEnabled.equals(that.requestCompressionEnabled)) {
            return false;
        }
        return Objects.equals(minimumCompressionThresholdInBytes, that.minimumCompressionThresholdInBytes);
    }

    @Override
    public int hashCode() {
        int result = requestCompressionEnabled != null ? requestCompressionEnabled.hashCode() : 0;
        result = 31 * result + (minimumCompressionThresholdInBytes != null ? minimumCompressionThresholdInBytes.hashCode() : 0);
        return result;
    }


    public interface Builder extends CopyableBuilder<Builder, CompressionConfiguration> {

        /**
         * Configures whether request compression is enabled or not, for operations that the service has designated as
         * supporting compression. The default value is true.
         *
         * @param requestCompressionEnabled
         * @return This object for method chaining.
         */
        Builder requestCompressionEnabled(Boolean requestCompressionEnabled);

        /**
         * Configures the minimum compression threshold, inclusive, in bytes. A request whose size is less than the threshold
         * will not be compressed, even if the compression trait is present. The default value is 10_240. The value must be
         * non-negative and no greater than 10_485_760.
         *
         * @param minimumCompressionThresholdInBytes
         * @return This object for method chaining.
         */
        Builder minimumCompressionThresholdInBytes(Integer minimumCompressionThresholdInBytes);
    }

    private static final class DefaultBuilder implements Builder {
        private Boolean requestCompressionEnabled;
        private Integer minimumCompressionThresholdInBytes;

        private DefaultBuilder() {
        }

        private DefaultBuilder(CompressionConfiguration compressionConfiguration) {
            this.requestCompressionEnabled = compressionConfiguration.requestCompressionEnabled;
            this.minimumCompressionThresholdInBytes = compressionConfiguration.minimumCompressionThresholdInBytes;
        }

        @Override
        public Builder requestCompressionEnabled(Boolean requestCompressionEnabled) {
            this.requestCompressionEnabled = requestCompressionEnabled;
            return this;
        }

        @Override
        public Builder minimumCompressionThresholdInBytes(Integer minimumCompressionThresholdInBytes) {
            this.minimumCompressionThresholdInBytes = minimumCompressionThresholdInBytes;
            return this;
        }

        @Override
        public CompressionConfiguration build() {
            return new CompressionConfiguration(this);
        }
    }
}
