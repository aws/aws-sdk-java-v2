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
 * Information about the configuration of persistent storage for a Dev Environment.
 * </p>
 */
@Generated("software.amazon.awssdk:codegen")
public final class PersistentStorageConfiguration implements SdkPojo, Serializable,
        ToCopyableBuilder<PersistentStorageConfiguration.Builder, PersistentStorageConfiguration> {
    private static final SdkField<Integer> SIZE_IN_GIB_FIELD = SdkField.<Integer> builder(MarshallingType.INTEGER)
            .memberName("sizeInGiB").getter(getter(PersistentStorageConfiguration::sizeInGiB)).setter(setter(Builder::sizeInGiB))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("sizeInGiB").build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(SIZE_IN_GIB_FIELD));

    private static final long serialVersionUID = 1L;

    private final Integer sizeInGiB;

    private PersistentStorageConfiguration(BuilderImpl builder) {
        this.sizeInGiB = builder.sizeInGiB;
    }

    /**
     * <p>
     * The size of the persistent storage in gigabytes (specifically GiB).
     * </p>
     * <note>
     * <p>
     * Valid values for storage are based on memory sizes in 16GB increments. Valid values are 16, 32, and 64.
     * </p>
     * </note>
     * 
     * @return The size of the persistent storage in gigabytes (specifically GiB).</p> <note>
     *         <p>
     *         Valid values for storage are based on memory sizes in 16GB increments. Valid values are 16, 32, and 64.
     *         </p>
     */
    public final Integer sizeInGiB() {
        return sizeInGiB;
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
        hashCode = 31 * hashCode + Objects.hashCode(sizeInGiB());
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
        if (!(obj instanceof PersistentStorageConfiguration)) {
            return false;
        }
        PersistentStorageConfiguration other = (PersistentStorageConfiguration) obj;
        return Objects.equals(sizeInGiB(), other.sizeInGiB());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public final String toString() {
        return ToString.builder("PersistentStorageConfiguration").add("SizeInGiB", sizeInGiB()).build();
    }

    public final <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
        case "sizeInGiB":
            return Optional.ofNullable(clazz.cast(sizeInGiB()));
        default:
            return Optional.empty();
        }
    }

    @Override
    public final List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    private static <T> Function<Object, T> getter(Function<PersistentStorageConfiguration, T> g) {
        return obj -> g.apply((PersistentStorageConfiguration) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    public interface Builder extends SdkPojo, CopyableBuilder<Builder, PersistentStorageConfiguration> {
        /**
         * <p>
         * The size of the persistent storage in gigabytes (specifically GiB).
         * </p>
         * <note>
         * <p>
         * Valid values for storage are based on memory sizes in 16GB increments. Valid values are 16, 32, and 64.
         * </p>
         * </note>
         * 
         * @param sizeInGiB
         *        The size of the persistent storage in gigabytes (specifically GiB).</p> <note>
         *        <p>
         *        Valid values for storage are based on memory sizes in 16GB increments. Valid values are 16, 32, and
         *        64.
         *        </p>
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder sizeInGiB(Integer sizeInGiB);
    }

    static final class BuilderImpl implements Builder {
        private Integer sizeInGiB;

        private BuilderImpl() {
        }

        private BuilderImpl(PersistentStorageConfiguration model) {
            sizeInGiB(model.sizeInGiB);
        }

        public final Integer getSizeInGiB() {
            return sizeInGiB;
        }

        public final void setSizeInGiB(Integer sizeInGiB) {
            this.sizeInGiB = sizeInGiB;
        }

        @Override
        public final Builder sizeInGiB(Integer sizeInGiB) {
            this.sizeInGiB = sizeInGiB;
            return this;
        }

        @Override
        public PersistentStorageConfiguration build() {
            return new PersistentStorageConfiguration(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}
