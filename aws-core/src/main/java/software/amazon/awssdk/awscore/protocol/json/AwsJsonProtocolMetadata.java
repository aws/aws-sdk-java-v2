/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.awscore.protocol.json;

import software.amazon.awssdk.annotations.SdkProtectedApi;

/**
 * Provides additional metadata about AWS Json protocol.
 */
@SdkProtectedApi
public class AwsJsonProtocolMetadata {

    private final AwsJsonProtocol protocol;
    private final String protocolVersion;

    private AwsJsonProtocolMetadata(Builder builder) {
        this.protocol = builder.protocol;
        this.protocolVersion = builder.protocolVersion;
    }

    /**
     * @return the protocol
     */
    public AwsJsonProtocol protocol() {
        return protocol;
    }

    /**
     * @return the protocol version
     */
    public String protocolVersion() {
        return protocolVersion;
    }

    public static Builder builder() {
        return new AwsJsonProtocolMetadata.Builder();
    }

    public static final class Builder {
        private AwsJsonProtocol protocol;
        private String protocolVersion;

        private Builder() {
        }

        public Builder protocol(AwsJsonProtocol protocol) {
            this.protocol = protocol;
            return this;
        }

        public Builder protocolVersion(String protocolVersion) {
            this.protocolVersion = protocolVersion;
            return this;
        }

        public AwsJsonProtocolMetadata build() {
            return new AwsJsonProtocolMetadata(this);
        }
    }
}
