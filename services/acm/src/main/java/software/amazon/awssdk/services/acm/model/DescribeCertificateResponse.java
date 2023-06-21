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
public final class DescribeCertificateResponse extends AcmResponse implements
        ToCopyableBuilder<DescribeCertificateResponse.Builder, DescribeCertificateResponse> {
    private static final SdkField<CertificateDetail> CERTIFICATE_FIELD = SdkField
            .<CertificateDetail> builder(MarshallingType.SDK_POJO).memberName("Certificate")
            .getter(getter(DescribeCertificateResponse::certificate)).setter(setter(Builder::certificate))
            .constructor(CertificateDetail::builder)
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("Certificate").build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(CERTIFICATE_FIELD));

    private final CertificateDetail certificate;

    private DescribeCertificateResponse(BuilderImpl builder) {
        super(builder);
        this.certificate = builder.certificate;
    }

    /**
     * <p>
     * Metadata about an ACM certificate.
     * </p>
     * 
     * @return Metadata about an ACM certificate.
     */
    public final CertificateDetail certificate() {
        return certificate;
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
        if (!(obj instanceof DescribeCertificateResponse)) {
            return false;
        }
        DescribeCertificateResponse other = (DescribeCertificateResponse) obj;
        return Objects.equals(certificate(), other.certificate());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public final String toString() {
        return ToString.builder("DescribeCertificateResponse").add("Certificate", certificate()).build();
    }

    public final <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
        case "Certificate":
            return Optional.ofNullable(clazz.cast(certificate()));
        default:
            return Optional.empty();
        }
    }

    @Override
    public final List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    private static <T> Function<Object, T> getter(Function<DescribeCertificateResponse, T> g) {
        return obj -> g.apply((DescribeCertificateResponse) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    public interface Builder extends AcmResponse.Builder, SdkPojo, CopyableBuilder<Builder, DescribeCertificateResponse> {
        /**
         * <p>
         * Metadata about an ACM certificate.
         * </p>
         * 
         * @param certificate
         *        Metadata about an ACM certificate.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder certificate(CertificateDetail certificate);

        /**
         * <p>
         * Metadata about an ACM certificate.
         * </p>
         * This is a convenience method that creates an instance of the {@link CertificateDetail.Builder} avoiding the
         * need to create one manually via {@link CertificateDetail#builder()}.
         *
         * <p>
         * When the {@link Consumer} completes, {@link CertificateDetail.Builder#build()} is called immediately and its
         * result is passed to {@link #certificate(CertificateDetail)}.
         * 
         * @param certificate
         *        a consumer that will call methods on {@link CertificateDetail.Builder}
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see #certificate(CertificateDetail)
         */
        default Builder certificate(Consumer<CertificateDetail.Builder> certificate) {
            return certificate(CertificateDetail.builder().applyMutation(certificate).build());
        }
    }

    static final class BuilderImpl extends AcmResponse.BuilderImpl implements Builder {
        private CertificateDetail certificate;

        private BuilderImpl() {
        }

        private BuilderImpl(DescribeCertificateResponse model) {
            super(model);
            certificate(model.certificate);
        }

        public final CertificateDetail.Builder getCertificate() {
            return certificate != null ? certificate.toBuilder() : null;
        }

        public final void setCertificate(CertificateDetail.BuilderImpl certificate) {
            this.certificate = certificate != null ? certificate.build() : null;
        }

        @Override
        public final Builder certificate(CertificateDetail certificate) {
            this.certificate = certificate;
            return this;
        }

        @Override
        public DescribeCertificateResponse build() {
            return new DescribeCertificateResponse(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}
