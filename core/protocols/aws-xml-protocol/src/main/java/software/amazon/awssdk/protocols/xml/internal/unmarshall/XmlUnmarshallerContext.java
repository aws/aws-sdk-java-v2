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

package software.amazon.awssdk.protocols.xml.internal.unmarshall;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.http.SdkHttpFullResponse;

@SdkInternalApi
public final class XmlUnmarshallerContext {

    private final SdkHttpFullResponse response;
    private final XmlUnmarshallerRegistry registry;
    private final XmlProtocolUnmarshaller protocolUnmarshaller;

    private XmlUnmarshallerContext(Builder builder) {
        this.response = builder.response;
        this.registry = builder.registry;
        this.protocolUnmarshaller = builder.protocolUnmarshaller;
    }

    /**
     * @return The {@link SdkHttpFullResponse} of the API call.
     */
    public SdkHttpFullResponse response() {
        return response;
    }

    public XmlProtocolUnmarshaller protocolUnmarshaller() {
        return protocolUnmarshaller;
    }

    public <T> XmlUnmarshaller<Object> getUnmarshaller(MarshallLocation marshallLocation, MarshallingType<T> marshallingType) {
        return registry.getUnmarshaller(marshallLocation, marshallingType);
    }

    /**
     * @return Builder instance to construct a {@link XmlUnmarshallerContext}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for a {@link XmlUnmarshallerContext}.
     */
    public static final class Builder {

        private SdkHttpFullResponse response;
        private XmlUnmarshallerRegistry registry;
        private XmlProtocolUnmarshaller protocolUnmarshaller;

        private Builder() {
        }

        public Builder response(SdkHttpFullResponse response) {
            this.response = response;
            return this;
        }

        public Builder registry(XmlUnmarshallerRegistry registry) {
            this.registry = registry;
            return this;
        }

        public Builder protocolUnmarshaller(XmlProtocolUnmarshaller protocolUnmarshaller) {
            this.protocolUnmarshaller = protocolUnmarshaller;
            return this;
        }

        /**
         * @return An immutable {@link XmlUnmarshallerContext} object.
         */
        public XmlUnmarshallerContext build() {
            return new XmlUnmarshallerContext(this);
        }
    }

}
