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
public final class ImportCertificateResponse extends AcmResponse implements
        ToCopyableBuilder<ImportCertificateResponse.Builder, ImportCertificateResponse> {
    private static final SdkField<String> CERTIFICATE_ARN_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("CertificateArn").getter(getter(ImportCertificateResponse::certificateArn))
            .setter(setter(Builder::certificateArn))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("CertificateArn").build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(CERTIFICATE_ARN_FIELD));

    private final String certificateArn;

    private ImportCertificateResponse(BuilderImpl builder) {
        super(builder);
        this.certificateArn = builder.certificateArn;
    }

    /**
     * <p>
     * The <a href="https://docs.aws.amazon.com/general/latest/gr/aws-arns-and-namespaces.html">Amazon Resource Name
     * (ARN)</a> of the imported certificate.
     * </p>
     * 
     * @return The <a href="https://docs.aws.amazon.com/general/latest/gr/aws-arns-and-namespaces.html">Amazon Resource
     *         Name (ARN)</a> of the imported certificate.
     */
    public final String certificateArn() {
        return certificateArn;
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
        if (!(obj instanceof ImportCertificateResponse)) {
            return false;
        }
        ImportCertificateResponse other = (ImportCertificateResponse) obj;
        return Objects.equals(certificateArn(), other.certificateArn());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public final String toString() {
        return ToString.builder("ImportCertificateResponse").add("CertificateArn", certificateArn()).build();
    }

    public final <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
        case "CertificateArn":
            return Optional.ofNullable(clazz.cast(certificateArn()));
        default:
            return Optional.empty();
        }
    }

    @Override
    public final List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    private static <T> Function<Object, T> getter(Function<ImportCertificateResponse, T> g) {
        return obj -> g.apply((ImportCertificateResponse) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    public interface Builder extends AcmResponse.Builder, SdkPojo, CopyableBuilder<Builder, ImportCertificateResponse> {
        /**
         * <p>
         * The <a href="https://docs.aws.amazon.com/general/latest/gr/aws-arns-and-namespaces.html">Amazon Resource Name
         * (ARN)</a> of the imported certificate.
         * </p>
         * 
         * @param certificateArn
         *        The <a href="https://docs.aws.amazon.com/general/latest/gr/aws-arns-and-namespaces.html">Amazon
         *        Resource Name (ARN)</a> of the imported certificate.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder certificateArn(String certificateArn);
    }

    static final class BuilderImpl extends AcmResponse.BuilderImpl implements Builder {
        private String certificateArn;

        private BuilderImpl() {
        }

        private BuilderImpl(ImportCertificateResponse model) {
            super(model);
            certificateArn(model.certificateArn);
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

        @Override
        public ImportCertificateResponse build() {
            return new ImportCertificateResponse(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}
