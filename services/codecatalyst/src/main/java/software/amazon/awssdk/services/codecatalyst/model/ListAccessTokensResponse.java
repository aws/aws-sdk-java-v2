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
public final class ListAccessTokensResponse extends CodeCatalystResponse implements
        ToCopyableBuilder<ListAccessTokensResponse.Builder, ListAccessTokensResponse> {
    private static final SdkField<List<AccessTokenSummary>> ITEMS_FIELD = SdkField
            .<List<AccessTokenSummary>> builder(MarshallingType.LIST)
            .memberName("items")
            .getter(getter(ListAccessTokensResponse::items))
            .setter(setter(Builder::items))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("items").build(),
                    ListTrait
                            .builder()
                            .memberLocationName(null)
                            .memberFieldInfo(
                                    SdkField.<AccessTokenSummary> builder(MarshallingType.SDK_POJO)
                                            .constructor(AccessTokenSummary::builder)
                                            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD)
                                                    .locationName("member").build()).build()).build()).build();

    private static final SdkField<String> NEXT_TOKEN_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("nextToken").getter(getter(ListAccessTokensResponse::nextToken)).setter(setter(Builder::nextToken))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("nextToken").build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections
            .unmodifiableList(Arrays.asList(ITEMS_FIELD, NEXT_TOKEN_FIELD));

    private final List<AccessTokenSummary> items;

    private final String nextToken;

    private ListAccessTokensResponse(BuilderImpl builder) {
        super(builder);
        this.items = builder.items;
        this.nextToken = builder.nextToken;
    }

    /**
     * For responses, this returns true if the service returned a value for the Items property. This DOES NOT check that
     * the value is non-empty (for which, you should check the {@code isEmpty()} method on the property). This is useful
     * because the SDK will never return a null collection or map, but you may need to differentiate between the service
     * returning nothing (or null) and the service returning an empty collection or map. For requests, this returns true
     * if a value for the property was specified in the request builder, and false if a value was not specified.
     */
    public final boolean hasItems() {
        return items != null && !(items instanceof SdkAutoConstructList);
    }

    /**
     * <p>
     * A list of personal access tokens (PATs) associated with the calling user identity.
     * </p>
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     * <p>
     * This method will never return null. If you would like to know whether the service returned this field (so that
     * you can differentiate between null and empty), you can use the {@link #hasItems} method.
     * </p>
     * 
     * @return A list of personal access tokens (PATs) associated with the calling user identity.
     */
    public final List<AccessTokenSummary> items() {
        return items;
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
        hashCode = 31 * hashCode + Objects.hashCode(hasItems() ? items() : null);
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
        if (!(obj instanceof ListAccessTokensResponse)) {
            return false;
        }
        ListAccessTokensResponse other = (ListAccessTokensResponse) obj;
        return hasItems() == other.hasItems() && Objects.equals(items(), other.items())
                && Objects.equals(nextToken(), other.nextToken());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public final String toString() {
        return ToString.builder("ListAccessTokensResponse").add("Items", hasItems() ? items() : null)
                .add("NextToken", nextToken()).build();
    }

    public final <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
        case "items":
            return Optional.ofNullable(clazz.cast(items()));
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

    private static <T> Function<Object, T> getter(Function<ListAccessTokensResponse, T> g) {
        return obj -> g.apply((ListAccessTokensResponse) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    public interface Builder extends CodeCatalystResponse.Builder, SdkPojo, CopyableBuilder<Builder, ListAccessTokensResponse> {
        /**
         * <p>
         * A list of personal access tokens (PATs) associated with the calling user identity.
         * </p>
         * 
         * @param items
         *        A list of personal access tokens (PATs) associated with the calling user identity.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder items(Collection<AccessTokenSummary> items);

        /**
         * <p>
         * A list of personal access tokens (PATs) associated with the calling user identity.
         * </p>
         * 
         * @param items
         *        A list of personal access tokens (PATs) associated with the calling user identity.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder items(AccessTokenSummary... items);

        /**
         * <p>
         * A list of personal access tokens (PATs) associated with the calling user identity.
         * </p>
         * This is a convenience method that creates an instance of the
         * {@link software.amazon.awssdk.services.codecatalyst.model.AccessTokenSummary.Builder} avoiding the need to
         * create one manually via
         * {@link software.amazon.awssdk.services.codecatalyst.model.AccessTokenSummary#builder()}.
         *
         * <p>
         * When the {@link Consumer} completes,
         * {@link software.amazon.awssdk.services.codecatalyst.model.AccessTokenSummary.Builder#build()} is called
         * immediately and its result is passed to {@link #items(List<AccessTokenSummary>)}.
         * 
         * @param items
         *        a consumer that will call methods on
         *        {@link software.amazon.awssdk.services.codecatalyst.model.AccessTokenSummary.Builder}
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see #items(java.util.Collection<AccessTokenSummary>)
         */
        Builder items(Consumer<AccessTokenSummary.Builder>... items);

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
    }

    static final class BuilderImpl extends CodeCatalystResponse.BuilderImpl implements Builder {
        private List<AccessTokenSummary> items = DefaultSdkAutoConstructList.getInstance();

        private String nextToken;

        private BuilderImpl() {
        }

        private BuilderImpl(ListAccessTokensResponse model) {
            super(model);
            items(model.items);
            nextToken(model.nextToken);
        }

        public final List<AccessTokenSummary.Builder> getItems() {
            List<AccessTokenSummary.Builder> result = AccessTokenSummariesCopier.copyToBuilder(this.items);
            if (result instanceof SdkAutoConstructList) {
                return null;
            }
            return result;
        }

        public final void setItems(Collection<AccessTokenSummary.BuilderImpl> items) {
            this.items = AccessTokenSummariesCopier.copyFromBuilder(items);
        }

        @Override
        public final Builder items(Collection<AccessTokenSummary> items) {
            this.items = AccessTokenSummariesCopier.copy(items);
            return this;
        }

        @Override
        @SafeVarargs
        public final Builder items(AccessTokenSummary... items) {
            items(Arrays.asList(items));
            return this;
        }

        @Override
        @SafeVarargs
        public final Builder items(Consumer<AccessTokenSummary.Builder>... items) {
            items(Stream.of(items).map(c -> AccessTokenSummary.builder().applyMutation(c).build()).collect(Collectors.toList()));
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
        public ListAccessTokensResponse build() {
            return new ListAccessTokensResponse(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}
