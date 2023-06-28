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

package software.amazon.awssdk.services.codecatalyst.model;

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
 * Information about an integrated development environment (IDE) used in a Dev Environment.
 * </p>
 */
@Generated("software.amazon.awssdk:codegen")
public final class Ide implements SdkPojo, Serializable, ToCopyableBuilder<Ide.Builder, Ide> {
    private static final SdkField<String> RUNTIME_FIELD = SdkField.<String> builder(MarshallingType.STRING).memberName("runtime")
            .getter(getter(Ide::runtime)).setter(setter(Builder::runtime))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("runtime").build()).build();

    private static final SdkField<String> NAME_FIELD = SdkField.<String> builder(MarshallingType.STRING).memberName("name")
            .getter(getter(Ide::name)).setter(setter(Builder::name))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("name").build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(RUNTIME_FIELD, NAME_FIELD));

    private static final long serialVersionUID = 1L;

    private final String runtime;

    private final String name;

    private Ide(BuilderImpl builder) {
        this.runtime = builder.runtime;
        this.name = builder.name;
    }

    /**
     * <p>
     * A link to the IDE runtime image.
     * </p>
     * 
     * @return A link to the IDE runtime image.
     */
    public final String runtime() {
        return runtime;
    }

    /**
     * <p>
     * The name of the IDE.
     * </p>
     * 
     * @return The name of the IDE.
     */
    public final String name() {
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
        hashCode = 31 * hashCode + Objects.hashCode(runtime());
        hashCode = 31 * hashCode + Objects.hashCode(name());
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
        if (!(obj instanceof Ide)) {
            return false;
        }
        Ide other = (Ide) obj;
        return Objects.equals(runtime(), other.runtime()) && Objects.equals(name(), other.name());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public final String toString() {
        return ToString.builder("Ide").add("Runtime", runtime()).add("Name", name()).build();
    }

    public final <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
        case "runtime":
            return Optional.ofNullable(clazz.cast(runtime()));
        case "name":
            return Optional.ofNullable(clazz.cast(name()));
        default:
            return Optional.empty();
        }
    }

    @Override
    public final List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    private static <T> Function<Object, T> getter(Function<Ide, T> g) {
        return obj -> g.apply((Ide) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    public interface Builder extends SdkPojo, CopyableBuilder<Builder, Ide> {
        /**
         * <p>
         * A link to the IDE runtime image.
         * </p>
         * 
         * @param runtime
         *        A link to the IDE runtime image.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder runtime(String runtime);

        /**
         * <p>
         * The name of the IDE.
         * </p>
         * 
         * @param name
         *        The name of the IDE.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder name(String name);
    }

    static final class BuilderImpl implements Builder {
        private String runtime;

        private String name;

        private BuilderImpl() {
        }

        private BuilderImpl(Ide model) {
            runtime(model.runtime);
            name(model.name);
        }

        public final String getRuntime() {
            return runtime;
        }

        public final void setRuntime(String runtime) {
            this.runtime = runtime;
        }

        @Override
        public final Builder runtime(String runtime) {
            this.runtime = runtime;
            return this;
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
        public Ide build() {
            return new Ide(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}
