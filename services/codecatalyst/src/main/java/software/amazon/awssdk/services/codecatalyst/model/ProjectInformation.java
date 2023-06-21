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
 * Information about a project in a space.
 * </p>
 */
@Generated("software.amazon.awssdk:codegen")
public final class ProjectInformation implements SdkPojo, Serializable,
        ToCopyableBuilder<ProjectInformation.Builder, ProjectInformation> {
    private static final SdkField<String> NAME_FIELD = SdkField.<String> builder(MarshallingType.STRING).memberName("name")
            .getter(getter(ProjectInformation::name)).setter(setter(Builder::name))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("name").build()).build();

    private static final SdkField<String> PROJECT_ID_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("projectId").getter(getter(ProjectInformation::projectId)).setter(setter(Builder::projectId))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("projectId").build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(NAME_FIELD, PROJECT_ID_FIELD));

    private static final long serialVersionUID = 1L;

    private final String name;

    private final String projectId;

    private ProjectInformation(BuilderImpl builder) {
        this.name = builder.name;
        this.projectId = builder.projectId;
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
     * The system-generated unique ID of the project.
     * </p>
     * 
     * @return The system-generated unique ID of the project.
     */
    public final String projectId() {
        return projectId;
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
        hashCode = 31 * hashCode + Objects.hashCode(projectId());
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
        if (!(obj instanceof ProjectInformation)) {
            return false;
        }
        ProjectInformation other = (ProjectInformation) obj;
        return Objects.equals(name(), other.name()) && Objects.equals(projectId(), other.projectId());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public final String toString() {
        return ToString.builder("ProjectInformation").add("Name", name()).add("ProjectId", projectId()).build();
    }

    public final <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
        case "name":
            return Optional.ofNullable(clazz.cast(name()));
        case "projectId":
            return Optional.ofNullable(clazz.cast(projectId()));
        default:
            return Optional.empty();
        }
    }

    @Override
    public final List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    private static <T> Function<Object, T> getter(Function<ProjectInformation, T> g) {
        return obj -> g.apply((ProjectInformation) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    public interface Builder extends SdkPojo, CopyableBuilder<Builder, ProjectInformation> {
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
         * The system-generated unique ID of the project.
         * </p>
         * 
         * @param projectId
         *        The system-generated unique ID of the project.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder projectId(String projectId);
    }

    static final class BuilderImpl implements Builder {
        private String name;

        private String projectId;

        private BuilderImpl() {
        }

        private BuilderImpl(ProjectInformation model) {
            name(model.name);
            projectId(model.projectId);
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

        public final String getProjectId() {
            return projectId;
        }

        public final void setProjectId(String projectId) {
            this.projectId = projectId;
        }

        @Override
        public final Builder projectId(String projectId) {
            this.projectId = projectId;
            return this;
        }

        @Override
        public ProjectInformation build() {
            return new ProjectInformation(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}
