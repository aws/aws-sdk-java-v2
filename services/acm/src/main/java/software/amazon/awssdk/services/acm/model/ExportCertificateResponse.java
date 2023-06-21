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
import java.util.function.Function;
import software.amazon.awssdk.annotations.Generated;
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
public final class ExportCertificateResponse extends AcmResponse implements
        ToCopyableBuilder<ExportCertificateResponse.Builder, ExportCertificateResponse> {
    private static final SdkField<String> CERTIFICATE_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("Certificate").getter(getter(ExportCertificateResponse::certificate))
            .setter(setter(Builder::certificate))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("Certificate").build()).build();

    private static final SdkField<String> CERTIFICATE_CHAIN_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("CertificateChain").getter(getter(ExportCertificateResponse::certificateChain))
            .setter(setter(Builder::certificateChain))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("CertificateChain").build()).build();

    private static final SdkField<String> PRIVATE_KEY_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("PrivateKey").getter(getter(ExportCertificateResponse::privateKey)).setter(setter(Builder::privateKey))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("PrivateKey").build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(CERTIFICATE_FIELD,
            CERTIFICATE_CHAIN_FIELD, PRIVATE_KEY_FIELD));

    private final String certificate;

    private final String certificateChain;

    private final String privateKey;

    private ExportCertificateResponse(BuilderImpl builder) {
        super(builder);
        this.certificate = builder.certificate;
        this.certificateChain = builder.certificateChain;
        this.privateKey = builder.privateKey;
    }

    /**
     * <p>
     * The base64 PEM-encoded certificate.
     * </p>
     * 
     * @return The base64 PEM-encoded certificate.
     */
    public final String certificate() {
        return certificate;
    }

    /**
     * <p>
     * The base64 PEM-encoded certificate chain. This does not include the certificate that you are exporting.
     * </p>
     * 
     * @return The base64 PEM-encoded certificate chain. This does not include the certificate that you are exporting.
     */
    public final String certificateChain() {
        return certificateChain;
    }

    /**
     * <p>
     * The encrypted private key associated with the public key in the certificate. The key is output in PKCS #8 format
     * and is base64 PEM-encoded.
     * </p>
     * 
     * @return The encrypted private key associated with the public key in the certificate. The key is output in PKCS #8
     *         format and is base64 PEM-encoded.
     */
    public final String privateKey() {
        return privateKey;
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
        hashCode = 31 * hashCode + Objects.hashCode(certificate());
        hashCode = 31 * hashCode + Objects.hashCode(certificateChain());
        hashCode = 31 * hashCode + Objects.hashCode(privateKey());
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
        if (!(obj instanceof ExportCertificateResponse)) {
            return false;
        }
        ExportCertificateResponse other = (ExportCertificateResponse) obj;
        return Objects.equals(certificate(), other.certificate()) && Objects.equals(certificateChain(), other.certificateChain())
                && Objects.equals(privateKey(), other.privateKey());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public final String toString() {
        return ToString.builder("ExportCertificateResponse").add("Certificate", certificate())
                .add("CertificateChain", certificateChain())
                .add("PrivateKey", privateKey() == null ? null : "*** Sensitive Data Redacted ***").build();
    }

    public final <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
        case "Certificate":
            return Optional.ofNullable(clazz.cast(certificate()));
        case "CertificateChain":
            return Optional.ofNullable(clazz.cast(certificateChain()));
        case "PrivateKey":
            return Optional.ofNullable(clazz.cast(privateKey()));
        default:
            return Optional.empty();
        }
    }

    @Override
    public final List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    private static <T> Function<Object, T> getter(Function<ExportCertificateResponse, T> g) {
        return obj -> g.apply((ExportCertificateResponse) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    public interface Builder extends AcmResponse.Builder, SdkPojo, CopyableBuilder<Builder, ExportCertificateResponse> {
        /**
         * <p>
         * The base64 PEM-encoded certificate.
         * </p>
         * 
         * @param certificate
         *        The base64 PEM-encoded certificate.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder certificate(String certificate);

        /**
         * <p>
         * The base64 PEM-encoded certificate chain. This does not include the certificate that you are exporting.
         * </p>
         * 
         * @param certificateChain
         *        The base64 PEM-encoded certificate chain. This does not include the certificate that you are
         *        exporting.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder certificateChain(String certificateChain);

        /**
         * <p>
         * The encrypted private key associated with the public key in the certificate. The key is output in PKCS #8
         * format and is base64 PEM-encoded.
         * </p>
         * 
         * @param privateKey
         *        The encrypted private key associated with the public key in the certificate. The key is output in PKCS
         *        #8 format and is base64 PEM-encoded.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder privateKey(String privateKey);
    }

    static final class BuilderImpl extends AcmResponse.BuilderImpl implements Builder {
        private String certificate;

        private String certificateChain;

        private String privateKey;

        private BuilderImpl() {
        }

        private BuilderImpl(ExportCertificateResponse model) {
            super(model);
            certificate(model.certificate);
            certificateChain(model.certificateChain);
            privateKey(model.privateKey);
        }

        public final String getCertificate() {
            return certificate;
        }

        public final void setCertificate(String certificate) {
            this.certificate = certificate;
        }

        @Override
        public final Builder certificate(String certificate) {
            this.certificate = certificate;
            return this;
        }

        public final String getCertificateChain() {
            return certificateChain;
        }

        public final void setCertificateChain(String certificateChain) {
            this.certificateChain = certificateChain;
        }

        @Override
        public final Builder certificateChain(String certificateChain) {
            this.certificateChain = certificateChain;
            return this;
        }

        public final String getPrivateKey() {
            return privateKey;
        }

        public final void setPrivateKey(String privateKey) {
            this.privateKey = privateKey;
        }

        @Override
        public final Builder privateKey(String privateKey) {
            this.privateKey = privateKey;
            return this;
        }

        @Override
        public ExportCertificateResponse build() {
            return new ExportCertificateResponse(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}
