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
public final class GetSubscriptionRequest extends CodeCatalystRequest implements
        ToCopyableBuilder<GetSubscriptionRequest.Builder, GetSubscriptionRequest> {
    private static final SdkField<String> SPACE_NAME_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("spaceName").getter(getter(GetSubscriptionRequest::spaceName)).setter(setter(Builder::spaceName))
            .traits(LocationTrait.builder().location(MarshallLocation.PATH).locationName("spaceName").build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(SPACE_NAME_FIELD));

    private final String spaceName;

    private GetSubscriptionRequest(BuilderImpl builder) {
        super(builder);
        this.spaceName = builder.spaceName;
    }

    /**
     * <p>
     * The name of the space.
     * </p>
     * 
     * @return The name of the space.
     */
    public final String spaceName() {
        return spaceName;
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
        hashCode = 31 * hashCode + Objects.hashCode(spaceName());
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
        if (!(obj instanceof GetSubscriptionRequest)) {
            return false;
        }
        GetSubscriptionRequest other = (GetSubscriptionRequest) obj;
        return Objects.equals(spaceName(), other.spaceName());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public final String toString() {
        return ToString.builder("GetSubscriptionRequest").add("SpaceName", spaceName()).build();
    }

    public final <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
        case "spaceName":
            return Optional.ofNullable(clazz.cast(spaceName()));
        default:
            return Optional.empty();
        }
    }

    @Override
    public final List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    private static <T> Function<Object, T> getter(Function<GetSubscriptionRequest, T> g) {
        return obj -> g.apply((GetSubscriptionRequest) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    public interface Builder extends CodeCatalystRequest.Builder, SdkPojo, CopyableBuilder<Builder, GetSubscriptionRequest> {
        /**
         * <p>
         * The name of the space.
         * </p>
         * 
         * @param spaceName
         *        The name of the space.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder spaceName(String spaceName);

        @Override
        Builder overrideConfiguration(AwsRequestOverrideConfiguration overrideConfiguration);

        @Override
        Builder overrideConfiguration(Consumer<AwsRequestOverrideConfiguration.Builder> builderConsumer);
    }

    static final class BuilderImpl extends CodeCatalystRequest.BuilderImpl implements Builder {
        private String spaceName;

        private BuilderImpl() {
        }

        private BuilderImpl(GetSubscriptionRequest model) {
            super(model);
            spaceName(model.spaceName);
        }

        public final String getSpaceName() {
            return spaceName;
        }

        public final void setSpaceName(String spaceName) {
            this.spaceName = spaceName;
        }

        @Override
        public final Builder spaceName(String spaceName) {
            this.spaceName = spaceName;
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
        public GetSubscriptionRequest build() {
            return new GetSubscriptionRequest(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}
