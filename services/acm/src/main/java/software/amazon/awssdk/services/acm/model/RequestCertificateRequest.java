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
public final class RequestCertificateRequest extends AcmRequest implements
        ToCopyableBuilder<RequestCertificateRequest.Builder, RequestCertificateRequest> {
    private static final SdkField<String> DOMAIN_NAME_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("DomainName").getter(getter(RequestCertificateRequest::domainName)).setter(setter(Builder::domainName))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("DomainName").build()).build();

    private static final SdkField<String> VALIDATION_METHOD_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("ValidationMethod").getter(getter(RequestCertificateRequest::validationMethodAsString))
            .setter(setter(Builder::validationMethod))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("ValidationMethod").build()).build();

    private static final SdkField<List<String>> SUBJECT_ALTERNATIVE_NAMES_FIELD = SdkField
            .<List<String>> builder(MarshallingType.LIST)
            .memberName("SubjectAlternativeNames")
            .getter(getter(RequestCertificateRequest::subjectAlternativeNames))
            .setter(setter(Builder::subjectAlternativeNames))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("SubjectAlternativeNames").build(),
                    ListTrait
                            .builder()
                            .memberLocationName(null)
                            .memberFieldInfo(
                                    SdkField.<String> builder(MarshallingType.STRING)
                                            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD)
                                                    .locationName("member").build()).build()).build()).build();

    private static final SdkField<String> IDEMPOTENCY_TOKEN_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("IdempotencyToken").getter(getter(RequestCertificateRequest::idempotencyToken))
            .setter(setter(Builder::idempotencyToken))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("IdempotencyToken").build()).build();

    private static final SdkField<List<DomainValidationOption>> DOMAIN_VALIDATION_OPTIONS_FIELD = SdkField
            .<List<DomainValidationOption>> builder(MarshallingType.LIST)
            .memberName("DomainValidationOptions")
            .getter(getter(RequestCertificateRequest::domainValidationOptions))
            .setter(setter(Builder::domainValidationOptions))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("DomainValidationOptions").build(),
                    ListTrait
                            .builder()
                            .memberLocationName(null)
                            .memberFieldInfo(
                                    SdkField.<DomainValidationOption> builder(MarshallingType.SDK_POJO)
                                            .constructor(DomainValidationOption::builder)
                                            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD)
                                                    .locationName("member").build()).build()).build()).build();

    private static final SdkField<CertificateOptions> OPTIONS_FIELD = SdkField
            .<CertificateOptions> builder(MarshallingType.SDK_POJO).memberName("Options")
            .getter(getter(RequestCertificateRequest::options)).setter(setter(Builder::options))
            .constructor(CertificateOptions::builder)
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("Options").build()).build();

    private static final SdkField<String> CERTIFICATE_AUTHORITY_ARN_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("CertificateAuthorityArn").getter(getter(RequestCertificateRequest::certificateAuthorityArn))
            .setter(setter(Builder::certificateAuthorityArn))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("CertificateAuthorityArn").build())
            .build();

    private static final SdkField<List<Tag>> TAGS_FIELD = SdkField
            .<List<Tag>> builder(MarshallingType.LIST)
            .memberName("Tags")
            .getter(getter(RequestCertificateRequest::tags))
            .setter(setter(Builder::tags))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("Tags").build(),
                    ListTrait
                            .builder()
                            .memberLocationName(null)
                            .memberFieldInfo(
                                    SdkField.<Tag> builder(MarshallingType.SDK_POJO)
                                            .constructor(Tag::builder)
                                            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD)
                                                    .locationName("member").build()).build()).build()).build();

    private static final SdkField<String> KEY_ALGORITHM_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("KeyAlgorithm").getter(getter(RequestCertificateRequest::keyAlgorithmAsString))
            .setter(setter(Builder::keyAlgorithm))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("KeyAlgorithm").build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(DOMAIN_NAME_FIELD,
            VALIDATION_METHOD_FIELD, SUBJECT_ALTERNATIVE_NAMES_FIELD, IDEMPOTENCY_TOKEN_FIELD, DOMAIN_VALIDATION_OPTIONS_FIELD,
            OPTIONS_FIELD, CERTIFICATE_AUTHORITY_ARN_FIELD, TAGS_FIELD, KEY_ALGORITHM_FIELD));

    private final String domainName;

    private final String validationMethod;

    private final List<String> subjectAlternativeNames;

    private final String idempotencyToken;

    private final List<DomainValidationOption> domainValidationOptions;

    private final CertificateOptions options;

    private final String certificateAuthorityArn;

    private final List<Tag> tags;

    private final String keyAlgorithm;

    private RequestCertificateRequest(BuilderImpl builder) {
        super(builder);
        this.domainName = builder.domainName;
        this.validationMethod = builder.validationMethod;
        this.subjectAlternativeNames = builder.subjectAlternativeNames;
        this.idempotencyToken = builder.idempotencyToken;
        this.domainValidationOptions = builder.domainValidationOptions;
        this.options = builder.options;
        this.certificateAuthorityArn = builder.certificateAuthorityArn;
        this.tags = builder.tags;
        this.keyAlgorithm = builder.keyAlgorithm;
    }

    /**
     * <p>
     * Fully qualified domain name (FQDN), such as www.example.com, that you want to secure with an ACM certificate. Use
     * an asterisk (*) to create a wildcard certificate that protects several sites in the same domain. For example,
     * *.example.com protects www.example.com, site.example.com, and images.example.com.
     * </p>
     * <p>
     * In compliance with <a href="https://datatracker.ietf.org/doc/html/rfc5280">RFC 5280</a>, the length of the domain
     * name (technically, the Common Name) that you provide cannot exceed 64 octets (characters), including periods. To
     * add a longer domain name, specify it in the Subject Alternative Name field, which supports names up to 253 octets
     * in length.
     * </p>
     * 
     * @return Fully qualified domain name (FQDN), such as www.example.com, that you want to secure with an ACM
     *         certificate. Use an asterisk (*) to create a wildcard certificate that protects several sites in the same
     *         domain. For example, *.example.com protects www.example.com, site.example.com, and images.example.com.
     *         </p>
     *         <p>
     *         In compliance with <a href="https://datatracker.ietf.org/doc/html/rfc5280">RFC 5280</a>, the length of
     *         the domain name (technically, the Common Name) that you provide cannot exceed 64 octets (characters),
     *         including periods. To add a longer domain name, specify it in the Subject Alternative Name field, which
     *         supports names up to 253 octets in length.
     */
    public final String domainName() {
        return domainName;
    }

    /**
     * <p>
     * The method you want to use if you are requesting a public certificate to validate that you own or control domain.
     * You can <a href="https://docs.aws.amazon.com/acm/latest/userguide/gs-acm-validate-dns.html">validate with DNS</a>
     * or <a href="https://docs.aws.amazon.com/acm/latest/userguide/gs-acm-validate-email.html">validate with email</a>.
     * We recommend that you use DNS validation.
     * </p>
     * <p>
     * If the service returns an enum value that is not available in the current SDK version, {@link #validationMethod}
     * will return {@link ValidationMethod#UNKNOWN_TO_SDK_VERSION}. The raw value returned by the service is available
     * from {@link #validationMethodAsString}.
     * </p>
     * 
     * @return The method you want to use if you are requesting a public certificate to validate that you own or control
     *         domain. You can <a
     *         href="https://docs.aws.amazon.com/acm/latest/userguide/gs-acm-validate-dns.html">validate with DNS</a> or
     *         <a href="https://docs.aws.amazon.com/acm/latest/userguide/gs-acm-validate-email.html">validate with
     *         email</a>. We recommend that you use DNS validation.
     * @see ValidationMethod
     */
    public final ValidationMethod validationMethod() {
        return ValidationMethod.fromValue(validationMethod);
    }

    /**
     * <p>
     * The method you want to use if you are requesting a public certificate to validate that you own or control domain.
     * You can <a href="https://docs.aws.amazon.com/acm/latest/userguide/gs-acm-validate-dns.html">validate with DNS</a>
     * or <a href="https://docs.aws.amazon.com/acm/latest/userguide/gs-acm-validate-email.html">validate with email</a>.
     * We recommend that you use DNS validation.
     * </p>
     * <p>
     * If the service returns an enum value that is not available in the current SDK version, {@link #validationMethod}
     * will return {@link ValidationMethod#UNKNOWN_TO_SDK_VERSION}. The raw value returned by the service is available
     * from {@link #validationMethodAsString}.
     * </p>
     * 
     * @return The method you want to use if you are requesting a public certificate to validate that you own or control
     *         domain. You can <a
     *         href="https://docs.aws.amazon.com/acm/latest/userguide/gs-acm-validate-dns.html">validate with DNS</a> or
     *         <a href="https://docs.aws.amazon.com/acm/latest/userguide/gs-acm-validate-email.html">validate with
     *         email</a>. We recommend that you use DNS validation.
     * @see ValidationMethod
     */
    public final String validationMethodAsString() {
        return validationMethod;
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
     * Additional FQDNs to be included in the Subject Alternative Name extension of the ACM certificate. For example,
     * add the name www.example.net to a certificate for which the <code>DomainName</code> field is www.example.com if
     * users can reach your site by using either name. The maximum number of domain names that you can add to an ACM
     * certificate is 100. However, the initial quota is 10 domain names. If you need more than 10 names, you must
     * request a quota increase. For more information, see <a
     * href="https://docs.aws.amazon.com/acm/latest/userguide/acm-limits.html">Quotas</a>.
     * </p>
     * <p>
     * The maximum length of a SAN DNS name is 253 octets. The name is made up of multiple labels separated by periods.
     * No label can be longer than 63 octets. Consider the following examples:
     * </p>
     * <ul>
     * <li>
     * <p>
     * <code>(63 octets).(63 octets).(63 octets).(61 octets)</code> is legal because the total length is 253 octets
     * (63+1+63+1+63+1+61) and no label exceeds 63 octets.
     * </p>
     * </li>
     * <li>
     * <p>
     * <code>(64 octets).(63 octets).(63 octets).(61 octets)</code> is not legal because the total length exceeds 253
     * octets (64+1+63+1+63+1+61) and the first label exceeds 63 octets.
     * </p>
     * </li>
     * <li>
     * <p>
     * <code>(63 octets).(63 octets).(63 octets).(62 octets)</code> is not legal because the total length of the DNS
     * name (63+1+63+1+63+1+62) exceeds 253 octets.
     * </p>
     * </li>
     * </ul>
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     * <p>
     * This method will never return null. If you would like to know whether the service returned this field (so that
     * you can differentiate between null and empty), you can use the {@link #hasSubjectAlternativeNames} method.
     * </p>
     * 
     * @return Additional FQDNs to be included in the Subject Alternative Name extension of the ACM certificate. For
     *         example, add the name www.example.net to a certificate for which the <code>DomainName</code> field is
     *         www.example.com if users can reach your site by using either name. The maximum number of domain names
     *         that you can add to an ACM certificate is 100. However, the initial quota is 10 domain names. If you need
     *         more than 10 names, you must request a quota increase. For more information, see <a
     *         href="https://docs.aws.amazon.com/acm/latest/userguide/acm-limits.html">Quotas</a>.</p>
     *         <p>
     *         The maximum length of a SAN DNS name is 253 octets. The name is made up of multiple labels separated by
     *         periods. No label can be longer than 63 octets. Consider the following examples:
     *         </p>
     *         <ul>
     *         <li>
     *         <p>
     *         <code>(63 octets).(63 octets).(63 octets).(61 octets)</code> is legal because the total length is 253
     *         octets (63+1+63+1+63+1+61) and no label exceeds 63 octets.
     *         </p>
     *         </li>
     *         <li>
     *         <p>
     *         <code>(64 octets).(63 octets).(63 octets).(61 octets)</code> is not legal because the total length
     *         exceeds 253 octets (64+1+63+1+63+1+61) and the first label exceeds 63 octets.
     *         </p>
     *         </li>
     *         <li>
     *         <p>
     *         <code>(63 octets).(63 octets).(63 octets).(62 octets)</code> is not legal because the total length of the
     *         DNS name (63+1+63+1+63+1+62) exceeds 253 octets.
     *         </p>
     *         </li>
     */
    public final List<String> subjectAlternativeNames() {
        return subjectAlternativeNames;
    }

    /**
     * <p>
     * Customer chosen string that can be used to distinguish between calls to <code>RequestCertificate</code>.
     * Idempotency tokens time out after one hour. Therefore, if you call <code>RequestCertificate</code> multiple times
     * with the same idempotency token within one hour, ACM recognizes that you are requesting only one certificate and
     * will issue only one. If you change the idempotency token for each call, ACM recognizes that you are requesting
     * multiple certificates.
     * </p>
     * 
     * @return Customer chosen string that can be used to distinguish between calls to <code>RequestCertificate</code>.
     *         Idempotency tokens time out after one hour. Therefore, if you call <code>RequestCertificate</code>
     *         multiple times with the same idempotency token within one hour, ACM recognizes that you are requesting
     *         only one certificate and will issue only one. If you change the idempotency token for each call, ACM
     *         recognizes that you are requesting multiple certificates.
     */
    public final String idempotencyToken() {
        return idempotencyToken;
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
     * The domain name that you want ACM to use to send you emails so that you can validate domain ownership.
     * </p>
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     * <p>
     * This method will never return null. If you would like to know whether the service returned this field (so that
     * you can differentiate between null and empty), you can use the {@link #hasDomainValidationOptions} method.
     * </p>
     * 
     * @return The domain name that you want ACM to use to send you emails so that you can validate domain ownership.
     */
    public final List<DomainValidationOption> domainValidationOptions() {
        return domainValidationOptions;
    }

    /**
     * <p>
     * Currently, you can use this parameter to specify whether to add the certificate to a certificate transparency
     * log. Certificate transparency makes it possible to detect SSL/TLS certificates that have been mistakenly or
     * maliciously issued. Certificates that have not been logged typically produce an error message in a browser. For
     * more information, see <a
     * href="https://docs.aws.amazon.com/acm/latest/userguide/acm-bestpractices.html#best-practices-transparency">Opting
     * Out of Certificate Transparency Logging</a>.
     * </p>
     * 
     * @return Currently, you can use this parameter to specify whether to add the certificate to a certificate
     *         transparency log. Certificate transparency makes it possible to detect SSL/TLS certificates that have
     *         been mistakenly or maliciously issued. Certificates that have not been logged typically produce an error
     *         message in a browser. For more information, see <a href=
     *         "https://docs.aws.amazon.com/acm/latest/userguide/acm-bestpractices.html#best-practices-transparency"
     *         >Opting Out of Certificate Transparency Logging</a>.
     */
    public final CertificateOptions options() {
        return options;
    }

    /**
     * <p>
     * The Amazon Resource Name (ARN) of the private certificate authority (CA) that will be used to issue the
     * certificate. If you do not provide an ARN and you are trying to request a private certificate, ACM will attempt
     * to issue a public certificate. For more information about private CAs, see the <a
     * href="https://docs.aws.amazon.com/privateca/latest/userguide/PcaWelcome.html">Amazon Web Services Private
     * Certificate Authority</a> user guide. The ARN must have the following form:
     * </p>
     * <p>
     * <code>arn:aws:acm-pca:region:account:certificate-authority/12345678-1234-1234-1234-123456789012</code>
     * </p>
     * 
     * @return The Amazon Resource Name (ARN) of the private certificate authority (CA) that will be used to issue the
     *         certificate. If you do not provide an ARN and you are trying to request a private certificate, ACM will
     *         attempt to issue a public certificate. For more information about private CAs, see the <a
     *         href="https://docs.aws.amazon.com/privateca/latest/userguide/PcaWelcome.html">Amazon Web Services Private
     *         Certificate Authority</a> user guide. The ARN must have the following form: </p>
     *         <p>
     *         <code>arn:aws:acm-pca:region:account:certificate-authority/12345678-1234-1234-1234-123456789012</code>
     */
    public final String certificateAuthorityArn() {
        return certificateAuthorityArn;
    }

    /**
     * For responses, this returns true if the service returned a value for the Tags property. This DOES NOT check that
     * the value is non-empty (for which, you should check the {@code isEmpty()} method on the property). This is useful
     * because the SDK will never return a null collection or map, but you may need to differentiate between the service
     * returning nothing (or null) and the service returning an empty collection or map. For requests, this returns true
     * if a value for the property was specified in the request builder, and false if a value was not specified.
     */
    public final boolean hasTags() {
        return tags != null && !(tags instanceof SdkAutoConstructList);
    }

    /**
     * <p>
     * One or more resource tags to associate with the certificate.
     * </p>
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     * <p>
     * This method will never return null. If you would like to know whether the service returned this field (so that
     * you can differentiate between null and empty), you can use the {@link #hasTags} method.
     * </p>
     * 
     * @return One or more resource tags to associate with the certificate.
     */
    public final List<Tag> tags() {
        return tags;
    }

    /**
     * <p>
     * Specifies the algorithm of the public and private key pair that your certificate uses to encrypt data. RSA is the
     * default key algorithm for ACM certificates. Elliptic Curve Digital Signature Algorithm (ECDSA) keys are smaller,
     * offering security comparable to RSA keys but with greater computing efficiency. However, ECDSA is not supported
     * by all network clients. Some AWS services may require RSA keys, or only support ECDSA keys of a particular size,
     * while others allow the use of either RSA and ECDSA keys to ensure that compatibility is not broken. Check the
     * requirements for the AWS service where you plan to deploy your certificate.
     * </p>
     * <p>
     * Default: RSA_2048
     * </p>
     * <p>
     * If the service returns an enum value that is not available in the current SDK version, {@link #keyAlgorithm} will
     * return {@link KeyAlgorithm#UNKNOWN_TO_SDK_VERSION}. The raw value returned by the service is available from
     * {@link #keyAlgorithmAsString}.
     * </p>
     * 
     * @return Specifies the algorithm of the public and private key pair that your certificate uses to encrypt data.
     *         RSA is the default key algorithm for ACM certificates. Elliptic Curve Digital Signature Algorithm (ECDSA)
     *         keys are smaller, offering security comparable to RSA keys but with greater computing efficiency.
     *         However, ECDSA is not supported by all network clients. Some AWS services may require RSA keys, or only
     *         support ECDSA keys of a particular size, while others allow the use of either RSA and ECDSA keys to
     *         ensure that compatibility is not broken. Check the requirements for the AWS service where you plan to
     *         deploy your certificate.</p>
     *         <p>
     *         Default: RSA_2048
     * @see KeyAlgorithm
     */
    public final KeyAlgorithm keyAlgorithm() {
        return KeyAlgorithm.fromValue(keyAlgorithm);
    }

    /**
     * <p>
     * Specifies the algorithm of the public and private key pair that your certificate uses to encrypt data. RSA is the
     * default key algorithm for ACM certificates. Elliptic Curve Digital Signature Algorithm (ECDSA) keys are smaller,
     * offering security comparable to RSA keys but with greater computing efficiency. However, ECDSA is not supported
     * by all network clients. Some AWS services may require RSA keys, or only support ECDSA keys of a particular size,
     * while others allow the use of either RSA and ECDSA keys to ensure that compatibility is not broken. Check the
     * requirements for the AWS service where you plan to deploy your certificate.
     * </p>
     * <p>
     * Default: RSA_2048
     * </p>
     * <p>
     * If the service returns an enum value that is not available in the current SDK version, {@link #keyAlgorithm} will
     * return {@link KeyAlgorithm#UNKNOWN_TO_SDK_VERSION}. The raw value returned by the service is available from
     * {@link #keyAlgorithmAsString}.
     * </p>
     * 
     * @return Specifies the algorithm of the public and private key pair that your certificate uses to encrypt data.
     *         RSA is the default key algorithm for ACM certificates. Elliptic Curve Digital Signature Algorithm (ECDSA)
     *         keys are smaller, offering security comparable to RSA keys but with greater computing efficiency.
     *         However, ECDSA is not supported by all network clients. Some AWS services may require RSA keys, or only
     *         support ECDSA keys of a particular size, while others allow the use of either RSA and ECDSA keys to
     *         ensure that compatibility is not broken. Check the requirements for the AWS service where you plan to
     *         deploy your certificate.</p>
     *         <p>
     *         Default: RSA_2048
     * @see KeyAlgorithm
     */
    public final String keyAlgorithmAsString() {
        return keyAlgorithm;
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
        hashCode = 31 * hashCode + Objects.hashCode(domainName());
        hashCode = 31 * hashCode + Objects.hashCode(validationMethodAsString());
        hashCode = 31 * hashCode + Objects.hashCode(hasSubjectAlternativeNames() ? subjectAlternativeNames() : null);
        hashCode = 31 * hashCode + Objects.hashCode(idempotencyToken());
        hashCode = 31 * hashCode + Objects.hashCode(hasDomainValidationOptions() ? domainValidationOptions() : null);
        hashCode = 31 * hashCode + Objects.hashCode(options());
        hashCode = 31 * hashCode + Objects.hashCode(certificateAuthorityArn());
        hashCode = 31 * hashCode + Objects.hashCode(hasTags() ? tags() : null);
        hashCode = 31 * hashCode + Objects.hashCode(keyAlgorithmAsString());
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
        if (!(obj instanceof RequestCertificateRequest)) {
            return false;
        }
        RequestCertificateRequest other = (RequestCertificateRequest) obj;
        return Objects.equals(domainName(), other.domainName())
                && Objects.equals(validationMethodAsString(), other.validationMethodAsString())
                && hasSubjectAlternativeNames() == other.hasSubjectAlternativeNames()
                && Objects.equals(subjectAlternativeNames(), other.subjectAlternativeNames())
                && Objects.equals(idempotencyToken(), other.idempotencyToken())
                && hasDomainValidationOptions() == other.hasDomainValidationOptions()
                && Objects.equals(domainValidationOptions(), other.domainValidationOptions())
                && Objects.equals(options(), other.options())
                && Objects.equals(certificateAuthorityArn(), other.certificateAuthorityArn()) && hasTags() == other.hasTags()
                && Objects.equals(tags(), other.tags()) && Objects.equals(keyAlgorithmAsString(), other.keyAlgorithmAsString());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public final String toString() {
        return ToString.builder("RequestCertificateRequest").add("DomainName", domainName())
                .add("ValidationMethod", validationMethodAsString())
                .add("SubjectAlternativeNames", hasSubjectAlternativeNames() ? subjectAlternativeNames() : null)
                .add("IdempotencyToken", idempotencyToken())
                .add("DomainValidationOptions", hasDomainValidationOptions() ? domainValidationOptions() : null)
                .add("Options", options()).add("CertificateAuthorityArn", certificateAuthorityArn())
                .add("Tags", hasTags() ? tags() : null).add("KeyAlgorithm", keyAlgorithmAsString()).build();
    }

    public final <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
        case "DomainName":
            return Optional.ofNullable(clazz.cast(domainName()));
        case "ValidationMethod":
            return Optional.ofNullable(clazz.cast(validationMethodAsString()));
        case "SubjectAlternativeNames":
            return Optional.ofNullable(clazz.cast(subjectAlternativeNames()));
        case "IdempotencyToken":
            return Optional.ofNullable(clazz.cast(idempotencyToken()));
        case "DomainValidationOptions":
            return Optional.ofNullable(clazz.cast(domainValidationOptions()));
        case "Options":
            return Optional.ofNullable(clazz.cast(options()));
        case "CertificateAuthorityArn":
            return Optional.ofNullable(clazz.cast(certificateAuthorityArn()));
        case "Tags":
            return Optional.ofNullable(clazz.cast(tags()));
        case "KeyAlgorithm":
            return Optional.ofNullable(clazz.cast(keyAlgorithmAsString()));
        default:
            return Optional.empty();
        }
    }

    @Override
    public final List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    private static <T> Function<Object, T> getter(Function<RequestCertificateRequest, T> g) {
        return obj -> g.apply((RequestCertificateRequest) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    public interface Builder extends AcmRequest.Builder, SdkPojo, CopyableBuilder<Builder, RequestCertificateRequest> {
        /**
         * <p>
         * Fully qualified domain name (FQDN), such as www.example.com, that you want to secure with an ACM certificate.
         * Use an asterisk (*) to create a wildcard certificate that protects several sites in the same domain. For
         * example, *.example.com protects www.example.com, site.example.com, and images.example.com.
         * </p>
         * <p>
         * In compliance with <a href="https://datatracker.ietf.org/doc/html/rfc5280">RFC 5280</a>, the length of the
         * domain name (technically, the Common Name) that you provide cannot exceed 64 octets (characters), including
         * periods. To add a longer domain name, specify it in the Subject Alternative Name field, which supports names
         * up to 253 octets in length.
         * </p>
         * 
         * @param domainName
         *        Fully qualified domain name (FQDN), such as www.example.com, that you want to secure with an ACM
         *        certificate. Use an asterisk (*) to create a wildcard certificate that protects several sites in the
         *        same domain. For example, *.example.com protects www.example.com, site.example.com, and
         *        images.example.com. </p>
         *        <p>
         *        In compliance with <a href="https://datatracker.ietf.org/doc/html/rfc5280">RFC 5280</a>, the length of
         *        the domain name (technically, the Common Name) that you provide cannot exceed 64 octets (characters),
         *        including periods. To add a longer domain name, specify it in the Subject Alternative Name field,
         *        which supports names up to 253 octets in length.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder domainName(String domainName);

        /**
         * <p>
         * The method you want to use if you are requesting a public certificate to validate that you own or control
         * domain. You can <a href="https://docs.aws.amazon.com/acm/latest/userguide/gs-acm-validate-dns.html">validate
         * with DNS</a> or <a
         * href="https://docs.aws.amazon.com/acm/latest/userguide/gs-acm-validate-email.html">validate with email</a>.
         * We recommend that you use DNS validation.
         * </p>
         * 
         * @param validationMethod
         *        The method you want to use if you are requesting a public certificate to validate that you own or
         *        control domain. You can <a
         *        href="https://docs.aws.amazon.com/acm/latest/userguide/gs-acm-validate-dns.html">validate with DNS</a>
         *        or <a href="https://docs.aws.amazon.com/acm/latest/userguide/gs-acm-validate-email.html">validate with
         *        email</a>. We recommend that you use DNS validation.
         * @see ValidationMethod
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see ValidationMethod
         */
        Builder validationMethod(String validationMethod);

        /**
         * <p>
         * The method you want to use if you are requesting a public certificate to validate that you own or control
         * domain. You can <a href="https://docs.aws.amazon.com/acm/latest/userguide/gs-acm-validate-dns.html">validate
         * with DNS</a> or <a
         * href="https://docs.aws.amazon.com/acm/latest/userguide/gs-acm-validate-email.html">validate with email</a>.
         * We recommend that you use DNS validation.
         * </p>
         * 
         * @param validationMethod
         *        The method you want to use if you are requesting a public certificate to validate that you own or
         *        control domain. You can <a
         *        href="https://docs.aws.amazon.com/acm/latest/userguide/gs-acm-validate-dns.html">validate with DNS</a>
         *        or <a href="https://docs.aws.amazon.com/acm/latest/userguide/gs-acm-validate-email.html">validate with
         *        email</a>. We recommend that you use DNS validation.
         * @see ValidationMethod
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see ValidationMethod
         */
        Builder validationMethod(ValidationMethod validationMethod);

        /**
         * <p>
         * Additional FQDNs to be included in the Subject Alternative Name extension of the ACM certificate. For
         * example, add the name www.example.net to a certificate for which the <code>DomainName</code> field is
         * www.example.com if users can reach your site by using either name. The maximum number of domain names that
         * you can add to an ACM certificate is 100. However, the initial quota is 10 domain names. If you need more
         * than 10 names, you must request a quota increase. For more information, see <a
         * href="https://docs.aws.amazon.com/acm/latest/userguide/acm-limits.html">Quotas</a>.
         * </p>
         * <p>
         * The maximum length of a SAN DNS name is 253 octets. The name is made up of multiple labels separated by
         * periods. No label can be longer than 63 octets. Consider the following examples:
         * </p>
         * <ul>
         * <li>
         * <p>
         * <code>(63 octets).(63 octets).(63 octets).(61 octets)</code> is legal because the total length is 253 octets
         * (63+1+63+1+63+1+61) and no label exceeds 63 octets.
         * </p>
         * </li>
         * <li>
         * <p>
         * <code>(64 octets).(63 octets).(63 octets).(61 octets)</code> is not legal because the total length exceeds
         * 253 octets (64+1+63+1+63+1+61) and the first label exceeds 63 octets.
         * </p>
         * </li>
         * <li>
         * <p>
         * <code>(63 octets).(63 octets).(63 octets).(62 octets)</code> is not legal because the total length of the DNS
         * name (63+1+63+1+63+1+62) exceeds 253 octets.
         * </p>
         * </li>
         * </ul>
         * 
         * @param subjectAlternativeNames
         *        Additional FQDNs to be included in the Subject Alternative Name extension of the ACM certificate. For
         *        example, add the name www.example.net to a certificate for which the <code>DomainName</code> field is
         *        www.example.com if users can reach your site by using either name. The maximum number of domain names
         *        that you can add to an ACM certificate is 100. However, the initial quota is 10 domain names. If you
         *        need more than 10 names, you must request a quota increase. For more information, see <a
         *        href="https://docs.aws.amazon.com/acm/latest/userguide/acm-limits.html">Quotas</a>.</p>
         *        <p>
         *        The maximum length of a SAN DNS name is 253 octets. The name is made up of multiple labels separated
         *        by periods. No label can be longer than 63 octets. Consider the following examples:
         *        </p>
         *        <ul>
         *        <li>
         *        <p>
         *        <code>(63 octets).(63 octets).(63 octets).(61 octets)</code> is legal because the total length is 253
         *        octets (63+1+63+1+63+1+61) and no label exceeds 63 octets.
         *        </p>
         *        </li>
         *        <li>
         *        <p>
         *        <code>(64 octets).(63 octets).(63 octets).(61 octets)</code> is not legal because the total length
         *        exceeds 253 octets (64+1+63+1+63+1+61) and the first label exceeds 63 octets.
         *        </p>
         *        </li>
         *        <li>
         *        <p>
         *        <code>(63 octets).(63 octets).(63 octets).(62 octets)</code> is not legal because the total length of
         *        the DNS name (63+1+63+1+63+1+62) exceeds 253 octets.
         *        </p>
         *        </li>
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder subjectAlternativeNames(Collection<String> subjectAlternativeNames);

        /**
         * <p>
         * Additional FQDNs to be included in the Subject Alternative Name extension of the ACM certificate. For
         * example, add the name www.example.net to a certificate for which the <code>DomainName</code> field is
         * www.example.com if users can reach your site by using either name. The maximum number of domain names that
         * you can add to an ACM certificate is 100. However, the initial quota is 10 domain names. If you need more
         * than 10 names, you must request a quota increase. For more information, see <a
         * href="https://docs.aws.amazon.com/acm/latest/userguide/acm-limits.html">Quotas</a>.
         * </p>
         * <p>
         * The maximum length of a SAN DNS name is 253 octets. The name is made up of multiple labels separated by
         * periods. No label can be longer than 63 octets. Consider the following examples:
         * </p>
         * <ul>
         * <li>
         * <p>
         * <code>(63 octets).(63 octets).(63 octets).(61 octets)</code> is legal because the total length is 253 octets
         * (63+1+63+1+63+1+61) and no label exceeds 63 octets.
         * </p>
         * </li>
         * <li>
         * <p>
         * <code>(64 octets).(63 octets).(63 octets).(61 octets)</code> is not legal because the total length exceeds
         * 253 octets (64+1+63+1+63+1+61) and the first label exceeds 63 octets.
         * </p>
         * </li>
         * <li>
         * <p>
         * <code>(63 octets).(63 octets).(63 octets).(62 octets)</code> is not legal because the total length of the DNS
         * name (63+1+63+1+63+1+62) exceeds 253 octets.
         * </p>
         * </li>
         * </ul>
         * 
         * @param subjectAlternativeNames
         *        Additional FQDNs to be included in the Subject Alternative Name extension of the ACM certificate. For
         *        example, add the name www.example.net to a certificate for which the <code>DomainName</code> field is
         *        www.example.com if users can reach your site by using either name. The maximum number of domain names
         *        that you can add to an ACM certificate is 100. However, the initial quota is 10 domain names. If you
         *        need more than 10 names, you must request a quota increase. For more information, see <a
         *        href="https://docs.aws.amazon.com/acm/latest/userguide/acm-limits.html">Quotas</a>.</p>
         *        <p>
         *        The maximum length of a SAN DNS name is 253 octets. The name is made up of multiple labels separated
         *        by periods. No label can be longer than 63 octets. Consider the following examples:
         *        </p>
         *        <ul>
         *        <li>
         *        <p>
         *        <code>(63 octets).(63 octets).(63 octets).(61 octets)</code> is legal because the total length is 253
         *        octets (63+1+63+1+63+1+61) and no label exceeds 63 octets.
         *        </p>
         *        </li>
         *        <li>
         *        <p>
         *        <code>(64 octets).(63 octets).(63 octets).(61 octets)</code> is not legal because the total length
         *        exceeds 253 octets (64+1+63+1+63+1+61) and the first label exceeds 63 octets.
         *        </p>
         *        </li>
         *        <li>
         *        <p>
         *        <code>(63 octets).(63 octets).(63 octets).(62 octets)</code> is not legal because the total length of
         *        the DNS name (63+1+63+1+63+1+62) exceeds 253 octets.
         *        </p>
         *        </li>
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder subjectAlternativeNames(String... subjectAlternativeNames);

        /**
         * <p>
         * Customer chosen string that can be used to distinguish between calls to <code>RequestCertificate</code>.
         * Idempotency tokens time out after one hour. Therefore, if you call <code>RequestCertificate</code> multiple
         * times with the same idempotency token within one hour, ACM recognizes that you are requesting only one
         * certificate and will issue only one. If you change the idempotency token for each call, ACM recognizes that
         * you are requesting multiple certificates.
         * </p>
         * 
         * @param idempotencyToken
         *        Customer chosen string that can be used to distinguish between calls to
         *        <code>RequestCertificate</code>. Idempotency tokens time out after one hour. Therefore, if you call
         *        <code>RequestCertificate</code> multiple times with the same idempotency token within one hour, ACM
         *        recognizes that you are requesting only one certificate and will issue only one. If you change the
         *        idempotency token for each call, ACM recognizes that you are requesting multiple certificates.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder idempotencyToken(String idempotencyToken);

        /**
         * <p>
         * The domain name that you want ACM to use to send you emails so that you can validate domain ownership.
         * </p>
         * 
         * @param domainValidationOptions
         *        The domain name that you want ACM to use to send you emails so that you can validate domain ownership.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder domainValidationOptions(Collection<DomainValidationOption> domainValidationOptions);

        /**
         * <p>
         * The domain name that you want ACM to use to send you emails so that you can validate domain ownership.
         * </p>
         * 
         * @param domainValidationOptions
         *        The domain name that you want ACM to use to send you emails so that you can validate domain ownership.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder domainValidationOptions(DomainValidationOption... domainValidationOptions);

        /**
         * <p>
         * The domain name that you want ACM to use to send you emails so that you can validate domain ownership.
         * </p>
         * This is a convenience method that creates an instance of the
         * {@link software.amazon.awssdk.services.acm.model.DomainValidationOption.Builder} avoiding the need to create
         * one manually via {@link software.amazon.awssdk.services.acm.model.DomainValidationOption#builder()}.
         *
         * <p>
         * When the {@link Consumer} completes,
         * {@link software.amazon.awssdk.services.acm.model.DomainValidationOption.Builder#build()} is called
         * immediately and its result is passed to {@link #domainValidationOptions(List<DomainValidationOption>)}.
         * 
         * @param domainValidationOptions
         *        a consumer that will call methods on
         *        {@link software.amazon.awssdk.services.acm.model.DomainValidationOption.Builder}
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see #domainValidationOptions(java.util.Collection<DomainValidationOption>)
         */
        Builder domainValidationOptions(Consumer<DomainValidationOption.Builder>... domainValidationOptions);

        /**
         * <p>
         * Currently, you can use this parameter to specify whether to add the certificate to a certificate transparency
         * log. Certificate transparency makes it possible to detect SSL/TLS certificates that have been mistakenly or
         * maliciously issued. Certificates that have not been logged typically produce an error message in a browser.
         * For more information, see <a
         * href="https://docs.aws.amazon.com/acm/latest/userguide/acm-bestpractices.html#best-practices-transparency"
         * >Opting Out of Certificate Transparency Logging</a>.
         * </p>
         * 
         * @param options
         *        Currently, you can use this parameter to specify whether to add the certificate to a certificate
         *        transparency log. Certificate transparency makes it possible to detect SSL/TLS certificates that have
         *        been mistakenly or maliciously issued. Certificates that have not been logged typically produce an
         *        error message in a browser. For more information, see <a href=
         *        "https://docs.aws.amazon.com/acm/latest/userguide/acm-bestpractices.html#best-practices-transparency"
         *        >Opting Out of Certificate Transparency Logging</a>.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder options(CertificateOptions options);

        /**
         * <p>
         * Currently, you can use this parameter to specify whether to add the certificate to a certificate transparency
         * log. Certificate transparency makes it possible to detect SSL/TLS certificates that have been mistakenly or
         * maliciously issued. Certificates that have not been logged typically produce an error message in a browser.
         * For more information, see <a
         * href="https://docs.aws.amazon.com/acm/latest/userguide/acm-bestpractices.html#best-practices-transparency"
         * >Opting Out of Certificate Transparency Logging</a>.
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

        /**
         * <p>
         * The Amazon Resource Name (ARN) of the private certificate authority (CA) that will be used to issue the
         * certificate. If you do not provide an ARN and you are trying to request a private certificate, ACM will
         * attempt to issue a public certificate. For more information about private CAs, see the <a
         * href="https://docs.aws.amazon.com/privateca/latest/userguide/PcaWelcome.html">Amazon Web Services Private
         * Certificate Authority</a> user guide. The ARN must have the following form:
         * </p>
         * <p>
         * <code>arn:aws:acm-pca:region:account:certificate-authority/12345678-1234-1234-1234-123456789012</code>
         * </p>
         * 
         * @param certificateAuthorityArn
         *        The Amazon Resource Name (ARN) of the private certificate authority (CA) that will be used to issue
         *        the certificate. If you do not provide an ARN and you are trying to request a private certificate, ACM
         *        will attempt to issue a public certificate. For more information about private CAs, see the <a
         *        href="https://docs.aws.amazon.com/privateca/latest/userguide/PcaWelcome.html">Amazon Web Services
         *        Private Certificate Authority</a> user guide. The ARN must have the following form: </p>
         *        <p>
         *        <code>arn:aws:acm-pca:region:account:certificate-authority/12345678-1234-1234-1234-123456789012</code>
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder certificateAuthorityArn(String certificateAuthorityArn);

        /**
         * <p>
         * One or more resource tags to associate with the certificate.
         * </p>
         * 
         * @param tags
         *        One or more resource tags to associate with the certificate.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder tags(Collection<Tag> tags);

        /**
         * <p>
         * One or more resource tags to associate with the certificate.
         * </p>
         * 
         * @param tags
         *        One or more resource tags to associate with the certificate.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder tags(Tag... tags);

        /**
         * <p>
         * One or more resource tags to associate with the certificate.
         * </p>
         * This is a convenience method that creates an instance of the
         * {@link software.amazon.awssdk.services.acm.model.Tag.Builder} avoiding the need to create one manually via
         * {@link software.amazon.awssdk.services.acm.model.Tag#builder()}.
         *
         * <p>
         * When the {@link Consumer} completes, {@link software.amazon.awssdk.services.acm.model.Tag.Builder#build()} is
         * called immediately and its result is passed to {@link #tags(List<Tag>)}.
         * 
         * @param tags
         *        a consumer that will call methods on {@link software.amazon.awssdk.services.acm.model.Tag.Builder}
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see #tags(java.util.Collection<Tag>)
         */
        Builder tags(Consumer<Tag.Builder>... tags);

        /**
         * <p>
         * Specifies the algorithm of the public and private key pair that your certificate uses to encrypt data. RSA is
         * the default key algorithm for ACM certificates. Elliptic Curve Digital Signature Algorithm (ECDSA) keys are
         * smaller, offering security comparable to RSA keys but with greater computing efficiency. However, ECDSA is
         * not supported by all network clients. Some AWS services may require RSA keys, or only support ECDSA keys of a
         * particular size, while others allow the use of either RSA and ECDSA keys to ensure that compatibility is not
         * broken. Check the requirements for the AWS service where you plan to deploy your certificate.
         * </p>
         * <p>
         * Default: RSA_2048
         * </p>
         * 
         * @param keyAlgorithm
         *        Specifies the algorithm of the public and private key pair that your certificate uses to encrypt data.
         *        RSA is the default key algorithm for ACM certificates. Elliptic Curve Digital Signature Algorithm
         *        (ECDSA) keys are smaller, offering security comparable to RSA keys but with greater computing
         *        efficiency. However, ECDSA is not supported by all network clients. Some AWS services may require RSA
         *        keys, or only support ECDSA keys of a particular size, while others allow the use of either RSA and
         *        ECDSA keys to ensure that compatibility is not broken. Check the requirements for the AWS service
         *        where you plan to deploy your certificate.</p>
         *        <p>
         *        Default: RSA_2048
         * @see KeyAlgorithm
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see KeyAlgorithm
         */
        Builder keyAlgorithm(String keyAlgorithm);

        /**
         * <p>
         * Specifies the algorithm of the public and private key pair that your certificate uses to encrypt data. RSA is
         * the default key algorithm for ACM certificates. Elliptic Curve Digital Signature Algorithm (ECDSA) keys are
         * smaller, offering security comparable to RSA keys but with greater computing efficiency. However, ECDSA is
         * not supported by all network clients. Some AWS services may require RSA keys, or only support ECDSA keys of a
         * particular size, while others allow the use of either RSA and ECDSA keys to ensure that compatibility is not
         * broken. Check the requirements for the AWS service where you plan to deploy your certificate.
         * </p>
         * <p>
         * Default: RSA_2048
         * </p>
         * 
         * @param keyAlgorithm
         *        Specifies the algorithm of the public and private key pair that your certificate uses to encrypt data.
         *        RSA is the default key algorithm for ACM certificates. Elliptic Curve Digital Signature Algorithm
         *        (ECDSA) keys are smaller, offering security comparable to RSA keys but with greater computing
         *        efficiency. However, ECDSA is not supported by all network clients. Some AWS services may require RSA
         *        keys, or only support ECDSA keys of a particular size, while others allow the use of either RSA and
         *        ECDSA keys to ensure that compatibility is not broken. Check the requirements for the AWS service
         *        where you plan to deploy your certificate.</p>
         *        <p>
         *        Default: RSA_2048
         * @see KeyAlgorithm
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see KeyAlgorithm
         */
        Builder keyAlgorithm(KeyAlgorithm keyAlgorithm);

        @Override
        Builder overrideConfiguration(AwsRequestOverrideConfiguration overrideConfiguration);

        @Override
        Builder overrideConfiguration(Consumer<AwsRequestOverrideConfiguration.Builder> builderConsumer);
    }

    static final class BuilderImpl extends AcmRequest.BuilderImpl implements Builder {
        private String domainName;

        private String validationMethod;

        private List<String> subjectAlternativeNames = DefaultSdkAutoConstructList.getInstance();

        private String idempotencyToken;

        private List<DomainValidationOption> domainValidationOptions = DefaultSdkAutoConstructList.getInstance();

        private CertificateOptions options;

        private String certificateAuthorityArn;

        private List<Tag> tags = DefaultSdkAutoConstructList.getInstance();

        private String keyAlgorithm;

        private BuilderImpl() {
        }

        private BuilderImpl(RequestCertificateRequest model) {
            super(model);
            domainName(model.domainName);
            validationMethod(model.validationMethod);
            subjectAlternativeNames(model.subjectAlternativeNames);
            idempotencyToken(model.idempotencyToken);
            domainValidationOptions(model.domainValidationOptions);
            options(model.options);
            certificateAuthorityArn(model.certificateAuthorityArn);
            tags(model.tags);
            keyAlgorithm(model.keyAlgorithm);
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

        public final String getIdempotencyToken() {
            return idempotencyToken;
        }

        public final void setIdempotencyToken(String idempotencyToken) {
            this.idempotencyToken = idempotencyToken;
        }

        @Override
        public final Builder idempotencyToken(String idempotencyToken) {
            this.idempotencyToken = idempotencyToken;
            return this;
        }

        public final List<DomainValidationOption.Builder> getDomainValidationOptions() {
            List<DomainValidationOption.Builder> result = DomainValidationOptionListCopier
                    .copyToBuilder(this.domainValidationOptions);
            if (result instanceof SdkAutoConstructList) {
                return null;
            }
            return result;
        }

        public final void setDomainValidationOptions(Collection<DomainValidationOption.BuilderImpl> domainValidationOptions) {
            this.domainValidationOptions = DomainValidationOptionListCopier.copyFromBuilder(domainValidationOptions);
        }

        @Override
        public final Builder domainValidationOptions(Collection<DomainValidationOption> domainValidationOptions) {
            this.domainValidationOptions = DomainValidationOptionListCopier.copy(domainValidationOptions);
            return this;
        }

        @Override
        @SafeVarargs
        public final Builder domainValidationOptions(DomainValidationOption... domainValidationOptions) {
            domainValidationOptions(Arrays.asList(domainValidationOptions));
            return this;
        }

        @Override
        @SafeVarargs
        public final Builder domainValidationOptions(Consumer<DomainValidationOption.Builder>... domainValidationOptions) {
            domainValidationOptions(Stream.of(domainValidationOptions)
                    .map(c -> DomainValidationOption.builder().applyMutation(c).build()).collect(Collectors.toList()));
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

        public final List<Tag.Builder> getTags() {
            List<Tag.Builder> result = TagListCopier.copyToBuilder(this.tags);
            if (result instanceof SdkAutoConstructList) {
                return null;
            }
            return result;
        }

        public final void setTags(Collection<Tag.BuilderImpl> tags) {
            this.tags = TagListCopier.copyFromBuilder(tags);
        }

        @Override
        public final Builder tags(Collection<Tag> tags) {
            this.tags = TagListCopier.copy(tags);
            return this;
        }

        @Override
        @SafeVarargs
        public final Builder tags(Tag... tags) {
            tags(Arrays.asList(tags));
            return this;
        }

        @Override
        @SafeVarargs
        public final Builder tags(Consumer<Tag.Builder>... tags) {
            tags(Stream.of(tags).map(c -> Tag.builder().applyMutation(c).build()).collect(Collectors.toList()));
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
        public RequestCertificateRequest build() {
            return new RequestCertificateRequest(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}
