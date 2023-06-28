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
 * Information about connection details for a Dev Environment.
 * </p>
 */
@Generated("software.amazon.awssdk:codegen")
public final class DevEnvironmentAccessDetails implements SdkPojo, Serializable,
        ToCopyableBuilder<DevEnvironmentAccessDetails.Builder, DevEnvironmentAccessDetails> {
    private static final SdkField<String> STREAM_URL_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("streamUrl").getter(getter(DevEnvironmentAccessDetails::streamUrl)).setter(setter(Builder::streamUrl))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("streamUrl").build()).build();

    private static final SdkField<String> TOKEN_VALUE_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("tokenValue").getter(getter(DevEnvironmentAccessDetails::tokenValue)).setter(setter(Builder::tokenValue))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("tokenValue").build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(STREAM_URL_FIELD,
            TOKEN_VALUE_FIELD));

    private static final long serialVersionUID = 1L;

    private final String streamUrl;

    private final String tokenValue;

    private DevEnvironmentAccessDetails(BuilderImpl builder) {
        this.streamUrl = builder.streamUrl;
        this.tokenValue = builder.tokenValue;
    }

    /**
     * <p>
     * The URL used to send commands to and from the Dev Environment.
     * </p>
     * 
     * @return The URL used to send commands to and from the Dev Environment.
     */
    public final String streamUrl() {
        return streamUrl;
    }

    /**
     * <p>
     * An encrypted token value that contains session and caller information used to authenticate the connection.
     * </p>
     * 
     * @return An encrypted token value that contains session and caller information used to authenticate the
     *         connection.
     */
    public final String tokenValue() {
        return tokenValue;
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
        hashCode = 31 * hashCode + Objects.hashCode(streamUrl());
        hashCode = 31 * hashCode + Objects.hashCode(tokenValue());
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
        if (!(obj instanceof DevEnvironmentAccessDetails)) {
            return false;
        }
        DevEnvironmentAccessDetails other = (DevEnvironmentAccessDetails) obj;
        return Objects.equals(streamUrl(), other.streamUrl()) && Objects.equals(tokenValue(), other.tokenValue());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public final String toString() {
        return ToString.builder("DevEnvironmentAccessDetails")
                .add("StreamUrl", streamUrl() == null ? null : "*** Sensitive Data Redacted ***")
                .add("TokenValue", tokenValue() == null ? null : "*** Sensitive Data Redacted ***").build();
    }

    public final <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
        case "streamUrl":
            return Optional.ofNullable(clazz.cast(streamUrl()));
        case "tokenValue":
            return Optional.ofNullable(clazz.cast(tokenValue()));
        default:
            return Optional.empty();
        }
    }

    @Override
    public final List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    private static <T> Function<Object, T> getter(Function<DevEnvironmentAccessDetails, T> g) {
        return obj -> g.apply((DevEnvironmentAccessDetails) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    public interface Builder extends SdkPojo, CopyableBuilder<Builder, DevEnvironmentAccessDetails> {
        /**
         * <p>
         * The URL used to send commands to and from the Dev Environment.
         * </p>
         * 
         * @param streamUrl
         *        The URL used to send commands to and from the Dev Environment.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder streamUrl(String streamUrl);

        /**
         * <p>
         * An encrypted token value that contains session and caller information used to authenticate the connection.
         * </p>
         * 
         * @param tokenValue
         *        An encrypted token value that contains session and caller information used to authenticate the
         *        connection.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder tokenValue(String tokenValue);
    }

    static final class BuilderImpl implements Builder {
        private String streamUrl;

        private String tokenValue;

        private BuilderImpl() {
        }

        private BuilderImpl(DevEnvironmentAccessDetails model) {
            streamUrl(model.streamUrl);
            tokenValue(model.tokenValue);
        }

        public final String getStreamUrl() {
            return streamUrl;
        }

        public final void setStreamUrl(String streamUrl) {
            this.streamUrl = streamUrl;
        }

        @Override
        public final Builder streamUrl(String streamUrl) {
            this.streamUrl = streamUrl;
            return this;
        }

        public final String getTokenValue() {
            return tokenValue;
        }

        public final void setTokenValue(String tokenValue) {
            this.tokenValue = tokenValue;
        }

        @Override
        public final Builder tokenValue(String tokenValue) {
            this.tokenValue = tokenValue;
            return this;
        }

        @Override
        public DevEnvironmentAccessDetails build() {
            return new DevEnvironmentAccessDetails(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}
