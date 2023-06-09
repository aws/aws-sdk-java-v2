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
public final class VerifySessionResponse extends CodeCatalystResponse implements
        ToCopyableBuilder<VerifySessionResponse.Builder, VerifySessionResponse> {
    private static final SdkField<String> IDENTITY_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("identity").getter(getter(VerifySessionResponse::identity)).setter(setter(Builder::identity))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("identity").build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(IDENTITY_FIELD));

    private final String identity;

    private VerifySessionResponse(BuilderImpl builder) {
        super(builder);
        this.identity = builder.identity;
    }

    /**
     * <p>
     * The system-generated unique ID of the user in Amazon CodeCatalyst.
     * </p>
     * 
     * @return The system-generated unique ID of the user in Amazon CodeCatalyst.
     */
    public final String identity() {
        return identity;
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
        hashCode = 31 * hashCode + Objects.hashCode(identity());
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
        if (!(obj instanceof VerifySessionResponse)) {
            return false;
        }
        VerifySessionResponse other = (VerifySessionResponse) obj;
        return Objects.equals(identity(), other.identity());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public final String toString() {
        return ToString.builder("VerifySessionResponse").add("Identity", identity()).build();
    }

    public final <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
        case "identity":
            return Optional.ofNullable(clazz.cast(identity()));
        default:
            return Optional.empty();
        }
    }

    @Override
    public final List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    private static <T> Function<Object, T> getter(Function<VerifySessionResponse, T> g) {
        return obj -> g.apply((VerifySessionResponse) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    public interface Builder extends CodeCatalystResponse.Builder, SdkPojo, CopyableBuilder<Builder, VerifySessionResponse> {
        /**
         * <p>
         * The system-generated unique ID of the user in Amazon CodeCatalyst.
         * </p>
         * 
         * @param identity
         *        The system-generated unique ID of the user in Amazon CodeCatalyst.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder identity(String identity);
    }

    static final class BuilderImpl extends CodeCatalystResponse.BuilderImpl implements Builder {
        private String identity;

        private BuilderImpl() {
        }

        private BuilderImpl(VerifySessionResponse model) {
            super(model);
            identity(model.identity);
        }

        public final String getIdentity() {
            return identity;
        }

        public final void setIdentity(String identity) {
            this.identity = identity;
        }

        @Override
        public final Builder identity(String identity) {
            this.identity = identity;
            return this;
        }

        @Override
        public VerifySessionResponse build() {
            return new VerifySessionResponse(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}
