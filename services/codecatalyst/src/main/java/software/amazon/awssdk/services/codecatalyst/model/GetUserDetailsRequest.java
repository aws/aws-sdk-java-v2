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
public final class GetUserDetailsRequest extends CodeCatalystRequest implements
        ToCopyableBuilder<GetUserDetailsRequest.Builder, GetUserDetailsRequest> {
    private static final SdkField<String> ID_FIELD = SdkField.<String> builder(MarshallingType.STRING).memberName("id")
            .getter(getter(GetUserDetailsRequest::id)).setter(setter(Builder::id))
            .traits(LocationTrait.builder().location(MarshallLocation.QUERY_PARAM).locationName("id").build()).build();

    private static final SdkField<String> USER_NAME_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("userName").getter(getter(GetUserDetailsRequest::userName)).setter(setter(Builder::userName))
            .traits(LocationTrait.builder().location(MarshallLocation.QUERY_PARAM).locationName("userName").build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(ID_FIELD, USER_NAME_FIELD));

    private final String id;

    private final String userName;

    private GetUserDetailsRequest(BuilderImpl builder) {
        super(builder);
        this.id = builder.id;
        this.userName = builder.userName;
    }

    /**
     * <p>
     * The system-generated unique ID of the user.
     * </p>
     * 
     * @return The system-generated unique ID of the user.
     */
    public final String id() {
        return id;
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
        hashCode = 31 * hashCode + Objects.hashCode(id());
        hashCode = 31 * hashCode + Objects.hashCode(userName());
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
        if (!(obj instanceof GetUserDetailsRequest)) {
            return false;
        }
        GetUserDetailsRequest other = (GetUserDetailsRequest) obj;
        return Objects.equals(id(), other.id()) && Objects.equals(userName(), other.userName());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public final String toString() {
        return ToString.builder("GetUserDetailsRequest").add("Id", id()).add("UserName", userName()).build();
    }

    public final <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
        case "id":
            return Optional.ofNullable(clazz.cast(id()));
        case "userName":
            return Optional.ofNullable(clazz.cast(userName()));
        default:
            return Optional.empty();
        }
    }

    @Override
    public final List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    private static <T> Function<Object, T> getter(Function<GetUserDetailsRequest, T> g) {
        return obj -> g.apply((GetUserDetailsRequest) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    public interface Builder extends CodeCatalystRequest.Builder, SdkPojo, CopyableBuilder<Builder, GetUserDetailsRequest> {
        /**
         * <p>
         * The system-generated unique ID of the user.
         * </p>
         * 
         * @param id
         *        The system-generated unique ID of the user.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder id(String id);

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

        @Override
        Builder overrideConfiguration(AwsRequestOverrideConfiguration overrideConfiguration);

        @Override
        Builder overrideConfiguration(Consumer<AwsRequestOverrideConfiguration.Builder> builderConsumer);
    }

    static final class BuilderImpl extends CodeCatalystRequest.BuilderImpl implements Builder {
        private String id;

        private String userName;

        private BuilderImpl() {
        }

        private BuilderImpl(GetUserDetailsRequest model) {
            super(model);
            id(model.id);
            userName(model.userName);
        }

        public final String getId() {
            return id;
        }

        public final void setId(String id) {
            this.id = id;
        }

        @Override
        public final Builder id(String id) {
            this.id = id;
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
        public GetUserDetailsRequest build() {
            return new GetUserDetailsRequest(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}
