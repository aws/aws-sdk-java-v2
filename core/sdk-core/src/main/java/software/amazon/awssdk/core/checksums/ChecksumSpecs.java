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
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Defines all the Specifications that are required while adding HttpChecksum to a request and validating HttpChecksum of a
 * response.
 */
@SdkInternalApi
public class ChecksumSpecs {

    private final Algorithm algorithm;
    private final String headerName;
    private final List<Algorithm> responseValidationAlgorithms;
    private final boolean isValidationEnabled;
    private final boolean isRequestChecksumRequired;
    private final boolean isRequestStreaming;

    private ChecksumSpecs(Builder builder) {
        this.algorithm = builder.algorithm;
        this.headerName = builder.headerName;
        this.responseValidationAlgorithms = builder.responseValidationAlgorithms;
        this.isValidationEnabled = builder.isValidationEnabled;
        this.isRequestChecksumRequired = builder.isRequestChecksumRequired;
        this.isRequestStreaming = builder.isRequestStreaming;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Algorithm algorithm() {
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

    public List<Algorithm> responseValidationAlgorithms() {
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
               '}';
    }

    public static final class Builder {
        private Algorithm algorithm;
        private String headerName;
        private List<Algorithm> responseValidationAlgorithms;
        private boolean isValidationEnabled;
        private boolean isRequestChecksumRequired;
        private boolean isRequestStreaming;

        private Builder() {
        }

        public Builder algorithm(Algorithm algorithm) {
            this.algorithm = algorithm;
            return this;
        }

        public Builder headerName(String headerName) {
            this.headerName = headerName;
            return this;
        }

        public Builder responseValidationAlgorithms(List<Algorithm> responseValidationAlgorithms) {
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

        public ChecksumSpecs build() {
            return new ChecksumSpecs(this);
        }
    }
}
