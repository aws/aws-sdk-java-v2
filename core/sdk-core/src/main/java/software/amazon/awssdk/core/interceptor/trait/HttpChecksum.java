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

package software.amazon.awssdk.core.interceptor.trait;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.checksums.DefaultChecksumAlgorithm;
import software.amazon.awssdk.checksums.spi.ChecksumAlgorithm;

@SdkProtectedApi
public class HttpChecksum {

    private final boolean requestChecksumRequired;

    private final String requestAlgorithm;

    private final String requestAlgorithmHeader;

    private final String requestValidationMode;

    private final boolean isRequestStreaming;

    private final List<ChecksumAlgorithm> responseAlgorithms;

    private HttpChecksum(Builder builder) {
        this.requestChecksumRequired = builder.requestChecksumRequired;
        this.requestAlgorithm = builder.requestAlgorithm;
        this.requestValidationMode = builder.requestValidationMode;
        this.responseAlgorithms = builder.responseAlgorithms;
        this.isRequestStreaming = builder.isRequestStreaming;
        this.requestAlgorithmHeader = builder.requestAlgorithmHeader;
    }

    public boolean isRequestChecksumRequired() {
        return requestChecksumRequired;
    }

    public String requestAlgorithm() {
        return requestAlgorithm;
    }

    public String requestAlgorithmHeader() {
        return requestAlgorithmHeader;
    }

    /**
     * @deprecated use {@link #responseAlgorithmsV2()} instead
     */
    @Deprecated
    public List<String> responseAlgorithms() {
        if (responseAlgorithms == null) {
            return null;
        }

        return responseAlgorithms.stream().map(ChecksumAlgorithm::algorithmId).collect(Collectors.toList());
    }

    public List<ChecksumAlgorithm> responseAlgorithmsV2() {
        return responseAlgorithms;
    }

    public String requestValidationMode() {
        return requestValidationMode;
    }

    public boolean isRequestStreaming() {
        return isRequestStreaming;
    }

    public static Builder builder() {
        return new Builder();
    }


    public static final class Builder {

        private boolean requestChecksumRequired;

        private String requestAlgorithm;

        private String requestValidationMode;

        private List<ChecksumAlgorithm> responseAlgorithms;

        private boolean isRequestStreaming;

        private String requestAlgorithmHeader;

        public Builder requestChecksumRequired(boolean requestChecksumRequired) {
            this.requestChecksumRequired = requestChecksumRequired;
            return this;
        }

        public Builder requestAlgorithm(String requestAlgorithm) {
            this.requestAlgorithm = requestAlgorithm;
            return this;
        }

        public Builder requestAlgorithmHeader(String requestAlgorithmHeader) {
            this.requestAlgorithmHeader = requestAlgorithmHeader;
            return this;
        }

        public Builder requestValidationMode(String requestValidationMode) {
            this.requestValidationMode = requestValidationMode;
            return this;
        }

        /**
         * @deprecated use {@link #responseAlgorithmsV2(ChecksumAlgorithm...)} instead
         */
        @Deprecated
        public Builder responseAlgorithms(List<String> responseAlgorithms) {
            this.responseAlgorithms = responseAlgorithms.stream()
                                                        .map(algo -> DefaultChecksumAlgorithm.fromValue(algo))
                                                        .collect(Collectors.toList());
            return this;
        }

        /**
         * @deprecated use {@link #responseAlgorithmsV2(ChecksumAlgorithm...)} instead
         */
        @Deprecated
        public Builder responseAlgorithms(String... responseAlgorithms) {
            if (responseAlgorithms != null) {
                return responseAlgorithms(Arrays.asList(responseAlgorithms));
            }
            return this;
        }

        public Builder responseAlgorithmsV2(ChecksumAlgorithm... responseAlgorithms) {
            if (responseAlgorithms != null) {
                this.responseAlgorithms = Arrays.asList(responseAlgorithms);
            }
            return this;
        }

        public Builder isRequestStreaming(boolean isRequestStreaming) {
            this.isRequestStreaming = isRequestStreaming;
            return this;
        }

        public HttpChecksum build() {
            return new HttpChecksum(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        HttpChecksum that = (HttpChecksum) o;
        return requestChecksumRequired == that.requestChecksumRequired
               && isRequestStreaming == that.isRequestStreaming
               && Objects.equals(requestAlgorithm, that.requestAlgorithm)
               && Objects.equals(requestValidationMode, that.requestValidationMode)
               && Objects.equals(responseAlgorithms, that.responseAlgorithms);
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        hashCode = 31 * hashCode + (requestChecksumRequired ? 1 : 0);
        hashCode = 31 * hashCode + (isRequestStreaming ? 1 : 0);
        hashCode = 31 * hashCode + Objects.hashCode(requestAlgorithm);
        hashCode = 31 * hashCode + Objects.hashCode(requestValidationMode);
        hashCode = 31 * hashCode + Objects.hashCode(responseAlgorithms);
        return hashCode;
    }
}
