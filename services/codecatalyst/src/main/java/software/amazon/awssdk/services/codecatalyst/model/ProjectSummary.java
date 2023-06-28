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
 * Information about a project.
 * </p>
 */
@Generated("software.amazon.awssdk:codegen")
public final class ProjectSummary implements SdkPojo, Serializable, ToCopyableBuilder<ProjectSummary.Builder, ProjectSummary> {
    private static final SdkField<String> NAME_FIELD = SdkField.<String> builder(MarshallingType.STRING).memberName("name")
            .getter(getter(ProjectSummary::name)).setter(setter(Builder::name))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("name").build()).build();

    private static final SdkField<String> DISPLAY_NAME_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("displayName").getter(getter(ProjectSummary::displayName)).setter(setter(Builder::displayName))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("displayName").build()).build();

    private static final SdkField<String> DESCRIPTION_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("description").getter(getter(ProjectSummary::description)).setter(setter(Builder::description))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("description").build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(NAME_FIELD,
            DISPLAY_NAME_FIELD, DESCRIPTION_FIELD));

    private static final long serialVersionUID = 1L;

    private final String name;

    private final String displayName;

    private final String description;

    private ProjectSummary(BuilderImpl builder) {
        this.name = builder.name;
        this.displayName = builder.displayName;
        this.description = builder.description;
    }

    /**
     * <p>
     * The name of the project in the space.
     * </p>
     * 
     * @return The name of the project in the space.
     */
    public final String name() {
        return name;
    }

    /**
     * <p>
     * The friendly name displayed to users of the project in Amazon CodeCatalyst.
     * </p>
     * 
     * @return The friendly name displayed to users of the project in Amazon CodeCatalyst.
     */
    public final String displayName() {
        return displayName;
    }

    /**
     * <p>
     * The description of the project.
     * </p>
     * 
     * @return The description of the project.
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
        if (!(obj instanceof ProjectSummary)) {
            return false;
        }
        ProjectSummary other = (ProjectSummary) obj;
        return Objects.equals(name(), other.name()) && Objects.equals(displayName(), other.displayName())
                && Objects.equals(description(), other.description());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public final String toString() {
        return ToString.builder("ProjectSummary").add("Name", name()).add("DisplayName", displayName())
                .add("Description", description()).build();
    }

    public final <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
        case "name":
            return Optional.ofNullable(clazz.cast(name()));
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

    private static <T> Function<Object, T> getter(Function<ProjectSummary, T> g) {
        return obj -> g.apply((ProjectSummary) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    public interface Builder extends SdkPojo, CopyableBuilder<Builder, ProjectSummary> {
        /**
         * <p>
         * The name of the project in the space.
         * </p>
         * 
         * @param name
         *        The name of the project in the space.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder name(String name);

        /**
         * <p>
         * The friendly name displayed to users of the project in Amazon CodeCatalyst.
         * </p>
         * 
         * @param displayName
         *        The friendly name displayed to users of the project in Amazon CodeCatalyst.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder displayName(String displayName);

        /**
         * <p>
         * The description of the project.
         * </p>
         * 
         * @param description
         *        The description of the project.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder description(String description);
    }

    static final class BuilderImpl implements Builder {
        private String name;

        private String displayName;

        private String description;

        private BuilderImpl() {
        }

        private BuilderImpl(ProjectSummary model) {
            name(model.name);
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
        public ProjectSummary build() {
            return new ProjectSummary(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}
