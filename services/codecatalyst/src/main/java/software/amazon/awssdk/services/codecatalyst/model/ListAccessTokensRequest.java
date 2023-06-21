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
public final class ListAccessTokensRequest extends CodeCatalystRequest implements
        ToCopyableBuilder<ListAccessTokensRequest.Builder, ListAccessTokensRequest> {
    private static final SdkField<Integer> MAX_RESULTS_FIELD = SdkField.<Integer> builder(MarshallingType.INTEGER)
            .memberName("maxResults").getter(getter(ListAccessTokensRequest::maxResults)).setter(setter(Builder::maxResults))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("maxResults").build()).build();

    private static final SdkField<String> NEXT_TOKEN_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("nextToken").getter(getter(ListAccessTokensRequest::nextToken)).setter(setter(Builder::nextToken))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("nextToken").build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(MAX_RESULTS_FIELD,
            NEXT_TOKEN_FIELD));

    private final Integer maxResults;

    private final String nextToken;

    private ListAccessTokensRequest(BuilderImpl builder) {
        super(builder);
        this.maxResults = builder.maxResults;
        this.nextToken = builder.nextToken;
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
        hashCode = 31 * hashCode + Objects.hashCode(maxResults());
        hashCode = 31 * hashCode + Objects.hashCode(nextToken());
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
        if (!(obj instanceof ListAccessTokensRequest)) {
            return false;
        }
        ListAccessTokensRequest other = (ListAccessTokensRequest) obj;
        return Objects.equals(maxResults(), other.maxResults()) && Objects.equals(nextToken(), other.nextToken());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public final String toString() {
        return ToString.builder("ListAccessTokensRequest").add("MaxResults", maxResults()).add("NextToken", nextToken()).build();
    }

    public final <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
        case "maxResults":
            return Optional.ofNullable(clazz.cast(maxResults()));
        case "nextToken":
            return Optional.ofNullable(clazz.cast(nextToken()));
        default:
            return Optional.empty();
        }
    }

    @Override
    public final List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    private static <T> Function<Object, T> getter(Function<ListAccessTokensRequest, T> g) {
        return obj -> g.apply((ListAccessTokensRequest) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    public interface Builder extends CodeCatalystRequest.Builder, SdkPojo, CopyableBuilder<Builder, ListAccessTokensRequest> {
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

        @Override
        Builder overrideConfiguration(AwsRequestOverrideConfiguration overrideConfiguration);

        @Override
        Builder overrideConfiguration(Consumer<AwsRequestOverrideConfiguration.Builder> builderConsumer);
    }

    static final class BuilderImpl extends CodeCatalystRequest.BuilderImpl implements Builder {
        private Integer maxResults;

        private String nextToken;

        private BuilderImpl() {
        }

        private BuilderImpl(ListAccessTokensRequest model) {
            super(model);
            maxResults(model.maxResults);
            nextToken(model.nextToken);
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
        public ListAccessTokensRequest build() {
            return new ListAccessTokensRequest(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}
