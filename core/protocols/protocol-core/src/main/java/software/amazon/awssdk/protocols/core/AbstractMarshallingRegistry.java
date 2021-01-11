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

package software.amazon.awssdk.protocols.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.protocol.MarshallingType;

/**
 * Base class for marshaller/unmarshaller registry implementations.
 */
@SdkProtectedApi
public abstract class AbstractMarshallingRegistry {

    private final Map<MarshallLocation, Map<MarshallingType, Object>> registry;
    private final Set<MarshallingType<?>> marshallingTypes;
    private final Map<Class<?>, MarshallingType<?>> marshallingTypeCache;

    protected AbstractMarshallingRegistry(Builder builder) {
        this.registry = builder.registry;
        this.marshallingTypes = builder.marshallingTypes;
        this.marshallingTypeCache = new HashMap<>(marshallingTypes.size());

    }

    /**
     * Get a registered marshaller/unmarshaller by location and type.
     *
     * @param marshallLocation Location of registered (un)marshaller.
     * @param marshallingType Type of registered (un)marshaller.
     * @return Registered marshaller/unmarshaller.
     * @throws SdkClientException if no marshaller/unmarshaller is registered for the given location and type.
     */
    protected Object get(MarshallLocation marshallLocation, MarshallingType<?> marshallingType) {
        Map<MarshallingType, Object> byLocation = registry.get(marshallLocation);
        if (byLocation == null) {
            throw SdkClientException.create("No marshaller/unmarshaller registered for location " + marshallLocation.name());
        }
        Object registered = byLocation.get(marshallingType);
        if (registered == null) {
            throw SdkClientException.create(String.format("No marshaller/unmarshaller of type %s registered for location %s.",
                                                          marshallingType,
                                                          marshallLocation.name()));
        }
        return registered;
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
     * Builder for a {@link AbstractMarshallingRegistry}.
     */
    public abstract static class Builder {

        private final Map<MarshallLocation, Map<MarshallingType, Object>> registry = new HashMap<>();
        private final Set<MarshallingType<?>> marshallingTypes = new HashSet<>();

        protected Builder() {
        }

        protected <T> Builder register(MarshallLocation marshallLocation,
                                       MarshallingType<T> marshallingType,
                                       Object marshaller) {
            marshallingTypes.add(marshallingType);
            if (!registry.containsKey(marshallLocation)) {
                registry.put(marshallLocation, new HashMap<>());
            }
            registry.get(marshallLocation).put(marshallingType, marshaller);
            return this;
        }
    }
}
