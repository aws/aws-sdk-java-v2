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
 * This structure is returned in the response object of <a>ListCertificates</a> action.
 * </p>
 */
@Generated("software.amazon.awssdk:codegen")
public final class CertificateSummary implements SdkPojo, Serializable,
        ToCopyableBuilder<CertificateSummary.Builder, CertificateSummary> {
    private static final SdkField<String> CERTIFICATE_ARN_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("CertificateArn").getter(getter(CertificateSummary::certificateArn))
            .setter(setter(Builder::certificateArn))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("CertificateArn").build()).build();

    private static final SdkField<String> DOMAIN_NAME_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("DomainName").getter(getter(CertificateSummary::domainName)).setter(setter(Builder::domainName))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("DomainName").build()).build();

    private static final SdkField<List<String>> SUBJECT_ALTERNATIVE_NAME_SUMMARIES_FIELD = SdkField
            .<List<String>> builder(MarshallingType.LIST)
            .memberName("SubjectAlternativeNameSummaries")
            .getter(getter(CertificateSummary::subjectAlternativeNameSummaries))
            .setter(setter(Builder::subjectAlternativeNameSummaries))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("SubjectAlternativeNameSummaries")
                    .build(),
                    ListTrait
                            .builder()
                            .memberLocationName(null)
                            .memberFieldInfo(
                                    SdkField.<String> builder(MarshallingType.STRING)
                                            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD)
                                                    .locationName("member").build()).build()).build()).build();

    private static final SdkField<Boolean> HAS_ADDITIONAL_SUBJECT_ALTERNATIVE_NAMES_FIELD = SdkField
            .<Boolean> builder(MarshallingType.BOOLEAN)
            .memberName("HasAdditionalSubjectAlternativeNames")
            .getter(getter(CertificateSummary::hasAdditionalSubjectAlternativeNames))
            .setter(setter(Builder::hasAdditionalSubjectAlternativeNames))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD)
                    .locationName("HasAdditionalSubjectAlternativeNames").build()).build();

    private static final SdkField<String> STATUS_FIELD = SdkField.<String> builder(MarshallingType.STRING).memberName("Status")
            .getter(getter(CertificateSummary::statusAsString)).setter(setter(Builder::status))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("Status").build()).build();

    private static final SdkField<String> TYPE_FIELD = SdkField.<String> builder(MarshallingType.STRING).memberName("Type")
            .getter(getter(CertificateSummary::typeAsString)).setter(setter(Builder::type))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("Type").build()).build();

    private static final SdkField<String> KEY_ALGORITHM_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("KeyAlgorithm").getter(getter(CertificateSummary::keyAlgorithmAsString))
            .setter(setter(Builder::keyAlgorithm))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("KeyAlgorithm").build()).build();

    private static final SdkField<List<String>> KEY_USAGES_FIELD = SdkField
            .<List<String>> builder(MarshallingType.LIST)
            .memberName("KeyUsages")
            .getter(getter(CertificateSummary::keyUsagesAsStrings))
            .setter(setter(Builder::keyUsagesWithStrings))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("KeyUsages").build(),
                    ListTrait
                            .builder()
                            .memberLocationName(null)
                            .memberFieldInfo(
                                    SdkField.<String> builder(MarshallingType.STRING)
                                            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD)
                                                    .locationName("member").build()).build()).build()).build();

    private static final SdkField<List<String>> EXTENDED_KEY_USAGES_FIELD = SdkField
            .<List<String>> builder(MarshallingType.LIST)
            .memberName("ExtendedKeyUsages")
            .getter(getter(CertificateSummary::extendedKeyUsagesAsStrings))
            .setter(setter(Builder::extendedKeyUsagesWithStrings))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("ExtendedKeyUsages").build(),
                    ListTrait
                            .builder()
                            .memberLocationName(null)
                            .memberFieldInfo(
                                    SdkField.<String> builder(MarshallingType.STRING)
                                            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD)
                                                    .locationName("member").build()).build()).build()).build();

    private static final SdkField<Boolean> IN_USE_FIELD = SdkField.<Boolean> builder(MarshallingType.BOOLEAN).memberName("InUse")
            .getter(getter(CertificateSummary::inUse)).setter(setter(Builder::inUse))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("InUse").build()).build();

    private static final SdkField<Boolean> EXPORTED_FIELD = SdkField.<Boolean> builder(MarshallingType.BOOLEAN)
            .memberName("Exported").getter(getter(CertificateSummary::exported)).setter(setter(Builder::exported))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("Exported").build()).build();

    private static final SdkField<String> RENEWAL_ELIGIBILITY_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("RenewalEligibility").getter(getter(CertificateSummary::renewalEligibilityAsString))
            .setter(setter(Builder::renewalEligibility))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("RenewalEligibility").build())
            .build();

    private static final SdkField<Instant> NOT_BEFORE_FIELD = SdkField.<Instant> builder(MarshallingType.INSTANT)
            .memberName("NotBefore").getter(getter(CertificateSummary::notBefore)).setter(setter(Builder::notBefore))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("NotBefore").build()).build();

    private static final SdkField<Instant> NOT_AFTER_FIELD = SdkField.<Instant> builder(MarshallingType.INSTANT)
            .memberName("NotAfter").getter(getter(CertificateSummary::notAfter)).setter(setter(Builder::notAfter))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("NotAfter").build()).build();

    private static final SdkField<Instant> CREATED_AT_FIELD = SdkField.<Instant> builder(MarshallingType.INSTANT)
            .memberName("CreatedAt").getter(getter(CertificateSummary::createdAt)).setter(setter(Builder::createdAt))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("CreatedAt").build()).build();

    private static final SdkField<Instant> ISSUED_AT_FIELD = SdkField.<Instant> builder(MarshallingType.INSTANT)
            .memberName("IssuedAt").getter(getter(CertificateSummary::issuedAt)).setter(setter(Builder::issuedAt))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("IssuedAt").build()).build();

    private static final SdkField<Instant> IMPORTED_AT_FIELD = SdkField.<Instant> builder(MarshallingType.INSTANT)
            .memberName("ImportedAt").getter(getter(CertificateSummary::importedAt)).setter(setter(Builder::importedAt))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("ImportedAt").build()).build();

    private static final SdkField<Instant> REVOKED_AT_FIELD = SdkField.<Instant> builder(MarshallingType.INSTANT)
            .memberName("RevokedAt").getter(getter(CertificateSummary::revokedAt)).setter(setter(Builder::revokedAt))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("RevokedAt").build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(CERTIFICATE_ARN_FIELD,
            DOMAIN_NAME_FIELD, SUBJECT_ALTERNATIVE_NAME_SUMMARIES_FIELD, HAS_ADDITIONAL_SUBJECT_ALTERNATIVE_NAMES_FIELD,
            STATUS_FIELD, TYPE_FIELD, KEY_ALGORITHM_FIELD, KEY_USAGES_FIELD, EXTENDED_KEY_USAGES_FIELD, IN_USE_FIELD,
            EXPORTED_FIELD, RENEWAL_ELIGIBILITY_FIELD, NOT_BEFORE_FIELD, NOT_AFTER_FIELD, CREATED_AT_FIELD, ISSUED_AT_FIELD,
            IMPORTED_AT_FIELD, REVOKED_AT_FIELD));

    private static final long serialVersionUID = 1L;

    private final String certificateArn;

    private final String domainName;

    private final List<String> subjectAlternativeNameSummaries;

    private final Boolean hasAdditionalSubjectAlternativeNames;

    private final String status;

    private final String type;

    private final String keyAlgorithm;

    private final List<String> keyUsages;

    private final List<String> extendedKeyUsages;

    private final Boolean inUse;

    private final Boolean exported;

    private final String renewalEligibility;

    private final Instant notBefore;

    private final Instant notAfter;

    private final Instant createdAt;

    private final Instant issuedAt;

    private final Instant importedAt;

    private final Instant revokedAt;

    private CertificateSummary(BuilderImpl builder) {
        this.certificateArn = builder.certificateArn;
        this.domainName = builder.domainName;
        this.subjectAlternativeNameSummaries = builder.subjectAlternativeNameSummaries;
        this.hasAdditionalSubjectAlternativeNames = builder.hasAdditionalSubjectAlternativeNames;
        this.status = builder.status;
        this.type = builder.type;
        this.keyAlgorithm = builder.keyAlgorithm;
        this.keyUsages = builder.keyUsages;
        this.extendedKeyUsages = builder.extendedKeyUsages;
        this.inUse = builder.inUse;
        this.exported = builder.exported;
        this.renewalEligibility = builder.renewalEligibility;
        this.notBefore = builder.notBefore;
        this.notAfter = builder.notAfter;
        this.createdAt = builder.createdAt;
        this.issuedAt = builder.issuedAt;
        this.importedAt = builder.importedAt;
        this.revokedAt = builder.revokedAt;
    }

    /**
     * <p>
     * Amazon Resource Name (ARN) of the certificate. This is of the form:
     * </p>
     * <p>
     * <code>arn:aws:acm:region:123456789012:certificate/12345678-1234-1234-1234-123456789012</code>
     * </p>
     * <p>
     * For more information about ARNs, see <a
     * href="https://docs.aws.amazon.com/general/latest/gr/aws-arns-and-namespaces.html">Amazon Resource Names
     * (ARNs)</a>.
     * </p>
     * 
     * @return Amazon Resource Name (ARN) of the certificate. This is of the form:</p>
     *         <p>
     *         <code>arn:aws:acm:region:123456789012:certificate/12345678-1234-1234-1234-123456789012</code>
     *         </p>
     *         <p>
     *         For more information about ARNs, see <a
     *         href="https://docs.aws.amazon.com/general/latest/gr/aws-arns-and-namespaces.html">Amazon Resource Names
     *         (ARNs)</a>.
     */
    public final String certificateArn() {
        return certificateArn;
    }

    /**
     * <p>
     * Fully qualified domain name (FQDN), such as www.example.com or example.com, for the certificate.
     * </p>
     * 
     * @return Fully qualified domain name (FQDN), such as www.example.com or example.com, for the certificate.
     */
    public final String domainName() {
        return domainName;
    }

    /**
     * For responses, this returns true if the service returned a value for the SubjectAlternativeNameSummaries
     * property. This DOES NOT check that the value is non-empty (for which, you should check the {@code isEmpty()}
     * method on the property). This is useful because the SDK will never return a null collection or map, but you may
     * need to differentiate between the service returning nothing (or null) and the service returning an empty
     * collection or map. For requests, this returns true if a value for the property was specified in the request
     * builder, and false if a value was not specified.
     */
    public final boolean hasSubjectAlternativeNameSummaries() {
        return subjectAlternativeNameSummaries != null && !(subjectAlternativeNameSummaries instanceof SdkAutoConstructList);
    }

    /**
     * <p>
     * One or more domain names (subject alternative names) included in the certificate. This list contains the domain
     * names that are bound to the public key that is contained in the certificate. The subject alternative names
     * include the canonical domain name (CN) of the certificate and additional domain names that can be used to connect
     * to the website.
     * </p>
     * <p>
     * When called by <a
     * href="https://docs.aws.amazon.com/acm/latestAPIReference/API_ListCertificates.html">ListCertificates</a>, this
     * parameter will only return the first 100 subject alternative names included in the certificate. To display the
     * full list of subject alternative names, use <a
     * href="https://docs.aws.amazon.com/acm/latestAPIReference/API_DescribeCertificate.html">DescribeCertificate</a>.
     * </p>
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     * <p>
     * This method will never return null. If you would like to know whether the service returned this field (so that
     * you can differentiate between null and empty), you can use the {@link #hasSubjectAlternativeNameSummaries}
     * method.
     * </p>
     * 
     * @return One or more domain names (subject alternative names) included in the certificate. This list contains the
     *         domain names that are bound to the public key that is contained in the certificate. The subject
     *         alternative names include the canonical domain name (CN) of the certificate and additional domain names
     *         that can be used to connect to the website. </p>
     *         <p>
     *         When called by <a
     *         href="https://docs.aws.amazon.com/acm/latestAPIReference/API_ListCertificates.html">ListCertificates</a>,
     *         this parameter will only return the first 100 subject alternative names included in the certificate. To
     *         display the full list of subject alternative names, use <a
     *         href="https://docs.aws.amazon.com/acm/latestAPIReference/API_DescribeCertificate.html"
     *         >DescribeCertificate</a>.
     */
    public final List<String> subjectAlternativeNameSummaries() {
        return subjectAlternativeNameSummaries;
    }

    /**
     * <p>
     * When called by <a
     * href="https://docs.aws.amazon.com/acm/latestAPIReference/API_ListCertificates.html">ListCertificates</a>,
     * indicates whether the full list of subject alternative names has been included in the response. If false, the
     * response includes all of the subject alternative names included in the certificate. If true, the response only
     * includes the first 100 subject alternative names included in the certificate. To display the full list of subject
     * alternative names, use <a
     * href="https://docs.aws.amazon.com/acm/latestAPIReference/API_DescribeCertificate.html">DescribeCertificate</a>.
     * </p>
     * 
     * @return When called by <a
     *         href="https://docs.aws.amazon.com/acm/latestAPIReference/API_ListCertificates.html">ListCertificates</a>,
     *         indicates whether the full list of subject alternative names has been included in the response. If false,
     *         the response includes all of the subject alternative names included in the certificate. If true, the
     *         response only includes the first 100 subject alternative names included in the certificate. To display
     *         the full list of subject alternative names, use <a
     *         href="https://docs.aws.amazon.com/acm/latestAPIReference/API_DescribeCertificate.html"
     *         >DescribeCertificate</a>.
     */
    public final Boolean hasAdditionalSubjectAlternativeNames() {
        return hasAdditionalSubjectAlternativeNames;
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
    public final List<KeyUsageName> keyUsages() {
        return KeyUsageNamesCopier.copyStringToEnum(keyUsages);
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
    public final List<String> keyUsagesAsStrings() {
        return keyUsages;
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
    public final List<ExtendedKeyUsageName> extendedKeyUsages() {
        return ExtendedKeyUsageNamesCopier.copyStringToEnum(extendedKeyUsages);
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
    public final List<String> extendedKeyUsagesAsStrings() {
        return extendedKeyUsages;
    }

    /**
     * <p>
     * Indicates whether the certificate is currently in use by any Amazon Web Services resources.
     * </p>
     * 
     * @return Indicates whether the certificate is currently in use by any Amazon Web Services resources.
     */
    public final Boolean inUse() {
        return inUse;
    }

    /**
     * <p>
     * Indicates whether the certificate has been exported. This value exists only when the certificate type is
     * <code>PRIVATE</code>.
     * </p>
     * 
     * @return Indicates whether the certificate has been exported. This value exists only when the certificate type is
     *         <code>PRIVATE</code>.
     */
    public final Boolean exported() {
        return exported;
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
        hashCode = 31 * hashCode
                + Objects.hashCode(hasSubjectAlternativeNameSummaries() ? subjectAlternativeNameSummaries() : null);
        hashCode = 31 * hashCode + Objects.hashCode(hasAdditionalSubjectAlternativeNames());
        hashCode = 31 * hashCode + Objects.hashCode(statusAsString());
        hashCode = 31 * hashCode + Objects.hashCode(typeAsString());
        hashCode = 31 * hashCode + Objects.hashCode(keyAlgorithmAsString());
        hashCode = 31 * hashCode + Objects.hashCode(hasKeyUsages() ? keyUsagesAsStrings() : null);
        hashCode = 31 * hashCode + Objects.hashCode(hasExtendedKeyUsages() ? extendedKeyUsagesAsStrings() : null);
        hashCode = 31 * hashCode + Objects.hashCode(inUse());
        hashCode = 31 * hashCode + Objects.hashCode(exported());
        hashCode = 31 * hashCode + Objects.hashCode(renewalEligibilityAsString());
        hashCode = 31 * hashCode + Objects.hashCode(notBefore());
        hashCode = 31 * hashCode + Objects.hashCode(notAfter());
        hashCode = 31 * hashCode + Objects.hashCode(createdAt());
        hashCode = 31 * hashCode + Objects.hashCode(issuedAt());
        hashCode = 31 * hashCode + Objects.hashCode(importedAt());
        hashCode = 31 * hashCode + Objects.hashCode(revokedAt());
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
        if (!(obj instanceof CertificateSummary)) {
            return false;
        }
        CertificateSummary other = (CertificateSummary) obj;
        return Objects.equals(certificateArn(), other.certificateArn()) && Objects.equals(domainName(), other.domainName())
                && hasSubjectAlternativeNameSummaries() == other.hasSubjectAlternativeNameSummaries()
                && Objects.equals(subjectAlternativeNameSummaries(), other.subjectAlternativeNameSummaries())
                && Objects.equals(hasAdditionalSubjectAlternativeNames(), other.hasAdditionalSubjectAlternativeNames())
                && Objects.equals(statusAsString(), other.statusAsString())
                && Objects.equals(typeAsString(), other.typeAsString())
                && Objects.equals(keyAlgorithmAsString(), other.keyAlgorithmAsString()) && hasKeyUsages() == other.hasKeyUsages()
                && Objects.equals(keyUsagesAsStrings(), other.keyUsagesAsStrings())
                && hasExtendedKeyUsages() == other.hasExtendedKeyUsages()
                && Objects.equals(extendedKeyUsagesAsStrings(), other.extendedKeyUsagesAsStrings())
                && Objects.equals(inUse(), other.inUse()) && Objects.equals(exported(), other.exported())
                && Objects.equals(renewalEligibilityAsString(), other.renewalEligibilityAsString())
                && Objects.equals(notBefore(), other.notBefore()) && Objects.equals(notAfter(), other.notAfter())
                && Objects.equals(createdAt(), other.createdAt()) && Objects.equals(issuedAt(), other.issuedAt())
                && Objects.equals(importedAt(), other.importedAt()) && Objects.equals(revokedAt(), other.revokedAt());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public final String toString() {
        return ToString
                .builder("CertificateSummary")
                .add("CertificateArn", certificateArn())
                .add("DomainName", domainName())
                .add("SubjectAlternativeNameSummaries",
                        hasSubjectAlternativeNameSummaries() ? subjectAlternativeNameSummaries() : null)
                .add("HasAdditionalSubjectAlternativeNames", hasAdditionalSubjectAlternativeNames())
                .add("Status", statusAsString()).add("Type", typeAsString()).add("KeyAlgorithm", keyAlgorithmAsString())
                .add("KeyUsages", hasKeyUsages() ? keyUsagesAsStrings() : null)
                .add("ExtendedKeyUsages", hasExtendedKeyUsages() ? extendedKeyUsagesAsStrings() : null).add("InUse", inUse())
                .add("Exported", exported()).add("RenewalEligibility", renewalEligibilityAsString())
                .add("NotBefore", notBefore()).add("NotAfter", notAfter()).add("CreatedAt", createdAt())
                .add("IssuedAt", issuedAt()).add("ImportedAt", importedAt()).add("RevokedAt", revokedAt()).build();
    }

    public final <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
        case "CertificateArn":
            return Optional.ofNullable(clazz.cast(certificateArn()));
        case "DomainName":
            return Optional.ofNullable(clazz.cast(domainName()));
        case "SubjectAlternativeNameSummaries":
            return Optional.ofNullable(clazz.cast(subjectAlternativeNameSummaries()));
        case "HasAdditionalSubjectAlternativeNames":
            return Optional.ofNullable(clazz.cast(hasAdditionalSubjectAlternativeNames()));
        case "Status":
            return Optional.ofNullable(clazz.cast(statusAsString()));
        case "Type":
            return Optional.ofNullable(clazz.cast(typeAsString()));
        case "KeyAlgorithm":
            return Optional.ofNullable(clazz.cast(keyAlgorithmAsString()));
        case "KeyUsages":
            return Optional.ofNullable(clazz.cast(keyUsagesAsStrings()));
        case "ExtendedKeyUsages":
            return Optional.ofNullable(clazz.cast(extendedKeyUsagesAsStrings()));
        case "InUse":
            return Optional.ofNullable(clazz.cast(inUse()));
        case "Exported":
            return Optional.ofNullable(clazz.cast(exported()));
        case "RenewalEligibility":
            return Optional.ofNullable(clazz.cast(renewalEligibilityAsString()));
        case "NotBefore":
            return Optional.ofNullable(clazz.cast(notBefore()));
        case "NotAfter":
            return Optional.ofNullable(clazz.cast(notAfter()));
        case "CreatedAt":
            return Optional.ofNullable(clazz.cast(createdAt()));
        case "IssuedAt":
            return Optional.ofNullable(clazz.cast(issuedAt()));
        case "ImportedAt":
            return Optional.ofNullable(clazz.cast(importedAt()));
        case "RevokedAt":
            return Optional.ofNullable(clazz.cast(revokedAt()));
        default:
            return Optional.empty();
        }
    }

    @Override
    public final List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    private static <T> Function<Object, T> getter(Function<CertificateSummary, T> g) {
        return obj -> g.apply((CertificateSummary) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    public interface Builder extends SdkPojo, CopyableBuilder<Builder, CertificateSummary> {
        /**
         * <p>
         * Amazon Resource Name (ARN) of the certificate. This is of the form:
         * </p>
         * <p>
         * <code>arn:aws:acm:region:123456789012:certificate/12345678-1234-1234-1234-123456789012</code>
         * </p>
         * <p>
         * For more information about ARNs, see <a
         * href="https://docs.aws.amazon.com/general/latest/gr/aws-arns-and-namespaces.html">Amazon Resource Names
         * (ARNs)</a>.
         * </p>
         * 
         * @param certificateArn
         *        Amazon Resource Name (ARN) of the certificate. This is of the form:</p>
         *        <p>
         *        <code>arn:aws:acm:region:123456789012:certificate/12345678-1234-1234-1234-123456789012</code>
         *        </p>
         *        <p>
         *        For more information about ARNs, see <a
         *        href="https://docs.aws.amazon.com/general/latest/gr/aws-arns-and-namespaces.html">Amazon Resource
         *        Names (ARNs)</a>.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder certificateArn(String certificateArn);

        /**
         * <p>
         * Fully qualified domain name (FQDN), such as www.example.com or example.com, for the certificate.
         * </p>
         * 
         * @param domainName
         *        Fully qualified domain name (FQDN), such as www.example.com or example.com, for the certificate.
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
         * <p>
         * When called by <a
         * href="https://docs.aws.amazon.com/acm/latestAPIReference/API_ListCertificates.html">ListCertificates</a>,
         * this parameter will only return the first 100 subject alternative names included in the certificate. To
         * display the full list of subject alternative names, use <a
         * href="https://docs.aws.amazon.com/acm/latestAPIReference/API_DescribeCertificate.html"
         * >DescribeCertificate</a>.
         * </p>
         * 
         * @param subjectAlternativeNameSummaries
         *        One or more domain names (subject alternative names) included in the certificate. This list contains
         *        the domain names that are bound to the public key that is contained in the certificate. The subject
         *        alternative names include the canonical domain name (CN) of the certificate and additional domain
         *        names that can be used to connect to the website. </p>
         *        <p>
         *        When called by <a
         *        href="https://docs.aws.amazon.com/acm/latestAPIReference/API_ListCertificates.html">ListCertificates
         *        </a>, this parameter will only return the first 100 subject alternative names included in the
         *        certificate. To display the full list of subject alternative names, use <a
         *        href="https://docs.aws.amazon.com/acm/latestAPIReference/API_DescribeCertificate.html"
         *        >DescribeCertificate</a>.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder subjectAlternativeNameSummaries(Collection<String> subjectAlternativeNameSummaries);

        /**
         * <p>
         * One or more domain names (subject alternative names) included in the certificate. This list contains the
         * domain names that are bound to the public key that is contained in the certificate. The subject alternative
         * names include the canonical domain name (CN) of the certificate and additional domain names that can be used
         * to connect to the website.
         * </p>
         * <p>
         * When called by <a
         * href="https://docs.aws.amazon.com/acm/latestAPIReference/API_ListCertificates.html">ListCertificates</a>,
         * this parameter will only return the first 100 subject alternative names included in the certificate. To
         * display the full list of subject alternative names, use <a
         * href="https://docs.aws.amazon.com/acm/latestAPIReference/API_DescribeCertificate.html"
         * >DescribeCertificate</a>.
         * </p>
         * 
         * @param subjectAlternativeNameSummaries
         *        One or more domain names (subject alternative names) included in the certificate. This list contains
         *        the domain names that are bound to the public key that is contained in the certificate. The subject
         *        alternative names include the canonical domain name (CN) of the certificate and additional domain
         *        names that can be used to connect to the website. </p>
         *        <p>
         *        When called by <a
         *        href="https://docs.aws.amazon.com/acm/latestAPIReference/API_ListCertificates.html">ListCertificates
         *        </a>, this parameter will only return the first 100 subject alternative names included in the
         *        certificate. To display the full list of subject alternative names, use <a
         *        href="https://docs.aws.amazon.com/acm/latestAPIReference/API_DescribeCertificate.html"
         *        >DescribeCertificate</a>.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder subjectAlternativeNameSummaries(String... subjectAlternativeNameSummaries);

        /**
         * <p>
         * When called by <a
         * href="https://docs.aws.amazon.com/acm/latestAPIReference/API_ListCertificates.html">ListCertificates</a>,
         * indicates whether the full list of subject alternative names has been included in the response. If false, the
         * response includes all of the subject alternative names included in the certificate. If true, the response
         * only includes the first 100 subject alternative names included in the certificate. To display the full list
         * of subject alternative names, use <a
         * href="https://docs.aws.amazon.com/acm/latestAPIReference/API_DescribeCertificate.html"
         * >DescribeCertificate</a>.
         * </p>
         * 
         * @param hasAdditionalSubjectAlternativeNames
         *        When called by <a
         *        href="https://docs.aws.amazon.com/acm/latestAPIReference/API_ListCertificates.html">ListCertificates
         *        </a>, indicates whether the full list of subject alternative names has been included in the response.
         *        If false, the response includes all of the subject alternative names included in the certificate. If
         *        true, the response only includes the first 100 subject alternative names included in the certificate.
         *        To display the full list of subject alternative names, use <a
         *        href="https://docs.aws.amazon.com/acm/latestAPIReference/API_DescribeCertificate.html"
         *        >DescribeCertificate</a>.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder hasAdditionalSubjectAlternativeNames(Boolean hasAdditionalSubjectAlternativeNames);

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
        Builder keyUsagesWithStrings(Collection<String> keyUsages);

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
        Builder keyUsagesWithStrings(String... keyUsages);

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
        Builder keyUsages(Collection<KeyUsageName> keyUsages);

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
        Builder keyUsages(KeyUsageName... keyUsages);

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
        Builder extendedKeyUsagesWithStrings(Collection<String> extendedKeyUsages);

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
        Builder extendedKeyUsagesWithStrings(String... extendedKeyUsages);

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
        Builder extendedKeyUsages(Collection<ExtendedKeyUsageName> extendedKeyUsages);

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
        Builder extendedKeyUsages(ExtendedKeyUsageName... extendedKeyUsages);

        /**
         * <p>
         * Indicates whether the certificate is currently in use by any Amazon Web Services resources.
         * </p>
         * 
         * @param inUse
         *        Indicates whether the certificate is currently in use by any Amazon Web Services resources.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder inUse(Boolean inUse);

        /**
         * <p>
         * Indicates whether the certificate has been exported. This value exists only when the certificate type is
         * <code>PRIVATE</code>.
         * </p>
         * 
         * @param exported
         *        Indicates whether the certificate has been exported. This value exists only when the certificate type
         *        is <code>PRIVATE</code>.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder exported(Boolean exported);

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
    }

    static final class BuilderImpl implements Builder {
        private String certificateArn;

        private String domainName;

        private List<String> subjectAlternativeNameSummaries = DefaultSdkAutoConstructList.getInstance();

        private Boolean hasAdditionalSubjectAlternativeNames;

        private String status;

        private String type;

        private String keyAlgorithm;

        private List<String> keyUsages = DefaultSdkAutoConstructList.getInstance();

        private List<String> extendedKeyUsages = DefaultSdkAutoConstructList.getInstance();

        private Boolean inUse;

        private Boolean exported;

        private String renewalEligibility;

        private Instant notBefore;

        private Instant notAfter;

        private Instant createdAt;

        private Instant issuedAt;

        private Instant importedAt;

        private Instant revokedAt;

        private BuilderImpl() {
        }

        private BuilderImpl(CertificateSummary model) {
            certificateArn(model.certificateArn);
            domainName(model.domainName);
            subjectAlternativeNameSummaries(model.subjectAlternativeNameSummaries);
            hasAdditionalSubjectAlternativeNames(model.hasAdditionalSubjectAlternativeNames);
            status(model.status);
            type(model.type);
            keyAlgorithm(model.keyAlgorithm);
            keyUsagesWithStrings(model.keyUsages);
            extendedKeyUsagesWithStrings(model.extendedKeyUsages);
            inUse(model.inUse);
            exported(model.exported);
            renewalEligibility(model.renewalEligibility);
            notBefore(model.notBefore);
            notAfter(model.notAfter);
            createdAt(model.createdAt);
            issuedAt(model.issuedAt);
            importedAt(model.importedAt);
            revokedAt(model.revokedAt);
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

        public final Collection<String> getSubjectAlternativeNameSummaries() {
            if (subjectAlternativeNameSummaries instanceof SdkAutoConstructList) {
                return null;
            }
            return subjectAlternativeNameSummaries;
        }

        public final void setSubjectAlternativeNameSummaries(Collection<String> subjectAlternativeNameSummaries) {
            this.subjectAlternativeNameSummaries = DomainListCopier.copy(subjectAlternativeNameSummaries);
        }

        @Override
        public final Builder subjectAlternativeNameSummaries(Collection<String> subjectAlternativeNameSummaries) {
            this.subjectAlternativeNameSummaries = DomainListCopier.copy(subjectAlternativeNameSummaries);
            return this;
        }

        @Override
        @SafeVarargs
        public final Builder subjectAlternativeNameSummaries(String... subjectAlternativeNameSummaries) {
            subjectAlternativeNameSummaries(Arrays.asList(subjectAlternativeNameSummaries));
            return this;
        }

        public final Boolean getHasAdditionalSubjectAlternativeNames() {
            return hasAdditionalSubjectAlternativeNames;
        }

        public final void setHasAdditionalSubjectAlternativeNames(Boolean hasAdditionalSubjectAlternativeNames) {
            this.hasAdditionalSubjectAlternativeNames = hasAdditionalSubjectAlternativeNames;
        }

        @Override
        public final Builder hasAdditionalSubjectAlternativeNames(Boolean hasAdditionalSubjectAlternativeNames) {
            this.hasAdditionalSubjectAlternativeNames = hasAdditionalSubjectAlternativeNames;
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

        public final Collection<String> getKeyUsages() {
            if (keyUsages instanceof SdkAutoConstructList) {
                return null;
            }
            return keyUsages;
        }

        public final void setKeyUsages(Collection<String> keyUsages) {
            this.keyUsages = KeyUsageNamesCopier.copy(keyUsages);
        }

        @Override
        public final Builder keyUsagesWithStrings(Collection<String> keyUsages) {
            this.keyUsages = KeyUsageNamesCopier.copy(keyUsages);
            return this;
        }

        @Override
        @SafeVarargs
        public final Builder keyUsagesWithStrings(String... keyUsages) {
            keyUsagesWithStrings(Arrays.asList(keyUsages));
            return this;
        }

        @Override
        public final Builder keyUsages(Collection<KeyUsageName> keyUsages) {
            this.keyUsages = KeyUsageNamesCopier.copyEnumToString(keyUsages);
            return this;
        }

        @Override
        @SafeVarargs
        public final Builder keyUsages(KeyUsageName... keyUsages) {
            keyUsages(Arrays.asList(keyUsages));
            return this;
        }

        public final Collection<String> getExtendedKeyUsages() {
            if (extendedKeyUsages instanceof SdkAutoConstructList) {
                return null;
            }
            return extendedKeyUsages;
        }

        public final void setExtendedKeyUsages(Collection<String> extendedKeyUsages) {
            this.extendedKeyUsages = ExtendedKeyUsageNamesCopier.copy(extendedKeyUsages);
        }

        @Override
        public final Builder extendedKeyUsagesWithStrings(Collection<String> extendedKeyUsages) {
            this.extendedKeyUsages = ExtendedKeyUsageNamesCopier.copy(extendedKeyUsages);
            return this;
        }

        @Override
        @SafeVarargs
        public final Builder extendedKeyUsagesWithStrings(String... extendedKeyUsages) {
            extendedKeyUsagesWithStrings(Arrays.asList(extendedKeyUsages));
            return this;
        }

        @Override
        public final Builder extendedKeyUsages(Collection<ExtendedKeyUsageName> extendedKeyUsages) {
            this.extendedKeyUsages = ExtendedKeyUsageNamesCopier.copyEnumToString(extendedKeyUsages);
            return this;
        }

        @Override
        @SafeVarargs
        public final Builder extendedKeyUsages(ExtendedKeyUsageName... extendedKeyUsages) {
            extendedKeyUsages(Arrays.asList(extendedKeyUsages));
            return this;
        }

        public final Boolean getInUse() {
            return inUse;
        }

        public final void setInUse(Boolean inUse) {
            this.inUse = inUse;
        }

        @Override
        public final Builder inUse(Boolean inUse) {
            this.inUse = inUse;
            return this;
        }

        public final Boolean getExported() {
            return exported;
        }

        public final void setExported(Boolean exported) {
            this.exported = exported;
        }

        @Override
        public final Builder exported(Boolean exported) {
            this.exported = exported;
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

        @Override
        public CertificateSummary build() {
            return new CertificateSummary(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}
