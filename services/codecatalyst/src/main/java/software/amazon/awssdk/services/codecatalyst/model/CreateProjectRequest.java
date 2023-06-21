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
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
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
public final class CreateProjectRequest extends CodeCatalystRequest implements
        ToCopyableBuilder<CreateProjectRequest.Builder, CreateProjectRequest> {
    private static final SdkField<String> SPACE_NAME_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("spaceName").getter(getter(CreateProjectRequest::spaceName)).setter(setter(Builder::spaceName))
            .traits(LocationTrait.builder().location(MarshallLocation.PATH).locationName("spaceName").build()).build();

    private static final SdkField<String> DISPLAY_NAME_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("displayName").getter(getter(CreateProjectRequest::displayName)).setter(setter(Builder::displayName))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("displayName").build()).build();

    private static final SdkField<String> DESCRIPTION_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("description").getter(getter(CreateProjectRequest::description)).setter(setter(Builder::description))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("description").build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(SPACE_NAME_FIELD,
            DISPLAY_NAME_FIELD, DESCRIPTION_FIELD));

    private final String spaceName;

    private final String displayName;

    private final String description;

    private CreateProjectRequest(BuilderImpl builder) {
        super(builder);
        this.spaceName = builder.spaceName;
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
    public final String spaceName() {
        return spaceName;
    }

    /**
     * <p>
     * The friendly name of the project that will be displayed to users.
     * </p>
     * 
     * @return The friendly name of the project that will be displayed to users.
     */
    public final String displayName() {
        return displayName;
    }

    /**
     * <p>
     * The description of the project. This description will be displayed to all users of the project. We recommend
     * providing a brief description of the project and its intended purpose.
     * </p>
     * 
     * @return The description of the project. This description will be displayed to all users of the project. We
     *         recommend providing a brief description of the project and its intended purpose.
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
        hashCode = 31 * hashCode + super.hashCode();
        hashCode = 31 * hashCode + Objects.hashCode(spaceName());
        hashCode = 31 * hashCode + Objects.hashCode(displayName());
        hashCode = 31 * hashCode + Objects.hashCode(description());
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
        if (!(obj instanceof CreateProjectRequest)) {
            return false;
        }
        CreateProjectRequest other = (CreateProjectRequest) obj;
        return Objects.equals(spaceName(), other.spaceName()) && Objects.equals(displayName(), other.displayName())
                && Objects.equals(description(), other.description());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public final String toString() {
        return ToString.builder("CreateProjectRequest").add("SpaceName", spaceName()).add("DisplayName", displayName())
                .add("Description", description()).build();
    }

    public final <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
        case "spaceName":
            return Optional.ofNullable(clazz.cast(spaceName()));
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

    private static <T> Function<Object, T> getter(Function<CreateProjectRequest, T> g) {
        return obj -> g.apply((CreateProjectRequest) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    public interface Builder extends CodeCatalystRequest.Builder, SdkPojo, CopyableBuilder<Builder, CreateProjectRequest> {
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
         * The friendly name of the project that will be displayed to users.
         * </p>
         * 
         * @param displayName
         *        The friendly name of the project that will be displayed to users.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder displayName(String displayName);

        /**
         * <p>
         * The description of the project. This description will be displayed to all users of the project. We recommend
         * providing a brief description of the project and its intended purpose.
         * </p>
         * 
         * @param description
         *        The description of the project. This description will be displayed to all users of the project. We
         *        recommend providing a brief description of the project and its intended purpose.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder description(String description);

        @Override
        Builder overrideConfiguration(AwsRequestOverrideConfiguration overrideConfiguration);

        @Override
        Builder overrideConfiguration(Consumer<AwsRequestOverrideConfiguration.Builder> builderConsumer);
    }

    static final class BuilderImpl extends CodeCatalystRequest.BuilderImpl implements Builder {
        private String spaceName;

        private String displayName;

        private String description;

        private BuilderImpl() {
        }

        private BuilderImpl(CreateProjectRequest model) {
            super(model);
            spaceName(model.spaceName);
            displayName(model.displayName);
            description(model.description);
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
        public Builder overrideConfiguration(AwsRequestOverrideConfiguration overrideConfiguration) {
            super.overrideConfiguration(overrideConfiguration);
            return this;
        }

        @Override
        public Builder overrideConfiguration(Consumer<AwsRequestOverrideConfiguration.Builder> builderConsumer) {
            super.overrideConfiguration(builderConsumer);
            return this;
        }

        @Override
        public CreateProjectRequest build() {
            return new CreateProjectRequest(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}
