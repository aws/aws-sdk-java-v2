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

package software.amazon.awssdk.protocols.xml.internal.marshall;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.http.SdkHttpFullRequest;

@SdkInternalApi
public final class XmlMarshallerContext {

    private final XmlGenerator xmlGenerator;
    private final XmlProtocolMarshaller protocolMarshaller;
    private final XmlMarshallerRegistry marshallerRegistry;
    private final SdkHttpFullRequest.Builder request;

    public XmlMarshallerContext(Builder builder) {
        this.xmlGenerator = builder.xmlGenerator;
        this.protocolMarshaller = builder.protocolMarshaller;
        this.marshallerRegistry = builder.marshallerRegistry;
        this.request = builder.request;
    }

    public XmlGenerator xmlGenerator() {
        return xmlGenerator;
    }

    public XmlProtocolMarshaller protocolMarshaller() {
        return protocolMarshaller;
    }

    /**
     * @return Marshaller registry to obtain marshaller implementations for nested types (i.e. lists of objects or maps of string
     *     to string).
     */
    public XmlMarshallerRegistry marshallerRegistry() {
        return marshallerRegistry;
    }

    /**
     * @return Mutable {@link SdkHttpFullRequest.Builder} object that can be used to add headers, query params,
     * modify request URI, etc.
     */
    public SdkHttpFullRequest.Builder request() {
        return request;
    }


    /**
     * Convenience method to marshall a nested object (may be simple or structured) at the given location.
     *
     * @param marshallLocation Current {@link MarshallLocation}
     * @param val              Value to marshall.
     */
    public void marshall(MarshallLocation marshallLocation, Object val) {
        marshallerRegistry.getMarshaller(marshallLocation, val).marshall(val, this, null, null);
    }

    /**
     * Convenience method to marshall a nested object (may be simple or structured) at the given location.
     *
     * @param marshallLocation Current {@link MarshallLocation}
     * @param val              Value to marshall.
     * @param paramName        Name of parameter to marshall.
     */
    public <T> void marshall(MarshallLocation marshallLocation, T val, String paramName, SdkField<T> sdkField) {
        marshallerRegistry.getMarshaller(marshallLocation, val).marshall(val, this, paramName, sdkField);
    }


    /**
     * @return Builder instance to construct a {@link XmlMarshallerContext}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for a {@link XmlMarshallerContext}.
     */
    public static final class Builder {

        private XmlGenerator xmlGenerator;
        private XmlProtocolMarshaller protocolMarshaller;
        private XmlMarshallerRegistry marshallerRegistry;
        private SdkHttpFullRequest.Builder request;

        private Builder() {
        }

        public Builder xmlGenerator(XmlGenerator xmlGenerator) {
            this.xmlGenerator = xmlGenerator;
            return this;
        }

        public Builder protocolMarshaller(XmlProtocolMarshaller protocolMarshaller) {
            this.protocolMarshaller = protocolMarshaller;
            return this;
        }

        public Builder marshallerRegistry(XmlMarshallerRegistry marshallerRegistry) {
            this.marshallerRegistry = marshallerRegistry;
            return this;
        }

        public Builder request(SdkHttpFullRequest.Builder request) {
            this.request = request;
            return this;
        }

        public XmlMarshallerContext build() {
            return new XmlMarshallerContext(this);
        }
    }
}
