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

package software.amazon.awssdk.auth.signer.params;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.checksums.Algorithm;
import software.amazon.awssdk.utils.Validate;

/**
 * Encapsulates the Checksum information like Algorithm and header name for the checksum in header/trailer locations.
 */
@SdkPublicApi
public class SignerChecksumParams {


    private final Algorithm algorithm;
    private final String checksumHeaderName;
    private final boolean isStreamingRequest;

    private SignerChecksumParams(Builder builder) {
        Validate.notNull(builder.algorithm, "algorithm is null");
        Validate.notNull(builder.checksumHeaderName, "checksumHeaderName is null");
        this.algorithm = builder.algorithm;
        this.checksumHeaderName = builder.checksumHeaderName;
        this.isStreamingRequest = builder.isStreamingRequest;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return {@code Algorithm} that will be used to compute the checksum.
     */
    public Algorithm algorithm() {
        return algorithm;
    }

    /**
     *
     * @return Header name for the checksum.
     */
    public String checksumHeaderName() {
        return checksumHeaderName;
    }

    /**
     *
     * @return true if the checksum is for a streaming request member.
     */
    public boolean isStreamingRequest() {
        return isStreamingRequest;
    }

    public static final class Builder {
        private Algorithm algorithm;
        private String checksumHeaderName;
        private boolean isStreamingRequest;


        private Builder() {
        }

        /**
         * @param algorithm {@code Algorithm} that will be used to compute checksum for the content.
         * @return this builder for method chaining.
         */
        public Builder algorithm(Algorithm algorithm) {
            this.algorithm = algorithm;
            return this;
        }

        /**
         *
         * @param checksumHeaderName Header name for the Checksum.
         * @return this builder for method chaining.
         */
        public Builder checksumHeaderName(String checksumHeaderName) {
            this.checksumHeaderName = checksumHeaderName;
            return this;
        }

        /**
         *
         * @param isStreamingRequest True if the request has a streaming memberin it.
         * @return this builder for method chaining.
         */
        public Builder isStreamingRequest(boolean isStreamingRequest) {
            this.isStreamingRequest = isStreamingRequest;
            return this;
        }

        /**
         * Builds an instance of {@link SignerChecksumParams}.
         *
         * @return New instance of {@link SignerChecksumParams}.
         */
        public SignerChecksumParams build() {
            return new SignerChecksumParams(this);
        }

    }
}
