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
 * Information about the payload of an event recording Amazon CodeCatalyst activity.
 * </p>
 */
@Generated("software.amazon.awssdk:codegen")
public final class EventPayload implements SdkPojo, Serializable, ToCopyableBuilder<EventPayload.Builder, EventPayload> {
    private static final SdkField<String> CONTENT_TYPE_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("contentType").getter(getter(EventPayload::contentType)).setter(setter(Builder::contentType))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("contentType").build()).build();

    private static final SdkField<String> DATA_FIELD = SdkField.<String> builder(MarshallingType.STRING).memberName("data")
            .getter(getter(EventPayload::data)).setter(setter(Builder::data))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("data").build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays
            .asList(CONTENT_TYPE_FIELD, DATA_FIELD));

    private static final long serialVersionUID = 1L;

    private final String contentType;

    private final String data;

    private EventPayload(BuilderImpl builder) {
        this.contentType = builder.contentType;
        this.data = builder.data;
    }

    /**
     * <p>
     * The type of content in the event payload.
     * </p>
     * 
     * @return The type of content in the event payload.
     */
    public final String contentType() {
        return contentType;
    }

    /**
     * <p>
     * The data included in the event payload.
     * </p>
     * 
     * @return The data included in the event payload.
     */
    public final String data() {
        return data;
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
        hashCode = 31 * hashCode + Objects.hashCode(contentType());
        hashCode = 31 * hashCode + Objects.hashCode(data());
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
        if (!(obj instanceof EventPayload)) {
            return false;
        }
        EventPayload other = (EventPayload) obj;
        return Objects.equals(contentType(), other.contentType()) && Objects.equals(data(), other.data());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public final String toString() {
        return ToString.builder("EventPayload").add("ContentType", contentType()).add("Data", data()).build();
    }

    public final <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
        case "contentType":
            return Optional.ofNullable(clazz.cast(contentType()));
        case "data":
            return Optional.ofNullable(clazz.cast(data()));
        default:
            return Optional.empty();
        }
    }

    @Override
    public final List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    private static <T> Function<Object, T> getter(Function<EventPayload, T> g) {
        return obj -> g.apply((EventPayload) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    public interface Builder extends SdkPojo, CopyableBuilder<Builder, EventPayload> {
        /**
         * <p>
         * The type of content in the event payload.
         * </p>
         * 
         * @param contentType
         *        The type of content in the event payload.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder contentType(String contentType);

        /**
         * <p>
         * The data included in the event payload.
         * </p>
         * 
         * @param data
         *        The data included in the event payload.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder data(String data);
    }

    static final class BuilderImpl implements Builder {
        private String contentType;

        private String data;

        private BuilderImpl() {
        }

        private BuilderImpl(EventPayload model) {
            contentType(model.contentType);
            data(model.data);
        }

        public final String getContentType() {
            return contentType;
        }

        public final void setContentType(String contentType) {
            this.contentType = contentType;
        }

        @Override
        public final Builder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public final String getData() {
            return data;
        }

        public final void setData(String data) {
            this.data = data;
        }

        @Override
        public final Builder data(String data) {
            this.data = data;
            return this;
        }

        @Override
        public EventPayload build() {
            return new EventPayload(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}
