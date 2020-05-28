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
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.protocols.core.AbstractMarshallingRegistry;

/**
 * Marshaller registry for the AWS Query protocol.
 */
@SdkInternalApi
public final class QueryMarshallerRegistry extends AbstractMarshallingRegistry {

    private QueryMarshallerRegistry(Builder builder) {
        super(builder);
    }

    @SuppressWarnings("unchecked")
    public <T> QueryMarshaller<Object> getMarshaller(T val) {
        MarshallingType<T> marshallingType = toMarshallingType(val);
        return (QueryMarshaller<Object>) get(MarshallLocation.PAYLOAD, marshallingType);
    }

    @SuppressWarnings("unchecked")
    public <T> QueryMarshaller<Object> getMarshaller(MarshallingType<T> marshallingType,
                                                     Object val) {
        return (QueryMarshaller<Object>) get(MarshallLocation.PAYLOAD,
                                                       val == null ? MarshallingType.NULL : marshallingType);
    }

    /**
     * @return Builder instance to construct a {@link AbstractMarshallingRegistry}.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends AbstractMarshallingRegistry.Builder {

        private Builder() {
        }

        /**
         * Registers a marshaller of the given type. Since the Query protocol doesn't support location
         * constraints this uses the location 'PAYLOAD' to register.
         *
         * @param marshallingType Type of marshaller
         * @param marshaller Marshaller implementation.
         * @param <T> Type of marshaller being registered.
         * @return This builder for method chaining.
         */
        public <T> Builder marshaller(MarshallingType<T> marshallingType,
                                      QueryMarshaller<T> marshaller) {
            register(MarshallLocation.PAYLOAD, marshallingType, marshaller);
            return this;
        }


        /**
         * @return An immutable {@link QueryMarshallerRegistry} object.
         */
        public QueryMarshallerRegistry build() {
            return new QueryMarshallerRegistry(this);
        }
    }
}
