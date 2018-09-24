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

import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.protocol.MarshallingType;

@SdkInternalApi
public final class UnmarshallerRegistry {

    private final Map<MarshallLocation, Map<MarshallingType, JsonUnmarshaller<?>>> unmarshallers;

    private UnmarshallerRegistry(Builder builder) {
        this.unmarshallers = builder.unmarshallers;

    }

    @SuppressWarnings("unchecked")
    public <T> JsonUnmarshaller<Object> getUnmarshaller(MarshallLocation marshallLocation, MarshallingType<T> marshallingType) {
        return (JsonUnmarshaller<Object>) unmarshallers.get(marshallLocation).get(marshallingType);
    }

    /**
     * @return Builder instance to construct a {@link UnmarshallerRegistry}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for a {@link UnmarshallerRegistry}.
     */
    public static final class Builder {

        private final Map<MarshallLocation, Map<MarshallingType, JsonUnmarshaller<?>>> unmarshallers = new HashMap<>();

        private Builder() {
        }

        public <T> Builder payloadUnmarshaller(MarshallingType<T> marshallingType,
                                               JsonUnmarshaller<T> marshaller) {
            return addUnmarshaller(MarshallLocation.PAYLOAD, marshallingType, marshaller);
        }

        public <T> Builder headerUnmarshaller(MarshallingType<T> marshallingType,
                                              JsonUnmarshaller<T> marshaller) {
            return addUnmarshaller(MarshallLocation.HEADER, marshallingType, marshaller);
        }

        private <T> Builder addUnmarshaller(MarshallLocation marshallLocation,
                                            MarshallingType<T> marshallingType,
                                            JsonUnmarshaller<T> marshaller) {
            if (!unmarshallers.containsKey(marshallLocation)) {
                unmarshallers.put(marshallLocation, new HashMap<>());
            }
            unmarshallers.get(marshallLocation).put(marshallingType, marshaller);
            return this;
        }

        /**
         * @return An immutable {@link UnmarshallerRegistry} object.
         */
        public UnmarshallerRegistry build() {
            return new UnmarshallerRegistry(this);
        }
    }
}
