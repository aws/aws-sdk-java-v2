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
 * Information about the source repsitory for a Dev Environment.
 * </p>
 */
@Generated("software.amazon.awssdk:codegen")
public final class DevEnvironmentRepositorySummary implements SdkPojo, Serializable,
        ToCopyableBuilder<DevEnvironmentRepositorySummary.Builder, DevEnvironmentRepositorySummary> {
    private static final SdkField<String> REPOSITORY_NAME_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("repositoryName").getter(getter(DevEnvironmentRepositorySummary::repositoryName))
            .setter(setter(Builder::repositoryName))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("repositoryName").build()).build();

    private static final SdkField<String> BRANCH_NAME_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("branchName").getter(getter(DevEnvironmentRepositorySummary::branchName))
            .setter(setter(Builder::branchName))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("branchName").build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(REPOSITORY_NAME_FIELD,
            BRANCH_NAME_FIELD));

    private static final long serialVersionUID = 1L;

    private final String repositoryName;

    private final String branchName;

    private DevEnvironmentRepositorySummary(BuilderImpl builder) {
        this.repositoryName = builder.repositoryName;
        this.branchName = builder.branchName;
    }

    /**
     * <p>
     * The name of the source repository.
     * </p>
     * 
     * @return The name of the source repository.
     */
    public final String repositoryName() {
        return repositoryName;
    }

    /**
     * <p>
     * The name of the branch in a source repository cloned into the Dev Environment.
     * </p>
     * 
     * @return The name of the branch in a source repository cloned into the Dev Environment.
     */
    public final String branchName() {
        return branchName;
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
        hashCode = 31 * hashCode + Objects.hashCode(repositoryName());
        hashCode = 31 * hashCode + Objects.hashCode(branchName());
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
        if (!(obj instanceof DevEnvironmentRepositorySummary)) {
            return false;
        }
        DevEnvironmentRepositorySummary other = (DevEnvironmentRepositorySummary) obj;
        return Objects.equals(repositoryName(), other.repositoryName()) && Objects.equals(branchName(), other.branchName());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public final String toString() {
        return ToString.builder("DevEnvironmentRepositorySummary").add("RepositoryName", repositoryName())
                .add("BranchName", branchName()).build();
    }

    public final <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
        case "repositoryName":
            return Optional.ofNullable(clazz.cast(repositoryName()));
        case "branchName":
            return Optional.ofNullable(clazz.cast(branchName()));
        default:
            return Optional.empty();
        }
    }

    @Override
    public final List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    private static <T> Function<Object, T> getter(Function<DevEnvironmentRepositorySummary, T> g) {
        return obj -> g.apply((DevEnvironmentRepositorySummary) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    public interface Builder extends SdkPojo, CopyableBuilder<Builder, DevEnvironmentRepositorySummary> {
        /**
         * <p>
         * The name of the source repository.
         * </p>
         * 
         * @param repositoryName
         *        The name of the source repository.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder repositoryName(String repositoryName);

        /**
         * <p>
         * The name of the branch in a source repository cloned into the Dev Environment.
         * </p>
         * 
         * @param branchName
         *        The name of the branch in a source repository cloned into the Dev Environment.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder branchName(String branchName);
    }

    static final class BuilderImpl implements Builder {
        private String repositoryName;

        private String branchName;

        private BuilderImpl() {
        }

        private BuilderImpl(DevEnvironmentRepositorySummary model) {
            repositoryName(model.repositoryName);
            branchName(model.branchName);
        }

        public final String getRepositoryName() {
            return repositoryName;
        }

        public final void setRepositoryName(String repositoryName) {
            this.repositoryName = repositoryName;
        }

        @Override
        public final Builder repositoryName(String repositoryName) {
            this.repositoryName = repositoryName;
            return this;
        }

        public final String getBranchName() {
            return branchName;
        }

        public final void setBranchName(String branchName) {
            this.branchName = branchName;
        }

        @Override
        public final Builder branchName(String branchName) {
            this.branchName = branchName;
            return this;
        }

        @Override
        public DevEnvironmentRepositorySummary build() {
            return new DevEnvironmentRepositorySummary(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}
