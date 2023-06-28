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
public final class UpdateCertificateOptionsRequest extends AcmRequest implements
        ToCopyableBuilder<UpdateCertificateOptionsRequest.Builder, UpdateCertificateOptionsRequest> {
    private static final SdkField<String> CERTIFICATE_ARN_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("CertificateArn").getter(getter(UpdateCertificateOptionsRequest::certificateArn))
            .setter(setter(Builder::certificateArn))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("CertificateArn").build()).build();

    private static final SdkField<CertificateOptions> OPTIONS_FIELD = SdkField
            .<CertificateOptions> builder(MarshallingType.SDK_POJO).memberName("Options")
            .getter(getter(UpdateCertificateOptionsRequest::options)).setter(setter(Builder::options))
            .constructor(CertificateOptions::builder)
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("Options").build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(CERTIFICATE_ARN_FIELD,
            OPTIONS_FIELD));

    private final String certificateArn;

    private final CertificateOptions options;

    private UpdateCertificateOptionsRequest(BuilderImpl builder) {
        super(builder);
        this.certificateArn = builder.certificateArn;
        this.options = builder.options;
    }

    /**
     * <p>
     * ARN of the requested certificate to update. This must be of the form:
     * </p>
     * <p>
     * <code>arn:aws:acm:us-east-1:<i>account</i>:certificate/<i>12345678-1234-1234-1234-123456789012</i> </code>
     * </p>
     * 
     * @return ARN of the requested certificate to update. This must be of the form:</p>
     *         <p>
     *         <code>arn:aws:acm:us-east-1:<i>account</i>:certificate/<i>12345678-1234-1234-1234-123456789012</i> </code>
     */
    public final String certificateArn() {
        return certificateArn;
    }

    /**
     * <p>
     * Use to update the options for your certificate. Currently, you can specify whether to add your certificate to a
     * transparency log. Certificate transparency makes it possible to detect SSL/TLS certificates that have been
     * mistakenly or maliciously issued. Certificates that have not been logged typically produce an error message in a
     * browser.
     * </p>
     * 
     * @return Use to update the options for your certificate. Currently, you can specify whether to add your
     *         certificate to a transparency log. Certificate transparency makes it possible to detect SSL/TLS
     *         certificates that have been mistakenly or maliciously issued. Certificates that have not been logged
     *         typically produce an error message in a browser.
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
        hashCode = 31 * hashCode + super.hashCode();
        hashCode = 31 * hashCode + Objects.hashCode(certificateArn());
        hashCode = 31 * hashCode + Objects.hashCode(options());
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
        if (!(obj instanceof UpdateCertificateOptionsRequest)) {
            return false;
        }
        UpdateCertificateOptionsRequest other = (UpdateCertificateOptionsRequest) obj;
        return Objects.equals(certificateArn(), other.certificateArn()) && Objects.equals(options(), other.options());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public final String toString() {
        return ToString.builder("UpdateCertificateOptionsRequest").add("CertificateArn", certificateArn())
                .add("Options", options()).build();
    }

    public final <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
        case "CertificateArn":
            return Optional.ofNullable(clazz.cast(certificateArn()));
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

    private static <T> Function<Object, T> getter(Function<UpdateCertificateOptionsRequest, T> g) {
        return obj -> g.apply((UpdateCertificateOptionsRequest) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    public interface Builder extends AcmRequest.Builder, SdkPojo, CopyableBuilder<Builder, UpdateCertificateOptionsRequest> {
        /**
         * <p>
         * ARN of the requested certificate to update. This must be of the form:
         * </p>
         * <p>
         * <code>arn:aws:acm:us-east-1:<i>account</i>:certificate/<i>12345678-1234-1234-1234-123456789012</i> </code>
         * </p>
         * 
         * @param certificateArn
         *        ARN of the requested certificate to update. This must be of the form:</p>
         *        <p>
         *        <code>arn:aws:acm:us-east-1:<i>account</i>:certificate/<i>12345678-1234-1234-1234-123456789012</i> </code>
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder certificateArn(String certificateArn);

        /**
         * <p>
         * Use to update the options for your certificate. Currently, you can specify whether to add your certificate to
         * a transparency log. Certificate transparency makes it possible to detect SSL/TLS certificates that have been
         * mistakenly or maliciously issued. Certificates that have not been logged typically produce an error message
         * in a browser.
         * </p>
         * 
         * @param options
         *        Use to update the options for your certificate. Currently, you can specify whether to add your
         *        certificate to a transparency log. Certificate transparency makes it possible to detect SSL/TLS
         *        certificates that have been mistakenly or maliciously issued. Certificates that have not been logged
         *        typically produce an error message in a browser.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder options(CertificateOptions options);

        /**
         * <p>
         * Use to update the options for your certificate. Currently, you can specify whether to add your certificate to
         * a transparency log. Certificate transparency makes it possible to detect SSL/TLS certificates that have been
         * mistakenly or maliciously issued. Certificates that have not been logged typically produce an error message
         * in a browser.
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

        @Override
        Builder overrideConfiguration(AwsRequestOverrideConfiguration overrideConfiguration);

        @Override
        Builder overrideConfiguration(Consumer<AwsRequestOverrideConfiguration.Builder> builderConsumer);
    }

    static final class BuilderImpl extends AcmRequest.BuilderImpl implements Builder {
        private String certificateArn;

        private CertificateOptions options;

        private BuilderImpl() {
        }

        private BuilderImpl(UpdateCertificateOptionsRequest model) {
            super(model);
            certificateArn(model.certificateArn);
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
        public UpdateCertificateOptionsRequest build() {
            return new UpdateCertificateOptionsRequest(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}
