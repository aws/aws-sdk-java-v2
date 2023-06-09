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
import java.time.Instant;
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
import software.amazon.awssdk.core.traits.TimestampFormatTrait;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * <p>
 * Information about active sessions for a Dev Environment.
 * </p>
 */
@Generated("software.amazon.awssdk:codegen")
public final class DevEnvironmentSessionSummary implements SdkPojo, Serializable,
        ToCopyableBuilder<DevEnvironmentSessionSummary.Builder, DevEnvironmentSessionSummary> {
    private static final SdkField<String> SPACE_NAME_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("spaceName").getter(getter(DevEnvironmentSessionSummary::spaceName)).setter(setter(Builder::spaceName))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("spaceName").build()).build();

    private static final SdkField<String> PROJECT_NAME_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("projectName").getter(getter(DevEnvironmentSessionSummary::projectName))
            .setter(setter(Builder::projectName))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("projectName").build()).build();

    private static final SdkField<String> DEV_ENVIRONMENT_ID_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("devEnvironmentId").getter(getter(DevEnvironmentSessionSummary::devEnvironmentId))
            .setter(setter(Builder::devEnvironmentId))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("devEnvironmentId").build()).build();

    private static final SdkField<Instant> STARTED_TIME_FIELD = SdkField
            .<Instant> builder(MarshallingType.INSTANT)
            .memberName("startedTime")
            .getter(getter(DevEnvironmentSessionSummary::startedTime))
            .setter(setter(Builder::startedTime))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("startedTime").build(),
                    TimestampFormatTrait.create(TimestampFormatTrait.Format.ISO_8601)).build();

    private static final SdkField<String> ID_FIELD = SdkField.<String> builder(MarshallingType.STRING).memberName("id")
            .getter(getter(DevEnvironmentSessionSummary::id)).setter(setter(Builder::id))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("id").build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(SPACE_NAME_FIELD,
            PROJECT_NAME_FIELD, DEV_ENVIRONMENT_ID_FIELD, STARTED_TIME_FIELD, ID_FIELD));

    private static final long serialVersionUID = 1L;

    private final String spaceName;

    private final String projectName;

    private final String devEnvironmentId;

    private final Instant startedTime;

    private final String id;

    private DevEnvironmentSessionSummary(BuilderImpl builder) {
        this.spaceName = builder.spaceName;
        this.projectName = builder.projectName;
        this.devEnvironmentId = builder.devEnvironmentId;
        this.startedTime = builder.startedTime;
        this.id = builder.id;
    }

    /**
     * <p>
     * The name of the space.
     * </p>
     * 
     * @return The name of the space.
     */
    public final String spaceName() {
        return spaceName;
    }

    /**
     * <p>
     * The name of the project in the space.
     * </p>
     * 
     * @return The name of the project in the space.
     */
    public final String projectName() {
        return projectName;
    }

    /**
     * <p>
     * The system-generated unique ID of the Dev Environment.
     * </p>
     * 
     * @return The system-generated unique ID of the Dev Environment.
     */
    public final String devEnvironmentId() {
        return devEnvironmentId;
    }

    /**
     * <p>
     * The date and time the session started, in coordinated universal time (UTC) timestamp format as specified in <a
     * href="https://www.rfc-editor.org/rfc/rfc3339#section-5.6">RFC 3339</a>
     * </p>
     * 
     * @return The date and time the session started, in coordinated universal time (UTC) timestamp format as specified
     *         in <a href="https://www.rfc-editor.org/rfc/rfc3339#section-5.6">RFC 3339</a>
     */
    public final Instant startedTime() {
        return startedTime;
    }

    /**
     * <p>
     * The system-generated unique ID of the Dev Environment session.
     * </p>
     * 
     * @return The system-generated unique ID of the Dev Environment session.
     */
    public final String id() {
        return id;
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
        hashCode = 31 * hashCode + Objects.hashCode(spaceName());
        hashCode = 31 * hashCode + Objects.hashCode(projectName());
        hashCode = 31 * hashCode + Objects.hashCode(devEnvironmentId());
        hashCode = 31 * hashCode + Objects.hashCode(startedTime());
        hashCode = 31 * hashCode + Objects.hashCode(id());
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
        if (!(obj instanceof DevEnvironmentSessionSummary)) {
            return false;
        }
        DevEnvironmentSessionSummary other = (DevEnvironmentSessionSummary) obj;
        return Objects.equals(spaceName(), other.spaceName()) && Objects.equals(projectName(), other.projectName())
                && Objects.equals(devEnvironmentId(), other.devEnvironmentId())
                && Objects.equals(startedTime(), other.startedTime()) && Objects.equals(id(), other.id());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public final String toString() {
        return ToString.builder("DevEnvironmentSessionSummary").add("SpaceName", spaceName()).add("ProjectName", projectName())
                .add("DevEnvironmentId", devEnvironmentId()).add("StartedTime", startedTime()).add("Id", id()).build();
    }

    public final <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
        case "spaceName":
            return Optional.ofNullable(clazz.cast(spaceName()));
        case "projectName":
            return Optional.ofNullable(clazz.cast(projectName()));
        case "devEnvironmentId":
            return Optional.ofNullable(clazz.cast(devEnvironmentId()));
        case "startedTime":
            return Optional.ofNullable(clazz.cast(startedTime()));
        case "id":
            return Optional.ofNullable(clazz.cast(id()));
        default:
            return Optional.empty();
        }
    }

    @Override
    public final List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    private static <T> Function<Object, T> getter(Function<DevEnvironmentSessionSummary, T> g) {
        return obj -> g.apply((DevEnvironmentSessionSummary) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    public interface Builder extends SdkPojo, CopyableBuilder<Builder, DevEnvironmentSessionSummary> {
        /**
         * <p>
         * The name of the space.
         * </p>
         * 
         * @param spaceName
         *        The name of the space.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder spaceName(String spaceName);

        /**
         * <p>
         * The name of the project in the space.
         * </p>
         * 
         * @param projectName
         *        The name of the project in the space.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder projectName(String projectName);

        /**
         * <p>
         * The system-generated unique ID of the Dev Environment.
         * </p>
         * 
         * @param devEnvironmentId
         *        The system-generated unique ID of the Dev Environment.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder devEnvironmentId(String devEnvironmentId);

        /**
         * <p>
         * The date and time the session started, in coordinated universal time (UTC) timestamp format as specified in
         * <a href="https://www.rfc-editor.org/rfc/rfc3339#section-5.6">RFC 3339</a>
         * </p>
         * 
         * @param startedTime
         *        The date and time the session started, in coordinated universal time (UTC) timestamp format as
         *        specified in <a href="https://www.rfc-editor.org/rfc/rfc3339#section-5.6">RFC 3339</a>
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder startedTime(Instant startedTime);

        /**
         * <p>
         * The system-generated unique ID of the Dev Environment session.
         * </p>
         * 
         * @param id
         *        The system-generated unique ID of the Dev Environment session.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder id(String id);
    }

    static final class BuilderImpl implements Builder {
        private String spaceName;

        private String projectName;

        private String devEnvironmentId;

        private Instant startedTime;

        private String id;

        private BuilderImpl() {
        }

        private BuilderImpl(DevEnvironmentSessionSummary model) {
            spaceName(model.spaceName);
            projectName(model.projectName);
            devEnvironmentId(model.devEnvironmentId);
            startedTime(model.startedTime);
            id(model.id);
        }

        public final String getSpaceName() {
            return spaceName;
        }

        public final void setSpaceName(String spaceName) {
            this.spaceName = spaceName;
        }

        @Override
        public final Builder spaceName(String spaceName) {
            this.spaceName = spaceName;
            return this;
        }

        public final String getProjectName() {
            return projectName;
        }

        public final void setProjectName(String projectName) {
            this.projectName = projectName;
        }

        @Override
        public final Builder projectName(String projectName) {
            this.projectName = projectName;
            return this;
        }

        public final String getDevEnvironmentId() {
            return devEnvironmentId;
        }

        public final void setDevEnvironmentId(String devEnvironmentId) {
            this.devEnvironmentId = devEnvironmentId;
        }

        @Override
        public final Builder devEnvironmentId(String devEnvironmentId) {
            this.devEnvironmentId = devEnvironmentId;
            return this;
        }

        public final Instant getStartedTime() {
            return startedTime;
        }

        public final void setStartedTime(Instant startedTime) {
            this.startedTime = startedTime;
        }

        @Override
        public final Builder startedTime(Instant startedTime) {
            this.startedTime = startedTime;
            return this;
        }

        public final String getId() {
            return id;
        }

        public final void setId(String id) {
            this.id = id;
        }

        @Override
        public final Builder id(String id) {
            this.id = id;
            return this;
        }

        @Override
        public DevEnvironmentSessionSummary build() {
            return new DevEnvironmentSessionSummary(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}
