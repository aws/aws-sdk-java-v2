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
public final class ListSourceRepositoryBranchesRequest extends CodeCatalystRequest implements
        ToCopyableBuilder<ListSourceRepositoryBranchesRequest.Builder, ListSourceRepositoryBranchesRequest> {
    private static final SdkField<String> SPACE_NAME_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("spaceName").getter(getter(ListSourceRepositoryBranchesRequest::spaceName))
            .setter(setter(Builder::spaceName))
            .traits(LocationTrait.builder().location(MarshallLocation.PATH).locationName("spaceName").build()).build();

    private static final SdkField<String> PROJECT_NAME_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("projectName").getter(getter(ListSourceRepositoryBranchesRequest::projectName))
            .setter(setter(Builder::projectName))
            .traits(LocationTrait.builder().location(MarshallLocation.PATH).locationName("projectName").build()).build();

    private static final SdkField<String> SOURCE_REPOSITORY_NAME_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("sourceRepositoryName").getter(getter(ListSourceRepositoryBranchesRequest::sourceRepositoryName))
            .setter(setter(Builder::sourceRepositoryName))
            .traits(LocationTrait.builder().location(MarshallLocation.PATH).locationName("sourceRepositoryName").build()).build();

    private static final SdkField<String> NEXT_TOKEN_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("nextToken").getter(getter(ListSourceRepositoryBranchesRequest::nextToken))
            .setter(setter(Builder::nextToken))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("nextToken").build()).build();

    private static final SdkField<Integer> MAX_RESULTS_FIELD = SdkField.<Integer> builder(MarshallingType.INTEGER)
            .memberName("maxResults").getter(getter(ListSourceRepositoryBranchesRequest::maxResults))
            .setter(setter(Builder::maxResults))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("maxResults").build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(SPACE_NAME_FIELD,
            PROJECT_NAME_FIELD, SOURCE_REPOSITORY_NAME_FIELD, NEXT_TOKEN_FIELD, MAX_RESULTS_FIELD));

    private final String spaceName;

    private final String projectName;

    private final String sourceRepositoryName;

    private final String nextToken;

    private final Integer maxResults;

    private ListSourceRepositoryBranchesRequest(BuilderImpl builder) {
        super(builder);
        this.spaceName = builder.spaceName;
        this.projectName = builder.projectName;
        this.sourceRepositoryName = builder.sourceRepositoryName;
        this.nextToken = builder.nextToken;
        this.maxResults = builder.maxResults;
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
     * The name of the source repository.
     * </p>
     * 
     * @return The name of the source repository.
     */
    public final String sourceRepositoryName() {
        return sourceRepositoryName;
    }

    /**
     * <p>
     * A token returned from a call to this API to indicate the next batch of results to return, if any.
     * </p>
     * 
     * @return A token returned from a call to this API to indicate the next batch of results to return, if any.
     */
    public final String nextToken() {
        return nextToken;
    }

    /**
     * <p>
     * The maximum number of results to show in a single call to this API. If the number of results is larger than the
     * number you specified, the response will include a <code>NextToken</code> element, which you can use to obtain
     * additional results.
     * </p>
     * 
     * @return The maximum number of results to show in a single call to this API. If the number of results is larger
     *         than the number you specified, the response will include a <code>NextToken</code> element, which you can
     *         use to obtain additional results.
     */
    public final Integer maxResults() {
        return maxResults;
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
        hashCode = 31 * hashCode + Objects.hashCode(nextToken());
        hashCode = 31 * hashCode + Objects.hashCode(maxResults());
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
        if (!(obj instanceof ListSourceRepositoryBranchesRequest)) {
            return false;
        }
        ListSourceRepositoryBranchesRequest other = (ListSourceRepositoryBranchesRequest) obj;
        return Objects.equals(spaceName(), other.spaceName()) && Objects.equals(projectName(), other.projectName())
                && Objects.equals(sourceRepositoryName(), other.sourceRepositoryName())
                && Objects.equals(nextToken(), other.nextToken()) && Objects.equals(maxResults(), other.maxResults());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public final String toString() {
        return ToString.builder("ListSourceRepositoryBranchesRequest").add("SpaceName", spaceName())
                .add("ProjectName", projectName()).add("SourceRepositoryName", sourceRepositoryName())
                .add("NextToken", nextToken()).add("MaxResults", maxResults()).build();
    }

    public final <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
        case "spaceName":
            return Optional.ofNullable(clazz.cast(spaceName()));
        case "projectName":
            return Optional.ofNullable(clazz.cast(projectName()));
        case "sourceRepositoryName":
            return Optional.ofNullable(clazz.cast(sourceRepositoryName()));
        case "nextToken":
            return Optional.ofNullable(clazz.cast(nextToken()));
        case "maxResults":
            return Optional.ofNullable(clazz.cast(maxResults()));
        default:
            return Optional.empty();
        }
    }

    @Override
    public final List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    private static <T> Function<Object, T> getter(Function<ListSourceRepositoryBranchesRequest, T> g) {
        return obj -> g.apply((ListSourceRepositoryBranchesRequest) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    public interface Builder extends CodeCatalystRequest.Builder, SdkPojo,
            CopyableBuilder<Builder, ListSourceRepositoryBranchesRequest> {
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
         * The name of the source repository.
         * </p>
         * 
         * @param sourceRepositoryName
         *        The name of the source repository.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder sourceRepositoryName(String sourceRepositoryName);

        /**
         * <p>
         * A token returned from a call to this API to indicate the next batch of results to return, if any.
         * </p>
         * 
         * @param nextToken
         *        A token returned from a call to this API to indicate the next batch of results to return, if any.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder nextToken(String nextToken);

        /**
         * <p>
         * The maximum number of results to show in a single call to this API. If the number of results is larger than
         * the number you specified, the response will include a <code>NextToken</code> element, which you can use to
         * obtain additional results.
         * </p>
         * 
         * @param maxResults
         *        The maximum number of results to show in a single call to this API. If the number of results is larger
         *        than the number you specified, the response will include a <code>NextToken</code> element, which you
         *        can use to obtain additional results.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder maxResults(Integer maxResults);

        @Override
        Builder overrideConfiguration(AwsRequestOverrideConfiguration overrideConfiguration);

        @Override
        Builder overrideConfiguration(Consumer<AwsRequestOverrideConfiguration.Builder> builderConsumer);
    }

    static final class BuilderImpl extends CodeCatalystRequest.BuilderImpl implements Builder {
        private String spaceName;

        private String projectName;

        private String sourceRepositoryName;

        private String nextToken;

        private Integer maxResults;

        private BuilderImpl() {
        }

        private BuilderImpl(ListSourceRepositoryBranchesRequest model) {
            super(model);
            spaceName(model.spaceName);
            projectName(model.projectName);
            sourceRepositoryName(model.sourceRepositoryName);
            nextToken(model.nextToken);
            maxResults(model.maxResults);
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

        public final String getNextToken() {
            return nextToken;
        }

        public final void setNextToken(String nextToken) {
            this.nextToken = nextToken;
        }

        @Override
        public final Builder nextToken(String nextToken) {
            this.nextToken = nextToken;
            return this;
        }

        public final Integer getMaxResults() {
            return maxResults;
        }

        public final void setMaxResults(Integer maxResults) {
            this.maxResults = maxResults;
        }

        @Override
        public final Builder maxResults(Integer maxResults) {
            this.maxResults = maxResults;
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
        public ListSourceRepositoryBranchesRequest build() {
            return new ListSourceRepositoryBranchesRequest(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}
