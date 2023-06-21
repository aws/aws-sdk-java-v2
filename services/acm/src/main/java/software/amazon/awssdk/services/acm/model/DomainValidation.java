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
 * Contains information about the validation of each domain name in the certificate.
 * </p>
 */
@Generated("software.amazon.awssdk:codegen")
public final class DomainValidation implements SdkPojo, Serializable,
        ToCopyableBuilder<DomainValidation.Builder, DomainValidation> {
    private static final SdkField<String> DOMAIN_NAME_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("DomainName").getter(getter(DomainValidation::domainName)).setter(setter(Builder::domainName))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("DomainName").build()).build();

    private static final SdkField<List<String>> VALIDATION_EMAILS_FIELD = SdkField
            .<List<String>> builder(MarshallingType.LIST)
            .memberName("ValidationEmails")
            .getter(getter(DomainValidation::validationEmails))
            .setter(setter(Builder::validationEmails))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("ValidationEmails").build(),
                    ListTrait
                            .builder()
                            .memberLocationName(null)
                            .memberFieldInfo(
                                    SdkField.<String> builder(MarshallingType.STRING)
                                            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD)
                                                    .locationName("member").build()).build()).build()).build();

    private static final SdkField<String> VALIDATION_DOMAIN_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("ValidationDomain").getter(getter(DomainValidation::validationDomain))
            .setter(setter(Builder::validationDomain))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("ValidationDomain").build()).build();

    private static final SdkField<String> VALIDATION_STATUS_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("ValidationStatus").getter(getter(DomainValidation::validationStatusAsString))
            .setter(setter(Builder::validationStatus))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("ValidationStatus").build()).build();

    private static final SdkField<ResourceRecord> RESOURCE_RECORD_FIELD = SdkField
            .<ResourceRecord> builder(MarshallingType.SDK_POJO).memberName("ResourceRecord")
            .getter(getter(DomainValidation::resourceRecord)).setter(setter(Builder::resourceRecord))
            .constructor(ResourceRecord::builder)
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("ResourceRecord").build()).build();

    private static final SdkField<String> VALIDATION_METHOD_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("ValidationMethod").getter(getter(DomainValidation::validationMethodAsString))
            .setter(setter(Builder::validationMethod))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("ValidationMethod").build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(DOMAIN_NAME_FIELD,
            VALIDATION_EMAILS_FIELD, VALIDATION_DOMAIN_FIELD, VALIDATION_STATUS_FIELD, RESOURCE_RECORD_FIELD,
            VALIDATION_METHOD_FIELD));

    private static final long serialVersionUID = 1L;

    private final String domainName;

    private final List<String> validationEmails;

    private final String validationDomain;

    private final String validationStatus;

    private final ResourceRecord resourceRecord;

    private final String validationMethod;

    private DomainValidation(BuilderImpl builder) {
        this.domainName = builder.domainName;
        this.validationEmails = builder.validationEmails;
        this.validationDomain = builder.validationDomain;
        this.validationStatus = builder.validationStatus;
        this.resourceRecord = builder.resourceRecord;
        this.validationMethod = builder.validationMethod;
    }

    /**
     * <p>
     * A fully qualified domain name (FQDN) in the certificate. For example, <code>www.example.com</code> or
     * <code>example.com</code>.
     * </p>
     * 
     * @return A fully qualified domain name (FQDN) in the certificate. For example, <code>www.example.com</code> or
     *         <code>example.com</code>.
     */
    public final String domainName() {
        return domainName;
    }

    /**
     * For responses, this returns true if the service returned a value for the ValidationEmails property. This DOES NOT
     * check that the value is non-empty (for which, you should check the {@code isEmpty()} method on the property).
     * This is useful because the SDK will never return a null collection or map, but you may need to differentiate
     * between the service returning nothing (or null) and the service returning an empty collection or map. For
     * requests, this returns true if a value for the property was specified in the request builder, and false if a
     * value was not specified.
     */
    public final boolean hasValidationEmails() {
        return validationEmails != null && !(validationEmails instanceof SdkAutoConstructList);
    }

    /**
     * <p>
     * A list of email addresses that ACM used to send domain validation emails.
     * </p>
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     * <p>
     * This method will never return null. If you would like to know whether the service returned this field (so that
     * you can differentiate between null and empty), you can use the {@link #hasValidationEmails} method.
     * </p>
     * 
     * @return A list of email addresses that ACM used to send domain validation emails.
     */
    public final List<String> validationEmails() {
        return validationEmails;
    }

    /**
     * <p>
     * The domain name that ACM used to send domain validation emails.
     * </p>
     * 
     * @return The domain name that ACM used to send domain validation emails.
     */
    public final String validationDomain() {
        return validationDomain;
    }

    /**
     * <p>
     * The validation status of the domain name. This can be one of the following values:
     * </p>
     * <ul>
     * <li>
     * <p>
     * <code>PENDING_VALIDATION</code>
     * </p>
     * </li>
     * <li>
     * <p>
     * <code/>SUCCESS
     * </p>
     * </li>
     * <li>
     * <p>
     * <code/>FAILED
     * </p>
     * </li>
     * </ul>
     * <p>
     * If the service returns an enum value that is not available in the current SDK version, {@link #validationStatus}
     * will return {@link DomainStatus#UNKNOWN_TO_SDK_VERSION}. The raw value returned by the service is available from
     * {@link #validationStatusAsString}.
     * </p>
     * 
     * @return The validation status of the domain name. This can be one of the following values:</p>
     *         <ul>
     *         <li>
     *         <p>
     *         <code>PENDING_VALIDATION</code>
     *         </p>
     *         </li>
     *         <li>
     *         <p>
     *         <code/>SUCCESS
     *         </p>
     *         </li>
     *         <li>
     *         <p>
     *         <code/>FAILED
     *         </p>
     *         </li>
     * @see DomainStatus
     */
    public final DomainStatus validationStatus() {
        return DomainStatus.fromValue(validationStatus);
    }

    /**
     * <p>
     * The validation status of the domain name. This can be one of the following values:
     * </p>
     * <ul>
     * <li>
     * <p>
     * <code>PENDING_VALIDATION</code>
     * </p>
     * </li>
     * <li>
     * <p>
     * <code/>SUCCESS
     * </p>
     * </li>
     * <li>
     * <p>
     * <code/>FAILED
     * </p>
     * </li>
     * </ul>
     * <p>
     * If the service returns an enum value that is not available in the current SDK version, {@link #validationStatus}
     * will return {@link DomainStatus#UNKNOWN_TO_SDK_VERSION}. The raw value returned by the service is available from
     * {@link #validationStatusAsString}.
     * </p>
     * 
     * @return The validation status of the domain name. This can be one of the following values:</p>
     *         <ul>
     *         <li>
     *         <p>
     *         <code>PENDING_VALIDATION</code>
     *         </p>
     *         </li>
     *         <li>
     *         <p>
     *         <code/>SUCCESS
     *         </p>
     *         </li>
     *         <li>
     *         <p>
     *         <code/>FAILED
     *         </p>
     *         </li>
     * @see DomainStatus
     */
    public final String validationStatusAsString() {
        return validationStatus;
    }

    /**
     * <p>
     * Contains the CNAME record that you add to your DNS database for domain validation. For more information, see <a
     * href="https://docs.aws.amazon.com/acm/latest/userguide/gs-acm-validate-dns.html">Use DNS to Validate Domain
     * Ownership</a>.
     * </p>
     * <p>
     * Note: The CNAME information that you need does not include the name of your domain. If you include&#x2028; your
     * domain name in the DNS database CNAME record, validation fails.&#x2028; For example, if the name is
     * "_a79865eb4cd1a6ab990a45779b4e0b96.yourdomain.com", only "_a79865eb4cd1a6ab990a45779b4e0b96" must be used.
     * </p>
     * 
     * @return Contains the CNAME record that you add to your DNS database for domain validation. For more information,
     *         see <a href="https://docs.aws.amazon.com/acm/latest/userguide/gs-acm-validate-dns.html">Use DNS to
     *         Validate Domain Ownership</a>.</p>
     *         <p>
     *         Note: The CNAME information that you need does not include the name of your domain. If you
     *         include&#x2028; your domain name in the DNS database CNAME record, validation fails.&#x2028; For example,
     *         if the name is "_a79865eb4cd1a6ab990a45779b4e0b96.yourdomain.com", only
     *         "_a79865eb4cd1a6ab990a45779b4e0b96" must be used.
     */
    public final ResourceRecord resourceRecord() {
        return resourceRecord;
    }

    /**
     * <p>
     * Specifies the domain validation method.
     * </p>
     * <p>
     * If the service returns an enum value that is not available in the current SDK version, {@link #validationMethod}
     * will return {@link ValidationMethod#UNKNOWN_TO_SDK_VERSION}. The raw value returned by the service is available
     * from {@link #validationMethodAsString}.
     * </p>
     * 
     * @return Specifies the domain validation method.
     * @see ValidationMethod
     */
    public final ValidationMethod validationMethod() {
        return ValidationMethod.fromValue(validationMethod);
    }

    /**
     * <p>
     * Specifies the domain validation method.
     * </p>
     * <p>
     * If the service returns an enum value that is not available in the current SDK version, {@link #validationMethod}
     * will return {@link ValidationMethod#UNKNOWN_TO_SDK_VERSION}. The raw value returned by the service is available
     * from {@link #validationMethodAsString}.
     * </p>
     * 
     * @return Specifies the domain validation method.
     * @see ValidationMethod
     */
    public final String validationMethodAsString() {
        return validationMethod;
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
        hashCode = 31 * hashCode + Objects.hashCode(domainName());
        hashCode = 31 * hashCode + Objects.hashCode(hasValidationEmails() ? validationEmails() : null);
        hashCode = 31 * hashCode + Objects.hashCode(validationDomain());
        hashCode = 31 * hashCode + Objects.hashCode(validationStatusAsString());
        hashCode = 31 * hashCode + Objects.hashCode(resourceRecord());
        hashCode = 31 * hashCode + Objects.hashCode(validationMethodAsString());
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
        if (!(obj instanceof DomainValidation)) {
            return false;
        }
        DomainValidation other = (DomainValidation) obj;
        return Objects.equals(domainName(), other.domainName()) && hasValidationEmails() == other.hasValidationEmails()
                && Objects.equals(validationEmails(), other.validationEmails())
                && Objects.equals(validationDomain(), other.validationDomain())
                && Objects.equals(validationStatusAsString(), other.validationStatusAsString())
                && Objects.equals(resourceRecord(), other.resourceRecord())
                && Objects.equals(validationMethodAsString(), other.validationMethodAsString());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public final String toString() {
        return ToString.builder("DomainValidation").add("DomainName", domainName())
                .add("ValidationEmails", hasValidationEmails() ? validationEmails() : null)
                .add("ValidationDomain", validationDomain()).add("ValidationStatus", validationStatusAsString())
                .add("ResourceRecord", resourceRecord()).add("ValidationMethod", validationMethodAsString()).build();
    }

    public final <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
        case "DomainName":
            return Optional.ofNullable(clazz.cast(domainName()));
        case "ValidationEmails":
            return Optional.ofNullable(clazz.cast(validationEmails()));
        case "ValidationDomain":
            return Optional.ofNullable(clazz.cast(validationDomain()));
        case "ValidationStatus":
            return Optional.ofNullable(clazz.cast(validationStatusAsString()));
        case "ResourceRecord":
            return Optional.ofNullable(clazz.cast(resourceRecord()));
        case "ValidationMethod":
            return Optional.ofNullable(clazz.cast(validationMethodAsString()));
        default:
            return Optional.empty();
        }
    }

    @Override
    public final List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    private static <T> Function<Object, T> getter(Function<DomainValidation, T> g) {
        return obj -> g.apply((DomainValidation) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    public interface Builder extends SdkPojo, CopyableBuilder<Builder, DomainValidation> {
        /**
         * <p>
         * A fully qualified domain name (FQDN) in the certificate. For example, <code>www.example.com</code> or
         * <code>example.com</code>.
         * </p>
         * 
         * @param domainName
         *        A fully qualified domain name (FQDN) in the certificate. For example, <code>www.example.com</code> or
         *        <code>example.com</code>.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder domainName(String domainName);

        /**
         * <p>
         * A list of email addresses that ACM used to send domain validation emails.
         * </p>
         * 
         * @param validationEmails
         *        A list of email addresses that ACM used to send domain validation emails.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder validationEmails(Collection<String> validationEmails);

        /**
         * <p>
         * A list of email addresses that ACM used to send domain validation emails.
         * </p>
         * 
         * @param validationEmails
         *        A list of email addresses that ACM used to send domain validation emails.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder validationEmails(String... validationEmails);

        /**
         * <p>
         * The domain name that ACM used to send domain validation emails.
         * </p>
         * 
         * @param validationDomain
         *        The domain name that ACM used to send domain validation emails.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder validationDomain(String validationDomain);

        /**
         * <p>
         * The validation status of the domain name. This can be one of the following values:
         * </p>
         * <ul>
         * <li>
         * <p>
         * <code>PENDING_VALIDATION</code>
         * </p>
         * </li>
         * <li>
         * <p>
         * <code/>SUCCESS
         * </p>
         * </li>
         * <li>
         * <p>
         * <code/>FAILED
         * </p>
         * </li>
         * </ul>
         * 
         * @param validationStatus
         *        The validation status of the domain name. This can be one of the following values:</p>
         *        <ul>
         *        <li>
         *        <p>
         *        <code>PENDING_VALIDATION</code>
         *        </p>
         *        </li>
         *        <li>
         *        <p>
         *        <code/>SUCCESS
         *        </p>
         *        </li>
         *        <li>
         *        <p>
         *        <code/>FAILED
         *        </p>
         *        </li>
         * @see DomainStatus
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see DomainStatus
         */
        Builder validationStatus(String validationStatus);

        /**
         * <p>
         * The validation status of the domain name. This can be one of the following values:
         * </p>
         * <ul>
         * <li>
         * <p>
         * <code>PENDING_VALIDATION</code>
         * </p>
         * </li>
         * <li>
         * <p>
         * <code/>SUCCESS
         * </p>
         * </li>
         * <li>
         * <p>
         * <code/>FAILED
         * </p>
         * </li>
         * </ul>
         * 
         * @param validationStatus
         *        The validation status of the domain name. This can be one of the following values:</p>
         *        <ul>
         *        <li>
         *        <p>
         *        <code>PENDING_VALIDATION</code>
         *        </p>
         *        </li>
         *        <li>
         *        <p>
         *        <code/>SUCCESS
         *        </p>
         *        </li>
         *        <li>
         *        <p>
         *        <code/>FAILED
         *        </p>
         *        </li>
         * @see DomainStatus
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see DomainStatus
         */
        Builder validationStatus(DomainStatus validationStatus);

        /**
         * <p>
         * Contains the CNAME record that you add to your DNS database for domain validation. For more information, see
         * <a href="https://docs.aws.amazon.com/acm/latest/userguide/gs-acm-validate-dns.html">Use DNS to Validate
         * Domain Ownership</a>.
         * </p>
         * <p>
         * Note: The CNAME information that you need does not include the name of your domain. If you include&#x2028;
         * your domain name in the DNS database CNAME record, validation fails.&#x2028; For example, if the name is
         * "_a79865eb4cd1a6ab990a45779b4e0b96.yourdomain.com", only "_a79865eb4cd1a6ab990a45779b4e0b96" must be used.
         * </p>
         * 
         * @param resourceRecord
         *        Contains the CNAME record that you add to your DNS database for domain validation. For more
         *        information, see <a
         *        href="https://docs.aws.amazon.com/acm/latest/userguide/gs-acm-validate-dns.html">Use DNS to Validate
         *        Domain Ownership</a>.</p>
         *        <p>
         *        Note: The CNAME information that you need does not include the name of your domain. If you
         *        include&#x2028; your domain name in the DNS database CNAME record, validation fails.&#x2028; For
         *        example, if the name is "_a79865eb4cd1a6ab990a45779b4e0b96.yourdomain.com", only
         *        "_a79865eb4cd1a6ab990a45779b4e0b96" must be used.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder resourceRecord(ResourceRecord resourceRecord);

        /**
         * <p>
         * Contains the CNAME record that you add to your DNS database for domain validation. For more information, see
         * <a href="https://docs.aws.amazon.com/acm/latest/userguide/gs-acm-validate-dns.html">Use DNS to Validate
         * Domain Ownership</a>.
         * </p>
         * <p>
         * Note: The CNAME information that you need does not include the name of your domain. If you include&#x2028;
         * your domain name in the DNS database CNAME record, validation fails.&#x2028; For example, if the name is
         * "_a79865eb4cd1a6ab990a45779b4e0b96.yourdomain.com", only "_a79865eb4cd1a6ab990a45779b4e0b96" must be used.
         * </p>
         * This is a convenience method that creates an instance of the {@link ResourceRecord.Builder} avoiding the need
         * to create one manually via {@link ResourceRecord#builder()}.
         *
         * <p>
         * When the {@link Consumer} completes, {@link ResourceRecord.Builder#build()} is called immediately and its
         * result is passed to {@link #resourceRecord(ResourceRecord)}.
         * 
         * @param resourceRecord
         *        a consumer that will call methods on {@link ResourceRecord.Builder}
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see #resourceRecord(ResourceRecord)
         */
        default Builder resourceRecord(Consumer<ResourceRecord.Builder> resourceRecord) {
            return resourceRecord(ResourceRecord.builder().applyMutation(resourceRecord).build());
        }

        /**
         * <p>
         * Specifies the domain validation method.
         * </p>
         * 
         * @param validationMethod
         *        Specifies the domain validation method.
         * @see ValidationMethod
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see ValidationMethod
         */
        Builder validationMethod(String validationMethod);

        /**
         * <p>
         * Specifies the domain validation method.
         * </p>
         * 
         * @param validationMethod
         *        Specifies the domain validation method.
         * @see ValidationMethod
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see ValidationMethod
         */
        Builder validationMethod(ValidationMethod validationMethod);
    }

    static final class BuilderImpl implements Builder {
        private String domainName;

        private List<String> validationEmails = DefaultSdkAutoConstructList.getInstance();

        private String validationDomain;

        private String validationStatus;

        private ResourceRecord resourceRecord;

        private String validationMethod;

        private BuilderImpl() {
        }

        private BuilderImpl(DomainValidation model) {
            domainName(model.domainName);
            validationEmails(model.validationEmails);
            validationDomain(model.validationDomain);
            validationStatus(model.validationStatus);
            resourceRecord(model.resourceRecord);
            validationMethod(model.validationMethod);
        }

        public final String getDomainName() {
            return domainName;
        }

        public final void setDomainName(String domainName) {
            this.domainName = domainName;
        }

        @Override
        public final Builder domainName(String domainName) {
            this.domainName = domainName;
            return this;
        }

        public final Collection<String> getValidationEmails() {
            if (validationEmails instanceof SdkAutoConstructList) {
                return null;
            }
            return validationEmails;
        }

        public final void setValidationEmails(Collection<String> validationEmails) {
            this.validationEmails = ValidationEmailListCopier.copy(validationEmails);
        }

        @Override
        public final Builder validationEmails(Collection<String> validationEmails) {
            this.validationEmails = ValidationEmailListCopier.copy(validationEmails);
            return this;
        }

        @Override
        @SafeVarargs
        public final Builder validationEmails(String... validationEmails) {
            validationEmails(Arrays.asList(validationEmails));
            return this;
        }

        public final String getValidationDomain() {
            return validationDomain;
        }

        public final void setValidationDomain(String validationDomain) {
            this.validationDomain = validationDomain;
        }

        @Override
        public final Builder validationDomain(String validationDomain) {
            this.validationDomain = validationDomain;
            return this;
        }

        public final String getValidationStatus() {
            return validationStatus;
        }

        public final void setValidationStatus(String validationStatus) {
            this.validationStatus = validationStatus;
        }

        @Override
        public final Builder validationStatus(String validationStatus) {
            this.validationStatus = validationStatus;
            return this;
        }

        @Override
        public final Builder validationStatus(DomainStatus validationStatus) {
            this.validationStatus(validationStatus == null ? null : validationStatus.toString());
            return this;
        }

        public final ResourceRecord.Builder getResourceRecord() {
            return resourceRecord != null ? resourceRecord.toBuilder() : null;
        }

        public final void setResourceRecord(ResourceRecord.BuilderImpl resourceRecord) {
            this.resourceRecord = resourceRecord != null ? resourceRecord.build() : null;
        }

        @Override
        public final Builder resourceRecord(ResourceRecord resourceRecord) {
            this.resourceRecord = resourceRecord;
            return this;
        }

        public final String getValidationMethod() {
            return validationMethod;
        }

        public final void setValidationMethod(String validationMethod) {
            this.validationMethod = validationMethod;
        }

        @Override
        public final Builder validationMethod(String validationMethod) {
            this.validationMethod = validationMethod;
            return this;
        }

        @Override
        public final Builder validationMethod(ValidationMethod validationMethod) {
            this.validationMethod(validationMethod == null ? null : validationMethod.toString());
            return this;
        }

        @Override
        public DomainValidation build() {
            return new DomainValidation(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}
