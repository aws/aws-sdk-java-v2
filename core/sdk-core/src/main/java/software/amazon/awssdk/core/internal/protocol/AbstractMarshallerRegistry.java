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

package software.amazon.awssdk.core.internal.protocol;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.core.protocol.SdkPojo;

/**
 * Base class for marshaller registry implementations.
 */
@SdkInternalApi
public abstract class AbstractMarshallerRegistry {

    private final Map<MarshallLocation, Map<MarshallingType, Marshaller<?>>> marshallers;
    private final Set<MarshallingType<?>> marshallingTypes;
    private final Map<Class<?>, MarshallingType<?>> marshallingTypeCache;

    protected AbstractMarshallerRegistry(Builder builder) {
        this.marshallers = builder.marshallers;
        this.marshallingTypes = builder.marshallingTypes;
        this.marshallingTypeCache = new HashMap<>(marshallingTypes.size());

    }

    @SuppressWarnings("unchecked")
    protected Marshaller<?> getMarshaller(MarshallLocation marshallLocation, MarshallingType<?> marshallingType) {
        return marshallers.get(marshallLocation).get(marshallingType);
    }

    @SuppressWarnings("unchecked")
    protected <T> MarshallingType<T> toMarshallingType(T val) {
        if (val == null) {
            return (MarshallingType<T>) MarshallingType.NULL;
        } else if (val instanceof SdkPojo) {
            // We don't want to cache every single POJO type so we make a special case of it here.
            return (MarshallingType<T>) MarshallingType.SDK_POJO;
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
     * Builder for a {@link AbstractMarshallerRegistry}.
     */
    public abstract static class Builder {

        private final Map<MarshallLocation, Map<MarshallingType, Marshaller<?>>> marshallers = new HashMap<>();
        private final Set<MarshallingType<?>> marshallingTypes = new HashSet<>();

        protected Builder() {
        }

        protected <T> Builder addMarshaller(MarshallLocation marshallLocation,
                                            MarshallingType<T> marshallingType,
                                            Marshaller<T> marshaller) {
            marshallingTypes.add(marshallingType);
            if (!marshallers.containsKey(marshallLocation)) {
                marshallers.put(marshallLocation, new HashMap<>());
            }
            marshallers.get(marshallLocation).put(marshallingType, marshaller);
            return this;
        }
    }
}
