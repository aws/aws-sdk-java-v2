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
public final class CreateSourceRepositoryBranchRequest extends CodeCatalystRequest implements
        ToCopyableBuilder<CreateSourceRepositoryBranchRequest.Builder, CreateSourceRepositoryBranchRequest> {
    private static final SdkField<String> SPACE_NAME_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("spaceName").getter(getter(CreateSourceRepositoryBranchRequest::spaceName))
            .setter(setter(Builder::spaceName))
            .traits(LocationTrait.builder().location(MarshallLocation.PATH).locationName("spaceName").build()).build();

    private static final SdkField<String> PROJECT_NAME_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("projectName").getter(getter(CreateSourceRepositoryBranchRequest::projectName))
            .setter(setter(Builder::projectName))
            .traits(LocationTrait.builder().location(MarshallLocation.PATH).locationName("projectName").build()).build();

    private static final SdkField<String> SOURCE_REPOSITORY_NAME_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("sourceRepositoryName").getter(getter(CreateSourceRepositoryBranchRequest::sourceRepositoryName))
            .setter(setter(Builder::sourceRepositoryName))
            .traits(LocationTrait.builder().location(MarshallLocation.PATH).locationName("sourceRepositoryName").build()).build();

    private static final SdkField<String> NAME_FIELD = SdkField.<String> builder(MarshallingType.STRING).memberName("name")
            .getter(getter(CreateSourceRepositoryBranchRequest::name)).setter(setter(Builder::name))
            .traits(LocationTrait.builder().location(MarshallLocation.PATH).locationName("name").build()).build();

    private static final SdkField<String> HEAD_COMMIT_ID_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("headCommitId").getter(getter(CreateSourceRepositoryBranchRequest::headCommitId))
            .setter(setter(Builder::headCommitId))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("headCommitId").build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(SPACE_NAME_FIELD,
            PROJECT_NAME_FIELD, SOURCE_REPOSITORY_NAME_FIELD, NAME_FIELD, HEAD_COMMIT_ID_FIELD));

    private final String spaceName;

    private final String projectName;

    private final String sourceRepositoryName;

    private final String name;

    private final String headCommitId;

    private CreateSourceRepositoryBranchRequest(BuilderImpl builder) {
        super(builder);
        this.spaceName = builder.spaceName;
        this.projectName = builder.projectName;
        this.sourceRepositoryName = builder.sourceRepositoryName;
        this.name = builder.name;
        this.headCommitId = builder.headCommitId;
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
     * The name of the repository where you want to create a branch.
     * </p>
     * 
     * @return The name of the repository where you want to create a branch.
     */
    public final String sourceRepositoryName() {
        return sourceRepositoryName;
    }

    /**
     * <p>
     * The name for the branch you're creating.
     * </p>
     * 
     * @return The name for the branch you're creating.
     */
    public final String name() {
        return name;
    }

    /**
     * <p>
     * The commit ID in an existing branch from which you want to create the new branch.
     * </p>
     * 
     * @return The commit ID in an existing branch from which you want to create the new branch.
     */
    public final String headCommitId() {
        return headCommitId;
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
        hashCode = 31 * hashCode + Objects.hashCode(projectName());
        hashCode = 31 * hashCode + Objects.hashCode(sourceRepositoryName());
        hashCode = 31 * hashCode + Objects.hashCode(name());
        hashCode = 31 * hashCode + Objects.hashCode(headCommitId());
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
        if (!(obj instanceof CreateSourceRepositoryBranchRequest)) {
            return false;
        }
        CreateSourceRepositoryBranchRequest other = (CreateSourceRepositoryBranchRequest) obj;
        return Objects.equals(spaceName(), other.spaceName()) && Objects.equals(projectName(), other.projectName())
                && Objects.equals(sourceRepositoryName(), other.sourceRepositoryName()) && Objects.equals(name(), other.name())
                && Objects.equals(headCommitId(), other.headCommitId());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public final String toString() {
        return ToString.builder("CreateSourceRepositoryBranchRequest").add("SpaceName", spaceName())
                .add("ProjectName", projectName()).add("SourceRepositoryName", sourceRepositoryName()).add("Name", name())
                .add("HeadCommitId", headCommitId()).build();
    }

    public final <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
        case "spaceName":
            return Optional.ofNullable(clazz.cast(spaceName()));
        case "projectName":
            return Optional.ofNullable(clazz.cast(projectName()));
        case "sourceRepositoryName":
            return Optional.ofNullable(clazz.cast(sourceRepositoryName()));
        case "name":
            return Optional.ofNullable(clazz.cast(name()));
        case "headCommitId":
            return Optional.ofNullable(clazz.cast(headCommitId()));
        default:
            return Optional.empty();
        }
    }

    @Override
    public final List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    private static <T> Function<Object, T> getter(Function<CreateSourceRepositoryBranchRequest, T> g) {
        return obj -> g.apply((CreateSourceRepositoryBranchRequest) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    public interface Builder extends CodeCatalystRequest.Builder, SdkPojo,
            CopyableBuilder<Builder, CreateSourceRepositoryBranchRequest> {
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
         * The name of the repository where you want to create a branch.
         * </p>
         * 
         * @param sourceRepositoryName
         *        The name of the repository where you want to create a branch.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder sourceRepositoryName(String sourceRepositoryName);

        /**
         * <p>
         * The name for the branch you're creating.
         * </p>
         * 
         * @param name
         *        The name for the branch you're creating.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder name(String name);

        /**
         * <p>
         * The commit ID in an existing branch from which you want to create the new branch.
         * </p>
         * 
         * @param headCommitId
         *        The commit ID in an existing branch from which you want to create the new branch.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder headCommitId(String headCommitId);

        @Override
        Builder overrideConfiguration(AwsRequestOverrideConfiguration overrideConfiguration);

        @Override
        Builder overrideConfiguration(Consumer<AwsRequestOverrideConfiguration.Builder> builderConsumer);
    }

    static final class BuilderImpl extends CodeCatalystRequest.BuilderImpl implements Builder {
        private String spaceName;

        private String projectName;

        private String sourceRepositoryName;

        private String name;

        private String headCommitId;

        private BuilderImpl() {
        }

        private BuilderImpl(CreateSourceRepositoryBranchRequest model) {
            super(model);
            spaceName(model.spaceName);
            projectName(model.projectName);
            sourceRepositoryName(model.sourceRepositoryName);
            name(model.name);
            headCommitId(model.headCommitId);
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

        public final String getSourceRepositoryName() {
            return sourceRepositoryName;
        }

        public final void setSourceRepositoryName(String sourceRepositoryName) {
            this.sourceRepositoryName = sourceRepositoryName;
        }

        @Override
        public final Builder sourceRepositoryName(String sourceRepositoryName) {
            this.sourceRepositoryName = sourceRepositoryName;
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

        public final String getHeadCommitId() {
            return headCommitId;
        }

        public final void setHeadCommitId(String headCommitId) {
            this.headCommitId = headCommitId;
        }

        @Override
        public final Builder headCommitId(String headCommitId) {
            this.headCommitId = headCommitId;
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
        public CreateSourceRepositoryBranchRequest build() {
            return new CreateSourceRepositoryBranchRequest(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}
