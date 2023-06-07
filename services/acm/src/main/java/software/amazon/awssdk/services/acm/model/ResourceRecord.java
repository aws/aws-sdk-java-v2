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

package software.amazon.awssdk.services.acm.model;

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
 * Contains a DNS record value that you can use to validate ownership or control of a domain. This is used by the
 * <a>DescribeCertificate</a> action.
 * </p>
 */
@Generated("software.amazon.awssdk:codegen")
public final class ResourceRecord implements SdkPojo, Serializable, ToCopyableBuilder<ResourceRecord.Builder, ResourceRecord> {
    private static final SdkField<String> NAME_FIELD = SdkField.<String> builder(MarshallingType.STRING).memberName("Name")
            .getter(getter(ResourceRecord::name)).setter(setter(Builder::name))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("Name").build()).build();

    private static final SdkField<String> TYPE_FIELD = SdkField.<String> builder(MarshallingType.STRING).memberName("Type")
            .getter(getter(ResourceRecord::typeAsString)).setter(setter(Builder::type))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("Type").build()).build();

    private static final SdkField<String> VALUE_FIELD = SdkField.<String> builder(MarshallingType.STRING).memberName("Value")
            .getter(getter(ResourceRecord::value)).setter(setter(Builder::value))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("Value").build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(NAME_FIELD, TYPE_FIELD,
            VALUE_FIELD));

    private static final long serialVersionUID = 1L;

    private final String name;

    private final String type;

    private final String value;

    private ResourceRecord(BuilderImpl builder) {
        this.name = builder.name;
        this.type = builder.type;
        this.value = builder.value;
    }

    /**
     * <p>
     * The name of the DNS record to create in your domain. This is supplied by ACM.
     * </p>
     * 
     * @return The name of the DNS record to create in your domain. This is supplied by ACM.
     */
    public final String name() {
        return name;
    }

    /**
     * <p>
     * The type of DNS record. Currently this can be <code>CNAME</code>.
     * </p>
     * <p>
     * If the service returns an enum value that is not available in the current SDK version, {@link #type} will return
     * {@link RecordType#UNKNOWN_TO_SDK_VERSION}. The raw value returned by the service is available from
     * {@link #typeAsString}.
     * </p>
     * 
     * @return The type of DNS record. Currently this can be <code>CNAME</code>.
     * @see RecordType
     */
    public final RecordType type() {
        return RecordType.fromValue(type);
    }

    /**
     * <p>
     * The type of DNS record. Currently this can be <code>CNAME</code>.
     * </p>
     * <p>
     * If the service returns an enum value that is not available in the current SDK version, {@link #type} will return
     * {@link RecordType#UNKNOWN_TO_SDK_VERSION}. The raw value returned by the service is available from
     * {@link #typeAsString}.
     * </p>
     * 
     * @return The type of DNS record. Currently this can be <code>CNAME</code>.
     * @see RecordType
     */
    public final String typeAsString() {
        return type;
    }

    /**
     * <p>
     * The value of the CNAME record to add to your DNS database. This is supplied by ACM.
     * </p>
     * 
     * @return The value of the CNAME record to add to your DNS database. This is supplied by ACM.
     */
    public final String value() {
        return value;
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
        hashCode = 31 * hashCode + Objects.hashCode(name());
        hashCode = 31 * hashCode + Objects.hashCode(typeAsString());
        hashCode = 31 * hashCode + Objects.hashCode(value());
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
        if (!(obj instanceof ResourceRecord)) {
            return false;
        }
        ResourceRecord other = (ResourceRecord) obj;
        return Objects.equals(name(), other.name()) && Objects.equals(typeAsString(), other.typeAsString())
                && Objects.equals(value(), other.value());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public final String toString() {
        return ToString.builder("ResourceRecord").add("Name", name()).add("Type", typeAsString()).add("Value", value()).build();
    }

    public final <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
        case "Name":
            return Optional.ofNullable(clazz.cast(name()));
        case "Type":
            return Optional.ofNullable(clazz.cast(typeAsString()));
        case "Value":
            return Optional.ofNullable(clazz.cast(value()));
        default:
            return Optional.empty();
        }
    }

    @Override
    public final List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    private static <T> Function<Object, T> getter(Function<ResourceRecord, T> g) {
        return obj -> g.apply((ResourceRecord) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    public interface Builder extends SdkPojo, CopyableBuilder<Builder, ResourceRecord> {
        /**
         * <p>
         * The name of the DNS record to create in your domain. This is supplied by ACM.
         * </p>
         * 
         * @param name
         *        The name of the DNS record to create in your domain. This is supplied by ACM.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder name(String name);

        /**
         * <p>
         * The type of DNS record. Currently this can be <code>CNAME</code>.
         * </p>
         * 
         * @param type
         *        The type of DNS record. Currently this can be <code>CNAME</code>.
         * @see RecordType
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see RecordType
         */
        Builder type(String type);

        /**
         * <p>
         * The type of DNS record. Currently this can be <code>CNAME</code>.
         * </p>
         * 
         * @param type
         *        The type of DNS record. Currently this can be <code>CNAME</code>.
         * @see RecordType
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see RecordType
         */
        Builder type(RecordType type);

        /**
         * <p>
         * The value of the CNAME record to add to your DNS database. This is supplied by ACM.
         * </p>
         * 
         * @param value
         *        The value of the CNAME record to add to your DNS database. This is supplied by ACM.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder value(String value);
    }

    static final class BuilderImpl implements Builder {
        private String name;

        private String type;

        private String value;

        private BuilderImpl() {
        }

        private BuilderImpl(ResourceRecord model) {
            name(model.name);
            type(model.type);
            value(model.value);
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

        public final String getType() {
            return type;
        }

        public final void setType(String type) {
            this.type = type;
        }

        @Override
        public final Builder type(String type) {
            this.type = type;
            return this;
        }

        @Override
        public final Builder type(RecordType type) {
            this.type(type == null ? null : type.toString());
            return this;
        }

        public final String getValue() {
            return value;
        }

        public final void setValue(String value) {
            this.value = value;
        }

        @Override
        public final Builder value(String value) {
            this.value = value;
            return this;
        }

        @Override
        public ResourceRecord build() {
            return new ResourceRecord(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}
