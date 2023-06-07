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
public final class ListCertificatesResponse extends AcmResponse implements
        ToCopyableBuilder<ListCertificatesResponse.Builder, ListCertificatesResponse> {
    private static final SdkField<String> NEXT_TOKEN_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("NextToken").getter(getter(ListCertificatesResponse::nextToken)).setter(setter(Builder::nextToken))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("NextToken").build()).build();

    private static final SdkField<List<CertificateSummary>> CERTIFICATE_SUMMARY_LIST_FIELD = SdkField
            .<List<CertificateSummary>> builder(MarshallingType.LIST)
            .memberName("CertificateSummaryList")
            .getter(getter(ListCertificatesResponse::certificateSummaryList))
            .setter(setter(Builder::certificateSummaryList))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("CertificateSummaryList").build(),
                    ListTrait
                            .builder()
                            .memberLocationName(null)
                            .memberFieldInfo(
                                    SdkField.<CertificateSummary> builder(MarshallingType.SDK_POJO)
                                            .constructor(CertificateSummary::builder)
                                            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD)
                                                    .locationName("member").build()).build()).build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(NEXT_TOKEN_FIELD,
            CERTIFICATE_SUMMARY_LIST_FIELD));

    private final String nextToken;

    private final List<CertificateSummary> certificateSummaryList;

    private ListCertificatesResponse(BuilderImpl builder) {
        super(builder);
        this.nextToken = builder.nextToken;
        this.certificateSummaryList = builder.certificateSummaryList;
    }

    /**
     * <p>
     * When the list is truncated, this value is present and contains the value to use for the <code>NextToken</code>
     * parameter in a subsequent pagination request.
     * </p>
     * 
     * @return When the list is truncated, this value is present and contains the value to use for the
     *         <code>NextToken</code> parameter in a subsequent pagination request.
     */
    public final String nextToken() {
        return nextToken;
    }

    /**
     * For responses, this returns true if the service returned a value for the CertificateSummaryList property. This
     * DOES NOT check that the value is non-empty (for which, you should check the {@code isEmpty()} method on the
     * property). This is useful because the SDK will never return a null collection or map, but you may need to
     * differentiate between the service returning nothing (or null) and the service returning an empty collection or
     * map. For requests, this returns true if a value for the property was specified in the request builder, and false
     * if a value was not specified.
     */
    public final boolean hasCertificateSummaryList() {
        return certificateSummaryList != null && !(certificateSummaryList instanceof SdkAutoConstructList);
    }

    /**
     * <p>
     * A list of ACM certificates.
     * </p>
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     * <p>
     * This method will never return null. If you would like to know whether the service returned this field (so that
     * you can differentiate between null and empty), you can use the {@link #hasCertificateSummaryList} method.
     * </p>
     * 
     * @return A list of ACM certificates.
     */
    public final List<CertificateSummary> certificateSummaryList() {
        return certificateSummaryList;
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
        hashCode = 31 * hashCode + Objects.hashCode(nextToken());
        hashCode = 31 * hashCode + Objects.hashCode(hasCertificateSummaryList() ? certificateSummaryList() : null);
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
        if (!(obj instanceof ListCertificatesResponse)) {
            return false;
        }
        ListCertificatesResponse other = (ListCertificatesResponse) obj;
        return Objects.equals(nextToken(), other.nextToken()) && hasCertificateSummaryList() == other.hasCertificateSummaryList()
                && Objects.equals(certificateSummaryList(), other.certificateSummaryList());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public final String toString() {
        return ToString.builder("ListCertificatesResponse").add("NextToken", nextToken())
                .add("CertificateSummaryList", hasCertificateSummaryList() ? certificateSummaryList() : null).build();
    }

    public final <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
        case "NextToken":
            return Optional.ofNullable(clazz.cast(nextToken()));
        case "CertificateSummaryList":
            return Optional.ofNullable(clazz.cast(certificateSummaryList()));
        default:
            return Optional.empty();
        }
    }

    @Override
    public final List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    private static <T> Function<Object, T> getter(Function<ListCertificatesResponse, T> g) {
        return obj -> g.apply((ListCertificatesResponse) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    public interface Builder extends AcmResponse.Builder, SdkPojo, CopyableBuilder<Builder, ListCertificatesResponse> {
        /**
         * <p>
         * When the list is truncated, this value is present and contains the value to use for the
         * <code>NextToken</code> parameter in a subsequent pagination request.
         * </p>
         * 
         * @param nextToken
         *        When the list is truncated, this value is present and contains the value to use for the
         *        <code>NextToken</code> parameter in a subsequent pagination request.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder nextToken(String nextToken);

        /**
         * <p>
         * A list of ACM certificates.
         * </p>
         * 
         * @param certificateSummaryList
         *        A list of ACM certificates.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder certificateSummaryList(Collection<CertificateSummary> certificateSummaryList);

        /**
         * <p>
         * A list of ACM certificates.
         * </p>
         * 
         * @param certificateSummaryList
         *        A list of ACM certificates.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder certificateSummaryList(CertificateSummary... certificateSummaryList);

        /**
         * <p>
         * A list of ACM certificates.
         * </p>
         * This is a convenience method that creates an instance of the
         * {@link software.amazon.awssdk.services.acm.model.CertificateSummary.Builder} avoiding the need to create one
         * manually via {@link software.amazon.awssdk.services.acm.model.CertificateSummary#builder()}.
         *
         * <p>
         * When the {@link Consumer} completes,
         * {@link software.amazon.awssdk.services.acm.model.CertificateSummary.Builder#build()} is called immediately
         * and its result is passed to {@link #certificateSummaryList(List<CertificateSummary>)}.
         * 
         * @param certificateSummaryList
         *        a consumer that will call methods on
         *        {@link software.amazon.awssdk.services.acm.model.CertificateSummary.Builder}
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see #certificateSummaryList(java.util.Collection<CertificateSummary>)
         */
        Builder certificateSummaryList(Consumer<CertificateSummary.Builder>... certificateSummaryList);
    }

    static final class BuilderImpl extends AcmResponse.BuilderImpl implements Builder {
        private String nextToken;

        private List<CertificateSummary> certificateSummaryList = DefaultSdkAutoConstructList.getInstance();

        private BuilderImpl() {
        }

        private BuilderImpl(ListCertificatesResponse model) {
            super(model);
            nextToken(model.nextToken);
            certificateSummaryList(model.certificateSummaryList);
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

        public final List<CertificateSummary.Builder> getCertificateSummaryList() {
            List<CertificateSummary.Builder> result = CertificateSummaryListCopier.copyToBuilder(this.certificateSummaryList);
            if (result instanceof SdkAutoConstructList) {
                return null;
            }
            return result;
        }

        public final void setCertificateSummaryList(Collection<CertificateSummary.BuilderImpl> certificateSummaryList) {
            this.certificateSummaryList = CertificateSummaryListCopier.copyFromBuilder(certificateSummaryList);
        }

        @Override
        public final Builder certificateSummaryList(Collection<CertificateSummary> certificateSummaryList) {
            this.certificateSummaryList = CertificateSummaryListCopier.copy(certificateSummaryList);
            return this;
        }

        @Override
        @SafeVarargs
        public final Builder certificateSummaryList(CertificateSummary... certificateSummaryList) {
            certificateSummaryList(Arrays.asList(certificateSummaryList));
            return this;
        }

        @Override
        @SafeVarargs
        public final Builder certificateSummaryList(Consumer<CertificateSummary.Builder>... certificateSummaryList) {
            certificateSummaryList(Stream.of(certificateSummaryList)
                    .map(c -> CertificateSummary.builder().applyMutation(c).build()).collect(Collectors.toList()));
            return this;
        }

        @Override
        public ListCertificatesResponse build() {
            return new ListCertificatesResponse(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}
