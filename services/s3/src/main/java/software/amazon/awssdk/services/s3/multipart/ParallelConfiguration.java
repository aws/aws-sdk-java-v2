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
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Class that holds configuration properties related to multipart operations for a {@link S3AsyncClient}, related specifically
 * to non-linear, parallel operations, that is, when the {@link AsyncResponseTransformer} supports non-serial split.
 */
@SdkPublicApi
public class ParallelConfiguration implements ToCopyableBuilder<ParallelConfiguration.Builder, ParallelConfiguration> {

    private final Integer maxInFlightParts;
    private final Integer maxInFlightPutObjectParts;

    public ParallelConfiguration(Builder builder) {
        this.maxInFlightParts = builder.maxInFlightParts;
        this.maxInFlightPutObjectParts = builder.maxInFlightPutObjectParts;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * The maximum number of concurrent GetObject requests that are allowed for multipart download.
     * @return The value for the maximum number of concurrent GetObject requests that are allowed for multipart download.
     */
    public Integer maxInFlightParts() {
        return maxInFlightParts;
    }

    /**
     * The maximum number of concurrent PutObject/UploadPart requests that are allowed for multipart upload. This limits the
     * number of parts that can be in flight at any given time during a multipart putObject operation, preventing the client from
     * overwhelming the HTTP connection pool when uploading large objects.
     *
     * @return The value for the maximum number of concurrent UploadPart requests that are allowed for multipart upload.
     */
    public Integer maxInFlightPutObjectParts() {
        return maxInFlightPutObjectParts;
    }

    @Override
    public Builder toBuilder() {
        return builder().maxInFlightParts(maxInFlightParts)
                        .maxInFlightPutObjectParts(maxInFlightPutObjectParts);
    }

    public static class Builder implements CopyableBuilder<Builder, ParallelConfiguration> {
        private int maxInFlightParts;
        private Integer maxInFlightPutObjectParts;

        public Builder maxInFlightParts(int maxInFlightParts) {
            this.maxInFlightParts = maxInFlightParts;
            return this;
        }

        public int maxInFlightParts() {
            return maxInFlightParts;
        }

        /**
         * Configures the maximum number of concurrent UploadPart requests that are allowed for multipart upload. This limits
         * the number of parts that can be in flight at any given time during a multipart putObject operation, preventing the
         * client from overwhelming the HTTP connection pool when uploading large objects.
         *
         * <p>Default value: 50 (matching the default HTTP client max concurrency)
         *
         * @param maxInFlightPutObjectParts the maximum number of concurrent UploadPart requests
         * @return an instance of this builder.
         */
        public Builder maxInFlightPutObjectParts(Integer maxInFlightPutObjectParts) {
            this.maxInFlightPutObjectParts = maxInFlightPutObjectParts;
            return this;
        }

        public Integer maxInFlightPutObjectParts() {
            return maxInFlightPutObjectParts;
        }

        @Override
        public ParallelConfiguration build() {
            return new ParallelConfiguration(this);
        }
    }
}
