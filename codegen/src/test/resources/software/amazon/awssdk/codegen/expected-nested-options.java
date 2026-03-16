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

package software.amazon.awssdk.services.restjson.model;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.Mutable;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.core.traits.LocationTrait;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 */
@Generated("software.amazon.awssdk:codegen")
public final class NestedOptions implements SdkPojo, Serializable, ToCopyableBuilder<NestedOptions.Builder, NestedOptions> {
    private static final SdkField<String> PAGE_SIZE_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("pageSize").getter(getter(NestedOptions::pageSize)).setter(setter(Builder::pageSize))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("pageSize").build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(PAGE_SIZE_FIELD));

    private static final Map<String, SdkField<?>> SDK_NAME_TO_FIELD = memberNameToFieldInitializer();

    private static final long serialVersionUID = 1L;

    private final String pageSize;

    private NestedOptions(BuilderImpl builder) {
        this.pageSize = builder.pageSize;
    }

    /**
     * Returns the value of the PageSize property for this object.
     * 
     * @return The value of the PageSize property for this object.
     */
    public final String pageSize() {
        return pageSize;
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
        hashCode = 31 * hashCode + Objects.hashCode(pageSize());
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
        if (!(obj instanceof NestedOptions)) {
            return false;
        }
        NestedOptions other = (NestedOptions) obj;
        return Objects.equals(pageSize(), other.pageSize());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public final String toString() {
        return ToString.builder("NestedOptions").add("PageSize", pageSize()).build();
    }

    public final <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
        case "pageSize":
            return Optional.ofNullable(clazz.cast(pageSize()));
        default:
            return Optional.empty();
        }
    }

    @Override
    public final List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    @Override
    public final Map<String, SdkField<?>> sdkFieldNameToField() {
        return SDK_NAME_TO_FIELD;
    }

    private static Map<String, SdkField<?>> memberNameToFieldInitializer() {
        Map<String, SdkField<?>> map = new HashMap<>();
        map.put("pageSize", PAGE_SIZE_FIELD);
        return Collections.unmodifiableMap(map);
    }

    private static <T> Function<Object, T> getter(Function<NestedOptions, T> g) {
        return obj -> g.apply((NestedOptions) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    @Mutable
    @NotThreadSafe
    public interface Builder extends SdkPojo, CopyableBuilder<Builder, NestedOptions> {
        /**
         * Sets the value of the PageSize property for this object.
         *
         * @param pageSize
         *        The new value for the PageSize property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder pageSize(String pageSize);
    }

    static final class BuilderImpl implements Builder {
        private String pageSize;

        private BuilderImpl() {
        }

        private BuilderImpl(NestedOptions model) {
            pageSize(model.pageSize);
        }

        public final String getPageSize() {
            return pageSize;
        }

        public final void setPageSize(String pageSize) {
            this.pageSize = pageSize;
        }

        @Override
        public final Builder pageSize(String pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        @Override
        public NestedOptions build() {
            return new NestedOptions(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }

        @Override
        public Map<String, SdkField<?>> sdkFieldNameToField() {
            return SDK_NAME_TO_FIELD;
        }
    }
}
