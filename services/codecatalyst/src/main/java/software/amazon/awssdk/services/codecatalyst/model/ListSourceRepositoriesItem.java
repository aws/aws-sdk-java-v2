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
 * <p>
 * Information about a source repository returned in a list of source repositories.
 * </p>
 */
@Generated("software.amazon.awssdk:codegen")
public final class ListSourceRepositoriesItem implements SdkPojo, Serializable,
        ToCopyableBuilder<ListSourceRepositoriesItem.Builder, ListSourceRepositoriesItem> {
    private static final SdkField<String> ID_FIELD = SdkField.<String> builder(MarshallingType.STRING).memberName("id")
            .getter(getter(ListSourceRepositoriesItem::id)).setter(setter(Builder::id))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("id").build()).build();

    private static final SdkField<String> NAME_FIELD = SdkField.<String> builder(MarshallingType.STRING).memberName("name")
            .getter(getter(ListSourceRepositoriesItem::name)).setter(setter(Builder::name))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("name").build()).build();

    private static final SdkField<String> DESCRIPTION_FIELD = SdkField.<String> builder(MarshallingType.STRING)
            .memberName("description").getter(getter(ListSourceRepositoriesItem::description))
            .setter(setter(Builder::description))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("description").build()).build();

    private static final SdkField<Instant> LAST_UPDATED_TIME_FIELD = SdkField
            .<Instant> builder(MarshallingType.INSTANT)
            .memberName("lastUpdatedTime")
            .getter(getter(ListSourceRepositoriesItem::lastUpdatedTime))
            .setter(setter(Builder::lastUpdatedTime))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("lastUpdatedTime").build(),
                    TimestampFormatTrait.create(TimestampFormatTrait.Format.ISO_8601)).build();

    private static final SdkField<Instant> CREATED_TIME_FIELD = SdkField
            .<Instant> builder(MarshallingType.INSTANT)
            .memberName("createdTime")
            .getter(getter(ListSourceRepositoriesItem::createdTime))
            .setter(setter(Builder::createdTime))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("createdTime").build(),
                    TimestampFormatTrait.create(TimestampFormatTrait.Format.ISO_8601)).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(ID_FIELD, NAME_FIELD,
            DESCRIPTION_FIELD, LAST_UPDATED_TIME_FIELD, CREATED_TIME_FIELD));

    private static final long serialVersionUID = 1L;

    private final String id;

    private final String name;

    private final String description;

    private final Instant lastUpdatedTime;

    private final Instant createdTime;

    private ListSourceRepositoriesItem(BuilderImpl builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.description = builder.description;
        this.lastUpdatedTime = builder.lastUpdatedTime;
        this.createdTime = builder.createdTime;
    }

    /**
     * <p>
     * The system-generated unique ID of the source repository.
     * </p>
     * 
     * @return The system-generated unique ID of the source repository.
     */
    public final String id() {
        return id;
    }

    /**
     * <p>
     * The name of the source repository.
     * </p>
     * 
     * @return The name of the source repository.
     */
    public final String name() {
        return name;
    }

    /**
     * <p>
     * The description of the repository, if any.
     * </p>
     * 
     * @return The description of the repository, if any.
     */
    public final String description() {
        return description;
    }

    /**
     * <p>
     * The time the source repository was last updated, in coordinated universal time (UTC) timestamp format as
     * specified in <a href="https://www.rfc-editor.org/rfc/rfc3339#section-5.6">RFC 3339</a>.
     * </p>
     * 
     * @return The time the source repository was last updated, in coordinated universal time (UTC) timestamp format as
     *         specified in <a href="https://www.rfc-editor.org/rfc/rfc3339#section-5.6">RFC 3339</a>.
     */
    public final Instant lastUpdatedTime() {
        return lastUpdatedTime;
    }

    /**
     * <p>
     * The time the source repository was created, in coordinated universal time (UTC) timestamp format as specified in
     * <a href="https://www.rfc-editor.org/rfc/rfc3339#section-5.6">RFC 3339</a>.
     * </p>
     * 
     * @return The time the source repository was created, in coordinated universal time (UTC) timestamp format as
     *         specified in <a href="https://www.rfc-editor.org/rfc/rfc3339#section-5.6">RFC 3339</a>.
     */
    public final Instant createdTime() {
        return createdTime;
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
        hashCode = 31 * hashCode + Objects.hashCode(id());
        hashCode = 31 * hashCode + Objects.hashCode(name());
        hashCode = 31 * hashCode + Objects.hashCode(description());
        hashCode = 31 * hashCode + Objects.hashCode(lastUpdatedTime());
        hashCode = 31 * hashCode + Objects.hashCode(createdTime());
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
        if (!(obj instanceof ListSourceRepositoriesItem)) {
            return false;
        }
        ListSourceRepositoriesItem other = (ListSourceRepositoriesItem) obj;
        return Objects.equals(id(), other.id()) && Objects.equals(name(), other.name())
                && Objects.equals(description(), other.description())
                && Objects.equals(lastUpdatedTime(), other.lastUpdatedTime())
                && Objects.equals(createdTime(), other.createdTime());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public final String toString() {
        return ToString.builder("ListSourceRepositoriesItem").add("Id", id()).add("Name", name())
                .add("Description", description()).add("LastUpdatedTime", lastUpdatedTime()).add("CreatedTime", createdTime())
                .build();
    }

    public final <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
        case "id":
            return Optional.ofNullable(clazz.cast(id()));
        case "name":
            return Optional.ofNullable(clazz.cast(name()));
        case "description":
            return Optional.ofNullable(clazz.cast(description()));
        case "lastUpdatedTime":
            return Optional.ofNullable(clazz.cast(lastUpdatedTime()));
        case "createdTime":
            return Optional.ofNullable(clazz.cast(createdTime()));
        default:
            return Optional.empty();
        }
    }

    @Override
    public final List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    private static <T> Function<Object, T> getter(Function<ListSourceRepositoriesItem, T> g) {
        return obj -> g.apply((ListSourceRepositoriesItem) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    public interface Builder extends SdkPojo, CopyableBuilder<Builder, ListSourceRepositoriesItem> {
        /**
         * <p>
         * The system-generated unique ID of the source repository.
         * </p>
         * 
         * @param id
         *        The system-generated unique ID of the source repository.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder id(String id);

        /**
         * <p>
         * The name of the source repository.
         * </p>
         * 
         * @param name
         *        The name of the source repository.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder name(String name);

        /**
         * <p>
         * The description of the repository, if any.
         * </p>
         * 
         * @param description
         *        The description of the repository, if any.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder description(String description);

        /**
         * <p>
         * The time the source repository was last updated, in coordinated universal time (UTC) timestamp format as
         * specified in <a href="https://www.rfc-editor.org/rfc/rfc3339#section-5.6">RFC 3339</a>.
         * </p>
         * 
         * @param lastUpdatedTime
         *        The time the source repository was last updated, in coordinated universal time (UTC) timestamp format
         *        as specified in <a href="https://www.rfc-editor.org/rfc/rfc3339#section-5.6">RFC 3339</a>.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder lastUpdatedTime(Instant lastUpdatedTime);

        /**
         * <p>
         * The time the source repository was created, in coordinated universal time (UTC) timestamp format as specified
         * in <a href="https://www.rfc-editor.org/rfc/rfc3339#section-5.6">RFC 3339</a>.
         * </p>
         * 
         * @param createdTime
         *        The time the source repository was created, in coordinated universal time (UTC) timestamp format as
         *        specified in <a href="https://www.rfc-editor.org/rfc/rfc3339#section-5.6">RFC 3339</a>.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder createdTime(Instant createdTime);
    }

    static final class BuilderImpl implements Builder {
        private String id;

        private String name;

        private String description;

        private Instant lastUpdatedTime;

        private Instant createdTime;

        private BuilderImpl() {
        }

        private BuilderImpl(ListSourceRepositoriesItem model) {
            id(model.id);
            name(model.name);
            description(model.description);
            lastUpdatedTime(model.lastUpdatedTime);
            createdTime(model.createdTime);
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

        public final String getDescription() {
            return description;
        }

        public final void setDescription(String description) {
            this.description = description;
        }

        @Override
        public final Builder description(String description) {
            this.description = description;
            return this;
        }

        public final Instant getLastUpdatedTime() {
            return lastUpdatedTime;
        }

        public final void setLastUpdatedTime(Instant lastUpdatedTime) {
            this.lastUpdatedTime = lastUpdatedTime;
        }

        @Override
        public final Builder lastUpdatedTime(Instant lastUpdatedTime) {
            this.lastUpdatedTime = lastUpdatedTime;
            return this;
        }

        public final Instant getCreatedTime() {
            return createdTime;
        }

        public final void setCreatedTime(Instant createdTime) {
            this.createdTime = createdTime;
        }

        @Override
        public final Builder createdTime(Instant createdTime) {
            this.createdTime = createdTime;
            return this;
        }

        @Override
        public ListSourceRepositoriesItem build() {
            return new ListSourceRepositoriesItem(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}
