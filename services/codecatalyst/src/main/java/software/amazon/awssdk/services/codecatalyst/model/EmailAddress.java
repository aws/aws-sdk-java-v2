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

package software.amazon.awssdk.services.codecatalyst.model;

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
 * Information about an email address.
 * </p>
 */
@Generated("software.amazon.awssdk:codegen")
public final class EmailAddress implements SdkPojo, Serializable, ToCopyableBuilder<EmailAddress.Builder, EmailAddress> {
    private static final SdkField<String> EMAIL_FIELD = SdkField.<String> builder(MarshallingType.STRING).memberName("email")
            .getter(getter(EmailAddress::email)).setter(setter(Builder::email))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("email").build()).build();

    private static final SdkField<Boolean> VERIFIED_FIELD = SdkField.<Boolean> builder(MarshallingType.BOOLEAN)
            .memberName("verified").getter(getter(EmailAddress::verified)).setter(setter(Builder::verified))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("verified").build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(EMAIL_FIELD, VERIFIED_FIELD));

    private static final long serialVersionUID = 1L;

    private final String email;

    private final Boolean verified;

    private EmailAddress(BuilderImpl builder) {
        this.email = builder.email;
        this.verified = builder.verified;
    }

    /**
     * <p>
     * The email address.
     * </p>
     * 
     * @return The email address.
     */
    public final String email() {
        return email;
    }

    /**
     * <p>
     * Whether the email address has been verified.
     * </p>
     * 
     * @return Whether the email address has been verified.
     */
    public final Boolean verified() {
        return verified;
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
        hashCode = 31 * hashCode + Objects.hashCode(email());
        hashCode = 31 * hashCode + Objects.hashCode(verified());
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
        if (!(obj instanceof EmailAddress)) {
            return false;
        }
        EmailAddress other = (EmailAddress) obj;
        return Objects.equals(email(), other.email()) && Objects.equals(verified(), other.verified());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public final String toString() {
        return ToString.builder("EmailAddress").add("Email", email()).add("Verified", verified()).build();
    }

    public final <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
        case "email":
            return Optional.ofNullable(clazz.cast(email()));
        case "verified":
            return Optional.ofNullable(clazz.cast(verified()));
        default:
            return Optional.empty();
        }
    }

    @Override
    public final List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    private static <T> Function<Object, T> getter(Function<EmailAddress, T> g) {
        return obj -> g.apply((EmailAddress) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    public interface Builder extends SdkPojo, CopyableBuilder<Builder, EmailAddress> {
        /**
         * <p>
         * The email address.
         * </p>
         * 
         * @param email
         *        The email address.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder email(String email);

        /**
         * <p>
         * Whether the email address has been verified.
         * </p>
         * 
         * @param verified
         *        Whether the email address has been verified.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder verified(Boolean verified);
    }

    static final class BuilderImpl implements Builder {
        private String email;

        private Boolean verified;

        private BuilderImpl() {
        }

        private BuilderImpl(EmailAddress model) {
            email(model.email);
            verified(model.verified);
        }

        public final String getEmail() {
            return email;
        }

        public final void setEmail(String email) {
            this.email = email;
        }

        @Override
        public final Builder email(String email) {
            this.email = email;
            return this;
        }

        public final Boolean getVerified() {
            return verified;
        }

        public final void setVerified(Boolean verified) {
            this.verified = verified;
        }

        @Override
        public final Builder verified(Boolean verified) {
            this.verified = verified;
            return this;
        }

        @Override
        public EmailAddress build() {
            return new EmailAddress(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}
