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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
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
 */
@Generated("software.amazon.awssdk:codegen")
public final class StartDevEnvironmentSessionResponse extends CodeCatalystResponse implements
        ToCopyableBuilder<StartDevEnvironmentSessionResponse.Builder, StartDevEnvironmentSessionResponse> {
    private static final SdkField<DevEnvironmentAccessDetails> ACCESS_DETAILS_FIELD = SdkField
            .<DevEnvironmentAccessDetails> builder(MarshallingType.SDK_POJO).memberName("accessDetails")
            .getter(getter(StartDevEnvironmentSessionResponse::accessDetails)).setter(setter(Builder::accessDetails))
            .constructor(DevEnvironmentAccessDetails::builder)
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("accessDetails").build()).build();

    private static final SdkField<String> SESSION_ID_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("sessionId").getter(getter(StartDevEnvironmentSessionResponse::sessionId))
            .setter(setter(Builder::sessionId))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("sessionId").build()).build();

    private static final SdkField<String> SPACE_NAME_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("spaceName").getter(getter(StartDevEnvironmentSessionResponse::spaceName))
            .setter(setter(Builder::spaceName))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("spaceName").build()).build();

    private static final SdkField<String> PROJECT_NAME_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("projectName").getter(getter(StartDevEnvironmentSessionResponse::projectName))
            .setter(setter(Builder::projectName))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("projectName").build()).build();

    private static final SdkField<String> ID_FIELD = SdkField.<String> builder(MarshallingType.STRING).memberName("id")
            .getter(getter(StartDevEnvironmentSessionResponse::id)).setter(setter(Builder::id))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("id").build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(ACCESS_DETAILS_FIELD,
            SESSION_ID_FIELD, SPACE_NAME_FIELD, PROJECT_NAME_FIELD, ID_FIELD));

    private final DevEnvironmentAccessDetails accessDetails;

    private final String sessionId;

    private final String spaceName;

    private final String projectName;

    private final String id;

    private StartDevEnvironmentSessionResponse(BuilderImpl builder) {
        super(builder);
        this.accessDetails = builder.accessDetails;
        this.sessionId = builder.sessionId;
        this.spaceName = builder.spaceName;
        this.projectName = builder.projectName;
        this.id = builder.id;
    }

    /**
     * Returns the value of the AccessDetails property for this object.
     * 
     * @return The value of the AccessDetails property for this object.
     */
    public final DevEnvironmentAccessDetails accessDetails() {
        return accessDetails;
    }

    /**
     * <p>
     * The system-generated unique ID of the Dev Environment session.
     * </p>
     * 
     * @return The system-generated unique ID of the Dev Environment session.
     */
    public final String sessionId() {
        return sessionId;
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
        hashCode = 31 * hashCode + super.hashCode();
        hashCode = 31 * hashCode + Objects.hashCode(accessDetails());
        hashCode = 31 * hashCode + Objects.hashCode(sessionId());
        hashCode = 31 * hashCode + Objects.hashCode(spaceName());
        hashCode = 31 * hashCode + Objects.hashCode(projectName());
        hashCode = 31 * hashCode + Objects.hashCode(id());
        return hashCode;
    }

    @Override
    public final boolean equals(Object obj) {
        return super.equals(obj) && equalsBySdkFields(obj);
    }

    @Override
    public final boolean equalsBySdkFields(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof StartDevEnvironmentSessionResponse)) {
            return false;
        }
        StartDevEnvironmentSessionResponse other = (StartDevEnvironmentSessionResponse) obj;
        return Objects.equals(accessDetails(), other.accessDetails()) && Objects.equals(sessionId(), other.sessionId())
                && Objects.equals(spaceName(), other.spaceName()) && Objects.equals(projectName(), other.projectName())
                && Objects.equals(id(), other.id());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public final String toString() {
        return ToString.builder("StartDevEnvironmentSessionResponse")
                .add("AccessDetails", accessDetails() == null ? null : "*** Sensitive Data Redacted ***")
                .add("SessionId", sessionId()).add("SpaceName", spaceName()).add("ProjectName", projectName()).add("Id", id())
                .build();
    }

    public final <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
        case "accessDetails":
            return Optional.ofNullable(clazz.cast(accessDetails()));
        case "sessionId":
            return Optional.ofNullable(clazz.cast(sessionId()));
        case "spaceName":
            return Optional.ofNullable(clazz.cast(spaceName()));
        case "projectName":
            return Optional.ofNullable(clazz.cast(projectName()));
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

    private static <T> Function<Object, T> getter(Function<StartDevEnvironmentSessionResponse, T> g) {
        return obj -> g.apply((StartDevEnvironmentSessionResponse) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    public interface Builder extends CodeCatalystResponse.Builder, SdkPojo,
            CopyableBuilder<Builder, StartDevEnvironmentSessionResponse> {
        /**
         * Sets the value of the AccessDetails property for this object.
         *
         * @param accessDetails
         *        The new value for the AccessDetails property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder accessDetails(DevEnvironmentAccessDetails accessDetails);

        /**
         * Sets the value of the AccessDetails property for this object.
         *
         * This is a convenience method that creates an instance of the {@link DevEnvironmentAccessDetails.Builder}
         * avoiding the need to create one manually via {@link DevEnvironmentAccessDetails#builder()}.
         *
         * <p>
         * When the {@link Consumer} completes, {@link DevEnvironmentAccessDetails.Builder#build()} is called
         * immediately and its result is passed to {@link #accessDetails(DevEnvironmentAccessDetails)}.
         * 
         * @param accessDetails
         *        a consumer that will call methods on {@link DevEnvironmentAccessDetails.Builder}
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see #accessDetails(DevEnvironmentAccessDetails)
         */
        default Builder accessDetails(Consumer<DevEnvironmentAccessDetails.Builder> accessDetails) {
            return accessDetails(DevEnvironmentAccessDetails.builder().applyMutation(accessDetails).build());
        }

        /**
         * <p>
         * The system-generated unique ID of the Dev Environment session.
         * </p>
         * 
         * @param sessionId
         *        The system-generated unique ID of the Dev Environment session.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder sessionId(String sessionId);

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
         * @param id
         *        The system-generated unique ID of the Dev Environment.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder id(String id);
    }

    static final class BuilderImpl extends CodeCatalystResponse.BuilderImpl implements Builder {
        private DevEnvironmentAccessDetails accessDetails;

        private String sessionId;

        private String spaceName;

        private String projectName;

        private String id;

        private BuilderImpl() {
        }

        private BuilderImpl(StartDevEnvironmentSessionResponse model) {
            super(model);
            accessDetails(model.accessDetails);
            sessionId(model.sessionId);
            spaceName(model.spaceName);
            projectName(model.projectName);
            id(model.id);
        }

        public final DevEnvironmentAccessDetails.Builder getAccessDetails() {
            return accessDetails != null ? accessDetails.toBuilder() : null;
        }

        public final void setAccessDetails(DevEnvironmentAccessDetails.BuilderImpl accessDetails) {
            this.accessDetails = accessDetails != null ? accessDetails.build() : null;
        }

        @Override
        public final Builder accessDetails(DevEnvironmentAccessDetails accessDetails) {
            this.accessDetails = accessDetails;
            return this;
        }

        public final String getSessionId() {
            return sessionId;
        }

        public final void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }

        @Override
        public final Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
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
        public StartDevEnvironmentSessionResponse build() {
            return new StartDevEnvironmentSessionResponse(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}
