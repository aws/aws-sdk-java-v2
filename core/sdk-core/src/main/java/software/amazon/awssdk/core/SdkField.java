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

package software.amazon.awssdk.core;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.core.traits.DefaultValueTrait;
import software.amazon.awssdk.core.traits.LocationTrait;
import software.amazon.awssdk.core.traits.Trait;

/**
 * Metadata about a member in an {@link SdkPojo}. Contains information about how to marshall/unmarshall.
 *
 * @param <TypeT> Java Type of member.
 */
@SdkProtectedApi
public final class SdkField<TypeT> {
    private final String memberName;
    private final MarshallingType<? super TypeT> marshallingType;
    private final MarshallLocation location;
    private final String locationName;
    private final String unmarshallLocationName;
    private final Supplier<SdkPojo> constructor;
    private final BiConsumer<Object, TypeT> setter;
    private final Function<Object, TypeT> getter;
    private final Map<Class<? extends Trait>, Trait> traits;

    private SdkField(Builder<TypeT> builder) {
        this.memberName = builder.memberName;
        this.marshallingType = builder.marshallingType;
        this.traits = new HashMap<>(builder.traits);
        this.constructor = builder.constructor;
        this.setter = builder.setter;
        this.getter = builder.getter;

        // Eagerly dereference location trait since it's so commonly used.
        LocationTrait locationTrait = getTrait(LocationTrait.class);
        this.location = locationTrait.location();
        this.locationName = locationTrait.locationName();
        this.unmarshallLocationName = locationTrait.unmarshallLocationName();
    }

    public String memberName() {
        return memberName;
    }

    /**
     * @return MarshallingType of member. Used primarily for marshaller/unmarshaller lookups.
     */
    public MarshallingType<? super TypeT> marshallingType() {
        return marshallingType;
    }

    /**
     * @return Location the member should be marshalled into (i.e. headers/query/path/payload).
     */
    public MarshallLocation location() {
        return location;
    }

    /**
     * @return The location name to use when marshalling. I.E. the field name of the JSON document, or the header name, etc.
     */
    public String locationName() {
        return locationName;
    }

    /**
     * @return The location name to use when unmarshalling. This is only needed for AWS/Query or EC2 services. All
     * other services should use {@link #locationName} for both marshalling and unmarshalling.
     */
    public String unmarshallLocationName() {
        return unmarshallLocationName;
    }

    public Supplier<SdkPojo> constructor() {
        return constructor;
    }

    /**
     * Gets the trait of the specified class if available.
     *
     * @param clzz Trait class to get.
     * @param <T> Type of trait.
     * @return Trait instance or null if trait is not present.
     */
    @SuppressWarnings("unchecked")
    public <T extends Trait> T getTrait(Class<T> clzz) {
        return (T) traits.get(clzz);
    }

    /**
     * Gets the trait of the specified class if available.
     *
     * @param clzz Trait class to get.
     * @param <T> Type of trait.
     * @return Optional of trait instance.
     */
    @SuppressWarnings("unchecked")
    public <T extends Trait> Optional<T> getOptionalTrait(Class<T> clzz) {
        return Optional.ofNullable((T) traits.get(clzz));
    }

    /**
     * Checks if a given {@link Trait} is present on the field.
     *
     * @param clzz Trait class to check.
     * @return True if trait is present, false if not.
     */
    public boolean containsTrait(Class<? extends Trait> clzz) {
        return traits.containsKey(clzz);
    }

    /**
     * Retrieves the current value of 'this' field from the given POJO. Uses the getter passed into the {@link Builder}.
     *
     * @param pojo POJO to retrieve value from.
     * @return Current value of 'this' field in the POJO.
     */
    private TypeT get(Object pojo) {
        return getter.apply(pojo);
    }

    /**
     * Retrieves the current value of 'this' field from the given POJO. Uses the getter passed into the {@link Builder}. If the
     * current value is null this method will look for the {@link DefaultValueTrait} on the field and attempt to resolve a default
     * value. If the {@link DefaultValueTrait} is not present this just returns null.
     *
     * @param pojo POJO to retrieve value from.
     * @return Current value of 'this' field in the POJO or default value if current value is null.
     */
    public TypeT getValueOrDefault(Object pojo) {
        TypeT val = this.get(pojo);
        DefaultValueTrait trait = getTrait(DefaultValueTrait.class);
        return (trait == null ? val : (TypeT) trait.resolveValue(val));
    }

    /**
     * Sets the given value on the POJO via the setter passed into the {@link Builder}.
     *
     * @param pojo POJO containing field to set.
     * @param val Value of field.
     */
    @SuppressWarnings("unchecked")
    public void set(Object pojo, Object val) {
        setter.accept(pojo, (TypeT) val);
    }

    /**
     * Creates a new instance of {@link Builder} bound to the specified type.
     *
     * @param marshallingType Type of field.
     * @param <TypeT> Type of field. Must be a subtype of the {@link MarshallingType} type param.
     * @return New builder instance.
     */
    public static <TypeT> Builder<TypeT> builder(MarshallingType<? super TypeT> marshallingType) {
        return new Builder<>(marshallingType);
    }

    /**
     * Builder for {@link SdkField}.
     *
     * @param <TypeT> Java type of field.
     */
    public static final class Builder<TypeT> {

        private final MarshallingType<? super TypeT> marshallingType;
        private String memberName;
        private Supplier<SdkPojo> constructor;
        private BiConsumer<Object, TypeT> setter;
        private Function<Object, TypeT> getter;
        private final Map<Class<? extends Trait>, Trait> traits = new HashMap<>();

        private Builder(MarshallingType<? super TypeT> marshallingType) {
            this.marshallingType = marshallingType;
        }

        public Builder<TypeT> memberName(String memberName) {
            this.memberName = memberName;
            return this;
        }

        /**
         * Sets a {@link Supplier} which will create a new <b>MUTABLE</b> instance of the POJO. I.E. this will
         * create the Builder for a given POJO and not the immutable POJO itself.
         *
         * @param constructor Supplier method to create the mutable POJO.
         * @return This object for method chaining.
         */
        public Builder<TypeT> constructor(Supplier<SdkPojo> constructor) {
            this.constructor = constructor;
            return this;
        }

        /**
         * Sets the {@link BiConsumer} which will accept an object and a value and set that value on the appropriate
         * member of the object. This requires a <b>MUTABLE</b> pojo so thus this setter will be on the Builder
         * for the given POJO.
         *
         * @param setter Setter method.
         * @return This object for method chaining.
         */
        public Builder<TypeT> setter(BiConsumer<Object, TypeT> setter) {
            this.setter = setter;
            return this;
        }

        /**
         * Sets the {@link Function} that will accept an object and return the current value of 'this' field on that object.
         * This will typically be a getter on the immutable representation of the POJO and is used mostly during marshalling.
         *
         * @param getter Getter method.
         * @return This object for method chaining.
         */
        public Builder<TypeT> getter(Function<Object, TypeT> getter) {
            this.getter = getter;
            return this;
        }

        /**
         * Attaches one or more traits to the {@link SdkField}. Traits can have additional metadata and behavior that
         * influence how a field is marshalled/unmarshalled.
         *
         * @param traits Traits to attach.
         * @return This object for method chaining.
         */
        public Builder<TypeT> traits(Trait... traits) {
            Arrays.stream(traits).forEach(t -> this.traits.put(t.getClass(), t));
            return this;
        }

        /**
         * @return An immutable {@link SdkField}.
         */
        public SdkField<TypeT> build() {
            return new SdkField<>(this);
        }
    }
}
