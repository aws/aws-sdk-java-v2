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
 * Information about an space.
 * </p>
 */
@Generated("software.amazon.awssdk:codegen")
public final class SpaceSummary implements SdkPojo, Serializable, ToCopyableBuilder<SpaceSummary.Builder, SpaceSummary> {
    private static final SdkField<String> NAME_FIELD = SdkField.<String> builder(MarshallingType.STRING).memberName("name")
            .getter(getter(SpaceSummary::name)).setter(setter(Builder::name))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("name").build()).build();

    private static final SdkField<String> REGION_NAME_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("regionName").getter(getter(SpaceSummary::regionName)).setter(setter(Builder::regionName))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("regionName").build()).build();

    private static final SdkField<String> DISPLAY_NAME_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("displayName").getter(getter(SpaceSummary::displayName)).setter(setter(Builder::displayName))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("displayName").build()).build();

    private static final SdkField<String> DESCRIPTION_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("description").getter(getter(SpaceSummary::description)).setter(setter(Builder::description))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("description").build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(NAME_FIELD, REGION_NAME_FIELD,
            DISPLAY_NAME_FIELD, DESCRIPTION_FIELD));

    private static final long serialVersionUID = 1L;

    private final String name;

    private final String regionName;

    private final String displayName;

    private final String description;

    private SpaceSummary(BuilderImpl builder) {
        this.name = builder.name;
        this.regionName = builder.regionName;
        this.displayName = builder.displayName;
        this.description = builder.description;
    }

    /**
     * <p>
     * The name of the space.
     * </p>
     * 
     * @return The name of the space.
     */
    public final String name() {
        return name;
    }

    /**
     * <p>
     * The Amazon Web Services Region where the space exists.
     * </p>
     * 
     * @return The Amazon Web Services Region where the space exists.
     */
    public final String regionName() {
        return regionName;
    }

    /**
     * <p>
     * The friendly name of the space displayed to users.
     * </p>
     * 
     * @return The friendly name of the space displayed to users.
     */
    public final String displayName() {
        return displayName;
    }

    /**
     * <p>
     * The description of the space.
     * </p>
     * 
     * @return The description of the space.
     */
    public final String description() {
        return description;
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
        hashCode = 31 * hashCode + Objects.hashCode(name());
        hashCode = 31 * hashCode + Objects.hashCode(regionName());
        hashCode = 31 * hashCode + Objects.hashCode(displayName());
        hashCode = 31 * hashCode + Objects.hashCode(description());
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
        if (!(obj instanceof SpaceSummary)) {
            return false;
        }
        SpaceSummary other = (SpaceSummary) obj;
        return Objects.equals(name(), other.name()) && Objects.equals(regionName(), other.regionName())
                && Objects.equals(displayName(), other.displayName()) && Objects.equals(description(), other.description());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public final String toString() {
        return ToString.builder("SpaceSummary").add("Name", name()).add("RegionName", regionName())
                .add("DisplayName", displayName()).add("Description", description()).build();
    }

    public final <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
        case "name":
            return Optional.ofNullable(clazz.cast(name()));
        case "regionName":
            return Optional.ofNullable(clazz.cast(regionName()));
        case "displayName":
            return Optional.ofNullable(clazz.cast(displayName()));
        case "description":
            return Optional.ofNullable(clazz.cast(description()));
        default:
            return Optional.empty();
        }
    }

    @Override
    public final List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    private static <T> Function<Object, T> getter(Function<SpaceSummary, T> g) {
        return obj -> g.apply((SpaceSummary) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    public interface Builder extends SdkPojo, CopyableBuilder<Builder, SpaceSummary> {
        /**
         * <p>
         * The name of the space.
         * </p>
         * 
         * @param name
         *        The name of the space.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder name(String name);

        /**
         * <p>
         * The Amazon Web Services Region where the space exists.
         * </p>
         * 
         * @param regionName
         *        The Amazon Web Services Region where the space exists.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder regionName(String regionName);

        /**
         * <p>
         * The friendly name of the space displayed to users.
         * </p>
         * 
         * @param displayName
         *        The friendly name of the space displayed to users.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder displayName(String displayName);

        /**
         * <p>
         * The description of the space.
         * </p>
         * 
         * @param description
         *        The description of the space.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder description(String description);
    }

    static final class BuilderImpl implements Builder {
        private String name;

        private String regionName;

        private String displayName;

        private String description;

        private BuilderImpl() {
        }

        private BuilderImpl(SpaceSummary model) {
            name(model.name);
            regionName(model.regionName);
            displayName(model.displayName);
            description(model.description);
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

        public final String getRegionName() {
            return regionName;
        }

        public final void setRegionName(String regionName) {
            this.regionName = regionName;
        }

        @Override
        public final Builder regionName(String regionName) {
            this.regionName = regionName;
            return this;
        }

        public final String getDisplayName() {
            return displayName;
        }

        public final void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public final Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public final String getDescription() {
            return description;
        }

        public final void setDescription(String description) {
            this.description = description;
        }

        @Override
        public final Builder description(String description) {
            this.description = description;
            return this;
        }

        @Override
        public SpaceSummary build() {
            return new SpaceSummary(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}
