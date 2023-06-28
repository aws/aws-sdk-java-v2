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

import java.io.Serializable;
import java.time.Instant;
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
 * <p>
 * Contains information about the status of ACM's <a
 * href="https://docs.aws.amazon.com/acm/latest/userguide/acm-renewal.html">managed renewal</a> for the certificate.
 * This structure exists only when the certificate type is <code>AMAZON_ISSUED</code>.
 * </p>
 */
@Generated("software.amazon.awssdk:codegen")
public final class RenewalSummary implements SdkPojo, Serializable, ToCopyableBuilder<RenewalSummary.Builder, RenewalSummary> {
    private static final SdkField<String> RENEWAL_STATUS_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("RenewalStatus").getter(getter(RenewalSummary::renewalStatusAsString))
            .setter(setter(Builder::renewalStatus))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("RenewalStatus").build()).build();

    private static final SdkField<List<DomainValidation>> DOMAIN_VALIDATION_OPTIONS_FIELD = SdkField
            .<List<DomainValidation>> builder(MarshallingType.LIST)
            .memberName("DomainValidationOptions")
            .getter(getter(RenewalSummary::domainValidationOptions))
            .setter(setter(Builder::domainValidationOptions))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("DomainValidationOptions").build(),
                    ListTrait
                            .builder()
                            .memberLocationName(null)
                            .memberFieldInfo(
                                    SdkField.<DomainValidation> builder(MarshallingType.SDK_POJO)
                                            .constructor(DomainValidation::builder)
                                            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD)
                                                    .locationName("member").build()).build()).build()).build();

    private static final SdkField<String> RENEWAL_STATUS_REASON_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("RenewalStatusReason").getter(getter(RenewalSummary::renewalStatusReasonAsString))
            .setter(setter(Builder::renewalStatusReason))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("RenewalStatusReason").build())
            .build();

    private static final SdkField<Instant> UPDATED_AT_FIELD = SdkField.<Instant> builder(MarshallingType.INSTANT)
            .memberName("UpdatedAt").getter(getter(RenewalSummary::updatedAt)).setter(setter(Builder::updatedAt))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("UpdatedAt").build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(RENEWAL_STATUS_FIELD,
            DOMAIN_VALIDATION_OPTIONS_FIELD, RENEWAL_STATUS_REASON_FIELD, UPDATED_AT_FIELD));

    private static final long serialVersionUID = 1L;

    private final String renewalStatus;

    private final List<DomainValidation> domainValidationOptions;

    private final String renewalStatusReason;

    private final Instant updatedAt;

    private RenewalSummary(BuilderImpl builder) {
        this.renewalStatus = builder.renewalStatus;
        this.domainValidationOptions = builder.domainValidationOptions;
        this.renewalStatusReason = builder.renewalStatusReason;
        this.updatedAt = builder.updatedAt;
    }

    /**
     * <p>
     * The status of ACM's <a href="https://docs.aws.amazon.com/acm/latest/userguide/acm-renewal.html">managed
     * renewal</a> of the certificate.
     * </p>
     * <p>
     * If the service returns an enum value that is not available in the current SDK version, {@link #renewalStatus}
     * will return {@link RenewalStatus#UNKNOWN_TO_SDK_VERSION}. The raw value returned by the service is available from
     * {@link #renewalStatusAsString}.
     * </p>
     * 
     * @return The status of ACM's <a href="https://docs.aws.amazon.com/acm/latest/userguide/acm-renewal.html">managed
     *         renewal</a> of the certificate.
     * @see RenewalStatus
     */
    public final RenewalStatus renewalStatus() {
        return RenewalStatus.fromValue(renewalStatus);
    }

    /**
     * <p>
     * The status of ACM's <a href="https://docs.aws.amazon.com/acm/latest/userguide/acm-renewal.html">managed
     * renewal</a> of the certificate.
     * </p>
     * <p>
     * If the service returns an enum value that is not available in the current SDK version, {@link #renewalStatus}
     * will return {@link RenewalStatus#UNKNOWN_TO_SDK_VERSION}. The raw value returned by the service is available from
     * {@link #renewalStatusAsString}.
     * </p>
     * 
     * @return The status of ACM's <a href="https://docs.aws.amazon.com/acm/latest/userguide/acm-renewal.html">managed
     *         renewal</a> of the certificate.
     * @see RenewalStatus
     */
    public final String renewalStatusAsString() {
        return renewalStatus;
    }

    /**
     * For responses, this returns true if the service returned a value for the DomainValidationOptions property. This
     * DOES NOT check that the value is non-empty (for which, you should check the {@code isEmpty()} method on the
     * property). This is useful because the SDK will never return a null collection or map, but you may need to
     * differentiate between the service returning nothing (or null) and the service returning an empty collection or
     * map. For requests, this returns true if a value for the property was specified in the request builder, and false
     * if a value was not specified.
     */
    public final boolean hasDomainValidationOptions() {
        return domainValidationOptions != null && !(domainValidationOptions instanceof SdkAutoConstructList);
    }

    /**
     * <p>
     * Contains information about the validation of each domain name in the certificate, as it pertains to ACM's <a
     * href="https://docs.aws.amazon.com/acm/latest/userguide/acm-renewal.html">managed renewal</a>. This is different
     * from the initial validation that occurs as a result of the <a>RequestCertificate</a> request. This field exists
     * only when the certificate type is <code>AMAZON_ISSUED</code>.
     * </p>
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     * <p>
     * This method will never return null. If you would like to know whether the service returned this field (so that
     * you can differentiate between null and empty), you can use the {@link #hasDomainValidationOptions} method.
     * </p>
     * 
     * @return Contains information about the validation of each domain name in the certificate, as it pertains to ACM's
     *         <a href="https://docs.aws.amazon.com/acm/latest/userguide/acm-renewal.html">managed renewal</a>. This is
     *         different from the initial validation that occurs as a result of the <a>RequestCertificate</a> request.
     *         This field exists only when the certificate type is <code>AMAZON_ISSUED</code>.
     */
    public final List<DomainValidation> domainValidationOptions() {
        return domainValidationOptions;
    }

    /**
     * <p>
     * The reason that a renewal request was unsuccessful.
     * </p>
     * <p>
     * If the service returns an enum value that is not available in the current SDK version,
     * {@link #renewalStatusReason} will return {@link FailureReason#UNKNOWN_TO_SDK_VERSION}. The raw value returned by
     * the service is available from {@link #renewalStatusReasonAsString}.
     * </p>
     * 
     * @return The reason that a renewal request was unsuccessful.
     * @see FailureReason
     */
    public final FailureReason renewalStatusReason() {
        return FailureReason.fromValue(renewalStatusReason);
    }

    /**
     * <p>
     * The reason that a renewal request was unsuccessful.
     * </p>
     * <p>
     * If the service returns an enum value that is not available in the current SDK version,
     * {@link #renewalStatusReason} will return {@link FailureReason#UNKNOWN_TO_SDK_VERSION}. The raw value returned by
     * the service is available from {@link #renewalStatusReasonAsString}.
     * </p>
     * 
     * @return The reason that a renewal request was unsuccessful.
     * @see FailureReason
     */
    public final String renewalStatusReasonAsString() {
        return renewalStatusReason;
    }

    /**
     * <p>
     * The time at which the renewal summary was last updated.
     * </p>
     * 
     * @return The time at which the renewal summary was last updated.
     */
    public final Instant updatedAt() {
        return updatedAt;
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
        hashCode = 31 * hashCode + Objects.hashCode(renewalStatusAsString());
        hashCode = 31 * hashCode + Objects.hashCode(hasDomainValidationOptions() ? domainValidationOptions() : null);
        hashCode = 31 * hashCode + Objects.hashCode(renewalStatusReasonAsString());
        hashCode = 31 * hashCode + Objects.hashCode(updatedAt());
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
        if (!(obj instanceof RenewalSummary)) {
            return false;
        }
        RenewalSummary other = (RenewalSummary) obj;
        return Objects.equals(renewalStatusAsString(), other.renewalStatusAsString())
                && hasDomainValidationOptions() == other.hasDomainValidationOptions()
                && Objects.equals(domainValidationOptions(), other.domainValidationOptions())
                && Objects.equals(renewalStatusReasonAsString(), other.renewalStatusReasonAsString())
                && Objects.equals(updatedAt(), other.updatedAt());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public final String toString() {
        return ToString.builder("RenewalSummary").add("RenewalStatus", renewalStatusAsString())
                .add("DomainValidationOptions", hasDomainValidationOptions() ? domainValidationOptions() : null)
                .add("RenewalStatusReason", renewalStatusReasonAsString()).add("UpdatedAt", updatedAt()).build();
    }

    public final <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
        case "RenewalStatus":
            return Optional.ofNullable(clazz.cast(renewalStatusAsString()));
        case "DomainValidationOptions":
            return Optional.ofNullable(clazz.cast(domainValidationOptions()));
        case "RenewalStatusReason":
            return Optional.ofNullable(clazz.cast(renewalStatusReasonAsString()));
        case "UpdatedAt":
            return Optional.ofNullable(clazz.cast(updatedAt()));
        default:
            return Optional.empty();
        }
    }

    @Override
    public final List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    private static <T> Function<Object, T> getter(Function<RenewalSummary, T> g) {
        return obj -> g.apply((RenewalSummary) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    public interface Builder extends SdkPojo, CopyableBuilder<Builder, RenewalSummary> {
        /**
         * <p>
         * The status of ACM's <a href="https://docs.aws.amazon.com/acm/latest/userguide/acm-renewal.html">managed
         * renewal</a> of the certificate.
         * </p>
         * 
         * @param renewalStatus
         *        The status of ACM's <a
         *        href="https://docs.aws.amazon.com/acm/latest/userguide/acm-renewal.html">managed renewal</a> of the
         *        certificate.
         * @see RenewalStatus
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see RenewalStatus
         */
        Builder renewalStatus(String renewalStatus);

        /**
         * <p>
         * The status of ACM's <a href="https://docs.aws.amazon.com/acm/latest/userguide/acm-renewal.html">managed
         * renewal</a> of the certificate.
         * </p>
         * 
         * @param renewalStatus
         *        The status of ACM's <a
         *        href="https://docs.aws.amazon.com/acm/latest/userguide/acm-renewal.html">managed renewal</a> of the
         *        certificate.
         * @see RenewalStatus
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see RenewalStatus
         */
        Builder renewalStatus(RenewalStatus renewalStatus);

        /**
         * <p>
         * Contains information about the validation of each domain name in the certificate, as it pertains to ACM's <a
         * href="https://docs.aws.amazon.com/acm/latest/userguide/acm-renewal.html">managed renewal</a>. This is
         * different from the initial validation that occurs as a result of the <a>RequestCertificate</a> request. This
         * field exists only when the certificate type is <code>AMAZON_ISSUED</code>.
         * </p>
         * 
         * @param domainValidationOptions
         *        Contains information about the validation of each domain name in the certificate, as it pertains to
         *        ACM's <a href="https://docs.aws.amazon.com/acm/latest/userguide/acm-renewal.html">managed renewal</a>.
         *        This is different from the initial validation that occurs as a result of the <a>RequestCertificate</a>
         *        request. This field exists only when the certificate type is <code>AMAZON_ISSUED</code>.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder domainValidationOptions(Collection<DomainValidation> domainValidationOptions);

        /**
         * <p>
         * Contains information about the validation of each domain name in the certificate, as it pertains to ACM's <a
         * href="https://docs.aws.amazon.com/acm/latest/userguide/acm-renewal.html">managed renewal</a>. This is
         * different from the initial validation that occurs as a result of the <a>RequestCertificate</a> request. This
         * field exists only when the certificate type is <code>AMAZON_ISSUED</code>.
         * </p>
         * 
         * @param domainValidationOptions
         *        Contains information about the validation of each domain name in the certificate, as it pertains to
         *        ACM's <a href="https://docs.aws.amazon.com/acm/latest/userguide/acm-renewal.html">managed renewal</a>.
         *        This is different from the initial validation that occurs as a result of the <a>RequestCertificate</a>
         *        request. This field exists only when the certificate type is <code>AMAZON_ISSUED</code>.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder domainValidationOptions(DomainValidation... domainValidationOptions);

        /**
         * <p>
         * Contains information about the validation of each domain name in the certificate, as it pertains to ACM's <a
         * href="https://docs.aws.amazon.com/acm/latest/userguide/acm-renewal.html">managed renewal</a>. This is
         * different from the initial validation that occurs as a result of the <a>RequestCertificate</a> request. This
         * field exists only when the certificate type is <code>AMAZON_ISSUED</code>.
         * </p>
         * This is a convenience method that creates an instance of the
         * {@link software.amazon.awssdk.services.acm.model.DomainValidation.Builder} avoiding the need to create one
         * manually via {@link software.amazon.awssdk.services.acm.model.DomainValidation#builder()}.
         *
         * <p>
         * When the {@link Consumer} completes,
         * {@link software.amazon.awssdk.services.acm.model.DomainValidation.Builder#build()} is called immediately and
         * its result is passed to {@link #domainValidationOptions(List<DomainValidation>)}.
         * 
         * @param domainValidationOptions
         *        a consumer that will call methods on
         *        {@link software.amazon.awssdk.services.acm.model.DomainValidation.Builder}
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see #domainValidationOptions(java.util.Collection<DomainValidation>)
         */
        Builder domainValidationOptions(Consumer<DomainValidation.Builder>... domainValidationOptions);

        /**
         * <p>
         * The reason that a renewal request was unsuccessful.
         * </p>
         * 
         * @param renewalStatusReason
         *        The reason that a renewal request was unsuccessful.
         * @see FailureReason
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see FailureReason
         */
        Builder renewalStatusReason(String renewalStatusReason);

        /**
         * <p>
         * The reason that a renewal request was unsuccessful.
         * </p>
         * 
         * @param renewalStatusReason
         *        The reason that a renewal request was unsuccessful.
         * @see FailureReason
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see FailureReason
         */
        Builder renewalStatusReason(FailureReason renewalStatusReason);

        /**
         * <p>
         * The time at which the renewal summary was last updated.
         * </p>
         * 
         * @param updatedAt
         *        The time at which the renewal summary was last updated.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder updatedAt(Instant updatedAt);
    }

    static final class BuilderImpl implements Builder {
        private String renewalStatus;

        private List<DomainValidation> domainValidationOptions = DefaultSdkAutoConstructList.getInstance();

        private String renewalStatusReason;

        private Instant updatedAt;

        private BuilderImpl() {
        }

        private BuilderImpl(RenewalSummary model) {
            renewalStatus(model.renewalStatus);
            domainValidationOptions(model.domainValidationOptions);
            renewalStatusReason(model.renewalStatusReason);
            updatedAt(model.updatedAt);
        }

        public final String getRenewalStatus() {
            return renewalStatus;
        }

        public final void setRenewalStatus(String renewalStatus) {
            this.renewalStatus = renewalStatus;
        }

        @Override
        public final Builder renewalStatus(String renewalStatus) {
            this.renewalStatus = renewalStatus;
            return this;
        }

        @Override
        public final Builder renewalStatus(RenewalStatus renewalStatus) {
            this.renewalStatus(renewalStatus == null ? null : renewalStatus.toString());
            return this;
        }

        public final List<DomainValidation.Builder> getDomainValidationOptions() {
            List<DomainValidation.Builder> result = DomainValidationListCopier.copyToBuilder(this.domainValidationOptions);
            if (result instanceof SdkAutoConstructList) {
                return null;
            }
            return result;
        }

        public final void setDomainValidationOptions(Collection<DomainValidation.BuilderImpl> domainValidationOptions) {
            this.domainValidationOptions = DomainValidationListCopier.copyFromBuilder(domainValidationOptions);
        }

        @Override
        public final Builder domainValidationOptions(Collection<DomainValidation> domainValidationOptions) {
            this.domainValidationOptions = DomainValidationListCopier.copy(domainValidationOptions);
            return this;
        }

        @Override
        @SafeVarargs
        public final Builder domainValidationOptions(DomainValidation... domainValidationOptions) {
            domainValidationOptions(Arrays.asList(domainValidationOptions));
            return this;
        }

        @Override
        @SafeVarargs
        public final Builder domainValidationOptions(Consumer<DomainValidation.Builder>... domainValidationOptions) {
            domainValidationOptions(Stream.of(domainValidationOptions)
                    .map(c -> DomainValidation.builder().applyMutation(c).build()).collect(Collectors.toList()));
            return this;
        }

        public final String getRenewalStatusReason() {
            return renewalStatusReason;
        }

        public final void setRenewalStatusReason(String renewalStatusReason) {
            this.renewalStatusReason = renewalStatusReason;
        }

        @Override
        public final Builder renewalStatusReason(String renewalStatusReason) {
            this.renewalStatusReason = renewalStatusReason;
            return this;
        }

        @Override
        public final Builder renewalStatusReason(FailureReason renewalStatusReason) {
            this.renewalStatusReason(renewalStatusReason == null ? null : renewalStatusReason.toString());
            return this;
        }

        public final Instant getUpdatedAt() {
            return updatedAt;
        }

        public final void setUpdatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
        }

        @Override
        public final Builder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        @Override
        public RenewalSummary build() {
            return new RenewalSummary(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}
