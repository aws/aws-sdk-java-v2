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
public final class GetCertificateResponse extends AcmResponse implements
        ToCopyableBuilder<GetCertificateResponse.Builder, GetCertificateResponse> {
    private static final SdkField<String> CERTIFICATE_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("Certificate").getter(getter(GetCertificateResponse::certificate)).setter(setter(Builder::certificate))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("Certificate").build()).build();

    private static final SdkField<String> CERTIFICATE_CHAIN_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("CertificateChain").getter(getter(GetCertificateResponse::certificateChain))
            .setter(setter(Builder::certificateChain))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("CertificateChain").build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(CERTIFICATE_FIELD,
            CERTIFICATE_CHAIN_FIELD));

    private final String certificate;

    private final String certificateChain;

    private GetCertificateResponse(BuilderImpl builder) {
        super(builder);
        this.certificate = builder.certificate;
        this.certificateChain = builder.certificateChain;
    }

    /**
     * <p>
     * The ACM-issued certificate corresponding to the ARN specified as input.
     * </p>
     * 
     * @return The ACM-issued certificate corresponding to the ARN specified as input.
     */
    public final String certificate() {
        return certificate;
    }

    /**
     * <p>
     * Certificates forming the requested certificate's chain of trust. The chain consists of the certificate of the
     * issuing CA and the intermediate certificates of any other subordinate CAs.
     * </p>
     * 
     * @return Certificates forming the requested certificate's chain of trust. The chain consists of the certificate of
     *         the issuing CA and the intermediate certificates of any other subordinate CAs.
     */
    public final String certificateChain() {
        return certificateChain;
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
        if (!(obj instanceof GetCertificateResponse)) {
            return false;
        }
        GetCertificateResponse other = (GetCertificateResponse) obj;
        return Objects.equals(certificate(), other.certificate()) && Objects.equals(certificateChain(), other.certificateChain());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public final String toString() {
        return ToString.builder("GetCertificateResponse").add("Certificate", certificate())
                .add("CertificateChain", certificateChain()).build();
    }

    public final <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
        case "Certificate":
            return Optional.ofNullable(clazz.cast(certificate()));
        case "CertificateChain":
            return Optional.ofNullable(clazz.cast(certificateChain()));
        default:
            return Optional.empty();
        }
    }

    @Override
    public final List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    private static <T> Function<Object, T> getter(Function<GetCertificateResponse, T> g) {
        return obj -> g.apply((GetCertificateResponse) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    public interface Builder extends AcmResponse.Builder, SdkPojo, CopyableBuilder<Builder, GetCertificateResponse> {
        /**
         * <p>
         * The ACM-issued certificate corresponding to the ARN specified as input.
         * </p>
         * 
         * @param certificate
         *        The ACM-issued certificate corresponding to the ARN specified as input.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder certificate(String certificate);

        /**
         * <p>
         * Certificates forming the requested certificate's chain of trust. The chain consists of the certificate of the
         * issuing CA and the intermediate certificates of any other subordinate CAs.
         * </p>
         * 
         * @param certificateChain
         *        Certificates forming the requested certificate's chain of trust. The chain consists of the certificate
         *        of the issuing CA and the intermediate certificates of any other subordinate CAs.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder certificateChain(String certificateChain);
    }

    static final class BuilderImpl extends AcmResponse.BuilderImpl implements Builder {
        private String certificate;

        private String certificateChain;

        private BuilderImpl() {
        }

        private BuilderImpl(GetCertificateResponse model) {
            super(model);
            certificate(model.certificate);
            certificateChain(model.certificateChain);
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

        @Override
        public GetCertificateResponse build() {
            return new GetCertificateResponse(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}
