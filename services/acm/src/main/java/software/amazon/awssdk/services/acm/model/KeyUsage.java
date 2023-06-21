/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with
 * the License. A copy of the License is located at
 * 
 * http://aws.amazon.com/apache2.0
 * 
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package software.amazon.awssdk.services.acm.model;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.core.traits.LocationTrait;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * <p>
 * The Key Usage X.509 v3 extension defines the purpose of the public key contained in the certificate.
 * </p>
 */
@Generated("software.amazon.awssdk:codegen")
public final class KeyUsage implements SdkPojo, Serializable, ToCopyableBuilder<KeyUsage.Builder, KeyUsage> {
    private static final SdkField<String> NAME_FIELD = SdkField.<String> builder(MarshallingType.STRING).memberName("Name")
            .getter(getter(KeyUsage::nameAsString)).setter(setter(Builder::name))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("Name").build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(NAME_FIELD));

    private static final long serialVersionUID = 1L;

    private final String name;

    private KeyUsage(BuilderImpl builder) {
        this.name = builder.name;
    }

    /**
     * <p>
     * A string value that contains a Key Usage extension name.
     * </p>
     * <p>
     * If the service returns an enum value that is not available in the current SDK version, {@link #name} will return
     * {@link KeyUsageName#UNKNOWN_TO_SDK_VERSION}. The raw value returned by the service is available from
     * {@link #nameAsString}.
     * </p>
     * 
     * @return A string value that contains a Key Usage extension name.
     * @see KeyUsageName
     */
    public final KeyUsageName name() {
        return KeyUsageName.fromValue(name);
    }

    /**
     * <p>
     * A string value that contains a Key Usage extension name.
     * </p>
     * <p>
     * If the service returns an enum value that is not available in the current SDK version, {@link #name} will return
     * {@link KeyUsageName#UNKNOWN_TO_SDK_VERSION}. The raw value returned by the service is available from
     * {@link #nameAsString}.
     * </p>
     * 
     * @return A string value that contains a Key Usage extension name.
     * @see KeyUsageName
     */
    public final String nameAsString() {
        return name;
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    public static Class<? extends Builder> serializableBuilderClass() {
        return BuilderImpl.class;
    }

    @Override
    public final int hashCode() {
        int hashCode = 1;
        hashCode = 31 * hashCode + Objects.hashCode(nameAsString());
        return hashCode;
    }

    @Override
    public final boolean equals(Object obj) {
        return equalsBySdkFields(obj);
    }

    @Override
    public final boolean equalsBySdkFields(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof KeyUsage)) {
            return false;
        }
        KeyUsage other = (KeyUsage) obj;
        return Objects.equals(nameAsString(), other.nameAsString());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public final String toString() {
        return ToString.builder("KeyUsage").add("Name", nameAsString()).build();
    }

    public final <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
        case "Name":
            return Optional.ofNullable(clazz.cast(nameAsString()));
        default:
            return Optional.empty();
        }
    }

    @Override
    public final List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    private static <T> Function<Object, T> getter(Function<KeyUsage, T> g) {
        return obj -> g.apply((KeyUsage) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    public interface Builder extends SdkPojo, CopyableBuilder<Builder, KeyUsage> {
        /**
         * <p>
         * A string value that contains a Key Usage extension name.
         * </p>
         * 
         * @param name
         *        A string value that contains a Key Usage extension name.
         * @see KeyUsageName
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see KeyUsageName
         */
        Builder name(String name);

        /**
         * <p>
         * A string value that contains a Key Usage extension name.
         * </p>
         * 
         * @param name
         *        A string value that contains a Key Usage extension name.
         * @see KeyUsageName
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see KeyUsageName
         */
        Builder name(KeyUsageName name);
    }

    static final class BuilderImpl implements Builder {
        private String name;

        private BuilderImpl() {
        }

        private BuilderImpl(KeyUsage model) {
            name(model.name);
        }

        public final String getName() {
            return name;
        }

        public final void setName(String name) {
            this.name = name;
        }

        @Override
        public final Builder name(String name) {
            this.name = name;
            return this;
        }

        @Override
        public final Builder name(KeyUsageName name) {
            this.name(name == null ? null : name.toString());
            return this;
        }

        @Override
        public KeyUsage build() {
            return new KeyUsage(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}
