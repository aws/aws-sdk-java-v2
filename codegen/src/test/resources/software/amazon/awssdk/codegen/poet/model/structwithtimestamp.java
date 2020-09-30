package software.amazon.awssdk.services.jsonprotocoltests.model;

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
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 */
@Generated("software.amazon.awssdk:codegen")
public final class StructWithTimestamp implements SdkPojo, Serializable,
                                                  ToCopyableBuilder<StructWithTimestamp.Builder, StructWithTimestamp> {
    private static final SdkField<Instant> NESTED_TIMESTAMP_FIELD = SdkField.<Instant> builder(MarshallingType.INSTANT)
        .memberName("NestedTimestamp").getter(getter(StructWithTimestamp::nestedTimestamp))
        .setter(setter(Builder::nestedTimestamp))
        .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("NestedTimestamp").build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(NESTED_TIMESTAMP_FIELD));

    private static final long serialVersionUID = 1L;

    private final Instant nestedTimestamp;

    private StructWithTimestamp(BuilderImpl builder) {
        this.nestedTimestamp = builder.nestedTimestamp;
    }

    /**
     * Returns the value of the NestedTimestamp property for this object.
     *
     * @return The value of the NestedTimestamp property for this object.
     */
    public Instant nestedTimestamp() {
        return nestedTimestamp;
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
    public int hashCode() {
        int hashCode = 1;
        hashCode = 31 * hashCode + Objects.hashCode(nestedTimestamp());
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        return equalsBySdkFields(obj);
    }

    @Override
    public boolean equalsBySdkFields(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof StructWithTimestamp)) {
            return false;
        }
        StructWithTimestamp other = (StructWithTimestamp) obj;
        return Objects.equals(nestedTimestamp(), other.nestedTimestamp());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public String toString() {
        return ToString.builder("StructWithTimestamp").add("NestedTimestamp", nestedTimestamp()).build();
    }

    public <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
            case "NestedTimestamp":
                return Optional.ofNullable(clazz.cast(nestedTimestamp()));
            default:
                return Optional.empty();
        }
    }

    @Override
    public List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    private static <T> Function<Object, T> getter(Function<StructWithTimestamp, T> g) {
        return obj -> g.apply((StructWithTimestamp) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    public interface Builder extends SdkPojo, CopyableBuilder<Builder, StructWithTimestamp> {
        /**
         * Sets the value of the NestedTimestamp property for this object.
         *
         * @param nestedTimestamp
         *        The new value for the NestedTimestamp property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder nestedTimestamp(Instant nestedTimestamp);
    }

    static final class BuilderImpl implements Builder {
        private Instant nestedTimestamp;

        private BuilderImpl() {
        }

        private BuilderImpl(StructWithTimestamp model) {
            nestedTimestamp(model.nestedTimestamp);
        }

        public final Instant getNestedTimestamp() {
            return nestedTimestamp;
        }

        @Override
        public final Builder nestedTimestamp(Instant nestedTimestamp) {
            this.nestedTimestamp = nestedTimestamp;
            return this;
        }

        public final void setNestedTimestamp(Instant nestedTimestamp) {
            this.nestedTimestamp = nestedTimestamp;
        }

        @Override
        public StructWithTimestamp build() {
            return new StructWithTimestamp(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}
