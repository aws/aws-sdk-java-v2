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
 * Information about a user whose activity is recorded in an event for a space.
 * </p>
 */
@Generated("software.amazon.awssdk:codegen")
public final class UserIdentity implements SdkPojo, Serializable, ToCopyableBuilder<UserIdentity.Builder, UserIdentity> {
    private static final SdkField<String> USER_TYPE_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("userType").getter(getter(UserIdentity::userTypeAsString)).setter(setter(Builder::userType))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("userType").build()).build();

    private static final SdkField<String> PRINCIPAL_ID_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("principalId").getter(getter(UserIdentity::principalId)).setter(setter(Builder::principalId))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("principalId").build()).build();

    private static final SdkField<String> USER_NAME_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("userName").getter(getter(UserIdentity::userName)).setter(setter(Builder::userName))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("userName").build()).build();

    private static final SdkField<String> AWS_ACCOUNT_ID_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("awsAccountId").getter(getter(UserIdentity::awsAccountId)).setter(setter(Builder::awsAccountId))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("awsAccountId").build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(USER_TYPE_FIELD,
            PRINCIPAL_ID_FIELD, USER_NAME_FIELD, AWS_ACCOUNT_ID_FIELD));

    private static final long serialVersionUID = 1L;

    private final String userType;

    private final String principalId;

    private final String userName;

    private final String awsAccountId;

    private UserIdentity(BuilderImpl builder) {
        this.userType = builder.userType;
        this.principalId = builder.principalId;
        this.userName = builder.userName;
        this.awsAccountId = builder.awsAccountId;
    }

    /**
     * <p>
     * The role assigned to the user in a Amazon CodeCatalyst space or project when the event occurred.
     * </p>
     * <p>
     * If the service returns an enum value that is not available in the current SDK version, {@link #userType} will
     * return {@link UserType#UNKNOWN_TO_SDK_VERSION}. The raw value returned by the service is available from
     * {@link #userTypeAsString}.
     * </p>
     * 
     * @return The role assigned to the user in a Amazon CodeCatalyst space or project when the event occurred.
     * @see UserType
     */
    public final UserType userType() {
        return UserType.fromValue(userType);
    }

    /**
     * <p>
     * The role assigned to the user in a Amazon CodeCatalyst space or project when the event occurred.
     * </p>
     * <p>
     * If the service returns an enum value that is not available in the current SDK version, {@link #userType} will
     * return {@link UserType#UNKNOWN_TO_SDK_VERSION}. The raw value returned by the service is available from
     * {@link #userTypeAsString}.
     * </p>
     * 
     * @return The role assigned to the user in a Amazon CodeCatalyst space or project when the event occurred.
     * @see UserType
     */
    public final String userTypeAsString() {
        return userType;
    }

    /**
     * <p/>
     * 
     * @return
     */
    public final String principalId() {
        return principalId;
    }

    /**
     * <p>
     * The display name of the user in Amazon CodeCatalyst.
     * </p>
     * 
     * @return The display name of the user in Amazon CodeCatalyst.
     */
    public final String userName() {
        return userName;
    }

    /**
     * <p>
     * The Amazon Web Services account number of the user in Amazon Web Services, if any.
     * </p>
     * 
     * @return The Amazon Web Services account number of the user in Amazon Web Services, if any.
     */
    public final String awsAccountId() {
        return awsAccountId;
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
        hashCode = 31 * hashCode + Objects.hashCode(userTypeAsString());
        hashCode = 31 * hashCode + Objects.hashCode(principalId());
        hashCode = 31 * hashCode + Objects.hashCode(userName());
        hashCode = 31 * hashCode + Objects.hashCode(awsAccountId());
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
        if (!(obj instanceof UserIdentity)) {
            return false;
        }
        UserIdentity other = (UserIdentity) obj;
        return Objects.equals(userTypeAsString(), other.userTypeAsString()) && Objects.equals(principalId(), other.principalId())
                && Objects.equals(userName(), other.userName()) && Objects.equals(awsAccountId(), other.awsAccountId());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public final String toString() {
        return ToString.builder("UserIdentity").add("UserType", userTypeAsString()).add("PrincipalId", principalId())
                .add("UserName", userName()).add("AwsAccountId", awsAccountId()).build();
    }

    public final <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
        case "userType":
            return Optional.ofNullable(clazz.cast(userTypeAsString()));
        case "principalId":
            return Optional.ofNullable(clazz.cast(principalId()));
        case "userName":
            return Optional.ofNullable(clazz.cast(userName()));
        case "awsAccountId":
            return Optional.ofNullable(clazz.cast(awsAccountId()));
        default:
            return Optional.empty();
        }
    }

    @Override
    public final List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    private static <T> Function<Object, T> getter(Function<UserIdentity, T> g) {
        return obj -> g.apply((UserIdentity) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    public interface Builder extends SdkPojo, CopyableBuilder<Builder, UserIdentity> {
        /**
         * <p>
         * The role assigned to the user in a Amazon CodeCatalyst space or project when the event occurred.
         * </p>
         * 
         * @param userType
         *        The role assigned to the user in a Amazon CodeCatalyst space or project when the event occurred.
         * @see UserType
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see UserType
         */
        Builder userType(String userType);

        /**
         * <p>
         * The role assigned to the user in a Amazon CodeCatalyst space or project when the event occurred.
         * </p>
         * 
         * @param userType
         *        The role assigned to the user in a Amazon CodeCatalyst space or project when the event occurred.
         * @see UserType
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see UserType
         */
        Builder userType(UserType userType);

        /**
         * <p/>
         * 
         * @param principalId
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder principalId(String principalId);

        /**
         * <p>
         * The display name of the user in Amazon CodeCatalyst.
         * </p>
         * 
         * @param userName
         *        The display name of the user in Amazon CodeCatalyst.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder userName(String userName);

        /**
         * <p>
         * The Amazon Web Services account number of the user in Amazon Web Services, if any.
         * </p>
         * 
         * @param awsAccountId
         *        The Amazon Web Services account number of the user in Amazon Web Services, if any.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder awsAccountId(String awsAccountId);
    }

    static final class BuilderImpl implements Builder {
        private String userType;

        private String principalId;

        private String userName;

        private String awsAccountId;

        private BuilderImpl() {
        }

        private BuilderImpl(UserIdentity model) {
            userType(model.userType);
            principalId(model.principalId);
            userName(model.userName);
            awsAccountId(model.awsAccountId);
        }

        public final String getUserType() {
            return userType;
        }

        public final void setUserType(String userType) {
            this.userType = userType;
        }

        @Override
        public final Builder userType(String userType) {
            this.userType = userType;
            return this;
        }

        @Override
        public final Builder userType(UserType userType) {
            this.userType(userType == null ? null : userType.toString());
            return this;
        }

        public final String getPrincipalId() {
            return principalId;
        }

        public final void setPrincipalId(String principalId) {
            this.principalId = principalId;
        }

        @Override
        public final Builder principalId(String principalId) {
            this.principalId = principalId;
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

        public final String getAwsAccountId() {
            return awsAccountId;
        }

        public final void setAwsAccountId(String awsAccountId) {
            this.awsAccountId = awsAccountId;
        }

        @Override
        public final Builder awsAccountId(String awsAccountId) {
            this.awsAccountId = awsAccountId;
            return this;
        }

        @Override
        public UserIdentity build() {
            return new UserIdentity(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}
