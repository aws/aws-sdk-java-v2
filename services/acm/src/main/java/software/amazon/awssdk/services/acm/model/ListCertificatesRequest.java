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

package software.amazon.awssdk.services.acm.model;

import java.util.Arrays;
import java.util.Collection;
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
public final class ListCertificatesRequest extends AcmRequest implements
        ToCopyableBuilder<ListCertificatesRequest.Builder, ListCertificatesRequest> {
    private static final SdkField<List<String>> CERTIFICATE_STATUSES_FIELD = SdkField
            .<List<String>> builder(MarshallingType.LIST)
            .memberName("CertificateStatuses")
            .getter(getter(ListCertificatesRequest::certificateStatusesAsStrings))
            .setter(setter(Builder::certificateStatusesWithStrings))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("CertificateStatuses").build(),
                    ListTrait
                            .builder()
                            .memberLocationName(null)
                            .memberFieldInfo(
                                    SdkField.<String> builder(MarshallingType.STRING)
                                            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD)
                                                    .locationName("member").build()).build()).build()).build();

    private static final SdkField<Filters> INCLUDES_FIELD = SdkField.<Filters> builder(MarshallingType.SDK_POJO)
            .memberName("Includes").getter(getter(ListCertificatesRequest::includes)).setter(setter(Builder::includes))
            .constructor(Filters::builder)
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("Includes").build()).build();

    private static final SdkField<String> NEXT_TOKEN_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("NextToken").getter(getter(ListCertificatesRequest::nextToken)).setter(setter(Builder::nextToken))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("NextToken").build()).build();

    private static final SdkField<Integer> MAX_ITEMS_FIELD = SdkField.<Integer> builder(MarshallingType.INTEGER)
            .memberName("MaxItems").getter(getter(ListCertificatesRequest::maxItems)).setter(setter(Builder::maxItems))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("MaxItems").build()).build();

    private static final SdkField<String> SORT_BY_FIELD = SdkField.<String> builder(MarshallingType.STRING).memberName("SortBy")
            .getter(getter(ListCertificatesRequest::sortByAsString)).setter(setter(Builder::sortBy))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("SortBy").build()).build();

    private static final SdkField<String> SORT_ORDER_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("SortOrder").getter(getter(ListCertificatesRequest::sortOrderAsString))
            .setter(setter(Builder::sortOrder))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("SortOrder").build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(CERTIFICATE_STATUSES_FIELD,
            INCLUDES_FIELD, NEXT_TOKEN_FIELD, MAX_ITEMS_FIELD, SORT_BY_FIELD, SORT_ORDER_FIELD));

    private final List<String> certificateStatuses;

    private final Filters includes;

    private final String nextToken;

    private final Integer maxItems;

    private final String sortBy;

    private final String sortOrder;

    private ListCertificatesRequest(BuilderImpl builder) {
        super(builder);
        this.certificateStatuses = builder.certificateStatuses;
        this.includes = builder.includes;
        this.nextToken = builder.nextToken;
        this.maxItems = builder.maxItems;
        this.sortBy = builder.sortBy;
        this.sortOrder = builder.sortOrder;
    }

    /**
     * <p>
     * Filter the certificate list by status value.
     * </p>
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     * <p>
     * This method will never return null. If you would like to know whether the service returned this field (so that
     * you can differentiate between null and empty), you can use the {@link #hasCertificateStatuses} method.
     * </p>
     * 
     * @return Filter the certificate list by status value.
     */
    public final List<CertificateStatus> certificateStatuses() {
        return CertificateStatusesCopier.copyStringToEnum(certificateStatuses);
    }

    /**
     * For responses, this returns true if the service returned a value for the CertificateStatuses property. This DOES
     * NOT check that the value is non-empty (for which, you should check the {@code isEmpty()} method on the property).
     * This is useful because the SDK will never return a null collection or map, but you may need to differentiate
     * between the service returning nothing (or null) and the service returning an empty collection or map. For
     * requests, this returns true if a value for the property was specified in the request builder, and false if a
     * value was not specified.
     */
    public final boolean hasCertificateStatuses() {
        return certificateStatuses != null && !(certificateStatuses instanceof SdkAutoConstructList);
    }

    /**
     * <p>
     * Filter the certificate list by status value.
     * </p>
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     * <p>
     * This method will never return null. If you would like to know whether the service returned this field (so that
     * you can differentiate between null and empty), you can use the {@link #hasCertificateStatuses} method.
     * </p>
     * 
     * @return Filter the certificate list by status value.
     */
    public final List<String> certificateStatusesAsStrings() {
        return certificateStatuses;
    }

    /**
     * <p>
     * Filter the certificate list. For more information, see the <a>Filters</a> structure.
     * </p>
     * 
     * @return Filter the certificate list. For more information, see the <a>Filters</a> structure.
     */
    public final Filters includes() {
        return includes;
    }

    /**
     * <p>
     * Use this parameter only when paginating results and only in a subsequent request after you receive a response
     * with truncated results. Set it to the value of <code>NextToken</code> from the response you just received.
     * </p>
     * 
     * @return Use this parameter only when paginating results and only in a subsequent request after you receive a
     *         response with truncated results. Set it to the value of <code>NextToken</code> from the response you just
     *         received.
     */
    public final String nextToken() {
        return nextToken;
    }

    /**
     * <p>
     * Use this parameter when paginating results to specify the maximum number of items to return in the response. If
     * additional items exist beyond the number you specify, the <code>NextToken</code> element is sent in the response.
     * Use this <code>NextToken</code> value in a subsequent request to retrieve additional items.
     * </p>
     * 
     * @return Use this parameter when paginating results to specify the maximum number of items to return in the
     *         response. If additional items exist beyond the number you specify, the <code>NextToken</code> element is
     *         sent in the response. Use this <code>NextToken</code> value in a subsequent request to retrieve
     *         additional items.
     */
    public final Integer maxItems() {
        return maxItems;
    }

    /**
     * <p>
     * Specifies the field to sort results by. If you specify <code>SortBy</code>, you must also specify
     * <code>SortOrder</code>.
     * </p>
     * <p>
     * If the service returns an enum value that is not available in the current SDK version, {@link #sortBy} will
     * return {@link SortBy#UNKNOWN_TO_SDK_VERSION}. The raw value returned by the service is available from
     * {@link #sortByAsString}.
     * </p>
     * 
     * @return Specifies the field to sort results by. If you specify <code>SortBy</code>, you must also specify
     *         <code>SortOrder</code>.
     * @see SortBy
     */
    public final SortBy sortBy() {
        return SortBy.fromValue(sortBy);
    }

    /**
     * <p>
     * Specifies the field to sort results by. If you specify <code>SortBy</code>, you must also specify
     * <code>SortOrder</code>.
     * </p>
     * <p>
     * If the service returns an enum value that is not available in the current SDK version, {@link #sortBy} will
     * return {@link SortBy#UNKNOWN_TO_SDK_VERSION}. The raw value returned by the service is available from
     * {@link #sortByAsString}.
     * </p>
     * 
     * @return Specifies the field to sort results by. If you specify <code>SortBy</code>, you must also specify
     *         <code>SortOrder</code>.
     * @see SortBy
     */
    public final String sortByAsString() {
        return sortBy;
    }

    /**
     * <p>
     * Specifies the order of sorted results. If you specify <code>SortOrder</code>, you must also specify
     * <code>SortBy</code>.
     * </p>
     * <p>
     * If the service returns an enum value that is not available in the current SDK version, {@link #sortOrder} will
     * return {@link SortOrder#UNKNOWN_TO_SDK_VERSION}. The raw value returned by the service is available from
     * {@link #sortOrderAsString}.
     * </p>
     * 
     * @return Specifies the order of sorted results. If you specify <code>SortOrder</code>, you must also specify
     *         <code>SortBy</code>.
     * @see SortOrder
     */
    public final SortOrder sortOrder() {
        return SortOrder.fromValue(sortOrder);
    }

    /**
     * <p>
     * Specifies the order of sorted results. If you specify <code>SortOrder</code>, you must also specify
     * <code>SortBy</code>.
     * </p>
     * <p>
     * If the service returns an enum value that is not available in the current SDK version, {@link #sortOrder} will
     * return {@link SortOrder#UNKNOWN_TO_SDK_VERSION}. The raw value returned by the service is available from
     * {@link #sortOrderAsString}.
     * </p>
     * 
     * @return Specifies the order of sorted results. If you specify <code>SortOrder</code>, you must also specify
     *         <code>SortBy</code>.
     * @see SortOrder
     */
    public final String sortOrderAsString() {
        return sortOrder;
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
        hashCode = 31 * hashCode + Objects.hashCode(hasCertificateStatuses() ? certificateStatusesAsStrings() : null);
        hashCode = 31 * hashCode + Objects.hashCode(includes());
        hashCode = 31 * hashCode + Objects.hashCode(nextToken());
        hashCode = 31 * hashCode + Objects.hashCode(maxItems());
        hashCode = 31 * hashCode + Objects.hashCode(sortByAsString());
        hashCode = 31 * hashCode + Objects.hashCode(sortOrderAsString());
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
        if (!(obj instanceof ListCertificatesRequest)) {
            return false;
        }
        ListCertificatesRequest other = (ListCertificatesRequest) obj;
        return hasCertificateStatuses() == other.hasCertificateStatuses()
                && Objects.equals(certificateStatusesAsStrings(), other.certificateStatusesAsStrings())
                && Objects.equals(includes(), other.includes()) && Objects.equals(nextToken(), other.nextToken())
                && Objects.equals(maxItems(), other.maxItems()) && Objects.equals(sortByAsString(), other.sortByAsString())
                && Objects.equals(sortOrderAsString(), other.sortOrderAsString());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public final String toString() {
        return ToString.builder("ListCertificatesRequest")
                .add("CertificateStatuses", hasCertificateStatuses() ? certificateStatusesAsStrings() : null)
                .add("Includes", includes()).add("NextToken", nextToken()).add("MaxItems", maxItems())
                .add("SortBy", sortByAsString()).add("SortOrder", sortOrderAsString()).build();
    }

    public final <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
        case "CertificateStatuses":
            return Optional.ofNullable(clazz.cast(certificateStatusesAsStrings()));
        case "Includes":
            return Optional.ofNullable(clazz.cast(includes()));
        case "NextToken":
            return Optional.ofNullable(clazz.cast(nextToken()));
        case "MaxItems":
            return Optional.ofNullable(clazz.cast(maxItems()));
        case "SortBy":
            return Optional.ofNullable(clazz.cast(sortByAsString()));
        case "SortOrder":
            return Optional.ofNullable(clazz.cast(sortOrderAsString()));
        default:
            return Optional.empty();
        }
    }

    @Override
    public final List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    private static <T> Function<Object, T> getter(Function<ListCertificatesRequest, T> g) {
        return obj -> g.apply((ListCertificatesRequest) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    public interface Builder extends AcmRequest.Builder, SdkPojo, CopyableBuilder<Builder, ListCertificatesRequest> {
        /**
         * <p>
         * Filter the certificate list by status value.
         * </p>
         * 
         * @param certificateStatuses
         *        Filter the certificate list by status value.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder certificateStatusesWithStrings(Collection<String> certificateStatuses);

        /**
         * <p>
         * Filter the certificate list by status value.
         * </p>
         * 
         * @param certificateStatuses
         *        Filter the certificate list by status value.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder certificateStatusesWithStrings(String... certificateStatuses);

        /**
         * <p>
         * Filter the certificate list by status value.
         * </p>
         * 
         * @param certificateStatuses
         *        Filter the certificate list by status value.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder certificateStatuses(Collection<CertificateStatus> certificateStatuses);

        /**
         * <p>
         * Filter the certificate list by status value.
         * </p>
         * 
         * @param certificateStatuses
         *        Filter the certificate list by status value.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder certificateStatuses(CertificateStatus... certificateStatuses);

        /**
         * <p>
         * Filter the certificate list. For more information, see the <a>Filters</a> structure.
         * </p>
         * 
         * @param includes
         *        Filter the certificate list. For more information, see the <a>Filters</a> structure.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder includes(Filters includes);

        /**
         * <p>
         * Filter the certificate list. For more information, see the <a>Filters</a> structure.
         * </p>
         * This is a convenience method that creates an instance of the {@link Filters.Builder} avoiding the need to
         * create one manually via {@link Filters#builder()}.
         *
         * <p>
         * When the {@link Consumer} completes, {@link Filters.Builder#build()} is called immediately and its result is
         * passed to {@link #includes(Filters)}.
         * 
         * @param includes
         *        a consumer that will call methods on {@link Filters.Builder}
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see #includes(Filters)
         */
        default Builder includes(Consumer<Filters.Builder> includes) {
            return includes(Filters.builder().applyMutation(includes).build());
        }

        /**
         * <p>
         * Use this parameter only when paginating results and only in a subsequent request after you receive a response
         * with truncated results. Set it to the value of <code>NextToken</code> from the response you just received.
         * </p>
         * 
         * @param nextToken
         *        Use this parameter only when paginating results and only in a subsequent request after you receive a
         *        response with truncated results. Set it to the value of <code>NextToken</code> from the response you
         *        just received.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder nextToken(String nextToken);

        /**
         * <p>
         * Use this parameter when paginating results to specify the maximum number of items to return in the response.
         * If additional items exist beyond the number you specify, the <code>NextToken</code> element is sent in the
         * response. Use this <code>NextToken</code> value in a subsequent request to retrieve additional items.
         * </p>
         * 
         * @param maxItems
         *        Use this parameter when paginating results to specify the maximum number of items to return in the
         *        response. If additional items exist beyond the number you specify, the <code>NextToken</code> element
         *        is sent in the response. Use this <code>NextToken</code> value in a subsequent request to retrieve
         *        additional items.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder maxItems(Integer maxItems);

        /**
         * <p>
         * Specifies the field to sort results by. If you specify <code>SortBy</code>, you must also specify
         * <code>SortOrder</code>.
         * </p>
         * 
         * @param sortBy
         *        Specifies the field to sort results by. If you specify <code>SortBy</code>, you must also specify
         *        <code>SortOrder</code>.
         * @see SortBy
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see SortBy
         */
        Builder sortBy(String sortBy);

        /**
         * <p>
         * Specifies the field to sort results by. If you specify <code>SortBy</code>, you must also specify
         * <code>SortOrder</code>.
         * </p>
         * 
         * @param sortBy
         *        Specifies the field to sort results by. If you specify <code>SortBy</code>, you must also specify
         *        <code>SortOrder</code>.
         * @see SortBy
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see SortBy
         */
        Builder sortBy(SortBy sortBy);

        /**
         * <p>
         * Specifies the order of sorted results. If you specify <code>SortOrder</code>, you must also specify
         * <code>SortBy</code>.
         * </p>
         * 
         * @param sortOrder
         *        Specifies the order of sorted results. If you specify <code>SortOrder</code>, you must also specify
         *        <code>SortBy</code>.
         * @see SortOrder
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see SortOrder
         */
        Builder sortOrder(String sortOrder);

        /**
         * <p>
         * Specifies the order of sorted results. If you specify <code>SortOrder</code>, you must also specify
         * <code>SortBy</code>.
         * </p>
         * 
         * @param sortOrder
         *        Specifies the order of sorted results. If you specify <code>SortOrder</code>, you must also specify
         *        <code>SortBy</code>.
         * @see SortOrder
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see SortOrder
         */
        Builder sortOrder(SortOrder sortOrder);

        @Override
        Builder overrideConfiguration(AwsRequestOverrideConfiguration overrideConfiguration);

        @Override
        Builder overrideConfiguration(Consumer<AwsRequestOverrideConfiguration.Builder> builderConsumer);
    }

    static final class BuilderImpl extends AcmRequest.BuilderImpl implements Builder {
        private List<String> certificateStatuses = DefaultSdkAutoConstructList.getInstance();

        private Filters includes;

        private String nextToken;

        private Integer maxItems;

        private String sortBy;

        private String sortOrder;

        private BuilderImpl() {
        }

        private BuilderImpl(ListCertificatesRequest model) {
            super(model);
            certificateStatusesWithStrings(model.certificateStatuses);
            includes(model.includes);
            nextToken(model.nextToken);
            maxItems(model.maxItems);
            sortBy(model.sortBy);
            sortOrder(model.sortOrder);
        }

        public final Collection<String> getCertificateStatuses() {
            if (certificateStatuses instanceof SdkAutoConstructList) {
                return null;
            }
            return certificateStatuses;
        }

        public final void setCertificateStatuses(Collection<String> certificateStatuses) {
            this.certificateStatuses = CertificateStatusesCopier.copy(certificateStatuses);
        }

        @Override
        public final Builder certificateStatusesWithStrings(Collection<String> certificateStatuses) {
            this.certificateStatuses = CertificateStatusesCopier.copy(certificateStatuses);
            return this;
        }

        @Override
        @SafeVarargs
        public final Builder certificateStatusesWithStrings(String... certificateStatuses) {
            certificateStatusesWithStrings(Arrays.asList(certificateStatuses));
            return this;
        }

        @Override
        public final Builder certificateStatuses(Collection<CertificateStatus> certificateStatuses) {
            this.certificateStatuses = CertificateStatusesCopier.copyEnumToString(certificateStatuses);
            return this;
        }

        @Override
        @SafeVarargs
        public final Builder certificateStatuses(CertificateStatus... certificateStatuses) {
            certificateStatuses(Arrays.asList(certificateStatuses));
            return this;
        }

        public final Filters.Builder getIncludes() {
            return includes != null ? includes.toBuilder() : null;
        }

        public final void setIncludes(Filters.BuilderImpl includes) {
            this.includes = includes != null ? includes.build() : null;
        }

        @Override
        public final Builder includes(Filters includes) {
            this.includes = includes;
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

        public final Integer getMaxItems() {
            return maxItems;
        }

        public final void setMaxItems(Integer maxItems) {
            this.maxItems = maxItems;
        }

        @Override
        public final Builder maxItems(Integer maxItems) {
            this.maxItems = maxItems;
            return this;
        }

        public final String getSortBy() {
            return sortBy;
        }

        public final void setSortBy(String sortBy) {
            this.sortBy = sortBy;
        }

        @Override
        public final Builder sortBy(String sortBy) {
            this.sortBy = sortBy;
            return this;
        }

        @Override
        public final Builder sortBy(SortBy sortBy) {
            this.sortBy(sortBy == null ? null : sortBy.toString());
            return this;
        }

        public final String getSortOrder() {
            return sortOrder;
        }

        public final void setSortOrder(String sortOrder) {
            this.sortOrder = sortOrder;
        }

        @Override
        public final Builder sortOrder(String sortOrder) {
            this.sortOrder = sortOrder;
            return this;
        }

        @Override
        public final Builder sortOrder(SortOrder sortOrder) {
            this.sortOrder(sortOrder == null ? null : sortOrder.toString());
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
        public ListCertificatesRequest build() {
            return new ListCertificatesRequest(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}
