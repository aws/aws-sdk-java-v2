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

package software.amazon.awssdk.protocols.query.internal.marshall;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.protocols.core.ProtocolMarshaller;
import software.amazon.awssdk.protocols.query.internal.unmarshall.QueryProtocolUnmarshaller;

/**
 * Dependencies needed by {@link QueryProtocolUnmarshaller}.
 */
@SdkInternalApi
public final class QueryMarshallerContext {

    private final QueryProtocolMarshaller protocolHandler;
    private final QueryMarshallerRegistry marshallerRegistry;
    private final SdkHttpFullRequest.Builder request;

    private QueryMarshallerContext(Builder builder) {
        this.protocolHandler = builder.protocolHandler;
        this.marshallerRegistry = builder.marshallerRegistry;
        this.request = builder.request;
    }

    /**
     * @return Implementation of {@link ProtocolMarshaller} that can be used to call back out to marshall structured data (i.e.
     * lists of objects).
     */
    public QueryProtocolMarshaller protocolHandler() {
        return protocolHandler;
    }

    /**
     * @return Marshaller registry to obtain marshaller implementations for nested types (i.e. lists of objects or maps of string
     * to string).
     */
    public QueryMarshallerRegistry marshallerRegistry() {
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
     * @return Builder instance to construct a {@link QueryMarshallerContext}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for a {@link QueryMarshallerContext}.
     */
    public static final class Builder {

        private QueryProtocolMarshaller protocolHandler;
        private QueryMarshallerRegistry marshallerRegistry;
        private SdkHttpFullRequest.Builder request;

        private Builder() {
        }

        public Builder protocolHandler(QueryProtocolMarshaller protocolHandler) {
            this.protocolHandler = protocolHandler;
            return this;
        }

        public Builder marshallerRegistry(QueryMarshallerRegistry marshallerRegistry) {
            this.marshallerRegistry = marshallerRegistry;
            return this;
        }

        public Builder request(SdkHttpFullRequest.Builder request) {
            this.request = request;
            return this;
        }

        /**
         * @return An immutable {@link QueryMarshallerContext} object.
         */
        public QueryMarshallerContext build() {
            return new QueryMarshallerContext(this);
        }
    }
}
