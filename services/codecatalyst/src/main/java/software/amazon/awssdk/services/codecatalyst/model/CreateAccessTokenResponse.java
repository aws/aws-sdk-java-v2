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

import java.time.Instant;
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
import software.amazon.awssdk.core.traits.TimestampFormatTrait;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 */
@Generated("software.amazon.awssdk:codegen")
public final class CreateAccessTokenResponse extends CodeCatalystResponse implements
        ToCopyableBuilder<CreateAccessTokenResponse.Builder, CreateAccessTokenResponse> {
    private static final SdkField<String> SECRET_FIELD = SdkField.<String> builder(MarshallingType.STRING).memberName("secret")
            .getter(getter(CreateAccessTokenResponse::secret)).setter(setter(Builder::secret))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("secret").build()).build();

    private static final SdkField<String> NAME_FIELD = SdkField.<String> builder(MarshallingType.STRING).memberName("name")
            .getter(getter(CreateAccessTokenResponse::name)).setter(setter(Builder::name))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("name").build()).build();

    private static final SdkField<Instant> EXPIRES_TIME_FIELD = SdkField
            .<Instant> builder(MarshallingType.INSTANT)
            .memberName("expiresTime")
            .getter(getter(CreateAccessTokenResponse::expiresTime))
            .setter(setter(Builder::expiresTime))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("expiresTime").build(),
                    TimestampFormatTrait.create(TimestampFormatTrait.Format.ISO_8601)).build();

    private static final SdkField<String> ACCESS_TOKEN_ID_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("accessTokenId").getter(getter(CreateAccessTokenResponse::accessTokenId))
            .setter(setter(Builder::accessTokenId))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("accessTokenId").build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(SECRET_FIELD, NAME_FIELD,
            EXPIRES_TIME_FIELD, ACCESS_TOKEN_ID_FIELD));

    private final String secret;

    private final String name;

    private final Instant expiresTime;

    private final String accessTokenId;

    private CreateAccessTokenResponse(BuilderImpl builder) {
        super(builder);
        this.secret = builder.secret;
        this.name = builder.name;
        this.expiresTime = builder.expiresTime;
        this.accessTokenId = builder.accessTokenId;
    }

    /**
     * <p>
     * The secret value of the personal access token.
     * </p>
     * 
     * @return The secret value of the personal access token.
     */
    public final String secret() {
        return secret;
    }

    /**
     * <p>
     * The friendly name of the personal access token.
     * </p>
     * 
     * @return The friendly name of the personal access token.
     */
    public final String name() {
        return name;
    }

    /**
     * <p>
     * The date and time the personal access token expires, in coordinated universal time (UTC) timestamp format as
     * specified in <a href="https://www.rfc-editor.org/rfc/rfc3339#section-5.6">RFC 3339</a>. If not specified, the
     * default is one year from creation.
     * </p>
     * 
     * @return The date and time the personal access token expires, in coordinated universal time (UTC) timestamp format
     *         as specified in <a href="https://www.rfc-editor.org/rfc/rfc3339#section-5.6">RFC 3339</a>. If not
     *         specified, the default is one year from creation.
     */
    public final Instant expiresTime() {
        return expiresTime;
    }

    /**
     * <p>
     * The system-generated unique ID of the access token.
     * </p>
     * 
     * @return The system-generated unique ID of the access token.
     */
    public final String accessTokenId() {
        return accessTokenId;
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
        hashCode = 31 * hashCode + Objects.hashCode(secret());
        hashCode = 31 * hashCode + Objects.hashCode(name());
        hashCode = 31 * hashCode + Objects.hashCode(expiresTime());
        hashCode = 31 * hashCode + Objects.hashCode(accessTokenId());
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
        if (!(obj instanceof CreateAccessTokenResponse)) {
            return false;
        }
        CreateAccessTokenResponse other = (CreateAccessTokenResponse) obj;
        return Objects.equals(secret(), other.secret()) && Objects.equals(name(), other.name())
                && Objects.equals(expiresTime(), other.expiresTime()) && Objects.equals(accessTokenId(), other.accessTokenId());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public final String toString() {
        return ToString.builder("CreateAccessTokenResponse")
                .add("Secret", secret() == null ? null : "*** Sensitive Data Redacted ***").add("Name", name())
                .add("ExpiresTime", expiresTime()).add("AccessTokenId", accessTokenId()).build();
    }

    public final <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
        case "secret":
            return Optional.ofNullable(clazz.cast(secret()));
        case "name":
            return Optional.ofNullable(clazz.cast(name()));
        case "expiresTime":
            return Optional.ofNullable(clazz.cast(expiresTime()));
        case "accessTokenId":
            return Optional.ofNullable(clazz.cast(accessTokenId()));
        default:
            return Optional.empty();
        }
    }

    @Override
    public final List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    private static <T> Function<Object, T> getter(Function<CreateAccessTokenResponse, T> g) {
        return obj -> g.apply((CreateAccessTokenResponse) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    public interface Builder extends CodeCatalystResponse.Builder, SdkPojo, CopyableBuilder<Builder, CreateAccessTokenResponse> {
        /**
         * <p>
         * The secret value of the personal access token.
         * </p>
         * 
         * @param secret
         *        The secret value of the personal access token.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder secret(String secret);

        /**
         * <p>
         * The friendly name of the personal access token.
         * </p>
         * 
         * @param name
         *        The friendly name of the personal access token.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder name(String name);

        /**
         * <p>
         * The date and time the personal access token expires, in coordinated universal time (UTC) timestamp format as
         * specified in <a href="https://www.rfc-editor.org/rfc/rfc3339#section-5.6">RFC 3339</a>. If not specified, the
         * default is one year from creation.
         * </p>
         * 
         * @param expiresTime
         *        The date and time the personal access token expires, in coordinated universal time (UTC) timestamp
         *        format as specified in <a href="https://www.rfc-editor.org/rfc/rfc3339#section-5.6">RFC 3339</a>. If
         *        not specified, the default is one year from creation.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder expiresTime(Instant expiresTime);

        /**
         * <p>
         * The system-generated unique ID of the access token.
         * </p>
         * 
         * @param accessTokenId
         *        The system-generated unique ID of the access token.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder accessTokenId(String accessTokenId);
    }

    static final class BuilderImpl extends CodeCatalystResponse.BuilderImpl implements Builder {
        private String secret;

        private String name;

        private Instant expiresTime;

        private String accessTokenId;

        private BuilderImpl() {
        }

        private BuilderImpl(CreateAccessTokenResponse model) {
            super(model);
            secret(model.secret);
            name(model.name);
            expiresTime(model.expiresTime);
            accessTokenId(model.accessTokenId);
        }

        public final String getSecret() {
            return secret;
        }

        public final void setSecret(String secret) {
            this.secret = secret;
        }

        @Override
        public final Builder secret(String secret) {
            this.secret = secret;
            return this;
        }

        public final String getName() {
            return name;
        }

        public final void setName(String name) {
            this.name = name;
        }

        @Override
        public final Builder name(String name) {
            this.name = name;
            return this;
        }

        public final Instant getExpiresTime() {
            return expiresTime;
        }

        public final void setExpiresTime(Instant expiresTime) {
            this.expiresTime = expiresTime;
        }

        @Override
        public final Builder expiresTime(Instant expiresTime) {
            this.expiresTime = expiresTime;
            return this;
        }

        public final String getAccessTokenId() {
            return accessTokenId;
        }

        public final void setAccessTokenId(String accessTokenId) {
            this.accessTokenId = accessTokenId;
        }

        @Override
        public final Builder accessTokenId(String accessTokenId) {
            this.accessTokenId = accessTokenId;
            return this;
        }

        @Override
        public CreateAccessTokenResponse build() {
            return new CreateAccessTokenResponse(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}
