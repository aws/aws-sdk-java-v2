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
public final class GetSourceRepositoryCloneUrlsResponse extends CodeCatalystResponse implements
        ToCopyableBuilder<GetSourceRepositoryCloneUrlsResponse.Builder, GetSourceRepositoryCloneUrlsResponse> {
    private static final SdkField<String> HTTPS_FIELD = SdkField.<String> builder(MarshallingType.STRING).memberName("https")
            .getter(getter(GetSourceRepositoryCloneUrlsResponse::https)).setter(setter(Builder::https))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("https").build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(HTTPS_FIELD));

    private final String https;

    private GetSourceRepositoryCloneUrlsResponse(BuilderImpl builder) {
        super(builder);
        this.https = builder.https;
    }

    /**
     * <p>
     * The HTTPS URL to use when cloning the source repository.
     * </p>
     * 
     * @return The HTTPS URL to use when cloning the source repository.
     */
    public final String https() {
        return https;
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
        hashCode = 31 * hashCode + Objects.hashCode(https());
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
        if (!(obj instanceof GetSourceRepositoryCloneUrlsResponse)) {
            return false;
        }
        GetSourceRepositoryCloneUrlsResponse other = (GetSourceRepositoryCloneUrlsResponse) obj;
        return Objects.equals(https(), other.https());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public final String toString() {
        return ToString.builder("GetSourceRepositoryCloneUrlsResponse").add("Https", https()).build();
    }

    public final <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
        case "https":
            return Optional.ofNullable(clazz.cast(https()));
        default:
            return Optional.empty();
        }
    }

    @Override
    public final List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    private static <T> Function<Object, T> getter(Function<GetSourceRepositoryCloneUrlsResponse, T> g) {
        return obj -> g.apply((GetSourceRepositoryCloneUrlsResponse) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    public interface Builder extends CodeCatalystResponse.Builder, SdkPojo,
            CopyableBuilder<Builder, GetSourceRepositoryCloneUrlsResponse> {
        /**
         * <p>
         * The HTTPS URL to use when cloning the source repository.
         * </p>
         * 
         * @param https
         *        The HTTPS URL to use when cloning the source repository.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder https(String https);
    }

    static final class BuilderImpl extends CodeCatalystResponse.BuilderImpl implements Builder {
        private String https;

        private BuilderImpl() {
        }

        private BuilderImpl(GetSourceRepositoryCloneUrlsResponse model) {
            super(model);
            https(model.https);
        }

        public final String getHttps() {
            return https;
        }

        public final void setHttps(String https) {
            this.https = https;
        }

        @Override
        public final Builder https(String https) {
            this.https = https;
            return this;
        }

        @Override
        public GetSourceRepositoryCloneUrlsResponse build() {
            return new GetSourceRepositoryCloneUrlsResponse(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}
