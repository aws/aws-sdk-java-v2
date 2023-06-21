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

import java.nio.ByteBuffer;
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
import software.amazon.awssdk.core.SdkBytes;
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
public final class ImportCertificateRequest extends AcmRequest implements
        ToCopyableBuilder<ImportCertificateRequest.Builder, ImportCertificateRequest> {
    private static final SdkField<String> CERTIFICATE_ARN_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("CertificateArn").getter(getter(ImportCertificateRequest::certificateArn))
            .setter(setter(Builder::certificateArn))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("CertificateArn").build()).build();

    private static final SdkField<SdkBytes> CERTIFICATE_FIELD = SdkField.<SdkBytes> builder(MarshallingType.SDK_BYTES)
            .memberName("Certificate").getter(getter(ImportCertificateRequest::certificate)).setter(setter(Builder::certificate))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("Certificate").build()).build();

    private static final SdkField<SdkBytes> PRIVATE_KEY_FIELD = SdkField.<SdkBytes> builder(MarshallingType.SDK_BYTES)
            .memberName("PrivateKey").getter(getter(ImportCertificateRequest::privateKey)).setter(setter(Builder::privateKey))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("PrivateKey").build()).build();

    private static final SdkField<SdkBytes> CERTIFICATE_CHAIN_FIELD = SdkField.<SdkBytes> builder(MarshallingType.SDK_BYTES)
            .memberName("CertificateChain").getter(getter(ImportCertificateRequest::certificateChain))
            .setter(setter(Builder::certificateChain))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("CertificateChain").build()).build();

    private static final SdkField<List<Tag>> TAGS_FIELD = SdkField
            .<List<Tag>> builder(MarshallingType.LIST)
            .memberName("Tags")
            .getter(getter(ImportCertificateRequest::tags))
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

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(CERTIFICATE_ARN_FIELD,
            CERTIFICATE_FIELD, PRIVATE_KEY_FIELD, CERTIFICATE_CHAIN_FIELD, TAGS_FIELD));

    private final String certificateArn;

    private final SdkBytes certificate;

    private final SdkBytes privateKey;

    private final SdkBytes certificateChain;

    private final List<Tag> tags;

    private ImportCertificateRequest(BuilderImpl builder) {
        super(builder);
        this.certificateArn = builder.certificateArn;
        this.certificate = builder.certificate;
        this.privateKey = builder.privateKey;
        this.certificateChain = builder.certificateChain;
        this.tags = builder.tags;
    }

    /**
     * <p>
     * The <a href="https://docs.aws.amazon.com/general/latest/gr/aws-arns-and-namespaces.html">Amazon Resource Name
     * (ARN)</a> of an imported certificate to replace. To import a new certificate, omit this field.
     * </p>
     * 
     * @return The <a href="https://docs.aws.amazon.com/general/latest/gr/aws-arns-and-namespaces.html">Amazon Resource
     *         Name (ARN)</a> of an imported certificate to replace. To import a new certificate, omit this field.
     */
    public final String certificateArn() {
        return certificateArn;
    }

    /**
     * <p>
     * The certificate to import.
     * </p>
     * 
     * @return The certificate to import.
     */
    public final SdkBytes certificate() {
        return certificate;
    }

    /**
     * <p>
     * The private key that matches the public key in the certificate.
     * </p>
     * 
     * @return The private key that matches the public key in the certificate.
     */
    public final SdkBytes privateKey() {
        return privateKey;
    }

    /**
     * <p>
     * The PEM encoded certificate chain.
     * </p>
     * 
     * @return The PEM encoded certificate chain.
     */
    public final SdkBytes certificateChain() {
        return certificateChain;
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
     * One or more resource tags to associate with the imported certificate.
     * </p>
     * <p>
     * Note: You cannot apply tags when reimporting a certificate.
     * </p>
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     * <p>
     * This method will never return null. If you would like to know whether the service returned this field (so that
     * you can differentiate between null and empty), you can use the {@link #hasTags} method.
     * </p>
     * 
     * @return One or more resource tags to associate with the imported certificate. </p>
     *         <p>
     *         Note: You cannot apply tags when reimporting a certificate.
     */
    public final List<Tag> tags() {
        return tags;
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
        hashCode = 31 * hashCode + Objects.hashCode(certificate());
        hashCode = 31 * hashCode + Objects.hashCode(privateKey());
        hashCode = 31 * hashCode + Objects.hashCode(certificateChain());
        hashCode = 31 * hashCode + Objects.hashCode(hasTags() ? tags() : null);
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
        if (!(obj instanceof ImportCertificateRequest)) {
            return false;
        }
        ImportCertificateRequest other = (ImportCertificateRequest) obj;
        return Objects.equals(certificateArn(), other.certificateArn()) && Objects.equals(certificate(), other.certificate())
                && Objects.equals(privateKey(), other.privateKey())
                && Objects.equals(certificateChain(), other.certificateChain()) && hasTags() == other.hasTags()
                && Objects.equals(tags(), other.tags());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public final String toString() {
        return ToString.builder("ImportCertificateRequest").add("CertificateArn", certificateArn())
                .add("Certificate", certificate())
                .add("PrivateKey", privateKey() == null ? null : "*** Sensitive Data Redacted ***")
                .add("CertificateChain", certificateChain()).add("Tags", hasTags() ? tags() : null).build();
    }

    public final <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
        case "CertificateArn":
            return Optional.ofNullable(clazz.cast(certificateArn()));
        case "Certificate":
            return Optional.ofNullable(clazz.cast(certificate()));
        case "PrivateKey":
            return Optional.ofNullable(clazz.cast(privateKey()));
        case "CertificateChain":
            return Optional.ofNullable(clazz.cast(certificateChain()));
        case "Tags":
            return Optional.ofNullable(clazz.cast(tags()));
        default:
            return Optional.empty();
        }
    }

    @Override
    public final List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    private static <T> Function<Object, T> getter(Function<ImportCertificateRequest, T> g) {
        return obj -> g.apply((ImportCertificateRequest) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    public interface Builder extends AcmRequest.Builder, SdkPojo, CopyableBuilder<Builder, ImportCertificateRequest> {
        /**
         * <p>
         * The <a href="https://docs.aws.amazon.com/general/latest/gr/aws-arns-and-namespaces.html">Amazon Resource Name
         * (ARN)</a> of an imported certificate to replace. To import a new certificate, omit this field.
         * </p>
         * 
         * @param certificateArn
         *        The <a href="https://docs.aws.amazon.com/general/latest/gr/aws-arns-and-namespaces.html">Amazon
         *        Resource Name (ARN)</a> of an imported certificate to replace. To import a new certificate, omit this
         *        field.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder certificateArn(String certificateArn);

        /**
         * <p>
         * The certificate to import.
         * </p>
         * 
         * @param certificate
         *        The certificate to import.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder certificate(SdkBytes certificate);

        /**
         * <p>
         * The private key that matches the public key in the certificate.
         * </p>
         * 
         * @param privateKey
         *        The private key that matches the public key in the certificate.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder privateKey(SdkBytes privateKey);

        /**
         * <p>
         * The PEM encoded certificate chain.
         * </p>
         * 
         * @param certificateChain
         *        The PEM encoded certificate chain.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder certificateChain(SdkBytes certificateChain);

        /**
         * <p>
         * One or more resource tags to associate with the imported certificate.
         * </p>
         * <p>
         * Note: You cannot apply tags when reimporting a certificate.
         * </p>
         * 
         * @param tags
         *        One or more resource tags to associate with the imported certificate. </p>
         *        <p>
         *        Note: You cannot apply tags when reimporting a certificate.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder tags(Collection<Tag> tags);

        /**
         * <p>
         * One or more resource tags to associate with the imported certificate.
         * </p>
         * <p>
         * Note: You cannot apply tags when reimporting a certificate.
         * </p>
         * 
         * @param tags
         *        One or more resource tags to associate with the imported certificate. </p>
         *        <p>
         *        Note: You cannot apply tags when reimporting a certificate.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder tags(Tag... tags);

        /**
         * <p>
         * One or more resource tags to associate with the imported certificate.
         * </p>
         * <p>
         * Note: You cannot apply tags when reimporting a certificate.
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

        @Override
        Builder overrideConfiguration(AwsRequestOverrideConfiguration overrideConfiguration);

        @Override
        Builder overrideConfiguration(Consumer<AwsRequestOverrideConfiguration.Builder> builderConsumer);
    }

    static final class BuilderImpl extends AcmRequest.BuilderImpl implements Builder {
        private String certificateArn;

        private SdkBytes certificate;

        private SdkBytes privateKey;

        private SdkBytes certificateChain;

        private List<Tag> tags = DefaultSdkAutoConstructList.getInstance();

        private BuilderImpl() {
        }

        private BuilderImpl(ImportCertificateRequest model) {
            super(model);
            certificateArn(model.certificateArn);
            certificate(model.certificate);
            privateKey(model.privateKey);
            certificateChain(model.certificateChain);
            tags(model.tags);
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

        public final ByteBuffer getCertificate() {
            return certificate == null ? null : certificate.asByteBuffer();
        }

        public final void setCertificate(ByteBuffer certificate) {
            certificate(certificate == null ? null : SdkBytes.fromByteBuffer(certificate));
        }

        @Override
        public final Builder certificate(SdkBytes certificate) {
            this.certificate = certificate;
            return this;
        }

        public final ByteBuffer getPrivateKey() {
            return privateKey == null ? null : privateKey.asByteBuffer();
        }

        public final void setPrivateKey(ByteBuffer privateKey) {
            privateKey(privateKey == null ? null : SdkBytes.fromByteBuffer(privateKey));
        }

        @Override
        public final Builder privateKey(SdkBytes privateKey) {
            this.privateKey = privateKey;
            return this;
        }

        public final ByteBuffer getCertificateChain() {
            return certificateChain == null ? null : certificateChain.asByteBuffer();
        }

        public final void setCertificateChain(ByteBuffer certificateChain) {
            certificateChain(certificateChain == null ? null : SdkBytes.fromByteBuffer(certificateChain));
        }

        @Override
        public final Builder certificateChain(SdkBytes certificateChain) {
            this.certificateChain = certificateChain;
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
        public ImportCertificateRequest build() {
            return new ImportCertificateRequest(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}
