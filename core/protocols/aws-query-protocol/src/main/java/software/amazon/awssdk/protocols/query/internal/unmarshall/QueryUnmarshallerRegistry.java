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
import software.amazon.awssdk.protocols.core.AbstractMarshallingRegistry;

/**
 * Registry of {@link QueryUnmarshaller} implementations by location and type.
 */
@SdkInternalApi
public final class QueryUnmarshallerRegistry extends AbstractMarshallingRegistry {

    private QueryUnmarshallerRegistry(Builder builder) {
        super(builder);
    }

    @SuppressWarnings("unchecked")
    public <T> QueryUnmarshaller<Object> getUnmarshaller(MarshallLocation marshallLocation, MarshallingType<T> marshallingType) {
        return (QueryUnmarshaller<Object>) super.get(marshallLocation, marshallingType);
    }

    /**
     * @return Builder instance to construct a {@link QueryUnmarshallerRegistry}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for a {@link QueryUnmarshallerRegistry}.
     */
    public static final class Builder extends AbstractMarshallingRegistry.Builder {

        private Builder() {
        }

        public <T> Builder unmarshaller(MarshallingType<T> marshallingType,
                                        QueryUnmarshaller<T> marshaller) {
            register(MarshallLocation.PAYLOAD, marshallingType, marshaller);
            return this;
        }

        /**
         * @return An immutable {@link QueryUnmarshallerRegistry} object.
         */
        public QueryUnmarshallerRegistry build() {
            return new QueryUnmarshallerRegistry(this);
        }
    }
}
