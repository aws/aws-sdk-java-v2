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

package software.amazon.awssdk.core.async;

import java.util.Objects;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Configuration options for {@link AsyncRequestBody#split} to configure how the SDK
 * should split an {@link SdkPublisher}.
 */
@SdkPublicApi
public final class AsyncRequestBodySplitConfiguration implements ToCopyableBuilder<AsyncRequestBodySplitConfiguration.Builder,
    AsyncRequestBodySplitConfiguration> {
    private static final long DEFAULT_CHUNK_SIZE = 2 * 1024 * 1024L;
    private static final long DEFAULT_BUFFER_SIZE = DEFAULT_CHUNK_SIZE * 4;
    private static final AsyncRequestBodySplitConfiguration DEFAULT_CONFIG = builder()
        .bufferSizeInBytes(DEFAULT_BUFFER_SIZE)
        .chunkSizeInBytes(DEFAULT_CHUNK_SIZE)
        .build();
    private final Long chunkSizeInBytes;
    private final Long bufferSizeInBytes;

    private AsyncRequestBodySplitConfiguration(DefaultBuilder builder) {
        this.chunkSizeInBytes = Validate.isPositiveOrNull(builder.chunkSizeInBytes, "chunkSizeInBytes");
        this.bufferSizeInBytes = Validate.isPositiveOrNull(builder.bufferSizeInBytes, "bufferSizeInBytes");
    }

    public static AsyncRequestBodySplitConfiguration defaultConfiguration() {
        return DEFAULT_CONFIG;
    }

    /**
     * The configured chunk size for each divided {@link AsyncRequestBody}.
     */
    public Long chunkSizeInBytes() {
        return chunkSizeInBytes;
    }

    /**
     * The configured maximum buffer size the SDK will use to buffer the content from the source {@link SdkPublisher}.
     */
    public Long bufferSizeInBytes() {
        return bufferSizeInBytes;
    }

    /**
     * Create a {@link Builder}, used to create a {@link AsyncRequestBodySplitConfiguration}.
     */
    public static Builder builder() {
        return new DefaultBuilder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AsyncRequestBodySplitConfiguration that = (AsyncRequestBodySplitConfiguration) o;

        if (!Objects.equals(chunkSizeInBytes, that.chunkSizeInBytes)) {
            return false;
        }
        return Objects.equals(bufferSizeInBytes, that.bufferSizeInBytes);
    }

    @Override
    public int hashCode() {
        int result = chunkSizeInBytes != null ? chunkSizeInBytes.hashCode() : 0;
        result = 31 * result + (bufferSizeInBytes != null ? bufferSizeInBytes.hashCode() : 0);
        return result;
    }

    @Override
    public AsyncRequestBodySplitConfiguration.Builder toBuilder() {
        return new DefaultBuilder(this);
    }

    public interface Builder extends CopyableBuilder<AsyncRequestBodySplitConfiguration.Builder,
        AsyncRequestBodySplitConfiguration>  {

        /**
         * Configures the size for each divided chunk. The last chunk may be smaller than the configured size. The default value
         * is 2MB.
         *
         * @param chunkSizeInBytes the chunk size in bytes
         * @return This object for method chaining.
         */
        Builder chunkSizeInBytes(Long chunkSizeInBytes);

        /**
         * The maximum buffer size the SDK will use to buffer the content from the source {@link SdkPublisher}. The default value
         * is 8MB.
         *
         * @param bufferSizeInBytes the buffer size in bytes
         * @return This object for method chaining.
         */
        Builder bufferSizeInBytes(Long bufferSizeInBytes);
    }

    private static final class DefaultBuilder implements Builder {
        private Long chunkSizeInBytes;
        private Long bufferSizeInBytes;

        private DefaultBuilder(AsyncRequestBodySplitConfiguration asyncRequestBodySplitConfiguration) {
            this.chunkSizeInBytes = asyncRequestBodySplitConfiguration.chunkSizeInBytes;
            this.bufferSizeInBytes = asyncRequestBodySplitConfiguration.bufferSizeInBytes;
        }

        private DefaultBuilder() {

        }

        @Override
        public Builder chunkSizeInBytes(Long chunkSizeInBytes) {
            this.chunkSizeInBytes = chunkSizeInBytes;
            return this;
        }

        @Override
        public Builder bufferSizeInBytes(Long bufferSizeInBytes) {
            this.bufferSizeInBytes = bufferSizeInBytes;
            return this;
        }

        @Override
        public AsyncRequestBodySplitConfiguration build() {
            return new AsyncRequestBodySplitConfiguration(this);
        }
    }
}
