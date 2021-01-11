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

package software.amazon.awssdk.protocols.query.internal.unmarshall;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.protocol.MarshallingType;

/**
 * Container for dependencies used during AWS/Query unmarshalling.
 */
@SdkInternalApi
public final class QueryUnmarshallerContext {

    private final QueryUnmarshallerRegistry registry;
    private final QueryProtocolUnmarshaller protocolUnmarshaller;

    private QueryUnmarshallerContext(Builder builder) {
        this.registry = builder.registry;
        this.protocolUnmarshaller = builder.protocolUnmarshaller;
    }

    /**
     * @return Protocol unmarshaller used for unmarshalling nested structs.
     */
    public QueryProtocolUnmarshaller protocolUnmarshaller() {
        return protocolUnmarshaller;
    }

    /**
     * Conveience method to get an unmarshaller from the registry.
     *
     * @param marshallLocation Location of field being unmarshalled.
     * @param marshallingType Type of field being unmarshalled.
     * @param <T> Type of field being unmarshalled.
     * @return Unmarshaller implementation.
     */
    public <T> QueryUnmarshaller<Object> getUnmarshaller(MarshallLocation marshallLocation, MarshallingType<T> marshallingType) {
        return registry.getUnmarshaller(marshallLocation, marshallingType);
    }

    /**
     * @return New {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link QueryUnmarshallerContext}.
     */
    public static final class Builder {

        private QueryUnmarshallerRegistry registry;
        private QueryProtocolUnmarshaller protocolUnmarshaller;

        private Builder() {
        }

        public Builder registry(QueryUnmarshallerRegistry registry) {
            this.registry = registry;
            return this;
        }

        public Builder protocolUnmarshaller(QueryProtocolUnmarshaller protocolUnmarshaller) {
            this.protocolUnmarshaller = protocolUnmarshaller;
            return this;
        }

        public QueryUnmarshallerContext build() {
            return new QueryUnmarshallerContext(this);
        }
    }
}
