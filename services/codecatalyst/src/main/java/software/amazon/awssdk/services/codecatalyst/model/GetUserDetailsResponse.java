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
public final class GetUserDetailsResponse extends CodeCatalystResponse implements
        ToCopyableBuilder<GetUserDetailsResponse.Builder, GetUserDetailsResponse> {
    private static final SdkField<String> USER_ID_FIELD = SdkField.<String> builder(MarshallingType.STRING).memberName("userId")
            .getter(getter(GetUserDetailsResponse::userId)).setter(setter(Builder::userId))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("userId").build()).build();

    private static final SdkField<String> USER_NAME_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("userName").getter(getter(GetUserDetailsResponse::userName)).setter(setter(Builder::userName))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("userName").build()).build();

    private static final SdkField<String> DISPLAY_NAME_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("displayName").getter(getter(GetUserDetailsResponse::displayName)).setter(setter(Builder::displayName))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("displayName").build()).build();

    private static final SdkField<EmailAddress> PRIMARY_EMAIL_FIELD = SdkField.<EmailAddress> builder(MarshallingType.SDK_POJO)
            .memberName("primaryEmail").getter(getter(GetUserDetailsResponse::primaryEmail))
            .setter(setter(Builder::primaryEmail)).constructor(EmailAddress::builder)
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("primaryEmail").build()).build();

    private static final SdkField<String> VERSION_FIELD = SdkField.<String> builder(MarshallingType.STRING).memberName("version")
            .getter(getter(GetUserDetailsResponse::version)).setter(setter(Builder::version))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("version").build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(USER_ID_FIELD,
            USER_NAME_FIELD, DISPLAY_NAME_FIELD, PRIMARY_EMAIL_FIELD, VERSION_FIELD));

    private final String userId;

    private final String userName;

    private final String displayName;

    private final EmailAddress primaryEmail;

    private final String version;

    private GetUserDetailsResponse(BuilderImpl builder) {
        super(builder);
        this.userId = builder.userId;
        this.userName = builder.userName;
        this.displayName = builder.displayName;
        this.primaryEmail = builder.primaryEmail;
        this.version = builder.version;
    }

    /**
     * <p>
     * The system-generated unique ID of the user.
     * </p>
     * 
     * @return The system-generated unique ID of the user.
     */
    public final String userId() {
        return userId;
    }

    /**
     * <p>
     * The name of the user as displayed in Amazon CodeCatalyst.
     * </p>
     * 
     * @return The name of the user as displayed in Amazon CodeCatalyst.
     */
    public final String userName() {
        return userName;
    }

    /**
     * <p>
     * The friendly name displayed for the user in Amazon CodeCatalyst.
     * </p>
     * 
     * @return The friendly name displayed for the user in Amazon CodeCatalyst.
     */
    public final String displayName() {
        return displayName;
    }

    /**
     * <p>
     * The email address provided by the user when they signed up.
     * </p>
     * 
     * @return The email address provided by the user when they signed up.
     */
    public final EmailAddress primaryEmail() {
        return primaryEmail;
    }

    /**
     * <p/>
     * 
     * @return
     */
    public final String version() {
        return version;
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
        hashCode = 31 * hashCode + Objects.hashCode(userId());
        hashCode = 31 * hashCode + Objects.hashCode(userName());
        hashCode = 31 * hashCode + Objects.hashCode(displayName());
        hashCode = 31 * hashCode + Objects.hashCode(primaryEmail());
        hashCode = 31 * hashCode + Objects.hashCode(version());
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
        if (!(obj instanceof GetUserDetailsResponse)) {
            return false;
        }
        GetUserDetailsResponse other = (GetUserDetailsResponse) obj;
        return Objects.equals(userId(), other.userId()) && Objects.equals(userName(), other.userName())
                && Objects.equals(displayName(), other.displayName()) && Objects.equals(primaryEmail(), other.primaryEmail())
                && Objects.equals(version(), other.version());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public final String toString() {
        return ToString.builder("GetUserDetailsResponse").add("UserId", userId()).add("UserName", userName())
                .add("DisplayName", displayName()).add("PrimaryEmail", primaryEmail()).add("Version", version()).build();
    }

    public final <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
        case "userId":
            return Optional.ofNullable(clazz.cast(userId()));
        case "userName":
            return Optional.ofNullable(clazz.cast(userName()));
        case "displayName":
            return Optional.ofNullable(clazz.cast(displayName()));
        case "primaryEmail":
            return Optional.ofNullable(clazz.cast(primaryEmail()));
        case "version":
            return Optional.ofNullable(clazz.cast(version()));
        default:
            return Optional.empty();
        }
    }

    @Override
    public final List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    private static <T> Function<Object, T> getter(Function<GetUserDetailsResponse, T> g) {
        return obj -> g.apply((GetUserDetailsResponse) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    public interface Builder extends CodeCatalystResponse.Builder, SdkPojo, CopyableBuilder<Builder, GetUserDetailsResponse> {
        /**
         * <p>
         * The system-generated unique ID of the user.
         * </p>
         * 
         * @param userId
         *        The system-generated unique ID of the user.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder userId(String userId);

        /**
         * <p>
         * The name of the user as displayed in Amazon CodeCatalyst.
         * </p>
         * 
         * @param userName
         *        The name of the user as displayed in Amazon CodeCatalyst.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder userName(String userName);

        /**
         * <p>
         * The friendly name displayed for the user in Amazon CodeCatalyst.
         * </p>
         * 
         * @param displayName
         *        The friendly name displayed for the user in Amazon CodeCatalyst.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder displayName(String displayName);

        /**
         * <p>
         * The email address provided by the user when they signed up.
         * </p>
         * 
         * @param primaryEmail
         *        The email address provided by the user when they signed up.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder primaryEmail(EmailAddress primaryEmail);

        /**
         * <p>
         * The email address provided by the user when they signed up.
         * </p>
         * This is a convenience method that creates an instance of the {@link EmailAddress.Builder} avoiding the need
         * to create one manually via {@link EmailAddress#builder()}.
         *
         * <p>
         * When the {@link Consumer} completes, {@link EmailAddress.Builder#build()} is called immediately and its
         * result is passed to {@link #primaryEmail(EmailAddress)}.
         * 
         * @param primaryEmail
         *        a consumer that will call methods on {@link EmailAddress.Builder}
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see #primaryEmail(EmailAddress)
         */
        default Builder primaryEmail(Consumer<EmailAddress.Builder> primaryEmail) {
            return primaryEmail(EmailAddress.builder().applyMutation(primaryEmail).build());
        }

        /**
         * <p/>
         * 
         * @param version
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder version(String version);
    }

    static final class BuilderImpl extends CodeCatalystResponse.BuilderImpl implements Builder {
        private String userId;

        private String userName;

        private String displayName;

        private EmailAddress primaryEmail;

        private String version;

        private BuilderImpl() {
        }

        private BuilderImpl(GetUserDetailsResponse model) {
            super(model);
            userId(model.userId);
            userName(model.userName);
            displayName(model.displayName);
            primaryEmail(model.primaryEmail);
            version(model.version);
        }

        public final String getUserId() {
            return userId;
        }

        public final void setUserId(String userId) {
            this.userId = userId;
        }

        @Override
        public final Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public final String getUserName() {
            return userName;
        }

        public final void setUserName(String userName) {
            this.userName = userName;
        }

        @Override
        public final Builder userName(String userName) {
            this.userName = userName;
            return this;
        }

        public final String getDisplayName() {
            return displayName;
        }

        public final void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public final Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public final EmailAddress.Builder getPrimaryEmail() {
            return primaryEmail != null ? primaryEmail.toBuilder() : null;
        }

        public final void setPrimaryEmail(EmailAddress.BuilderImpl primaryEmail) {
            this.primaryEmail = primaryEmail != null ? primaryEmail.build() : null;
        }

        @Override
        public final Builder primaryEmail(EmailAddress primaryEmail) {
            this.primaryEmail = primaryEmail;
            return this;
        }

        public final String getVersion() {
            return version;
        }

        public final void setVersion(String version) {
            this.version = version;
        }

        @Override
        public final Builder version(String version) {
            this.version = version;
            return this;
        }

        @Override
        public GetUserDetailsResponse build() {
            return new GetUserDetailsResponse(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}
