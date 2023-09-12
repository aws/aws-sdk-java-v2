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

package software.amazon.awssdk.core.internal.interceptor.trait;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public final class RequestCompression {

    private List<String> encodings;
    private boolean isStreaming;

    private RequestCompression(Builder builder) {
        this.encodings = builder.encodings;
        this.isStreaming = builder.isStreaming;
    }

    public List<String> getEncodings() {
        return encodings;
    }

    public boolean isStreaming() {
        return isStreaming;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private List<String> encodings;
        private boolean isStreaming;

        public Builder encodings(List<String> encodings) {
            this.encodings = encodings;
            return this;
        }

        public Builder encodings(String... encodings) {
            if (encodings != null) {
                this.encodings = Arrays.asList(encodings);
            }
            return this;
        }

        public Builder isStreaming(boolean isStreaming) {
            this.isStreaming = isStreaming;
            return this;
        }

        public RequestCompression build() {
            return new RequestCompression(this);
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
        RequestCompression that = (RequestCompression) o;
        return isStreaming == that.isStreaming()
               && Objects.equals(encodings, that.getEncodings());
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        hashCode = 31 * hashCode + (isStreaming ? 1 : 0);
        hashCode = 31 * hashCode + Objects.hashCode(encodings);
        return hashCode;
    }
}
