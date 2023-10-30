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

import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Configuration options for {@link AsyncRequestBody#fromInputStream(AsyncRequestBodyFromInputStreamConfiguration)}
 * to configure how the SDK should create an {@link AsyncRequestBody} from an {@link InputStream}.
 */
@SdkPublicApi
public final class AsyncRequestBodyFromInputStreamConfiguration
    implements ToCopyableBuilder<AsyncRequestBodyFromInputStreamConfiguration.Builder,
    AsyncRequestBodyFromInputStreamConfiguration> {
    private final InputStream inputStream;
    private final Long contentLength;
    private final ExecutorService executor;
    private final Integer maxReadLimit;

    private AsyncRequestBodyFromInputStreamConfiguration(DefaultBuilder builder) {
        this.inputStream = Validate.paramNotNull(builder.inputStream, "inputStream");
        this.contentLength = Validate.isNotNegativeOrNull(builder.contentLength, "contentLength");
        this.maxReadLimit = Validate.isPositiveOrNull(builder.maxReadLimit, "maxReadLimit");
        this.executor = Validate.paramNotNull(builder.executor, "executor");
    }

    /**
     * @return the provided {@link InputStream}.
     */
    public InputStream inputStream() {
        return inputStream;
    }

    /**
     * @return the provided content length.
     */
    public Long contentLength() {
        return contentLength;
    }

    /**
     * @return the provided {@link ExecutorService}.
     */
    public ExecutorService executor() {
        return executor;
    }

    /**
     * @return the provided max read limit used to mark and reset the {@link InputStream}).
     */
    public Integer maxReadLimit() {
        return maxReadLimit;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AsyncRequestBodyFromInputStreamConfiguration that = (AsyncRequestBodyFromInputStreamConfiguration) o;

        if (!Objects.equals(inputStream, that.inputStream)) {
            return false;
        }
        if (!Objects.equals(contentLength, that.contentLength)) {
            return false;
        }
        if (!Objects.equals(executor, that.executor)) {
            return false;
        }
        return Objects.equals(maxReadLimit, that.maxReadLimit);
    }

    @Override
    public int hashCode() {
        int result = inputStream != null ? inputStream.hashCode() : 0;
        result = 31 * result + (contentLength != null ? contentLength.hashCode() : 0);
        result = 31 * result + (executor != null ? executor.hashCode() : 0);
        result = 31 * result + (maxReadLimit != null ? maxReadLimit.hashCode() : 0);
        return result;
    }

    /**
     * Create a {@link Builder}, used to create a {@link AsyncRequestBodyFromInputStreamConfiguration}.
     */
    public static Builder builder() {
        return new DefaultBuilder();
    }


    @Override
    public AsyncRequestBodyFromInputStreamConfiguration.Builder toBuilder() {
        return new DefaultBuilder(this);
    }

    public interface Builder extends CopyableBuilder<AsyncRequestBodyFromInputStreamConfiguration.Builder,
        AsyncRequestBodyFromInputStreamConfiguration>  {

        /**
         * Configures the InputStream.
         *
         * @param inputStream the InputStream
         * @return This object for method chaining.
         */
        Builder inputStream(InputStream inputStream);

        /**
         * Configures the length of the provided {@link InputStream}
         * @param contentLength the content length
         * @return This object for method chaining.
         */
        Builder contentLength(Long contentLength);

        /**
         * Configures the {@link ExecutorService} to perform the blocking data reads.
         *
         * @param executor the executor
         * @return This object for method chaining.
         */
        Builder executor(ExecutorService executor);

        /**
         * Configures max read limit used to mark and reset the {@link InputStream}. This will have no
         * effect if the stream doesn't support mark and reset.
         *
         * <p>
         * By default, it is 128 KiB.
         *
         * @param maxReadLimit the max read limit
         * @return This object for method chaining.
         * @see InputStream#mark(int)
         */
        Builder maxReadLimit(Integer maxReadLimit);
    }

    private static final class DefaultBuilder implements Builder {
        private InputStream inputStream;
        private Long contentLength;
        private ExecutorService executor;
        private Integer maxReadLimit;

        private DefaultBuilder(AsyncRequestBodyFromInputStreamConfiguration asyncRequestBodyFromInputStreamConfiguration) {
            this.inputStream = asyncRequestBodyFromInputStreamConfiguration.inputStream;
            this.contentLength = asyncRequestBodyFromInputStreamConfiguration.contentLength;
            this.executor = asyncRequestBodyFromInputStreamConfiguration.executor;
            this.maxReadLimit = asyncRequestBodyFromInputStreamConfiguration.maxReadLimit;
        }

        private DefaultBuilder() {

        }

        public Builder inputStream(InputStream inputStream) {
            this.inputStream = inputStream;
            return this;
        }

        public Builder contentLength(Long contentLength) {
            this.contentLength = contentLength;
            return this;
        }

        public Builder executor(ExecutorService executor) {
            this.executor = executor;
            return this;
        }

        public Builder maxReadLimit(Integer maxReadLimit) {
            this.maxReadLimit = maxReadLimit;
            return this;
        }

        @Override
        public AsyncRequestBodyFromInputStreamConfiguration build() {
            return new AsyncRequestBodyFromInputStreamConfiguration(this);
        }
    }
}
