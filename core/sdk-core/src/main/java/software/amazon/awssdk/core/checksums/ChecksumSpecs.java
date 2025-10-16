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

package software.amazon.awssdk.core.checksums;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.checksums.spi.ChecksumAlgorithm;
import software.amazon.awssdk.core.internal.util.HttpChecksumUtils;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Defines all the Specifications that are required while adding HttpChecksum to a request and validating HttpChecksum of a
 * response.
 *
 * <p>
 * Implementor notes: this class is technically not needed, but we can't remove it now for backwards compatibility reasons.
 */
@SdkProtectedApi
public class ChecksumSpecs implements ToCopyableBuilder<ChecksumSpecs.Builder, ChecksumSpecs> {

    private final ChecksumAlgorithm algorithm;
    private final String headerName;
    private final List<ChecksumAlgorithm> responseValidationAlgorithms;
    private final boolean isValidationEnabled;
    private final boolean isRequestChecksumRequired;
    private final boolean isRequestStreaming;
    private final String requestAlgorithmHeader;

    private ChecksumSpecs(Builder builder) {
        this.algorithm = builder.algorithm;
        this.headerName = builder.headerName;
        this.responseValidationAlgorithms = builder.responseValidationAlgorithms;
        this.isValidationEnabled = builder.isValidationEnabled;
        this.isRequestChecksumRequired = builder.isRequestChecksumRequired;
        this.isRequestStreaming = builder.isRequestStreaming;
        this.requestAlgorithmHeader = builder.requestAlgorithmHeader;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * @deprecated use {@link #algorithmV2()} instead
     */
    @Deprecated
    public Algorithm algorithm() {
        return HttpChecksumUtils.toLegacyChecksumAlgorithm(algorithm);
    }

    public ChecksumAlgorithm algorithmV2() {
        return algorithm;
    }

    public String headerName() {
        return headerName;
    }

    public boolean isRequestStreaming() {
        return isRequestStreaming;
    }

    public boolean isValidationEnabled() {
        return isValidationEnabled;
    }

    public boolean isRequestChecksumRequired() {
        return isRequestChecksumRequired;
    }

    public String requestAlgorithmHeader() {
        return requestAlgorithmHeader;
    }

    /**
     * @deprecated use {@link #algorithmV2()} instead
     */
    @Deprecated
    public List<Algorithm> responseValidationAlgorithms() {
        return responseValidationAlgorithms == null ? null :
               responseValidationAlgorithms.stream()
                                            .map(algo -> HttpChecksumUtils.toLegacyChecksumAlgorithm(algo))
                                            .collect(Collectors.toList());
    }

    public List<ChecksumAlgorithm> responseValidationAlgorithmsV2() {
        return responseValidationAlgorithms;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ChecksumSpecs)) {
            return false;
        }
        ChecksumSpecs checksum = (ChecksumSpecs) o;
        return algorithm() == checksum.algorithm() &&
               isRequestStreaming() == checksum.isRequestStreaming() &&
               Objects.equals(headerName(), checksum.headerName()) &&
               Objects.equals(responseValidationAlgorithms(), checksum.responseValidationAlgorithms()) &&
               Objects.equals(isValidationEnabled(), checksum.isValidationEnabled()) &&
               Objects.equals(requestAlgorithmHeader(), checksum.requestAlgorithmHeader()) &&
               Objects.equals(isRequestChecksumRequired(), checksum.isRequestChecksumRequired());
    }

    @Override
    public int hashCode() {
        int result = algorithm != null ? algorithm.hashCode() : 0;
        result = 31 * result + (headerName != null ? headerName.hashCode() : 0);
        result = 31 * result + (responseValidationAlgorithms != null ? responseValidationAlgorithms.hashCode() : 0);
        result = 31 * result + (isValidationEnabled ? 1 : 0);
        result = 31 * result + (isRequestChecksumRequired ? 1 : 0);
        result = 31 * result + (isRequestStreaming ? 1 : 0);
        result = 31 * result + (requestAlgorithmHeader != null ? requestAlgorithmHeader.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ChecksumSpecs{" +
               "algorithm=" + algorithm +
               ", headerName='" + headerName + '\'' +
               ", responseValidationAlgorithms=" + responseValidationAlgorithms +
               ", isValidationEnabled=" + isValidationEnabled +
               ", isRequestChecksumRequired=" + isRequestChecksumRequired +
               ", isStreamingData=" + isRequestStreaming +
               ", requestAlgorithmHeader='" + requestAlgorithmHeader + '\'' +
               '}';
    }

    @Override
    public ChecksumSpecs.Builder toBuilder() {
        return new Builder(this);
    }

    public static final class Builder implements CopyableBuilder<Builder, ChecksumSpecs> {
        private String requestAlgorithmHeader;
        private ChecksumAlgorithm algorithm;
        private String headerName;
        private List<ChecksumAlgorithm> responseValidationAlgorithms;
        private boolean isValidationEnabled;
        private boolean isRequestChecksumRequired;
        private boolean isRequestStreaming;

        private Builder() {
        }

        private Builder(ChecksumSpecs checksumSpecs) {
            this.algorithm = checksumSpecs.algorithm;
            this.headerName = checksumSpecs.headerName;
            this.responseValidationAlgorithms = checksumSpecs.responseValidationAlgorithms;
            this.isValidationEnabled = checksumSpecs.isValidationEnabled;
            this.isRequestChecksumRequired = checksumSpecs.isRequestChecksumRequired;
            this.isRequestStreaming = checksumSpecs.isRequestStreaming;
            this.requestAlgorithmHeader = checksumSpecs.requestAlgorithmHeader;
        }

        /**
         * @deprecated use {@link #algorithmV2(ChecksumAlgorithm)} instead
         */
        @Deprecated
        public Builder algorithm(Algorithm algorithm) {
            this.algorithm = HttpChecksumUtils.toNewChecksumAlgorithm(algorithm);
            return this;
        }

        public Builder algorithmV2(ChecksumAlgorithm algorithm) {
            this.algorithm = algorithm;
            return this;
        }

        public Builder headerName(String headerName) {
            this.headerName = headerName;
            return this;
        }

        /**
         * @deprecated use {@link #responseValidationAlgorithms} instead
         */
        @Deprecated
        public Builder responseValidationAlgorithms(List<Algorithm> responseValidationAlgorithms) {
            this.responseValidationAlgorithms =
                responseValidationAlgorithms == null ? null :
                Collections.unmodifiableList(
                    responseValidationAlgorithms.stream()
                                                .map(algorithm -> HttpChecksumUtils.toNewChecksumAlgorithm(algorithm))
                                                .collect(Collectors.toList()));
            return this;
        }

        public Builder responseValidationAlgorithmsV2(List<ChecksumAlgorithm> responseValidationAlgorithms) {
            this.responseValidationAlgorithms = responseValidationAlgorithms != null
                                                ? Collections.unmodifiableList(responseValidationAlgorithms) : null;
            return this;
        }

        public Builder isValidationEnabled(boolean isValidationEnabled) {
            this.isValidationEnabled = isValidationEnabled;
            return this;
        }

        public Builder isRequestChecksumRequired(boolean isRequestChecksumRequired) {
            this.isRequestChecksumRequired = isRequestChecksumRequired;
            return this;
        }

        public Builder isRequestStreaming(boolean isRequestStreaming) {
            this.isRequestStreaming = isRequestStreaming;
            return this;
        }

        /**
         * Configure the request algorithm header, for example: "x-amz-sdk-checksum-algorithm:CRC32" Note this is different from
         * {@link #headerName(String)} which is the checksum header, "x-amz-checksum-crc32:zc3xbw=="
         */
        public Builder requestAlgorithmHeader(String requestAlgorithmHeader) {
            this.requestAlgorithmHeader = requestAlgorithmHeader;
            return this;
        }

        public ChecksumSpecs build() {
            return new ChecksumSpecs(this);
        }
    }
}
