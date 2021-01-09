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

package software.amazon.awssdk.protocols.json.internal.marshall;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.protocols.core.ProtocolMarshaller;
import software.amazon.awssdk.protocols.json.StructuredJsonGenerator;

/**
 * Dependencies needed by implementations of {@link JsonMarshaller}.
 */
@SdkInternalApi
public final class JsonMarshallerContext {

    private final StructuredJsonGenerator jsonGenerator;
    private final JsonProtocolMarshaller protocolHandler;
    private final JsonMarshallerRegistry marshallerRegistry;
    private final SdkHttpFullRequest.Builder request;

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
    public JsonProtocolMarshaller protocolHandler() {
        return protocolHandler;
    }

    /**
     * @return Marshaller registry to obtain marshaller implementations for nested types (i.e. lists of objects or maps of string
     *     to string).
     */
    public JsonMarshallerRegistry marshallerRegistry() {
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
        marshallerRegistry().getMarshaller(marshallLocation, val).marshall(val, this, null, null);
    }

    /**
     * Convenience method to marshall a nested object (may be simple or structured) at the given location.
     *
     * @param marshallLocation Current {@link MarshallLocation}
     * @param val              Value to marshall.
     * @param paramName        Name of parameter to marshall.
     */
    public <T> void marshall(MarshallLocation marshallLocation, T val, String paramName) {
        marshallerRegistry().getMarshaller(marshallLocation, val).marshall(val, this, paramName, null);
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
        private JsonMarshallerRegistry marshallerRegistry;
        private SdkHttpFullRequest.Builder request;

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

        public Builder marshallerRegistry(JsonMarshallerRegistry marshallerRegistry) {
            this.marshallerRegistry = marshallerRegistry;
            return this;
        }

        public Builder request(SdkHttpFullRequest.Builder request) {
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
