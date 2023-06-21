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
 */
@Generated("software.amazon.awssdk:codegen")
public final class CreateSourceRepositoryBranchResponse extends CodeCatalystResponse implements
        ToCopyableBuilder<CreateSourceRepositoryBranchResponse.Builder, CreateSourceRepositoryBranchResponse> {
    private static final SdkField<String> REF_FIELD = SdkField.<String> builder(MarshallingType.STRING).memberName("ref")
            .getter(getter(CreateSourceRepositoryBranchResponse::ref)).setter(setter(Builder::ref))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("ref").build()).build();

    private static final SdkField<String> NAME_FIELD = SdkField.<String> builder(MarshallingType.STRING).memberName("name")
            .getter(getter(CreateSourceRepositoryBranchResponse::name)).setter(setter(Builder::name))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("name").build()).build();

    private static final SdkField<Instant> LAST_UPDATED_TIME_FIELD = SdkField
            .<Instant> builder(MarshallingType.INSTANT)
            .memberName("lastUpdatedTime")
            .getter(getter(CreateSourceRepositoryBranchResponse::lastUpdatedTime))
            .setter(setter(Builder::lastUpdatedTime))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("lastUpdatedTime").build(),
                    TimestampFormatTrait.create(TimestampFormatTrait.Format.ISO_8601)).build();

    private static final SdkField<String> HEAD_COMMIT_ID_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("headCommitId").getter(getter(CreateSourceRepositoryBranchResponse::headCommitId))
            .setter(setter(Builder::headCommitId))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("headCommitId").build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(REF_FIELD, NAME_FIELD,
            LAST_UPDATED_TIME_FIELD, HEAD_COMMIT_ID_FIELD));

    private final String ref;

    private final String name;

    private final Instant lastUpdatedTime;

    private final String headCommitId;

    private CreateSourceRepositoryBranchResponse(BuilderImpl builder) {
        super(builder);
        this.ref = builder.ref;
        this.name = builder.name;
        this.lastUpdatedTime = builder.lastUpdatedTime;
        this.headCommitId = builder.headCommitId;
    }

    /**
     * <p>
     * The Git reference name of the branch.
     * </p>
     * 
     * @return The Git reference name of the branch.
     */
    public final String ref() {
        return ref;
    }

    /**
     * <p>
     * The name of the newly created branch.
     * </p>
     * 
     * @return The name of the newly created branch.
     */
    public final String name() {
        return name;
    }

    /**
     * <p>
     * The time the branch was last updated, in coordinated universal time (UTC) timestamp format as specified in <a
     * href="https://www.rfc-editor.org/rfc/rfc3339#section-5.6">RFC 3339</a>.
     * </p>
     * 
     * @return The time the branch was last updated, in coordinated universal time (UTC) timestamp format as specified
     *         in <a href="https://www.rfc-editor.org/rfc/rfc3339#section-5.6">RFC 3339</a>.
     */
    public final Instant lastUpdatedTime() {
        return lastUpdatedTime;
    }

    /**
     * <p>
     * The commit ID of the tip of the newly created branch.
     * </p>
     * 
     * @return The commit ID of the tip of the newly created branch.
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
        hashCode = 31 * hashCode + Objects.hashCode(ref());
        hashCode = 31 * hashCode + Objects.hashCode(name());
        hashCode = 31 * hashCode + Objects.hashCode(lastUpdatedTime());
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
        if (!(obj instanceof CreateSourceRepositoryBranchResponse)) {
            return false;
        }
        CreateSourceRepositoryBranchResponse other = (CreateSourceRepositoryBranchResponse) obj;
        return Objects.equals(ref(), other.ref()) && Objects.equals(name(), other.name())
                && Objects.equals(lastUpdatedTime(), other.lastUpdatedTime())
                && Objects.equals(headCommitId(), other.headCommitId());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public final String toString() {
        return ToString.builder("CreateSourceRepositoryBranchResponse").add("Ref", ref()).add("Name", name())
                .add("LastUpdatedTime", lastUpdatedTime()).add("HeadCommitId", headCommitId()).build();
    }

    public final <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
        case "ref":
            return Optional.ofNullable(clazz.cast(ref()));
        case "name":
            return Optional.ofNullable(clazz.cast(name()));
        case "lastUpdatedTime":
            return Optional.ofNullable(clazz.cast(lastUpdatedTime()));
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

    private static <T> Function<Object, T> getter(Function<CreateSourceRepositoryBranchResponse, T> g) {
        return obj -> g.apply((CreateSourceRepositoryBranchResponse) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    public interface Builder extends CodeCatalystResponse.Builder, SdkPojo,
            CopyableBuilder<Builder, CreateSourceRepositoryBranchResponse> {
        /**
         * <p>
         * The Git reference name of the branch.
         * </p>
         * 
         * @param ref
         *        The Git reference name of the branch.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder ref(String ref);

        /**
         * <p>
         * The name of the newly created branch.
         * </p>
         * 
         * @param name
         *        The name of the newly created branch.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder name(String name);

        /**
         * <p>
         * The time the branch was last updated, in coordinated universal time (UTC) timestamp format as specified in <a
         * href="https://www.rfc-editor.org/rfc/rfc3339#section-5.6">RFC 3339</a>.
         * </p>
         * 
         * @param lastUpdatedTime
         *        The time the branch was last updated, in coordinated universal time (UTC) timestamp format as
         *        specified in <a href="https://www.rfc-editor.org/rfc/rfc3339#section-5.6">RFC 3339</a>.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder lastUpdatedTime(Instant lastUpdatedTime);

        /**
         * <p>
         * The commit ID of the tip of the newly created branch.
         * </p>
         * 
         * @param headCommitId
         *        The commit ID of the tip of the newly created branch.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder headCommitId(String headCommitId);
    }

    static final class BuilderImpl extends CodeCatalystResponse.BuilderImpl implements Builder {
        private String ref;

        private String name;

        private Instant lastUpdatedTime;

        private String headCommitId;

        private BuilderImpl() {
        }

        private BuilderImpl(CreateSourceRepositoryBranchResponse model) {
            super(model);
            ref(model.ref);
            name(model.name);
            lastUpdatedTime(model.lastUpdatedTime);
            headCommitId(model.headCommitId);
        }

        public final String getRef() {
            return ref;
        }

        public final void setRef(String ref) {
            this.ref = ref;
        }

        @Override
        public final Builder ref(String ref) {
            this.ref = ref;
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

        public final Instant getLastUpdatedTime() {
            return lastUpdatedTime;
        }

        public final void setLastUpdatedTime(Instant lastUpdatedTime) {
            this.lastUpdatedTime = lastUpdatedTime;
        }

        @Override
        public final Builder lastUpdatedTime(Instant lastUpdatedTime) {
            this.lastUpdatedTime = lastUpdatedTime;
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
        public CreateSourceRepositoryBranchResponse build() {
            return new CreateSourceRepositoryBranchResponse(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}
