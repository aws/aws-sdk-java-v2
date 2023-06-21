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
 * <p>
 * Structure that contains options for your certificate. Currently, you can use this only to specify whether to opt in
 * to or out of certificate transparency logging. Some browsers require that public certificates issued for your domain
 * be recorded in a log. Certificates that are not logged typically generate a browser error. Transparency makes it
 * possible for you to detect SSL/TLS certificates that have been mistakenly or maliciously issued for your domain. For
 * general information, see <a
 * href="https://docs.aws.amazon.com/acm/latest/userguide/acm-concepts.html#concept-transparency">Certificate
 * Transparency Logging</a>.
 * </p>
 */
@Generated("software.amazon.awssdk:codegen")
public final class CertificateOptions implements SdkPojo, Serializable,
        ToCopyableBuilder<CertificateOptions.Builder, CertificateOptions> {
    private static final SdkField<String> CERTIFICATE_TRANSPARENCY_LOGGING_PREFERENCE_FIELD = SdkField
            .<String> builder(MarshallingType.STRING)
            .memberName("CertificateTransparencyLoggingPreference")
            .getter(getter(CertificateOptions::certificateTransparencyLoggingPreferenceAsString))
            .setter(setter(Builder::certificateTransparencyLoggingPreference))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD)
                    .locationName("CertificateTransparencyLoggingPreference").build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays
            .asList(CERTIFICATE_TRANSPARENCY_LOGGING_PREFERENCE_FIELD));

    private static final long serialVersionUID = 1L;

    private final String certificateTransparencyLoggingPreference;

    private CertificateOptions(BuilderImpl builder) {
        this.certificateTransparencyLoggingPreference = builder.certificateTransparencyLoggingPreference;
    }

    /**
     * <p>
     * You can opt out of certificate transparency logging by specifying the <code>DISABLED</code> option. Opt in by
     * specifying <code>ENABLED</code>.
     * </p>
     * <p>
     * If the service returns an enum value that is not available in the current SDK version,
     * {@link #certificateTransparencyLoggingPreference} will return
     * {@link CertificateTransparencyLoggingPreference#UNKNOWN_TO_SDK_VERSION}. The raw value returned by the service is
     * available from {@link #certificateTransparencyLoggingPreferenceAsString}.
     * </p>
     * 
     * @return You can opt out of certificate transparency logging by specifying the <code>DISABLED</code> option. Opt
     *         in by specifying <code>ENABLED</code>.
     * @see CertificateTransparencyLoggingPreference
     */
    public final CertificateTransparencyLoggingPreference certificateTransparencyLoggingPreference() {
        return CertificateTransparencyLoggingPreference.fromValue(certificateTransparencyLoggingPreference);
    }

    /**
     * <p>
     * You can opt out of certificate transparency logging by specifying the <code>DISABLED</code> option. Opt in by
     * specifying <code>ENABLED</code>.
     * </p>
     * <p>
     * If the service returns an enum value that is not available in the current SDK version,
     * {@link #certificateTransparencyLoggingPreference} will return
     * {@link CertificateTransparencyLoggingPreference#UNKNOWN_TO_SDK_VERSION}. The raw value returned by the service is
     * available from {@link #certificateTransparencyLoggingPreferenceAsString}.
     * </p>
     * 
     * @return You can opt out of certificate transparency logging by specifying the <code>DISABLED</code> option. Opt
     *         in by specifying <code>ENABLED</code>.
     * @see CertificateTransparencyLoggingPreference
     */
    public final String certificateTransparencyLoggingPreferenceAsString() {
        return certificateTransparencyLoggingPreference;
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
        hashCode = 31 * hashCode + Objects.hashCode(certificateTransparencyLoggingPreferenceAsString());
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
        if (!(obj instanceof CertificateOptions)) {
            return false;
        }
        CertificateOptions other = (CertificateOptions) obj;
        return Objects.equals(certificateTransparencyLoggingPreferenceAsString(),
                other.certificateTransparencyLoggingPreferenceAsString());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public final String toString() {
        return ToString.builder("CertificateOptions")
                .add("CertificateTransparencyLoggingPreference", certificateTransparencyLoggingPreferenceAsString()).build();
    }

    public final <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
        case "CertificateTransparencyLoggingPreference":
            return Optional.ofNullable(clazz.cast(certificateTransparencyLoggingPreferenceAsString()));
        default:
            return Optional.empty();
        }
    }

    @Override
    public final List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    private static <T> Function<Object, T> getter(Function<CertificateOptions, T> g) {
        return obj -> g.apply((CertificateOptions) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    public interface Builder extends SdkPojo, CopyableBuilder<Builder, CertificateOptions> {
        /**
         * <p>
         * You can opt out of certificate transparency logging by specifying the <code>DISABLED</code> option. Opt in by
         * specifying <code>ENABLED</code>.
         * </p>
         * 
         * @param certificateTransparencyLoggingPreference
         *        You can opt out of certificate transparency logging by specifying the <code>DISABLED</code> option.
         *        Opt in by specifying <code>ENABLED</code>.
         * @see CertificateTransparencyLoggingPreference
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see CertificateTransparencyLoggingPreference
         */
        Builder certificateTransparencyLoggingPreference(String certificateTransparencyLoggingPreference);

        /**
         * <p>
         * You can opt out of certificate transparency logging by specifying the <code>DISABLED</code> option. Opt in by
         * specifying <code>ENABLED</code>.
         * </p>
         * 
         * @param certificateTransparencyLoggingPreference
         *        You can opt out of certificate transparency logging by specifying the <code>DISABLED</code> option.
         *        Opt in by specifying <code>ENABLED</code>.
         * @see CertificateTransparencyLoggingPreference
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see CertificateTransparencyLoggingPreference
         */
        Builder certificateTransparencyLoggingPreference(
                CertificateTransparencyLoggingPreference certificateTransparencyLoggingPreference);
    }

    static final class BuilderImpl implements Builder {
        private String certificateTransparencyLoggingPreference;

        private BuilderImpl() {
        }

        private BuilderImpl(CertificateOptions model) {
            certificateTransparencyLoggingPreference(model.certificateTransparencyLoggingPreference);
        }

        public final String getCertificateTransparencyLoggingPreference() {
            return certificateTransparencyLoggingPreference;
        }

        public final void setCertificateTransparencyLoggingPreference(String certificateTransparencyLoggingPreference) {
            this.certificateTransparencyLoggingPreference = certificateTransparencyLoggingPreference;
        }

        @Override
        public final Builder certificateTransparencyLoggingPreference(String certificateTransparencyLoggingPreference) {
            this.certificateTransparencyLoggingPreference = certificateTransparencyLoggingPreference;
            return this;
        }

        @Override
        public final Builder certificateTransparencyLoggingPreference(
                CertificateTransparencyLoggingPreference certificateTransparencyLoggingPreference) {
            this.certificateTransparencyLoggingPreference(certificateTransparencyLoggingPreference == null ? null
                    : certificateTransparencyLoggingPreference.toString());
            return this;
        }

        @Override
        public CertificateOptions build() {
            return new CertificateOptions(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}
