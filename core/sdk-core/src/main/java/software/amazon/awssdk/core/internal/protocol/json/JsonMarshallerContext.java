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

package software.amazon.awssdk.core.internal.protocol.json;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.Request;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.protocol.ProtocolMarshaller;
import software.amazon.awssdk.core.protocol.json.StructuredJsonGenerator;

/**
 * Dependencies needed by implementations of {@link JsonMarshaller}.
 */
@SdkInternalApi
public final class JsonMarshallerContext {

    private final StructuredJsonGenerator jsonGenerator;
    private final JsonProtocolMarshaller protocolHandler;
    private final MarshallerRegistry marshallerRegistry;
    private final Request<?> request;

    private JsonMarshallerContext(Builder builder) {
        this.jsonGenerator = builder.jsonGenerator;
        this.protocolHandler = builder.protocolHandler;
        this.marshallerRegistry = builder.marshallerRegistry;
        this.request = builder.request;
    }

    /**
     * @return StructuredJsonGenerator used to produce the JSON document for a request.
     */
    public StructuredJsonGenerator jsonGenerator() {
        return jsonGenerator;
    }

    /**
     * @return Implementation of {@link ProtocolMarshaller} that can be used to call back out to marshall structured data (i.e.
     *      dlists of objects).
     */
    public ProtocolMarshaller protocolHandler() {
        return protocolHandler;
    }

    /**
     * @return Marshaller registry to obtain marshaller implementations for nested types (i.e. lists of objects or maps of string
     *     to string).
     */
    public MarshallerRegistry marshallerRegistry() {
        return marshallerRegistry;
    }

    /**
     * @return Mutable {@link Request} object that can be used to add headers, query params, modify request URI, etc.
     */
    public Request<?> request() {
        return request;
    }

    /**
     * Convenience method to marshall a nested object (may be simple or structured) at the given location.
     *
     * @param marshallLocation Current {@link MarshallLocation}
     * @param val              Value to marshall.
     */
    public void marshall(MarshallLocation marshallLocation, Object val) {
        marshallerRegistry().getMarshaller(marshallLocation, val).marshall(val, this, null);
    }

    /**
     * Convenience method to marshall a nested object (may be simple or structured) at the given location.
     *
     * @param marshallLocation Current {@link MarshallLocation}
     * @param val              Value to marshall.
     * @param paramName        Name of parameter to marshall.
     */
    public void marshall(MarshallLocation marshallLocation, Object val, String paramName) {
        marshallerRegistry().getMarshaller(marshallLocation, val).marshall(val, this, paramName);
    }

    /**
     * @return Builder instance to construct a {@link JsonMarshallerContext}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for a {@link JsonMarshallerContext}.
     */
    public static final class Builder {

        private StructuredJsonGenerator jsonGenerator;
        private JsonProtocolMarshaller protocolHandler;
        private MarshallerRegistry marshallerRegistry;
        private Request<?> request;

        private Builder() {
        }

        public Builder jsonGenerator(StructuredJsonGenerator jsonGenerator) {
            this.jsonGenerator = jsonGenerator;
            return this;
        }

        public Builder protocolHandler(JsonProtocolMarshaller protocolHandler) {
            this.protocolHandler = protocolHandler;
            return this;
        }

        public Builder marshallerRegistry(MarshallerRegistry marshallerRegistry) {
            this.marshallerRegistry = marshallerRegistry;
            return this;
        }

        public Builder request(Request<?> request) {
            this.request = request;
            return this;
        }

        /**
         * @return An immutable {@link JsonMarshallerContext} object.
         */
        public JsonMarshallerContext build() {
            return new JsonMarshallerContext(this);
        }
    }
}
