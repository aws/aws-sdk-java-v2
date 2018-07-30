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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.core.protocol.StructuredPojo;

@SdkInternalApi
public final class MarshallerRegistry {

    private final Map<MarshallLocation, Map<MarshallingType, JsonMarshaller<?>>> marshallers;
    private final Set<MarshallingType<?>> marshallingTypes;
    private final Map<Class<?>, MarshallingType<?>> marshallingTypeCache;

    private MarshallerRegistry(Builder builder) {
        this.marshallers = builder.marshallers;
        this.marshallingTypes = builder.marshallingTypes;
        this.marshallingTypeCache = new HashMap<>(marshallingTypes.size());

    }

    public <T> JsonMarshaller<T> getMarshaller(MarshallLocation marshallLocation, T val) {
        return getMarshaller(marshallLocation, toMarshallingType(val));
    }

    public <T> JsonMarshaller<T> getMarshaller(MarshallLocation marshallLocation, MarshallingType<T> marshallingType, T val) {
        return getMarshaller(marshallLocation,
                             val == null ? MarshallingType.NULL : marshallingType);
    }

    @SuppressWarnings("unchecked")
    private <T> JsonMarshaller<T> getMarshaller(MarshallLocation marshallLocation, MarshallingType<?> marshallingType) {
        return (JsonMarshaller<T>) marshallers.get(marshallLocation).get(marshallingType);
    }

    @SuppressWarnings("unchecked")
    public <T> MarshallingType<T> toMarshallingType(T val) {
        if (val == null) {
            return (MarshallingType<T>) MarshallingType.NULL;
        } else if (val instanceof StructuredPojo) {
            // We don't want to cache every single POJO type so we make a special case of it here.
            return (MarshallingType<T>) MarshallingType.STRUCTURED;
        } else if (!marshallingTypeCache.containsKey(val.getClass())) {
            return (MarshallingType<T>) populateMarshallingTypeCache(val.getClass());
        }
        return (MarshallingType<T>) marshallingTypeCache.get(val.getClass());
    }

    private MarshallingType<?> populateMarshallingTypeCache(Class<?> clzz) {
        synchronized (marshallingTypeCache) {
            if (!marshallingTypeCache.containsKey(clzz)) {
                for (MarshallingType<?> marshallingType : marshallingTypes) {
                    if (marshallingType.getTargetClass().isAssignableFrom(clzz)) {
                        marshallingTypeCache.put(clzz, marshallingType);
                        return marshallingType;
                    }
                }
                throw SdkClientException.builder().message("MarshallingType not found for class " + clzz).build();
            }
        }
        return marshallingTypeCache.get(clzz);
    }


    /**
     * @return Builder instance to construct a {@link MarshallerRegistry}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for a {@link MarshallerRegistry}.
     */
    public static final class Builder {

        private final Map<MarshallLocation, Map<MarshallingType, JsonMarshaller<?>>> marshallers = new HashMap<>();
        private final Set<MarshallingType<?>> marshallingTypes = new HashSet<>();

        private Builder() {
        }

        public <T> Builder payloadMarshaller(MarshallingType<T> marshallingType,
                                             JsonMarshaller<T> marshaller) {
            return addMarshaller(MarshallLocation.PAYLOAD, marshallingType, marshaller);
        }

        public <T> Builder headerMarshaller(MarshallingType<T> marshallingType,
                                            JsonMarshaller<T> marshaller) {
            return addMarshaller(MarshallLocation.HEADER, marshallingType, marshaller);
        }

        public <T> Builder queryParamMarshaller(MarshallingType<T> marshallingType,
                                                JsonMarshaller<T> marshaller) {
            return addMarshaller(MarshallLocation.QUERY_PARAM, marshallingType, marshaller);
        }

        public <T> Builder pathParamMarshaller(MarshallingType<T> marshallingType,
                                               JsonMarshaller<T> marshaller) {
            return addMarshaller(MarshallLocation.PATH, marshallingType, marshaller);
        }

        public <T> Builder greedyPathParamMarshaller(MarshallingType<T> marshallingType,
                                                     JsonMarshaller<T> marshaller) {
            return addMarshaller(MarshallLocation.GREEDY_PATH, marshallingType, marshaller);
        }

        private <T> Builder addMarshaller(MarshallLocation marshallLocation,
                                          MarshallingType<T> marshallingType,
                                          JsonMarshaller<T> marshaller) {
            marshallingTypes.add(marshallingType);
            if (!marshallers.containsKey(marshallLocation)) {
                marshallers.put(marshallLocation, new HashMap<>());
            }
            marshallers.get(marshallLocation).put(marshallingType, marshaller);
            return this;
        }

        /**
         * @return An immutable {@link MarshallerRegistry} object.
         */
        public MarshallerRegistry build() {
            return new MarshallerRegistry(this);
        }
    }
}
