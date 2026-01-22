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

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.protocol.MarshallingKnownType;
import software.amazon.awssdk.core.protocol.MarshallingType;

/**
 * Base class for marshaller/unmarshaller registry implementations.
 */
@SdkProtectedApi
public abstract class AbstractMarshallingRegistry {
    private final MarshallingLocationRegistry locationRegistry;
    private final Set<MarshallingType<?>> marshallingTypes;
    private final Map<Class<?>, MarshallingType<?>> marshallingTypeCache;

    protected AbstractMarshallingRegistry(Builder builder) {
        this.locationRegistry = builder.locationRegistry.build();
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
        return locationRegistry.get(marshallLocation, marshallingType);
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
        private final MarshallingLocationRegistry.Builder locationRegistry;
        private final Set<MarshallingType<?>> marshallingTypes = new HashSet<>();

        protected Builder() {
            this.locationRegistry = MarshallingLocationRegistry.builder();
        }

        protected Builder(AbstractMarshallingRegistry marshallingRegistry) {
            this.locationRegistry = marshallingRegistry.locationRegistry.toBuilder();
            this.marshallingTypes.addAll(marshallingRegistry.marshallingTypes);
        }

        protected <T> Builder register(MarshallLocation marshallLocation,
                                       MarshallingType<T> marshallingType,
                                       Object marshaller) {
            marshallingTypes.add(marshallingType);
            locationRegistry.register(marshallLocation, marshallingType, marshaller);
            return this;
        }
    }

    /**
     * A registry of marshaller per marshalling location and marshalling type.
     */
    static class MarshallingLocationRegistry {
        private final Map<MarshallLocation, MarshallingTypeRegistry> registry;

        private MarshallingLocationRegistry(Builder builder) {
            this.registry = new EnumMap<>(MarshallLocation.class);
            builder.registry.forEach((k, v) -> this.registry.put(k, v.build()));
        }

        public Object get(MarshallLocation location, MarshallingType<?> marshallingType) {
            MarshallingTypeRegistry byLocation = registry.get(location);
            if (byLocation != null) {
                return byLocation.get(marshallingType);
            }
            return null;
        }

        public static Builder builder() {
            return new Builder();
        }

        public Builder toBuilder() {
            return new Builder(this);
        }

        static class Builder {
            private Map<MarshallLocation, MarshallingTypeRegistry.Builder> registry = new EnumMap<>(MarshallLocation.class);

            private Builder() {
            }

            private Builder(MarshallingLocationRegistry marshallingLocationRegistry) {
                marshallingLocationRegistry.registry.forEach((k, v) -> this.registry.put(k, v.toBuilder()));
            }

            public <T> Builder register(
                MarshallLocation location,
                MarshallingType<T> marshallingType,
                Object marshaller
            ) {
                registry.computeIfAbsent(location, x -> MarshallingTypeRegistry.builder())
                    .register(marshallingType, marshaller);
                return this;
            }

            public MarshallingLocationRegistry build() {
                return new MarshallingLocationRegistry(this);
            }
        }
    }

    /**
     * A registry of marshaller per marshalling type. It keeps two maps, the level 1 is faster as it uses
     * an {@link EnumMap} for the known marshalling types. The level 2 one is slower, but it can be used
     * for other, not known marshalling types.
     */
    static class MarshallingTypeRegistry {
        private final Map<MarshallingKnownType, Object> l1registry;
        private final Map<MarshallingType<?>, Object> l2registry;

        private MarshallingTypeRegistry(Builder builder) {
            this.l1registry = new EnumMap<>(builder.l1registry);
            this.l2registry = builder.l2registry;
        }


        public Object get(MarshallingType<?> marshallingType) {
            MarshallingKnownType knownType = marshallingType.getKnownType();
            if (knownType != null) {
                return l1registry.get(knownType);
            }
            return l2registry.get(marshallingType);
        }

        public static Builder builder() {
            return new Builder();
        }

        public Builder toBuilder() {
            return new Builder(this);
        }

        static class Builder {
            private Map<MarshallingKnownType, Object> l1registry = new EnumMap<>(MarshallingKnownType.class);
            private Map<MarshallingType<?>, Object> l2registry = new HashMap<>();

            private Builder() {
            }

            private Builder(MarshallingTypeRegistry marshallingTypeRegistry) {
                this.l1registry.putAll(marshallingTypeRegistry.l1registry);
                this.l2registry.putAll(marshallingTypeRegistry.l2registry);
            }

            public <T> Builder register(
                MarshallingType<T> marshallingType,
                Object marshaller
            ) {
                MarshallingKnownType knownType = marshallingType.getKnownType();
                if (knownType != null) {
                    l1registry.put(knownType, marshaller);
                } else {
                    l2registry.put(marshallingType, marshaller);
                }
                return this;
            }

            public MarshallingTypeRegistry build() {
                return new MarshallingTypeRegistry(this);
            }
        }
    }
}
