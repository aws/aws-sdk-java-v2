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

package software.amazon.awssdk.services.s3.crt;

import java.util.Objects;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.services.s3.S3CrtAsyncClientBuilder;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Retry option configuration for AWS CRT-based S3 client.
 *
 * @see S3CrtAsyncClientBuilder#retryConfiguration
 */
@SdkPublicApi
@Immutable
@ThreadSafe
public final class S3CrtRetryConfiguration implements ToCopyableBuilder<S3CrtRetryConfiguration.Builder,
    S3CrtRetryConfiguration> {
    private final Integer numRetries;

    private S3CrtRetryConfiguration(DefaultBuilder builder) {
        Validate.notNull(builder.numRetries, "numRetries");
        this.numRetries = builder.numRetries;
    }

    /**
     * Creates a default builder for {@link S3CrtRetryConfiguration}.
     */
    public static Builder builder() {
        return new S3CrtRetryConfiguration.DefaultBuilder();
    }

    /**
     * Retrieve the {@link S3CrtRetryConfiguration.Builder#numRetries(Integer)} configured on the builder.
     */
    public Integer numRetries() {
        return numRetries;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        S3CrtRetryConfiguration that = (S3CrtRetryConfiguration) o;
        return Objects.equals(numRetries, that.numRetries);
    }

    @Override
    public int hashCode() {
        return numRetries != null ? numRetries.hashCode() : 0;
    }

    @Override
    public Builder toBuilder() {
        return new S3CrtRetryConfiguration.DefaultBuilder(this);
    }

    public interface Builder extends CopyableBuilder<S3CrtRetryConfiguration.Builder, S3CrtRetryConfiguration> {

        /**
         * Sets the maximum number of retries for a single HTTP request.
         * <p> For example, if an upload operation is split into 5 HTTP service requests ( One for initiate, Three for
         * uploadPart and one for completeUpload), then numRetries specifies the maximum number of retries for each failed
         * request, not for the entire uploadObject operation.
         *
         * @param numRetries The maximum number of retries for a single HTTP request.
         * @return The builder of the method chaining.
         */
        Builder numRetries(Integer numRetries);

    }

    private static final class DefaultBuilder implements Builder {
        private Integer numRetries;

        private DefaultBuilder() {
        }

        private DefaultBuilder(S3CrtRetryConfiguration crtRetryConfiguration) {
            this.numRetries = crtRetryConfiguration.numRetries;
        }

        @Override
        public Builder numRetries(Integer numRetries) {
            this.numRetries = numRetries;
            return this;
        }

        @Override
        public S3CrtRetryConfiguration build() {
            return new S3CrtRetryConfiguration(this);
        }
    }
}
