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
 * Contains metadata about an ACM certificate. This structure is returned in the response to a
 * <a>DescribeCertificate</a> request.
 * </p>
 */
@Generated("software.amazon.awssdk:codegen")
public final class CertificateDetail implements SdkPojo, Serializable,
        ToCopyableBuilder<CertificateDetail.Builder, CertificateDetail> {
    private static final SdkField<String> CERTIFICATE_ARN_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("CertificateArn").getter(getter(CertificateDetail::certificateArn))
            .setter(setter(Builder::certificateArn))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("CertificateArn").build()).build();

    private static final SdkField<String> DOMAIN_NAME_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("DomainName").getter(getter(CertificateDetail::domainName)).setter(setter(Builder::domainName))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("DomainName").build()).build();

    private static final SdkField<List<String>> SUBJECT_ALTERNATIVE_NAMES_FIELD = SdkField
            .<List<String>> builder(MarshallingType.LIST)
            .memberName("SubjectAlternativeNames")
            .getter(getter(CertificateDetail::subjectAlternativeNames))
            .setter(setter(Builder::subjectAlternativeNames))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("SubjectAlternativeNames").build(),
                    ListTrait
                            .builder()
                            .memberLocationName(null)
                            .memberFieldInfo(
                                    SdkField.<String> builder(MarshallingType.STRING)
                                            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD)
                                                    .locationName("member").build()).build()).build()).build();

    private static final SdkField<List<DomainValidation>> DOMAIN_VALIDATION_OPTIONS_FIELD = SdkField
            .<List<DomainValidation>> builder(MarshallingType.LIST)
            .memberName("DomainValidationOptions")
            .getter(getter(CertificateDetail::domainValidationOptions))
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

    private static final SdkField<String> SERIAL_FIELD = SdkField.<String> builder(MarshallingType.STRING).memberName("Serial")
            .getter(getter(CertificateDetail::serial)).setter(setter(Builder::serial))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("Serial").build()).build();

    private static final SdkField<String> SUBJECT_FIELD = SdkField.<String> builder(MarshallingType.STRING).memberName("Subject")
            .getter(getter(CertificateDetail::subject)).setter(setter(Builder::subject))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("Subject").build()).build();

    private static final SdkField<String> ISSUER_FIELD = SdkField.<String> builder(MarshallingType.STRING).memberName("Issuer")
            .getter(getter(CertificateDetail::issuer)).setter(setter(Builder::issuer))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("Issuer").build()).build();

    private static final SdkField<Instant> CREATED_AT_FIELD = SdkField.<Instant> builder(MarshallingType.INSTANT)
            .memberName("CreatedAt").getter(getter(CertificateDetail::createdAt)).setter(setter(Builder::createdAt))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("CreatedAt").build()).build();

    private static final SdkField<Instant> ISSUED_AT_FIELD = SdkField.<Instant> builder(MarshallingType.INSTANT)
            .memberName("IssuedAt").getter(getter(CertificateDetail::issuedAt)).setter(setter(Builder::issuedAt))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("IssuedAt").build()).build();

    private static final SdkField<Instant> IMPORTED_AT_FIELD = SdkField.<Instant> builder(MarshallingType.INSTANT)
            .memberName("ImportedAt").getter(getter(CertificateDetail::importedAt)).setter(setter(Builder::importedAt))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("ImportedAt").build()).build();

    private static final SdkField<String> STATUS_FIELD = SdkField.<String> builder(MarshallingType.STRING).memberName("Status")
            .getter(getter(CertificateDetail::statusAsString)).setter(setter(Builder::status))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("Status").build()).build();

    private static final SdkField<Instant> REVOKED_AT_FIELD = SdkField.<Instant> builder(MarshallingType.INSTANT)
            .memberName("RevokedAt").getter(getter(CertificateDetail::revokedAt)).setter(setter(Builder::revokedAt))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("RevokedAt").build()).build();

    private static final SdkField<String> REVOCATION_REASON_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("RevocationReason").getter(getter(CertificateDetail::revocationReasonAsString))
            .setter(setter(Builder::revocationReason))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("RevocationReason").build()).build();

    private static final SdkField<Instant> NOT_BEFORE_FIELD = SdkField.<Instant> builder(MarshallingType.INSTANT)
            .memberName("NotBefore").getter(getter(CertificateDetail::notBefore)).setter(setter(Builder::notBefore))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("NotBefore").build()).build();

    private static final SdkField<Instant> NOT_AFTER_FIELD = SdkField.<Instant> builder(MarshallingType.INSTANT)
            .memberName("NotAfter").getter(getter(CertificateDetail::notAfter)).setter(setter(Builder::notAfter))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("NotAfter").build()).build();

    private static final SdkField<String> KEY_ALGORITHM_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("KeyAlgorithm").getter(getter(CertificateDetail::keyAlgorithmAsString))
            .setter(setter(Builder::keyAlgorithm))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("KeyAlgorithm").build()).build();

    private static final SdkField<String> SIGNATURE_ALGORITHM_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("SignatureAlgorithm").getter(getter(CertificateDetail::signatureAlgorithm))
            .setter(setter(Builder::signatureAlgorithm))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("SignatureAlgorithm").build())
            .build();

    private static final SdkField<List<String>> IN_USE_BY_FIELD = SdkField
            .<List<String>> builder(MarshallingType.LIST)
            .memberName("InUseBy")
            .getter(getter(CertificateDetail::inUseBy))
            .setter(setter(Builder::inUseBy))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("InUseBy").build(),
                    ListTrait
                            .builder()
                            .memberLocationName(null)
                            .memberFieldInfo(
                                    SdkField.<String> builder(MarshallingType.STRING)
                                            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD)
                                                    .locationName("member").build()).build()).build()).build();

    private static final SdkField<String> FAILURE_REASON_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("FailureReason").getter(getter(CertificateDetail::failureReasonAsString))
            .setter(setter(Builder::failureReason))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("FailureReason").build()).build();

    private static final SdkField<String> TYPE_FIELD = SdkField.<String> builder(MarshallingType.STRING).memberName("Type")
            .getter(getter(CertificateDetail::typeAsString)).setter(setter(Builder::type))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("Type").build()).build();

    private static final SdkField<RenewalSummary> RENEWAL_SUMMARY_FIELD = SdkField
            .<RenewalSummary> builder(MarshallingType.SDK_POJO).memberName("RenewalSummary")
            .getter(getter(CertificateDetail::renewalSummary)).setter(setter(Builder::renewalSummary))
            .constructor(RenewalSummary::builder)
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("RenewalSummary").build()).build();

    private static final SdkField<List<KeyUsage>> KEY_USAGES_FIELD = SdkField
            .<List<KeyUsage>> builder(MarshallingType.LIST)
            .memberName("KeyUsages")
            .getter(getter(CertificateDetail::keyUsages))
            .setter(setter(Builder::keyUsages))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("KeyUsages").build(),
                    ListTrait
                            .builder()
                            .memberLocationName(null)
                            .memberFieldInfo(
                                    SdkField.<KeyUsage> builder(MarshallingType.SDK_POJO)
                                            .constructor(KeyUsage::builder)
                                            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD)
                                                    .locationName("member").build()).build()).build()).build();

    private static final SdkField<List<ExtendedKeyUsage>> EXTENDED_KEY_USAGES_FIELD = SdkField
            .<List<ExtendedKeyUsage>> builder(MarshallingType.LIST)
            .memberName("ExtendedKeyUsages")
            .getter(getter(CertificateDetail::extendedKeyUsages))
            .setter(setter(Builder::extendedKeyUsages))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("ExtendedKeyUsages").build(),
                    ListTrait
                            .builder()
                            .memberLocationName(null)
                            .memberFieldInfo(
                                    SdkField.<ExtendedKeyUsage> builder(MarshallingType.SDK_POJO)
                                            .constructor(ExtendedKeyUsage::builder)
                                            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD)
                                                    .locationName("member").build()).build()).build()).build();

    private static final SdkField<String> CERTIFICATE_AUTHORITY_ARN_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("CertificateAuthorityArn").getter(getter(CertificateDetail::certificateAuthorityArn))
            .setter(setter(Builder::certificateAuthorityArn))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("CertificateAuthorityArn").build())
            .build();

    private static final SdkField<String> RENEWAL_ELIGIBILITY_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("RenewalEligibility").getter(getter(CertificateDetail::renewalEligibilityAsString))
            .setter(setter(Builder::renewalEligibility))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("RenewalEligibility").build())
            .build();

    private static final SdkField<CertificateOptions> OPTIONS_FIELD = SdkField
            .<CertificateOptions> builder(MarshallingType.SDK_POJO).memberName("Options")
            .getter(getter(CertificateDetail::options)).setter(setter(Builder::options)).constructor(CertificateOptions::builder)
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("Options").build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(CERTIFICATE_ARN_FIELD,
            DOMAIN_NAME_FIELD, SUBJECT_ALTERNATIVE_NAMES_FIELD, DOMAIN_VALIDATION_OPTIONS_FIELD, SERIAL_FIELD, SUBJECT_FIELD,
            ISSUER_FIELD, CREATED_AT_FIELD, ISSUED_AT_FIELD, IMPORTED_AT_FIELD, STATUS_FIELD, REVOKED_AT_FIELD,
            REVOCATION_REASON_FIELD, NOT_BEFORE_FIELD, NOT_AFTER_FIELD, KEY_ALGORITHM_FIELD, SIGNATURE_ALGORITHM_FIELD,
            IN_USE_BY_FIELD, FAILURE_REASON_FIELD, TYPE_FIELD, RENEWAL_SUMMARY_FIELD, KEY_USAGES_FIELD,
            EXTENDED_KEY_USAGES_FIELD, CERTIFICATE_AUTHORITY_ARN_FIELD, RENEWAL_ELIGIBILITY_FIELD, OPTIONS_FIELD));

    private static final long serialVersionUID = 1L;

    private final String certificateArn;

    private final String domainName;

    private final List<String> subjectAlternativeNames;

    private final List<DomainValidation> domainValidationOptions;

    private final String serial;

    private final String subject;

    private final String issuer;

    private final Instant createdAt;

    private final Instant issuedAt;

    private final Instant importedAt;

    private final String status;

    private final Instant revokedAt;

    private final String revocationReason;

    private final Instant notBefore;

    private final Instant notAfter;

    private final String keyAlgorithm;

    private final String signatureAlgorithm;

    private final List<String> inUseBy;

    private final String failureReason;

    private final String type;

    private final RenewalSummary renewalSummary;

    private final List<KeyUsage> keyUsages;

    private final List<ExtendedKeyUsage> extendedKeyUsages;

    private final String certificateAuthorityArn;

    private final String renewalEligibility;

    private final CertificateOptions options;

    private CertificateDetail(BuilderImpl builder) {
        this.certificateArn = builder.certificateArn;
        this.domainName = builder.domainName;
        this.subjectAlternativeNames = builder.subjectAlternativeNames;
        this.domainValidationOptions = builder.domainValidationOptions;
        this.serial = builder.serial;
        this.subject = builder.subject;
        this.issuer = builder.issuer;
        this.createdAt = builder.createdAt;
        this.issuedAt = builder.issuedAt;
        this.importedAt = builder.importedAt;
        this.status = builder.status;
        this.revokedAt = builder.revokedAt;
        this.revocationReason = builder.revocationReason;
        this.notBefore = builder.notBefore;
        this.notAfter = builder.notAfter;
        this.keyAlgorithm = builder.keyAlgorithm;
        this.signatureAlgorithm = builder.signatureAlgorithm;
        this.inUseBy = builder.inUseBy;
        this.failureReason = builder.failureReason;
        this.type = builder.type;
        this.renewalSummary = builder.renewalSummary;
        this.keyUsages = builder.keyUsages;
        this.extendedKeyUsages = builder.extendedKeyUsages;
        this.certificateAuthorityArn = builder.certificateAuthorityArn;
        this.renewalEligibility = builder.renewalEligibility;
        this.options = builder.options;
    }

    /**
     * <p>
     * The Amazon Resource Name (ARN) of the certificate. For more information about ARNs, see <a
     * href="https://docs.aws.amazon.com/general/latest/gr/aws-arns-and-namespaces.html">Amazon Resource Names
     * (ARNs)</a> in the <i>Amazon Web Services General Reference</i>.
     * </p>
     * 
     * @return The Amazon Resource Name (ARN) of the certificate. For more information about ARNs, see <a
     *         href="https://docs.aws.amazon.com/general/latest/gr/aws-arns-and-namespaces.html">Amazon Resource Names
     *         (ARNs)</a> in the <i>Amazon Web Services General Reference</i>.
     */
    public final String certificateArn() {
        return certificateArn;
    }

    /**
     * <p>
     * The fully qualified domain name for the certificate, such as www.example.com or example.com.
     * </p>
     * 
     * @return The fully qualified domain name for the certificate, such as www.example.com or example.com.
     */
    public final String domainName() {
        return domainName;
    }

    /**
     * For responses, this returns true if the service returned a value for the SubjectAlternativeNames property. This
     * DOES NOT check that the value is non-empty (for which, you should check the {@code isEmpty()} method on the
     * property). This is useful because the SDK will never return a null collection or map, but you may need to
     * differentiate between the service returning nothing (or null) and the service returning an empty collection or
     * map. For requests, this returns true if a value for the property was specified in the request builder, and false
     * if a value was not specified.
     */
    public final boolean hasSubjectAlternativeNames() {
        return subjectAlternativeNames != null && !(subjectAlternativeNames instanceof SdkAutoConstructList);
    }

    /**
     * <p>
     * One or more domain names (subject alternative names) included in the certificate. This list contains the domain
     * names that are bound to the public key that is contained in the certificate. The subject alternative names
     * include the canonical domain name (CN) of the certificate and additional domain names that can be used to connect
     * to the website.
     * </p>
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     * <p>
     * This method will never return null. If you would like to know whether the service returned this field (so that
     * you can differentiate between null and empty), you can use the {@link #hasSubjectAlternativeNames} method.
     * </p>
     * 
     * @return One or more domain names (subject alternative names) included in the certificate. This list contains the
     *         domain names that are bound to the public key that is contained in the certificate. The subject
     *         alternative names include the canonical domain name (CN) of the certificate and additional domain names
     *         that can be used to connect to the website.
     */
    public final List<String> subjectAlternativeNames() {
        return subjectAlternativeNames;
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
     * Contains information about the initial validation of each domain name that occurs as a result of the
     * <a>RequestCertificate</a> request. This field exists only when the certificate type is <code>AMAZON_ISSUED</code>
     * .
     * </p>
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     * <p>
     * This method will never return null. If you would like to know whether the service returned this field (so that
     * you can differentiate between null and empty), you can use the {@link #hasDomainValidationOptions} method.
     * </p>
     * 
     * @return Contains information about the initial validation of each domain name that occurs as a result of the
     *         <a>RequestCertificate</a> request. This field exists only when the certificate type is
     *         <code>AMAZON_ISSUED</code>.
     */
    public final List<DomainValidation> domainValidationOptions() {
        return domainValidationOptions;
    }

    /**
     * <p>
     * The serial number of the certificate.
     * </p>
     * 
     * @return The serial number of the certificate.
     */
    public final String serial() {
        return serial;
    }

    /**
     * <p>
     * The name of the entity that is associated with the public key contained in the certificate.
     * </p>
     * 
     * @return The name of the entity that is associated with the public key contained in the certificate.
     */
    public final String subject() {
        return subject;
    }

    /**
     * <p>
     * The name of the certificate authority that issued and signed the certificate.
     * </p>
     * 
     * @return The name of the certificate authority that issued and signed the certificate.
     */
    public final String issuer() {
        return issuer;
    }

    /**
     * <p>
     * The time at which the certificate was requested.
     * </p>
     * 
     * @return The time at which the certificate was requested.
     */
    public final Instant createdAt() {
        return createdAt;
    }

    /**
     * <p>
     * The time at which the certificate was issued. This value exists only when the certificate type is
     * <code>AMAZON_ISSUED</code>.
     * </p>
     * 
     * @return The time at which the certificate was issued. This value exists only when the certificate type is
     *         <code>AMAZON_ISSUED</code>.
     */
    public final Instant issuedAt() {
        return issuedAt;
    }

    /**
     * <p>
     * The date and time when the certificate was imported. This value exists only when the certificate type is
     * <code>IMPORTED</code>.
     * </p>
     * 
     * @return The date and time when the certificate was imported. This value exists only when the certificate type is
     *         <code>IMPORTED</code>.
     */
    public final Instant importedAt() {
        return importedAt;
    }

    /**
     * <p>
     * The status of the certificate.
     * </p>
     * <p>
     * A certificate enters status PENDING_VALIDATION upon being requested, unless it fails for any of the reasons given
     * in the troubleshooting topic <a
     * href="https://docs.aws.amazon.com/acm/latest/userguide/troubleshooting-failed.html">Certificate request
     * fails</a>. ACM makes repeated attempts to validate a certificate for 72 hours and then times out. If a
     * certificate shows status FAILED or VALIDATION_TIMED_OUT, delete the request, correct the issue with <a
     * href="https://docs.aws.amazon.com/acm/latest/userguide/dns-validation.html">DNS validation</a> or <a
     * href="https://docs.aws.amazon.com/acm/latest/userguide/email-validation.html">Email validation</a>, and try
     * again. If validation succeeds, the certificate enters status ISSUED.
     * </p>
     * <p>
     * If the service returns an enum value that is not available in the current SDK version, {@link #status} will
     * return {@link CertificateStatus#UNKNOWN_TO_SDK_VERSION}. The raw value returned by the service is available from
     * {@link #statusAsString}.
     * </p>
     * 
     * @return The status of the certificate.</p>
     *         <p>
     *         A certificate enters status PENDING_VALIDATION upon being requested, unless it fails for any of the
     *         reasons given in the troubleshooting topic <a
     *         href="https://docs.aws.amazon.com/acm/latest/userguide/troubleshooting-failed.html">Certificate request
     *         fails</a>. ACM makes repeated attempts to validate a certificate for 72 hours and then times out. If a
     *         certificate shows status FAILED or VALIDATION_TIMED_OUT, delete the request, correct the issue with <a
     *         href="https://docs.aws.amazon.com/acm/latest/userguide/dns-validation.html">DNS validation</a> or <a
     *         href="https://docs.aws.amazon.com/acm/latest/userguide/email-validation.html">Email validation</a>, and
     *         try again. If validation succeeds, the certificate enters status ISSUED.
     * @see CertificateStatus
     */
    public final CertificateStatus status() {
        return CertificateStatus.fromValue(status);
    }

    /**
     * <p>
     * The status of the certificate.
     * </p>
     * <p>
     * A certificate enters status PENDING_VALIDATION upon being requested, unless it fails for any of the reasons given
     * in the troubleshooting topic <a
     * href="https://docs.aws.amazon.com/acm/latest/userguide/troubleshooting-failed.html">Certificate request
     * fails</a>. ACM makes repeated attempts to validate a certificate for 72 hours and then times out. If a
     * certificate shows status FAILED or VALIDATION_TIMED_OUT, delete the request, correct the issue with <a
     * href="https://docs.aws.amazon.com/acm/latest/userguide/dns-validation.html">DNS validation</a> or <a
     * href="https://docs.aws.amazon.com/acm/latest/userguide/email-validation.html">Email validation</a>, and try
     * again. If validation succeeds, the certificate enters status ISSUED.
     * </p>
     * <p>
     * If the service returns an enum value that is not available in the current SDK version, {@link #status} will
     * return {@link CertificateStatus#UNKNOWN_TO_SDK_VERSION}. The raw value returned by the service is available from
     * {@link #statusAsString}.
     * </p>
     * 
     * @return The status of the certificate.</p>
     *         <p>
     *         A certificate enters status PENDING_VALIDATION upon being requested, unless it fails for any of the
     *         reasons given in the troubleshooting topic <a
     *         href="https://docs.aws.amazon.com/acm/latest/userguide/troubleshooting-failed.html">Certificate request
     *         fails</a>. ACM makes repeated attempts to validate a certificate for 72 hours and then times out. If a
     *         certificate shows status FAILED or VALIDATION_TIMED_OUT, delete the request, correct the issue with <a
     *         href="https://docs.aws.amazon.com/acm/latest/userguide/dns-validation.html">DNS validation</a> or <a
     *         href="https://docs.aws.amazon.com/acm/latest/userguide/email-validation.html">Email validation</a>, and
     *         try again. If validation succeeds, the certificate enters status ISSUED.
     * @see CertificateStatus
     */
    public final String statusAsString() {
        return status;
    }

    /**
     * <p>
     * The time at which the certificate was revoked. This value exists only when the certificate status is
     * <code>REVOKED</code>.
     * </p>
     * 
     * @return The time at which the certificate was revoked. This value exists only when the certificate status is
     *         <code>REVOKED</code>.
     */
    public final Instant revokedAt() {
        return revokedAt;
    }

    /**
     * <p>
     * The reason the certificate was revoked. This value exists only when the certificate status is
     * <code>REVOKED</code>.
     * </p>
     * <p>
     * If the service returns an enum value that is not available in the current SDK version, {@link #revocationReason}
     * will return {@link RevocationReason#UNKNOWN_TO_SDK_VERSION}. The raw value returned by the service is available
     * from {@link #revocationReasonAsString}.
     * </p>
     * 
     * @return The reason the certificate was revoked. This value exists only when the certificate status is
     *         <code>REVOKED</code>.
     * @see RevocationReason
     */
    public final RevocationReason revocationReason() {
        return RevocationReason.fromValue(revocationReason);
    }

    /**
     * <p>
     * The reason the certificate was revoked. This value exists only when the certificate status is
     * <code>REVOKED</code>.
     * </p>
     * <p>
     * If the service returns an enum value that is not available in the current SDK version, {@link #revocationReason}
     * will return {@link RevocationReason#UNKNOWN_TO_SDK_VERSION}. The raw value returned by the service is available
     * from {@link #revocationReasonAsString}.
     * </p>
     * 
     * @return The reason the certificate was revoked. This value exists only when the certificate status is
     *         <code>REVOKED</code>.
     * @see RevocationReason
     */
    public final String revocationReasonAsString() {
        return revocationReason;
    }

    /**
     * <p>
     * The time before which the certificate is not valid.
     * </p>
     * 
     * @return The time before which the certificate is not valid.
     */
    public final Instant notBefore() {
        return notBefore;
    }

    /**
     * <p>
     * The time after which the certificate is not valid.
     * </p>
     * 
     * @return The time after which the certificate is not valid.
     */
    public final Instant notAfter() {
        return notAfter;
    }

    /**
     * <p>
     * The algorithm that was used to generate the public-private key pair.
     * </p>
     * <p>
     * If the service returns an enum value that is not available in the current SDK version, {@link #keyAlgorithm} will
     * return {@link KeyAlgorithm#UNKNOWN_TO_SDK_VERSION}. The raw value returned by the service is available from
     * {@link #keyAlgorithmAsString}.
     * </p>
     * 
     * @return The algorithm that was used to generate the public-private key pair.
     * @see KeyAlgorithm
     */
    public final KeyAlgorithm keyAlgorithm() {
        return KeyAlgorithm.fromValue(keyAlgorithm);
    }

    /**
     * <p>
     * The algorithm that was used to generate the public-private key pair.
     * </p>
     * <p>
     * If the service returns an enum value that is not available in the current SDK version, {@link #keyAlgorithm} will
     * return {@link KeyAlgorithm#UNKNOWN_TO_SDK_VERSION}. The raw value returned by the service is available from
     * {@link #keyAlgorithmAsString}.
     * </p>
     * 
     * @return The algorithm that was used to generate the public-private key pair.
     * @see KeyAlgorithm
     */
    public final String keyAlgorithmAsString() {
        return keyAlgorithm;
    }

    /**
     * <p>
     * The algorithm that was used to sign the certificate.
     * </p>
     * 
     * @return The algorithm that was used to sign the certificate.
     */
    public final String signatureAlgorithm() {
        return signatureAlgorithm;
    }

    /**
     * For responses, this returns true if the service returned a value for the InUseBy property. This DOES NOT check
     * that the value is non-empty (for which, you should check the {@code isEmpty()} method on the property). This is
     * useful because the SDK will never return a null collection or map, but you may need to differentiate between the
     * service returning nothing (or null) and the service returning an empty collection or map. For requests, this
     * returns true if a value for the property was specified in the request builder, and false if a value was not
     * specified.
     */
    public final boolean hasInUseBy() {
        return inUseBy != null && !(inUseBy instanceof SdkAutoConstructList);
    }

    /**
     * <p>
     * A list of ARNs for the Amazon Web Services resources that are using the certificate. A certificate can be used by
     * multiple Amazon Web Services resources.
     * </p>
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     * <p>
     * This method will never return null. If you would like to know whether the service returned this field (so that
     * you can differentiate between null and empty), you can use the {@link #hasInUseBy} method.
     * </p>
     * 
     * @return A list of ARNs for the Amazon Web Services resources that are using the certificate. A certificate can be
     *         used by multiple Amazon Web Services resources.
     */
    public final List<String> inUseBy() {
        return inUseBy;
    }

    /**
     * <p>
     * The reason the certificate request failed. This value exists only when the certificate status is
     * <code>FAILED</code>. For more information, see <a
     * href="https://docs.aws.amazon.com/acm/latest/userguide/troubleshooting.html#troubleshooting-failed">Certificate
     * Request Failed</a> in the <i>Certificate Manager User Guide</i>.
     * </p>
     * <p>
     * If the service returns an enum value that is not available in the current SDK version, {@link #failureReason}
     * will return {@link FailureReason#UNKNOWN_TO_SDK_VERSION}. The raw value returned by the service is available from
     * {@link #failureReasonAsString}.
     * </p>
     * 
     * @return The reason the certificate request failed. This value exists only when the certificate status is
     *         <code>FAILED</code>. For more information, see <a
     *         href="https://docs.aws.amazon.com/acm/latest/userguide/troubleshooting.html#troubleshooting-failed"
     *         >Certificate Request Failed</a> in the <i>Certificate Manager User Guide</i>.
     * @see FailureReason
     */
    public final FailureReason failureReason() {
        return FailureReason.fromValue(failureReason);
    }

    /**
     * <p>
     * The reason the certificate request failed. This value exists only when the certificate status is
     * <code>FAILED</code>. For more information, see <a
     * href="https://docs.aws.amazon.com/acm/latest/userguide/troubleshooting.html#troubleshooting-failed">Certificate
     * Request Failed</a> in the <i>Certificate Manager User Guide</i>.
     * </p>
     * <p>
     * If the service returns an enum value that is not available in the current SDK version, {@link #failureReason}
     * will return {@link FailureReason#UNKNOWN_TO_SDK_VERSION}. The raw value returned by the service is available from
     * {@link #failureReasonAsString}.
     * </p>
     * 
     * @return The reason the certificate request failed. This value exists only when the certificate status is
     *         <code>FAILED</code>. For more information, see <a
     *         href="https://docs.aws.amazon.com/acm/latest/userguide/troubleshooting.html#troubleshooting-failed"
     *         >Certificate Request Failed</a> in the <i>Certificate Manager User Guide</i>.
     * @see FailureReason
     */
    public final String failureReasonAsString() {
        return failureReason;
    }

    /**
     * <p>
     * The source of the certificate. For certificates provided by ACM, this value is <code>AMAZON_ISSUED</code>. For
     * certificates that you imported with <a>ImportCertificate</a>, this value is <code>IMPORTED</code>. ACM does not
     * provide <a href="https://docs.aws.amazon.com/acm/latest/userguide/acm-renewal.html">managed renewal</a> for
     * imported certificates. For more information about the differences between certificates that you import and those
     * that ACM provides, see <a
     * href="https://docs.aws.amazon.com/acm/latest/userguide/import-certificate.html">Importing Certificates</a> in the
     * <i>Certificate Manager User Guide</i>.
     * </p>
     * <p>
     * If the service returns an enum value that is not available in the current SDK version, {@link #type} will return
     * {@link CertificateType#UNKNOWN_TO_SDK_VERSION}. The raw value returned by the service is available from
     * {@link #typeAsString}.
     * </p>
     * 
     * @return The source of the certificate. For certificates provided by ACM, this value is <code>AMAZON_ISSUED</code>
     *         . For certificates that you imported with <a>ImportCertificate</a>, this value is <code>IMPORTED</code>.
     *         ACM does not provide <a href="https://docs.aws.amazon.com/acm/latest/userguide/acm-renewal.html">managed
     *         renewal</a> for imported certificates. For more information about the differences between certificates
     *         that you import and those that ACM provides, see <a
     *         href="https://docs.aws.amazon.com/acm/latest/userguide/import-certificate.html">Importing
     *         Certificates</a> in the <i>Certificate Manager User Guide</i>.
     * @see CertificateType
     */
    public final CertificateType type() {
        return CertificateType.fromValue(type);
    }

    /**
     * <p>
     * The source of the certificate. For certificates provided by ACM, this value is <code>AMAZON_ISSUED</code>. For
     * certificates that you imported with <a>ImportCertificate</a>, this value is <code>IMPORTED</code>. ACM does not
     * provide <a href="https://docs.aws.amazon.com/acm/latest/userguide/acm-renewal.html">managed renewal</a> for
     * imported certificates. For more information about the differences between certificates that you import and those
     * that ACM provides, see <a
     * href="https://docs.aws.amazon.com/acm/latest/userguide/import-certificate.html">Importing Certificates</a> in the
     * <i>Certificate Manager User Guide</i>.
     * </p>
     * <p>
     * If the service returns an enum value that is not available in the current SDK version, {@link #type} will return
     * {@link CertificateType#UNKNOWN_TO_SDK_VERSION}. The raw value returned by the service is available from
     * {@link #typeAsString}.
     * </p>
     * 
     * @return The source of the certificate. For certificates provided by ACM, this value is <code>AMAZON_ISSUED</code>
     *         . For certificates that you imported with <a>ImportCertificate</a>, this value is <code>IMPORTED</code>.
     *         ACM does not provide <a href="https://docs.aws.amazon.com/acm/latest/userguide/acm-renewal.html">managed
     *         renewal</a> for imported certificates. For more information about the differences between certificates
     *         that you import and those that ACM provides, see <a
     *         href="https://docs.aws.amazon.com/acm/latest/userguide/import-certificate.html">Importing
     *         Certificates</a> in the <i>Certificate Manager User Guide</i>.
     * @see CertificateType
     */
    public final String typeAsString() {
        return type;
    }

    /**
     * <p>
     * Contains information about the status of ACM's <a
     * href="https://docs.aws.amazon.com/acm/latest/userguide/acm-renewal.html">managed renewal</a> for the certificate.
     * This field exists only when the certificate type is <code>AMAZON_ISSUED</code>.
     * </p>
     * 
     * @return Contains information about the status of ACM's <a
     *         href="https://docs.aws.amazon.com/acm/latest/userguide/acm-renewal.html">managed renewal</a> for the
     *         certificate. This field exists only when the certificate type is <code>AMAZON_ISSUED</code>.
     */
    public final RenewalSummary renewalSummary() {
        return renewalSummary;
    }

    /**
     * For responses, this returns true if the service returned a value for the KeyUsages property. This DOES NOT check
     * that the value is non-empty (for which, you should check the {@code isEmpty()} method on the property). This is
     * useful because the SDK will never return a null collection or map, but you may need to differentiate between the
     * service returning nothing (or null) and the service returning an empty collection or map. For requests, this
     * returns true if a value for the property was specified in the request builder, and false if a value was not
     * specified.
     */
    public final boolean hasKeyUsages() {
        return keyUsages != null && !(keyUsages instanceof SdkAutoConstructList);
    }

    /**
     * <p>
     * A list of Key Usage X.509 v3 extension objects. Each object is a string value that identifies the purpose of the
     * public key contained in the certificate. Possible extension values include DIGITAL_SIGNATURE, KEY_ENCHIPHERMENT,
     * NON_REPUDIATION, and more.
     * </p>
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     * <p>
     * This method will never return null. If you would like to know whether the service returned this field (so that
     * you can differentiate between null and empty), you can use the {@link #hasKeyUsages} method.
     * </p>
     * 
     * @return A list of Key Usage X.509 v3 extension objects. Each object is a string value that identifies the purpose
     *         of the public key contained in the certificate. Possible extension values include DIGITAL_SIGNATURE,
     *         KEY_ENCHIPHERMENT, NON_REPUDIATION, and more.
     */
    public final List<KeyUsage> keyUsages() {
        return keyUsages;
    }

    /**
     * For responses, this returns true if the service returned a value for the ExtendedKeyUsages property. This DOES
     * NOT check that the value is non-empty (for which, you should check the {@code isEmpty()} method on the property).
     * This is useful because the SDK will never return a null collection or map, but you may need to differentiate
     * between the service returning nothing (or null) and the service returning an empty collection or map. For
     * requests, this returns true if a value for the property was specified in the request builder, and false if a
     * value was not specified.
     */
    public final boolean hasExtendedKeyUsages() {
        return extendedKeyUsages != null && !(extendedKeyUsages instanceof SdkAutoConstructList);
    }

    /**
     * <p>
     * Contains a list of Extended Key Usage X.509 v3 extension objects. Each object specifies a purpose for which the
     * certificate public key can be used and consists of a name and an object identifier (OID).
     * </p>
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     * <p>
     * This method will never return null. If you would like to know whether the service returned this field (so that
     * you can differentiate between null and empty), you can use the {@link #hasExtendedKeyUsages} method.
     * </p>
     * 
     * @return Contains a list of Extended Key Usage X.509 v3 extension objects. Each object specifies a purpose for
     *         which the certificate public key can be used and consists of a name and an object identifier (OID).
     */
    public final List<ExtendedKeyUsage> extendedKeyUsages() {
        return extendedKeyUsages;
    }

    /**
     * <p>
     * The Amazon Resource Name (ARN) of the private certificate authority (CA) that issued the certificate. This has
     * the following format:
     * </p>
     * <p>
     * <code>arn:aws:acm-pca:region:account:certificate-authority/12345678-1234-1234-1234-123456789012</code>
     * </p>
     * 
     * @return The Amazon Resource Name (ARN) of the private certificate authority (CA) that issued the certificate.
     *         This has the following format: </p>
     *         <p>
     *         <code>arn:aws:acm-pca:region:account:certificate-authority/12345678-1234-1234-1234-123456789012</code>
     */
    public final String certificateAuthorityArn() {
        return certificateAuthorityArn;
    }

    /**
     * <p>
     * Specifies whether the certificate is eligible for renewal. At this time, only exported private certificates can
     * be renewed with the <a>RenewCertificate</a> command.
     * </p>
     * <p>
     * If the service returns an enum value that is not available in the current SDK version,
     * {@link #renewalEligibility} will return {@link RenewalEligibility#UNKNOWN_TO_SDK_VERSION}. The raw value returned
     * by the service is available from {@link #renewalEligibilityAsString}.
     * </p>
     * 
     * @return Specifies whether the certificate is eligible for renewal. At this time, only exported private
     *         certificates can be renewed with the <a>RenewCertificate</a> command.
     * @see RenewalEligibility
     */
    public final RenewalEligibility renewalEligibility() {
        return RenewalEligibility.fromValue(renewalEligibility);
    }

    /**
     * <p>
     * Specifies whether the certificate is eligible for renewal. At this time, only exported private certificates can
     * be renewed with the <a>RenewCertificate</a> command.
     * </p>
     * <p>
     * If the service returns an enum value that is not available in the current SDK version,
     * {@link #renewalEligibility} will return {@link RenewalEligibility#UNKNOWN_TO_SDK_VERSION}. The raw value returned
     * by the service is available from {@link #renewalEligibilityAsString}.
     * </p>
     * 
     * @return Specifies whether the certificate is eligible for renewal. At this time, only exported private
     *         certificates can be renewed with the <a>RenewCertificate</a> command.
     * @see RenewalEligibility
     */
    public final String renewalEligibilityAsString() {
        return renewalEligibility;
    }

    /**
     * <p>
     * Value that specifies whether to add the certificate to a transparency log. Certificate transparency makes it
     * possible to detect SSL certificates that have been mistakenly or maliciously issued. A browser might respond to
     * certificate that has not been logged by showing an error message. The logs are cryptographically secure.
     * </p>
     * 
     * @return Value that specifies whether to add the certificate to a transparency log. Certificate transparency makes
     *         it possible to detect SSL certificates that have been mistakenly or maliciously issued. A browser might
     *         respond to certificate that has not been logged by showing an error message. The logs are
     *         cryptographically secure.
     */
    public final CertificateOptions options() {
        return options;
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
        hashCode = 31 * hashCode + Objects.hashCode(certificateArn());
        hashCode = 31 * hashCode + Objects.hashCode(domainName());
        hashCode = 31 * hashCode + Objects.hashCode(hasSubjectAlternativeNames() ? subjectAlternativeNames() : null);
        hashCode = 31 * hashCode + Objects.hashCode(hasDomainValidationOptions() ? domainValidationOptions() : null);
        hashCode = 31 * hashCode + Objects.hashCode(serial());
        hashCode = 31 * hashCode + Objects.hashCode(subject());
        hashCode = 31 * hashCode + Objects.hashCode(issuer());
        hashCode = 31 * hashCode + Objects.hashCode(createdAt());
        hashCode = 31 * hashCode + Objects.hashCode(issuedAt());
        hashCode = 31 * hashCode + Objects.hashCode(importedAt());
        hashCode = 31 * hashCode + Objects.hashCode(statusAsString());
        hashCode = 31 * hashCode + Objects.hashCode(revokedAt());
        hashCode = 31 * hashCode + Objects.hashCode(revocationReasonAsString());
        hashCode = 31 * hashCode + Objects.hashCode(notBefore());
        hashCode = 31 * hashCode + Objects.hashCode(notAfter());
        hashCode = 31 * hashCode + Objects.hashCode(keyAlgorithmAsString());
        hashCode = 31 * hashCode + Objects.hashCode(signatureAlgorithm());
        hashCode = 31 * hashCode + Objects.hashCode(hasInUseBy() ? inUseBy() : null);
        hashCode = 31 * hashCode + Objects.hashCode(failureReasonAsString());
        hashCode = 31 * hashCode + Objects.hashCode(typeAsString());
        hashCode = 31 * hashCode + Objects.hashCode(renewalSummary());
        hashCode = 31 * hashCode + Objects.hashCode(hasKeyUsages() ? keyUsages() : null);
        hashCode = 31 * hashCode + Objects.hashCode(hasExtendedKeyUsages() ? extendedKeyUsages() : null);
        hashCode = 31 * hashCode + Objects.hashCode(certificateAuthorityArn());
        hashCode = 31 * hashCode + Objects.hashCode(renewalEligibilityAsString());
        hashCode = 31 * hashCode + Objects.hashCode(options());
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
        if (!(obj instanceof CertificateDetail)) {
            return false;
        }
        CertificateDetail other = (CertificateDetail) obj;
        return Objects.equals(certificateArn(), other.certificateArn()) && Objects.equals(domainName(), other.domainName())
                && hasSubjectAlternativeNames() == other.hasSubjectAlternativeNames()
                && Objects.equals(subjectAlternativeNames(), other.subjectAlternativeNames())
                && hasDomainValidationOptions() == other.hasDomainValidationOptions()
                && Objects.equals(domainValidationOptions(), other.domainValidationOptions())
                && Objects.equals(serial(), other.serial()) && Objects.equals(subject(), other.subject())
                && Objects.equals(issuer(), other.issuer()) && Objects.equals(createdAt(), other.createdAt())
                && Objects.equals(issuedAt(), other.issuedAt()) && Objects.equals(importedAt(), other.importedAt())
                && Objects.equals(statusAsString(), other.statusAsString()) && Objects.equals(revokedAt(), other.revokedAt())
                && Objects.equals(revocationReasonAsString(), other.revocationReasonAsString())
                && Objects.equals(notBefore(), other.notBefore()) && Objects.equals(notAfter(), other.notAfter())
                && Objects.equals(keyAlgorithmAsString(), other.keyAlgorithmAsString())
                && Objects.equals(signatureAlgorithm(), other.signatureAlgorithm()) && hasInUseBy() == other.hasInUseBy()
                && Objects.equals(inUseBy(), other.inUseBy())
                && Objects.equals(failureReasonAsString(), other.failureReasonAsString())
                && Objects.equals(typeAsString(), other.typeAsString())
                && Objects.equals(renewalSummary(), other.renewalSummary()) && hasKeyUsages() == other.hasKeyUsages()
                && Objects.equals(keyUsages(), other.keyUsages()) && hasExtendedKeyUsages() == other.hasExtendedKeyUsages()
                && Objects.equals(extendedKeyUsages(), other.extendedKeyUsages())
                && Objects.equals(certificateAuthorityArn(), other.certificateAuthorityArn())
                && Objects.equals(renewalEligibilityAsString(), other.renewalEligibilityAsString())
                && Objects.equals(options(), other.options());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public final String toString() {
        return ToString.builder("CertificateDetail").add("CertificateArn", certificateArn()).add("DomainName", domainName())
                .add("SubjectAlternativeNames", hasSubjectAlternativeNames() ? subjectAlternativeNames() : null)
                .add("DomainValidationOptions", hasDomainValidationOptions() ? domainValidationOptions() : null)
                .add("Serial", serial()).add("Subject", subject()).add("Issuer", issuer()).add("CreatedAt", createdAt())
                .add("IssuedAt", issuedAt()).add("ImportedAt", importedAt()).add("Status", statusAsString())
                .add("RevokedAt", revokedAt()).add("RevocationReason", revocationReasonAsString()).add("NotBefore", notBefore())
                .add("NotAfter", notAfter()).add("KeyAlgorithm", keyAlgorithmAsString())
                .add("SignatureAlgorithm", signatureAlgorithm()).add("InUseBy", hasInUseBy() ? inUseBy() : null)
                .add("FailureReason", failureReasonAsString()).add("Type", typeAsString())
                .add("RenewalSummary", renewalSummary()).add("KeyUsages", hasKeyUsages() ? keyUsages() : null)
                .add("ExtendedKeyUsages", hasExtendedKeyUsages() ? extendedKeyUsages() : null)
                .add("CertificateAuthorityArn", certificateAuthorityArn())
                .add("RenewalEligibility", renewalEligibilityAsString()).add("Options", options()).build();
    }

    public final <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
        case "CertificateArn":
            return Optional.ofNullable(clazz.cast(certificateArn()));
        case "DomainName":
            return Optional.ofNullable(clazz.cast(domainName()));
        case "SubjectAlternativeNames":
            return Optional.ofNullable(clazz.cast(subjectAlternativeNames()));
        case "DomainValidationOptions":
            return Optional.ofNullable(clazz.cast(domainValidationOptions()));
        case "Serial":
            return Optional.ofNullable(clazz.cast(serial()));
        case "Subject":
            return Optional.ofNullable(clazz.cast(subject()));
        case "Issuer":
            return Optional.ofNullable(clazz.cast(issuer()));
        case "CreatedAt":
            return Optional.ofNullable(clazz.cast(createdAt()));
        case "IssuedAt":
            return Optional.ofNullable(clazz.cast(issuedAt()));
        case "ImportedAt":
            return Optional.ofNullable(clazz.cast(importedAt()));
        case "Status":
            return Optional.ofNullable(clazz.cast(statusAsString()));
        case "RevokedAt":
            return Optional.ofNullable(clazz.cast(revokedAt()));
        case "RevocationReason":
            return Optional.ofNullable(clazz.cast(revocationReasonAsString()));
        case "NotBefore":
            return Optional.ofNullable(clazz.cast(notBefore()));
        case "NotAfter":
            return Optional.ofNullable(clazz.cast(notAfter()));
        case "KeyAlgorithm":
            return Optional.ofNullable(clazz.cast(keyAlgorithmAsString()));
        case "SignatureAlgorithm":
            return Optional.ofNullable(clazz.cast(signatureAlgorithm()));
        case "InUseBy":
            return Optional.ofNullable(clazz.cast(inUseBy()));
        case "FailureReason":
            return Optional.ofNullable(clazz.cast(failureReasonAsString()));
        case "Type":
            return Optional.ofNullable(clazz.cast(typeAsString()));
        case "RenewalSummary":
            return Optional.ofNullable(clazz.cast(renewalSummary()));
        case "KeyUsages":
            return Optional.ofNullable(clazz.cast(keyUsages()));
        case "ExtendedKeyUsages":
            return Optional.ofNullable(clazz.cast(extendedKeyUsages()));
        case "CertificateAuthorityArn":
            return Optional.ofNullable(clazz.cast(certificateAuthorityArn()));
        case "RenewalEligibility":
            return Optional.ofNullable(clazz.cast(renewalEligibilityAsString()));
        case "Options":
            return Optional.ofNullable(clazz.cast(options()));
        default:
            return Optional.empty();
        }
    }

    @Override
    public final List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    private static <T> Function<Object, T> getter(Function<CertificateDetail, T> g) {
        return obj -> g.apply((CertificateDetail) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    public interface Builder extends SdkPojo, CopyableBuilder<Builder, CertificateDetail> {
        /**
         * <p>
         * The Amazon Resource Name (ARN) of the certificate. For more information about ARNs, see <a
         * href="https://docs.aws.amazon.com/general/latest/gr/aws-arns-and-namespaces.html">Amazon Resource Names
         * (ARNs)</a> in the <i>Amazon Web Services General Reference</i>.
         * </p>
         * 
         * @param certificateArn
         *        The Amazon Resource Name (ARN) of the certificate. For more information about ARNs, see <a
         *        href="https://docs.aws.amazon.com/general/latest/gr/aws-arns-and-namespaces.html">Amazon Resource
         *        Names (ARNs)</a> in the <i>Amazon Web Services General Reference</i>.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder certificateArn(String certificateArn);

        /**
         * <p>
         * The fully qualified domain name for the certificate, such as www.example.com or example.com.
         * </p>
         * 
         * @param domainName
         *        The fully qualified domain name for the certificate, such as www.example.com or example.com.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder domainName(String domainName);

        /**
         * <p>
         * One or more domain names (subject alternative names) included in the certificate. This list contains the
         * domain names that are bound to the public key that is contained in the certificate. The subject alternative
         * names include the canonical domain name (CN) of the certificate and additional domain names that can be used
         * to connect to the website.
         * </p>
         * 
         * @param subjectAlternativeNames
         *        One or more domain names (subject alternative names) included in the certificate. This list contains
         *        the domain names that are bound to the public key that is contained in the certificate. The subject
         *        alternative names include the canonical domain name (CN) of the certificate and additional domain
         *        names that can be used to connect to the website.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder subjectAlternativeNames(Collection<String> subjectAlternativeNames);

        /**
         * <p>
         * One or more domain names (subject alternative names) included in the certificate. This list contains the
         * domain names that are bound to the public key that is contained in the certificate. The subject alternative
         * names include the canonical domain name (CN) of the certificate and additional domain names that can be used
         * to connect to the website.
         * </p>
         * 
         * @param subjectAlternativeNames
         *        One or more domain names (subject alternative names) included in the certificate. This list contains
         *        the domain names that are bound to the public key that is contained in the certificate. The subject
         *        alternative names include the canonical domain name (CN) of the certificate and additional domain
         *        names that can be used to connect to the website.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder subjectAlternativeNames(String... subjectAlternativeNames);

        /**
         * <p>
         * Contains information about the initial validation of each domain name that occurs as a result of the
         * <a>RequestCertificate</a> request. This field exists only when the certificate type is
         * <code>AMAZON_ISSUED</code>.
         * </p>
         * 
         * @param domainValidationOptions
         *        Contains information about the initial validation of each domain name that occurs as a result of the
         *        <a>RequestCertificate</a> request. This field exists only when the certificate type is
         *        <code>AMAZON_ISSUED</code>.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder domainValidationOptions(Collection<DomainValidation> domainValidationOptions);

        /**
         * <p>
         * Contains information about the initial validation of each domain name that occurs as a result of the
         * <a>RequestCertificate</a> request. This field exists only when the certificate type is
         * <code>AMAZON_ISSUED</code>.
         * </p>
         * 
         * @param domainValidationOptions
         *        Contains information about the initial validation of each domain name that occurs as a result of the
         *        <a>RequestCertificate</a> request. This field exists only when the certificate type is
         *        <code>AMAZON_ISSUED</code>.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder domainValidationOptions(DomainValidation... domainValidationOptions);

        /**
         * <p>
         * Contains information about the initial validation of each domain name that occurs as a result of the
         * <a>RequestCertificate</a> request. This field exists only when the certificate type is
         * <code>AMAZON_ISSUED</code>.
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
         * The serial number of the certificate.
         * </p>
         * 
         * @param serial
         *        The serial number of the certificate.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder serial(String serial);

        /**
         * <p>
         * The name of the entity that is associated with the public key contained in the certificate.
         * </p>
         * 
         * @param subject
         *        The name of the entity that is associated with the public key contained in the certificate.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder subject(String subject);

        /**
         * <p>
         * The name of the certificate authority that issued and signed the certificate.
         * </p>
         * 
         * @param issuer
         *        The name of the certificate authority that issued and signed the certificate.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder issuer(String issuer);

        /**
         * <p>
         * The time at which the certificate was requested.
         * </p>
         * 
         * @param createdAt
         *        The time at which the certificate was requested.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder createdAt(Instant createdAt);

        /**
         * <p>
         * The time at which the certificate was issued. This value exists only when the certificate type is
         * <code>AMAZON_ISSUED</code>.
         * </p>
         * 
         * @param issuedAt
         *        The time at which the certificate was issued. This value exists only when the certificate type is
         *        <code>AMAZON_ISSUED</code>.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder issuedAt(Instant issuedAt);

        /**
         * <p>
         * The date and time when the certificate was imported. This value exists only when the certificate type is
         * <code>IMPORTED</code>.
         * </p>
         * 
         * @param importedAt
         *        The date and time when the certificate was imported. This value exists only when the certificate type
         *        is <code>IMPORTED</code>.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder importedAt(Instant importedAt);

        /**
         * <p>
         * The status of the certificate.
         * </p>
         * <p>
         * A certificate enters status PENDING_VALIDATION upon being requested, unless it fails for any of the reasons
         * given in the troubleshooting topic <a
         * href="https://docs.aws.amazon.com/acm/latest/userguide/troubleshooting-failed.html">Certificate request
         * fails</a>. ACM makes repeated attempts to validate a certificate for 72 hours and then times out. If a
         * certificate shows status FAILED or VALIDATION_TIMED_OUT, delete the request, correct the issue with <a
         * href="https://docs.aws.amazon.com/acm/latest/userguide/dns-validation.html">DNS validation</a> or <a
         * href="https://docs.aws.amazon.com/acm/latest/userguide/email-validation.html">Email validation</a>, and try
         * again. If validation succeeds, the certificate enters status ISSUED.
         * </p>
         * 
         * @param status
         *        The status of the certificate.</p>
         *        <p>
         *        A certificate enters status PENDING_VALIDATION upon being requested, unless it fails for any of the
         *        reasons given in the troubleshooting topic <a
         *        href="https://docs.aws.amazon.com/acm/latest/userguide/troubleshooting-failed.html">Certificate
         *        request fails</a>. ACM makes repeated attempts to validate a certificate for 72 hours and then times
         *        out. If a certificate shows status FAILED or VALIDATION_TIMED_OUT, delete the request, correct the
         *        issue with <a href="https://docs.aws.amazon.com/acm/latest/userguide/dns-validation.html">DNS
         *        validation</a> or <a
         *        href="https://docs.aws.amazon.com/acm/latest/userguide/email-validation.html">Email validation</a>,
         *        and try again. If validation succeeds, the certificate enters status ISSUED.
         * @see CertificateStatus
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see CertificateStatus
         */
        Builder status(String status);

        /**
         * <p>
         * The status of the certificate.
         * </p>
         * <p>
         * A certificate enters status PENDING_VALIDATION upon being requested, unless it fails for any of the reasons
         * given in the troubleshooting topic <a
         * href="https://docs.aws.amazon.com/acm/latest/userguide/troubleshooting-failed.html">Certificate request
         * fails</a>. ACM makes repeated attempts to validate a certificate for 72 hours and then times out. If a
         * certificate shows status FAILED or VALIDATION_TIMED_OUT, delete the request, correct the issue with <a
         * href="https://docs.aws.amazon.com/acm/latest/userguide/dns-validation.html">DNS validation</a> or <a
         * href="https://docs.aws.amazon.com/acm/latest/userguide/email-validation.html">Email validation</a>, and try
         * again. If validation succeeds, the certificate enters status ISSUED.
         * </p>
         * 
         * @param status
         *        The status of the certificate.</p>
         *        <p>
         *        A certificate enters status PENDING_VALIDATION upon being requested, unless it fails for any of the
         *        reasons given in the troubleshooting topic <a
         *        href="https://docs.aws.amazon.com/acm/latest/userguide/troubleshooting-failed.html">Certificate
         *        request fails</a>. ACM makes repeated attempts to validate a certificate for 72 hours and then times
         *        out. If a certificate shows status FAILED or VALIDATION_TIMED_OUT, delete the request, correct the
         *        issue with <a href="https://docs.aws.amazon.com/acm/latest/userguide/dns-validation.html">DNS
         *        validation</a> or <a
         *        href="https://docs.aws.amazon.com/acm/latest/userguide/email-validation.html">Email validation</a>,
         *        and try again. If validation succeeds, the certificate enters status ISSUED.
         * @see CertificateStatus
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see CertificateStatus
         */
        Builder status(CertificateStatus status);

        /**
         * <p>
         * The time at which the certificate was revoked. This value exists only when the certificate status is
         * <code>REVOKED</code>.
         * </p>
         * 
         * @param revokedAt
         *        The time at which the certificate was revoked. This value exists only when the certificate status is
         *        <code>REVOKED</code>.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder revokedAt(Instant revokedAt);

        /**
         * <p>
         * The reason the certificate was revoked. This value exists only when the certificate status is
         * <code>REVOKED</code>.
         * </p>
         * 
         * @param revocationReason
         *        The reason the certificate was revoked. This value exists only when the certificate status is
         *        <code>REVOKED</code>.
         * @see RevocationReason
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see RevocationReason
         */
        Builder revocationReason(String revocationReason);

        /**
         * <p>
         * The reason the certificate was revoked. This value exists only when the certificate status is
         * <code>REVOKED</code>.
         * </p>
         * 
         * @param revocationReason
         *        The reason the certificate was revoked. This value exists only when the certificate status is
         *        <code>REVOKED</code>.
         * @see RevocationReason
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see RevocationReason
         */
        Builder revocationReason(RevocationReason revocationReason);

        /**
         * <p>
         * The time before which the certificate is not valid.
         * </p>
         * 
         * @param notBefore
         *        The time before which the certificate is not valid.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder notBefore(Instant notBefore);

        /**
         * <p>
         * The time after which the certificate is not valid.
         * </p>
         * 
         * @param notAfter
         *        The time after which the certificate is not valid.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder notAfter(Instant notAfter);

        /**
         * <p>
         * The algorithm that was used to generate the public-private key pair.
         * </p>
         * 
         * @param keyAlgorithm
         *        The algorithm that was used to generate the public-private key pair.
         * @see KeyAlgorithm
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see KeyAlgorithm
         */
        Builder keyAlgorithm(String keyAlgorithm);

        /**
         * <p>
         * The algorithm that was used to generate the public-private key pair.
         * </p>
         * 
         * @param keyAlgorithm
         *        The algorithm that was used to generate the public-private key pair.
         * @see KeyAlgorithm
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see KeyAlgorithm
         */
        Builder keyAlgorithm(KeyAlgorithm keyAlgorithm);

        /**
         * <p>
         * The algorithm that was used to sign the certificate.
         * </p>
         * 
         * @param signatureAlgorithm
         *        The algorithm that was used to sign the certificate.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder signatureAlgorithm(String signatureAlgorithm);

        /**
         * <p>
         * A list of ARNs for the Amazon Web Services resources that are using the certificate. A certificate can be
         * used by multiple Amazon Web Services resources.
         * </p>
         * 
         * @param inUseBy
         *        A list of ARNs for the Amazon Web Services resources that are using the certificate. A certificate can
         *        be used by multiple Amazon Web Services resources.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder inUseBy(Collection<String> inUseBy);

        /**
         * <p>
         * A list of ARNs for the Amazon Web Services resources that are using the certificate. A certificate can be
         * used by multiple Amazon Web Services resources.
         * </p>
         * 
         * @param inUseBy
         *        A list of ARNs for the Amazon Web Services resources that are using the certificate. A certificate can
         *        be used by multiple Amazon Web Services resources.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder inUseBy(String... inUseBy);

        /**
         * <p>
         * The reason the certificate request failed. This value exists only when the certificate status is
         * <code>FAILED</code>. For more information, see <a
         * href="https://docs.aws.amazon.com/acm/latest/userguide/troubleshooting.html#troubleshooting-failed"
         * >Certificate Request Failed</a> in the <i>Certificate Manager User Guide</i>.
         * </p>
         * 
         * @param failureReason
         *        The reason the certificate request failed. This value exists only when the certificate status is
         *        <code>FAILED</code>. For more information, see <a
         *        href="https://docs.aws.amazon.com/acm/latest/userguide/troubleshooting.html#troubleshooting-failed"
         *        >Certificate Request Failed</a> in the <i>Certificate Manager User Guide</i>.
         * @see FailureReason
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see FailureReason
         */
        Builder failureReason(String failureReason);

        /**
         * <p>
         * The reason the certificate request failed. This value exists only when the certificate status is
         * <code>FAILED</code>. For more information, see <a
         * href="https://docs.aws.amazon.com/acm/latest/userguide/troubleshooting.html#troubleshooting-failed"
         * >Certificate Request Failed</a> in the <i>Certificate Manager User Guide</i>.
         * </p>
         * 
         * @param failureReason
         *        The reason the certificate request failed. This value exists only when the certificate status is
         *        <code>FAILED</code>. For more information, see <a
         *        href="https://docs.aws.amazon.com/acm/latest/userguide/troubleshooting.html#troubleshooting-failed"
         *        >Certificate Request Failed</a> in the <i>Certificate Manager User Guide</i>.
         * @see FailureReason
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see FailureReason
         */
        Builder failureReason(FailureReason failureReason);

        /**
         * <p>
         * The source of the certificate. For certificates provided by ACM, this value is <code>AMAZON_ISSUED</code>.
         * For certificates that you imported with <a>ImportCertificate</a>, this value is <code>IMPORTED</code>. ACM
         * does not provide <a href="https://docs.aws.amazon.com/acm/latest/userguide/acm-renewal.html">managed
         * renewal</a> for imported certificates. For more information about the differences between certificates that
         * you import and those that ACM provides, see <a
         * href="https://docs.aws.amazon.com/acm/latest/userguide/import-certificate.html">Importing Certificates</a> in
         * the <i>Certificate Manager User Guide</i>.
         * </p>
         * 
         * @param type
         *        The source of the certificate. For certificates provided by ACM, this value is
         *        <code>AMAZON_ISSUED</code>. For certificates that you imported with <a>ImportCertificate</a>, this
         *        value is <code>IMPORTED</code>. ACM does not provide <a
         *        href="https://docs.aws.amazon.com/acm/latest/userguide/acm-renewal.html">managed renewal</a> for
         *        imported certificates. For more information about the differences between certificates that you import
         *        and those that ACM provides, see <a
         *        href="https://docs.aws.amazon.com/acm/latest/userguide/import-certificate.html">Importing
         *        Certificates</a> in the <i>Certificate Manager User Guide</i>.
         * @see CertificateType
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see CertificateType
         */
        Builder type(String type);

        /**
         * <p>
         * The source of the certificate. For certificates provided by ACM, this value is <code>AMAZON_ISSUED</code>.
         * For certificates that you imported with <a>ImportCertificate</a>, this value is <code>IMPORTED</code>. ACM
         * does not provide <a href="https://docs.aws.amazon.com/acm/latest/userguide/acm-renewal.html">managed
         * renewal</a> for imported certificates. For more information about the differences between certificates that
         * you import and those that ACM provides, see <a
         * href="https://docs.aws.amazon.com/acm/latest/userguide/import-certificate.html">Importing Certificates</a> in
         * the <i>Certificate Manager User Guide</i>.
         * </p>
         * 
         * @param type
         *        The source of the certificate. For certificates provided by ACM, this value is
         *        <code>AMAZON_ISSUED</code>. For certificates that you imported with <a>ImportCertificate</a>, this
         *        value is <code>IMPORTED</code>. ACM does not provide <a
         *        href="https://docs.aws.amazon.com/acm/latest/userguide/acm-renewal.html">managed renewal</a> for
         *        imported certificates. For more information about the differences between certificates that you import
         *        and those that ACM provides, see <a
         *        href="https://docs.aws.amazon.com/acm/latest/userguide/import-certificate.html">Importing
         *        Certificates</a> in the <i>Certificate Manager User Guide</i>.
         * @see CertificateType
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see CertificateType
         */
        Builder type(CertificateType type);

        /**
         * <p>
         * Contains information about the status of ACM's <a
         * href="https://docs.aws.amazon.com/acm/latest/userguide/acm-renewal.html">managed renewal</a> for the
         * certificate. This field exists only when the certificate type is <code>AMAZON_ISSUED</code>.
         * </p>
         * 
         * @param renewalSummary
         *        Contains information about the status of ACM's <a
         *        href="https://docs.aws.amazon.com/acm/latest/userguide/acm-renewal.html">managed renewal</a> for the
         *        certificate. This field exists only when the certificate type is <code>AMAZON_ISSUED</code>.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder renewalSummary(RenewalSummary renewalSummary);

        /**
         * <p>
         * Contains information about the status of ACM's <a
         * href="https://docs.aws.amazon.com/acm/latest/userguide/acm-renewal.html">managed renewal</a> for the
         * certificate. This field exists only when the certificate type is <code>AMAZON_ISSUED</code>.
         * </p>
         * This is a convenience method that creates an instance of the {@link RenewalSummary.Builder} avoiding the need
         * to create one manually via {@link RenewalSummary#builder()}.
         *
         * <p>
         * When the {@link Consumer} completes, {@link RenewalSummary.Builder#build()} is called immediately and its
         * result is passed to {@link #renewalSummary(RenewalSummary)}.
         * 
         * @param renewalSummary
         *        a consumer that will call methods on {@link RenewalSummary.Builder}
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see #renewalSummary(RenewalSummary)
         */
        default Builder renewalSummary(Consumer<RenewalSummary.Builder> renewalSummary) {
            return renewalSummary(RenewalSummary.builder().applyMutation(renewalSummary).build());
        }

        /**
         * <p>
         * A list of Key Usage X.509 v3 extension objects. Each object is a string value that identifies the purpose of
         * the public key contained in the certificate. Possible extension values include DIGITAL_SIGNATURE,
         * KEY_ENCHIPHERMENT, NON_REPUDIATION, and more.
         * </p>
         * 
         * @param keyUsages
         *        A list of Key Usage X.509 v3 extension objects. Each object is a string value that identifies the
         *        purpose of the public key contained in the certificate. Possible extension values include
         *        DIGITAL_SIGNATURE, KEY_ENCHIPHERMENT, NON_REPUDIATION, and more.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder keyUsages(Collection<KeyUsage> keyUsages);

        /**
         * <p>
         * A list of Key Usage X.509 v3 extension objects. Each object is a string value that identifies the purpose of
         * the public key contained in the certificate. Possible extension values include DIGITAL_SIGNATURE,
         * KEY_ENCHIPHERMENT, NON_REPUDIATION, and more.
         * </p>
         * 
         * @param keyUsages
         *        A list of Key Usage X.509 v3 extension objects. Each object is a string value that identifies the
         *        purpose of the public key contained in the certificate. Possible extension values include
         *        DIGITAL_SIGNATURE, KEY_ENCHIPHERMENT, NON_REPUDIATION, and more.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder keyUsages(KeyUsage... keyUsages);

        /**
         * <p>
         * A list of Key Usage X.509 v3 extension objects. Each object is a string value that identifies the purpose of
         * the public key contained in the certificate. Possible extension values include DIGITAL_SIGNATURE,
         * KEY_ENCHIPHERMENT, NON_REPUDIATION, and more.
         * </p>
         * This is a convenience method that creates an instance of the
         * {@link software.amazon.awssdk.services.acm.model.KeyUsage.Builder} avoiding the need to create one manually
         * via {@link software.amazon.awssdk.services.acm.model.KeyUsage#builder()}.
         *
         * <p>
         * When the {@link Consumer} completes,
         * {@link software.amazon.awssdk.services.acm.model.KeyUsage.Builder#build()} is called immediately and its
         * result is passed to {@link #keyUsages(List<KeyUsage>)}.
         * 
         * @param keyUsages
         *        a consumer that will call methods on
         *        {@link software.amazon.awssdk.services.acm.model.KeyUsage.Builder}
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see #keyUsages(java.util.Collection<KeyUsage>)
         */
        Builder keyUsages(Consumer<KeyUsage.Builder>... keyUsages);

        /**
         * <p>
         * Contains a list of Extended Key Usage X.509 v3 extension objects. Each object specifies a purpose for which
         * the certificate public key can be used and consists of a name and an object identifier (OID).
         * </p>
         * 
         * @param extendedKeyUsages
         *        Contains a list of Extended Key Usage X.509 v3 extension objects. Each object specifies a purpose for
         *        which the certificate public key can be used and consists of a name and an object identifier (OID).
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder extendedKeyUsages(Collection<ExtendedKeyUsage> extendedKeyUsages);

        /**
         * <p>
         * Contains a list of Extended Key Usage X.509 v3 extension objects. Each object specifies a purpose for which
         * the certificate public key can be used and consists of a name and an object identifier (OID).
         * </p>
         * 
         * @param extendedKeyUsages
         *        Contains a list of Extended Key Usage X.509 v3 extension objects. Each object specifies a purpose for
         *        which the certificate public key can be used and consists of a name and an object identifier (OID).
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder extendedKeyUsages(ExtendedKeyUsage... extendedKeyUsages);

        /**
         * <p>
         * Contains a list of Extended Key Usage X.509 v3 extension objects. Each object specifies a purpose for which
         * the certificate public key can be used and consists of a name and an object identifier (OID).
         * </p>
         * This is a convenience method that creates an instance of the
         * {@link software.amazon.awssdk.services.acm.model.ExtendedKeyUsage.Builder} avoiding the need to create one
         * manually via {@link software.amazon.awssdk.services.acm.model.ExtendedKeyUsage#builder()}.
         *
         * <p>
         * When the {@link Consumer} completes,
         * {@link software.amazon.awssdk.services.acm.model.ExtendedKeyUsage.Builder#build()} is called immediately and
         * its result is passed to {@link #extendedKeyUsages(List<ExtendedKeyUsage>)}.
         * 
         * @param extendedKeyUsages
         *        a consumer that will call methods on
         *        {@link software.amazon.awssdk.services.acm.model.ExtendedKeyUsage.Builder}
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see #extendedKeyUsages(java.util.Collection<ExtendedKeyUsage>)
         */
        Builder extendedKeyUsages(Consumer<ExtendedKeyUsage.Builder>... extendedKeyUsages);

        /**
         * <p>
         * The Amazon Resource Name (ARN) of the private certificate authority (CA) that issued the certificate. This
         * has the following format:
         * </p>
         * <p>
         * <code>arn:aws:acm-pca:region:account:certificate-authority/12345678-1234-1234-1234-123456789012</code>
         * </p>
         * 
         * @param certificateAuthorityArn
         *        The Amazon Resource Name (ARN) of the private certificate authority (CA) that issued the certificate.
         *        This has the following format: </p>
         *        <p>
         *        <code>arn:aws:acm-pca:region:account:certificate-authority/12345678-1234-1234-1234-123456789012</code>
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder certificateAuthorityArn(String certificateAuthorityArn);

        /**
         * <p>
         * Specifies whether the certificate is eligible for renewal. At this time, only exported private certificates
         * can be renewed with the <a>RenewCertificate</a> command.
         * </p>
         * 
         * @param renewalEligibility
         *        Specifies whether the certificate is eligible for renewal. At this time, only exported private
         *        certificates can be renewed with the <a>RenewCertificate</a> command.
         * @see RenewalEligibility
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see RenewalEligibility
         */
        Builder renewalEligibility(String renewalEligibility);

        /**
         * <p>
         * Specifies whether the certificate is eligible for renewal. At this time, only exported private certificates
         * can be renewed with the <a>RenewCertificate</a> command.
         * </p>
         * 
         * @param renewalEligibility
         *        Specifies whether the certificate is eligible for renewal. At this time, only exported private
         *        certificates can be renewed with the <a>RenewCertificate</a> command.
         * @see RenewalEligibility
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see RenewalEligibility
         */
        Builder renewalEligibility(RenewalEligibility renewalEligibility);

        /**
         * <p>
         * Value that specifies whether to add the certificate to a transparency log. Certificate transparency makes it
         * possible to detect SSL certificates that have been mistakenly or maliciously issued. A browser might respond
         * to certificate that has not been logged by showing an error message. The logs are cryptographically secure.
         * </p>
         * 
         * @param options
         *        Value that specifies whether to add the certificate to a transparency log. Certificate transparency
         *        makes it possible to detect SSL certificates that have been mistakenly or maliciously issued. A
         *        browser might respond to certificate that has not been logged by showing an error message. The logs
         *        are cryptographically secure.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder options(CertificateOptions options);

        /**
         * <p>
         * Value that specifies whether to add the certificate to a transparency log. Certificate transparency makes it
         * possible to detect SSL certificates that have been mistakenly or maliciously issued. A browser might respond
         * to certificate that has not been logged by showing an error message. The logs are cryptographically secure.
         * </p>
         * This is a convenience method that creates an instance of the {@link CertificateOptions.Builder} avoiding the
         * need to create one manually via {@link CertificateOptions#builder()}.
         *
         * <p>
         * When the {@link Consumer} completes, {@link CertificateOptions.Builder#build()} is called immediately and its
         * result is passed to {@link #options(CertificateOptions)}.
         * 
         * @param options
         *        a consumer that will call methods on {@link CertificateOptions.Builder}
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see #options(CertificateOptions)
         */
        default Builder options(Consumer<CertificateOptions.Builder> options) {
            return options(CertificateOptions.builder().applyMutation(options).build());
        }
    }

    static final class BuilderImpl implements Builder {
        private String certificateArn;

        private String domainName;

        private List<String> subjectAlternativeNames = DefaultSdkAutoConstructList.getInstance();

        private List<DomainValidation> domainValidationOptions = DefaultSdkAutoConstructList.getInstance();

        private String serial;

        private String subject;

        private String issuer;

        private Instant createdAt;

        private Instant issuedAt;

        private Instant importedAt;

        private String status;

        private Instant revokedAt;

        private String revocationReason;

        private Instant notBefore;

        private Instant notAfter;

        private String keyAlgorithm;

        private String signatureAlgorithm;

        private List<String> inUseBy = DefaultSdkAutoConstructList.getInstance();

        private String failureReason;

        private String type;

        private RenewalSummary renewalSummary;

        private List<KeyUsage> keyUsages = DefaultSdkAutoConstructList.getInstance();

        private List<ExtendedKeyUsage> extendedKeyUsages = DefaultSdkAutoConstructList.getInstance();

        private String certificateAuthorityArn;

        private String renewalEligibility;

        private CertificateOptions options;

        private BuilderImpl() {
        }

        private BuilderImpl(CertificateDetail model) {
            certificateArn(model.certificateArn);
            domainName(model.domainName);
            subjectAlternativeNames(model.subjectAlternativeNames);
            domainValidationOptions(model.domainValidationOptions);
            serial(model.serial);
            subject(model.subject);
            issuer(model.issuer);
            createdAt(model.createdAt);
            issuedAt(model.issuedAt);
            importedAt(model.importedAt);
            status(model.status);
            revokedAt(model.revokedAt);
            revocationReason(model.revocationReason);
            notBefore(model.notBefore);
            notAfter(model.notAfter);
            keyAlgorithm(model.keyAlgorithm);
            signatureAlgorithm(model.signatureAlgorithm);
            inUseBy(model.inUseBy);
            failureReason(model.failureReason);
            type(model.type);
            renewalSummary(model.renewalSummary);
            keyUsages(model.keyUsages);
            extendedKeyUsages(model.extendedKeyUsages);
            certificateAuthorityArn(model.certificateAuthorityArn);
            renewalEligibility(model.renewalEligibility);
            options(model.options);
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

        public final Collection<String> getSubjectAlternativeNames() {
            if (subjectAlternativeNames instanceof SdkAutoConstructList) {
                return null;
            }
            return subjectAlternativeNames;
        }

        public final void setSubjectAlternativeNames(Collection<String> subjectAlternativeNames) {
            this.subjectAlternativeNames = DomainListCopier.copy(subjectAlternativeNames);
        }

        @Override
        public final Builder subjectAlternativeNames(Collection<String> subjectAlternativeNames) {
            this.subjectAlternativeNames = DomainListCopier.copy(subjectAlternativeNames);
            return this;
        }

        @Override
        @SafeVarargs
        public final Builder subjectAlternativeNames(String... subjectAlternativeNames) {
            subjectAlternativeNames(Arrays.asList(subjectAlternativeNames));
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

        public final String getSerial() {
            return serial;
        }

        public final void setSerial(String serial) {
            this.serial = serial;
        }

        @Override
        public final Builder serial(String serial) {
            this.serial = serial;
            return this;
        }

        public final String getSubject() {
            return subject;
        }

        public final void setSubject(String subject) {
            this.subject = subject;
        }

        @Override
        public final Builder subject(String subject) {
            this.subject = subject;
            return this;
        }

        public final String getIssuer() {
            return issuer;
        }

        public final void setIssuer(String issuer) {
            this.issuer = issuer;
        }

        @Override
        public final Builder issuer(String issuer) {
            this.issuer = issuer;
            return this;
        }

        public final Instant getCreatedAt() {
            return createdAt;
        }

        public final void setCreatedAt(Instant createdAt) {
            this.createdAt = createdAt;
        }

        @Override
        public final Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public final Instant getIssuedAt() {
            return issuedAt;
        }

        public final void setIssuedAt(Instant issuedAt) {
            this.issuedAt = issuedAt;
        }

        @Override
        public final Builder issuedAt(Instant issuedAt) {
            this.issuedAt = issuedAt;
            return this;
        }

        public final Instant getImportedAt() {
            return importedAt;
        }

        public final void setImportedAt(Instant importedAt) {
            this.importedAt = importedAt;
        }

        @Override
        public final Builder importedAt(Instant importedAt) {
            this.importedAt = importedAt;
            return this;
        }

        public final String getStatus() {
            return status;
        }

        public final void setStatus(String status) {
            this.status = status;
        }

        @Override
        public final Builder status(String status) {
            this.status = status;
            return this;
        }

        @Override
        public final Builder status(CertificateStatus status) {
            this.status(status == null ? null : status.toString());
            return this;
        }

        public final Instant getRevokedAt() {
            return revokedAt;
        }

        public final void setRevokedAt(Instant revokedAt) {
            this.revokedAt = revokedAt;
        }

        @Override
        public final Builder revokedAt(Instant revokedAt) {
            this.revokedAt = revokedAt;
            return this;
        }

        public final String getRevocationReason() {
            return revocationReason;
        }

        public final void setRevocationReason(String revocationReason) {
            this.revocationReason = revocationReason;
        }

        @Override
        public final Builder revocationReason(String revocationReason) {
            this.revocationReason = revocationReason;
            return this;
        }

        @Override
        public final Builder revocationReason(RevocationReason revocationReason) {
            this.revocationReason(revocationReason == null ? null : revocationReason.toString());
            return this;
        }

        public final Instant getNotBefore() {
            return notBefore;
        }

        public final void setNotBefore(Instant notBefore) {
            this.notBefore = notBefore;
        }

        @Override
        public final Builder notBefore(Instant notBefore) {
            this.notBefore = notBefore;
            return this;
        }

        public final Instant getNotAfter() {
            return notAfter;
        }

        public final void setNotAfter(Instant notAfter) {
            this.notAfter = notAfter;
        }

        @Override
        public final Builder notAfter(Instant notAfter) {
            this.notAfter = notAfter;
            return this;
        }

        public final String getKeyAlgorithm() {
            return keyAlgorithm;
        }

        public final void setKeyAlgorithm(String keyAlgorithm) {
            this.keyAlgorithm = keyAlgorithm;
        }

        @Override
        public final Builder keyAlgorithm(String keyAlgorithm) {
            this.keyAlgorithm = keyAlgorithm;
            return this;
        }

        @Override
        public final Builder keyAlgorithm(KeyAlgorithm keyAlgorithm) {
            this.keyAlgorithm(keyAlgorithm == null ? null : keyAlgorithm.toString());
            return this;
        }

        public final String getSignatureAlgorithm() {
            return signatureAlgorithm;
        }

        public final void setSignatureAlgorithm(String signatureAlgorithm) {
            this.signatureAlgorithm = signatureAlgorithm;
        }

        @Override
        public final Builder signatureAlgorithm(String signatureAlgorithm) {
            this.signatureAlgorithm = signatureAlgorithm;
            return this;
        }

        public final Collection<String> getInUseBy() {
            if (inUseBy instanceof SdkAutoConstructList) {
                return null;
            }
            return inUseBy;
        }

        public final void setInUseBy(Collection<String> inUseBy) {
            this.inUseBy = InUseListCopier.copy(inUseBy);
        }

        @Override
        public final Builder inUseBy(Collection<String> inUseBy) {
            this.inUseBy = InUseListCopier.copy(inUseBy);
            return this;
        }

        @Override
        @SafeVarargs
        public final Builder inUseBy(String... inUseBy) {
            inUseBy(Arrays.asList(inUseBy));
            return this;
        }

        public final String getFailureReason() {
            return failureReason;
        }

        public final void setFailureReason(String failureReason) {
            this.failureReason = failureReason;
        }

        @Override
        public final Builder failureReason(String failureReason) {
            this.failureReason = failureReason;
            return this;
        }

        @Override
        public final Builder failureReason(FailureReason failureReason) {
            this.failureReason(failureReason == null ? null : failureReason.toString());
            return this;
        }

        public final String getType() {
            return type;
        }

        public final void setType(String type) {
            this.type = type;
        }

        @Override
        public final Builder type(String type) {
            this.type = type;
            return this;
        }

        @Override
        public final Builder type(CertificateType type) {
            this.type(type == null ? null : type.toString());
            return this;
        }

        public final RenewalSummary.Builder getRenewalSummary() {
            return renewalSummary != null ? renewalSummary.toBuilder() : null;
        }

        public final void setRenewalSummary(RenewalSummary.BuilderImpl renewalSummary) {
            this.renewalSummary = renewalSummary != null ? renewalSummary.build() : null;
        }

        @Override
        public final Builder renewalSummary(RenewalSummary renewalSummary) {
            this.renewalSummary = renewalSummary;
            return this;
        }

        public final List<KeyUsage.Builder> getKeyUsages() {
            List<KeyUsage.Builder> result = KeyUsageListCopier.copyToBuilder(this.keyUsages);
            if (result instanceof SdkAutoConstructList) {
                return null;
            }
            return result;
        }

        public final void setKeyUsages(Collection<KeyUsage.BuilderImpl> keyUsages) {
            this.keyUsages = KeyUsageListCopier.copyFromBuilder(keyUsages);
        }

        @Override
        public final Builder keyUsages(Collection<KeyUsage> keyUsages) {
            this.keyUsages = KeyUsageListCopier.copy(keyUsages);
            return this;
        }

        @Override
        @SafeVarargs
        public final Builder keyUsages(KeyUsage... keyUsages) {
            keyUsages(Arrays.asList(keyUsages));
            return this;
        }

        @Override
        @SafeVarargs
        public final Builder keyUsages(Consumer<KeyUsage.Builder>... keyUsages) {
            keyUsages(Stream.of(keyUsages).map(c -> KeyUsage.builder().applyMutation(c).build()).collect(Collectors.toList()));
            return this;
        }

        public final List<ExtendedKeyUsage.Builder> getExtendedKeyUsages() {
            List<ExtendedKeyUsage.Builder> result = ExtendedKeyUsageListCopier.copyToBuilder(this.extendedKeyUsages);
            if (result instanceof SdkAutoConstructList) {
                return null;
            }
            return result;
        }

        public final void setExtendedKeyUsages(Collection<ExtendedKeyUsage.BuilderImpl> extendedKeyUsages) {
            this.extendedKeyUsages = ExtendedKeyUsageListCopier.copyFromBuilder(extendedKeyUsages);
        }

        @Override
        public final Builder extendedKeyUsages(Collection<ExtendedKeyUsage> extendedKeyUsages) {
            this.extendedKeyUsages = ExtendedKeyUsageListCopier.copy(extendedKeyUsages);
            return this;
        }

        @Override
        @SafeVarargs
        public final Builder extendedKeyUsages(ExtendedKeyUsage... extendedKeyUsages) {
            extendedKeyUsages(Arrays.asList(extendedKeyUsages));
            return this;
        }

        @Override
        @SafeVarargs
        public final Builder extendedKeyUsages(Consumer<ExtendedKeyUsage.Builder>... extendedKeyUsages) {
            extendedKeyUsages(Stream.of(extendedKeyUsages).map(c -> ExtendedKeyUsage.builder().applyMutation(c).build())
                    .collect(Collectors.toList()));
            return this;
        }

        public final String getCertificateAuthorityArn() {
            return certificateAuthorityArn;
        }

        public final void setCertificateAuthorityArn(String certificateAuthorityArn) {
            this.certificateAuthorityArn = certificateAuthorityArn;
        }

        @Override
        public final Builder certificateAuthorityArn(String certificateAuthorityArn) {
            this.certificateAuthorityArn = certificateAuthorityArn;
            return this;
        }

        public final String getRenewalEligibility() {
            return renewalEligibility;
        }

        public final void setRenewalEligibility(String renewalEligibility) {
            this.renewalEligibility = renewalEligibility;
        }

        @Override
        public final Builder renewalEligibility(String renewalEligibility) {
            this.renewalEligibility = renewalEligibility;
            return this;
        }

        @Override
        public final Builder renewalEligibility(RenewalEligibility renewalEligibility) {
            this.renewalEligibility(renewalEligibility == null ? null : renewalEligibility.toString());
            return this;
        }

        public final CertificateOptions.Builder getOptions() {
            return options != null ? options.toBuilder() : null;
        }

        public final void setOptions(CertificateOptions.BuilderImpl options) {
            this.options = options != null ? options.build() : null;
        }

        @Override
        public final Builder options(CertificateOptions options) {
            this.options = options;
            return this;
        }

        @Override
        public CertificateDetail build() {
            return new CertificateDetail(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}
