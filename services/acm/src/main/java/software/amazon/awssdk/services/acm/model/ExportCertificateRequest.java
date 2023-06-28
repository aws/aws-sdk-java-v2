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
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkBytes;
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
public final class ExportCertificateRequest extends AcmRequest implements
        ToCopyableBuilder<ExportCertificateRequest.Builder, ExportCertificateRequest> {
    private static final SdkField<String> CERTIFICATE_ARN_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("CertificateArn").getter(getter(ExportCertificateRequest::certificateArn))
            .setter(setter(Builder::certificateArn))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("CertificateArn").build()).build();

    private static final SdkField<SdkBytes> PASSPHRASE_FIELD = SdkField.<SdkBytes> builder(MarshallingType.SDK_BYTES)
            .memberName("Passphrase").getter(getter(ExportCertificateRequest::passphrase)).setter(setter(Builder::passphrase))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("Passphrase").build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(CERTIFICATE_ARN_FIELD,
            PASSPHRASE_FIELD));

    private final String certificateArn;

    private final SdkBytes passphrase;

    private ExportCertificateRequest(BuilderImpl builder) {
        super(builder);
        this.certificateArn = builder.certificateArn;
        this.passphrase = builder.passphrase;
    }

    /**
     * <p>
     * An Amazon Resource Name (ARN) of the issued certificate. This must be of the form:
     * </p>
     * <p>
     * <code>arn:aws:acm:region:account:certificate/12345678-1234-1234-1234-123456789012</code>
     * </p>
     * 
     * @return An Amazon Resource Name (ARN) of the issued certificate. This must be of the form:</p>
     *         <p>
     *         <code>arn:aws:acm:region:account:certificate/12345678-1234-1234-1234-123456789012</code>
     */
    public final String certificateArn() {
        return certificateArn;
    }

    /**
     * <p>
     * Passphrase to associate with the encrypted exported private key.
     * </p>
     * <note>
     * <p>
     * When creating your passphrase, you can use any ASCII character except #, $, or %.
     * </p>
     * </note>
     * <p>
     * If you want to later decrypt the private key, you must have the passphrase. You can use the following OpenSSL
     * command to decrypt a private key. After entering the command, you are prompted for the passphrase.
     * </p>
     * <p>
     * <code>openssl rsa -in encrypted_key.pem -out decrypted_key.pem</code>
     * </p>
     * 
     * @return Passphrase to associate with the encrypted exported private key. </p> <note>
     *         <p>
     *         When creating your passphrase, you can use any ASCII character except #, $, or %.
     *         </p>
     *         </note>
     *         <p>
     *         If you want to later decrypt the private key, you must have the passphrase. You can use the following
     *         OpenSSL command to decrypt a private key. After entering the command, you are prompted for the
     *         passphrase.
     *         </p>
     *         <p>
     *         <code>openssl rsa -in encrypted_key.pem -out decrypted_key.pem</code>
     */
    public final SdkBytes passphrase() {
        return passphrase;
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
        hashCode = 31 * hashCode + Objects.hashCode(passphrase());
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
        if (!(obj instanceof ExportCertificateRequest)) {
            return false;
        }
        ExportCertificateRequest other = (ExportCertificateRequest) obj;
        return Objects.equals(certificateArn(), other.certificateArn()) && Objects.equals(passphrase(), other.passphrase());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public final String toString() {
        return ToString.builder("ExportCertificateRequest").add("CertificateArn", certificateArn())
                .add("Passphrase", passphrase() == null ? null : "*** Sensitive Data Redacted ***").build();
    }

    public final <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
        case "CertificateArn":
            return Optional.ofNullable(clazz.cast(certificateArn()));
        case "Passphrase":
            return Optional.ofNullable(clazz.cast(passphrase()));
        default:
            return Optional.empty();
        }
    }

    @Override
    public final List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    private static <T> Function<Object, T> getter(Function<ExportCertificateRequest, T> g) {
        return obj -> g.apply((ExportCertificateRequest) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    public interface Builder extends AcmRequest.Builder, SdkPojo, CopyableBuilder<Builder, ExportCertificateRequest> {
        /**
         * <p>
         * An Amazon Resource Name (ARN) of the issued certificate. This must be of the form:
         * </p>
         * <p>
         * <code>arn:aws:acm:region:account:certificate/12345678-1234-1234-1234-123456789012</code>
         * </p>
         * 
         * @param certificateArn
         *        An Amazon Resource Name (ARN) of the issued certificate. This must be of the form:</p>
         *        <p>
         *        <code>arn:aws:acm:region:account:certificate/12345678-1234-1234-1234-123456789012</code>
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder certificateArn(String certificateArn);

        /**
         * <p>
         * Passphrase to associate with the encrypted exported private key.
         * </p>
         * <note>
         * <p>
         * When creating your passphrase, you can use any ASCII character except #, $, or %.
         * </p>
         * </note>
         * <p>
         * If you want to later decrypt the private key, you must have the passphrase. You can use the following OpenSSL
         * command to decrypt a private key. After entering the command, you are prompted for the passphrase.
         * </p>
         * <p>
         * <code>openssl rsa -in encrypted_key.pem -out decrypted_key.pem</code>
         * </p>
         * 
         * @param passphrase
         *        Passphrase to associate with the encrypted exported private key. </p> <note>
         *        <p>
         *        When creating your passphrase, you can use any ASCII character except #, $, or %.
         *        </p>
         *        </note>
         *        <p>
         *        If you want to later decrypt the private key, you must have the passphrase. You can use the following
         *        OpenSSL command to decrypt a private key. After entering the command, you are prompted for the
         *        passphrase.
         *        </p>
         *        <p>
         *        <code>openssl rsa -in encrypted_key.pem -out decrypted_key.pem</code>
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder passphrase(SdkBytes passphrase);

        @Override
        Builder overrideConfiguration(AwsRequestOverrideConfiguration overrideConfiguration);

        @Override
        Builder overrideConfiguration(Consumer<AwsRequestOverrideConfiguration.Builder> builderConsumer);
    }

    static final class BuilderImpl extends AcmRequest.BuilderImpl implements Builder {
        private String certificateArn;

        private SdkBytes passphrase;

        private BuilderImpl() {
        }

        private BuilderImpl(ExportCertificateRequest model) {
            super(model);
            certificateArn(model.certificateArn);
            passphrase(model.passphrase);
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

        public final ByteBuffer getPassphrase() {
            return passphrase == null ? null : passphrase.asByteBuffer();
        }

        public final void setPassphrase(ByteBuffer passphrase) {
            passphrase(passphrase == null ? null : SdkBytes.fromByteBuffer(passphrase));
        }

        @Override
        public final Builder passphrase(SdkBytes passphrase) {
            this.passphrase = passphrase;
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
        public ExportCertificateRequest build() {
            return new ExportCertificateRequest(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}
