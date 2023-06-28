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
public final class ResendValidationEmailRequest extends AcmRequest implements
        ToCopyableBuilder<ResendValidationEmailRequest.Builder, ResendValidationEmailRequest> {
    private static final SdkField<String> CERTIFICATE_ARN_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("CertificateArn").getter(getter(ResendValidationEmailRequest::certificateArn))
            .setter(setter(Builder::certificateArn))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("CertificateArn").build()).build();

    private static final SdkField<String> DOMAIN_FIELD = SdkField.<String> builder(MarshallingType.STRING).memberName("Domain")
            .getter(getter(ResendValidationEmailRequest::domain)).setter(setter(Builder::domain))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("Domain").build()).build();

    private static final SdkField<String> VALIDATION_DOMAIN_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("ValidationDomain").getter(getter(ResendValidationEmailRequest::validationDomain))
            .setter(setter(Builder::validationDomain))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("ValidationDomain").build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(CERTIFICATE_ARN_FIELD,
            DOMAIN_FIELD, VALIDATION_DOMAIN_FIELD));

    private final String certificateArn;

    private final String domain;

    private final String validationDomain;

    private ResendValidationEmailRequest(BuilderImpl builder) {
        super(builder);
        this.certificateArn = builder.certificateArn;
        this.domain = builder.domain;
        this.validationDomain = builder.validationDomain;
    }

    /**
     * <p>
     * String that contains the ARN of the requested certificate. The certificate ARN is generated and returned by the
     * <a>RequestCertificate</a> action as soon as the request is made. By default, using this parameter causes email to
     * be sent to all top-level domains you specified in the certificate request. The ARN must be of the form:
     * </p>
     * <p>
     * <code>arn:aws:acm:us-east-1:123456789012:certificate/12345678-1234-1234-1234-123456789012</code>
     * </p>
     * 
     * @return String that contains the ARN of the requested certificate. The certificate ARN is generated and returned
     *         by the <a>RequestCertificate</a> action as soon as the request is made. By default, using this parameter
     *         causes email to be sent to all top-level domains you specified in the certificate request. The ARN must
     *         be of the form: </p>
     *         <p>
     *         <code>arn:aws:acm:us-east-1:123456789012:certificate/12345678-1234-1234-1234-123456789012</code>
     */
    public final String certificateArn() {
        return certificateArn;
    }

    /**
     * <p>
     * The fully qualified domain name (FQDN) of the certificate that needs to be validated.
     * </p>
     * 
     * @return The fully qualified domain name (FQDN) of the certificate that needs to be validated.
     */
    public final String domain() {
        return domain;
    }

    /**
     * <p>
     * The base validation domain that will act as the suffix of the email addresses that are used to send the emails.
     * This must be the same as the <code>Domain</code> value or a superdomain of the <code>Domain</code> value. For
     * example, if you requested a certificate for <code>site.subdomain.example.com</code> and specify a
     * <b>ValidationDomain</b> of <code>subdomain.example.com</code>, ACM sends email to the domain registrant,
     * technical contact, and administrative contact in WHOIS and the following five addresses:
     * </p>
     * <ul>
     * <li>
     * <p>
     * admin@subdomain.example.com
     * </p>
     * </li>
     * <li>
     * <p>
     * administrator@subdomain.example.com
     * </p>
     * </li>
     * <li>
     * <p>
     * hostmaster@subdomain.example.com
     * </p>
     * </li>
     * <li>
     * <p>
     * postmaster@subdomain.example.com
     * </p>
     * </li>
     * <li>
     * <p>
     * webmaster@subdomain.example.com
     * </p>
     * </li>
     * </ul>
     * 
     * @return The base validation domain that will act as the suffix of the email addresses that are used to send the
     *         emails. This must be the same as the <code>Domain</code> value or a superdomain of the
     *         <code>Domain</code> value. For example, if you requested a certificate for
     *         <code>site.subdomain.example.com</code> and specify a <b>ValidationDomain</b> of
     *         <code>subdomain.example.com</code>, ACM sends email to the domain registrant, technical contact, and
     *         administrative contact in WHOIS and the following five addresses:</p>
     *         <ul>
     *         <li>
     *         <p>
     *         admin@subdomain.example.com
     *         </p>
     *         </li>
     *         <li>
     *         <p>
     *         administrator@subdomain.example.com
     *         </p>
     *         </li>
     *         <li>
     *         <p>
     *         hostmaster@subdomain.example.com
     *         </p>
     *         </li>
     *         <li>
     *         <p>
     *         postmaster@subdomain.example.com
     *         </p>
     *         </li>
     *         <li>
     *         <p>
     *         webmaster@subdomain.example.com
     *         </p>
     *         </li>
     */
    public final String validationDomain() {
        return validationDomain;
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
        hashCode = 31 * hashCode + Objects.hashCode(certificateArn());
        hashCode = 31 * hashCode + Objects.hashCode(domain());
        hashCode = 31 * hashCode + Objects.hashCode(validationDomain());
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
        if (!(obj instanceof ResendValidationEmailRequest)) {
            return false;
        }
        ResendValidationEmailRequest other = (ResendValidationEmailRequest) obj;
        return Objects.equals(certificateArn(), other.certificateArn()) && Objects.equals(domain(), other.domain())
                && Objects.equals(validationDomain(), other.validationDomain());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public final String toString() {
        return ToString.builder("ResendValidationEmailRequest").add("CertificateArn", certificateArn()).add("Domain", domain())
                .add("ValidationDomain", validationDomain()).build();
    }

    public final <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
        case "CertificateArn":
            return Optional.ofNullable(clazz.cast(certificateArn()));
        case "Domain":
            return Optional.ofNullable(clazz.cast(domain()));
        case "ValidationDomain":
            return Optional.ofNullable(clazz.cast(validationDomain()));
        default:
            return Optional.empty();
        }
    }

    @Override
    public final List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    private static <T> Function<Object, T> getter(Function<ResendValidationEmailRequest, T> g) {
        return obj -> g.apply((ResendValidationEmailRequest) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    public interface Builder extends AcmRequest.Builder, SdkPojo, CopyableBuilder<Builder, ResendValidationEmailRequest> {
        /**
         * <p>
         * String that contains the ARN of the requested certificate. The certificate ARN is generated and returned by
         * the <a>RequestCertificate</a> action as soon as the request is made. By default, using this parameter causes
         * email to be sent to all top-level domains you specified in the certificate request. The ARN must be of the
         * form:
         * </p>
         * <p>
         * <code>arn:aws:acm:us-east-1:123456789012:certificate/12345678-1234-1234-1234-123456789012</code>
         * </p>
         * 
         * @param certificateArn
         *        String that contains the ARN of the requested certificate. The certificate ARN is generated and
         *        returned by the <a>RequestCertificate</a> action as soon as the request is made. By default, using
         *        this parameter causes email to be sent to all top-level domains you specified in the certificate
         *        request. The ARN must be of the form: </p>
         *        <p>
         *        <code>arn:aws:acm:us-east-1:123456789012:certificate/12345678-1234-1234-1234-123456789012</code>
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder certificateArn(String certificateArn);

        /**
         * <p>
         * The fully qualified domain name (FQDN) of the certificate that needs to be validated.
         * </p>
         * 
         * @param domain
         *        The fully qualified domain name (FQDN) of the certificate that needs to be validated.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder domain(String domain);

        /**
         * <p>
         * The base validation domain that will act as the suffix of the email addresses that are used to send the
         * emails. This must be the same as the <code>Domain</code> value or a superdomain of the <code>Domain</code>
         * value. For example, if you requested a certificate for <code>site.subdomain.example.com</code> and specify a
         * <b>ValidationDomain</b> of <code>subdomain.example.com</code>, ACM sends email to the domain registrant,
         * technical contact, and administrative contact in WHOIS and the following five addresses:
         * </p>
         * <ul>
         * <li>
         * <p>
         * admin@subdomain.example.com
         * </p>
         * </li>
         * <li>
         * <p>
         * administrator@subdomain.example.com
         * </p>
         * </li>
         * <li>
         * <p>
         * hostmaster@subdomain.example.com
         * </p>
         * </li>
         * <li>
         * <p>
         * postmaster@subdomain.example.com
         * </p>
         * </li>
         * <li>
         * <p>
         * webmaster@subdomain.example.com
         * </p>
         * </li>
         * </ul>
         * 
         * @param validationDomain
         *        The base validation domain that will act as the suffix of the email addresses that are used to send
         *        the emails. This must be the same as the <code>Domain</code> value or a superdomain of the
         *        <code>Domain</code> value. For example, if you requested a certificate for
         *        <code>site.subdomain.example.com</code> and specify a <b>ValidationDomain</b> of
         *        <code>subdomain.example.com</code>, ACM sends email to the domain registrant, technical contact, and
         *        administrative contact in WHOIS and the following five addresses:</p>
         *        <ul>
         *        <li>
         *        <p>
         *        admin@subdomain.example.com
         *        </p>
         *        </li>
         *        <li>
         *        <p>
         *        administrator@subdomain.example.com
         *        </p>
         *        </li>
         *        <li>
         *        <p>
         *        hostmaster@subdomain.example.com
         *        </p>
         *        </li>
         *        <li>
         *        <p>
         *        postmaster@subdomain.example.com
         *        </p>
         *        </li>
         *        <li>
         *        <p>
         *        webmaster@subdomain.example.com
         *        </p>
         *        </li>
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder validationDomain(String validationDomain);

        @Override
        Builder overrideConfiguration(AwsRequestOverrideConfiguration overrideConfiguration);

        @Override
        Builder overrideConfiguration(Consumer<AwsRequestOverrideConfiguration.Builder> builderConsumer);
    }

    static final class BuilderImpl extends AcmRequest.BuilderImpl implements Builder {
        private String certificateArn;

        private String domain;

        private String validationDomain;

        private BuilderImpl() {
        }

        private BuilderImpl(ResendValidationEmailRequest model) {
            super(model);
            certificateArn(model.certificateArn);
            domain(model.domain);
            validationDomain(model.validationDomain);
        }

        public final String getCertificateArn() {
            return certificateArn;
        }

        public final void setCertificateArn(String certificateArn) {
            this.certificateArn = certificateArn;
        }

        @Override
        public final Builder certificateArn(String certificateArn) {
            this.certificateArn = certificateArn;
            return this;
        }

        public final String getDomain() {
            return domain;
        }

        public final void setDomain(String domain) {
            this.domain = domain;
        }

        @Override
        public final Builder domain(String domain) {
            this.domain = domain;
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
        public ResendValidationEmailRequest build() {
            return new ResendValidationEmailRequest(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}
