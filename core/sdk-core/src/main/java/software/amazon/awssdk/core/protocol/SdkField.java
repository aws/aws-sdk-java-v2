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

package software.amazon.awssdk.core.protocol;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import software.amazon.awssdk.core.protocol.traits.LocationTrait;
import software.amazon.awssdk.core.protocol.traits.Trait;

public class SdkField<TypeT> {

    // Location name (need separate for marshall/unmarshall?)
    // Location (will always be same for marshall/unmarshall)
    // is payload member?
    // Default value supplier -> for idempotency
    // How to get field, how to set field

    private final MarshallingType<? super TypeT> marshallingType;
    private final MarshallLocation location;
    private final String locationName;
    private final Supplier<SdkPojo> constructor;
    private final BiConsumer<Object, TypeT> setter;
    private final Function<Object, TypeT> getter;
    private final Map<Class<? extends Trait>, Trait> traits;

    private SdkField(Builder<TypeT> builder) {
        this.marshallingType = builder.marshallingType;
        this.traits = new HashMap<>(builder.traits);
        LocationTrait locationTrait = getTrait(LocationTrait.class);
        this.location = locationTrait.location();
        this.locationName = locationTrait.locationName();
        this.constructor = builder.constructor;
        this.setter = builder.setter;
        this.getter = builder.getter;
    }

    public MarshallingType<? super TypeT> marshallingType() {
        return marshallingType;
    }

    public MarshallLocation location() {
        return location;
    }

    public String locationName() {
        return locationName;
    }

    public BiConsumer<Object, TypeT> setter() {
        return setter;
    }

    public Supplier<SdkPojo> constructor() {
        return constructor;
    }

    public TypeT get(Object pojo) {
        return getter.apply(pojo);
    }

    @SuppressWarnings("unchecked")
    public <T extends Trait> T getTrait(Class<T> clzz) {
        return (T) traits.get(clzz);
    }

    public boolean containsTrait(Class<? extends Trait> clzz) {
        return traits.containsKey(clzz);
    }

    // TODO type safety
    public void set(Object pojo, Object val) {
        setter.accept(pojo, (TypeT) val);
    }

    public static <TypeT> Builder<TypeT> builder(MarshallingType<? super TypeT> marshallingType) {
        return new Builder<>(marshallingType);
    }

    public static final class Builder<TypeT> {

        private final MarshallingType<? super TypeT> marshallingType;
        private Supplier<SdkPojo> constructor;
        private BiConsumer<Object, TypeT> setter;
        private Function<Object, TypeT> getter;
        private final Map<Class<? extends Trait>, Trait> traits = new HashMap<>();

        private Builder(MarshallingType<? super TypeT> marshallingType) {
            this.marshallingType = marshallingType;
        }

        public Builder<TypeT> constructor(Supplier<SdkPojo> constructor) {
            this.constructor = constructor;
            return this;
        }

        public Builder<TypeT> setter(BiConsumer<Object, TypeT> setter) {
            this.setter = setter;
            return this;
        }

        public Builder<TypeT> getter(Function<Object, TypeT> getter) {
            this.getter = getter;
            return this;
        }

        public Builder<TypeT> traits(Trait... traits) {
            Arrays.stream(traits).forEach(t -> this.traits.put(t.getClass(), t));
            return this;
        }

        public SdkField<TypeT> build() {
            return new SdkField<>(this);
        }
    }
}
