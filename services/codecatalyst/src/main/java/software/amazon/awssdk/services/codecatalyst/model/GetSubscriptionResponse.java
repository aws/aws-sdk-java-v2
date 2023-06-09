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
public final class GetSubscriptionResponse extends CodeCatalystResponse implements
        ToCopyableBuilder<GetSubscriptionResponse.Builder, GetSubscriptionResponse> {
    private static final SdkField<String> SUBSCRIPTION_TYPE_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("subscriptionType").getter(getter(GetSubscriptionResponse::subscriptionType))
            .setter(setter(Builder::subscriptionType))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("subscriptionType").build()).build();

    private static final SdkField<String> AWS_ACCOUNT_NAME_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("awsAccountName").getter(getter(GetSubscriptionResponse::awsAccountName))
            .setter(setter(Builder::awsAccountName))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("awsAccountName").build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(SUBSCRIPTION_TYPE_FIELD,
            AWS_ACCOUNT_NAME_FIELD));

    private final String subscriptionType;

    private final String awsAccountName;

    private GetSubscriptionResponse(BuilderImpl builder) {
        super(builder);
        this.subscriptionType = builder.subscriptionType;
        this.awsAccountName = builder.awsAccountName;
    }

    /**
     * <p>
     * The type of the billing plan for the space.
     * </p>
     * 
     * @return The type of the billing plan for the space.
     */
    public final String subscriptionType() {
        return subscriptionType;
    }

    /**
     * <p>
     * The display name of the Amazon Web Services account used for billing for the space.
     * </p>
     * 
     * @return The display name of the Amazon Web Services account used for billing for the space.
     */
    public final String awsAccountName() {
        return awsAccountName;
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
        hashCode = 31 * hashCode + Objects.hashCode(subscriptionType());
        hashCode = 31 * hashCode + Objects.hashCode(awsAccountName());
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
        if (!(obj instanceof GetSubscriptionResponse)) {
            return false;
        }
        GetSubscriptionResponse other = (GetSubscriptionResponse) obj;
        return Objects.equals(subscriptionType(), other.subscriptionType())
                && Objects.equals(awsAccountName(), other.awsAccountName());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public final String toString() {
        return ToString.builder("GetSubscriptionResponse").add("SubscriptionType", subscriptionType())
                .add("AwsAccountName", awsAccountName()).build();
    }

    public final <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
        case "subscriptionType":
            return Optional.ofNullable(clazz.cast(subscriptionType()));
        case "awsAccountName":
            return Optional.ofNullable(clazz.cast(awsAccountName()));
        default:
            return Optional.empty();
        }
    }

    @Override
    public final List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    private static <T> Function<Object, T> getter(Function<GetSubscriptionResponse, T> g) {
        return obj -> g.apply((GetSubscriptionResponse) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    public interface Builder extends CodeCatalystResponse.Builder, SdkPojo, CopyableBuilder<Builder, GetSubscriptionResponse> {
        /**
         * <p>
         * The type of the billing plan for the space.
         * </p>
         * 
         * @param subscriptionType
         *        The type of the billing plan for the space.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder subscriptionType(String subscriptionType);

        /**
         * <p>
         * The display name of the Amazon Web Services account used for billing for the space.
         * </p>
         * 
         * @param awsAccountName
         *        The display name of the Amazon Web Services account used for billing for the space.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder awsAccountName(String awsAccountName);
    }

    static final class BuilderImpl extends CodeCatalystResponse.BuilderImpl implements Builder {
        private String subscriptionType;

        private String awsAccountName;

        private BuilderImpl() {
        }

        private BuilderImpl(GetSubscriptionResponse model) {
            super(model);
            subscriptionType(model.subscriptionType);
            awsAccountName(model.awsAccountName);
        }

        public final String getSubscriptionType() {
            return subscriptionType;
        }

        public final void setSubscriptionType(String subscriptionType) {
            this.subscriptionType = subscriptionType;
        }

        @Override
        public final Builder subscriptionType(String subscriptionType) {
            this.subscriptionType = subscriptionType;
            return this;
        }

        public final String getAwsAccountName() {
            return awsAccountName;
        }

        public final void setAwsAccountName(String awsAccountName) {
            this.awsAccountName = awsAccountName;
        }

        @Override
        public final Builder awsAccountName(String awsAccountName) {
            this.awsAccountName = awsAccountName;
            return this;
        }

        @Override
        public GetSubscriptionResponse build() {
            return new GetSubscriptionResponse(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}
