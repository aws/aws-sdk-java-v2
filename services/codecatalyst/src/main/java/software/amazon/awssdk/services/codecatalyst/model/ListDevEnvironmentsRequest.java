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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.core.traits.ListTrait;
import software.amazon.awssdk.core.traits.LocationTrait;
import software.amazon.awssdk.core.util.DefaultSdkAutoConstructList;
import software.amazon.awssdk.core.util.SdkAutoConstructList;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 */
@Generated("software.amazon.awssdk:codegen")
public final class ListDevEnvironmentsRequest extends CodeCatalystRequest implements
        ToCopyableBuilder<ListDevEnvironmentsRequest.Builder, ListDevEnvironmentsRequest> {
    private static final SdkField<String> SPACE_NAME_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("spaceName").getter(getter(ListDevEnvironmentsRequest::spaceName)).setter(setter(Builder::spaceName))
            .traits(LocationTrait.builder().location(MarshallLocation.PATH).locationName("spaceName").build()).build();

    private static final SdkField<String> PROJECT_NAME_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("projectName").getter(getter(ListDevEnvironmentsRequest::projectName))
            .setter(setter(Builder::projectName))
            .traits(LocationTrait.builder().location(MarshallLocation.PATH).locationName("projectName").build()).build();

    private static final SdkField<List<Filter>> FILTERS_FIELD = SdkField
            .<List<Filter>> builder(MarshallingType.LIST)
            .memberName("filters")
            .getter(getter(ListDevEnvironmentsRequest::filters))
            .setter(setter(Builder::filters))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("filters").build(),
                    ListTrait
                            .builder()
                            .memberLocationName(null)
                            .memberFieldInfo(
                                    SdkField.<Filter> builder(MarshallingType.SDK_POJO)
                                            .constructor(Filter::builder)
                                            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD)
                                                    .locationName("member").build()).build()).build()).build();

    private static final SdkField<String> NEXT_TOKEN_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("nextToken").getter(getter(ListDevEnvironmentsRequest::nextToken)).setter(setter(Builder::nextToken))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("nextToken").build()).build();

    private static final SdkField<Integer> MAX_RESULTS_FIELD = SdkField.<Integer> builder(MarshallingType.INTEGER)
            .memberName("maxResults").getter(getter(ListDevEnvironmentsRequest::maxResults)).setter(setter(Builder::maxResults))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("maxResults").build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(SPACE_NAME_FIELD,
            PROJECT_NAME_FIELD, FILTERS_FIELD, NEXT_TOKEN_FIELD, MAX_RESULTS_FIELD));

    private final String spaceName;

    private final String projectName;

    private final List<Filter> filters;

    private final String nextToken;

    private final Integer maxResults;

    private ListDevEnvironmentsRequest(BuilderImpl builder) {
        super(builder);
        this.spaceName = builder.spaceName;
        this.projectName = builder.projectName;
        this.filters = builder.filters;
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
     * For responses, this returns true if the service returned a value for the Filters property. This DOES NOT check
     * that the value is non-empty (for which, you should check the {@code isEmpty()} method on the property). This is
     * useful because the SDK will never return a null collection or map, but you may need to differentiate between the
     * service returning nothing (or null) and the service returning an empty collection or map. For requests, this
     * returns true if a value for the property was specified in the request builder, and false if a value was not
     * specified.
     */
    public final boolean hasFilters() {
        return filters != null && !(filters instanceof SdkAutoConstructList);
    }

    /**
     * <p>
     * Information about filters to apply to narrow the results returned in the list.
     * </p>
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     * <p>
     * This method will never return null. If you would like to know whether the service returned this field (so that
     * you can differentiate between null and empty), you can use the {@link #hasFilters} method.
     * </p>
     * 
     * @return Information about filters to apply to narrow the results returned in the list.
     */
    public final List<Filter> filters() {
        return filters;
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
        hashCode = 31 * hashCode + Objects.hashCode(hasFilters() ? filters() : null);
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
        if (!(obj instanceof ListDevEnvironmentsRequest)) {
            return false;
        }
        ListDevEnvironmentsRequest other = (ListDevEnvironmentsRequest) obj;
        return Objects.equals(spaceName(), other.spaceName()) && Objects.equals(projectName(), other.projectName())
                && hasFilters() == other.hasFilters() && Objects.equals(filters(), other.filters())
                && Objects.equals(nextToken(), other.nextToken()) && Objects.equals(maxResults(), other.maxResults());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public final String toString() {
        return ToString.builder("ListDevEnvironmentsRequest").add("SpaceName", spaceName()).add("ProjectName", projectName())
                .add("Filters", hasFilters() ? filters() : null).add("NextToken", nextToken()).add("MaxResults", maxResults())
                .build();
    }

    public final <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
        case "spaceName":
            return Optional.ofNullable(clazz.cast(spaceName()));
        case "projectName":
            return Optional.ofNullable(clazz.cast(projectName()));
        case "filters":
            return Optional.ofNullable(clazz.cast(filters()));
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

    private static <T> Function<Object, T> getter(Function<ListDevEnvironmentsRequest, T> g) {
        return obj -> g.apply((ListDevEnvironmentsRequest) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    public interface Builder extends CodeCatalystRequest.Builder, SdkPojo, CopyableBuilder<Builder, ListDevEnvironmentsRequest> {
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
         * Information about filters to apply to narrow the results returned in the list.
         * </p>
         * 
         * @param filters
         *        Information about filters to apply to narrow the results returned in the list.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder filters(Collection<Filter> filters);

        /**
         * <p>
         * Information about filters to apply to narrow the results returned in the list.
         * </p>
         * 
         * @param filters
         *        Information about filters to apply to narrow the results returned in the list.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder filters(Filter... filters);

        /**
         * <p>
         * Information about filters to apply to narrow the results returned in the list.
         * </p>
         * This is a convenience method that creates an instance of the
         * {@link software.amazon.awssdk.services.codecatalyst.model.Filter.Builder} avoiding the need to create one
         * manually via {@link software.amazon.awssdk.services.codecatalyst.model.Filter#builder()}.
         *
         * <p>
         * When the {@link Consumer} completes,
         * {@link software.amazon.awssdk.services.codecatalyst.model.Filter.Builder#build()} is called immediately and
         * its result is passed to {@link #filters(List<Filter>)}.
         * 
         * @param filters
         *        a consumer that will call methods on
         *        {@link software.amazon.awssdk.services.codecatalyst.model.Filter.Builder}
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see #filters(java.util.Collection<Filter>)
         */
        Builder filters(Consumer<Filter.Builder>... filters);

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

        private List<Filter> filters = DefaultSdkAutoConstructList.getInstance();

        private String nextToken;

        private Integer maxResults;

        private BuilderImpl() {
        }

        private BuilderImpl(ListDevEnvironmentsRequest model) {
            super(model);
            spaceName(model.spaceName);
            projectName(model.projectName);
            filters(model.filters);
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

        public final List<Filter.Builder> getFilters() {
            List<Filter.Builder> result = FiltersCopier.copyToBuilder(this.filters);
            if (result instanceof SdkAutoConstructList) {
                return null;
            }
            return result;
        }

        public final void setFilters(Collection<Filter.BuilderImpl> filters) {
            this.filters = FiltersCopier.copyFromBuilder(filters);
        }

        @Override
        public final Builder filters(Collection<Filter> filters) {
            this.filters = FiltersCopier.copy(filters);
            return this;
        }

        @Override
        @SafeVarargs
        public final Builder filters(Filter... filters) {
            filters(Arrays.asList(filters));
            return this;
        }

        @Override
        @SafeVarargs
        public final Builder filters(Consumer<Filter.Builder>... filters) {
            filters(Stream.of(filters).map(c -> Filter.builder().applyMutation(c).build()).collect(Collectors.toList()));
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
        public ListDevEnvironmentsRequest build() {
            return new ListDevEnvironmentsRequest(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}
