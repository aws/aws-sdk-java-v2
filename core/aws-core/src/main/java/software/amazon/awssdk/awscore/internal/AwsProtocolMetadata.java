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

package software.amazon.awssdk.awscore.internal;

import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.SdkProtocolMetadata;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Contains AWS-specific protocol metadata. Implementation of {@link SdkProtocolMetadata}.
 *
 * <p>
 * Implementation notes: this class should've been outside internal package,
 * but we can't fix it due to backwards compatibility reasons.
 */
@SdkProtectedApi
public final class AwsProtocolMetadata implements SdkProtocolMetadata, ToCopyableBuilder<AwsProtocolMetadata.Builder,
    AwsProtocolMetadata> {

    private final AwsServiceProtocol serviceProtocol;

    private AwsProtocolMetadata(Builder builder) {
        this.serviceProtocol = builder.serviceProtocol;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Builder toBuilder() {
        return new Builder(this);
    }

    @Override
    public String serviceProtocol() {
        return serviceProtocol.toString();
    }

    @Override
    public String toString() {
        return ToString.builder("AwsProtocolMetadata")
                       .add("serviceProtocol", serviceProtocol)
                       .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AwsProtocolMetadata protocolMetadata = (AwsProtocolMetadata) o;
        return serviceProtocol == protocolMetadata.serviceProtocol;
    }

    @Override
    public int hashCode() {
        return serviceProtocol != null ? serviceProtocol.hashCode() : 0;
    }

    public static final class Builder implements CopyableBuilder<Builder, AwsProtocolMetadata> {

        private AwsServiceProtocol serviceProtocol;

        private Builder() {
        }

        private Builder(AwsProtocolMetadata protocolMetadata) {
            this.serviceProtocol = protocolMetadata.serviceProtocol;
        }

        public Builder serviceProtocol(AwsServiceProtocol serviceProtocol) {
            this.serviceProtocol = serviceProtocol;
            return this;
        }

        @Override
        public AwsProtocolMetadata build() {
            return new AwsProtocolMetadata(this);
        }
    }
}
